package com.kopal.smartknowledgebase.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /api/auth/register. Bean Validation (the same
 * @NotBlank pattern CreateDocumentRequest already uses) rejects obviously
 * bad input before AuthenticationService ever runs — @Email catches a
 * malformed address, @Size enforces a minimum password length so BCrypt
 * isn't hashing an empty or trivial string.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "name must not be blank")
    private String name;

    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be a valid email address")
    private String email;

    @NotBlank(message = "password must not be blank")
    @Size(min = 8, message = "password must be at least 8 characters")
    private String password;
}