package com.sgc.backend.repository;

import com.sgc.backend.domain.entity.JobPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Cargos/funcoes (lista de apoio do cadastro profissional). */
public interface JobPositionRepository extends JpaRepository<JobPosition, Long> {

    List<JobPosition> findAllByActiveTrueOrderByTitle();
}
