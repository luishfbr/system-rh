package com.sgc.backend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Valida um CPF brasileiro, conferindo os digitos verificadores.
 *
 * <p>Anotacao propria de Bean Validation: a marcacao ({@code @Cpf}) fica separada
 * da regra ({@link CpfValidator}), e o Hibernate Validator liga as duas atraves
 * do atributo {@code validatedBy}. O mesmo mecanismo por tras de
 * {@code @NotBlank} e {@code @Email}.
 *
 * <p>Aceita o valor com ou sem pontuacao -- a normalizacao para 11 digitos
 * acontece no service, antes de gravar.
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CpfValidator.class)
public @interface Cpf {

    String message() default "CPF invalido";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
