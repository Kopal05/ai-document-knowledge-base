package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.dto.CreateDocumentRequest;
import com.kopal.smartknowledgebase.dto.DocumentResponse;
import com.kopal.smartknowledgebase.dto.PdfExtractionResponse;
import com.kopal.smartknowledgebase.entity.Document;
import com.kopal.smartknowledgebase.entity.DocumentChunk;
import com.kopal.smartknowledgebase.entity.User;
import com.kopal.smartknowledgebase.exception.DocumentNotFoundException;
import com.kopal.smartknowledgebase.exception.InvalidFileException;
import com.kopal.smartknowledgebase.exception.PdfTextExtractionException;
import com.kopal.smartknowledgebase.repository.DocumentChunkRepository;
import com.kopal.smartknowledgebase.repository.DocumentRepository;
import com.kopal.smartknowledgebase.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final PdfTextExtractionService pdfTextExtractionService;
    private final TextChunkingService textChunkingService;
    private final EmbeddingService embeddingService;
    private final UserRepository userRepository;

    public DocumentService(
            DocumentRepository documentRepository,
            DocumentChunkRepository documentChunkRepository,
            PdfTextExtractionService pdfTextExtractionService,
            TextChunkingService textChunkingService,
            EmbeddingService embeddingService,
            UserRepository userRepository) {
        this.documentRepository = documentRepository;
        this.documentChunkRepository = documentChunkRepository;
        this.pdfTextExtractionService = pdfTextExtractionService;
        this.textChunkingService = textChunkingService;
        this.embeddingService = embeddingService;
        this.userRepository = userRepository;
    }

    public DocumentResponse createDocument(CreateDocumentRequest request, Long ownerId) {
        Document document = new Document();
        document.setTitle(request.getTitle());
        document.setFileName(request.getFileName());
        document.setOwner(userRepository.getReferenceById(ownerId));

        Document saved = documentRepository.save(document);
        return toResponse(saved);
    }

    public List<DocumentResponse> getAllDocuments(Long ownerId) {
        return documentRepository.findAllByOwnerId(ownerId).stream()
                .map(this::toResponse)
                .toList();
    }

    public DocumentResponse getDocumentById(Long id, Long ownerId) {
        Document document = documentRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new DocumentNotFoundException(id));
        return toResponse(document);
    }

    public void deleteDocument(Long id, Long ownerId) {
        Document document = documentRepository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new DocumentNotFoundException(id));
        documentRepository.delete(document);
    }

    @Transactional
    public PdfExtractionResponse extractTextFromPdf(MultipartFile file, Long ownerId) {
        validateFile(file);

        byte[] pdfBytes;
        try {
            pdfBytes = file.getBytes();
        } catch (IOException e) {
            throw new PdfTextExtractionException("Failed to read uploaded file", e);
        }

        String text = pdfTextExtractionService.extractText(pdfBytes);

        Document document = new Document();
        document.setTitle(file.getOriginalFilename());
        document.setFileName(file.getOriginalFilename());
        document.setOwner(userRepository.getReferenceById(ownerId));
        Document savedDocument = documentRepository.save(document);

        List<String> textChunks = textChunkingService.chunkText(text);

        List<DocumentChunk> chunkEntities = new java.util.ArrayList<>();
        for (int i = 0; i < textChunks.size(); i++) {
            String chunkText = textChunks.get(i);

            DocumentChunk chunk = new DocumentChunk();
            chunk.setDocument(savedDocument);
            chunk.setChunkText(chunkText);
            chunk.setChunkIndex(i);

            float[] embedding = embeddingService.generateEmbedding(chunkText);
            chunk.setEmbedding(embedding);

            chunkEntities.add(chunk);
        }

        documentChunkRepository.saveAll(chunkEntities);

        return new PdfExtractionResponse(
                savedDocument.getId(),
                savedDocument.getFileName(),
                text,
                chunkEntities.size());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("Uploaded file must not be empty");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new InvalidFileException("Uploaded file must be a PDF");
        }
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getFileName(),
                document.getCreatedAt(),
                document.getUpdatedAt());
    }
}