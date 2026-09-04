package com.sgc.backend.exception;

/**
 * Recurso solicitado nao existe. Traduzida para HTTP 404 pelo
 * {@link GlobalExceptionHandler}.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    /**
     * Atalho para a mensagem mais comum.
     *
     * @param resource nome do recurso, ex.: "Colaborador"
     * @param id       identificador procurado
     */
    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException("%s nao encontrado(a) para o identificador %s".formatted(resource, id));
    }
}
