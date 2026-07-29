package br.com.cuscrudrest.inventories.rename;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryAccessContext;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.inventories.InventoryRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servico de renomeacao de inventarios da aplicacao.
 * Centraliza a validacao de ownership e a atualizacao do nome persistido do inventario.
 * Efeitos colaterais: atualiza o campo `inv_name` na tabela `inventories`.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class RenameInventoryService {

    private final InventoryAccessService inventoryAccessService;
    private final InventoryRepository inventoryRepository;

    /**
     * Cria o servico de renomeacao de inventarios.
     *
     * @param inventoryAccessService servico responsavel por validar ownership do inventario.
     * @param inventoryRepository repositorio JDBC do dominio de inventarios.
     */
    public RenameInventoryService(
            InventoryAccessService inventoryAccessService,
            InventoryRepository inventoryRepository
    ) {
        this.inventoryAccessService = inventoryAccessService;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Renomeia um inventario existente pertencente ao usuario autenticado como owner.
     * Estrategia: valida o acesso owner, atualiza o nome persistido e retorna o payload atualizado.
     * Efeitos colaterais: atualiza o nome do inventario na base dentro de uma transacao.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param inventoryId identificador do inventario a ser renomeado.
     * @param request payload com o novo nome do inventario.
     * @return inventario atualizado e a role owner mantida para o usuario autenticado.
     */
    @Transactional
    public RenameInventoryResponse renameInventory(
            AuthenticatedUserPrincipal authenticatedUser,
            UUID inventoryId,
            RenameInventoryRequest request
    ) {
        InventoryAccessContext accessContext = inventoryAccessService.requireOwnerAccess(
                inventoryId,
                authenticatedUser.userId()
        );

        inventoryRepository.renameInventory(inventoryId, request.inventoryName());

        return new RenameInventoryResponse(
                new RenameInventoryBodyResponse(accessContext.inventoryId(), request.inventoryName()),
                accessContext.role()
        );
    }
}
