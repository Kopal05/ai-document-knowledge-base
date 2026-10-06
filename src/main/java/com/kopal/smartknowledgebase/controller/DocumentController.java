package com.kopal.smartknowledgebase.controller;

import com.kopal.smartknowledgebase.dto.AskRequest;
import com.kopal.smartknowledgebase.dto.AskResponse;
import com.kopal.smartknowledgebase.dto.CreateDocumentRequest;
import com.kopal.smartknowledgebase.dto.DocumentResponse;
import com.kopal.smartknowledgebase.dto.PdfExtractionResponse;
import com.kopal.smartknowledgebase.dto.SearchResponse;
import com.kopal.smartknowledgebase.security.CustomUserDetails;
import com.kopal.smartknowledgebase.service.DocumentService;
import com.kopal.smartknowledgebase.service.QuestionAnsweringService;
import com.kopal.smartknowledgebase.service.SemanticSearchService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/documents")
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

    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(
            @Valid @RequestBody CreateDocumentRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        DocumentResponse response = documentService.createDocument(request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments(
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(documentService.getAllDocuments(principal.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        return ResponseEntity.ok(documentService.getDocumentById(id, principal.getId()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails principal) {
        documentService.deleteDocument(id, principal.getId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/upload")
    public ResponseEntity<PdfExtractionResponse> uploadAndExtractText(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal CustomUserDetails principal) {
        PdfExtractionResponse response = documentService.extractTextFromPdf(file, principal.getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{documentId}/search")
    public ResponseEntity<SearchResponse> searchDocument(
            @PathVariable Long documentId,
            @RequestParam String query,
            @RequestParam(required = false) Integer topK,
            @AuthenticationPrincipal CustomUserDetails principal) {
        SearchResponse response = semanticSearchService.search(documentId, query, topK, principal.getId());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{documentId}/ask")
    public ResponseEntity<AskResponse> askDocument(
            @PathVariable Long documentId,
            @Valid @RequestBody AskRequest request,
            @AuthenticationPrincipal CustomUserDetails principal) {
        AskResponse response = questionAnsweringService.ask(documentId, request.getQuestion(), principal.getId());
        return ResponseEntity.ok(response);
    }
}