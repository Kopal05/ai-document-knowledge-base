package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.dto.CreateDocumentRequest;
import com.kopal.smartknowledgebase.dto.DocumentResponse;
import com.kopal.smartknowledgebase.dto.PdfExtractionResponse;
import com.kopal.smartknowledgebase.entity.Document;
import com.kopal.smartknowledgebase.entity.DocumentChunk;
import com.kopal.smartknowledgebase.exception.DocumentNotFoundException;
import com.kopal.smartknowledgebase.exception.InvalidFileException;
import com.kopal.smartknowledgebase.exception.PdfTextExtractionException;
import com.kopal.smartknowledgebase.repository.DocumentChunkRepository;
import com.kopal.smartknowledgebase.repository.DocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Business logic layer for documents.
 *
 * WHY THIS FILE EXISTS:
 * The service layer sits between the controller (HTTP concerns) and the
 * repository (database concerns). Its responsibilities:
 *  - Orchestrate calls to one or more repositories.
 *  - Enforce business rules (later: "only the owner can delete a
 *    document", "a document must be processed before it can be queried",
 *    etc.)
 *  - Convert between entities (internal representation) and DTOs
 *    (external representation).
 *
 * Keeping this logic OUT of the controller means:
 *  - Controllers stay thin and only deal with HTTP (status codes, request
 *    parsing).
 *  - This logic is reusable — e.g. later you might call createDocument
 *    from a batch import job or a message-queue listener, not just from
 *    an HTTP request.
 *  - It's easier to unit test business logic without spinning up a web
 *    server (see DocumentServiceTest).
 *
 * Constructor injection (via the constructor below, generated implicitly
 * — see note) is preferred over field injection (@Autowired on a field)
 * because it makes dependencies explicit, allows the field to be final
 * (immutable after construction), and makes the class trivially testable
 * by just calling `new DocumentService(mockRepo, mockPdfService,
 * mockChunkingService, mockChunkRepo)`.
 */
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final TextChunkingService textChunkingService;
    private final DocumentChunkRepository documentChunkRepository;

    // Constructor injection: Spring sees there is exactly one constructor,
    // so it automatically injects all four beans here — no @Autowired
    // annotation is required on a single constructor. This is also
    // exactly why constructor injection makes this class so easy to unit
    // test: in DocumentServiceTest we just pass `new DocumentService(
    // mockRepo, mockPdfService, mockChunkingService, mockChunkRepo)`, no
    // Spring container needed.
    public DocumentService(DocumentRepository documentRepository,
                           PdfTextExtractionService pdfTextExtractionService,
                           TextChunkingService textChunkingService,
                           DocumentChunkRepository documentChunkRepository) {
        this.documentRepository = documentRepository;
        this.pdfTextExtractionService = pdfTextExtractionService;
        this.textChunkingService = textChunkingService;
        this.documentChunkRepository = documentChunkRepository;
    }

    public DocumentResponse createDocument(CreateDocumentRequest request) {
        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setFileName(request.getFileName());
        // id, createdAt, updatedAt are set by JPA / @PrePersist — not here.

        Document saved = documentRepository.save(document);
        return toResponse(saved);
    }

    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public DocumentResponse getDocumentById(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new DocumentNotFoundException(id));
        return toResponse(document);
    }

    public void deleteDocument(Long id) {
        if (!documentRepository.existsById(id)) {
            throw new DocumentNotFoundException(id);
        }
        documentRepository.deleteById(id);
    }

    /**
     * Accepts an uploaded PDF, extracts its text, splits it into chunks,
     * and persists a Document row together with one DocumentChunk row
     * per chunk.
     *
     * @Transactional matters here specifically because this method
     * performs TWO separate writes: documentRepository.save(document)
     * and documentChunkRepository.saveAll(chunks). Without this
     * annotation, each call could commit independently — if the chunk
     * insert failed (e.g. a database error), the Document row would
     * already be permanently saved with zero chunks, a broken,
     * inconsistent record with no way to retry cleanly. With
     * @Transactional, Spring wraps the whole method in one database
     * transaction: any unchecked exception thrown anywhere in this
     * method causes EVERYTHING — including the already-executed
     * Document insert — to roll back. Either the Document and all of
     * its chunks are saved together, or none of it is visible in the
     * database at all.
     */
    @Transactional
    public PdfExtractionResponse extractTextFromPdf(MultipartFile file) {
        validatePdfFile(file);

        byte[] pdfBytes;
        try {
            // MultipartFile.getBytes() declares a checked IOException
            // (reading a file is I/O, and I/O can fail for reasons
            // outside our control — disk issues, a truncated upload,
            // etc.). We catch it here and rethrow our own unchecked
            // PdfTextExtractionException so this failure mode is
            // reported the same way a PDFBox parsing failure is: it's
            // still "we couldn't get usable text out of this upload",
            // just at an earlier step.
            pdfBytes = file.getBytes();
        } catch (IOException e) {
            throw new PdfTextExtractionException(
                    "Failed to read uploaded file: " + file.getOriginalFilename(), e);
        }

        // This is the reuse the task asked for: DocumentService does NOT
        // duplicate any PDFBox code. It only calls the existing,
        // already-tested PdfTextExtractionService.
        String extractedText = pdfTextExtractionService.extractText(pdfBytes);

        // NEW IN THIS PHASE: persist the Document first, so it has a
        // database-generated id that each chunk can reference via its
        // foreign key. There's no separate "title" collected on upload
        // (unlike the metadata-only POST /api/documents endpoint), so we
        // use the original filename for both fields — a reasonable
        // default for now, revisit if uploads ever need a title field.
        Document document = new Document();
        document.setTitle(file.getOriginalFilename());
        document.setFileName(file.getOriginalFilename());
        Document savedDocument = documentRepository.save(document);

        // Reuse TextChunkingService exactly as-is — DocumentService
        // doesn't know or care how chunking works internally.
        List<String> textChunks = textChunkingService.chunkText(extractedText);

        // Build one DocumentChunk per chunk string, explicitly assigning
        // chunkIndex from the loop position so the original order is
        // preserved once these rows land in the database (see
        // DocumentChunkRepository for how that order gets read back).
        List<DocumentChunk> chunkEntities = new ArrayList<>();
        for (int index = 0; index < textChunks.size(); index++) {
            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(savedDocument);
            chunk.setChunkIndex(index);
            chunk.setChunkText(textChunks.get(index));
            chunkEntities.add(chunk);
        }
        documentChunkRepository.saveAll(chunkEntities);

        return new PdfExtractionResponse(
                savedDocument.getId(),
                file.getOriginalFilename(),
                extractedText,
                chunkEntities.size()
        );
    }

    /**
     * Business-level validation for "is this an acceptable upload for
     * this feature". This is deliberately kept in the service, not the
     * controller: "must be non-empty" is a generic upload concern, but
     * "must be a PDF" is a rule specific to what THIS endpoint does with
     * the file — a different upload endpoint elsewhere in the app might
     * accept a different file type entirely. Keeping both checks
     * together here also means DocumentController stays a pure pass-
     * through, consistent with how thin it already is for the other four
     * endpoints.
     */
    private void validatePdfFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file must not be empty");
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        boolean hasPdfContentType = "application/pdf".equalsIgnoreCase(contentType);
        boolean hasPdfExtension = filename != null && filename.toLowerCase().endsWith(".pdf");

        if (!hasPdfContentType && !hasPdfExtension) {
            throw new InvalidFileException("Uploaded file must be a PDF");
        }
    }

    // Private mapping helper: keeps entity -> DTO conversion in one place.
    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getFileName(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}