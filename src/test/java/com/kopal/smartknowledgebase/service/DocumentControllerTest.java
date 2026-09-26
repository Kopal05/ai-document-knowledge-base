package com.kopal.smartknowledgebase.controller;

import com.kopal.smartknowledgebase.dto.PdfExtractionResponse;
import com.kopal.smartknowledgebase.service.DocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A "slice test" for DocumentController's upload endpoint.
 *
 * WHY THIS IS DIFFERENT FROM DocumentServiceTest:
 * DocumentServiceTest is a pure unit test — no Spring involved at all.
 * This test is different on purpose: @WebMvcTest starts JUST the web
 * layer (controllers, MockMvc, JSON serialization, Spring's built-in
 * request handling) without starting a real server, without connecting
 * to PostgreSQL, and without loading the rest of the application. It's
 * "heavier" than a plain unit test but far lighter than @SpringBootTest.
 *
 * We need this specific test file because ONE scenario — "the client
 * didn't attach a file at all" — can't be unit tested against
 * DocumentService, since there's no MultipartFile object to even pass
 * in. That behavior (rejecting a missing required multipart part) is
 * Spring's own request-dispatching logic kicking in BEFORE our
 * controller method runs, so proving it works requires an actual
 * (mocked) HTTP request going through Spring MVC.
 *
 * @MockBean replaces the real DocumentService bean in the Spring context
 * with a Mockito mock, so this test never touches PdfTextExtractionService,
 * PDFBox, or the database either — it only verifies that DocumentController
 * wires things up and translates results/exceptions correctly.
 */
@WebMvcTest(DocumentController.class)
class DocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DocumentService documentService;

    @Test
    void uploadAndExtractText_shouldReturnExtractedTextForAValidUpload() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.pdf", "application/pdf", "dummy content".getBytes()
        );

        when(documentService.extractTextFromPdf(any()))
                .thenReturn(new PdfExtractionResponse(1L, "sample.pdf", "extracted text", 1));

        mockMvc.perform(multipart("/api/documents/upload").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentId").value(1))
                .andExpect(jsonPath("$.fileName").value("sample.pdf"))
                .andExpect(jsonPath("$.text").value("extracted text"))
                .andExpect(jsonPath("$.chunkCount").value(1));
    }

    @Test
    void uploadAndExtractText_shouldReturn400WhenNoFilePartIsSent() throws Exception {
        // No .file(...) attached at all — Spring itself rejects this
        // before DocumentController.uploadAndExtractText(...) ever runs,
        // because the "file" @RequestParam is required by default.
        mockMvc.perform(multipart("/api/documents/upload"))
                .andExpect(status().isBadRequest());
    }
}