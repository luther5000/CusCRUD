package br.com.cuscrudrest.inventories.users.create;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Payload de entrada para concessao de acesso de usuario a um inventario.
 * Representa o email do usuario existente e a role permitida a ser atribuida.
 * Efeitos colaterais: nenhum.
 *
 * @param login email do usuario que recebera acesso.
 * @param role role atribuida ao usuario no inventario.
 */
public record AddInventoryUserRequest(
        @NotBlank(message = "must not be blank")
        @Size(max = 255, message = "must have between 1 and 255 characters")
        String login,
        @NotNull(message = "must not be null")
        Integer role
) {
}
