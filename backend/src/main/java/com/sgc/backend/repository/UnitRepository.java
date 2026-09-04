package com.sgc.backend.repository;

import com.sgc.backend.domain.entity.Unit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Agencias/unidades (lista de apoio do cadastro profissional). */
public interface UnitRepository extends JpaRepository<Unit, Long> {

    List<Unit> findAllByActiveTrueOrderByName();
}
