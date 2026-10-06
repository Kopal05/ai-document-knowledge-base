package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.dto.CreateDocumentRequest;
import com.kopal.smartknowledgebase.dto.DocumentResponse;
import com.kopal.smartknowledgebase.dto.PdfExtractionResponse;
import com.kopal.smartknowledgebase.entity.Document;
import com.kopal.smartknowledgebase.entity.User;
import com.kopal.smartknowledgebase.exception.DocumentNotFoundException;
import com.kopal.smartknowledgebase.exception.InvalidFileException;
import com.kopal.smartknowledgebase.repository.DocumentChunkRepository;
import com.kopal.smartknowledgebase.repository.DocumentRepository;
import com.kopal.smartknowledgebase.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    private static final Long OWNER_ID = 100L;

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private PdfTextExtractionService pdfTextExtractionService;

    @Mock
    private TextChunkingService textChunkingService;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private UserRepository userRepository;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
                documentRepository,
                documentChunkRepository,
                pdfTextExtractionService,
                textChunkingService,
                embeddingService,
                userRepository);
    }

    @Test
    void createsDocumentForOwner() {
        User owner = new User();
        owner.setId(OWNER_ID);
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId(1L);
            return doc;
        });

        CreateDocumentRequest request = new CreateDocumentRequest();
        request.setTitle("My Doc");
        request.setFileName("my-doc.pdf");

        DocumentResponse response = documentService.createDocument(request, OWNER_ID);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("My Doc");

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(documentRepository).save(captor.capture());
        assertThat(captor.getValue().getOwner()).isEqualTo(owner);
    }

    @Test
    void getAllDocumentsReturnsOnlyOwnedDocuments() {
        Document doc = new Document();
        doc.setId(1L);
        doc.setTitle("Doc 1");
        when(documentRepository.findAllByOwnerId(OWNER_ID)).thenReturn(List.of(doc));

        List<DocumentResponse> responses = documentService.getAllDocuments(OWNER_ID);

        assertThat(responses).hasSize(1);
        verify(documentRepository).findAllByOwnerId(OWNER_ID);
    }

    @Test
    void getDocumentByIdReturnsDocumentWhenOwnedByCaller() {
        Document doc = new Document();
        doc.setId(1L);
        doc.setTitle("Doc 1");
        when(documentRepository.findByIdAndOwnerId(1L, OWNER_ID)).thenReturn(Optional.of(doc));

        DocumentResponse response = documentService.getDocumentById(1L, OWNER_ID);

        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    void getDocumentByIdThrowsNotFoundWhenNotOwnedByCaller() {
        when(documentRepository.findByIdAndOwnerId(1L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocumentById(1L, OWNER_ID))
                .isInstanceOf(DocumentNotFoundException.class);
    }

    @Test
    void deleteDocumentThrowsNotFoundWhenNotOwnedByCaller() {
        when(documentRepository.findByIdAndOwnerId(1L, OWNER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.deleteDocument(1L, OWNER_ID))
                .isInstanceOf(DocumentNotFoundException.class);

        verify(documentRepository, never()).delete(any());
    }

    @Test
    void deleteDocumentDeletesWhenOwnedByCaller() {
        Document doc = new Document();
        doc.setId(1L);
        when(documentRepository.findByIdAndOwnerId(1L, OWNER_ID)).thenReturn(Optional.of(doc));

        documentService.deleteDocument(1L, OWNER_ID);

        verify(documentRepository).delete(doc);
    }

    @Test
    void extractTextFromPdfCreatesDocumentAndChunksWithEmbeddings() {
        User owner = new User();
        owner.setId(OWNER_ID);
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);

        MultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "dummy-pdf-bytes".getBytes());

        when(pdfTextExtractionService.extractText(any())).thenReturn("Some extracted text");
        when(textChunkingService.chunkText("Some extracted text"))
                .thenReturn(List.of("chunk 0", "chunk 1"));
        when(embeddingService.generateEmbedding(anyString()))
                .thenReturn(new float[]{0.1f, 0.2f});
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId(5L);
            return doc;
        });

        PdfExtractionResponse response = documentService.extractTextFromPdf(file, OWNER_ID);

        assertThat(response.getDocumentId()).isEqualTo(5L);
        assertThat(response.getChunkCount()).isEqualTo(2);

        verify(embeddingService, times(2)).generateEmbedding(anyString());
        verify(documentChunkRepository).saveAll(anyList());
    }

    @Test
    void extractTextFromPdfPropagatesEmbeddingFailure() {
        User owner = new User();
        owner.setId(OWNER_ID);
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);

        MultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "dummy-pdf-bytes".getBytes());

        when(pdfTextExtractionService.extractText(any())).thenReturn("text");
        when(textChunkingService.chunkText("text")).thenReturn(List.of("chunk 0"));
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document doc = invocation.getArgument(0);
            doc.setId(5L);
            return doc;
        });
        when(embeddingService.generateEmbedding(anyString()))
                .thenThrow(new com.kopal.smartknowledgebase.exception.EmbeddingGenerationException("down"));

        assertThatThrownBy(() -> documentService.extractTextFromPdf(file, OWNER_ID))
                .isInstanceOf(com.kopal.smartknowledgebase.exception.EmbeddingGenerationException.class);

        verify(documentChunkRepository, never()).saveAll(anyList());
    }

    @Test
    void extractTextFromPdfRejectsNonPdfContentType() {
        MultipartFile file = new MockMultipartFile(
                "file", "resume.txt", "text/plain", "not a pdf".getBytes());

        assertThatThrownBy(() -> documentService.extractTextFromPdf(file, OWNER_ID))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void extractTextFromPdfRejectsEmptyFile() {
        MultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> documentService.extractTextFromPdf(file, OWNER_ID))
                .isInstanceOf(InvalidFileException.class);
    }
}