package com.controledegastos.backend.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Recebe o codigo temporario digitado pelo usuario para confirmar ou validar o segundo fator.
 */
public record TwoFactorVerifyRequestDTO(
        @NotBlank(message = "Codigo de verificacao e obrigatorio")
        @Pattern(regexp = "\\d{6}", message = "Codigo de verificacao deve ter 6 digitos")
        String code
) {
}
