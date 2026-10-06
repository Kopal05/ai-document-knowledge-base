package com.kopal.smartknowledgebase.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Response body for POST /api/auth/login: the token the client must send
 * as "Authorization: Bearer <token>" on every subsequent request, plus
 * enough non-sensitive user info to avoid a second round trip just to
 * show "logged in as ...".
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    private Long userId;
    private String name;
    private String email;
}