package com.sgc.backend.repository;

import com.sgc.backend.domain.entity.Employment;
import com.sgc.backend.domain.enums.EmploymentStatus;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Filtros combinaveis para a listagem de vinculos.
 *
 * <p>Cada metodo devolve {@code null} quando o filtro nao foi informado. O
 * {@code Specification.allOf} do Spring Data <b>rejeita</b> nulos, entao a
 * combinacao passa por {@link #combine(Specification[])}, que descarta os
 * filtros ausentes antes de junta-los.
 *
 * <p>Note que nome e CPF agora ficam em {@code Person}: os filtros atravessam o
 * relacionamento com {@code root.get("person").get(...)}.
 */
public final class EmploymentSpecifications {

    private EmploymentSpecifications() {
    }

    /** Busca parcial pelo nome da pessoa, sem diferenciar maiusculas. */
    public static Specification<Employment> nameContains(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        String pattern = "%" + name.trim().toLowerCase(Locale.ROOT) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("person").get("fullName")), pattern);
    }

    public static Specification<Employment> hasCpf(String cpf) {
        if (cpf == null || cpf.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("person").get("cpf"), cpf);
    }

    public static Specification<Employment> hasStatus(EmploymentStatus status) {
        if (status == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Employment> inDepartment(Long departmentId) {
        if (departmentId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("department").get("id"), departmentId);
    }

    public static Specification<Employment> inUnit(Long unitId) {
        if (unitId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("unit").get("id"), unitId);
    }

    /** Admitidos a partir da data -- base do relatorio de admissoes (RF07). */
    public static Specification<Employment> hiredFrom(LocalDate from) {
        if (from == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("hireDate"), from);
    }

    public static Specification<Employment> hiredUntil(LocalDate until) {
        if (until == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("hireDate"), until);
    }

    /**
     * Apenas o vinculo corrente de cada pessoa (RF08).
     *
     * <p>Sem este filtro, a listagem padrao mostraria tambem os periodos
     * antigos de quem foi readmitido -- a mesma pessoa apareceria duas vezes.
     */
    public static Specification<Employment> onlyOpen(Boolean onlyOpen) {
        if (!Boolean.TRUE.equals(onlyOpen)) {
            return null;
        }
        return (root, query, cb) -> cb.notEqual(root.get("status"), EmploymentStatus.TERMINATED);
    }

    /**
     * Junta os filtros informados, ignorando os que vieram nulos.
     *
     * <p>Necessario porque {@code Specification.allOf} lanca
     * {@code IllegalArgumentException} ao encontrar um elemento nulo -- e, com
     * filtros opcionais, o normal e que a maioria venha nula.
     */
    @SafeVarargs
    public static Specification<Employment> combine(Specification<Employment>... specifications) {
        List<Specification<Employment>> present = Arrays.stream(specifications)
                .filter(Objects::nonNull)
                .toList();

        return present.isEmpty()
                ? (root, query, cb) -> cb.conjunction()
                : Specification.allOf(present);
    }

    /**
     * Carrega pessoa, area, cargo e unidade na mesma consulta da listagem.
     *
     * <p>Evita o problema N+1: sem isto, montar o resumo de 20 vinculos
     * dispararia uma consulta extra por relacionamento de cada linha.
     *
     * <p>O teste sobre {@code getResultType()} pula o fetch na consulta de
     * contagem que o Spring Data dispara para paginar, onde ele seria inutil.
     */
    public static Specification<Employment> fetchReferences() {
        return (root, query, cb) -> {
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("person", JoinType.INNER);
                root.fetch("department", JoinType.LEFT);
                root.fetch("jobPosition", JoinType.LEFT);
                root.fetch("unit", JoinType.LEFT);
            }
            return cb.conjunction();
        };
    }
}
