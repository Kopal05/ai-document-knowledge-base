package com.kopal.smartknowledgebase.exception;

/**
 * Thrown when a requested Document does not exist.
 *
 * WHY THIS FILE EXISTS:
 * Instead of returning null (which pushes null-checking responsibility
 * onto every caller) or a generic RuntimeException (which the exception
 * handler can't distinguish from a real bug), we throw this specific,
 * meaningful exception from the service layer. The GlobalExceptionHandler
 * catches it and converts it into a clean 404 response.
 *
 * This is a RuntimeException (unchecked), so callers are not forced to
 * catch it with try/catch or declare "throws" everywhere — it propagates
 * up naturally until the @RestControllerAdvice handles it.
 */
public class DocumentNotFoundException extends RuntimeException {

    public DocumentNotFoundException(Long id) {
        super("Document not found with id: " + id);
    }
}
