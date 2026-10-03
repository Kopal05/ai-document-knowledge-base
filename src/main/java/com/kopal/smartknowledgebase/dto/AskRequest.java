package com.kopal.smartknowledgebase.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /api/documents/{documentId}/ask.
 *
 * WHY @NotBlank HERE (UNLIKE SEARCH'S ?query= PARAMETER):
 * This is a request BODY DTO, so it can use the exact same Bean
 * Validation pattern CreateDocumentRequest already uses: @Valid on the
 * controller parameter triggers this automatically before the method
 * body runs, and a blank question is rejected with the project's
 * existing MethodArgumentNotValidException handler (already in
 * GlobalExceptionHandler) — no new exception class needed. Semantic
 * search's query couldn't use this pattern because it's a simple
 * @RequestParam String, not a validated object — that's why
 * InvalidSearchQueryException exists for THAT case but isn't needed here.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AskRequest {

    @NotBlank(message = "question must not be blank")
    private String question;
}