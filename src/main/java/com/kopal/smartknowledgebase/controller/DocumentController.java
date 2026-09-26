package com.kopal.smartknowledgebase.controller;

import com.kopal.smartknowledgebase.dto.CreateDocumentRequest;
import com.kopal.smartknowledgebase.dto.DocumentResponse;
import com.kopal.smartknowledgebase.dto.PdfExtractionResponse;
import com.kopal.smartknowledgebase.service.DocumentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST endpoints for documents.
 *
 * WHY THIS FILE EXISTS:
 * The controller is the only layer that knows about HTTP. Its job is to:
 *  - Map an HTTP request (method + path + body) to a Java method call.
 *  - Delegate the actual work to DocumentService.
 *  - Translate the service's return value into an HTTP response with the
 *    correct status code.
 *
 * Notice there is NO business logic here (no "if" checking whether a
 * document exists, no manual entity construction) — that all lives in
 * DocumentService. This separation is what "thin controller, fat service"
 * means in typical Spring Boot architecture.
 *
 * @RestController = @Controller + @ResponseBody, meaning every method's
 * return value is automatically serialized to JSON (via Jackson) instead
 * of being resolved to a view template.
 *
 * @RequestMapping("/api/documents") sets a base path shared by every
 * method below, so each method only needs to specify what's different.
 */
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    // POST /api/documents
    // @Valid triggers Bean Validation on CreateDocumentRequest before this
    // method body even runs. If validation fails, Spring throws
    // MethodArgumentNotValidException, handled by GlobalExceptionHandler.
    @PostMapping
    public ResponseEntity<DocumentResponse> createDocument(@Valid @RequestBody CreateDocumentRequest request) {
        DocumentResponse response = documentService.createDocument(request);
        // 201 Created is the correct status for a successful POST that
        // creates a new resource.
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/documents
    @GetMapping
    public ResponseEntity<List<DocumentResponse>> getAllDocuments() {
        return ResponseEntity.ok(documentService.getAllDocuments());
    }

    // GET /api/documents/{id}
    @GetMapping("/{id}")
    public ResponseEntity<DocumentResponse> getDocumentById(@PathVariable Long id) {
        return ResponseEntity.ok(documentService.getDocumentById(id));
    }

    // DELETE /api/documents/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable Long id) {
        documentService.deleteDocument(id);
        // 204 No Content is the correct status for a successful DELETE
        // with no response body.
        return ResponseEntity.noContent().build();
    }

    // POST /api/documents/upload
    // @RequestParam("file") tells Spring: "look in this multipart/form-
    // data request for a part named 'file', and bind it to this
    // MultipartFile parameter." required=true is the default, which is
    // exactly why a request with no "file" part never even reaches this
    // method body — Spring rejects it earlier (see GlobalExceptionHandler
    // for how that's turned into a clean 400).
    //
    // Notice there is still no validation logic and no PDFBox code here
    // — this method does exactly what every other method in this
    // controller does: parse the HTTP input, call the service, return
    // the result with the right status code.
    @PostMapping("/upload")
    public ResponseEntity<PdfExtractionResponse> uploadAndExtractText(@RequestParam("file") MultipartFile file) {
        PdfExtractionResponse response = documentService.extractTextFromPdf(file);
        return ResponseEntity.ok(response);
    }
}