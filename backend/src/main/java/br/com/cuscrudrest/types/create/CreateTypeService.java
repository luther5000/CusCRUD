package br.com.cuscrudrest.types.create;

import br.com.cuscrudrest.auth.security.AuthenticatedUserPrincipal;
import br.com.cuscrudrest.common.error.ConflictException;
import br.com.cuscrudrest.common.error.ValidationException;
import br.com.cuscrudrest.config.DatabaseConfiguredCondition;
import br.com.cuscrudrest.inventories.InventoryAccessContext;
import br.com.cuscrudrest.inventories.InventoryAccessService;
import br.com.cuscrudrest.types.TypeDetails;
import br.com.cuscrudrest.types.TypeRepository;
import org.springframework.context.annotation.Conditional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Servico de criacao de tipos da aplicacao.
 * Centraliza a validacao de permissao de escrita, unicidade do nome e formato da imagem em data URI.
 * Efeitos colaterais: cria registros persistidos na tabela `types`.
 */
@Service
@Conditional(DatabaseConfiguredCondition.class)
public class CreateTypeService {

    private static final Pattern DATA_URI_PATTERN = Pattern.compile("^data:([^;]+);base64,(.+)$", Pattern.DOTALL);
    private static final int MAX_IMAGE_BYTES = 5 * 1024 * 1024;

    private final InventoryAccessService inventoryAccessService;
    private final TypeRepository typeRepository;

    /**
     * Cria o servico de criacao de tipos.
     *
     * @param inventoryAccessService servico responsavel por validar acesso de escrita ao inventario.
     * @param typeRepository repositorio JDBC do dominio de tipos.
     */
    public CreateTypeService(
            InventoryAccessService inventoryAccessService,
            TypeRepository typeRepository
    ) {
        this.inventoryAccessService = inventoryAccessService;
        this.typeRepository = typeRepository;
    }

    /**
     * Cria um novo tipo no inventario informado.
     * Estrategia: valida permissao de escrita, unicidade do nome, formato da imagem e persiste o registro na mesma transacao.
     * Efeitos colaterais: cria um novo registro na tabela `types`.
     *
     * @param authenticatedUser usuario autenticado da request atual.
     * @param inventoryId identificador do inventario alvo.
     * @param request payload com nome e imagem opcional do tipo.
     * @return resposta HTTP com o tipo criado.
     */
    @Transactional
    public CreateTypeResponse createType(
            AuthenticatedUserPrincipal authenticatedUser,
            UUID inventoryId,
            CreateTypeRequest request
    ) {
        InventoryAccessContext accessContext = inventoryAccessService.requireWriteAccess(
                inventoryId,
                authenticatedUser.userId()
        );

        ensureUniqueTypeName(accessContext.inventoryId(), request.nome());
        ParsedImage parsedImage = parseImage(request.imagem());

        TypeDetails createdType;
        try {
            createdType = typeRepository.createType(
                    accessContext.inventoryId(),
                    request.nome(),
                    parsedImage.bytes()
            );
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException(
                    "Ja existe outro tipo com o mesmo nome no inventario.",
                    "nome",
                    "type name already exists in inventory"
            );
        }

        return new CreateTypeResponse(
                createdType.typeId(),
                createdType.nome(),
                parsedImage.originalDataUri(),
                createdType.inventoryId()
        );
    }

    /**
     * Garante que ainda nao exista um tipo com o mesmo nome no inventario informado.
     *
     * @param inventoryId identificador do inventario alvo.
     * @param nome nome do tipo cuja unicidade sera validada.
     * @throws ConflictException quando ja existir um tipo com o mesmo nome no inventario.
     */
    private void ensureUniqueTypeName(UUID inventoryId, String nome) {
        if (typeRepository.existsTypeByInventoryIdAndName(inventoryId, nome)) {
            throw new ConflictException(
                    "Ja existe outro tipo com o mesmo nome no inventario.",
                    "nome",
                    "type name already exists in inventory"
            );
        }
    }

    /**
     * Valida e decodifica a imagem opcional recebida no payload.
     *
     * @param imageDataUri imagem em data URI, quando informada.
     * @return imagem validada com bytes decodificados e valor original para resposta.
     * @throws ValidationException quando a imagem nao esta em data URI valido ou excede 5 MiB.
     */
    private ParsedImage parseImage(String imageDataUri) {
        if (imageDataUri == null) {
            return new ParsedImage(null, null);
        }

        Matcher matcher = DATA_URI_PATTERN.matcher(imageDataUri);
        if (!matcher.matches() || matcher.group(1).isBlank()) {
            throw new ValidationException(
                    "Imagem invalida.",
                    "imagem",
                    "must be a valid data URI with mime and base64 content"
            );
        }

        byte[] imageBytes;
        try {
            imageBytes = Base64.getDecoder().decode(matcher.group(2));
        } catch (IllegalArgumentException exception) {
            throw new ValidationException(
                    "Imagem invalida.",
                    "imagem",
                    "must be a valid data URI with mime and base64 content"
            );
        }

        if (imageBytes.length > MAX_IMAGE_BYTES) {
            throw new ValidationException(
                    "Imagem excede o tamanho maximo permitido.",
                    "imagem",
                    "must not exceed 5 MiB"
            );
        }

        return new ParsedImage(imageDataUri, imageBytes);
    }

    /**
     * Representa a imagem recebida no payload apos validacao e decodificacao.
     *
     * @param originalDataUri valor original recebido no request.
     * @param bytes bytes decodificados a serem persistidos.
     */
    private record ParsedImage(String originalDataUri, byte[] bytes) {
    }
}
