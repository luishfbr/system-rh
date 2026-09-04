package com.sgc.backend.domain.entity;

import com.sgc.backend.domain.enums.EmploymentStatus;
import com.sgc.backend.domain.enums.TerminationReason;
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
import java.time.LocalDate;

/**
 * Um periodo de contrato entre a empresa e uma pessoa (RF04, RF08).
 *
 * <p>E o que a API chama de "colaborador" em {@code /api/v1/employees}: quando
 * o gestor lista colaboradores, o que ele quer ver sao vinculos. Uma pessoa
 * readmitida aparece como um vinculo novo, com matricula e admissao proprias,
 * enquanto o periodo anterior fica preservado.
 *
 * <p>Cargo, salario e setor guardam o valor <b>corrente</b> deste periodo; o
 * historico das mudancas vive em {@link EmploymentEvent} (RF10/RF11).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "employments")
public class Employment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    /** Matricula deste periodo -- uma readmissao normalmente recebe outra. */
    @Column(name = "registration_number", length = 20)
    private String registrationNumber;

    @Column(name = "corporate_email", length = 255)
    private String corporateEmail;

    /** Data de admissao <b>deste</b> periodo. */
    @Column(name = "hire_date", nullable = false)
    private LocalDate hireDate;

    @Column(name = "termination_date")
    private LocalDate terminationDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "termination_reason", length = 30)
    private TerminationReason terminationReason;

    @Column(name = "termination_notes", columnDefinition = "text")
    private String terminationNotes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmploymentStatus status = EmploymentStatus.ACTIVE;

    @Column(name = "salary", precision = 12, scale = 2)
    private BigDecimal salary;

    /** Gratificacao. */
    @Column(name = "bonus", precision = 12, scale = 2)
    private BigDecimal bonus;

    /** Carga horaria semanal. */
    @Column(name = "weekly_hours", precision = 5, scale = 2)
    private BigDecimal weeklyHours;

    @Column(name = "grade", length = 10)
    private String grade;

    /** Faixa. */
    @Column(name = "band", length = 10)
    private String band;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_position_id")
    private JobPosition jobPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    /** Observacoes. */
    @Column(name = "notes", columnDefinition = "text")
    private String notes;

    // ------------------------------------------------------------------
    // Comportamento de dominio
    // ------------------------------------------------------------------

    /**
     * Vinculo ainda em aberto -- qualquer situacao que nao seja o desligamento.
     *
     * <p>E a definicao que o indice parcial
     * {@code uk_employments_one_open_per_person} usa no banco: uma pessoa pode
     * ter varios vinculos historicos, mas no maximo um em aberto (RN07).
     */
    public boolean isOpen() {
        return status != EmploymentStatus.TERMINATED;
    }

    /** RN05: desligado nao recebe alteracao de cargo/salario, apenas readmissao. */
    public boolean acceptsCareerChanges() {
        return status != null && status.acceptsCareerChanges();
    }

    /**
     * Encerra o vinculo (RF08).
     *
     * <p>Concentrado na entidade para que status, data e motivo mudem sempre
     * juntos -- as CHECK constraints do banco exigem os tres coerentes, e
     * espalhar essas atribuicoes pelo service abriria espaco para esquecer uma.
     */
    public void terminate(LocalDate date, TerminationReason reason, String notes) {
        this.status = EmploymentStatus.TERMINATED;
        this.terminationDate = date;
        this.terminationReason = reason;
        this.terminationNotes = notes;
    }
}
