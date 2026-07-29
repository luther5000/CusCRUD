package br.com.cuscrudrest.auth.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailAddressValidatorTest {

    private final EmailAddressValidator validator = new EmailAddressValidator();

    /**
     * Verifica que o validador aceita um email com formato valido.
     * Entrada: endereco simples com usuario, arroba e dominio.
     * Esperado: retorno true.
     */
    @Test
    void shouldAcceptWellFormedEmail() {
        assertTrue(validator.isValid("joao@example.com"));
    }

    /**
     * Verifica que o validador rejeita um email sem formato valido.
     * Entrada: texto sem arroba e dominio adequados.
     * Esperado: retorno false.
     */
    @Test
    void shouldRejectMalformedEmail() {
        assertFalse(validator.isValid("joao.example.com"));
    }
}
