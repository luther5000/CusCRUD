package com.cuscrud.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "tipo",
    foreignKeys = [
        ForeignKey(
            entity = ProdutoEntity::class,
            parentColumns = ["id"],
            childColumns = ["id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class TipoEntity(
    @PrimaryKey val id: Int,
    val nome: String,
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB) val imagem: ByteArray
)