package com.kopal.smartknowledgebase.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * A consistent JSON shape for every error the API returns.
 *
 * WHY THIS FILE EXISTS:
 * Without this, different errors could return differently-shaped JSON,
 * which makes life harder for anyone consuming the API (including you,
 * in a frontend or Postman collection, later). Every error response will
 * look like:
 * {
 *   "timestamp": "...",
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "Document not found with id: 5"
 * }
 */
@Getter
@AllArgsConstructor
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
}
