package com.sgc.backend.repository;

import com.sgc.backend.domain.entity.Person;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acesso as pessoas fisicas.
 *
 * <p>A busca por CPF e a operacao central da admissao (RF08): antes de criar um
 * vinculo o service pergunta "essa pessoa ja existe?". Se sim, reaproveita o
 * cadastro; se nao, cria. E isso que transforma readmissao em uma operacao
 * comum, sem endpoint especial.
 */
public interface PersonRepository extends JpaRepository<Person, Long> {

    Optional<Person> findByCpf(String cpf);

    boolean existsByCpf(String cpf);

    boolean existsByCpfAndIdNot(String cpf, Long id);
}
