package com.controledegastos.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Representa o pedido de abertura do fluxo seguro de redefinicao de senha.
 */
public record ForgotPasswordRequestDTO(
        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email invalido")
        String email,

        String captchaToken
) {
}
