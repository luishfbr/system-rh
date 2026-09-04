package com.sgc.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Endereco residencial (1:1 com a pessoa, opcional).
 *
 * <p>Pertence a {@link Person} e nao ao vinculo: mudar de emprego nao muda o
 * endereco de quem mora ali. Numa readmissao, o endereco ja cadastrado continua
 * valendo.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "addresses")
public class Address extends BaseEntity {

    /**
     * Lado dono do relacionamento: e esta classe que possui a coluna FK.
     * Em {@link Person} o mapeamento e {@code mappedBy = "person"}.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "person_id", nullable = false)
    private Person person;

    @Column(name = "street", nullable = false, length = 150)
    private String street;

    @Column(name = "number", length = 20)
    private String number;

    @Column(name = "complement", length = 100)
    private String complement;

    @Column(name = "district", length = 100)
    private String district;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    /** Sigla da UF em maiusculas -- a CHECK constraint do banco exige 2 letras. */
    @Column(name = "state", nullable = false, length = 2)
    private String state;

    /** CEP com 8 digitos, sem traco. */
    @Column(name = "zip_code", nullable = false, length = 8)
    private String zipCode;
}
