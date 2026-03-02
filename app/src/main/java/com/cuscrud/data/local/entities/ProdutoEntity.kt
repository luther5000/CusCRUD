package com.cuscrud.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "produto")
data class ProdutoEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val tipo: String,
    val marca: String,
    val dataValidade: Long, // miliseconds
    val unidade: Long,
    val unidadeMedida: String,
    val quantidade: Long
)