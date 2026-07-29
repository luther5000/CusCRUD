package br.com.cuscrudrest.auth.login;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada do endpoint de login.
 * Representa as credenciais minimas exigidas pela API para autenticacao de um usuario.
 * Efeitos colaterais: nenhum.
 *
 * @param login email unico da conta.
 * @param passwd senha em texto puro a ser validada contra o hash persistido.
 */
public record LoginRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 255, message = "must have between 1 and 255 characters")
        String login,
        @NotBlank(message = "must not be blank")
        @Size(min = 8, max = 50, message = "must have between 8 and 50 characters")
        String passwd
) {
}
