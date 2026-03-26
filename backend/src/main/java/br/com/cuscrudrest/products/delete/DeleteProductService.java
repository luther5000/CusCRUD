package br.com.cuscrudrest.products.delete;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.products.ProductRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servico de remocao de produtos da aplicacao.
 * Centraliza a validacao de permissao de escrita e a existencia do produto antes do delete.
 * Efeitos colaterais: remove registros persistidos da tabela `products`.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class DeleteProductService {

    private final InventoryAccessService inventoryAccessService;
    private final ProductRepository productRepository;

    /**
     * Cria o servico de remocao de produtos.
     *
     * @param inventoryAccessService servico responsavel por validar acesso de escrita ao inventario.
     * @param productRepository repositorio JDBC do dominio de produtos.
     */
    public DeleteProductService(
            InventoryAccessService inventoryAccessService,
            ProductRepository productRepository
    ) {
        this.inventoryAccessService = inventoryAccessService;
        this.productRepository = productRepository;
    }

    /**
     * Remove um produto existente do inventario informado.
     * Estrategia: valida permissao de escrita, garante a existencia do produto e executa o delete na mesma transacao.
     * Efeitos colaterais: remove um produto persistido da base.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param productId identificador do produto a ser removido.
     */
    @Transactional
    public void deleteProduct(AuthenticatedUserPrincipal authenticatedUser, UUID inventoryId, long productId) {
        inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId());
        ensureProductExists(inventoryId, productId);
        productRepository.deleteProduct(inventoryId, productId);
    }

    /**
     * Garante que o produto existe para o inventario informado antes da remocao.
     *
     * @param inventoryId identificador do inventario alvo.
     * @param productId identificador do produto a ser removido.
     * @throws NotFoundException quando o produto nao existe para o inventario informado.
     */
    private void ensureProductExists(UUID inventoryId, long productId) {
        if (productRepository.findProductById(inventoryId, productId).isEmpty()) {
            throw new NotFoundException(
                    "Produto nao encontrado.",
                    "product_id",
                    "product not found"
            );
        }
    }
}
