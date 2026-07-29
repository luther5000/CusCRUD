package com.cuscrud.testutil

import org.junit.Test
import org.junit.Assert.assertNotNull

/**
 * Classe de teste para verificar a configuração do source set compartilhado (sharedTest).
 * 
 * Este teste garante que as classes utilitárias localizadas em `src/sharedTest/java`
 * (como o [TestDataGenerator]) estão acessíveis para os testes unitários locais (JVM).
 * Isso é fundamental para evitar a duplicação de código de teste entre os testes
 * de unidade e os testes instrumentados.
 */
class SharedTestConfigurationVerify {

    /**
     * Verifica se o [TestDataGenerator] pode ser acessado e utilizado corretamente.
     */
    @Test
    fun verifySharedTestDataGeneratorIsAccessible() {
        // Esta classe TestDataGenerator reside em src/sharedTest/java
        val tipo = TestDataGenerator.createTipo()
        assertNotNull("O objeto Tipo não deve ser nulo se o gerador estiver acessível", tipo)
    }
}
