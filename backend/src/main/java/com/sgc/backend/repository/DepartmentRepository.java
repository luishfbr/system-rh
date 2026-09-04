package com.sgc.backend.repository;

import com.sgc.backend.domain.entity.Department;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/** Areas de atuacao (lista de apoio do cadastro profissional). */
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    List<Department> findAllByActiveTrueOrderByName();
}
