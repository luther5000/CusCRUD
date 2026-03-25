package br.com.cuscrudrest.inventories;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.create.CreateInventoryRequest;
import br.com.cuscrudrest.inventories.create.CreateInventoryResponse;
import br.com.cuscrudrest.inventories.create.CreateInventoryService;
import br.com.cuscrudrest.inventories.rename.RenameInventoryRequest;
import br.com.cuscrudrest.inventories.rename.RenameInventoryResponse;
import br.com.cuscrudrest.inventories.rename.RenameInventoryService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

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
    private final RenameInventoryService renameInventoryService;

    /**
     * Cria o controller de inventarios.
     *
     * @param createInventoryService servico responsavel pela criacao de inventarios.
     * @param renameInventoryService servico responsavel pela renomeacao de inventarios.
     */
    public InventoriesController(
            CreateInventoryService createInventoryService,
            RenameInventoryService renameInventoryService
    ) {
        this.createInventoryService = createInventoryService;
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
}
