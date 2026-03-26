package br.com.cuscrudrest.inventories.users.delete;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.auth.user.UserRepository;
import br.com.cuscrudrest.common.error.ConflictException;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.inventories.InventoryRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servico de remocao de acesso de usuarios a inventarios.
 * Centraliza validacao de ownership e protecao contra auto-remocao do owner.
 * Efeitos colaterais: remove registros persistidos da tabela `inventory_access`.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class DeleteInventoryUserService {

    private static final int OWNER_ROLE = 0;

    private final InventoryAccessService inventoryAccessService;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    /**
     * Cria o servico de remocao de usuarios de inventarios.
     *
     * @param inventoryAccessService servico responsavel por validar ownership do inventario.
     * @param inventoryRepository repositorio JDBC do dominio de inventarios.
     * @param userRepository repositorio JDBC do dominio de usuarios.
     */
    public DeleteInventoryUserService(
            InventoryAccessService inventoryAccessService,
            InventoryRepository inventoryRepository,
            UserRepository userRepository
    ) {
        this.inventoryAccessService = inventoryAccessService;
        this.inventoryRepository = inventoryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Remove o acesso de um usuario ao inventario informado.
     * Estrategia: valida ownership, garante existencia do usuario alvo e protege a auto-remocao do owner antes do delete.
     * Efeitos colaterais: remove o vinculo persistido em `inventory_access`.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param targetUserId identificador do usuario cujo acesso sera removido.
     */
    @Transactional
    public void deleteInventoryUser(
            AuthenticatedUserPrincipal authenticatedUser,
            UUID inventoryId,
            UUID targetUserId
    ) {
        inventoryAccessService.requireOwnerAccess(inventoryId, authenticatedUser.userId());

        userRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new NotFoundException(
                        "Usuario nao encontrado.",
                        "user_id",
                        "user not found"
                ));

        Integer currentRole = inventoryRepository.findUserRole(inventoryId, targetUserId)
                .orElseThrow(() -> new NotFoundException(
                        "Usuario nao possui acesso ao inventario.",
                        "user_id",
                        "user does not have access to inventory"
                ));

        ensureOwnerWillNotBeRemoved(authenticatedUser.userId(), targetUserId, currentRole);
        inventoryRepository.deleteUserAccess(inventoryId, targetUserId);
    }

    /**
     * Garante que a remocao nao atinja o owner do inventario por este endpoint.
     *
     * @param authenticatedUserId identificador do owner autenticado.
     * @param targetUserId identificador do usuario alvo.
     * @param currentRole role atual do usuario alvo no inventario.
     * @throws ConflictException quando o alvo e o proprio owner autenticado ou possui role owner.
     */
    private void ensureOwnerWillNotBeRemoved(UUID authenticatedUserId, UUID targetUserId, int currentRole) {
        if (currentRole == OWNER_ROLE || authenticatedUserId.equals(targetUserId)) {
            throw new ConflictException(
                    "Owner nao pode remover o proprio acesso ao inventario.",
                    "user_id",
                    "owner cannot remove own access"
            );
        }
    }
}
