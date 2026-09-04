package com.sgc.backend.dto.response;

/** Endereco do colaborador. */
public record AddressResponse(
        Long id,
        String street,
        String number,
        String complement,
        String district,
        String city,
        String state,
        String zipCode
) {
}
