package br.com.cuscrudrest.inventories.users.list;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryAccessContext;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.inventories.InventoryRepository;
import br.com.cuscrudrest.inventories.InventoryUserSummary;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Servico de listagem paginada de usuarios com acesso a um inventario.
 * Centraliza a validacao de ownership, a paginacao por offset e a consulta ordenada por `user_id`.
 * Efeitos colaterais: nenhum alem de leituras na base.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class ListInventoryUsersService {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 200;

    private final InventoryAccessService inventoryAccessService;
    private final InventoryRepository inventoryRepository;

    /**
     * Cria o servico de listagem de usuarios do inventario.
     *
     * @param inventoryAccessService servico responsavel por validar ownership do inventario.
     * @param inventoryRepository repositorio JDBC do dominio de inventarios.
     */
    public ListInventoryUsersService(
            InventoryAccessService inventoryAccessService,
            InventoryRepository inventoryRepository
    ) {
        this.inventoryAccessService = inventoryAccessService;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Lista os usuarios com acesso ao inventario informado quando o solicitante e owner.
     * Estrategia: valida ownership, normaliza `limit`/`offset`, conta o total e consulta a pagina ordenada.
     * Efeitos colaterais: nenhum alem de leituras na base.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param inventoryId identificador do inventario consultado.
     * @param limit limite opcional informado na query string.
     * @param offset offset opcional informado na query string.
     * @return pagina de usuarios com acesso ao inventario.
     * @throws NotFoundException quando `limit` ou `offset` estao fora do intervalo aceito.
     */
    public ListInventoryUsersPage listInventoryUsers(
            AuthenticatedUserPrincipal authenticatedUser,
            UUID inventoryId,
            Integer limit,
            Integer offset
    ) {
        InventoryAccessContext accessContext = inventoryAccessService.requireOwnerAccess(
                inventoryId,
                authenticatedUser.userId()
        );

        int effectiveLimit = normalizeLimit(limit);
        int effectiveOffset = normalizeOffset(offset);
        int total = inventoryRepository.countInventoryUsers(inventoryId);

        if (total > 0 && effectiveOffset >= total) {
            throw new NotFoundException(
                    "Paginacao fora do intervalo aceito.",
                    "offset",
                    "offset after end of list"
            );
        }

        List<ListInventoryUsersItemResponse> users = inventoryRepository.listInventoryUsers(
                        inventoryId,
                        effectiveLimit,
                        effectiveOffset
                )
                .stream()
                .map(this::toItemResponse)
                .toList();

        Integer nextOffset = effectiveOffset + users.size() < total
                ? effectiveOffset + effectiveLimit
                : null;

        return new ListInventoryUsersPage(
                new ListInventoryUsersInventoryResponse(accessContext.inventoryId(), accessContext.inventoryName()),
                users,
                nextOffset,
                effectiveLimit
        );
    }

    /**
     * Normaliza e valida o `limit` informado na query string.
     *
     * @param limit limite opcional recebido pelo endpoint.
     * @return limite efetivo a ser usado na consulta.
     * @throws NotFoundException quando o limite esta fora do intervalo `1..200`.
     */
    private int normalizeLimit(Integer limit) {
        int effectiveLimit = limit != null ? limit : DEFAULT_LIMIT;
        if (effectiveLimit < 1 || effectiveLimit > MAX_LIMIT) {
            throw new NotFoundException(
                    "Paginacao fora do intervalo aceito.",
                    "limit",
                    "limit must be between 1 and 200"
            );
        }
        return effectiveLimit;
    }

    /**
     * Normaliza e valida o `offset` informado na query string.
     *
     * @param offset offset opcional recebido pelo endpoint.
     * @return offset efetivo a ser usado na consulta.
     * @throws NotFoundException quando o offset e negativo.
     */
    private int normalizeOffset(Integer offset) {
        int effectiveOffset = offset != null ? offset : 0;
        if (effectiveOffset < 0) {
            throw new NotFoundException(
                    "Paginacao fora do intervalo aceito.",
                    "offset",
                    "offset must be greater than or equal to 0"
            );
        }
        return effectiveOffset;
    }

    /**
     * Converte a projecao interna do repositorio para o item de resposta HTTP.
     *
     * @param inventoryUserSummary usuario retornado da camada JDBC.
     * @return item serializavel da resposta da API.
     */
    private ListInventoryUsersItemResponse toItemResponse(InventoryUserSummary inventoryUserSummary) {
        return new ListInventoryUsersItemResponse(
                inventoryUserSummary.userId(),
                inventoryUserSummary.name(),
                inventoryUserSummary.login(),
                inventoryUserSummary.role()
        );
    }
}
