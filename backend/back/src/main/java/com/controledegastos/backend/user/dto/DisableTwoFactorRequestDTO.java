
package com.controledegastos.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Exige senha atual e codigo TOTP para desligar o segundo fator com seguranca.
 */
public record DisableTwoFactorRequestDTO(
        @NotBlank(message = "Senha atual e obrigatoria")
        @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
        String password,

        @NotBlank(message = "Codigo de verificacao e obrigatorio")
        @Pattern(regexp = "\\d{6}", message = "Codigo de verificacao deve ter 6 digitos")
        String code
) {
}
