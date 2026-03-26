package br.com.cuscrudrest.products;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.products.list.ListProductsPage;
import br.com.cuscrudrest.products.list.ListProductsResponse;
import br.com.cuscrudrest.products.list.ListProductsService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Conditional;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

/**
 * Controller HTTP dos endpoints de produtos.
 * Expoe operacoes de leitura de produtos associados a inventarios.
 * Efeitos colaterais: nenhum alem dos efeitos dos casos de uso delegados.
 */
@RestController
@Conditional(DatabaseConfiguredCondition.class)
public class ProductsController {

    private final ListProductsService listProductsService;

    /**
     * Cria o controller de produtos.
     *
     * @param listProductsService servico responsavel pela listagem paginada de produtos.
     */
    public ProductsController(ListProductsService listProductsService) {
        this.listProductsService = listProductsService;
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
