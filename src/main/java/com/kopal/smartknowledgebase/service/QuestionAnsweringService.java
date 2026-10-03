package com.kopal.smartknowledgebase.service;

import com.kopal.smartknowledgebase.dto.AskResponse;
import com.kopal.smartknowledgebase.dto.SearchResponse;
import com.kopal.smartknowledgebase.dto.SearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orchestrates the RAG flow: retrieve relevant chunks (reusing
 * SemanticSearchService), build a grounded prompt, and ask the chat
 * provider to answer using only that context.
 *
 * WHY THIS IS ITS OWN SERVICE, NOT ADDED TO SemanticSearchService:
 * SemanticSearchService's one job is "find relevant chunks." Turning
 * those chunks into a natural-language ANSWER — building a prompt,
 * calling an LLM, deciding what "no information available" means — is a
 * distinct concern with its own failure modes and its own reason to
 * change independently. Same one-class-one-job reasoning behind every
 * other service split in this project.
 *
 * NOTHING HERE TALKS TO PDFBox, PostgreSQL, OR HTTP DIRECTLY:
 * It depends on SemanticSearchService (for retrieval) and
 * ChatCompletionClient (an interface, for generation) — exactly two
 * collaborators, each already solving one piece of this puzzle.
 */
@Service
public class QuestionAnsweringService {

    private static final String NO_INFORMATION_ANSWER =
            "I don't have enough information in this document to answer that question.";

    private final SemanticSearchService semanticSearchService;
    private final ChatCompletionClient chatCompletionClient;
    private final int contextChunkCount;

    public QuestionAnsweringService(SemanticSearchService semanticSearchService,
                                    ChatCompletionClient chatCompletionClient,
                                    @Value("${rag.context-chunk-count:5}") int contextChunkCount) {
        this.semanticSearchService = semanticSearchService;
        this.chatCompletionClient = chatCompletionClient;
        this.contextChunkCount = contextChunkCount;
    }

    public AskResponse ask(Long documentId, String question) {
        // REUSE, not reimplement: this one call already validates the
        // question isn't blank (InvalidSearchQueryException), confirms
        // the document exists (DocumentNotFoundException), generates the
        // question's embedding via the existing EmbeddingService, and
        // runs the existing pgvector similarity search. All three of
        // those exceptions propagate straight out of this method
        // unchanged — GlobalExceptionHandler already knows how to turn
        // each into the right HTTP response.
        SearchResponse searchResponse = semanticSearchService.search(documentId, question, contextChunkCount);
        List<SearchResult> sources = searchResponse.getResults();

        if (sources.isEmpty()) {
            // No chunks relevant enough to ground an answer in. This is
            // the same "empty results isn't an error" philosophy from
            // semantic search, taken to its natural conclusion here:
            // rather than ask the LLM to answer with no context (risking
            // it inventing something), we return a clear, honest,
            // deterministic answer — and skip the LLM call entirely, so
            // this costs nothing extra.
            return new AskResponse(question, NO_INFORMATION_ANSWER, List.of());
        }

        String prompt = buildPrompt(question, sources);

        // REUSE, not reimplement: ChatCompletionClient is the only thing
        // in this method that knows an LLM exists. If its call fails,
        // AnswerGenerationException propagates unchanged — already
        // handled by GlobalExceptionHandler, mirroring
        // EmbeddingGenerationException's handling exactly.
        String answer = chatCompletionClient.generateAnswer(prompt);

        return new AskResponse(question, answer.strip(), sources);
    }

    /**
     * Builds a prompt that instructs the model to answer ONLY from the
     * supplied context, and to say so plainly if the context doesn't
     * contain the answer — directly implementing the "don't invent
     * information" requirement for the common case (some, but possibly
     * insufficient, context was found).
     */
    private String buildPrompt(String question, List<SearchResult> sources) {
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            context.append("[").append(i + 1).append("] ")
                    .append(sources.get(i).getChunkText())
                    .append("\n\n");
        }

        return """
                You are a helpful assistant answering questions using ONLY the context below, taken from a document.
                If the answer is not contained in the context, respond EXACTLY with: "%s"
                Do not use any knowledge beyond what is given below, and do not guess or make anything up.

                Context:
                %s
                Question: %s

                Answer:
                """.formatted(NO_INFORMATION_ANSWER, context, question);
    }
}