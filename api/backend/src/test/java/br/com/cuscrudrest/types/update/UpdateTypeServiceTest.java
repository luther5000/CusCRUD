package br.com.cuscrudrest.types.update;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.ConflictException;
import br.com.cuscrudrest.common.error.ForbiddenException;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.common.error.ValidationException;
import br.com.cuscrudrest.inventories.InventoryAccessContext;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.types.TypeDetails;
import br.com.cuscrudrest.types.TypeRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UpdateTypeServiceTest {

    private final InventoryAccessService inventoryAccessService = mock(InventoryAccessService.class);
    private final TypeRepository typeRepository = mock(TypeRepository.class);
    private final UpdateTypeService updateTypeService = new UpdateTypeService(inventoryAccessService, typeRepository);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Verifica que o servico atualiza nome e remove a imagem quando o patch e valido.
     * Entrada: tipo existente, usuario com permissao de escrita e payload com `nome` novo e `imagem = null`.
     * Esperado: resposta com nome atualizado e imagem removida.
     */
    @Test
    void shouldUpdateNameAndRemoveImageWhenPatchIsValid() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();
        TypeDetails existingType = new TypeDetails(10L, "Higiene", "old-image".getBytes(StandardCharsets.UTF_8), inventoryId);
        TypeDetails updatedType = new TypeDetails(10L, "Higiene e Limpeza", null, inventoryId);

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenReturn(new InventoryAccessContext(inventoryId, "Loja", 1));
        when(typeRepository.findTypeById(inventoryId, 10L)).thenReturn(java.util.Optional.of(existingType));
        when(typeRepository.existsTypeByInventoryIdAndNameExcludingTypeId(inventoryId, 10L, "Higiene e Limpeza"))
                .thenReturn(false);
        when(typeRepository.updateType(inventoryId, 10L, "Higiene e Limpeza", null)).thenReturn(updatedType);

        UpdateTypeResponse response = updateTypeService.updateType(
                authenticatedUser,
                inventoryId,
                10L,
                new UpdateTypeRequest(
                        objectMapper.getNodeFactory().textNode("Higiene e Limpeza"),
                        objectMapper.getNodeFactory().nullNode()
                )
        );

        assertEquals(10L, response.typeId());
        assertEquals("Higiene e Limpeza", response.nome());
        assertNull(response.imagem());
    }

    /**
     * Verifica que o servico preserva a imagem existente quando apenas o nome e enviado no patch.
     * Entrada: tipo existente com imagem persistida e payload contendo somente `nome`.
     * Esperado: resposta com nome atualizado e imagem serializada mantida.
     */
    @Test
    void shouldKeepExistingImageWhenOnlyNameIsUpdated() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();
        byte[] imageBytes = "png".getBytes(StandardCharsets.UTF_8);
        TypeDetails existingType = new TypeDetails(10L, "Higiene", imageBytes, inventoryId);
        TypeDetails updatedType = new TypeDetails(10L, "Higiene e Limpeza", imageBytes, inventoryId);

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenReturn(new InventoryAccessContext(inventoryId, "Loja", 0));
        when(typeRepository.findTypeById(inventoryId, 10L)).thenReturn(java.util.Optional.of(existingType));
        when(typeRepository.existsTypeByInventoryIdAndNameExcludingTypeId(inventoryId, 10L, "Higiene e Limpeza"))
                .thenReturn(false);
        when(typeRepository.updateType(inventoryId, 10L, "Higiene e Limpeza", imageBytes)).thenReturn(updatedType);

        UpdateTypeResponse response = updateTypeService.updateType(
                authenticatedUser,
                inventoryId,
                10L,
                new UpdateTypeRequest(objectMapper.getNodeFactory().textNode("Higiene e Limpeza"), null)
        );

        assertEquals("Higiene e Limpeza", response.nome());
        assertEquals("data:image/png;base64,cG5n", response.imagem());
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
                () -> updateTypeService.updateType(
                        authenticatedUser,
                        inventoryId,
                        10L,
                        new UpdateTypeRequest(null, null)
                )
        );

        assertEquals("payload", exception.getCampo());
    }

    /**
     * Verifica que o servico rejeita imagem fora do formato data URI.
     * Entrada: tipo existente e payload com `imagem` textual invalida.
     * Esperado: ValidationException associada ao campo `imagem`.
     */
    @Test
    void shouldRejectInvalidImageFormat() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();
        TypeDetails existingType = new TypeDetails(10L, "Higiene", null, inventoryId);

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenReturn(new InventoryAccessContext(inventoryId, "Loja", 1));
        when(typeRepository.findTypeById(inventoryId, 10L)).thenReturn(java.util.Optional.of(existingType));

        ValidationException exception = assertThrows(
                ValidationException.class,
                () -> updateTypeService.updateType(
                        authenticatedUser,
                        inventoryId,
                        10L,
                        new UpdateTypeRequest(null, objectMapper.getNodeFactory().textNode("imagem-invalida"))
                )
        );

        assertEquals("imagem", exception.getCampo());
    }

    /**
     * Verifica que o servico rejeita nome duplicado no mesmo inventario.
     * Entrada: tipo existente e payload com `nome` ja usado por outro tipo do inventario.
     * Esperado: ConflictException associada ao campo `nome`.
     */
    @Test
    void shouldRejectDuplicateNameInInventory() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();
        TypeDetails existingType = new TypeDetails(10L, "Higiene", null, inventoryId);

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenReturn(new InventoryAccessContext(inventoryId, "Loja", 1));
        when(typeRepository.findTypeById(inventoryId, 10L)).thenReturn(java.util.Optional.of(existingType));
        when(typeRepository.existsTypeByInventoryIdAndNameExcludingTypeId(inventoryId, 10L, "Alimentos"))
                .thenReturn(true);

        ConflictException exception = assertThrows(
                ConflictException.class,
                () -> updateTypeService.updateType(
                        authenticatedUser,
                        inventoryId,
                        10L,
                        new UpdateTypeRequest(objectMapper.getNodeFactory().textNode("Alimentos"), null)
                )
        );

        assertEquals("nome", exception.getCampo());
    }

    /**
     * Verifica que o servico rejeita `type_id` inexistente para o inventario informado.
     * Entrada: inventario acessivel sem tipo correspondente ao `type_id`.
     * Esperado: NotFoundException associada ao campo `type_id`.
     */
    @Test
    void shouldRejectWhenTypeDoesNotExist() {
        AuthenticatedUserPrincipal authenticatedUser = authenticatedUser();
        UUID inventoryId = UUID.randomUUID();

        when(inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId()))
                .thenReturn(new InventoryAccessContext(inventoryId, "Loja", 1));
        when(typeRepository.findTypeById(inventoryId, 10L)).thenReturn(java.util.Optional.empty());

        NotFoundException exception = assertThrows(
                NotFoundException.class,
                () -> updateTypeService.updateType(
                        authenticatedUser,
                        inventoryId,
                        10L,
                        new UpdateTypeRequest(objectMapper.getNodeFactory().textNode("Alimentos"), null)
                )
        );

        assertEquals("type_id", exception.getCampo());
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
                () -> updateTypeService.updateType(
                        authenticatedUser,
                        inventoryId,
                        10L,
                        new UpdateTypeRequest(objectMapper.getNodeFactory().textNode("Alimentos"), null)
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
