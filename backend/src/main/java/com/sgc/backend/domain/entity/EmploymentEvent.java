package com.sgc.backend.domain.entity;

import com.sgc.backend.domain.enums.EmploymentEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Um fato na linha do tempo do vinculo (RF11).
 *
 * <p><b>Nao estende {@code BaseEntity} de proposito.</b> Um evento e um registro
 * imutavel: nunca e editado, entao {@code updated_at} e {@code @Version} nao
 * fariam sentido. Corrigir a historia significa registrar um evento novo, nunca
 * alterar um existente -- e e isso que da valor a uma trilha de auditoria.
 *
 * <p>{@code previousValue} e {@code newValue} sao JSONB porque cada tipo de
 * evento muda campos diferentes: um SALARY_CHANGE guarda {@code {"salary": 7500}},
 * um POSITION_CHANGE guarda {@code {"jobPosition": "Analista"}}. Colunas fixas
 * viraria uma tabela larga e quase toda nula.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
// Necessario explicitamente: @CreatedBy/@CreatedDate so funcionam com este
// listener, que as demais entidades herdam de BaseEntity.
@EntityListeners(AuditingEntityListener.class)
@Table(name = "employment_events")
public class EmploymentEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employment_id", nullable = false)
    private Employment employment;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EmploymentEventType eventType;

    /**
     * Quando o fato passa a valer.
     *
     * <p>Diferente de {@code createdAt}: um desligamento pode ser lancado no
     * sistema depois da data em que de fato ocorreu. A timeline se ordena por
     * esta data; a auditoria usa a outra.
     */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    /**
     * {@code @JdbcTypeCode(SqlTypes.JSON)} instrui o Hibernate a tratar a coluna
     * como JSON, serializando o Map automaticamente.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "previous_value")
    private Map<String, Object> previousValue;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "new_value")
    private Map<String, Object> newValue;

    /**
     * Motivo controlado, quando o tipo de evento tem um.
     *
     * <p>E {@code String}, e nao um enum, porque o vocabulario valido <b>depende
     * do tipo do evento</b>: um TERMINATION usa {@code TerminationReason}
     * (RESIGNATION, RETIREMENT...), um SALARY_CHANGE usa {@code ChangeReason}
     * (PROMOTION, MERIT_INCREASE...). Nao existe um enum unico que descreva a
     * coluna -- e essa e a natureza de uma tabela polimorfica.
     *
     * <p>A tipagem forte fica na borda da API, onde cada requisicao declara o
     * enum que lhe cabe; a CHECK constraint do banco garante a uniao dos dois
     * conjuntos.
     */
    @Column(name = "reason", length = 30)
    private String reason;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    /** Quem executou a acao; preenchido pelo JPA Auditing a partir do SecurityContext. */
    @CreatedBy
    @Column(name = "performed_by", updatable = false)
    private String performedBy;

    /**
     * A alteracao programada que originou este evento (RF10), quando houver.
     *
     * <p>Permite ir da linha do tempo ate o lancamento que a produziu -- quem
     * solicitou, quando, com que observacao. Nulo nos eventos que nascem de uma
     * acao direta, como admissao e desligamento.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employment_change_id")
    private EmploymentChange employmentChange;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
