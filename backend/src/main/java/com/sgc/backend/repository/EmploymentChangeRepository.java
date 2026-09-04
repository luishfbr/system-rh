package com.sgc.backend.repository;

import com.sgc.backend.domain.entity.EmploymentChange;
import com.sgc.backend.domain.enums.ChangeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

/**
 * Acesso as alteracoes de carreira (RF10).
 */
public interface EmploymentChangeRepository extends JpaRepository<EmploymentChange, Long> {

    /**
     * Alteracoes pendentes que ja venceram -- a consulta do job diario.
     *
     * <p>Ordenada por data de vigencia: quando um vinculo tem mais de uma
     * alteracao vencida (por exemplo, o job ficou dias sem rodar), elas precisam
     * ser aplicadas em ordem cronologica, senao o "valor anterior" registrado em
     * cada evento fica errado.
     *
     * <p>O join fetch evita uma consulta por alteracao ao ler o vinculo.
     */
    @Query("""
            select c from EmploymentChange c
              join fetch c.employment e
              join fetch e.person
            where c.status = :status
              and c.effectiveDate <= :reference
            order by c.effectiveDate asc, c.id asc
            """)
    List<EmploymentChange> findDue(ChangeStatus status, LocalDate reference);

    /** Historico de alteracoes de um vinculo, da mais recente para a mais antiga. */
    List<EmploymentChange> findByEmploymentIdOrderByEffectiveDateDescIdDesc(Long employmentId);

    List<EmploymentChange> findByEmploymentIdAndStatusOrderByEffectiveDateAsc(Long employmentId, ChangeStatus status);

    /**
     * Fila de alteracoes por situacao -- alimenta o relatorio "Alteracoes em
     * andamento" do RF07 quando consultada com PENDING.
     */
    @Query("""
            select c from EmploymentChange c
              join fetch c.employment e
              join fetch e.person
            where c.status = :status
            """)
    Page<EmploymentChange> findByStatusWithDetails(ChangeStatus status, Pageable pageable);

    boolean existsByEmploymentIdAndStatus(Long employmentId, ChangeStatus status);
}
