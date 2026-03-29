package com.cuscrud.domain.model

/**
 * Representa os papéis de acesso de um usuário em um inventário.
 * Seguindo a convenção da Seção 3 da API.
 */
enum class Role(val value: Int) {
    /** Leitura, escrita e administração */
    OWNER(0),
    /** Leitura e escrita sem administração */
    EDITOR(1),
    /** Somente leitura */
    READER(2);

    companion object {
        fun fromInt(value: Int): Role? = entries.find { it.value == value }
    }
}
