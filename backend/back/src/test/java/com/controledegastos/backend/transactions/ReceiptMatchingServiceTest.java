package com.controledegastos.backend.transactions;

import com.controledegastos.backend.transactions.DTO.TransactionRequestDTO;
import com.controledegastos.backend.transactions.Repository.TransactionRepository;
import com.controledegastos.backend.user.Repository.UserRepository;
import com.controledegastos.backend.user.User;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ReceiptMatchingServiceTest {

    @Autowired private ReceiptMatchingService receiptMatchingService;
    @Autowired private TransactionService transactionService;
    @Autowired private TransactionRepository transactionRepository;
    @Autowired private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        transactionRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void matchesTextualPdfByAmountDateAndDescription() throws Exception {
        User user = userRepository.save(User.builder()
                .name("Jorge")
                .email("receipt-match@test.com")
                .password("123456")
                .role(User.Role.USER)
                .build());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities())
        );

        var transaction = transactionService.create(new TransactionRequestDTO(
                Transaction.TransactionType.DESPESA,
                "Loja do Notebook",
                Transaction.TransactionCategory.COMPRAS,
                new BigDecimal("220.00"),
                Transaction.PaymentMethod.PIX,
                1,
                LocalDate.of(2026, 8, 20)
        ));

        byte[] pdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 750);
                content.showText("Loja do Notebook 20/08/2026 Total R$ 220,00");
                content.endText();
            }
            document.save(output);
            pdf = output.toByteArray();
        }

        var previews = receiptMatchingService.preview(List.of(
                new MockMultipartFile("files", "nota.pdf", "application/pdf", pdf)
        ));

        assertThat(previews).singleElement().satisfies(preview -> {
            assertThat(preview.confidence()).isEqualTo("ALTA");
            assertThat(preview.candidates().getFirst().transactionId()).isEqualTo(transaction.id());
            assertThat(preview.candidates().getFirst().score()).isGreaterThanOrEqualTo(80);
        });
    }
}
