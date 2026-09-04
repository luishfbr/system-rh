package com.sgc.backend.exception;

/**
 * Violacao de uma regra de negocio (RN01-RN07).
 *
 * <p>Traduzida para HTTP <b>422 Unprocessable Entity</b>: a requisicao esta
 * sintaticamente correta e passou pela validacao de formato, mas o estado atual
 * do sistema nao permite a operacao -- por exemplo, alterar o cargo de um
 * colaborador ja desligado (RN05).
 *
 * <p>O {@code code} vira o campo {@code type} do ProblemDetail, permitindo ao
 * frontend reagir a uma regra especifica sem depender do texto da mensagem.
 */
public class BusinessRuleException extends RuntimeException {

    private final String code;

    public BusinessRuleException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
