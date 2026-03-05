package com.cuscrud.testutil

import org.junit.Test
import org.junit.Assert.assertNotNull

class SharedTestConfigurationVerify {
    @Test
    fun verifySharedTestDataGeneratorIsAccessible() {
        // This class is in src/sharedTest/java
        val tipo = TestDataGenerator.createTipo()
        assertNotNull(tipo)
    }
}
