export interface UserResponse {
    id: number;
    name: string;
    email: string;
    createdAt: string;
}

export interface AuthResponse {
    token: string;
    userId: number;
    name: string;
    email: string;
}

export interface DocumentResponse {
    id: number;
    title: string;
    fileName: string;
    createdAt: string;
    updatedAt: string;
}

export interface PdfExtractionResponse {
    documentId: number;
    fileName: string;
    text: string;
    chunkCount: number;
}

export interface SearchResult {
    chunkId: number;
    documentId: number;
    chunkIndex: number;
    chunkText: string;
    similarity: number;
}

export interface SearchResponse {
    query: string;
    results: SearchResult[];
}

export interface AskResponse {
    question: string;
    answer: string;
    sources: SearchResult[];
}

export interface ErrorResponse {
    timestamp: string;
    status: number;
    error: string;
    message: string;
}