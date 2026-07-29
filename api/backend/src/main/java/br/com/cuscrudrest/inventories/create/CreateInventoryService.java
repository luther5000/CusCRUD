package br.com.cuscrudrest.inventories.create;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.ConflictException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servico de criacao de inventarios da aplicacao.
 * Centraliza o limite de ownership e a coordenacao entre criacao do inventario e concessao do papel owner.
 * Efeitos colaterais: cria registros persistidos nas tabelas `inventories` e `inventory_access`.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class CreateInventoryService {

    private static final int OWNER_ROLE = 0;
    private static final int MAX_OWNED_INVENTORIES = 100;

    private final InventoryRepository inventoryRepository;

    /**
     * Cria o servico de criacao de inventarios.
     *
     * @param inventoryRepository repositorio JDBC dos inventarios.
     */
    public CreateInventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Cria um novo inventario para o usuario autenticado.
     * Estrategia: valida o limite de ownership, gera um UUID para o inventario e persiste inventario + acesso owner na mesma transacao.
     * Efeitos colaterais: cria um inventario e o vinculo owner correspondente na base.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param request payload com o nome do inventario a ser criado.
     * @return inventario criado e o papel owner atribuido ao usuario.
     * @throws ConflictException quando o usuario ja atingiu o limite de 100 inventarios como owner.
     */
    @Transactional
    public CreateInventoryResponse createInventory(
            AuthenticatedUserPrincipal authenticatedUser,
            CreateInventoryRequest request
    ) {
        ensureOwnerInventoryLimit(authenticatedUser.userId());

        UUID inventoryId = UUID.randomUUID();
        inventoryRepository.createInventoryWithOwner(inventoryId, request.inventoryName(), authenticatedUser.userId());

        return new CreateInventoryResponse(
                new CreateInventoryBodyResponse(inventoryId, request.inventoryName()),
                OWNER_ROLE
        );
    }

    /**
     * Garante que o usuario ainda pode criar inventarios como owner.
     *
     * @param userId identificador do usuario autenticado.
     * @throws ConflictException quando o limite de ownership ja foi atingido.
     */
    private void ensureOwnerInventoryLimit(UUID userId) {
        if (inventoryRepository.countOwnedInventories(userId) >= MAX_OWNED_INVENTORIES) {
            throw new ConflictException(
                    "Limite de inventarios como owner atingido.",
                    "inventory",
                    "owner inventory limit reached"
            );
        }
    }
}
