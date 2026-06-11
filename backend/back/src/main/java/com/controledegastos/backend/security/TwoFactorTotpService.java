package com.controledegastos.backend.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Gera e valida codigos TOTP para autenticacao em dois fatores via app autenticador.
 */
@Service
public class TwoFactorTotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int SECRET_BYTES_LENGTH = 20;
    private static final int CODE_DIGITS = 6;
    private static final long TIME_STEP_SECONDS = 30L;
    private static final int ACCEPTED_WINDOW_STEPS = 1;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.security.two-factor.issuer:Farol Financeiro}")
    private String issuer;

    /**
     * Gera um novo segredo base32 para cadastro em aplicativo autenticador.
     */
    public String generateSecret() {
        byte[] randomBytes = new byte[SECRET_BYTES_LENGTH];
        secureRandom.nextBytes(randomBytes);
        return encodeBase32(randomBytes);
    }

    /**
     * Verifica se o codigo informado bate com o segredo dentro de uma pequena janela de tolerancia.
     */
    public boolean verifyCode(String secret, String code) {
        if (secret == null || secret.isBlank() || code == null || !code.matches("\\d{6}")) {
            return false;
        }

        long currentCounter = System.currentTimeMillis() / 1000L / TIME_STEP_SECONDS;
        for (long offset = -ACCEPTED_WINDOW_STEPS; offset <= ACCEPTED_WINDOW_STEPS; offset++) {
            if (generateCode(secret, currentCounter + offset).equals(code)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Monta a URI padrao que pode ser lida por apps como Google Authenticator, Authy ou 1Password.
     */
    public String buildOtpAuthUri(String accountName, String secret) {
        String normalizedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8);
        String normalizedAccount = URLEncoder.encode(accountName, StandardCharsets.UTF_8);

        return "otpauth://totp/"
                + normalizedIssuer
                + ":"
                + normalizedAccount
                + "?secret="
                + secret
                + "&issuer="
                + normalizedIssuer
                + "&digits="
                + CODE_DIGITS
                + "&period="
                + TIME_STEP_SECONDS;
    }

    public String getIssuer() {
        return issuer;
    }

    private String generateCode(String secret, long counter) {
        try {
            byte[] secretBytes = decodeBase32(secret);
            byte[] counterBytes = ByteBuffer.allocate(8).putLong(counter).array();

            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secretBytes, "HmacSHA1"));
            byte[] hash = mac.doFinal(counterBytes);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % 1_000_000;
            return String.format("%06d", otp);
        } catch (Exception exception) {
            throw new IllegalStateException("Nao foi possivel validar o codigo de dois fatores", exception);
        }
    }

    private String encodeBase32(byte[] data) {
        StringBuilder builder = new StringBuilder();
        int buffer = data[0];
        int next = 1;
        int bitsLeft = 8;

        while (bitsLeft > 0 || next < data.length) {
            if (bitsLeft < 5) {
                if (next < data.length) {
                    buffer <<= 8;
                    buffer |= data[next++] & 0xFF;
                    bitsLeft += 8;
                } else {
                    int pad = 5 - bitsLeft;
                    buffer <<= pad;
                    bitsLeft += pad;
                }
            }

            int index = (buffer >> (bitsLeft - 5)) & 0x1F;
            bitsLeft -= 5;
            builder.append(BASE32_ALPHABET.charAt(index));
        }

        return builder.toString();
    }

    private byte[] decodeBase32(String value) {
        String normalizedValue = value.trim().replace("=", "").replace(" ", "").toUpperCase();
        byte[] bytes = new byte[normalizedValue.length() * 5 / 8];

        int buffer = 0;
        int bitsLeft = 0;
        int index = 0;

        for (char character : normalizedValue.toCharArray()) {
            int lookup = BASE32_ALPHABET.indexOf(character);
            if (lookup < 0) {
                throw new IllegalArgumentException("Segredo TOTP invalido");
            }

            buffer <<= 5;
            buffer |= lookup & 0x1F;
            bitsLeft += 5;

            if (bitsLeft >= 8) {
                bytes[index++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }

        return bytes;
    }
}
