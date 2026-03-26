package br.com.cuscrudrest.inventories.users.create;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.auth.support.EmailAddressValidator;
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

/**
 * Servico de concessao de acesso de usuarios a inventarios.
 * Centraliza validacao de ownership, formato do email, role permitida e prevencao de vinculos duplicados.
 * Efeitos colaterais: cria registros persistidos na tabela `inventory_access`.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class AddInventoryUserService {

    private static final int EDITOR_ROLE = 1;
    private static final int READER_ROLE = 2;

    private final EmailAddressValidator emailAddressValidator;
    private final InventoryAccessService inventoryAccessService;
    private final InventoryRepository inventoryRepository;
    private final UserRepository userRepository;

    /**
     * Cria o servico de adicao de usuarios a inventarios.
     *
     * @param emailAddressValidator validador de formato de email da aplicacao.
     * @param inventoryAccessService servico responsavel por validar ownership do inventario.
     * @param inventoryRepository repositorio JDBC do dominio de inventarios.
     * @param userRepository repositorio JDBC do dominio de usuarios.
     */
    public AddInventoryUserService(
            EmailAddressValidator emailAddressValidator,
            InventoryAccessService inventoryAccessService,
            InventoryRepository inventoryRepository,
            UserRepository userRepository
    ) {
        this.emailAddressValidator = emailAddressValidator;
        this.inventoryAccessService = inventoryAccessService;
        this.inventoryRepository = inventoryRepository;
        this.userRepository = userRepository;
    }

    /**
     * Adiciona um usuario existente ao inventario informado com role editor ou reader.
     * Estrategia: valida ownership, valida payload de negocio, localiza o usuario alvo e persiste o vinculo na mesma transacao.
     * Efeitos colaterais: cria um novo acesso persistido na tabela `inventory_access`.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param request payload com o login do usuario e a role desejada.
     * @return resposta HTTP com inventario e usuario adicionados.
     */
    @Transactional
    public AddInventoryUserResponse addInventoryUser(
            AuthenticatedUserPrincipal authenticatedUser,
            java.util.UUID inventoryId,
            AddInventoryUserRequest request
    ) {
        InventoryAccessContext accessContext = inventoryAccessService.requireOwnerAccess(
                inventoryId,
                authenticatedUser.userId()
        );

        validateEmail(request.login());
        validateRole(request.role());

        UserAccount targetUser = userRepository.findByLogin(request.login())
                .orElseThrow(() -> new NotFoundException(
                        "Usuario nao encontrado.",
                        "login",
                        "user login not found"
                ));

        ensureUserDoesNotAlreadyHaveAccess(inventoryId, targetUser.userId());
        inventoryRepository.addUserAccess(inventoryId, targetUser.userId(), request.role());

        return new AddInventoryUserResponse(
                new AddInventoryUserInventoryResponse(accessContext.inventoryId(), accessContext.inventoryName()),
                new AddInventoryUserUserResponse(
                        targetUser.userId(),
                        targetUser.name(),
                        targetUser.login(),
                        request.role()
                )
        );
    }

    /**
     * Valida o formato do login informado no request.
     *
     * @param login email do usuario alvo.
     * @throws ValidationException quando o login nao possui formato de email valido.
     */
    private void validateEmail(String login) {
        if (!emailAddressValidator.isValid(login)) {
            throw new ValidationException("Login invalido.", "login", "must be a valid email");
        }
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
                    "Role invalida para adicao de usuario.",
                    "role",
                    "must be 1 or 2"
            );
        }
    }

    /**
     * Garante que o usuario alvo ainda nao possui vinculo com o inventario.
     *
     * @param inventoryId identificador do inventario.
     * @param userId identificador do usuario alvo.
     * @throws ConflictException quando o usuario ja possui acesso ao inventario.
     */
    private void ensureUserDoesNotAlreadyHaveAccess(java.util.UUID inventoryId, java.util.UUID userId) {
        if (inventoryRepository.findUserRole(inventoryId, userId).isPresent()) {
            throw new ConflictException(
                    "Usuario ja possui acesso ao inventario.",
                    "login",
                    "user already has access to inventory"
            );
        }
    }
}
