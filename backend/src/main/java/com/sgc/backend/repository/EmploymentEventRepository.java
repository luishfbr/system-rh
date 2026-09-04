package com.sgc.backend.repository;

import com.sgc.backend.domain.entity.EmploymentEvent;
import com.sgc.backend.domain.enums.EmploymentEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * Acesso a linha do tempo (RF11).
 *
 * <p>Tabela append-only: o sistema insere e le, nunca atualiza. Por isso nao ha
 * aqui nenhum metodo de escrita alem do {@code save} herdado.
 */
public interface EmploymentEventRepository extends JpaRepository<EmploymentEvent, Long> {

    /** Timeline de um vinculo, do fato mais recente para o mais antigo. */
    List<EmploymentEvent> findByEmploymentIdOrderByEffectiveDateDescIdDesc(Long employmentId);

    /** Mesma timeline, restrita a certos tipos (ex.: esconder PROFILE_UPDATE). */
    List<EmploymentEvent> findByEmploymentIdAndEventTypeInOrderByEffectiveDateDescIdDesc(
            Long employmentId, Collection<EmploymentEventType> eventTypes);

    /** Timeline consolidada da pessoa, atravessando todos os seus vinculos. */
    List<EmploymentEvent> findByEmploymentPersonIdOrderByEffectiveDateDescIdDesc(Long personId);
}
