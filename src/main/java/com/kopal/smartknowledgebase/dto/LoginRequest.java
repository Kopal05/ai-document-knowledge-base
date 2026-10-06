package com.kopal.smartknowledgebase.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /api/auth/login. Deliberately no @Email here —
 * an invalid-format email on login should fail the SAME generic
 * "invalid credentials" way a wrong password would, not a different,
 * more specific validation error that would help an attacker fingerprint
 * valid accounts by format alone. @NotBlank is enough to reject an
 * obviously empty request without revealing anything about account state.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "email must not be blank")
    private String email;

    @NotBlank(message = "password must not be blank")
    private String password;
}