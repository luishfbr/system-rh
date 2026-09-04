package com.sgc.backend.repository;

import com.sgc.backend.domain.entity.Employment;
import com.sgc.backend.domain.enums.EmploymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Acesso aos vinculos empregaticios.
 *
 * <p>{@code JpaSpecificationExecutor} habilita os filtros combinaveis montados
 * em {@link EmploymentSpecifications}.
 */
public interface EmploymentRepository extends JpaRepository<Employment, Long>, JpaSpecificationExecutor<Employment> {

    /**
     * O vinculo em aberto de uma pessoa, se houver.
     *
     * <p>RN07: o indice parcial do banco garante no maximo um, entao o
     * {@code Optional} nunca esconde um segundo resultado.
     */
    Optional<Employment> findByPersonIdAndStatusNot(Long personId, EmploymentStatus status);

    boolean existsByPersonIdAndStatusNot(Long personId, EmploymentStatus status);

    /** Todos os periodos de uma pessoa, do mais recente para o mais antigo. */
    List<Employment> findByPersonIdOrderByHireDateDescIdDesc(Long personId);

    List<Employment> findByPersonCpfOrderByHireDateDescIdDesc(String cpf);

    boolean existsByRegistrationNumber(String registrationNumber);

    boolean existsByRegistrationNumberAndIdNot(String registrationNumber, Long id);

    boolean existsByCorporateEmail(String corporateEmail);

    boolean existsByCorporateEmailAndIdNot(String corporateEmail, Long id);

    /**
     * Carrega o vinculo com pessoa, endereco e listas de apoio numa consulta so.
     *
     * <p>A tela de detalhe precisa de tudo isso; sem o {@code join fetch}, cada
     * relacionamento LAZY dispararia uma consulta extra ao montar a resposta.
     */
    @Query("""
            select e from Employment e
              join fetch e.person p
              left join fetch p.address
              left join fetch e.department
              left join fetch e.jobPosition
              left join fetch e.unit
            where e.id = :id
            """)
    Optional<Employment> findByIdWithDetails(Long id);
}
