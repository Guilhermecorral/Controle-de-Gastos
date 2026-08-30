package com.controledegastos.backend.wishlist;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class WishlistImportServiceTest {

    private final WishlistImportService service = new WishlistImportService();

    @Test
    void parsesNamesWithoutPricesAndRecognizesSectionsFromTxt() throws Exception {
        String content = """
                VIAGENS:
                - Conhecer o Japao
                - Mala nova R$ 750,00
                ESTUDOS:
                1. Curso de ingles
                """;

        var result = service.preview(new MockMultipartFile(
                "file", "desejos.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8)
        ));

        assertThat(result.format()).isEqualTo("TXT");
        assertThat(result.items()).hasSize(3);
        assertThat(result.items().getFirst().description()).isEqualTo("Conhecer o Japao");
        assertThat(result.items().getFirst().originalPrice()).isZero();
        assertThat(result.items().get(1).originalPrice()).isEqualByComparingTo("750.00");
        assertThat(result.items().get(2).suggestedListName()).isEqualTo("ESTUDOS");
    }

    @Test
    void preservesQuotedDescriptionsFromCsvExports() throws Exception {
        String content = "descricao,preco\n\"Notebook, mochila e mouse\",4500.00\n";

        var result = service.preview(new MockMultipartFile(
                "file", "desejos.csv", "text/csv", content.getBytes(StandardCharsets.UTF_8)
        ));

        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.description()).isEqualTo("Notebook, mochila e mouse");
            assertThat(item.originalPrice()).isEqualByComparingTo("4500.00");
        });
    }

    @Test
    void extractsTextFromPdfWishlist() throws Exception {
        byte[] pdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 750);
                content.showText("Notebook para trabalho");
                content.endText();
            }
            document.save(output);
            pdf = output.toByteArray();
        }

        var result = service.preview(new MockMultipartFile("file", "lista.pdf", "application/pdf", pdf));

        assertThat(result.items()).singleElement().satisfies(item -> {
            assertThat(item.description()).isEqualTo("Notebook para trabalho");
            assertThat(item.originalPrice()).isZero();
        });
    }
}
