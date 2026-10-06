package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.exception.PdfTextExtractionException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PdfTextExtractionServiceTest {

    private final PdfTextExtractionService service = new PdfTextExtractionService();

    @Test
    void extractsTextFromValidPdf() throws Exception {
        byte[] pdfBytes = createTestPdf("Hello from a test PDF");

        String text = service.extractText(pdfBytes);

        assertThat(text).contains("Hello from a test PDF");
    }

    @Test
    void throwsExceptionForInvalidPdfBytes() {
        byte[] garbage = "not a pdf".getBytes();

        assertThatThrownBy(() -> service.extractText(garbage))
                .isInstanceOf(PdfTextExtractionException.class);
    }

    private byte[] createTestPdf(String text) throws Exception {
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