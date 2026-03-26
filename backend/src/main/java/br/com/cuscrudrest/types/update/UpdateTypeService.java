package br.com.cuscrudrest.types.update;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.ConflictException;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.common.error.ValidationException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.types.TypeDetails;
import br.com.cuscrudrest.types.TypeImageCodec;
import br.com.cuscrudrest.types.TypeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.annotation.Conditional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servico de atualizacao de tipos da aplicacao.
 * Centraliza a validacao de permissao de escrita, comportamento parcial do patch e unicidade do nome por inventario.
 * Efeitos colaterais: atualiza registros persistidos na tabela `types`.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class UpdateTypeService {

    private final InventoryAccessService inventoryAccessService;
    private final TypeRepository typeRepository;

    /**
     * Cria o servico de atualizacao de tipos.
     *
     * @param inventoryAccessService servico responsavel por validar acesso de escrita ao inventario.
     * @param typeRepository repositorio JDBC do dominio de tipos.
     */
    public UpdateTypeService(
            InventoryAccessService inventoryAccessService,
            TypeRepository typeRepository
    ) {
        this.inventoryAccessService = inventoryAccessService;
        this.typeRepository = typeRepository;
    }

    /**
     * Atualiza parcialmente um tipo existente do inventario informado.
     * Estrategia: valida permissao de escrita, garante existencia do tipo, aplica merge do patch e persiste o estado final.
     * Efeitos colaterais: atualiza o registro correspondente na tabela `types`.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param typeId identificador do tipo a ser atualizado.
     * @param request payload parcial do patch.
     * @return resposta HTTP com o tipo atualizado.
     */
    @Transactional
    public UpdateTypeResponse updateType(
            AuthenticatedUserPrincipal authenticatedUser,
            UUID inventoryId,
            long typeId,
            UpdateTypeRequest request
    ) {
        inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId());
        ensurePatchHasAtLeastOneField(request);

        TypeDetails existingType = typeRepository.findTypeById(inventoryId, typeId)
                .orElseThrow(() -> new NotFoundException(
                        "Tipo nao encontrado.",
                        "type_id",
                        "type not found"
                ));

        String updatedName = resolveUpdatedName(request.nome(), existingType.nome(), inventoryId, typeId);
        byte[] updatedImage = resolveUpdatedImage(request.imagem(), existingType.imagem());

        TypeDetails updatedType;
        try {
            updatedType = typeRepository.updateType(inventoryId, typeId, updatedName, updatedImage);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    "Ja existe outro tipo com o mesmo nome no inventario.",
                    "nome",
                    "type name already exists in inventory"
            );
        }

        return new UpdateTypeResponse(
                updatedType.typeId(),
                updatedType.nome(),
                TypeImageCodec.toDataUri(updatedType.imagem()),
                updatedType.inventoryId()
        );
    }

    private void ensurePatchHasAtLeastOneField(UpdateTypeRequest request) {
        if (request.isEmpty()) {
            throw new ValidationException(
                    "Payload invalido.",
                    "payload",
                    "at least one field must be provided"
            );
        }
    }

    private String resolveUpdatedName(JsonNode nomeNode, String currentName, UUID inventoryId, long typeId) {
        if (nomeNode == null) {
            return currentName;
        }

        if (!nomeNode.isTextual()) {
            throw new ValidationException(
                    "Nome invalido.",
                    "nome",
                    "must have between 1 and 255 characters"
            );
        }

        String nome = nomeNode.asText();
        if (nome.isBlank() || nome.length() > 255) {
            throw new ValidationException(
                    "Nome invalido.",
                    "nome",
                    "must have between 1 and 255 characters"
            );
        }

        if (typeRepository.existsTypeByInventoryIdAndNameExcludingTypeId(inventoryId, typeId, nome)) {
            throw new ConflictException(
                    "Ja existe outro tipo com o mesmo nome no inventario.",
                    "nome",
                    "type name already exists in inventory"
            );
        }

        return nome;
    }

    private byte[] resolveUpdatedImage(JsonNode imagemNode, byte[] currentImage) {
        if (imagemNode == null) {
            return currentImage;
        }

        if (imagemNode.isNull()) {
            return null;
        }

        if (!imagemNode.isTextual()) {
            throw new ValidationException(
                    "Imagem invalida.",
                    "imagem",
                    "must be a valid data URI with mime and base64 content"
            );
        }

        return TypeImageCodec.parseDataUri(imagemNode.asText());
    }
}
