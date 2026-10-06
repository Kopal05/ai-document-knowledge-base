package com.kopal.smartknowledgebase.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kopal.smartknowledgebase.config.SecurityConfig;
import com.kopal.smartknowledgebase.dto.*;
import com.kopal.smartknowledgebase.entity.User;
import com.kopal.smartknowledgebase.security.CustomUserDetails;
import com.kopal.smartknowledgebase.security.CustomUserDetailsService;
import com.kopal.smartknowledgebase.security.JsonAccessDeniedHandler;
import com.kopal.smartknowledgebase.security.JsonAuthenticationEntryPoint;
import com.kopal.smartknowledgebase.security.JwtAuthenticationFilter;
import com.kopal.smartknowledgebase.security.JwtService;
import com.kopal.smartknowledgebase.service.DocumentService;
import com.kopal.smartknowledgebase.service.QuestionAnsweringService;
import com.kopal.smartknowledgebase.service.SemanticSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DocumentController.class)
@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class,
        JwtAuthenticationFilter.class})
class DocumentControllerTest {

    private static final Long OWNER_ID = 100L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DocumentService documentService;

    @MockBean
    private SemanticSearchService semanticSearchService;

    @MockBean
    private QuestionAnsweringService questionAnsweringService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    private CustomUserDetails authenticatedUser() {
        User user = new User();
        user.setId(OWNER_ID);
        user.setName("Test User");
        user.setEmail("test@example.com");
        return new CustomUserDetails(user);
    }

    @Test
    void createDocumentReturns201WhenAuthenticated() throws Exception {
        DocumentResponse response = new DocumentResponse(1L, "My Doc", "my-doc.pdf", LocalDateTime.now(), LocalDateTime.now());
        when(documentService.createDocument(any(), eq(OWNER_ID))).thenReturn(response);

        CreateDocumentRequest request = new CreateDocumentRequest();
        request.setTitle("My Doc");
        request.setFileName("my-doc.pdf");

        mockMvc.perform(post("/api/documents")
                        .with(user(authenticatedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("My Doc"));
    }

    @Test
    void createDocumentReturns401WhenNotAuthenticated() throws Exception {
        CreateDocumentRequest request = new CreateDocumentRequest();
        request.setTitle("My Doc");

        mockMvc.perform(post("/api/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getAllDocumentsReturnsOwnedDocuments() throws Exception {
        when(documentService.getAllDocuments(OWNER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/documents").with(user(authenticatedUser())))
                .andExpect(status().isOk());
    }

    @Test
    void getDocumentByIdReturns200WhenFound() throws Exception {
        DocumentResponse response = new DocumentResponse(1L, "Doc", "doc.pdf", LocalDateTime.now(), LocalDateTime.now());
        when(documentService.getDocumentById(1L, OWNER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/documents/1").with(user(authenticatedUser())))
                .andExpect(status().isOk());
    }

    @Test
    void deleteDocumentReturns204() throws Exception {
        mockMvc.perform(delete("/api/documents/1").with(user(authenticatedUser())))
                .andExpect(status().isNoContent());
    }

    @Test
    void uploadAndExtractTextReturns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", "dummy".getBytes());
        PdfExtractionResponse response = new PdfExtractionResponse(1L, "resume.pdf", "extracted text", 3);
        when(documentService.extractTextFromPdf(any(), eq(OWNER_ID))).thenReturn(response);

        mockMvc.perform(multipart("/api/documents/upload")
                        .file(file)
                        .with(user(authenticatedUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chunkCount").value(3));
    }

    @Test
    void searchDocumentReturns200() throws Exception {
        SearchResponse response = new SearchResponse("query", List.of());
        when(semanticSearchService.search(1L, "query", null, OWNER_ID)).thenReturn(response);

        mockMvc.perform(get("/api/documents/1/search")
                        .param("query", "query")
                        .with(user(authenticatedUser())))
                .andExpect(status().isOk());
    }

    @Test
    void askDocumentReturns200() throws Exception {
        AskResponse response = new AskResponse("What is this?", "An answer.", List.of());
        when(questionAnsweringService.ask(eq(1L), anyString(), eq(OWNER_ID))).thenReturn(response);

        AskRequest request = new AskRequest();
        request.setQuestion("What is this?");

        mockMvc.perform(post("/api/documents/1/ask")
                        .with(user(authenticatedUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("An answer."));
    }
}