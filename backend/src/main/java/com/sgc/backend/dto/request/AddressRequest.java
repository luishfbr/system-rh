package com.sgc.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Endereco do colaborador. Opcional dentro do cadastro. */
public record AddressRequest(

        @NotBlank(message = "logradouro e obrigatorio")
        @Size(max = 150)
        String street,

        @Size(max = 20)
        String number,

        @Size(max = 100)
        String complement,

        @Size(max = 100)
        String district,

        @NotBlank(message = "cidade e obrigatoria")
        @Size(max = 100)
        String city,

        @NotBlank(message = "UF e obrigatoria")
        @Pattern(regexp = "^[A-Za-z]{2}$", message = "UF deve ter 2 letras")
        String state,

        @NotBlank(message = "CEP e obrigatorio")
        @Pattern(regexp = "^\\d{5}-?\\d{3}$", message = "CEP deve ter 8 digitos")
        String zipCode
) {
}
