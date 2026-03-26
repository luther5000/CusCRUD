package br.com.cuscrudrest.products.listbytype;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.products.ProductRepository;
import br.com.cuscrudrest.products.ProductSummary;
import br.com.cuscrudrest.products.list.ListProductsItemResponse;
import br.com.cuscrudrest.products.list.ListProductsPage;
import br.com.cuscrudrest.types.TypeRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Servico de listagem paginada de produtos filtrados por tipo.
 * Centraliza validacao de acesso ao inventario, existencia do tipo, `limit`/`offset` e calculo de continuidade da paginacao.
 * Efeitos colaterais: nenhum alem de leituras na base.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class ListProductsByTypeService {

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 200;

    private final InventoryAccessService inventoryAccessService;
    private final ProductRepository productRepository;
    private final TypeRepository typeRepository;

    /**
     * Cria o servico de listagem de produtos filtrados por tipo.
     *
     * @param inventoryAccessService servico responsavel por validar acesso ao inventario.
     * @param productRepository repositorio JDBC do dominio de produtos.
     * @param typeRepository repositorio JDBC do dominio de tipos.
     */
    public ListProductsByTypeService(
            InventoryAccessService inventoryAccessService,
            ProductRepository productRepository,
            TypeRepository typeRepository
    ) {
        this.inventoryAccessService = inventoryAccessService;
        this.productRepository = productRepository;
        this.typeRepository = typeRepository;
    }

    /**
     * Lista os produtos do inventario filtrados por um tipo especifico com paginacao por offset.
     * Estrategia: valida acesso ao inventario, garante que o tipo pertence ao inventario, normaliza a paginacao e retorna a pagina ordenada.
     * Efeitos colaterais: nenhum alem de leituras na base.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param inventoryId identificador do inventario consultado.
     * @param typeId identificador do tipo pelo qual os produtos serao filtrados.
     * @param limit limite opcional informado na query string.
     * @param offset offset opcional informado na query string.
     * @return pagina de produtos do tipo informado no inventario.
     */
    public ListProductsPage listProductsByType(
            AuthenticatedUserPrincipal authenticatedUser,
            UUID inventoryId,
            long typeId,
            Integer limit,
            Integer offset
    ) {
        inventoryAccessService.requireAnyAccess(inventoryId, authenticatedUser.userId());

        typeRepository.findTypeById(inventoryId, typeId)
                .orElseThrow(() -> new NotFoundException(
                        "Tipo nao encontrado.",
                        "type_id",
                        "type not found"
                ));

        int effectiveLimit = normalizeLimit(limit);
        int effectiveOffset = normalizeOffset(offset);
        int total = productRepository.countProductsByType(inventoryId, typeId);

        if (total > 0 && effectiveOffset >= total) {
            throw new NotFoundException(
                    "Paginacao fora do intervalo aceito.",
                    "offset",
                    "offset after end of list"
            );
        }

        List<ListProductsItemResponse> products = productRepository.listProductsByType(
                        inventoryId,
                        typeId,
                        effectiveLimit,
                        effectiveOffset
                )
                .stream()
                .map(this::toItemResponse)
                .toList();

        Integer nextOffset = effectiveOffset + products.size() < total
                ? effectiveOffset + effectiveLimit
                : null;

        return new ListProductsPage(products, nextOffset, effectiveLimit);
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

    private ListProductsItemResponse toItemResponse(ProductSummary productSummary) {
        return new ListProductsItemResponse(
                productSummary.productId(),
                productSummary.typeId(),
                productSummary.marca(),
                productSummary.dataValidade(),
                productSummary.unidade(),
                productSummary.unidadeMedida(),
                productSummary.quantidade(),
                productSummary.inventoryId()
        );
    }
}
