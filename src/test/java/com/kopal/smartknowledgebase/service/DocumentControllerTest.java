package com.kopal.smartknowledgebase.controller;

import com.kopal.smartknowledgebase.dto.AskResponse;
import com.kopal.smartknowledgebase.dto.PdfExtractionResponse;
import com.kopal.smartknowledgebase.dto.SearchResponse;
import com.kopal.smartknowledgebase.dto.SearchResult;
import com.kopal.smartknowledgebase.service.DocumentService;
import com.kopal.smartknowledgebase.service.QuestionAnsweringService;
import com.kopal.smartknowledgebase.service.SemanticSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @MockBean
    private SemanticSearchService semanticSearchService;

    @MockBean
    private QuestionAnsweringService questionAnsweringService;

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

    @Test
    void searchDocument_shouldReturnSearchResultsForAValidQuery() throws Exception {
        SearchResult result = new SearchResult(17L, 1L, 3, "JWT chunk text", 0.91);
        when(semanticSearchService.search(eq(1L), eq("How does JWT work?"), any()))
                .thenReturn(new SearchResponse("How does JWT work?", List.of(result)));

        mockMvc.perform(get("/api/documents/1/search").param("query", "How does JWT work?"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.query").value("How does JWT work?"))
                .andExpect(jsonPath("$.results[0].chunkId").value(17))
                .andExpect(jsonPath("$.results[0].documentId").value(1))
                .andExpect(jsonPath("$.results[0].chunkIndex").value(3))
                .andExpect(jsonPath("$.results[0].chunkText").value("JWT chunk text"))
                .andExpect(jsonPath("$.results[0].similarity").value(0.91));
    }

    @Test
    void searchDocument_shouldReturn400WhenQueryParameterIsMissing() throws Exception {
        // No ?query=... at all — Spring itself rejects this before the
        // controller method runs, same pattern as the missing-file test
        // above, just for a regular request parameter instead of a
        // multipart part.
        mockMvc.perform(get("/api/documents/1/search"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchDocument_shouldPassTopKThroughWhenProvided() throws Exception {
        when(semanticSearchService.search(eq(1L), eq("some query"), eq(3)))
                .thenReturn(new SearchResponse("some query", List.of()));

        mockMvc.perform(get("/api/documents/1/search")
                        .param("query", "some query")
                        .param("topK", "3"))
                .andExpect(status().isOk());
    }

    @Test
    void askDocument_shouldReturnGeneratedAnswerForAValidQuestion() throws Exception {
        SearchResult source = new SearchResult(17L, 1L, 3, "JWT chunk text", 0.91);
        when(questionAnsweringService.ask(1L, "How does JWT authentication work?"))
                .thenReturn(new AskResponse(
                        "How does JWT authentication work?",
                        "JWT uses signed tokens for stateless authentication.",
                        List.of(source)));

        mockMvc.perform(post("/api/documents/1/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"How does JWT authentication work?\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question").value("How does JWT authentication work?"))
                .andExpect(jsonPath("$.answer").value("JWT uses signed tokens for stateless authentication."))
                .andExpect(jsonPath("$.sources[0].chunkId").value(17))
                .andExpect(jsonPath("$.sources[0].documentId").value(1));
    }

    @Test
    void askDocument_shouldReturn400WhenQuestionIsBlank() throws Exception {
        // @NotBlank + @Valid rejects this before the controller method
        // body runs — the project's existing Bean Validation handler
        // (MethodArgumentNotValidException), same pattern createDocument
        // already relies on.
        mockMvc.perform(post("/api/documents/1/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"question\": \"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void askDocument_shouldReturn400WhenQuestionFieldIsMissingEntirely() throws Exception {
        mockMvc.perform(post("/api/documents/1/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }
}