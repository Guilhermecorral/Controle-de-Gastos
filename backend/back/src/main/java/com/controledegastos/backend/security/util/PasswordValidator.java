package com.controledegastos.backend.security.util;

/**
 * Utilitário centralizado para validação de senhas seguindo as políticas de segurança.
 */
public class PasswordValidator {

    /**
     * Valida se a senha atende aos requisitos mínimos de segurança.
     * 
     * @param password senha a ser validada
     * @throws IllegalArgumentException se a senha não atender aos requisitos
     */
    public static void validate(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Senha não pode ser nula");
        }

        boolean hasUppercase = password.chars().anyMatch(Character::isUpperCase);
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecial = password.chars().anyMatch(character -> !Character.isLetterOrDigit(character));

        if (!hasUppercase || !hasDigit || !hasSpecial || password.length() < 8) {
            throw new IllegalArgumentException("A senha precisa ter pelo menos 8 caracteres, letra maiuscula, numero e caractere especial");
        }
    }

    private PasswordValidator() {
        // Classe utilitária não deve ser instanciada
    }
}