package com.kopal.smartknowledgebase.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request body for POST /api/documents.
 *
 * WHY THIS FILE EXISTS:
 * This is the shape of JSON the client is allowed to send us. It is
 * intentionally separate from the Document entity:
 *  - The client should never be able to set fields like id, createdAt,
 *    or updatedAt — those are server-controlled.
 *  - If the entity later grows extra fields (e.g. internal processing
 *    status), this DTO doesn't change unless the API contract should
 *    change too.
 *
 * Bean Validation annotations (@NotBlank) are declarative rules that
 * Spring checks automatically when this DTO is used as a
 * @Valid @RequestBody parameter in the controller. If validation fails,
 * Spring throws a MethodArgumentNotValidException, which our
 * GlobalExceptionHandler catches and turns into a clean 400 response.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateDocumentRequest {

    @NotBlank(message = "title must not be blank")
    private String title;

    @NotBlank(message = "fileName must not be blank")
    private String fileName;
}
