import { FormEvent, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { apiRequest, ApiError } from "../api/client";
import type { DocumentResponse, AskResponse, SearchResponse, SearchResult } from "../types";

interface Props {
    document: DocumentResponse;
    onBack: () => void;
}

export function AskPage({ document, onBack }: Props) {
    const { token, logout } = useAuth();
    const [tab, setTab] = useState<"ask" | "search">("ask");
    const [question, setQuestion] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const [askResult, setAskResult] = useState<AskResponse | null>(null);
    const [searchResults, setSearchResults] = useState<SearchResult[] | null>(null);

    function handleError(err: unknown) {
        if (err instanceof ApiError && err.status === 401) {
            logout();
            return;
        }
        setError(err instanceof ApiError ? err.message : "Something went wrong.");
    }

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();
        if (!question.trim()) return;
        setLoading(true);
        setError(null);
        try {
            if (tab === "ask") {
                const data = await apiRequest<AskResponse>(`/api/documents/${document.id}/ask`, {
                    method: "POST",
                    token,
                    body: { question },
                });
                setAskResult(data);
            } else {
                const params = new URLSearchParams({ query: question });
                const data = await apiRequest<SearchResponse>(`/api/documents/${document.id}/search?${params}`, { token });
                setSearchResults(data.results);
            }
        } catch (err) {
            handleError(err);
        } finally {
            setLoading(false);
        }
    }

    function switchTab(next: "ask" | "search") {
        setTab(next);
        setError(null);
    }

    return (
        <div className="page-content">
            <button className="back-btn" onClick={onBack}>← Your documents</button>
            <h3 className="doc-title">{document.title || document.fileName}</h3>
            <p className="doc-sub">{document.fileName}</p>

            <div className="seg">
                <button className={tab === "ask" ? "active" : ""} onClick={() => switchTab("ask")}>Ask</button>
                <button className={tab === "search" ? "active" : ""} onClick={() => switchTab("search")}>Search</button>
            </div>

            <form className="ask-row" onSubmit={handleSubmit}>
                <input
                    value={question}
                    onChange={(e) => setQuestion(e.target.value)}
                    placeholder={tab === "ask" ? "Ask a question about this document" : "Search this document"}
                />
                <button className="btn auto" type="submit" disabled={loading}>
                    {loading ? "..." : tab === "ask" ? "Ask" : "Search"}
                </button>
            </form>

            {error && <p className="error-text">{error}</p>}

            {tab === "ask" && askResult && (
                <>
                    <div className="answer">
                        <p className="q-label">Question</p>
                        {askResult.question}
                        <p className="q-label" style={{ marginTop: 14 }}>Answer</p>
                        {askResult.answer}
                    </div>
                    {askResult.sources.length > 0 && (
                        <>
                            <p className="sources-label">Sources</p>
                            {askResult.sources.map((s) => (
                                <SourceCard key={s.chunkId} result={s} />
                            ))}
                        </>
                    )}
                </>
            )}

            {tab === "search" && searchResults && (
                <>
                    {searchResults.length === 0 ? (
                        <p className="empty-state">No matching chunks found.</p>
                    ) : (
                        searchResults.map((s) => <SourceCard key={s.chunkId} result={s} />)
                    )}
                </>
            )}
        </div>
    );
}

function SourceCard({ result }: { result: SearchResult }) {
    return (
        <div className="source">
            <div className="source-head">
                <span className="where mono">chunk {result.chunkIndex}</span>
                <span className="score mono">similarity {result.similarity.toFixed(2)}</span>
            </div>
            <p className="excerpt">{result.chunkText}</p>
        </div>
    );
}