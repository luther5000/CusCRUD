package br.com.cuscrudrest.types.create;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.ConflictException;
import br.com.cuscrudrest.common.error.ForbiddenException;
import br.com.cuscrudrest.common.error.ValidationException;
import br.com.cuscrudrest.inventories.InventoryAccessContext;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.types.TypeDetails;
import br.com.cuscrudrest.types.TypeRepository;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreateTypeServiceTest {

    private final InventoryAccessService inventoryAccessService = mock(InventoryAccessService.class);
    private final TypeRepository typeRepository = mock(TypeRepository.class);
    private final CreateTypeService createTypeService = new CreateTypeService(inventoryAccessService, typeRepository);

    @Test
    void shouldCreateTypeWithoutImageWhenUserHasWriteAccess() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();
        CreateTypeRequest request = new CreateTypeRequest("Higiene", null);
        TypeDetails createdType = new TypeDetails(10L, "Higiene", null, inventoryId);

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenReturn(new InventoryAccessContext(inventoryId, "Loja", 1));
        when(typeRepository.existsTypeByInventoryIdAndName(inventoryId, "Higiene")).thenReturn(false);
        when(typeRepository.createType(inventoryId, "Higiene", null)).thenReturn(createdType);

        CreateTypeResponse response = createTypeService.createType(authenticatedUser, inventoryId, request);

        assertEquals(10L, response.typeId());
        assertEquals("Higiene", response.nome());
        assertNull(response.imagem());
        assertEquals(inventoryId, response.inventoryId());
    }

    @Test
    void shouldReturnOriginalImageDataUriWhenTypeIsCreatedWithImage() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();
        byte[] imageBytes = "png-bytes".getBytes(StandardCharsets.UTF_8);
        String imageDataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(imageBytes);
        CreateTypeRequest request = new CreateTypeRequest("Higiene", imageDataUri);
        TypeDetails createdType = new TypeDetails(11L, "Higiene", imageBytes, inventoryId);

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenReturn(new InventoryAccessContext(inventoryId, "Loja", 0));
        when(typeRepository.existsTypeByInventoryIdAndName(inventoryId, "Higiene")).thenReturn(false);
        when(typeRepository.createType(inventoryId, "Higiene", imageBytes)).thenReturn(createdType);

        CreateTypeResponse response = createTypeService.createType(authenticatedUser, inventoryId, request);

        assertEquals(11L, response.typeId());
        assertEquals(imageDataUri, response.imagem());
    }

    @Test
    void shouldRejectInvalidImageFormat() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenReturn(new InventoryAccessContext(inventoryId, "Loja", 1));
        when(typeRepository.existsTypeByInventoryIdAndName(inventoryId, "Higiene")).thenReturn(false);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> createTypeService.createType(
                        authenticatedUser,
                        inventoryId,
                        new CreateTypeRequest("Higiene", "imagem-invalida")
                )
        );

        assertEquals("imagem", exception.getCampo());
    }

    @Test
    void shouldRejectImageAboveFiveMiB() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();
        byte[] largeImage = new byte[(5 * 1024 * 1024) + 1];
        String imageDataUri = "data:image/png;base64," + Base64.getEncoder().encodeToString(largeImage);

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenReturn(new InventoryAccessContext(inventoryId, "Loja", 1));
        when(typeRepository.existsTypeByInventoryIdAndName(inventoryId, "Higiene")).thenReturn(false);

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> createTypeService.createType(
                        authenticatedUser,
                        inventoryId,
                        new CreateTypeRequest("Higiene", imageDataUri)
                )
        );

        assertEquals("imagem", exception.getCampo());
        assertEquals("must not exceed 5 MiB", exception.getInfo());
    }

    @Test
    void shouldRejectDuplicateTypeNameInInventory() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenReturn(new InventoryAccessContext(inventoryId, "Loja", 1));
        when(typeRepository.existsTypeByInventoryIdAndName(inventoryId, "Higiene")).thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> createTypeService.createType(
                        authenticatedUser,
                        inventoryId,
                        new CreateTypeRequest("Higiene", null)
                )
        );

        assertEquals("nome", exception.getCampo());
    }

    @Test
    void shouldPropagateForbiddenWhenUserDoesNotHaveWriteAccess() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenThrow(new ForbiddenException("Sem escrita.", "inv_id", "write role required"));

        assertThrows(
                ForbiddenException.class,
                () -> createTypeService.createType(
                        authenticatedUser,
                        inventoryId,
                        new CreateTypeRequest("Higiene", null)
                )
        );
    }

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
