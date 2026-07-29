package com.cuscrud.data.mapper

import android.util.Base64
import com.cuscrud.data.local.entities.ProdutoEntity
import com.cuscrud.data.local.entities.TipoEntity
import com.cuscrud.data.remote.dto.*
import com.cuscrud.domain.model.Produto
import com.cuscrud.domain.model.Tipo
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*

/**
 * Testes unitários para as funções de mapeamento (Mappers).
 *
 * Estes testes garantem que a conversão entre as entidades do banco de dados (Room Entities),
 * os modelos de domínio (Domain Models) e os DTOs da API ocorra sem perda de dados ou erros de lógica,
 * permitindo que as camadas de UI e Negócio permaneçam isoladas da persistência e da comunicação remota.
 */
class MappersTest {

    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("America/Recife")
    }

    @Before
    fun setup() {
        mockkStatic(Base64::class)
        // Mock do Base64.decode do Android usando o java.util.Base64 padrão do Java para o ambiente de testes unitários.
        every { Base64.decode(any<String>(), any()) } answers {
            val input = it.invocation.args[0] as String
            java.util.Base64.getDecoder().decode(input)
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(Base64::class)
    }

    // --- TESTES DE ENTIDADES (ROOM) ---

    /**
     * Valida a conversão de [TipoEntity] (Banco) para [Tipo] (Domínio).
     */
    @Test
    fun tipoEntityToDomainMapping() {
        val image = byteArrayOf(0x01, 0x02)
        val entity = TipoEntity(id = 1L, nome = "Eletrônico", imagem = image)
        
        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.nome, domain.nome)
        assertArrayEquals(entity.imagem, domain.imagem)
    }

    /**
     * Valida a conversão de [Tipo] (Domínio) para [TipoEntity] (Banco).
     */
    @Test
    fun tipoDomainToEntityMapping() {
        val image = byteArrayOf(0x03, 0x04)
        val domain = Tipo(id = 2L, nome = "Alimentos", imagem = image)
        
        val entity = domain.toEntity()

        assertEquals(domain.id, entity.id)
        assertEquals(domain.nome, entity.nome)
        assertArrayEquals(domain.imagem, entity.imagem)
    }

    /**
     * Valida a conversão de [ProdutoEntity] (Banco) para [Produto] (Domínio).
     */
    @Test
    fun produtoEntityToDomainMapping() {
        val date = Date()
        val tipoEntity = TipoEntity(1L, "Teste Category", byteArrayOf(0x05))
        val entity = ProdutoEntity(
            id = 1L, 
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
        assertEquals(tipoEntity.id, domain.tipo.id)
    }

    /**
     * Valida a conversão de [Produto] (Domínio) para [ProdutoEntity] (Banco).
     */
    @Test
    fun produtoDomainToEntityMapping() {
        val date = Date()
        val tipoDomain = Tipo(2L, "Category Name", byteArrayOf(0x06))
        val domain = Produto(
            id = 5L,
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

    // --- TESTES DE DTOS (API REMOTE) ---

    /**
     * Valida o mapeamento de [TipoDto] para [Tipo], incluindo a decodificação da imagem Base64 com prefixo.
     */
    @Test
    fun tipoDtoToDomainMapping() {
        val base64Image = "SGVsbG8=" // "Hello"
        val dto = TipoDto(
            typeId = 10L,
            nome = "Categoria DTO",
            imagem = "data:image/png;base64,$base64Image"
        )

        val domain = dto.toDomain()

        assertEquals(dto.typeId, domain.id)
        assertEquals(dto.nome, domain.nome)
        assertArrayEquals("Hello".toByteArray(), domain.imagem)
    }

    /**
     * Valida o mapeamento de [TipoResponseDto] para [Tipo].
     */
    @Test
    fun tipoResponseDtoToDomainMapping() {
        val base64Image = "V29ybGQ=" // "World"
        val dto = TipoResponseDto(
            id = 20L,
            nome = "Categoria Response",
            imagem = base64Image
        )

        val domain = dto.toDomain()

        assertEquals(dto.id, domain.id)
        assertEquals(dto.nome, domain.nome)
        assertArrayEquals("World".toByteArray(), domain.imagem)
    }

    /**
     * Valida o mapeamento de [ProdutoResponseDto] para [Produto], garantindo o parse da data ISO 8601.
     */
    @Test
    fun produtoResponseDtoToDomainMapping() {
        val dateStr = "2023-12-25T10:00:00.000-0300"
        val tipoResponse = TipoResponseDto(1L, "Tipo", "SGVsbG8=")
        val dto = ProdutoResponseDto(
            id = 1L,
            type = tipoResponse,
            marca = "Marca",
            dataValidade = dateStr,
            unidade = 1,
            unidadeMedida = "un",
            quantidade = 5
        )

        val domain = dto.toDomain()

        assertEquals(dto.id, domain.id)
        assertEquals(dto.marca, domain.marca)
        val expectedDate = isoFormat.parse(dateStr)
        assertEquals(expectedDate?.time, domain.dataValidade.time)
    }

    /**
     * Valida o mapeamento de [Produto] para [ProdutoRequestDto], focando na formatação da data.
     */
    @Test
    fun produtoToRequestDtoMapping() {
        val date = Date()
        val tipo = Tipo(1L, "Tipo", byteArrayOf())
        val domain = Produto(1L, tipo, "Marca", date, 1, "un", 5)

        val dto = domain.toRequestDto()

        assertEquals(domain.tipo.id, dto.typeId)
        assertEquals(domain.marca, dto.marca)
        assertEquals(isoFormat.format(date), dto.dataValidade)
    }

    /**
     * Valida o mapeamento de [Produto] para [ProdutoUpdateDto].
     */
    @Test
    fun produtoToUpdateDtoMapping() {
        val date = Date()
        val tipo = Tipo(2L, "Tipo", byteArrayOf())
        val domain = Produto(2L, tipo, "Outra Marca", date, 2, "kg", 10)

        val dto = domain.toUpdateDto()

        assertEquals(domain.tipo.id, dto.typeId)
        assertEquals(domain.marca, dto.marca)
        assertEquals(isoFormat.format(date), dto.dataValidade)
    }
}
