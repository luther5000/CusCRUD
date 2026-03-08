package com.cuscrud.domain.util

/**
 * Wrapper para retornos da camada de domínio, garantindo o tratamento explícito de erros.
 */
sealed class Result<out T> {
    data class Success<out T>(val data: T) : Result<T>()
    data class Error(val exception: Throwable) : Result<Nothing>()
    object Loading : Result<Nothing>()
}