package com.kopal.smartknowledgebase.exception;

/**
 * Thrown when registering with an email address that's already in use.
 * Mapped to 409 Conflict in GlobalExceptionHandler — more precise than
 * a generic 400: the request is well-formed, it just conflicts with
 * existing state.
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String email) {
        super("An account with email '" + email + "' already exists");
    }
}