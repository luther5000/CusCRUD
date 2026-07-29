package br.com.cuscrudrest.types.create;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.ConflictException;
import br.com.cuscrudrest.common.error.ValidationException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryAccessContext;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.types.TypeDetails;
import br.com.cuscrudrest.types.TypeImageCodec;
import br.com.cuscrudrest.types.TypeRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servico de criacao de tipos da aplicacao.
 * Centraliza a validacao de permissao de escrita, unicidade do nome e formato da imagem em data URI.
 * Efeitos colaterais: cria registros persistidos na tabela `types`.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class CreateTypeService {

    private final InventoryAccessService inventoryAccessService;
    private final TypeRepository typeRepository;

    /**
     * Cria o servico de criacao de tipos.
     *
     * @param inventoryAccessService servico responsavel por validar acesso de escrita ao inventario.
     * @param typeRepository repositorio JDBC do dominio de tipos.
     */
    public CreateTypeService(
            InventoryAccessService inventoryAccessService,
            TypeRepository typeRepository
    ) {
        this.inventoryAccessService = inventoryAccessService;
        this.typeRepository = typeRepository;
    }

    /**
     * Cria um novo tipo no inventario informado.
     * Estrategia: valida permissao de escrita, unicidade do nome, formato da imagem e persiste o registro na mesma transacao.
     * Efeitos colaterais: cria um novo registro na tabela `types`.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param request payload com nome e imagem opcional do tipo.
     * @return resposta HTTP com o tipo criado.
     */
    @Transactional
    public CreateTypeResponse createType(
            AuthenticatedUserPrincipal authenticatedUser,
            UUID inventoryId,
            CreateTypeRequest request
    ) {
        InventoryAccessContext accessContext = inventoryAccessService.requireWriteAccess(
                inventoryId,
                authenticatedUser.userId()
        );

        ensureUniqueTypeName(accessContext.inventoryId(), request.nome());
        byte[] imageBytes = request.imagem() == null ? null : TypeImageCodec.parseDataUri(request.imagem());

        TypeDetails createdType;
        try {
            createdType = typeRepository.createType(
                    accessContext.inventoryId(),
                    request.nome(),
                    imageBytes
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    "Ja existe outro tipo com o mesmo nome no inventario.",
                    "nome",
                    "type name already exists in inventory"
            );
        }

        return new CreateTypeResponse(
                createdType.typeId(),
                createdType.nome(),
                TypeImageCodec.toDataUri(createdType.imagem()),
                createdType.inventoryId()
        );
    }

    /**
     * Garante que ainda nao exista um tipo com o mesmo nome no inventario informado.
     *
     * @param inventoryId identificador do inventario alvo.
     * @param nome nome do tipo cuja unicidade sera validada.
     * @throws ConflictException quando ja existir um tipo com o mesmo nome no inventario.
     */
    private void ensureUniqueTypeName(UUID inventoryId, String nome) {
        if (typeRepository.existsTypeByInventoryIdAndName(inventoryId, nome)) {
            throw new ConflictException(
                    "Ja existe outro tipo com o mesmo nome no inventario.",
                    "nome",
                    "type name already exists in inventory"
            );
        }
    }

}
