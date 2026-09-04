package com.sgc.backend.controller;

import com.sgc.backend.dto.response.ApiResponse;
import com.sgc.backend.dto.response.PersonResponse;
import com.sgc.backend.service.PersonService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Consulta da pessoa fisica e do seu historico de vinculos.
 *
 * <p>Complementa {@code /api/v1/employees}, que trata do vinculo corrente: aqui
 * se responde "quem e essa pessoa e quais periodos ela ja teve na empresa".
 */
@RestController
@RequestMapping("/api/v1/persons")
@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
@Tag(name = "Pessoas", description = "Dados pessoais e historico de vinculos")
public class PersonController {

    private final PersonService personService;

    public PersonController(PersonService personService) {
        this.personService = personService;
    }

    @GetMapping("/by-cpf/{cpf}")
    @Operation(summary = "Consulta a pessoa e todos os seus vinculos pelo CPF",
            description = "Aceita o CPF com ou sem pontuacao. Use antes de admitir, "
                    + "para saber se a pessoa ja trabalhou na empresa.")
    public ResponseEntity<ApiResponse<PersonResponse>> findByCpf(
            @Parameter(description = "CPF com ou sem pontuacao", example = "111.444.777-35")
            @PathVariable String cpf) {
        return ResponseEntity.ok(ApiResponse.ok(personService.findByCpf(cpf)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulta a pessoa e todos os seus vinculos pelo id")
    public ResponseEntity<ApiResponse<PersonResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(personService.findById(id)));
    }
}
