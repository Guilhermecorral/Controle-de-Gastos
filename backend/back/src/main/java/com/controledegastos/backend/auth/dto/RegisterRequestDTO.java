package com.controledegastos.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Representa os dados enviados no cadastro de um novo usuario.
 */
public record RegisterRequestDTO(
        @NotBlank(message = "Nome e obrigatorio")
        @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
        String name,

        @NotBlank(message = "Email e obrigatorio")
        @Email(message = "Email invalido")
        String email,

        @NotBlank(message = "Senha e obrigatoria")
        @Size(min = 8, max = 100, message = "Senha deve ter no minimo 8 caracteres")
        String password,

        String captchaToken
) {
}
