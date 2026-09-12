package com.controledegastos.backend.transactions;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionReceiptStorageServiceTest {

    @TempDir
    Path tempDirectory;

    @Test
    void shouldDeleteAllReceiptFilesForOnlyTheSelectedUser() throws Exception {
        TransactionReceiptStorageService storageService = new TransactionReceiptStorageService(tempDirectory.toString(), 1024);
        MockMultipartFile receipt = new MockMultipartFile(
                "file",
                "comprovante.pdf",
                "application/pdf",
                "%PDF-1.4 test".getBytes()
        );
        storageService.saveReceipt(10L, 1L, receipt);
        storageService.saveReceipt(20L, 2L, receipt);

        storageService.deleteAllReceipts(10L);

        assertThat(Files.exists(tempDirectory.resolve("user-10"))).isFalse();
        try (var remainingFiles = Files.list(tempDirectory.resolve("user-20"))) {
            assertThat(remainingFiles).isNotEmpty();
        }
    }
}
