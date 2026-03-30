package com.cuscrud.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Modelos de dados representando um produto.
 * Essa classe é utilizada pelo banco de dados.
 */
@Entity(
    tableName = "produto",
    foreignKeys = [
        ForeignKey(
            entity = TipoEntity::class,
            parentColumns = ["id"],
            childColumns = ["tipo"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class ProdutoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val tipo: Long,
    val marca: String,
    val dataValidade: Long, // miliseconds
    val unidade: Long,
    val unidadeMedida: String,
    val quantidade: Long
)
