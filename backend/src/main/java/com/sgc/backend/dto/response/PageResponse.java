package com.sgc.backend.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Recorte estavel de uma pagina de resultados.
 *
 * <p>Existe para nao expor o {@code Page} do Spring Data diretamente na API: a
 * serializacao dele inclui detalhes internos ({@code pageable}, {@code sort},
 * {@code numberOfElements}) que mudam entre versoes do framework e acabariam
 * virando contrato com o frontend sem ninguem ter decidido isso.
 *
 * @param content       os registros desta pagina
 * @param page          numero da pagina atual, base zero
 * @param size          tamanho solicitado
 * @param totalElements total de registros que atendem ao filtro
 * @param totalPages    total de paginas
 * @param last          indica se esta e a ultima pagina
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean last
) {

    /**
     * Converte um {@code Page} de entidades num {@code PageResponse} de DTOs.
     *
     * @param mapper funcao que transforma cada entidade em seu DTO
     */
    public static <E, D> PageResponse<D> from(Page<E> page, Function<E, D> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
