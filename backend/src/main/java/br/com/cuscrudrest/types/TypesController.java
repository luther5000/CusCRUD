package br.com.cuscrudrest.types;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.types.create.CreateTypeRequest;
import br.com.cuscrudrest.types.create.CreateTypeResponse;
import br.com.cuscrudrest.types.create.CreateTypeService;
import br.com.cuscrudrest.types.get.GetTypeResponse;
import br.com.cuscrudrest.types.get.GetTypeService;
import br.com.cuscrudrest.types.list.ListTypesPage;
import br.com.cuscrudrest.types.list.ListTypesResponse;
import br.com.cuscrudrest.types.list.ListTypesService;
import br.com.cuscrudrest.types.update.UpdateTypeRequest;
import br.com.cuscrudrest.types.update.UpdateTypeResponse;
import br.com.cuscrudrest.types.update.UpdateTypeService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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

    private final CreateTypeService createTypeService;
    private final GetTypeService getTypeService;
    private final ListTypesService listTypesService;
    private final UpdateTypeService updateTypeService;

    /**
     * Cria o controller de tipos.
     *
     * @param createTypeService servico responsavel pela criacao de tipos.
     * @param getTypeService servico responsavel pela leitura unitaria de tipos.
     * @param listTypesService servico responsavel pela listagem paginada de tipos.
     * @param updateTypeService servico responsavel pela atualizacao parcial de tipos.
     */
    public TypesController(
            CreateTypeService createTypeService,
            GetTypeService getTypeService,
            ListTypesService listTypesService,
            UpdateTypeService updateTypeService
    ) {
        this.createTypeService = createTypeService;
        this.getTypeService = getTypeService;
        this.listTypesService = listTypesService;
        this.updateTypeService = updateTypeService;
    }

    /**
     * POST /api/v1/inventories/{inv_id}/types
     * Cria um novo tipo no inventario quando o usuario autenticado possui permissao de escrita no recurso.
     * Estrategia: valida o payload via Bean Validation, resolve o UUID do path e delega a criacao ao servico de negocio.
     * Efeitos colaterais: persiste um novo tipo associado ao inventario informado.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param request payload HTTP com nome e imagem opcional do tipo.
     * @return tipo criado com `type_id` gerado.
     */
    @PostMapping("/inventories/{inv_id}/types")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateTypeResponse createType(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @PathVariable("inv_id") UUID inventoryId,
            @Valid @RequestBody CreateTypeRequest request
    ) {
        return createTypeService.createType(authenticatedUser, inventoryId, request);
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
     * GET /api/v1/inventories/{inv_id}/types/{type_id}
     * Retorna um tipo especifico do inventario quando o usuario autenticado possui algum acesso ao recurso.
     * Estrategia: resolve os identificadores do path e delega a leitura unitaria ao servico de negocio.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param inventoryId identificador do inventario consultado.
     * @param typeId identificador do tipo a ser retornado.
     * @return tipo encontrado no inventario informado.
     */
    @GetMapping("/inventories/{inv_id}/types/{type_id}")
    public GetTypeResponse getType(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @PathVariable("inv_id") UUID inventoryId,
            @PathVariable("type_id") long typeId
    ) {
        return getTypeService.getType(authenticatedUser, inventoryId, typeId);
    }

    /**
     * PATCH /api/v1/inventories/{inv_id}/types/{type_id}
     * Atualiza parcialmente um tipo existente quando o usuario autenticado possui permissao de escrita no inventario.
     * Estrategia: resolve os identificadores do path e delega a aplicacao do patch ao servico de negocio.
     * Efeitos colaterais: persiste alteracoes parciais do tipo informado.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param typeId identificador do tipo a ser atualizado.
     * @param request payload parcial do patch.
     * @return estado final persistido do tipo apos a atualizacao.
     */
    @PatchMapping("/inventories/{inv_id}/types/{type_id}")
    public UpdateTypeResponse updateType(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @PathVariable("inv_id") UUID inventoryId,
            @PathVariable("type_id") long typeId,
            @RequestBody UpdateTypeRequest request
    ) {
        return updateTypeService.updateType(authenticatedUser, inventoryId, typeId, request);
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
