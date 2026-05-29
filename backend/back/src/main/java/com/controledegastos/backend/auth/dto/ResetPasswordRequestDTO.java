package com.controledegastos.backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Carrega os dados necessarios para concluir a troca de senha a partir de um token valido.
 */
public record ResetPasswordRequestDTO(
        @NotBlank(message = "Token de redefinicao e obrigatorio")
        String token,

        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
        String password,

        @NotBlank(message = "Confirmacao de senha e obrigatoria")
        String confirmPassword,

        String captchaToken
) {
}
