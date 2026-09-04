package com.sgc.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** "Cargo/Funcao" exercido pelo colaborador. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "job_positions")
public class JobPosition extends BaseEntity {

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
