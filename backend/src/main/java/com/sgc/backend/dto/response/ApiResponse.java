package com.sgc.backend.dto.response;

import java.time.Instant;

/**
 * Envelope das respostas de <b>sucesso</b>.
 *
 * <p>Respostas de erro nao usam esta classe: elas seguem o padrao ProblemDetail
 * (RFC 9457), montado pelo {@code GlobalExceptionHandler}. Por isso nao existe
 * aqui um campo {@code success} -- ele seria sempre {@code true} e nao
 * carregaria informacao alguma; quem indica falha e o proprio status HTTP.
 *
 * @param message texto opcional para a interface exibir ("Colaborador inativado")
 * @param data    o conteudo em si
 */
public record ApiResponse<T>(String message, T data, Instant timestamp) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(null, data, Instant.now());
    }

    public static <T> ApiResponse<T> ok(String message, T data) {
        return new ApiResponse<>(message, data, Instant.now());
    }
}
