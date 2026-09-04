package com.sgc.backend.controller;

import com.sgc.backend.dto.response.ApiResponse;
import com.sgc.backend.dto.response.ReferenceResponse;
import com.sgc.backend.repository.DepartmentRepository;
import com.sgc.backend.repository.JobPositionRepository;
import com.sgc.backend.repository.UnitRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Listas de apoio usadas nos {@code select} do cadastro de colaborador.
 *
 * <p>Sao consultas de leitura simples, sem regra de negocio, entao vao direto
 * ao repository -- um service que apenas repassasse a chamada seria uma camada
 * sem proposito.
 */
@RestController
@RequestMapping("/api/v1/reference")
@PreAuthorize("hasAnyRole('ADMIN', 'HR_MANAGER')")
@Tag(name = "Listas de apoio", description = "Areas de atuacao, cargos e unidades")
public class ReferenceDataController {

    private final DepartmentRepository departmentRepository;
    private final JobPositionRepository jobPositionRepository;
    private final UnitRepository unitRepository;

    public ReferenceDataController(DepartmentRepository departmentRepository,
                                   JobPositionRepository jobPositionRepository,
                                   UnitRepository unitRepository) {
        this.departmentRepository = departmentRepository;
        this.jobPositionRepository = jobPositionRepository;
        this.unitRepository = unitRepository;
    }

    @GetMapping("/departments")
    @Operation(summary = "Areas de atuacao ativas")
    public ResponseEntity<ApiResponse<List<ReferenceResponse>>> departments() {
        List<ReferenceResponse> data = departmentRepository.findAllByActiveTrueOrderByName().stream()
                .map(d -> new ReferenceResponse(d.getId(), d.getName()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/job-positions")
    @Operation(summary = "Cargos ativos")
    public ResponseEntity<ApiResponse<List<ReferenceResponse>>> jobPositions() {
        List<ReferenceResponse> data = jobPositionRepository.findAllByActiveTrueOrderByTitle().stream()
                .map(p -> new ReferenceResponse(p.getId(), p.getTitle()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/units")
    @Operation(summary = "Unidades ativas")
    public ResponseEntity<ApiResponse<List<ReferenceResponse>>> units() {
        List<ReferenceResponse> data = unitRepository.findAllByActiveTrueOrderByName().stream()
                .map(u -> new ReferenceResponse(u.getId(), u.getName()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
