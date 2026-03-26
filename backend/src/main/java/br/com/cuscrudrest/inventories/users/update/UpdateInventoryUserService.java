package br.com.cuscrudrest.inventories.users.update;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.auth.user.UserAccount;
import br.com.cuscrudrest.auth.user.UserRepository;
import br.com.cuscrudrest.common.error.ConflictException;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.common.error.ValidationException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryAccessContext;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.inventories.InventoryRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servico de atualizacao da role de usuarios em inventarios.
 * Centraliza validacao de ownership, role permitida e protecao contra alteracao da role do owner.
 * Efeitos colaterais: atualiza registros persistidos na tabela `inventory_access`.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class UpdateInventoryUserService {

    private static final int OWNER_ROLE = 0;
    private static final int EDITOR_ROLE = 1;
    private static final int READER_ROLE = 2;

    private final InventoryAccessService inventoryAccessService;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    /**
     * Cria o servico de atualizacao de role de usuario no inventario.
     *
     * @param inventoryAccessService servico responsavel por validar ownership do inventario.
     * @param inventoryRepository repositorio JDBC do dominio de inventarios.
     * @param userRepository repositorio JDBC do dominio de usuarios.
     */
    public UpdateInventoryUserService(
            InventoryAccessService inventoryAccessService,
            InventoryRepository inventoryRepository,
            UserRepository userRepository
    ) {
        this.inventoryAccessService = inventoryAccessService;
        this.inventoryRepository = inventoryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Atualiza a role de um usuario ja vinculado ao inventario informado.
     * Estrategia: valida ownership, valida payload de negocio, protege a role do owner e persiste a nova role.
     * Efeitos colaterais: atualiza a role persistida do usuario na tabela `inventory_access`.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param targetUserId identificador do usuario cuja role sera alterada.
     * @param request payload com a nova role desejada.
     * @return resposta HTTP com inventario e usuario atualizados.
     */
    @Transactional
    public UpdateInventoryUserResponse updateInventoryUser(
            AuthenticatedUserPrincipal authenticatedUser,
            UUID inventoryId,
            UUID targetUserId,
            UpdateInventoryUserRequest request
    ) {
        InventoryAccessContext accessContext = inventoryAccessService.requireOwnerAccess(
                inventoryId,
                authenticatedUser.userId()
        );

        validateRole(request.role());

        UserAccount targetUser = userRepository.findByUserId(targetUserId)
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

        ensureOwnerRoleWillNotBeChanged(authenticatedUser.userId(), targetUserId, currentRole);
        inventoryRepository.updateUserAccessRole(inventoryId, targetUserId, request.role());

        return new UpdateInventoryUserResponse(
                new UpdateInventoryUserInventoryResponse(accessContext.inventoryId(), accessContext.inventoryName()),
                new UpdateInventoryUserUserResponse(
                        targetUser.userId(),
                        targetUser.name(),
                        targetUser.login(),
                        request.role()
                )
        );
    }

    /**
     * Valida se a role solicitada esta no conjunto permitido pela API.
     *
     * @param role role recebida no payload.
     * @throws ValidationException quando a role nao e `1` nem `2`.
     */
    private void validateRole(Integer role) {
        if (role != EDITOR_ROLE && role != READER_ROLE) {
            throw new ValidationException(
                    "Role invalida para atualizacao de usuario.",
                    "role",
                    "must be 1 or 2"
            );
        }
    }

    /**
     * Garante que a role owner nao seja alterada por este endpoint.
     *
     * @param authenticatedUserId identificador do owner autenticado.
     * @param targetUserId identificador do usuario alvo.
     * @param currentRole role atual do usuario alvo no inventario.
     * @throws ConflictException quando o alvo e o proprio owner autenticado ou possui role owner.
     */
    private void ensureOwnerRoleWillNotBeChanged(UUID authenticatedUserId, UUID targetUserId, int currentRole) {
        if (currentRole == OWNER_ROLE || authenticatedUserId.equals(targetUserId)) {
            throw new ConflictException(
                    "Owner nao pode alterar a propria role no inventario.",
                    "user_id",
                    "owner cannot change own role"
            );
        }
    }
}
