package com.sgc.backend.controller;

import com.sgc.backend.domain.enums.ChangeStatus;
import com.sgc.backend.dto.request.CancelChangeRequest;
import com.sgc.backend.dto.request.CareerChangeRequest;
import com.sgc.backend.dto.response.ApiResponse;
import com.sgc.backend.dto.response.EmploymentChangeResponse;
import com.sgc.backend.dto.response.PageResponse;
import com.sgc.backend.service.EmploymentChangeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

/**
 * Alteracoes de cargo, salario e setor (RF10).
 *
 * <p>Uma alteracao tem <b>data de vigencia</b>: lancada com data futura, fica
 * pendente ate o dia chegar; com data de hoje ou passada, entra em vigor na
 * hora. E por isso que existe um endpoint proprio em vez de simplesmente editar
 * o colaborador -- o {@code PUT /employees/{id}} nao altera mais esses campos.
 *
 * <p>As rotas ficam sob {@code /employees/{id}} porque uma alteracao so existe
 * no contexto de um vinculo. A excecao e {@code GET /career-changes}, a fila
 * global que alimenta o relatorio "Alteracoes em andamento" (RF07).
 */
@RestController
@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
@Tag(name = "Alteracoes de carreira", description = "Mudancas de cargo, salario e setor com data de vigencia")
public class CareerChangeController {

    private final EmploymentChangeService changeService;

    public CareerChangeController(EmploymentChangeService changeService) {
        this.changeService = changeService;
    }

    @PostMapping("/api/v1/employees/{id}/career-changes")
    @Operation(summary = "RF10 - Lancar alteracao de cargo/salario/setor",
            description = """
                    Informe ao menos um entre salario, cargo e area de atuacao.
                    Campos omitidos permanecem como estao.

                    A vigencia define quando passa a valer: hoje ou no passado aplica
                    imediatamente; no futuro fica PENDING e entra em vigor sozinha.

                    Recusa com 422 se o vinculo estiver desligado (RN05).
                    """)
    public ResponseEntity<ApiResponse<EmploymentChangeResponse>> schedule(
            @PathVariable Long id,
            @Valid @RequestBody CareerChangeRequest request) {

        EmploymentChangeResponse created = changeService.schedule(id, request);

        String message = created.status() == ChangeStatus.APPLIED
                ? "Alteracao aplicada com sucesso."
                : "Alteracao programada para %s.".formatted(created.effectiveDate());

        return ResponseEntity
                .created(URI.create("/api/v1/career-changes/" + created.id()))
                .body(ApiResponse.ok(message, created));
    }

    @GetMapping("/api/v1/employees/{id}/career-changes")
    @Operation(summary = "Historico de alteracoes de um colaborador",
            description = "Inclui pendentes, aplicadas e canceladas, da mais recente para a mais antiga.")
    public ResponseEntity<ApiResponse<List<EmploymentChangeResponse>>> listByEmployment(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(changeService.listByEmployment(id)));
    }

    @PatchMapping("/api/v1/career-changes/{changeId}/cancel")
    @Operation(summary = "RF10 - Cancelar alteracao pendente",
            description = """
                    So alteracoes PENDING podem ser canceladas. Uma alteracao ja aplicada
                    virou evento imutavel na timeline; para reverte-la, lance uma nova
                    alteracao no sentido contrario.
                    """)
    public ResponseEntity<ApiResponse<EmploymentChangeResponse>> cancel(
            @PathVariable Long changeId,
            @Valid @RequestBody CancelChangeRequest request) {

        return ResponseEntity.ok(
                ApiResponse.ok("Alteracao cancelada.", changeService.cancel(changeId, request)));
    }

    @GetMapping("/api/v1/career-changes/{changeId}")
    @Operation(summary = "Consulta uma alteracao pelo id")
    public ResponseEntity<ApiResponse<EmploymentChangeResponse>> findById(@PathVariable Long changeId) {
        return ResponseEntity.ok(ApiResponse.ok(changeService.findById(changeId)));
    }

    @GetMapping("/api/v1/career-changes")
    @Operation(summary = "RF07 - Alteracoes em andamento",
            description = """
                    Fila de alteracoes por situacao. Com status=PENDING (o padrao) devolve
                    tudo que foi decidido e ainda nao entrou em vigor -- o relatorio
                    "Alteracoes em andamento".
                    """)
    public ResponseEntity<ApiResponse<PageResponse<EmploymentChangeResponse>>> listByStatus(
            @Parameter(description = "Situacao das alteracoes") @RequestParam(defaultValue = "PENDING")
            ChangeStatus status,
            @PageableDefault(size = 20, sort = "effectiveDate", direction = Sort.Direction.ASC)
            Pageable pageable) {

        return ResponseEntity.ok(ApiResponse.ok(changeService.listByStatus(status, pageable)));
    }
}
