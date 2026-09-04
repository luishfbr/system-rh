package com.sgc.backend.validation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes do algoritmo de CPF.
 *
 * <p>Nao sobem contexto do Spring nem tocam o banco: e logica pura, entao o
 * teste roda em milissegundos. Reservar Testcontainers para o que realmente
 * precisa de banco mantem a suite rapida.
 */
class CpfValidatorTest {

    private final CpfValidator validator = new CpfValidator();

    @ParameterizedTest(name = "CPF valido: {0}")
    @ValueSource(strings = {
            "11144477735",
            "111.444.777-35",   // com pontuacao
            "12345678909",
            "529.982.247-25"
    })
    void deveAceitarCpfValido(String cpf) {
        assertThat(validator.isValid(cpf, null)).isTrue();
    }

    @ParameterizedTest(name = "CPF invalido: {0}")
    @ValueSource(strings = {
            "11144477734",      // digito verificador errado
            "12345678900",      // digito verificador errado
            "111444777",        // curto demais
            "111444777355",     // longo demais
            "abcdefghijk"       // sem digitos
    })
    void deveRejeitarCpfInvalido(String cpf) {
        assertThat(validator.isValid(cpf, null)).isFalse();
    }

    @ParameterizedTest(name = "sequencia repetida: {0}")
    @ValueSource(strings = {"00000000000", "11111111111", "99999999999"})
    @DisplayName("Sequencias de digitos iguais passam no calculo, mas nao sao CPFs validos")
    void deveRejeitarSequenciasRepetidas(String cpf) {
        assertThat(validator.isValid(cpf, null)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("Campo vazio e responsabilidade do @NotBlank, nao deste validador")
    void deveAceitarVazio(String cpf) {
        assertThat(validator.isValid(cpf, null)).isTrue();
    }

    @Test
    void normalizeDeveRemoverPontuacao() {
        assertThat(CpfValidator.normalize("111.444.777-35")).isEqualTo("11144477735");
        assertThat(CpfValidator.normalize("11144477735")).isEqualTo("11144477735");
        assertThat(CpfValidator.normalize(null)).isNull();
    }
}
