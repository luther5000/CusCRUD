package br.com.cuscrudrest.types.list;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.types.TypeRepository;
import br.com.cuscrudrest.types.TypeSummary;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Servico de listagem paginada de tipos de um inventario.
 * Centraliza validacao de acesso, `limit`/`offset`, consulta ordenada e calculo de continuidade da paginacao.
 * Efeitos colaterais: nenhum alem de leituras na base.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class ListTypesService {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 200;

    private final InventoryAccessService inventoryAccessService;
    private final TypeRepository typeRepository;

    /**
     * Cria o servico de listagem de tipos.
     *
     * @param inventoryAccessService servico responsavel por validar acesso ao inventario.
     * @param typeRepository repositorio JDBC do dominio de tipos.
     */
    public ListTypesService(
            InventoryAccessService inventoryAccessService,
            TypeRepository typeRepository
    ) {
        this.inventoryAccessService = inventoryAccessService;
        this.typeRepository = typeRepository;
    }

    /**
     * Lista os tipos do inventario com paginacao por offset.
     * Estrategia: valida acesso ao inventario, normaliza `limit` e `offset`, consulta o total e retorna a pagina ordenada.
     * Efeitos colaterais: nenhum alem de leituras na base.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param inventoryId identificador do inventario consultado.
     * @param limit limite opcional informado na query string.
     * @param offset offset opcional informado na query string.
     * @return pagina de tipos do inventario informado.
     */
    public ListTypesPage listTypes(
            AuthenticatedUserPrincipal authenticatedUser,
            UUID inventoryId,
            Integer limit,
            Integer offset
    ) {
        inventoryAccessService.requireAnyAccess(inventoryId, authenticatedUser.userId());

        int effectiveLimit = normalizeLimit(limit);
        int effectiveOffset = normalizeOffset(offset);
        int total = typeRepository.countTypes(inventoryId);

        if (total > 0 && effectiveOffset >= total) {
            throw new NotFoundException(
                    "Paginacao fora do intervalo aceito.",
                    "offset",
                    "offset after end of list"
            );
        }

        List<ListTypesItemResponse> types = typeRepository.listTypes(inventoryId, effectiveLimit, effectiveOffset)
                .stream()
                .map(this::toItemResponse)
                .toList();

        Integer nextOffset = effectiveOffset + types.size() < total
                ? effectiveOffset + effectiveLimit
                : null;

        return new ListTypesPage(types, nextOffset, effectiveLimit);
    }

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

    private ListTypesItemResponse toItemResponse(TypeSummary typeSummary) {
        return new ListTypesItemResponse(
                typeSummary.typeId(),
                typeSummary.nome(),
                typeSummary.hasImage()
        );
    }
}
