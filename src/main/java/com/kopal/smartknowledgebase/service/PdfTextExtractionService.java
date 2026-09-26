package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.exception.PdfTextExtractionException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;

/**
 * Reads raw PDF bytes and returns their text content.
 *
 * WHY THIS FILE EXISTS / IS SEPARATE FROM DocumentService:
 * This is a single-purpose service: "given PDF bytes, give me back text".
 * It knows NOTHING about Document entities, the database, controllers, or
 * HTTP. That isolation is deliberate — see the "Why is this a separate
 * service?" explanation in the accompanying response for the full
 * reasoning. In short: PDFBox is an implementation detail of "how we read
 * a PDF", and the rest of the app shouldn't need to know PDFBox exists.
 *
 * This class is intentionally NOT called by DocumentController or
 * DocumentService yet — it exists on its own, tested on its own, so you
 * can understand it in isolation before wiring it into the rest of the
 * app in a future enhancement (the upload endpoint).
 *
 * It's annotated @Service (not just a plain class) so that once you DO
 * want to use it elsewhere, Spring can inject it via the constructor the
 * same way DocumentRepository is injected into DocumentService today —
 * no extra wiring needed later.
 */
@Service
public class PdfTextExtractionService {

    /**
     * Extracts all text from a PDF given as raw bytes.
     *
     * @param pdfBytes the raw contents of a .pdf file
     * @return the text contained in the PDF
     * @throws PdfTextExtractionException if pdfBytes is empty, not a
     *         valid PDF, corrupted, or otherwise unreadable
     */
    public String extractText(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new PdfTextExtractionException("PDF content is null or empty");
        }

        // try-with-resources: PDDocument implements Closeable, and it
        // holds native/file resources under the hood. Declaring it inside
        // the parentheses of try(...) guarantees document.close() is
        // called automatically when the block ends — whether it ends
        // normally or via an exception — so we never leak resources by
        // forgetting a manual close() call.
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } catch (IOException e) {
            // Loader.loadPDF / PDFTextStripper declare IOException as a
            // checked exception (PDFBox's own InvalidPasswordException
            // for encrypted PDFs is actually a subclass of IOException,
            // so it's caught here too). We don't want that low-level,
            // PDFBox-specific exception type leaking out of this class,
            // so we catch it and rethrow our own unchecked exception,
            // keeping the original error as the "cause" for debugging.
            throw new PdfTextExtractionException("Failed to extract text from PDF", e);
        }
    }
}