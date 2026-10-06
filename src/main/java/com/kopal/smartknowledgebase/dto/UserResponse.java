package com.kopal.smartknowledgebase.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Response body representing a user — returned after registration.
 * Deliberately has no passwordHash field at all: this isn't "the hash
 * minus formatting," it's a type that structurally cannot carry it,
 * same reasoning DocumentResponse already follows for Document.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String name;
    private String email;
    private LocalDateTime createdAt;
}