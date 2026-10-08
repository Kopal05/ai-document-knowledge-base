package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.exception.PdfTextExtractionException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class PdfTextExtractionService {

    public String extractText(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String rawText = stripper.getText(document);
            return sanitize(rawText);
        } catch (IOException e) {
            throw new PdfTextExtractionException("Failed to extract text from PDF", e);
        }
    }

    /**
     * Removes NUL characters (0x00) that PDFBox can emit for glyphs with no
     * Unicode mapping (e.g. icon fonts). Postgres' text type rejects NUL
     * bytes outright, regardless of encoding, so they must be stripped
     * before this text is persisted.
     */
    private String sanitize(String text) {
        if (text == null) {
            return null;
        }
        return text.replace("\u0000", "");
    }
}