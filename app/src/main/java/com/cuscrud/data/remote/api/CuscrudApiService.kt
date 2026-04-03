package com.cuscrud.data.remote.api

import com.cuscrud.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Interface que define os endpoints da API Retrofit para o serviço CusCRUD.
 * Esta interface centraliza todas as chamadas de rede para o backend, abrangendo:
 * - **Auth**: Autenticação e validação de tokens de usuário.
 * - **Inventories**: Criação, listagem, atualização e exclusão de inventários.
 * - **Inventory Access (RBAC)**: Gerenciamento de permissões e usuários em inventários específicos.
 * - **Types**: Gerenciamento de categorias/tipos de produtos em um inventário.
 * - **Products**: Operações de CRUD para produtos vinculados a um inventário.
 *
 * Todas as funções são suspensas (suspend) para execução assíncrona utilizando Coroutines.
 */
interface CuscrudApiService {

    // region Auth

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("auth/validate")
    suspend fun validateToken(): Response<ValidateTokenResponse>

    // endregion

    // region Inventories

    @GET("inventories")
    suspend fun getInventories(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<InventoryListResponse>

    @POST("inventories")
    suspend fun createInventory(
        @Body request: CreateInventoryRequest
    ): Response<InventoryResponse>

    @PATCH("inventories/{inv_id}")
    suspend fun updateInventory(
        @Path("inv_id") invId: String,
        @Body request: UpdateInventoryRequest
    ): Response<InventoryResponse>

    @DELETE("inventories/{inv_id}")
    suspend fun deleteInventory(
        @Path("inv_id") invId: String
    ): Response<Unit>

    // endregion

    // region Inventory Access (RBAC)

    @GET("inventories/{inv_id}/users")
    suspend fun getInventoryUsers(
        @Path("inv_id") invId: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<UserAccessListResponse>

    @POST("inventories/{inv_id}/users")
    suspend fun addInventoryUser(
        @Path("inv_id") invId: String,
        @Body request: AddUserAccessRequest
    ): Response<UserAccessResponse>

    @PATCH("inventories/{inv_id}/users/{user_id}")
    suspend fun updateInventoryUserRole(
        @Path("inv_id") invId: String,
        @Path("user_id") userId: String,
        @Body request: UpdateUserAccessRequest
    ): Response<UserAccessResponse>

    @DELETE("inventories/{inv_id}/users/{user_id}")
    suspend fun removeInventoryUser(
        @Path("inv_id") invId: String,
        @Path("user_id") userId: String
    ): Response<Unit>

    // endregion

    // region Types

    /**
     * Lista os tipos de produtos de um inventário com paginação.
     */
    @GET("inventories/{inv_id}/types")
    suspend fun getTypes(
        @Path("inv_id") invId: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<TipoListResponse>

    /**
     * Busca os detalhes de um tipo específico, incluindo a imagem completa.
     */
    @GET("inventories/{inv_id}/types/{type_id}")
    suspend fun getTypeById(
        @Path("inv_id") invId: String,
        @Path("type_id") typeId: Long
    ): Response<TipoDto>

    /**
     * Cria um novo tipo de produto em um inventário.
     */
    @POST("inventories/{inv_id}/types")
    suspend fun createType(
        @Path("inv_id") invId: String,
        @Body request: CreateTipoRequest
    ): Response<TipoDto>

    /**
     * Atualiza parcialmente os dados de um tipo de produto.
     */
    @PATCH("inventories/{inv_id}/types/{type_id}")
    suspend fun updateType(
        @Path("inv_id") invId: String,
        @Path("type_id") typeId: Long,
        @Body request: UpdateTipoRequest
    ): Response<TipoDto>

    /**
     * Remove um tipo de produto do inventário.
     * Retorna 409 Conflict se houver produtos vinculados.
     */
    @DELETE("inventories/{inv_id}/types/{type_id}")
    suspend fun deleteType(
        @Path("inv_id") invId: String,
        @Path("type_id") typeId: Long
    ): Response<Unit>

    // endregion

    // region Products

    @GET("inventories/{inv_id}/products")
    suspend fun getProducts(
        @Path("inv_id") invId: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<ProdutoListResponse>

    @GET("inventories/{inv_id}/products/{product_id}")
    suspend fun getProductById(
        @Path("inv_id") invId: String,
        @Path("product_id") productId: Long
    ): Response<ProdutoResponseDto>

    @GET("inventories/{inv_id}/types/{type_id}/products")
    suspend fun getProductsByType(
        @Path("inv_id") invId: String,
        @Path("type_id") typeId: Long,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<ProdutoListResponse>

    @POST("inventories/{inv_id}/products")
    suspend fun addProduct(
        @Path("inv_id") invId: String,
        @Body product: ProdutoRequestDto
    ): Response<ProdutoResponseDto>

    @PATCH("inventories/{inv_id}/products/{product_id}")
    suspend fun updateProduct(
        @Path("inv_id") invId: String,
        @Path("product_id") productId: Long,
        @Body product: ProdutoUpdateDto
    ): Response<ProdutoResponseDto>

    @DELETE("inventories/{inv_id}/products/{product_id}")
    suspend fun deleteProduct(
        @Path("inv_id") invId: String,
        @Path("product_id") productId: Long
    ): Response<Unit>

    // endregion
}
