package br.com.cuscrudrest.types.delete;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.ConflictException;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.types.TypeRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servico de remocao de tipos da aplicacao.
 * Centraliza a validacao de permissao de escrita, existencia do tipo e conflitos por produtos vinculados.
 * Efeitos colaterais: remove registros persistidos da tabela `types`.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class DeleteTypeService {

    private final InventoryAccessService inventoryAccessService;
    private final TypeRepository typeRepository;

    /**
     * Cria o servico de remocao de tipos.
     *
     * @param inventoryAccessService servico responsavel por validar acesso de escrita ao inventario.
     * @param typeRepository repositorio JDBC do dominio de tipos.
     */
    public DeleteTypeService(
            InventoryAccessService inventoryAccessService,
            TypeRepository typeRepository
    ) {
        this.inventoryAccessService = inventoryAccessService;
        this.typeRepository = typeRepository;
    }

    /**
     * Remove um tipo existente do inventario informado.
     * Estrategia: valida permissao de escrita, garante a existencia do tipo e executa o delete na mesma transacao.
     * Efeitos colaterais: remove um tipo persistido da base.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param typeId identificador do tipo a ser removido.
     */
    @Transactional
    public void deleteType(AuthenticatedUserPrincipal authenticatedUser, UUID inventoryId, long typeId) {
        inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId());
        ensureTypeExists(inventoryId, typeId);

        try {
            typeRepository.deleteType(inventoryId, typeId);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    "Nao e possivel remover o tipo porque existem produtos vinculados.",
                    "type_id",
                    "type has associated products"
            );
        }
    }

    /**
     * Garante que o tipo existe para o inventario informado antes da remocao.
     *
     * @param inventoryId identificador do inventario alvo.
     * @param typeId identificador do tipo a ser removido.
     * @throws NotFoundException quando o tipo nao existe para o inventario informado.
     */
    private void ensureTypeExists(UUID inventoryId, long typeId) {
        if (typeRepository.findTypeById(inventoryId, typeId).isEmpty()) {
            throw new NotFoundException(
                    "Tipo nao encontrado.",
                    "type_id",
                    "type not found"
            );
        }
    }
}
