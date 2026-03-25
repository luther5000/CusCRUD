package br.com.cuscrudrest.inventories.delete;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.inventories.InventoryRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servico de remocao de inventarios da aplicacao.
 * Centraliza a validacao de ownership e a exclusao persistida do inventario solicitado.
 * Efeitos colaterais: remove o registro do inventario da base e dependencias em cascata conforme o schema.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class DeleteInventoryService {

    private final InventoryAccessService inventoryAccessService;
    private final InventoryRepository inventoryRepository;

    /**
     * Cria o servico de remocao de inventarios.
     *
     * @param inventoryAccessService servico responsavel por validar ownership do inventario.
     * @param inventoryRepository repositorio JDBC do dominio de inventarios.
     */
    public DeleteInventoryService(
            InventoryAccessService inventoryAccessService,
            InventoryRepository inventoryRepository
    ) {
        this.inventoryAccessService = inventoryAccessService;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Remove um inventario existente pertencente ao usuario autenticado como owner.
     * Estrategia: valida o acesso owner ao recurso e executa a exclusao do inventario na mesma transacao.
     * Efeitos colaterais: remove o inventario da base de dados.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param inventoryId identificador do inventario a ser removido.
     */
    @Transactional
    public void deleteInventory(AuthenticatedUserPrincipal authenticatedUser, UUID inventoryId) {
        inventoryAccessService.requireOwnerAccess(inventoryId, authenticatedUser.userId());
        inventoryRepository.deleteInventory(inventoryId);
    }
}
