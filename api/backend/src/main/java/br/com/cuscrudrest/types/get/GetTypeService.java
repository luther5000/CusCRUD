package br.com.cuscrudrest.types.get;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.NotFoundException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.types.TypeDetails;
import br.com.cuscrudrest.types.TypeImageCodec;
import br.com.cuscrudrest.types.TypeRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Servico de leitura unitaria de tipos.
 * Centraliza a validacao de acesso ao inventario e a serializacao da imagem para o formato da API.
 * Efeitos colaterais: nenhum alem de leituras na base.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class GetTypeService {

    private final InventoryAccessService inventoryAccessService;
    private final TypeRepository typeRepository;

    /**
     * Cria o servico de leitura unitaria de tipos.
     *
     * @param inventoryAccessService servico responsavel por validar acesso ao inventario.
     * @param typeRepository repositorio JDBC do dominio de tipos.
     */
    public GetTypeService(
            InventoryAccessService inventoryAccessService,
            TypeRepository typeRepository
    ) {
        this.inventoryAccessService = inventoryAccessService;
        this.typeRepository = typeRepository;
    }

    /**
     * Busca um tipo especifico do inventario informado quando o usuario possui acesso ao recurso.
     * Estrategia: valida acesso ao inventario, consulta o tipo por `inv_id` e `type_id` e serializa a imagem para data URI.
     * Efeitos colaterais: nenhum alem de leituras na base.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param inventoryId identificador do inventario consultado.
     * @param typeId identificador do tipo a ser retornado.
     * @return dados do tipo encontrado.
     */
    public GetTypeResponse getType(
            AuthenticatedUserPrincipal authenticatedUser,
            UUID inventoryId,
            long typeId
    ) {
        inventoryAccessService.requireAnyAccess(inventoryId, authenticatedUser.userId());

        TypeDetails type = typeRepository.findTypeById(inventoryId, typeId)
                .orElseThrow(() -> new NotFoundException(
                        "Tipo nao encontrado.",
                        "type_id",
                        "type not found"
                ));

        return new GetTypeResponse(
                type.typeId(),
                type.nome(),
                TypeImageCodec.toDataUri(type.imagem()),
                type.inventoryId()
        );
    }
}
