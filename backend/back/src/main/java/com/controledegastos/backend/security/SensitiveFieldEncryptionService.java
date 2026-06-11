package com.controledegastos.backend.security;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Protege segredos sensiveis do produto com criptografia simetrica antes de persistir no banco.
 */
@Service
public class SensitiveFieldEncryptionService {

    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.security.field-encryption.secret-key:}")
    private String configuredSecret;

    private SecretKeySpec secretKeySpec;

    @PostConstruct
    void initialize() {
        if (configuredSecret == null || configuredSecret.isBlank()) {
            throw new IllegalStateException("Configure APP_FIELD_ENCRYPTION_SECRET_KEY para proteger os segredos sensiveis");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] keyBytes = digest.digest(configuredSecret.getBytes(StandardCharsets.UTF_8));
            this.secretKeySpec = new SecretKeySpec(keyBytes, "AES");
        } catch (Exception exception) {
            throw new IllegalStateException("Nao foi possivel inicializar a criptografia de campos sensiveis", exception);
        }
    }

    /**
     * Criptografa um valor em texto puro usando AES-GCM antes de salvar no banco.
     */
    public String encrypt(String plainValue) {
        if (plainValue == null || plainValue.isBlank()) {
            return null;
        }

        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainValue.getBytes(StandardCharsets.UTF_8));

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);
            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception exception) {
            throw new IllegalStateException("Nao foi possivel criptografar o dado sensivel", exception);
        }
    }

    /**
     * Recupera o valor original de um campo sensivel salvo de forma criptografada.
     */
    public String decrypt(String encryptedValue) {
        if (encryptedValue == null || encryptedValue.isBlank()) {
            return null;
        }

        try {
            byte[] payload = Base64.getDecoder().decode(encryptedValue);
            ByteBuffer buffer = ByteBuffer.wrap(payload);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);

            byte[] cipherBytes = new byte[buffer.remaining()];
            buffer.get(cipherBytes);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(TAG_LENGTH_BITS, iv));
            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception exception) {
            throw new IllegalStateException("Nao foi possivel descriptografar o dado sensivel", exception);
        }
    }
}
