package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.dto.CreateDocumentRequest;
import com.kopal.smartknowledgebase.dto.DocumentResponse;
import com.kopal.smartknowledgebase.dto.PdfExtractionResponse;
import com.kopal.smartknowledgebase.entity.Document;
import com.kopal.smartknowledgebase.entity.DocumentChunk;
import com.kopal.smartknowledgebase.exception.DocumentNotFoundException;
import com.kopal.smartknowledgebase.exception.EmbeddingGenerationException;
import com.kopal.smartknowledgebase.exception.InvalidFileException;
import com.kopal.smartknowledgebase.repository.DocumentChunkRepository;
import com.kopal.smartknowledgebase.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Example test showing how to unit test the SERVICE layer in isolation,
 * without starting the full Spring context or touching a real database.
 *
 * WHY THIS FILE EXISTS (as an EXAMPLE, not a full suite):
 * - @ExtendWith(MockitoExtension.class) enables Mockito annotations.
 * - @Mock creates a fake DocumentRepository whose behavior we control with
 *   when(...).thenReturn(...), instead of hitting a real database.
 * - We construct DocumentService ourselves with `new DocumentService(...)`
 *   — this is only possible because the service uses constructor
 *   injection instead of field injection, which is one of the reasons
 *   constructor injection is preferred.
 *
 * This is intentionally ONE example test per method style (happy path +
 * one not-found case), plus focused tests for extractTextFromPdf(...)
 * — expand this yourself as you build more features.
 *
 * DocumentService now depends on FIVE collaborators. This is the direct
 * payoff of constructor injection: when a class's dependency list
 * changes, the test just adds more @Mock fields and constructor
 * arguments — no Spring context, no wiring changes anywhere else.
 */
@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private PdfTextExtractionService pdfTextExtractionService;

    @Mock
    private TextChunkingService textChunkingService;

    @Mock
    private DocumentChunkRepository documentChunkRepository;

    @Mock
    private EmbeddingService embeddingService;

    private DocumentService documentService;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(
                documentRepository, pdfTextExtractionService, textChunkingService,
                documentChunkRepository, embeddingService);
    }

    @Test
    void createDocument_shouldReturnSavedDocumentAsResponse() {
        CreateDocumentRequest request = new CreateDocumentRequest("My Doc", "my-doc.pdf");

        Document savedEntity = new Document();
        savedEntity.setId(1L);
        savedEntity.setTitle("My Doc");
        savedEntity.setFileName("my-doc.pdf");
        savedEntity.setCreatedAt(LocalDateTime.now());
        savedEntity.setUpdatedAt(LocalDateTime.now());

        when(documentRepository.save(any(Document.class))).thenReturn(savedEntity);

        DocumentResponse response = documentService.createDocument(request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("My Doc");
        assertThat(response.getFileName()).isEqualTo("my-doc.pdf");
    }

    @Test
    void getDocumentById_shouldThrowWhenDocumentDoesNotExist() {
        when(documentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getDocumentById(99L))
                .isInstanceOf(DocumentNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void extractTextFromPdf_shouldPersistDocumentAndReturnItsIdWithChunkCount() {
        MockMultipartFile file = new MockMultipartFile(
                "file",                    // the request part name
                "sample.pdf",               // original filename
                "application/pdf",          // content type
                "irrelevant bytes".getBytes() // content itself is never real
                // PDF content here, because we
                // mock PdfTextExtractionService
                // below instead of letting real
                // PDFBox parse it — this test
                // is only about DocumentService's
                // own logic, not PDFBox's.
        );

        when(pdfTextExtractionService.extractText(any(byte[].class)))
                .thenReturn("extracted text from the PDF");

        // Simulate the database assigning an id when the Document is saved.
        Document savedDocument = new Document();
        savedDocument.setId(42L);
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

        when(textChunkingService.chunkText("extracted text from the PDF"))
                .thenReturn(List.of("chunk zero", "chunk one", "chunk two"));

        when(embeddingService.generateEmbedding(anyString()))
                .thenReturn(new float[DocumentChunk.EMBEDDING_DIMENSIONS]);

        PdfExtractionResponse response = documentService.extractTextFromPdf(file);

        assertThat(response.getDocumentId()).isEqualTo(42L);
        assertThat(response.getFileName()).isEqualTo("sample.pdf");
        assertThat(response.getText()).isEqualTo("extracted text from the PDF");
        assertThat(response.getChunkCount()).isEqualTo(3);
    }

    @Test
    void extractTextFromPdf_shouldSaveChunksWithCorrectIndexAndDocumentReference() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.pdf", "application/pdf", "irrelevant bytes".getBytes()
        );

        when(pdfTextExtractionService.extractText(any(byte[].class)))
                .thenReturn("some extracted text");

        Document savedDocument = new Document();
        savedDocument.setId(7L);
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

        when(textChunkingService.chunkText("some extracted text"))
                .thenReturn(List.of("first chunk", "second chunk"));

        when(embeddingService.generateEmbedding(anyString()))
                .thenReturn(new float[DocumentChunk.EMBEDDING_DIMENSIONS]);

        documentService.extractTextFromPdf(file);

        // Capture exactly what DocumentService handed to the repository,
        // so we can verify the mapping logic itself — not just that
        // *something* was saved.
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentChunk>> chunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(documentChunkRepository).saveAll(chunksCaptor.capture());

        List<DocumentChunk> savedChunks = chunksCaptor.getValue();
        assertThat(savedChunks).hasSize(2);

        assertThat(savedChunks.get(0).getChunkIndex()).isEqualTo(0);
        assertThat(savedChunks.get(0).getChunkText()).isEqualTo("first chunk");
        assertThat(savedChunks.get(0).getDocument()).isSameAs(savedDocument);

        assertThat(savedChunks.get(1).getChunkIndex()).isEqualTo(1);
        assertThat(savedChunks.get(1).getChunkText()).isEqualTo("second chunk");
        assertThat(savedChunks.get(1).getDocument()).isSameAs(savedDocument);
    }

    @Test
    void extractTextFromPdf_shouldGenerateAndAssignA768DimensionEmbeddingToEachChunk() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.pdf", "application/pdf", "irrelevant bytes".getBytes()
        );

        when(pdfTextExtractionService.extractText(any(byte[].class)))
                .thenReturn("some extracted text");

        Document savedDocument = new Document();
        savedDocument.setId(9L);
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

        when(textChunkingService.chunkText("some extracted text"))
                .thenReturn(List.of("first chunk", "second chunk"));

        // Two DIFFERENT dummy embeddings, so we can confirm each chunk
        // gets the embedding that actually corresponds to ITS text, not
        // just any embedding reused across all chunks.
        float[] embeddingForFirstChunk = new float[DocumentChunk.EMBEDDING_DIMENSIONS];
        embeddingForFirstChunk[0] = 0.111f;
        float[] embeddingForSecondChunk = new float[DocumentChunk.EMBEDDING_DIMENSIONS];
        embeddingForSecondChunk[0] = 0.222f;

        when(embeddingService.generateEmbedding("first chunk")).thenReturn(embeddingForFirstChunk);
        when(embeddingService.generateEmbedding("second chunk")).thenReturn(embeddingForSecondChunk);

        documentService.extractTextFromPdf(file);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentChunk>> chunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(documentChunkRepository).saveAll(chunksCaptor.capture());
        List<DocumentChunk> savedChunks = chunksCaptor.getValue();

        assertThat(savedChunks).hasSize(2);

        assertThat(savedChunks.get(0).getEmbedding()).hasSize(DocumentChunk.EMBEDDING_DIMENSIONS);
        assertThat(savedChunks.get(0).getEmbedding()[0]).isEqualTo(0.111f);

        assertThat(savedChunks.get(1).getEmbedding()).hasSize(DocumentChunk.EMBEDDING_DIMENSIONS);
        assertThat(savedChunks.get(1).getEmbedding()[0]).isEqualTo(0.222f);
    }

    @Test
    void extractTextFromPdf_shouldPropagateEmbeddingGenerationFailureAndNeverSaveAnyChunks() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.pdf", "application/pdf", "irrelevant bytes".getBytes()
        );

        when(pdfTextExtractionService.extractText(any(byte[].class)))
                .thenReturn("some extracted text");

        Document savedDocument = new Document();
        savedDocument.setId(11L);
        when(documentRepository.save(any(Document.class))).thenReturn(savedDocument);

        when(textChunkingService.chunkText("some extracted text"))
                .thenReturn(List.of("first chunk", "second chunk"));

        when(embeddingService.generateEmbedding(anyString()))
                .thenThrow(new EmbeddingGenerationException("Failed to reach the embedding provider"));

        assertThatThrownBy(() -> documentService.extractTextFromPdf(file))
                .isInstanceOf(EmbeddingGenerationException.class);

        // Confirms DocumentService doesn't catch-and-continue: if even
        // one chunk's embedding fails, saveAll is never reached at all —
        // consistent with letting @Transactional roll back everything,
        // including the Document row already saved above.
        verify(documentChunkRepository, never()).saveAll(any());
    }

    @Test
    void extractTextFromPdf_shouldThrowForAnEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]
        );

        assertThatThrownBy(() -> documentService.extractTextFromPdf(emptyFile))
                .isInstanceOf(InvalidFileException.class);
    }

    @Test
    void extractTextFromPdf_shouldThrowForANonPdfFile() {
        MockMultipartFile textFile = new MockMultipartFile(
                "file", "notes.txt", "text/plain", "just some text".getBytes()
        );

        assertThatThrownBy(() -> documentService.extractTextFromPdf(textFile))
                .isInstanceOf(InvalidFileException.class);
    }
}