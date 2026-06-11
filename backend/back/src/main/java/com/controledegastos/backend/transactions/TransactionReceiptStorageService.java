package com.controledegastos.backend.transactions;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Valida e persiste anexos fiscais em disco local de forma controlada.
 */
@Service
public class TransactionReceiptStorageService {

    private static final Map<String, String> CONTENT_TYPE_EXTENSIONS = Map.of(
            "application/pdf", ".pdf",
            "image/jpeg", ".jpg",
            "image/png", ".png"
    );

    private final Path storageRoot;
    private final long maxBytes;

    public TransactionReceiptStorageService(
            @Value("${app.transaction-receipts.storage-dir:storage/transaction-receipts}") String storageDir,
            @Value("${app.transaction-receipts.max-bytes:10485760}") long maxBytes
    ) {
        this.storageRoot = Path.of(storageDir).toAbsolutePath().normalize();
        this.maxBytes = maxBytes;
    }

    /**
     * Salva o arquivo validado em uma pasta isolada por usuario.
     */
    public StoredReceipt saveReceipt(Long userId, Long transactionId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Selecione um arquivo de nota fiscal antes de enviar");
        }

        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException("A nota fiscal excede o limite permitido de 10 MB");
        }

        try {
            byte[] content = file.getBytes();
            String contentType = detectContentType(content);
            String extension = CONTENT_TYPE_EXTENSIONS.get(contentType);
            String originalFilename = sanitizeOriginalFilename(file.getOriginalFilename(), extension);
            String storageName = transactionId + "-" + UUID.randomUUID() + extension;
            Path userDirectory = storageRoot.resolve("user-" + userId).normalize();
            Path storedPath = userDirectory.resolve(storageName).normalize();

            if (!storedPath.startsWith(userDirectory)) {
                throw new IllegalArgumentException("Nao foi possivel processar o nome do arquivo enviado");
            }

            Files.createDirectories(userDirectory);
            Files.copy(file.getInputStream(), storedPath, StandardCopyOption.REPLACE_EXISTING);

            return new StoredReceipt(originalFilename, storageName, contentType, file.getSize());
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel salvar a nota fiscal agora");
        }
    }

    /**
     * Remove um anexo antigo quando a transacao e atualizada ou excluida.
     */
    public void deleteReceipt(String storageName, Long userId) {
        if (storageName == null || storageName.isBlank()) {
            return;
        }

        try {
            Path path = resolveUserFile(userId, storageName);
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel limpar o anexo fiscal anterior");
        }
    }

    /**
     * Carrega o arquivo salvo para download seguro via endpoint autenticado.
     */
    public Resource loadReceipt(String storageName, Long userId) {
        try {
            Path path = resolveUserFile(userId, storageName);
            if (!Files.exists(path) || !Files.isReadable(path)) {
                throw new IllegalArgumentException("A nota fiscal solicitada nao esta disponivel");
            }

            return new FileSystemResource(path);
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel ler a nota fiscal agora");
        }
    }

    /**
     * Detecta o tipo real do arquivo pelo cabecalho para reduzir upload malicioso.
     */
    private String detectContentType(byte[] content) {
        if (isPdf(content)) {
            return "application/pdf";
        }

        if (isJpeg(content)) {
            return "image/jpeg";
        }

        if (isPng(content)) {
            return "image/png";
        }

        throw new IllegalArgumentException("Envie a nota fiscal em PDF, JPG ou PNG");
    }

    private boolean isPdf(byte[] content) {
        return content.length >= 5
                && content[0] == 0x25
                && content[1] == 0x50
                && content[2] == 0x44
                && content[3] == 0x46
                && content[4] == 0x2D;
    }

    private boolean isJpeg(byte[] content) {
        return content.length >= 3
                && (content[0] & 0xFF) == 0xFF
                && (content[1] & 0xFF) == 0xD8
                && (content[2] & 0xFF) == 0xFF;
    }

    private boolean isPng(byte[] content) {
        return content.length >= 8
                && (content[0] & 0xFF) == 0x89
                && content[1] == 0x50
                && content[2] == 0x4E
                && content[3] == 0x47
                && content[4] == 0x0D
                && content[5] == 0x0A
                && content[6] == 0x1A
                && content[7] == 0x0A;
    }

    private String sanitizeOriginalFilename(String originalFilename, String extension) {
        String fallback = "nota-fiscal" + extension;
        if (originalFilename == null || originalFilename.isBlank()) {
            return fallback;
        }

        String normalized = Normalizer.normalize(originalFilename, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^a-zA-Z0-9._-]", "-")
                .replaceAll("-{2,}", "-")
                .toLowerCase(Locale.ROOT);

        if (normalized.isBlank()) {
            return fallback;
        }

        if (normalized.endsWith(extension)) {
            return normalized;
        }

        int dotIndex = normalized.lastIndexOf('.');
        String baseName = dotIndex >= 0 ? normalized.substring(0, dotIndex) : normalized;
        return baseName + extension;
    }

    private Path resolveUserFile(Long userId, String storageName) throws IOException {
        Path userDirectory = storageRoot.resolve("user-" + userId).normalize();
        Path resolved = userDirectory.resolve(storageName).normalize();

        if (!resolved.startsWith(userDirectory)) {
            throw new IOException("Caminho de anexo invalido");
        }

        return resolved;
    }

    /**
     * Leva de volta os metadados necessarios para popular a transacao apos o upload.
     */
    public record StoredReceipt(
            String originalFilename,
            String storageName,
            String contentType,
            long sizeBytes
    ) {
    }
}
