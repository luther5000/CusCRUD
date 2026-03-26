package br.com.cuscrudrest.types;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.types.list.ListTypesPage;
import br.com.cuscrudrest.types.list.ListTypesResponse;
import br.com.cuscrudrest.types.list.ListTypesService;
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
 * Controller HTTP dos endpoints de tipos.
 * Exponde operacoes de leitura e manutencao de tipos associados a inventarios.
 * Efeitos colaterais: nenhum alem dos efeitos dos casos de uso delegados.
 */
@RestController
@Conditional(DatabaseConfiguredCondition.class)
public class TypesController {

    private final ListTypesService listTypesService;

    /**
     * Cria o controller de tipos.
     *
     * @param listTypesService servico responsavel pela listagem paginada de tipos.
     */
    public TypesController(ListTypesService listTypesService) {
        this.listTypesService = listTypesService;
    }

    /**
     * GET /api/v1/inventories/{inv_id}/types
     * Lista os tipos do inventario quando o usuario autenticado possui algum acesso ao recurso.
     * Estrategia: delega validacao e consulta paginada ao servico de negocio e monta `next_page`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param inventoryId identificador do inventario consultado.
     * @param limit limite opcional da pagina.
     * @param offset offset opcional da pagina.
     * @param request request HTTP atual usada para montar `next_page`.
     * @return lista paginada de tipos do inventario.
     */
    @GetMapping("/inventories/{inv_id}/types")
    public ListTypesResponse listTypes(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @PathVariable("inv_id") UUID inventoryId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            HttpServletRequest request
    ) {
        ListTypesPage page = listTypesService.listTypes(authenticatedUser, inventoryId, limit, offset);
        return new ListTypesResponse(
                page.types(),
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
