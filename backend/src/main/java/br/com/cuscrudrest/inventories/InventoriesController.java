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
import br.com.cuscrudrest.inventories.users.create.AddInventoryUserRequest;
import br.com.cuscrudrest.inventories.users.create.AddInventoryUserResponse;
import br.com.cuscrudrest.inventories.users.create.AddInventoryUserService;
import br.com.cuscrudrest.inventories.users.delete.DeleteInventoryUserService;
import br.com.cuscrudrest.inventories.users.list.ListInventoryUsersPage;
import br.com.cuscrudrest.inventories.users.list.ListInventoryUsersResponse;
import br.com.cuscrudrest.inventories.users.list.ListInventoryUsersService;
import br.com.cuscrudrest.inventories.users.update.UpdateInventoryUserRequest;
import br.com.cuscrudrest.inventories.users.update.UpdateInventoryUserResponse;
import br.com.cuscrudrest.inventories.users.update.UpdateInventoryUserService;
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

    private final AddInventoryUserService addInventoryUserService;
    private final CreateInventoryService createInventoryService;
    private final DeleteInventoryService deleteInventoryService;
    private final DeleteInventoryUserService deleteInventoryUserService;
    private final ListInventoriesService listInventoriesService;
    private final ListInventoryUsersService listInventoryUsersService;
    private final RenameInventoryService renameInventoryService;
    private final UpdateInventoryUserService updateInventoryUserService;

    /**
     * Cria o controller de inventarios.
     *
     * @param addInventoryUserService servico responsavel pela concessao de acesso a usuarios no inventario.
     * @param createInventoryService servico responsavel pela criacao de inventarios.
     * @param deleteInventoryService servico responsavel pela remocao de inventarios.
     * @param deleteInventoryUserService servico responsavel pela remocao de usuarios do inventario.
     * @param listInventoriesService servico responsavel pela listagem paginada de inventarios.
     * @param listInventoryUsersService servico responsavel pela listagem paginada de usuarios do inventario.
     * @param renameInventoryService servico responsavel pela renomeacao de inventarios.
     * @param updateInventoryUserService servico responsavel pela atualizacao de roles de usuarios no inventario.
     */
    public InventoriesController(
            AddInventoryUserService addInventoryUserService,
            CreateInventoryService createInventoryService,
            DeleteInventoryService deleteInventoryService,
            DeleteInventoryUserService deleteInventoryUserService,
            ListInventoriesService listInventoriesService,
            ListInventoryUsersService listInventoryUsersService,
            RenameInventoryService renameInventoryService,
            UpdateInventoryUserService updateInventoryUserService
    ) {
        this.addInventoryUserService = addInventoryUserService;
        this.createInventoryService = createInventoryService;
        this.deleteInventoryService = deleteInventoryService;
        this.deleteInventoryUserService = deleteInventoryUserService;
        this.listInventoriesService = listInventoriesService;
        this.listInventoryUsersService = listInventoryUsersService;
        this.renameInventoryService = renameInventoryService;
        this.updateInventoryUserService = updateInventoryUserService;
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
                buildNextPageUrl(request, page.nextOffset(), page.limit())
        );
    }

    /**
     * GET /api/v1/inventories/{inv_id}/users
     * Lista os usuarios com acesso ao inventario quando o usuario autenticado possui role owner no recurso.
     * Estrategia: delega a validacao de ownership e a consulta paginada ao servico de negocio e monta `next_page`.
     * Efeitos colaterais: nenhum alem da leitura da base.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param inventoryId identificador do inventario consultado.
     * @param limit limite opcional da pagina.
     * @param offset offset opcional da pagina.
     * @param request request HTTP atual usada para montar `next_page`.
     * @return lista paginada de usuarios com acesso ao inventario.
     */
    @GetMapping("/inventories/{inv_id}/users")
    public ListInventoryUsersResponse listInventoryUsers(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @PathVariable("inv_id") UUID inventoryId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "offset", required = false) Integer offset,
            HttpServletRequest request
    ) {
        ListInventoryUsersPage page = listInventoryUsersService.listInventoryUsers(
                authenticatedUser,
                inventoryId,
                limit,
                offset
        );
        return new ListInventoryUsersResponse(
                page.inventory(),
                page.users(),
                buildNextPageUrl(request, page.nextOffset(), page.limit())
        );
    }

    /**
     * POST /api/v1/inventories/{inv_id}/users
     * Adiciona um usuario existente ao inventario quando o usuario autenticado possui role owner no recurso.
     * Estrategia: valida o payload via Bean Validation, resolve o UUID do path e delega a concessao de acesso ao servico de negocio.
     * Efeitos colaterais: persiste um novo vinculo de acesso do usuario ao inventario.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param request payload HTTP com login do usuario e role a ser atribuida.
     * @return inventario e usuario vinculados com a role informada.
     */
    @PostMapping("/inventories/{inv_id}/users")
    @ResponseStatus(HttpStatus.CREATED)
    public AddInventoryUserResponse addInventoryUser(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @PathVariable("inv_id") UUID inventoryId,
            @Valid @RequestBody AddInventoryUserRequest request
    ) {
        return addInventoryUserService.addInventoryUser(authenticatedUser, inventoryId, request);
    }

    /**
     * PATCH /api/v1/inventories/{inv_id}/users/{user_id}
     * Atualiza a role de um usuario existente no inventario quando o usuario autenticado possui role owner no recurso.
     * Estrategia: valida o payload via Bean Validation, resolve os UUIDs do path e delega a alteracao ao servico de negocio.
     * Efeitos colaterais: persiste a nova role do usuario no inventario.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param userId identificador do usuario cuja role sera alterada.
     * @param request payload HTTP com a nova role.
     * @return inventario e usuario com a role atualizada.
     */
    @PatchMapping("/inventories/{inv_id}/users/{user_id}")
    public UpdateInventoryUserResponse updateInventoryUser(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @PathVariable("inv_id") UUID inventoryId,
            @PathVariable("user_id") UUID userId,
            @Valid @RequestBody UpdateInventoryUserRequest request
    ) {
        return updateInventoryUserService.updateInventoryUser(authenticatedUser, inventoryId, userId, request);
    }

    /**
     * DELETE /api/v1/inventories/{inv_id}/users/{user_id}
     * Remove o acesso de um usuario existente ao inventario quando o usuario autenticado possui role owner no recurso.
     * Estrategia: resolve os UUIDs do path e delega a remocao ao servico de negocio.
     * Efeitos colaterais: remove o vinculo de acesso do usuario ao inventario.
     *
     * @param authenticatedUser principal autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param userId identificador do usuario cujo acesso sera removido.
     */
    @DeleteMapping("/inventories/{inv_id}/users/{user_id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteInventoryUser(
            @AuthenticationPrincipal AuthenticatedUserPrincipal authenticatedUser,
            @PathVariable("inv_id") UUID inventoryId,
            @PathVariable("user_id") UUID userId
    ) {
        deleteInventoryUserService.deleteInventoryUser(authenticatedUser, inventoryId, userId);
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
