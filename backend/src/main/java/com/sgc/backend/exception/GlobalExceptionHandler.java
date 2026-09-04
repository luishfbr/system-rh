package com.sgc.backend.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Traduz excecoes em respostas HTTP no formato <b>ProblemDetail</b> (RFC 9457).
 *
 * <p>ProblemDetail e o padrao nativo do Spring desde a versao 6: a resposta sai
 * com {@code Content-Type: application/problem+json} e um corpo previsivel
 * ({@code type}, {@code title}, {@code status}, {@code detail}, {@code instance}),
 * em vez de um envelope caseiro. Clientes HTTP e o proprio Swagger ja entendem
 * esse formato.
 *
 * <p>Estender {@code ResponseEntityExceptionHandler} traz de graca o tratamento
 * das excecoes internas do Spring MVC (payload malformado, metodo nao suportado,
 * parametro ausente...), que de outro modo virariam 500.
 *
 * <p><b>Atencao:</b> este advice so alcanca excecoes lancadas a partir do
 * DispatcherServlet. Falhas ocorridas antes, dentro da cadeia de filtros do
 * Spring Security (token ausente ou invalido), sao tratadas pelo
 * {@code AuthenticationEntryPoint} configurado em {@code SecurityConfig}.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Prefixo das URIs de tipo. Nao precisa resolver na web; serve como identificador estavel. */
    private static final String PROBLEM_BASE = "https://sgc.local/problems/";

    // ------------------------------------------------------------------
    // Excecoes de dominio
    // ------------------------------------------------------------------

    @ExceptionHandler(ResourceNotFoundException.class)
    public ProblemDetail handleNotFound(ResourceNotFoundException ex) {
        return problem(HttpStatus.NOT_FOUND, "Recurso nao encontrado", ex.getMessage(), "resource-not-found");
    }

    /**
     * 422 e nao 400: o corpo da requisicao esta bem formado e passou na validacao
     * de formato -- o que impede a operacao e o estado atual do sistema.
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(BusinessRuleException ex) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Regra de negocio violada", ex.getMessage(), ex.getCode());
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ProblemDetail handleDuplicate(DuplicateResourceException ex) {
        ProblemDetail problem = problem(HttpStatus.CONFLICT, "Registro duplicado", ex.getMessage(), "duplicate-resource");
        problem.setProperty("field", ex.getField());
        return problem;
    }

    // ------------------------------------------------------------------
    // Persistencia
    // ------------------------------------------------------------------

    /**
     * Rede de seguranca para as constraints do banco. A validacao de duplicidade
     * acontece antes, no service, com mensagem amigavel; se ainda assim o banco
     * recusar (corrida entre duas requisicoes simultaneas), o erro vira 409 --
     * nunca um 500 com stack trace vazando o nome da constraint.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Violacao de integridade no banco: {}", ex.getMostSpecificCause().getMessage());
        return problem(HttpStatus.CONFLICT,
                "Conflito de dados",
                "A operacao viola uma restricao de integridade do banco de dados.",
                "data-integrity-violation");
    }

    /** Dois usuarios editaram o mesmo registro; o segundo perde (ver @Version em BaseEntity). */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLock(OptimisticLockingFailureException ex) {
        return problem(HttpStatus.CONFLICT,
                "Registro alterado por outro usuario",
                "Este registro foi modificado enquanto voce o editava. Recarregue os dados e tente novamente.",
                "optimistic-lock");
    }

    // ------------------------------------------------------------------
    // Seguranca
    // ------------------------------------------------------------------

    @ExceptionHandler(BadCredentialsException.class)
    public ProblemDetail handleBadCredentials(BadCredentialsException ex) {
        // Mensagem deliberadamente generica: dizer "usuario nao existe" versus
        // "senha incorreta" permitiria enumerar contas validas.
        return problem(HttpStatus.UNAUTHORIZED, "Credenciais invalidas",
                "Email ou senha incorretos.", "bad-credentials");
    }

    @ExceptionHandler(DisabledException.class)
    public ProblemDetail handleDisabled(DisabledException ex) {
        return problem(HttpStatus.FORBIDDEN, "Usuario inativo",
                "Este usuario esta inativo e nao pode acessar o sistema.", "user-disabled");
    }

    /** Lancada pelo {@code @PreAuthorize} quando o perfil do usuario nao permite a acao. */
    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex) {
        return problem(HttpStatus.FORBIDDEN, "Acesso negado",
                "Seu perfil nao possui permissao para executar esta operacao.", "access-denied");
    }

    // ------------------------------------------------------------------
    // Validacao de payload (Bean Validation)
    // ------------------------------------------------------------------

    /**
     * Disparada por {@code @Valid} nos controllers. Alem do 400, devolve a lista
     * de campos rejeitados na propriedade {@code errors}, para o formulario do
     * frontend marcar cada campo.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {

        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of(
                        "field", error.getField(),
                        "message", error.getDefaultMessage() == null ? "valor invalido" : error.getDefaultMessage()))
                .toList();

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST,
                "Dados invalidos",
                "Um ou mais campos nao passaram na validacao.",
                "validation-error");
        problem.setProperty("errors", fieldErrors);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    // ------------------------------------------------------------------
    // Ultimo recurso
    // ------------------------------------------------------------------

    /**
     * Qualquer excecao nao prevista. O stack trace vai para o log; a resposta
     * carrega apenas uma mensagem generica, para nao expor detalhes internos.
     */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Erro nao tratado", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno",
                "Ocorreu um erro inesperado. Se persistir, contate o suporte.",
                "internal-error");
    }

    // ------------------------------------------------------------------

    private ProblemDetail problem(HttpStatus status, String title, String detail, String code) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(PROBLEM_BASE + code));
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
