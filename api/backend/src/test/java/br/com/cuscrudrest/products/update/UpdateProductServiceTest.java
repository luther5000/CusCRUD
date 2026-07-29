package br.com.cuscrudrest.products.update;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.ForbiddenException;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.common.error.ValidationException;
import br.com.cuscrudrest.inventories.InventoryAccessContext;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.products.ProductRepository;
import br.com.cuscrudrest.products.ProductSummary;
import br.com.cuscrudrest.types.TypeDetails;
import br.com.cuscrudrest.types.TypeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdateProductServiceTest {

    private final InventoryAccessService inventoryAccessService = mock(InventoryAccessService.class);
    private final ProductRepository productRepository = mock(ProductRepository.class);
    private final TypeRepository typeRepository = mock(TypeRepository.class);
    private final UpdateProductService updateProductService = new UpdateProductService(
            inventoryAccessService,
            productRepository,
            typeRepository
    );
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Verifica que o servico atualiza `type_id`, `quantidade` e preserva campos nao enviados.
     * Entrada: produto existente, usuario com escrita e payload parcial contendo novo `type_id` e nova `quantidade`.
     * Esperado: resposta com os campos atualizados e os demais preservados.
     */
    @Test
    void shouldUpdateSelectedFieldsWhenPatchIsValid() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();
        ProductSummary existingProduct = new ProductSummary(
                301L,
                1L,
                "Acme",
                OffsetDateTime.parse("2026-12-31T00:00:00-03:00"),
                1L,
                "un",
                50L,
                inventoryId
        );
        ProductSummary updatedProduct = new ProductSummary(
                301L,
                2L,
                "Acme",
                OffsetDateTime.parse("2026-12-31T00:00:00-03:00"),
                1L,
                "un",
                75L,
                inventoryId
        );

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenReturn(new InventoryAccessContext(inventoryId, "Loja", 1));
        when(productRepository.findProductById(inventoryId, 301L)).thenReturn(Optional.of(existingProduct));
        when(typeRepository.findTypeById(inventoryId, 2L))
                .thenReturn(Optional.of(new TypeDetails(2L, "Congelados", null, inventoryId)));
        when(productRepository.updateProduct(
                inventoryId,
                301L,
                2L,
                "Acme",
                existingProduct.dataValidade(),
                1L,
                "un",
                75L
        )).thenReturn(updatedProduct);

        UpdateProductResponse response = updateProductService.updateProduct(
                authenticatedUser,
                inventoryId,
                301L,
                new UpdateProductRequest(
                        objectMapper.getNodeFactory().numberNode(2L),
                        null,
                        null,
                        null,
                        null,
                        objectMapper.getNodeFactory().numberNode(75L)
                )
        );

        assertEquals(301L, response.productId());
        assertEquals(2L, response.typeId());
        assertEquals("Acme", response.marca());
        assertEquals(75L, response.quantidade());
    }

    /**
     * Verifica que o servico permite limpar campos opcionais anulaveis com `null` explicito.
     * Entrada: produto existente com campos opcionais preenchidos e payload com `marca`, `dataValidade`, `unidade` e `unidadeMedida` nulos.
     * Esperado: resposta com esses campos removidos e `quantidade` preservada.
     */
    @Test
    void shouldClearNullableFieldsWhenPatchUsesExplicitNull() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();
        ProductSummary existingProduct = new ProductSummary(
                301L,
                1L,
                "Acme",
                OffsetDateTime.parse("2026-12-31T00:00:00-03:00"),
                1L,
                "un",
                50L,
                inventoryId
        );
        ProductSummary updatedProduct = new ProductSummary(
                301L,
                1L,
                null,
                null,
                null,
                null,
                50L,
                inventoryId
        );

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenReturn(new InventoryAccessContext(inventoryId, "Loja", 0));
        when(productRepository.findProductById(inventoryId, 301L)).thenReturn(Optional.of(existingProduct));
        when(productRepository.updateProduct(inventoryId, 301L, 1L, null, null, null, null, 50L))
                .thenReturn(updatedProduct);

        UpdateProductResponse response = updateProductService.updateProduct(
                authenticatedUser,
                inventoryId,
                301L,
                new UpdateProductRequest(
                        null,
                        objectMapper.getNodeFactory().nullNode(),
                        objectMapper.getNodeFactory().nullNode(),
                        objectMapper.getNodeFactory().nullNode(),
                        objectMapper.getNodeFactory().nullNode(),
                        null
                )
        );

        assertNull(response.marca());
        assertNull(response.dataValidade());
        assertNull(response.unidade());
        assertNull(response.unidadeMedida());
        assertEquals(50L, response.quantidade());
    }

    /**
     * Verifica que o servico rejeita patch sem nenhum campo reconhecido.
     * Entrada: payload vazio.
     * Esperado: ValidationException associada ao campo `payload`.
     */
    @Test
    void shouldRejectEmptyPatch() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenReturn(new InventoryAccessContext(inventoryId, "Loja", 1));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> updateProductService.updateProduct(
                        authenticatedUser,
                        inventoryId,
                        301L,
                        new UpdateProductRequest(null, null, null, null, null, null)
                )
        );

        assertEquals("payload", exception.getCampo());
    }

    /**
     * Verifica que o servico rejeita `type_id` inexistente no inventario.
     * Entrada: produto existente e payload com `type_id` sem registro correspondente no inventario.
     * Esperado: NotFoundException associada ao campo `type_id`.
     */
    @Test
    void shouldRejectWhenTypeDoesNotExistInInventory() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();
        ProductSummary existingProduct = new ProductSummary(301L, 1L, "Acme", null, null, null, 50L, inventoryId);

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenReturn(new InventoryAccessContext(inventoryId, "Loja", 1));
        when(productRepository.findProductById(inventoryId, 301L)).thenReturn(Optional.of(existingProduct));
        when(typeRepository.findTypeById(inventoryId, 99L)).thenReturn(Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> updateProductService.updateProduct(
                        authenticatedUser,
                        inventoryId,
                        301L,
                        new UpdateProductRequest(
                                objectMapper.getNodeFactory().numberNode(99L),
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                )
        );

        assertEquals("type_id", exception.getCampo());
    }

    /**
     * Verifica que o servico rejeita quantidade negativa no patch.
     * Entrada: produto existente e payload com `quantidade < 0`.
     * Esperado: ValidationException associada ao campo `quantidade`.
     */
    @Test
    void shouldRejectNegativeQuantidade() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();
        ProductSummary existingProduct = new ProductSummary(301L, 1L, "Acme", null, null, null, 50L, inventoryId);

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenReturn(new InventoryAccessContext(inventoryId, "Loja", 1));
        when(productRepository.findProductById(inventoryId, 301L)).thenReturn(Optional.of(existingProduct));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> updateProductService.updateProduct(
                        authenticatedUser,
                        inventoryId,
                        301L,
                        new UpdateProductRequest(
                                null,
                                null,
                                null,
                                null,
                                null,
                                objectMapper.getNodeFactory().numberNode(-1L)
                        )
                )
        );

        assertEquals("quantidade", exception.getCampo());
    }

    /**
     * Verifica que o servico rejeita quantidade acima do teto permitido no patch.
     * Entrada: produto existente e payload com `quantidade > 999999999999999999`.
     * Esperado: ValidationException associada ao campo `quantidade`.
     */
    @Test
    void shouldRejectQuantidadeAboveMaximum() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();
        ProductSummary existingProduct = new ProductSummary(301L, 1L, "Acme", null, null, null, 50L, inventoryId);

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenReturn(new InventoryAccessContext(inventoryId, "Loja", 1));
        when(productRepository.findProductById(inventoryId, 301L)).thenReturn(Optional.of(existingProduct));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> updateProductService.updateProduct(
                        authenticatedUser,
                        inventoryId,
                        301L,
                        new UpdateProductRequest(
                                null,
                                null,
                                null,
                                null,
                                null,
                                objectMapper.getNodeFactory().numberNode(1_000_000_000_000_000_000L)
                        )
                )
        );

        assertEquals("quantidade", exception.getCampo());
    }

    /**
     * Verifica que o servico rejeita unidade acima do teto permitido no patch.
     * Entrada: produto existente e payload com `unidade > 999999999999999999`.
     * Esperado: ValidationException associada ao campo `unidade`.
     */
    @Test
    void shouldRejectUnidadeAboveMaximum() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();
        ProductSummary existingProduct = new ProductSummary(301L, 1L, "Acme", null, null, null, 50L, inventoryId);

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenReturn(new InventoryAccessContext(inventoryId, "Loja", 1));
        when(productRepository.findProductById(inventoryId, 301L)).thenReturn(Optional.of(existingProduct));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> updateProductService.updateProduct(
                        authenticatedUser,
                        inventoryId,
                        301L,
                        new UpdateProductRequest(
                                null,
                                null,
                                null,
                                objectMapper.getNodeFactory().numberNode(1_000_000_000_000_000_000L),
                                null,
                                null
                        )
                )
        );

        assertEquals("unidade", exception.getCampo());
    }

    /**
     * Verifica que o servico propaga a falha de autorizacao quando o usuario nao possui escrita no inventario.
     * Entrada: usuario autenticado sem `role = 0` ou `role = 1` para o inventario.
     * Esperado: ForbiddenException do servico de acesso.
     */
    @Test
    void shouldPropagateForbiddenWhenUserCannotWrite() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenThrow(new ForbiddenException("Sem escrita.", "inv_id", "write role required"));

        assertThrows(
                ForbiddenException.class,
                () -> updateProductService.updateProduct(
                        authenticatedUser,
                        inventoryId,
                        301L,
                        new UpdateProductRequest(
                                objectMapper.getNodeFactory().numberNode(2L),
                                null,
                                null,
                                null,
                                null,
                                null
                        )
                )
        );
    }

    /**
     * Cria um principal autenticado consistente com o formato usado pelos testes de servico protegidos.
     *
     * @return principal autenticado com metadados temporais fixos.
     */
    private AuthenticatedUserPrincipal authenticatedUser() {
        return new AuthenticatedUserPrincipal(
                UUID.randomUUID(),
                "Joao Novo",
                "joao.novo@example.com",
                OffsetDateTime.parse("2026-03-25T10:00:00-03:00"),
                OffsetDateTime.parse("2026-03-25T10:00:00-03:00"),
                OffsetDateTime.parse("2026-03-25T11:00:00-03:00"),
                3600
        );
    }
}
