package com.sgc.backend.controller;

import com.sgc.backend.domain.enums.EmploymentStatus;
import com.sgc.backend.dto.request.CreateEmployeeRequest;
import com.sgc.backend.dto.request.TerminateEmploymentRequest;
import com.sgc.backend.dto.request.UpdateEmployeeRequest;
import com.sgc.backend.dto.response.ApiResponse;
import com.sgc.backend.dto.response.EmployeeResponse;
import com.sgc.backend.dto.response.EmployeeSummaryResponse;
import com.sgc.backend.dto.response.EmploymentEventResponse;
import com.sgc.backend.dto.response.PageResponse;
import com.sgc.backend.service.EmploymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;

/**
 * Colaboradores (RF04, RF05, RF06, RF08).
 *
 * <p><b>O que "colaborador" significa aqui:</b> o {@code {id}} destas rotas e o
 * id do <b>vinculo</b> (periodo de contrato), nao o da pessoa. Uma pessoa
 * readmitida tem dois vinculos e, portanto, dois ids -- o antigo continua
 * consultavel com seu historico. Para ver a pessoa e todos os seus periodos, use
 * {@code /api/v1/persons/by-cpf/{cpf}}.
 *
 * <p><b>Readmissao nao tem endpoint proprio:</b> e um {@code POST /employees}
 * com o mesmo CPF. A operacao so e recusada se ja houver vinculo em aberto, que
 * e exatamente o que a RN07 determina.
 *
 * <p><b>Permissoes (RN04):</b> o cadastro esta liberado para {@code HR_MANAGER} e
 * {@code ADMIN}; apenas a exclusao definitiva exige {@code ADMIN}.
 */
@RestController
@RequestMapping("/api/v1/employees")
@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
@Tag(name = "Colaboradores", description = "Admissao, edicao, desligamento e consulta")
public class EmployeeController {

    private final EmploymentService employmentService;

    public EmployeeController(EmploymentService employmentService) {
        this.employmentService = employmentService;
    }

    // ------------------------------------------------------------------
    // RF04 / RF08 -- admitir
    // ------------------------------------------------------------------

    @PostMapping
    @Operation(summary = "RF04/RF08 - Admitir colaborador",
            description = """
                    Cria o vinculo. Se o CPF ainda nao existe, cadastra tambem a pessoa;
                    se ja existe e nao ha vinculo em aberto, trata-se de uma readmissao e
                    um novo periodo de contrato e criado ao lado do anterior.

                    Recusa com 422 quando a pessoa ja possui vinculo em aberto (RN07).
                    """)
    public ResponseEntity<ApiResponse<EmployeeResponse>> admit(@Valid @RequestBody CreateEmployeeRequest request) {
        EmployeeResponse created = employmentService.admit(request);

        String message = created.employmentCount() != null && created.employmentCount() > 1
                ? "Colaborador readmitido com sucesso."
                : "Colaborador admitido com sucesso.";

        return ResponseEntity
                .created(URI.create("/api/v1/employees/" + created.id()))
                .body(ApiResponse.ok(message, created));
    }

    // ------------------------------------------------------------------
    // RF08 -- desligar
    // ------------------------------------------------------------------

    @PatchMapping("/{id}/terminate")
    @Operation(summary = "RF08 - Desligar colaborador",
            description = """
                    Encerra o vinculo com data e motivo. O registro nao e apagado: passa a
                    TERMINATED e permanece no historico da pessoa.

                    E o desligamento que libera o CPF para uma futura readmissao.
                    """)
    public ResponseEntity<ApiResponse<EmployeeResponse>> terminate(
            @PathVariable Long id,
            @Valid @RequestBody TerminateEmploymentRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Colaborador desligado com sucesso.", employmentService.terminate(id, request)));
    }

    // ------------------------------------------------------------------
    // RF05 / RF06
    // ------------------------------------------------------------------

    @PutMapping("/{id}")
    @Operation(summary = "RF05 - Editar colaborador",
            description = "RN05: vinculo desligado nao aceita alteracoes. "
                    + "Mudancas de salario, cargo ou setor geram evento proprio na timeline (RN06).")
    public ResponseEntity<ApiResponse<EmployeeResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody UpdateEmployeeRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("Colaborador atualizado com sucesso.", employmentService.update(id, request)));
    }

    @PatchMapping("/{id}/inactivate")
    @Operation(summary = "RF06 - Inativar colaborador",
            description = "Diferente do desligamento: retira das listagens ativas sem encerrar o vinculo.")
    public ResponseEntity<ApiResponse<EmployeeResponse>> inactivate(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Colaborador inativado com sucesso.", employmentService.inactivate(id)));
    }

    @PatchMapping("/{id}/activate")
    @Operation(summary = "RF06 - Reativar colaborador inativo")
    public ResponseEntity<ApiResponse<EmployeeResponse>> activate(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Colaborador reativado com sucesso.", employmentService.activate(id)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "RN04 - Exclusao definitiva (somente ADMIN)",
            description = "Remove o vinculo e sua timeline. O caminho usual e o desligamento (RF08) "
                    + "ou a inativacao (RF06).")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        employmentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @GetMapping
    @Operation(summary = "Lista colaboradores com filtros e paginacao",
            description = "Todos os filtros sao opcionais e combinaveis. "
                    + "Use onlyOpen=true para excluir periodos ja encerrados.")
    public ResponseEntity<ApiResponse<PageResponse<EmployeeSummaryResponse>>> search(
            @Parameter(description = "Busca parcial por nome") @RequestParam(required = false) String name,
            @Parameter(description = "Situacao do vinculo") @RequestParam(required = false) EmploymentStatus status,
            @Parameter(description = "Id da area de atuacao") @RequestParam(required = false) Long departmentId,
            @Parameter(description = "Id da unidade") @RequestParam(required = false) Long unitId,
            @Parameter(description = "Admitidos a partir desta data") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hiredFrom,
            @Parameter(description = "Admitidos ate esta data") @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hiredUntil,
            @Parameter(description = "Apenas vinculos nao desligados") @RequestParam(required = false) Boolean onlyOpen,
            @PageableDefault(size = 20, sort = "hireDate", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(
                employmentService.search(name, status, departmentId, unitId, hiredFrom, hiredUntil, onlyOpen, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta um colaborador pelo id do vinculo")
    public ResponseEntity<ApiResponse<EmployeeResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(employmentService.findById(id)));
    }

    @GetMapping("/by-cpf/{cpf}")
    @Operation(summary = "Consulta o vinculo em aberto de um CPF",
            description = "Devolve 404 se a pessoa nao tem vinculo ativo. "
                    + "Para o historico completo use /api/v1/persons/by-cpf/{cpf}.")
    public ResponseEntity<ApiResponse<EmployeeResponse>> findByCpf(@PathVariable String cpf) {
        return ResponseEntity.ok(ApiResponse.ok(employmentService.findOpenByCpf(cpf)));
    }

    @GetMapping("/{id}/timeline")
    @Operation(summary = "RF11 - Linha do tempo do vinculo",
            description = """
                    Eventos do vinculo, do mais recente para o mais antigo.

                    Por padrao mostra apenas os marcos de carreira. Use
                    includeProfileUpdates=true para incluir tambem as edicoes de
                    dados cadastrais (RF05).
                    """)
    public ResponseEntity<ApiResponse<List<EmploymentEventResponse>>> timeline(
            @PathVariable Long id,
            @Parameter(description = "Incluir edicoes cadastrais") @RequestParam(defaultValue = "false")
            boolean includeProfileUpdates) {

        return ResponseEntity.ok(ApiResponse.ok(employmentService.timeline(id, includeProfileUpdates)));
    }
}
