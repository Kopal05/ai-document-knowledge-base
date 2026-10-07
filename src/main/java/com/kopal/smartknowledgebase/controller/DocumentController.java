package com.kopal.smartknowledgebase.controller;

import com.kopal.smartknowledgebase.dto.AskRequest;
import com.kopal.smartknowledgebase.dto.AskResponse;
import com.kopal.smartknowledgebase.dto.CreateDocumentRequest;
import com.kopal.smartknowledgebase.dto.DocumentResponse;
import com.kopal.smartknowledgebase.dto.PdfExtractionResponse;
import com.kopal.smartknowledgebase.dto.SearchResponse;
import com.kopal.smartknowledgebase.exception.ErrorResponse;
import com.kopal.smartknowledgebase.security.CustomUserDetails;
import com.kopal.smartknowledgebase.service.DocumentService;
import com.kopal.smartknowledgebase.service.QuestionAnsweringService;
import com.kopal.smartknowledgebase.service.SemanticSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
@Tag(name = "Documents", description = "Document CRUD, PDF upload, semantic search, and RAG Q&A. All endpoints require a Bearer JWT; results are scoped to the authenticated user's own documents.")
@SecurityRequirement(name = "bearerAuth")
public class DocumentController {

    private final DocumentService documentService;
    private final SemanticSearchService semanticSearchService;
    private final QuestionAnsweringService questionAnsweringService;

    public DocumentController(
            DocumentService documentService,
            SemanticSearchService semanticSearchService,
            QuestionAnsweringService questionAnsweringService) {
        this.documentService = documentService;
        this.semanticSearchService = semanticSearchService;
        this.questionAnsweringService = questionAnsweringService;
    }

    @Operation(summary = "Create a document record",
            description = "Creates document metadata without a file. Use /upload to also extract, chunk, and embed PDF content.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Document created",
                    content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(
            @Valid @RequestBody CreateDocumentRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        DocumentResponse response = documentService.createDocument(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List your documents",
            description = "Returns only documents owned by the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List returned (may be empty)",
                    content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(documentService.getAllDocuments(principal.getId()));
    }

    @Operation(summary = "Get a document by ID",
            description = "Returns 404 both when the document doesn't exist and when it belongs to another user — this is deliberate, to avoid confirming another user's document IDs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Document found",
                    content = @Content(schema = @Schema(implementation = DocumentResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found or not owned by caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(
            @Parameter(description = "Document ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(documentService.getDocumentById(id, principal.getId()));
    }

    @Operation(summary = "Delete a document",
            description = "Deletes the document and its chunks (cascade). 404 for not-found or not-owned, same as GET by ID.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Not found or not owned by caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @Parameter(description = "Document ID", required = true) @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        documentService.deleteDocument(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Upload a PDF",
            description = "Multipart upload. Extracts text (PDFBox), splits it into overlapping chunks, generates a 768-dim embedding per chunk via Ollama, and persists everything. Content-Type must be application/pdf.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "PDF processed and stored",
                    content = @Content(schema = @Schema(implementation = PdfExtractionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Empty file or non-PDF content type",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Embedding provider (Ollama) unreachable or failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PdfExtractionResponse> uploadAndExtractText(
            @Parameter(description = "PDF file, form field name \"file\". Max size governed by spring.servlet.multipart.max-file-size (10MB).", required = true)
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails principal) {
        PdfExtractionResponse response = documentService.extractTextFromPdf(file, principal.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Semantic search within a document",
            description = "Embeds the query and returns the topK most similar chunks by cosine similarity (pgvector), scoped to the given document and the caller's ownership.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Results returned (may be empty if no chunks have embeddings)",
                    content = @Content(schema = @Schema(implementation = SearchResponse.class))),
            @ApiResponse(responseCode = "400", description = "Blank query",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Document not found or not owned by caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Embedding provider (Ollama) unreachable or failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{documentId}/search")
    public ResponseEntity<SearchResponse> searchDocument(
            @Parameter(description = "Document ID to search within", required = true) @PathVariable Long documentId,
            @Parameter(description = "Natural-language search query", required = true) @RequestParam String query,
            @Parameter(description = "Number of top results to return (defaults to search.default-top-k if omitted)") @RequestParam(required = false) Integer topK,
            @AuthenticationPrincipal CustomUserDetails principal) {
        SearchResponse response = semanticSearchService.search(documentId, query, topK, principal.getId());
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Ask a question about a document (RAG)",
            description = "Retrieves the most relevant chunks for the question, then asks a local LLM (Ollama) to answer using only that retrieved context. Returns a fixed fallback message with an empty source list when no relevant chunks are found, without calling the LLM.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Answer generated",
                    content = @Content(schema = @Schema(implementation = AskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Blank question",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Document not found or not owned by caller",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "503", description = "Embedding or chat provider (Ollama) unreachable or failed",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/{documentId}/ask")
    public ResponseEntity<AskResponse> askDocument(
            @Parameter(description = "Document ID to ask about", required = true) @PathVariable Long documentId,
            @Valid @RequestBody AskRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        AskResponse response = questionAnsweringService.ask(documentId, request.getQuestion(), principal.getId());
        return ResponseEntity.ok(response);
    }
}