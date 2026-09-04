package com.sgc.backend.domain.entity;

import com.sgc.backend.domain.enums.ChangeReason;
import com.sgc.backend.domain.enums.ChangeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Uma alteracao de cargo, salario ou setor (RF10).
 *
 * <p>Representa uma <b>intencao</b>, nao um fato: uma promocao decidida em
 * setembro com vigencia em outubro existe aqui como PENDING durante todo esse
 * intervalo, e e essa fila que o relatorio "Alteracoes em andamento" (RF07)
 * enxerga. Quando a data chega, o job diario a aplica -- o vinculo passa a
 * refletir os novos valores e um {@link EmploymentEvent} imutavel entra na
 * timeline (RN06).
 *
 * <p>Campos {@code new*} nulos significam "este campo nao muda", e e o que
 * permite a mesma alteracao mexer so no salario, so no cargo, ou nos tres.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "employment_changes")
public class EmploymentChange extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employment_id", nullable = false)
    private Employment employment;

    /** Quando a alteracao passa (ou passou) a valer. Pode ser retroativa. */
    @Column(name = "effective_date", nullable = false)
    private LocalDate effectiveDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ChangeStatus status = ChangeStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "reason", nullable = false, length = 30)
    private ChangeReason reason;

    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    // ------------------------------------------------------------------
    // Valores solicitados
    // ------------------------------------------------------------------

    @Column(name = "new_salary", precision = 12, scale = 2)
    private BigDecimal newSalary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_job_position_id")
    private JobPosition newJobPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_department_id")
    private Department newDepartment;

    // ------------------------------------------------------------------
    // Valores anteriores -- preenchidos apenas na aplicacao
    // ------------------------------------------------------------------

    @Column(name = "previous_salary", precision = 12, scale = 2)
    private BigDecimal previousSalary;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_job_position_id")
    private JobPosition previousJobPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "previous_department_id")
    private Department previousDepartment;

    @Column(name = "applied_at")
    private Instant appliedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "text")
    private String cancellationReason;

    // ------------------------------------------------------------------
    // Comportamento de dominio
    // ------------------------------------------------------------------

    /** Ja venceu e ainda nao foi aplicada -- e o que o job diario procura. */
    public boolean isDue(LocalDate reference) {
        return status == ChangeStatus.PENDING && !effectiveDate.isAfter(reference);
    }

    public boolean changesSalary() {
        return newSalary != null;
    }

    public boolean changesJobPosition() {
        return newJobPosition != null;
    }

    public boolean changesDepartment() {
        return newDepartment != null;
    }

    /**
     * Congela o estado anterior do vinculo.
     *
     * <p>Chamado no <b>momento da aplicacao</b>, nunca na criacao: entre lancar a
     * alteracao e ela valer, os valores podem ter mudado por outra via. Registrar
     * o "antes" na criacao gravaria uma historia que nao aconteceu.
     */
    public void captureCurrentState(Employment current) {
        this.previousSalary = current.getSalary();
        this.previousJobPosition = current.getJobPosition();
        this.previousDepartment = current.getDepartment();
    }

    public void markApplied() {
        this.status = ChangeStatus.APPLIED;
        this.appliedAt = Instant.now();
    }

    public void cancel(String reason) {
        this.status = ChangeStatus.CANCELLED;
        this.cancelledAt = Instant.now();
        this.cancellationReason = reason;
    }
}
