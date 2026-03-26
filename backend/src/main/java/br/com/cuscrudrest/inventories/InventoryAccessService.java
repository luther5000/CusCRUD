package br.com.cuscrudrest.inventories;

import br.com.cuscrudrest.common.error.ForbiddenException;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Servico de resolucao e validacao de acesso a inventarios.
 * Centraliza a verificacao de existencia do inventario, vinculo do usuario e papel minimo exigido.
 * Efeitos colaterais: nenhum alem de leituras na base.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class InventoryAccessService {

    private static final int OWNER_ROLE = 0;

    private final InventoryRepository inventoryRepository;

    /**
     * Cria o servico de acesso a inventarios.
     *
     * @param inventoryRepository repositorio JDBC do dominio de inventarios.
     */
    public InventoryAccessService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Garante que o usuario autenticado possui qualquer vinculo valido com o inventario informado.
     * Estrategia: valida a existencia do inventario, verifica o vinculo do usuario e devolve o contexto resolvido.
     * Efeitos colaterais: nenhum alem de leituras na base.
     *
     * @param inventoryId identificador do inventario protegido.
     * @param userId identificador do usuario autenticado.
     * @return contexto de acesso do inventario resolvido para reutilizacao pelo caso de uso.
     * @throws NotFoundException quando o inventario nao existe ou o usuario nao possui acesso a ele.
     */
    public InventoryAccessContext requireAnyAccess(UUID inventoryId, UUID userId) {
        InventorySummary inventory = inventoryRepository.findInventoryById(inventoryId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventario nao encontrado.",
                        "inv_id",
                        "inventory not found"
                ));

        Integer role = inventoryRepository.findUserRole(inventoryId, userId)
                .orElseThrow(() -> new NotFoundException(
                        "Inventario nao encontrado.",
                        "inv_id",
                        "inventory not found"
                ));

        return new InventoryAccessContext(inventory.inventoryId(), inventory.inventoryName(), role);
    }

    /**
     * Garante que o usuario autenticado possui acesso owner ao inventario informado.
     * Estrategia: valida a existencia do inventario, verifica se o usuario pertence a ele e exige `role = 0`.
     * Efeitos colaterais: nenhum alem de leituras na base.
     *
     * @param inventoryId identificador do inventario protegido.
     * @param userId identificador do usuario autenticado.
     * @return contexto de acesso do inventario resolvido para reutilizacao pelo caso de uso.
     * @throws NotFoundException quando o inventario nao existe ou o usuario nao possui acesso a ele.
     * @throws ForbiddenException quando o usuario possui acesso, mas nao com role owner.
     */
    public InventoryAccessContext requireOwnerAccess(UUID inventoryId, UUID userId) {
        InventoryAccessContext accessContext = requireAnyAccess(inventoryId, userId);
        Integer role = accessContext.role();

        if (role != OWNER_ROLE) {
            throw new ForbiddenException(
                    "Usuario autenticado nao possui role owner para o inventario.",
                    "inv_id",
                    "owner role required"
            );
        }

        return accessContext;
    }
}
