package com.sgc.backend.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.Objects;

/**
 * Base de todas as entidades: identidade, auditoria e controle de concorrencia.
 *
 * <p><b>{@code @MappedSuperclass}</b> nao cria tabela. O Hibernate simplesmente
 * copia estes campos para cada tabela filha -- por isso toda tabela do V1 repete
 * as colunas de auditoria.
 *
 * <p><b>Auditoria:</b> {@code AuditingEntityListener} preenche os quatro campos
 * automaticamente no insert/update. Quem informa o usuario corrente e o
 * {@code AuditorAware} declarado em {@code JpaAuditingConfig} -- sem ele, os
 * campos {@code createdBy}/{@code updatedBy} ficariam nulos (RN03).
 *
 * <p><b>{@code @Version}:</b> lock otimista. Se dois gestores editarem o mesmo
 * colaborador simultaneamente, o segundo update falha com
 * {@code OptimisticLockingFailureException} em vez de sobrescrever silenciosamente
 * a alteracao do primeiro.
 */
@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * IDENTITY delega a geracao ao {@code GENERATED ALWAYS AS IDENTITY} do
     * Postgres, definido na migration.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    /**
     * Igualdade por identidade persistida.
     *
     * <p>Duas entidades so sao iguais se ambas ja possuem {@code id} e ele
     * coincide. Entidades novas (id nulo) nunca sao iguais entre si -- caso
     * contrario, duas instancias recem-criadas colidiriam dentro de um
     * {@code Set}.
     *
     * <p>O tratamento de {@code HibernateProxy} e necessario porque um
     * relacionamento LAZY devolve um proxy cuja classe e uma subclasse gerada,
     * e nao a entidade em si.
     */
    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> thisClass = effectiveClass(this);
        Class<?> otherClass = effectiveClass(o);
        if (!thisClass.equals(otherClass)) {
            return false;
        }
        BaseEntity other = (BaseEntity) o;
        return id != null && Objects.equals(id, other.getId());
    }

    /**
     * Constante por classe, de proposito: usar o {@code id} no hashCode quebraria
     * o contrato quando uma entidade transiente (id nulo) e adicionada a um
     * {@code HashSet} e depois persistida, ganhando id.
     */
    @Override
    public final int hashCode() {
        return effectiveClass(this).hashCode();
    }

    private static Class<?> effectiveClass(Object o) {
        return o instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
    }
}
