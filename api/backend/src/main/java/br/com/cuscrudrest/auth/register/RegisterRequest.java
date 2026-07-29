package br.com.cuscrudrest.auth.register;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada do endpoint de cadastro de usuario.
 * Representa os dados minimos exigidos pela API para criar uma nova conta.
 * Efeitos colaterais: nenhum.
 *
 * @param name nome completo do usuario.
 * @param login email unico da conta.
 * @param passwd senha em texto puro antes do hash.
 */
public record RegisterRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 255, message = "must have between 1 and 255 characters")
        String name,
        @NotBlank(message = "must not be blank")
        @Size(max = 255, message = "must have between 1 and 255 characters")
        String login,
        @NotBlank(message = "must not be blank")
        @Size(min = 8, max = 50, message = "must have between 8 and 50 characters")
        String passwd
) {
}
