package com.sgc.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** "Agencia/Unidade" onde o colaborador esta lotado. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "units")
public class Unit extends BaseEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
