package com.cuscrud.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Modelos de dados representando um tipo de produto.
 * Essa classe é utilizada pelo banco de dados.
 */
@Entity("tipo")
data class TipoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val nome: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val imagem: ByteArray
)