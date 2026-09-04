package com.sgc.backend.exception;

/**
 * Tentativa de criar um registro que violaria uma restricao de unicidade
 * (RN01 email, RN02/RN07 CPF). Traduzida para HTTP 409 Conflict.
 */
public class DuplicateResourceException extends RuntimeException {

    private final String field;

    public DuplicateResourceException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
