package br.com.cuscrudrest.products.update;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.common.error.ValidationException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.products.ProductRepository;
import br.com.cuscrudrest.products.ProductSummary;
import br.com.cuscrudrest.types.TypeRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Servico de atualizacao parcial de produtos da aplicacao.
 * Centraliza a validacao de permissao de escrita, merge do patch e consistencia do `type_id` no mesmo inventario.
 * Efeitos colaterais: atualiza registros persistidos na tabela `products`.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class UpdateProductService {

    private final InventoryAccessService inventoryAccessService;
    private final ProductRepository productRepository;
    private final TypeRepository typeRepository;

    /**
     * Cria o servico de atualizacao de produtos.
     *
     * @param inventoryAccessService servico responsavel por validar acesso de escrita ao inventario.
     * @param productRepository repositorio JDBC do dominio de produtos.
     * @param typeRepository repositorio JDBC do dominio de tipos.
     */
    public UpdateProductService(
            InventoryAccessService inventoryAccessService,
            ProductRepository productRepository,
            TypeRepository typeRepository
    ) {
        this.inventoryAccessService = inventoryAccessService;
        this.productRepository = productRepository;
        this.typeRepository = typeRepository;
    }

    /**
     * Atualiza parcialmente um produto existente do inventario informado.
     * Estrategia: valida permissao de escrita, garante existencia do produto, aplica merge campo a campo e persiste o estado final.
     * Efeitos colaterais: atualiza um registro na tabela `products`.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param productId identificador do produto a ser atualizado.
     * @param request payload parcial do patch.
     * @return resposta HTTP com o produto atualizado.
     */
    @Transactional
    public UpdateProductResponse updateProduct(
            AuthenticatedUserPrincipal authenticatedUser,
            UUID inventoryId,
            long productId,
            UpdateProductRequest request
    ) {
        inventoryAccessService.requireWriteAccess(inventoryId, authenticatedUser.userId());
        ensurePatchHasAtLeastOneField(request);

        ProductSummary existingProduct = productRepository.findProductById(inventoryId, productId)
                .orElseThrow(() -> new NotFoundException(
                        "Produto nao encontrado.",
                        "product_id",
                        "product not found"
                ));

        long updatedTypeId = resolveUpdatedTypeId(request.typeId(), existingProduct.typeId(), inventoryId);
        String updatedMarca = resolveUpdatedStringField(request.marca(), "marca", existingProduct.marca(), 255, true);
        OffsetDateTime updatedDataValidade = resolveUpdatedDateTime(
                request.dataValidade(),
                existingProduct.dataValidade()
        );
        Long updatedUnidade = resolveUpdatedLongField(request.unidade(), "unidade", existingProduct.unidade(), true);
        String updatedUnidadeMedida = resolveUpdatedStringField(
                request.unidadeMedida(),
                "unidadeMedida",
                existingProduct.unidadeMedida(),
                255,
                true
        );
        long updatedQuantidade = resolveUpdatedRequiredLongField(
                request.quantidade(),
                "quantidade",
                existingProduct.quantidade()
        );

        ProductSummary updatedProduct = productRepository.updateProduct(
                inventoryId,
                productId,
                updatedTypeId,
                updatedMarca,
                updatedDataValidade,
                updatedUnidade,
                updatedUnidadeMedida,
                updatedQuantidade
        );

        return new UpdateProductResponse(
                updatedProduct.productId(),
                updatedProduct.typeId(),
                updatedProduct.marca(),
                updatedProduct.dataValidade(),
                updatedProduct.unidade(),
                updatedProduct.unidadeMedida(),
                updatedProduct.quantidade(),
                updatedProduct.inventoryId()
        );
    }

    private void ensurePatchHasAtLeastOneField(UpdateProductRequest request) {
        if (request.isEmpty()) {
            throw new ValidationException(
                    "Payload invalido.",
                    "payload",
                    "at least one field must be provided"
            );
        }
    }

    private long resolveUpdatedTypeId(JsonNode typeIdNode, long currentTypeId, UUID inventoryId) {
        if (typeIdNode == null) {
            return currentTypeId;
        }

        if (typeIdNode.isNull() || !typeIdNode.canConvertToLong()) {
            throw new ValidationException(
                    "Tipo invalido.",
                    "type_id",
                    "must be a valid int64"
            );
        }

        long typeId = typeIdNode.longValue();
        typeRepository.findTypeById(inventoryId, typeId)
                .orElseThrow(() -> new NotFoundException(
                        "Tipo nao encontrado.",
                        "type_id",
                        "type not found"
                ));
        return typeId;
    }

    private String resolveUpdatedStringField(
            JsonNode node,
            String campo,
            String currentValue,
            int maxLength,
            boolean nullable
    ) {
        if (node == null) {
            return currentValue;
        }

        if (node.isNull()) {
            if (nullable) {
                return null;
            }
            throw new ValidationException("Campo invalido.", campo, "must not be null");
        }

        if (!node.isTextual()) {
            throw new ValidationException(
                    "Campo invalido.",
                    campo,
                    "must have at most %d characters".formatted(maxLength)
            );
        }

        String value = node.asText();
        if (value.length() > maxLength) {
            throw new ValidationException(
                    "Campo invalido.",
                    campo,
                    "must have at most %d characters".formatted(maxLength)
            );
        }

        return value;
    }

    private OffsetDateTime resolveUpdatedDateTime(JsonNode node, OffsetDateTime currentValue) {
        if (node == null) {
            return currentValue;
        }

        if (node.isNull()) {
            return null;
        }

        if (!node.isTextual()) {
            throw new ValidationException(
                    "Data de validade invalida.",
                    "dataValidade",
                    "invalid field format"
            );
        }

        try {
            return OffsetDateTime.parse(node.asText());
        } catch (DateTimeParseException exception) {
            throw new ValidationException(
                    "Data de validade invalida.",
                    "dataValidade",
                    "invalid field format"
            );
        }
    }

    private Long resolveUpdatedLongField(JsonNode node, String campo, Long currentValue, boolean nullable) {
        if (node == null) {
            return currentValue;
        }

        if (node.isNull()) {
            if (nullable) {
                return null;
            }
            throw new ValidationException("Campo invalido.", campo, "must not be null");
        }

        if (!node.canConvertToLong()) {
            throw new ValidationException(
                    "Campo invalido.",
                    campo,
                    "must be greater than or equal to 0"
            );
        }

        long value = node.longValue();
        if (value < 0) {
            throw new ValidationException(
                    "Campo invalido.",
                    campo,
                    "must be greater than or equal to 0"
            );
        }

        return value;
    }

    private long resolveUpdatedRequiredLongField(JsonNode node, String campo, long currentValue) {
        Long resolved = resolveUpdatedLongField(node, campo, currentValue, false);
        if (resolved == null) {
            throw new ValidationException("Campo invalido.", campo, "must not be null");
        }
        return resolved;
    }
}
