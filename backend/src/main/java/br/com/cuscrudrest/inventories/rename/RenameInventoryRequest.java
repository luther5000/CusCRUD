package br.com.cuscrudrest.inventories.rename;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada do endpoint de renomeacao de inventario.
 * Representa o novo nome exigido pela API para um inventario ja existente.
 * Efeitos colaterais: nenhum.
 *
 * @param inventoryName novo nome do inventario.
 */
public record RenameInventoryRequest(
        @JsonProperty("inv_name")
        @NotBlank(message = "must not be blank")
        @Size(max = 255, message = "must have between 1 and 255 characters")
        String inventoryName
) {
}
