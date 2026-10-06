package com.kopal.smartknowledgebase.exception;

/**
 * Thrown for ANY failed login attempt — unknown email, wrong password,
 * or anything else Spring Security's AuthenticationManager rejects.
 * Deliberately has a single, fixed, generic constructor message: see
 * AuthenticationService for why distinguishing "no such email" from
 * "wrong password" in the response would let an attacker enumerate
 * valid accounts by trying logins and watching which error comes back.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("Invalid email or password");
    }
}