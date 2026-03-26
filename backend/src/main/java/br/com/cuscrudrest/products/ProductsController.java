package br.com.cuscrudrest.products;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.products.create.CreateProductRequest;
import br.com.cuscrudrest.products.create.CreateProductResponse;
import br.com.cuscrudrest.products.create.CreateProductService;
import br.com.cuscrudrest.products.delete.DeleteProductService;
import br.com.cuscrudrest.products.get.GetProductResponse;
import br.com.cuscrudrest.products.get.GetProductService;
import br.com.cuscrudrest.products.list.ListProductsPage;
import br.com.cuscrudrest.products.list.ListProductsResponse;
import br.com.cuscrudrest.products.list.ListProductsService;
import br.com.cuscrudrest.products.listbytype.ListProductsByTypeService;
import br.com.cuscrudrest.products.update.UpdateProductRequest;
import br.com.cuscrudrest.products.update.UpdateProductResponse;
import br.com.cuscrudrest.products.update.UpdateProductService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

/**
 * Controller HTTP dos endpoints de produtos.
 * Expoe operacoes de leitura e escrita de produtos associados a inventarios.
 * Efeitos colaterais: delega aos casos de uso a consulta e persistencia dos recursos.
 */
@RestController
@Conditional(DatabaseConfiguredCondition.class)
public class ProductsController {

    private final CreateProductService createProductService;
    private final DeleteProductService deleteProductService;
    private final GetProductService getProductService;
    private final ListProductsByTypeService listProductsByTypeService;
    private final ListProductsService listProductsService;
    private final UpdateProductService updateProductService;

    /**
     * Cria o controller de produtos.
     *
     * @param createProductService servico responsavel pela criacao de produtos.
     * @param deleteProductService servico responsavel pela remocao de produtos.
     * @param getProductService servico responsavel pela leitura unitaria de produtos.
     * @param listProductsByTypeService servico responsavel pela listagem paginada de produtos filtrados por tipo.
     * @param listProductsService servico responsavel pela listagem paginada de produtos.
     * @param updateProductService servico responsavel pela atualizacao parcial de produtos.
     */
    public ProductsController(
            CreateProductService createProductService,
            DeleteProductService deleteProductService,
            GetProductService getProductService,
            ListProductsByTypeService listProductsByTypeService,
            ListProductsService listProductsService,
            UpdateProductService updateProductService
    ) {
        this.createProductService = createProductService;
        this.deleteProductService = deleteProductService;
        this.getProductService = getProductService;
        this.listProductsByTypeService = listProductsByTypeService;
        this.listProductsService = listProductsService;
        this.updateProductService = updateProductService;
    }

    /**
     * GET /api/v1/inventories/{inv_id}/products
     * Lista os produtos do inventario quando o usuario autenticado possui algum acesso ao recurso.
     * Estrategia: delega validacao e consulta paginada ao servico de negocio e monta `next_page`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param inventoryId identificador do inventario consultado.
     * @param limit limite opcional da pagina.
     * @param offset offset opcional da pagina.
     * @param request request HTTP atual usada para montar `next_page`.
     * @return lista paginada de produtos do inventario.
     */
    @GetMapping("/inventories/{inv_id}/products")
    public ListProductsResponse listProducts(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @PathVariable("inv_id") UUID inventoryId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            HttpServletRequest request
    ) {
        ListProductsPage page = listProductsService.listProducts(authenticatedUser, inventoryId, limit, offset);
        return new ListProductsResponse(
                page.products(),
                buildNextPageUrl(request, page.nextOffset(), page.limit())
        );
    }

    /**
     * POST /api/v1/inventories/{inv_id}/products
     * Cria um novo produto no inventario quando o usuario autenticado possui permissao de escrita no recurso.
     * Estrategia: valida o payload via Bean Validation, resolve o UUID do path e delega a criacao ao servico de negocio.
     * Efeitos colaterais: persiste um novo produto associado ao inventario informado.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param request payload HTTP com os dados do produto a ser criado.
     * @return produto criado com `product_id` gerado.
     */
    @PostMapping("/inventories/{inv_id}/products")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateProductResponse createProduct(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @PathVariable("inv_id") UUID inventoryId,
            @Valid @RequestBody CreateProductRequest request
    ) {
        return createProductService.createProduct(authenticatedUser, inventoryId, request);
    }

    /**
     * GET /api/v1/inventories/{inv_id}/products/{product_id}
     * Retorna um produto especifico do inventario quando o usuario autenticado possui algum acesso ao recurso.
     * Estrategia: resolve os identificadores do path e delega a leitura unitaria ao servico de negocio.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param inventoryId identificador do inventario consultado.
     * @param productId identificador do produto a ser retornado.
     * @return produto encontrado no inventario informado.
     */
    @GetMapping("/inventories/{inv_id}/products/{product_id}")
    public GetProductResponse getProduct(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @PathVariable("inv_id") UUID inventoryId,
            @PathVariable("product_id") long productId
    ) {
        return getProductService.getProduct(authenticatedUser, inventoryId, productId);
    }

    /**
     * PATCH /api/v1/inventories/{inv_id}/products/{product_id}
     * Atualiza parcialmente um produto do inventario quando o usuario autenticado possui permissao de escrita no recurso.
     * Estrategia: resolve os identificadores do path e delega o merge do patch ao servico de negocio.
     * Efeitos colaterais: persiste alteracoes no produto informado.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param productId identificador do produto a ser atualizado.
     * @param request payload parcial do patch.
     * @return produto atualizado com o estado persistido.
     */
    @PatchMapping("/inventories/{inv_id}/products/{product_id}")
    public UpdateProductResponse updateProduct(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @PathVariable("inv_id") UUID inventoryId,
            @PathVariable("product_id") long productId,
            @RequestBody UpdateProductRequest request
    ) {
        return updateProductService.updateProduct(authenticatedUser, inventoryId, productId, request);
    }

    /**
     * DELETE /api/v1/inventories/{inv_id}/products/{product_id}
     * Remove um produto do inventario quando o usuario autenticado possui permissao de escrita no recurso.
     * Estrategia: resolve os identificadores do path e delega a remocao ao servico de negocio.
     * Efeitos colaterais: remove o produto persistido correspondente.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param productId identificador do produto a ser removido.
     */
    @DeleteMapping("/inventories/{inv_id}/products/{product_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @PathVariable("inv_id") UUID inventoryId,
            @PathVariable("product_id") long productId
    ) {
        deleteProductService.deleteProduct(authenticatedUser, inventoryId, productId);
    }

    /**
     * GET /api/v1/inventories/{inv_id}/types/{type_id}/products
     * Lista os produtos de um tipo especifico quando o usuario autenticado possui algum acesso ao inventario.
     * Estrategia: delega validacao do inventario, do tipo e a consulta paginada ao servico de negocio, montando `next_page`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param inventoryId identificador do inventario consultado.
     * @param typeId identificador do tipo pelo qual os produtos serao filtrados.
     * @param limit limite opcional da pagina.
     * @param offset offset opcional da pagina.
     * @param request request HTTP atual usada para montar `next_page`.
     * @return lista paginada de produtos do tipo informado.
     */
    @GetMapping("/inventories/{inv_id}/types/{type_id}/products")
    public ListProductsResponse listProductsByType(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @PathVariable("inv_id") UUID inventoryId,
            @PathVariable("type_id") long typeId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            HttpServletRequest request
    ) {
        ListProductsPage page = listProductsByTypeService.listProductsByType(
                authenticatedUser,
                inventoryId,
                typeId,
                limit,
                offset
        );
        return new ListProductsResponse(
                page.products(),
                buildNextPageUrl(request, page.nextOffset(), page.limit())
        );
    }

    /**
     * Monta a URL absoluta da proxima pagina a partir da request atual.
     *
     * @param request request HTTP atual.
     * @param nextOffset proximo offset, quando houver.
     * @param limit limite efetivo usado na consulta.
     * @return URL absoluta da proxima pagina, ou `null` quando nao houver mais resultados.
     */
    private String buildNextPageUrl(HttpServletRequest request, Integer nextOffset, int limit) {
        if (nextOffset == null) {
            return null;
        }

        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replaceQueryParam("offset", nextOffset)
                .replaceQueryParam("limit", limit)
                .build()
                .toUriString();
    }
}
