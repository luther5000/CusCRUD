package com.cuscrud

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import dagger.hilt.android.testing.HiltTestApplication

/**
 * Um [AndroidJUnitRunner] personalizado usado para substituir a [Application] padrão
 * pela [HiltTestApplication].
 *
 * Isso é necessário para que os testes instrumentados (androidTest) que utilizam o Hilt
 * possam funcionar corretamente, fornecendo um ambiente onde a injeção de dependência
 * está disponível durante a execução dos testes no dispositivo.
 */
class CustomTestRunner : AndroidJUnitRunner() {

    override fun newApplication(
        cl: ClassLoader?,
        className: String?,
        context: Context?
    ): Application {
        return super.newApplication(cl, HiltTestApplication::class.java.name, context)
    }
}
