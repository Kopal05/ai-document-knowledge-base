package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.exception.PdfTextExtractionException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for PdfTextExtractionService.
 *
 * WHY THIS IS A "UNIT" TEST (not an integration test):
 * - No Spring context is started (no @SpringBootTest here).
 * - No database, no web server, no other beans involved.
 * - We create `new PdfTextExtractionService()` directly and call a method
 *   on it, exactly like calling any plain Java object.
 * This is possible because the class has no constructor dependencies —
 * it doesn't need anything injected. That keeps this test fast (no
 * Spring startup cost) and focused only on this one class's behavior.
 *
 * WHY WE GENERATE THE PDF IN CODE INSTEAD OF COMMITTING A .pdf FILE:
 * PDFBox can both WRITE and READ PDFs. We use its writing side
 * (PDDocument + PDPageContentStream) purely as a test-data factory to
 * build a tiny, valid, one-page PDF in memory, then feed those bytes into
 * the extractText() method we're actually testing. This means the repo
 * doesn't need a binary test fixture file, and the test is self-contained
 * and easy to read.
 */
class PdfTextExtractionServiceTest {

    private final PdfTextExtractionService service = new PdfTextExtractionService();

    @Test
    void extractText_shouldReturnTextFromAValidPdf() throws IOException {
        byte[] pdfBytes = createSimplePdfWithText("Hello, PDF extraction!");

        String extractedText = service.extractText(pdfBytes);

        assertThat(extractedText).contains("Hello, PDF extraction!");
    }

    @Test
    void extractText_shouldThrowForCorruptedOrNonPdfBytes() {
        byte[] notActuallyAPdf = "this is just plain text, not a PDF".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> service.extractText(notActuallyAPdf))
                .isInstanceOf(PdfTextExtractionException.class);
    }

    @Test
    void extractText_shouldThrowForNullOrEmptyInput() {
        assertThatThrownBy(() -> service.extractText(null))
                .isInstanceOf(PdfTextExtractionException.class);

        assertThatThrownBy(() -> service.extractText(new byte[0]))
                .isInstanceOf(PdfTextExtractionException.class);
    }

    /**
     * Builds a minimal, valid, single-page PDF containing the given text,
     * and returns its raw bytes — exactly the form extractText() expects.
     */
    private byte[] createSimplePdfWithText(String text) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(
                        new PDType1Font(Standard14Fonts.FontName.HELVETICA),
                        12
                );
                contentStream.newLineAtOffset(100, 700);
                contentStream.showText(text);
                contentStream.endText();
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
}