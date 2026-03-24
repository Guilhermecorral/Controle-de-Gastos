package com.controledegastos.backend.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Input DTO for the POST /api/auth/register endpoint
// @NotBlank: required field and cannot be just spaces
// @Email: automatically validates email format
// @Size: limits size — prevents absurd data from reaching the database
public record RegisterRequestDTO (

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 2, max = 100, message = "Nome deve ter entre 2 e 100 caracteres")
    String name,

    @NotBlank(message = "Email é obrigatório")
    @Email(message = "Email inválido")
    String email,

    @NotBlank(message = "Senha é Obrigatório")
    @Size(min = 6, max = 100, message = "Senha deve ter no mínimo 6 caracteres")
    String password

    ) {} // record: immutable, automatically generates equals/hashCode/toString — perfect for DTOs
