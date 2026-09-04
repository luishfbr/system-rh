package com.sgc.backend.dto.response;

/**
 * Par id/nome das listas de apoio (area, cargo, unidade).
 *
 * <p>Um unico tipo atende as tres: o frontend precisa do id para montar o
 * {@code select} e do rotulo para exibir.
 */
public record ReferenceResponse(Long id, String name) {
}
