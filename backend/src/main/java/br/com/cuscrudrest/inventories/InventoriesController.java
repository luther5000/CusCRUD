package br.com.cuscrudrest.inventories;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.create.CreateInventoryRequest;
import br.com.cuscrudrest.inventories.create.CreateInventoryResponse;
import br.com.cuscrudrest.inventories.create.CreateInventoryService;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller HTTP dos endpoints de inventarios.
 * Exponde operacoes protegidas de gerenciamento de inventarios baseadas no usuario autenticado.
 * Efeitos colaterais: cria e altera inventarios persistidos conforme os casos de uso implementados.
 */
@RestController
@Conditional(DatabaseConfiguredCondition.class)
public class InventoriesController {

    private final CreateInventoryService createInventoryService;

    /**
     * Cria o controller de inventarios.
     *
     * @param createInventoryService servico responsavel pela criacao de inventarios.
     */
    public InventoriesController(CreateInventoryService createInventoryService) {
        this.createInventoryService = createInventoryService;
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
}
