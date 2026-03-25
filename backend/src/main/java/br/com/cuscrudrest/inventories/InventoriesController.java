package br.com.cuscrudrest.inventories;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.create.CreateInventoryRequest;
import br.com.cuscrudrest.inventories.create.CreateInventoryResponse;
import br.com.cuscrudrest.inventories.create.CreateInventoryService;
import br.com.cuscrudrest.inventories.delete.DeleteInventoryService;
import br.com.cuscrudrest.inventories.list.ListInventoriesPage;
import br.com.cuscrudrest.inventories.list.ListInventoriesResponse;
import br.com.cuscrudrest.inventories.list.ListInventoriesService;
import br.com.cuscrudrest.inventories.rename.RenameInventoryRequest;
import br.com.cuscrudrest.inventories.rename.RenameInventoryResponse;
import br.com.cuscrudrest.inventories.rename.RenameInventoryService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Conditional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.UUID;

/**
 * Controller HTTP dos endpoints de inventarios.
 * Exponde operacoes protegidas de gerenciamento de inventarios baseadas no usuario autenticado.
 * Efeitos colaterais: cria e altera inventarios persistidos conforme os casos de uso implementados.
 */
@RestController
@Conditional(DatabaseConfiguredCondition.class)
public class InventoriesController {

    private final CreateInventoryService createInventoryService;
    private final DeleteInventoryService deleteInventoryService;
    private final ListInventoriesService listInventoriesService;
    private final RenameInventoryService renameInventoryService;

    /**
     * Cria o controller de inventarios.
     *
     * @param createInventoryService servico responsavel pela criacao de inventarios.
     * @param deleteInventoryService servico responsavel pela remocao de inventarios.
     * @param listInventoriesService servico responsavel pela listagem paginada de inventarios.
     * @param renameInventoryService servico responsavel pela renomeacao de inventarios.
     */
    public InventoriesController(
            CreateInventoryService createInventoryService,
            DeleteInventoryService deleteInventoryService,
            ListInventoriesService listInventoriesService,
            RenameInventoryService renameInventoryService
    ) {
        this.createInventoryService = createInventoryService;
        this.deleteInventoryService = deleteInventoryService;
        this.listInventoriesService = listInventoriesService;
        this.renameInventoryService = renameInventoryService;
    }

    /**
     * POST /api/v1/inventories
     * Cria um novo inventario e concede ao usuario autenticado a funcao de owner.
     * Estrategia: valida o payload via Bean Validation e delega a criacao ao servico de negocio.
     * Efeitos colaterais: persiste um novo inventario e o vinculo de acesso owner na base.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param request payload HTTP com o nome do inventario.
     * @return inventario criado e o papel owner atribuido ao usuario autenticado.
     */
    @PostMapping("/inventories")
    @ResponseStatus(HttpStatus.CREATED)
    public CreateInventoryResponse createInventory(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @Valid @RequestBody CreateInventoryRequest request
    ) {
        return createInventoryService.createInventory(authenticatedUser, request);
    }

    /**
     * PATCH /api/v1/inventories/{inv_id}
     * Renomeia um inventario existente quando o usuario autenticado possui role owner no recurso.
     * Estrategia: valida o payload via Bean Validation, resolve o UUID do path e delega a renomeacao ao servico de negocio.
     * Efeitos colaterais: atualiza o nome persistido do inventario informado.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param inventoryId identificador do inventario a ser renomeado.
     * @param request payload HTTP com o novo nome do inventario.
     * @return inventario atualizado e a role owner do usuario autenticado.
     */
    @PatchMapping("/inventories/{inv_id}")
    public RenameInventoryResponse renameInventory(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @PathVariable("inv_id") UUID inventoryId,
            @Valid @RequestBody RenameInventoryRequest request
    ) {
        return renameInventoryService.renameInventory(authenticatedUser, inventoryId, request);
    }

    /**
     * DELETE /api/v1/inventories/{inv_id}
     * Remove um inventario existente quando o usuario autenticado possui role owner no recurso.
     * Estrategia: resolve o UUID do path e delega a exclusao ao servico de negocio.
     * Efeitos colaterais: remove o inventario persistido informado.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param inventoryId identificador do inventario a ser removido.
     */
    @DeleteMapping("/inventories/{inv_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInventory(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @PathVariable("inv_id") UUID inventoryId
    ) {
        deleteInventoryService.deleteInventory(authenticatedUser, inventoryId);
    }

    /**
     * GET /api/v1/inventories
     * Lista os inventarios acessiveis ao usuario autenticado com paginacao por `limit` e `offset`.
     * Estrategia: delega a validacao e consulta ao servico de negocio e monta `next_page` a partir da request atual.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param limit limite opcional da pagina.
     * @param offset offset opcional da pagina.
     * @param request request HTTP atual usada para montar `next_page`.
     * @return lista paginada dos inventarios acessiveis ao usuario autenticado.
     */
    @GetMapping("/inventories")
    public ListInventoriesResponse listInventories(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            HttpServletRequest request
    ) {
        ListInventoriesPage page = listInventoriesService.listInventories(authenticatedUser, limit, offset);
        return new ListInventoriesResponse(
                page.inventories(),
                buildNextPageUrl(request, page)
        );
    }

    /**
     * Monta a URL absoluta da proxima pagina a partir da request atual.
     *
     * @param request request HTTP atual.
     * @param page pagina retornada pelo servico de negocio.
     * @return URL absoluta da proxima pagina, ou `null` quando nao houver mais resultados.
     */
    private String buildNextPageUrl(HttpServletRequest request, ListInventoriesPage page) {
        if (page.nextOffset() == null) {
            return null;
        }

        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replaceQueryParam("offset", page.nextOffset())
                .replaceQueryParam("limit", page.limit())
                .build()
                .toUriString();
    }
}
