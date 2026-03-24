package br.com.cuscrudrest.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher passwordHasher = new PasswordHasher(new BCryptPasswordEncoder());

    /**
     * Verifica que o hasher produz um valor diferente da senha em texto puro.
     * Entrada: senha valida em texto puro.
     * Esperado: hash nao vazio e diferente do valor original.
     */
    @Test
    void shouldHashPasswordWithoutKeepingPlaintext() {
        String rawPassword = "senhaforte456";

        String encodedPassword = passwordHasher.hash(rawPassword);

        assertNotEquals(rawPassword, encodedPassword);
        assertFalse(encodedPassword.isBlank());
    }

    /**
     * Verifica que o hasher confirma uma senha correta contra o hash persistido.
     * Entrada: senha original e hash derivado dela.
     * Esperado: retorno true na comparacao.
     */
    @Test
    void shouldMatchRawPasswordAgainstStoredHash() {
        String rawPassword = "senhaforte456";
        String encodedPassword = passwordHasher.hash(rawPassword);

        assertTrue(passwordHasher.matches(rawPassword, encodedPassword));
    }

    /**
     * Verifica que o hasher rejeita uma senha incorreta contra o hash persistido.
     * Entrada: hash de uma senha e tentativa com outro valor.
     * Esperado: retorno false na comparacao.
     */
    @Test
    void shouldRejectDifferentPasswordAgainstStoredHash() {
        String encodedPassword = passwordHasher.hash("senhaforte456");

        assertFalse(passwordHasher.matches("outrasenha789", encodedPassword));
    }
}
