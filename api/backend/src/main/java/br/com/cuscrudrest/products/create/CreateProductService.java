package br.com.cuscrudrest.products.create;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryAccessContext;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.products.ProductRepository;
import br.com.cuscrudrest.products.ProductSummary;
import br.com.cuscrudrest.types.TypeRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Servico de criacao de produtos da aplicacao.
 * Centraliza a validacao de permissao de escrita, existencia do tipo no inventario e a persistencia do produto.
 * Efeitos colaterais: cria registros persistidos na tabela `products`.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class CreateProductService {

    private final InventoryAccessService inventoryAccessService;
    private final ProductRepository productRepository;
    private final TypeRepository typeRepository;

    /**
     * Cria o servico de criacao de produtos.
     *
     * @param inventoryAccessService servico responsavel por validar acesso de escrita ao inventario.
     * @param productRepository repositorio JDBC do dominio de produtos.
     * @param typeRepository repositorio JDBC do dominio de tipos.
     */
    public CreateProductService(
            InventoryAccessService inventoryAccessService,
            ProductRepository productRepository,
            TypeRepository typeRepository
    ) {
        this.inventoryAccessService = inventoryAccessService;
        this.productRepository = productRepository;
        this.typeRepository = typeRepository;
    }

    /**
     * Cria um novo produto no inventario informado.
     * Estrategia: valida permissao de escrita, garante que o tipo pertence ao inventario e persiste o produto na mesma transacao.
     * Efeitos colaterais: cria um novo registro na tabela `products`.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param request payload com os dados do produto a ser criado.
     * @return resposta HTTP com o produto criado.
     */
    @Transactional
    public CreateProductResponse createProduct(
            AuthenticatedUserPrincipal authenticatedUser,
            UUID inventoryId,
            CreateProductRequest request
    ) {
        InventoryAccessContext accessContext = inventoryAccessService.requireWriteAccess(
                inventoryId,
                authenticatedUser.userId()
        );

        typeRepository.findTypeById(accessContext.inventoryId(), request.typeId())
                .orElseThrow(() -> new NotFoundException(
                        "Tipo nao encontrado.",
                        "type_id",
                        "type not found"
                ));

        ProductSummary createdProduct = productRepository.createProduct(
                accessContext.inventoryId(),
                request.typeId(),
                request.marca(),
                request.dataValidade(),
                request.unidade(),
                request.unidadeMedida(),
                request.quantidade() != null ? request.quantidade() : 0L
        );

        return new CreateProductResponse(
                createdProduct.productId(),
                createdProduct.typeId(),
                createdProduct.marca(),
                createdProduct.dataValidade(),
                createdProduct.unidade(),
                createdProduct.unidadeMedida(),
                createdProduct.quantidade(),
                createdProduct.inventoryId()
        );
    }
}
