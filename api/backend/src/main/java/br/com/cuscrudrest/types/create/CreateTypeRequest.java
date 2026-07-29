package br.com.cuscrudrest.types.create;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada do endpoint de criacao de tipos.
 * Representa os dados minimos exigidos pela API para criar um novo tipo em um inventario.
 * Efeitos colaterais: nenhum.
 *
 * @param nome nome do tipo a ser criado.
 * @param imagem imagem opcional no formato data URI.
 */
public record CreateTypeRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 255, message = "must have between 1 and 255 characters")
        String nome,
        String imagem
) {
}
