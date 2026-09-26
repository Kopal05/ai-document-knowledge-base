package com.kopal.smartknowledgebase.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Response body returned to clients for document endpoints.
 *
 * WHY THIS FILE EXISTS:
 * Just like CreateDocumentRequest controls what comes IN, this DTO controls
 * what goes OUT. Returning the entity directly from a controller is a
 * common beginner mistake because:
 *  - It couples your API's JSON shape 1:1 to your database schema.
 *  - It can accidentally serialize lazy-loaded JPA relationships, causing
 *    errors or huge/unwanted payloads once you add related entities
 *    (e.g. DocumentChunk in a future phase).
 *  - It gives you no place to reshape, rename, or hide fields per-endpoint.
 *
 * The mapping between Document (entity) and DocumentResponse (DTO) happens
 * in the service layer.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private Long id;
    private String title;
    private String fileName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
