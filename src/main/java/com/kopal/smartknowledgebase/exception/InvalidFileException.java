package com.kopal.smartknowledgebase.exception;

/**
 * Thrown when an uploaded file fails basic upload-shape or type checks —
 * e.g. it's empty, or it isn't a PDF at all.
 *
 * WHY THIS IS SEPARATE FROM PdfTextExtractionException:
 * The two exceptions represent two different failure points:
 *   - InvalidFileException: the upload itself is wrong (nothing to read,
 *     or it's clearly not a PDF based on its name/content type). PDFBox
 *     is never even invoked.
 *   - PdfTextExtractionException: the upload LOOKED like a PDF (right
 *     extension/content type, non-empty), but PDFBox couldn't actually
 *     parse its bytes — e.g. the file is corrupted or truncated.
 * Keeping them separate means each one can carry a message specific to
 * what actually went wrong, which makes the API response more useful to
 * the client and makes this code easier to reason about.
 *
 * This is a RuntimeException (unchecked), consistent with every other
 * custom exception in this project (DocumentNotFoundException,
 * PdfTextExtractionException) — callers aren't forced to catch it, and
 * GlobalExceptionHandler converts it into a clean HTTP response.
 */
public class InvalidFileException extends RuntimeException {

    public InvalidFileException(String message) {
        super(message);
    }
}