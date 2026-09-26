package com.kopal.smartknowledgebase.exception;

/**
 * Thrown when a PDF's text cannot be extracted — e.g. the bytes given
 * aren't a valid PDF, the file is corrupted, or it's password-protected.
 *
 * WHY THIS FILE EXISTS:
 * PDFBox throws checked exceptions (mainly IOException and its subclasses
 * like InvalidPasswordException). If we let those escape
 * PdfTextExtractionService as-is, every caller would be forced to handle
 * a low-level, library-specific exception type — and the Controller layer
 * (when it exists later) would end up depending on PDFBox's exception
 * types instead of our own domain vocabulary.
 *
 * Instead, PdfTextExtractionService catches the PDFBox exception and
 * wraps it in this one, unchecked exception. This mirrors the same
 * pattern DocumentNotFoundException already uses in this project: a
 * RuntimeException so callers aren't forced to catch it, with the
 * original cause preserved via the constructor's `cause` parameter so
 * nothing is silently lost — you can still see the root PDFBox error in
 * logs/stack traces.
 *
 * We are NOT wiring this into GlobalExceptionHandler yet, because there
 * is no API endpoint that can throw it yet (no upload endpoint). That
 * wiring will make sense once Enhancement — PDF upload API — exists.
 */
public class PdfTextExtractionException extends RuntimeException {

    public PdfTextExtractionException(String message) {
        super(message);
    }

    public PdfTextExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}