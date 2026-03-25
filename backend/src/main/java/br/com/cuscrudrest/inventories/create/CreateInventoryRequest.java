package br.com.cuscrudrest.inventories.create;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada do endpoint de criacao de inventario.
 * Representa os dados minimos exigidos pela API para criar um novo inventario.
 * Efeitos colaterais: nenhum.
 *
 * @param inventoryName nome do inventario a ser criado.
 */
public record CreateInventoryRequest(
        @JsonProperty("inv_name")
        @NotBlank(message = "must not be blank")
        @Size(max = 255, message = "must have between 1 and 255 characters")
        String inventoryName
) {
}
