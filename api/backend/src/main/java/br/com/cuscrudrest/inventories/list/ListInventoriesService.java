package br.com.cuscrudrest.inventories.list;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryRepository;
import br.com.cuscrudrest.inventories.UserInventorySummary;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servico de listagem paginada de inventarios acessiveis ao usuario autenticado.
 * Centraliza a validacao de `limit`/`offset`, a consulta ordenada e o calculo de continuidade da paginacao.
 * Efeitos colaterais: nenhum alem de leituras na base.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class ListInventoriesService {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 200;

    private final InventoryRepository inventoryRepository;

    /**
     * Cria o servico de listagem de inventarios.
     *
     * @param inventoryRepository repositorio JDBC do dominio de inventarios.
     */
    public ListInventoriesService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * Lista os inventarios acessiveis ao usuario autenticado com paginacao por offset.
     * Estrategia: normaliza `limit`, valida o intervalo aceito, consulta a quantidade total acessivel
     * e retorna a pagina ordenada por `inv_id ASC`.
     * Efeitos colaterais: nenhum alem de leituras na base.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param limit limite opcional informado na query string.
     * @param offset offset opcional informado na query string.
     * @return pagina de inventarios acessiveis ao usuario autenticado.
     * @throws NotFoundException quando `limit` ou `offset` estao fora do intervalo aceito.
     */
    public ListInventoriesPage listInventories(
            AuthenticatedUserPrincipal authenticatedUser,
            Integer limit,
            Integer offset
    ) {
        int effectiveLimit = normalizeLimit(limit);
        int effectiveOffset = normalizeOffset(offset);
        int total = inventoryRepository.countAccessibleInventories(authenticatedUser.userId());

        if (total > 0 && effectiveOffset >= total) {
            throw new NotFoundException(
                    "Paginacao fora do intervalo aceito.",
                    "offset",
                    "offset after end of list"
            );
        }

        List<ListInventoriesItemResponse> inventories = inventoryRepository.listAccessibleInventories(
                        authenticatedUser.userId(),
                        effectiveLimit,
                        effectiveOffset
                )
                .stream()
                .map(this::toItemResponse)
                .toList();

        Integer nextOffset = effectiveOffset + inventories.size() < total
                ? effectiveOffset + effectiveLimit
                : null;

        return new ListInventoriesPage(inventories, nextOffset, effectiveLimit);
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
     * @param inventorySummary inventario acessivel retornado da camada JDBC.
     * @return item serializavel da resposta da API.
     */
    private ListInventoriesItemResponse toItemResponse(UserInventorySummary inventorySummary) {
        return new ListInventoriesItemResponse(
                inventorySummary.inventoryId(),
                inventorySummary.inventoryName(),
                inventorySummary.role()
        );
    }
}
