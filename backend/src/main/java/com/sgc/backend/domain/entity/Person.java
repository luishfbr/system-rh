package com.sgc.backend.domain.entity;

import com.sgc.backend.domain.enums.Gender;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A pessoa fisica.
 *
 * <p>Separada de {@link Employment} porque uma mesma pessoa pode ter mais de um
 * periodo de contrato com a empresa (admissao, desligamento, readmissao). Os
 * dados pessoais existem uma vez so; o que se repete a cada periodo -- matricula,
 * cargo, salario, datas -- vive no vinculo.
 *
 * <p>A RN02 ("cada colaborador deve possuir um CPF unico") se aplica <b>aqui</b>:
 * uma pessoa existe uma unica vez no sistema, por mais vinculos que tenha tido.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "persons")
public class Person extends BaseEntity {

    /** Somente digitos (11 caracteres). A normalizacao acontece no service. */
    @Column(name = "cpf", nullable = false, length = 11)
    private String cpf;

    @Column(name = "rg", length = 20)
    private String rg;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(name = "personal_email", length = 255)
    private String personalEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 30)
    private Gender gender;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "dependents_count", nullable = false)
    private Integer dependentsCount = 0;

    /** Categorias da CNH concatenadas, ex.: "AB". */
    @Column(name = "driver_license_categories", length = 10)
    private String driverLicenseCategories;

    /** O endereco e da pessoa, nao do contrato -- por isso mora aqui. */
    @OneToOne(mappedBy = "person", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private Address address;

    /**
     * Todos os periodos de contrato desta pessoa, do mais recente para o mais antigo.
     *
     * <p>Sem {@code cascade = ALL}: apagar a pessoa apaga os vinculos por conta da
     * FK {@code ON DELETE CASCADE} no banco, e nao ha caso de uso para salvar um
     * vinculo novo atraves da pessoa -- o service o faz diretamente.
     */
    @OneToMany(mappedBy = "person", fetch = FetchType.LAZY)
    @OrderBy("hireDate DESC, id DESC")
    private List<Employment> employments = new ArrayList<>();

    // ------------------------------------------------------------------

    /** Mantem os dois lados do relacionamento coerentes (a FK vive em Address). */
    public void setAddress(Address address) {
        if (address == null) {
            if (this.address != null) {
                this.address.setPerson(null);
            }
        } else {
            address.setPerson(this);
        }
        this.address = address;
    }

    /**
     * O vinculo em aberto, se houver.
     *
     * <p>RN07 garantida pelo indice parcial {@code uk_employments_one_open_per_person}:
     * no maximo um vinculo nao-desligado por pessoa. Por isso o retorno e um
     * {@code Optional} de um unico elemento, e nao uma lista.
     */
    public Optional<Employment> currentEmployment() {
        return employments.stream()
                .filter(Employment::isOpen)
                .findFirst();
    }
}
