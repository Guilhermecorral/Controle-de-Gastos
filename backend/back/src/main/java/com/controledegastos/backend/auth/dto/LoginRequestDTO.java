package com.controledegastos.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Representa os dados enviados no login.
 */
public record LoginRequestDTO(
        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email invalido")
        String email,

        @NotBlank(message = "Senha e obrigatoria")
        String password,

        String captchaToken,

        String twoFactorCode
) {
}
