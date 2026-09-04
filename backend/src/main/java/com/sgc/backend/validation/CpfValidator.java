package com.sgc.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Implementa o calculo dos digitos verificadores do CPF.
 *
 * <p>Os nove primeiros digitos sao os identificadores; o decimo e o decimo
 * primeiro sao calculados a partir deles. Cada digito verificador e o resto da
 * divisao por 11 de uma soma ponderada -- restos 0 e 1 resultam em digito 0.
 */
public class CpfValidator implements ConstraintValidator<Cpf, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        // Campo vazio nao e problema deste validador: quem exige preenchimento
        // e o @NotBlank. Combinar as duas responsabilidades tornaria impossivel
        // ter um CPF opcional.
        if (value == null || value.isBlank()) {
            return true;
        }

        String digits = normalize(value);

        if (digits.length() != 11) {
            return false;
        }

        // Sequencias como 111.111.111-11 passam no calculo dos digitos, mas nao
        // sao CPFs validos -- por isso a checagem explicita.
        if (digits.chars().distinct().count() == 1) {
            return false;
        }

        return checkDigit(digits, 9) == Character.getNumericValue(digits.charAt(9))
                && checkDigit(digits, 10) == Character.getNumericValue(digits.charAt(10));
    }

    /**
     * Remove pontuacao, deixando apenas digitos.
     *
     * <p>Publico e estatico porque o service reutiliza a mesma normalizacao antes
     * de gravar -- garantindo que a unique constraint do CPF (RN02) nao seja
     * driblada por diferencas de formatacao.
     */
    public static String normalize(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    /**
     * @param position indice do digito verificador a calcular (9 ou 10)
     */
    private static int checkDigit(String digits, int position) {
        int sum = 0;
        int weight = position + 1;

        for (int i = 0; i < position; i++) {
            sum += Character.getNumericValue(digits.charAt(i)) * weight--;
        }

        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }
}
