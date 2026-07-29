package br.com.cuscrudrest.products.get;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.products.ProductRepository;
import br.com.cuscrudrest.products.ProductSummary;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Servico de leitura unitaria de produtos.
 * Centraliza a validacao de acesso ao inventario e a leitura do produto por `product_id`.
 * Efeitos colaterais: nenhum alem de leituras na base.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class GetProductService {

    private final InventoryAccessService inventoryAccessService;
    private final ProductRepository productRepository;

    /**
     * Cria o servico de leitura unitaria de produtos.
     *
     * @param inventoryAccessService servico responsavel por validar acesso ao inventario.
     * @param productRepository repositorio JDBC do dominio de produtos.
     */
    public GetProductService(
            InventoryAccessService inventoryAccessService,
            ProductRepository productRepository
    ) {
        this.inventoryAccessService = inventoryAccessService;
        this.productRepository = productRepository;
    }

    /**
     * Busca um produto especifico do inventario informado quando o usuario possui acesso ao recurso.
     * Estrategia: valida acesso ao inventario e consulta o produto por `inv_id` e `product_id`.
     * Efeitos colaterais: nenhum alem de leituras na base.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param inventoryId identificador do inventario consultado.
     * @param productId identificador do produto a ser retornado.
     * @return dados do produto encontrado.
     */
    public GetProductResponse getProduct(
            AuthenticatedUserPrincipal authenticatedUser,
            UUID inventoryId,
            long productId
    ) {
        inventoryAccessService.requireAnyAccess(inventoryId, authenticatedUser.userId());

        ProductSummary product = productRepository.findProductById(inventoryId, productId)
                .orElseThrow(() -> new NotFoundException(
                        "Produto nao encontrado.",
                        "product_id",
                        "product not found"
                ));

        return new GetProductResponse(
                product.productId(),
                product.typeId(),
                product.marca(),
                product.dataValidade(),
                product.unidade(),
                product.unidadeMedida(),
                product.quantidade(),
                product.inventoryId()
        );
    }
}
