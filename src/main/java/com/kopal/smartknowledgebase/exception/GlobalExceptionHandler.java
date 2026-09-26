package com.kopal.smartknowledgebase.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Central place where exceptions thrown anywhere in the request-handling
 * pipeline (controllers, services called from controllers, etc.) are
 * converted into clean HTTP responses.
 *
 * WHY THIS FILE EXISTS:
 * Without this class, an unhandled exception would either crash the
 * request with Spring Boot's default whitelabel error page/generic JSON,
 * or you'd need repetitive try/catch blocks in every controller method.
 *
 * @RestControllerAdvice = @ControllerAdvice + @ResponseBody. It tells
 * Spring "watch every @RestController in the app, and if one of these
 * exception types escapes a controller method, run this handler method
 * instead and serialize its return value as JSON".
 *
 * Each @ExceptionHandler method declares which exception type it handles.
 * Spring picks the most specific matching handler.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 1. Resource not found -> 404
    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(DocumentNotFoundException ex) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                HttpStatus.NOT_FOUND.getReasonPhrase(),
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    // 2. Bean Validation failures (e.g. @NotBlank on a @Valid @RequestBody) -> 400
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", HttpStatus.BAD_REQUEST.getReasonPhrase());

        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage())
        );
        body.put("fieldErrors", fieldErrors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    // 3. Bad file upload (empty file, wrong file type) -> 400
    // The client sent something, but it's not an acceptable upload for
    // this endpoint. That's a client mistake, so 400 (not 500) is
    // correct.
    @ExceptionHandler(InvalidFileException.class)
    public ResponseEntity<ErrorResponse> handleInvalidFile(InvalidFileException ex) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 4. PDF looked valid on the outside but PDFBox couldn't read it -> 400
    // WHY THIS IS HANDLED HERE (added deliberately for this enhancement):
    // Without this handler, a PdfTextExtractionException would fall
    // through to the generic Exception handler below and come back as a
    // 500. But a corrupted/unreadable PDF is caused by what the CLIENT
    // uploaded, not a bug on our server — so 400 is the honest status
    // code, the same way bad JSON in a request body gives 400, not 500.
    @ExceptionHandler(PdfTextExtractionException.class)
    public ResponseEntity<ErrorResponse> handlePdfTextExtractionFailure(PdfTextExtractionException ex) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage()
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 5. No "file" part was sent at all -> 400
    // WHY THIS IS HANDLED HERE (also new for this enhancement):
    // Spring itself already knows how to turn a missing required
    // multipart part into a 400 response — normally you'd get that for
    // free, with no code of your own. BUT this class already has a
    // catch-all `@ExceptionHandler(Exception.class)` below, and once a
    // @RestControllerAdvice exists, it takes over exception handling for
    // every matching type. Since Exception.class matches literally
    // everything, it would silently intercept this exception too and
    // turn a perfectly normal "you forgot to attach a file" mistake into
    // a scary-looking 500. Adding this specific handler restores the
    // correct, expected 400 behavior while keeping our consistent
    // ErrorResponse JSON shape.
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErrorResponse> handleMissingFilePart(MissingServletRequestPartException ex) {
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Required file part '" + ex.getRequestPartName() + "' is missing"
        );
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // 6. Anything else we didn't anticipate -> 500
    // Keeping this last/generic ensures unexpected bugs still return clean
    // JSON instead of leaking a stack trace to the client.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {

        ex.printStackTrace();

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "Something went wrong. Please try again later."
        );
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}