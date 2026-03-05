package com.cuscrud.data.mapper

import com.cuscrud.data.local.entities.ProdutoEntity
import com.cuscrud.data.local.entities.TipoEntity
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Date

class MappersTest {

    @Test
    fun tipoEntityToDomainMapping() {
        val image = byteArrayOf(0x01, 0x02)
        val entity = TipoEntity(id = 1L, nome = "Eletrônico", imagem = image)
        
        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.nome, domain.nome)
        assertArrayEquals(entity.imagem, domain.imagem)
    }

    @Test
    fun tipoDomainToEntityMapping() {
        val image = byteArrayOf(0x03, 0x04)
        val domain = Tipo(id = 2L, nome = "Alimentos", imagem = image)
        
        val entity = domain.toEntity()

        assertEquals(domain.id, entity.id)
        assertEquals(domain.nome, entity.nome)
        assertArrayEquals(domain.imagem, entity.imagem)
    }

    @Test
    fun produtoEntityToDomainMapping() {
        val date = Date()
        val tipoEntity = TipoEntity(1L, "Teste Category", byteArrayOf(0x05))
        val entity = ProdutoEntity(
            id = 1, 
            tipo = 1L, 
            marca = "Samsung",
            dataValidade = date.time, 
            unidade = 1,
            unidadeMedida = "un", 
            quantidade = 10
        )

        val domain = entity.toDomain(tipoEntity)

        assertEquals(entity.id, domain.id)
        assertEquals(entity.marca, domain.marca)
        assertEquals(date.time, domain.dataValidade.time)
        assertEquals(entity.unidade, domain.unidade)
        assertEquals(entity.unidadeMedida, domain.unidadeMedida)
        assertEquals(entity.quantidade, domain.quantidade)
        
        // Verify nested Tipo mapping
        assertEquals(tipoEntity.id, domain.tipo.id)
        assertEquals(tipoEntity.nome, domain.tipo.nome)
    }

    @Test
    fun produtoDomainToEntityMapping() {
        val date = Date()
        val tipoDomain = Tipo(2L, "Category Name", byteArrayOf(0x06))
        val domain = Produto(
            id = 5,
            tipo = tipoDomain,
            marca = "Apple",
            dataValidade = date,
            unidade = 2,
            unidadeMedida = "kg",
            quantidade = 20
        )

        val entity = domain.toEntity()

        assertEquals(domain.id, entity.id)
        assertEquals(domain.tipo.id, entity.tipo)
        assertEquals(domain.marca, entity.marca)
        assertEquals(domain.dataValidade.time, entity.dataValidade)
        assertEquals(domain.unidade, entity.unidade)
        assertEquals(domain.unidadeMedida, entity.unidadeMedida)
        assertEquals(domain.quantidade, entity.quantidade)
    }
}
