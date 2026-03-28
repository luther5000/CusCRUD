package com.cuscrud.data.remote.api

import com.cuscrud.data.remote.dto.*
import retrofit2.Response
import retrofit2.http.*

interface CuscrudApiService {

    // region Auth

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("auth/register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @GET("auth/validate")
    suspend fun validateToken(): Response<UserDto>

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
    ): Response<InventoryDto>

    @PATCH("inventories/{inv_id}")
    suspend fun updateInventory(
        @Path("inv_id") invId: String,
        @Body request: UpdateInventoryRequest
    ): Response<InventoryDto>

    @DELETE("inventories/{inv_id}")
    suspend fun deleteInventory(
        @Path("inv_id") invId: String
    ): Response<Unit>

    // endregion

    // region Products

    @GET("inventories/{inv_id}/products")
    suspend fun getProducts(
        @Path("inv_id") invId: String,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<List<ProdutoResponseDto>>

    @POST("inventories/{inv_id}/products")
    suspend fun addProduct(
        @Path("inv_id") invId: String,
        @Body product: ProdutoRequestDto
    ): Response<ProdutoResponseDto>

    @PATCH("inventories/{inv_id}/products/{product_id}")
    suspend fun updateProduct(
        @Path("inv_id") invId: String,
        @Path("product_id") productId: Int,
        @Body product: ProdutoUpdateDto
    ): Response<ProdutoResponseDto>

    @DELETE("inventories/{inv_id}/products/{product_id}")
    suspend fun deleteProduct(
        @Path("inv_id") invId: String,
        @Path("product_id") productId: Int
    ): Response<Unit>

    // endregion
}
