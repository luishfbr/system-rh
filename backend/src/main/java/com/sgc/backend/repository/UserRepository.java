package com.sgc.backend.repository;

import com.sgc.backend.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Acesso aos usuarios do sistema.
 *
 * <p>Nao ha implementacao a escrever: o Spring Data cria a classe concreta em
 * tempo de execucao derivando a query do <b>nome</b> do metodo.
 * {@code findByEmail} vira {@code select u from User u where u.email = ?1}.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** O email e sempre gravado em minusculas, entao a comparacao exata basta (RN01). */
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Usado na edicao, para permitir que o usuario mantenha o proprio email. */
    boolean existsByEmailAndIdNot(String email, Long id);
}
