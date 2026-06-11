package com.controledegastos.backend.config;

/**
 * Sinaliza que o recurso pedido nao foi encontrado para a operacao atual.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
