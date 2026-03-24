package com.controledegastos.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

//Entry DTO POST /api/auth/login
public record LoginRequestDTO (
        @NotBlank(message = "Email é obrigatório")
        @Email(message = "Email inválido")
        String email,

        @NotBlank(message = "Mensagem é obrigatória")
        String password

) {}
