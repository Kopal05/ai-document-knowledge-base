import { ChangeEvent, useEffect, useState } from "react";
import { useAuth } from "../context/AuthContext";
import { apiRequest, ApiError } from "../api/client";
import type { DocumentResponse, PdfExtractionResponse } from "../types";

interface Props {
    onOpenDocument: (doc: DocumentResponse) => void;
}

export function DocumentsPage({ onOpenDocument }: Props) {
    const { token, user, logout } = useAuth();
    const [documents, setDocuments] = useState<DocumentResponse[]>([]);
    const [loading, setLoading] = useState(true);
    const [uploading, setUploading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    async function fetchDocuments() {
        setLoading(true);
        setError(null);
        try {
            const data = await apiRequest<DocumentResponse[]>("/api/documents", { token });
            setDocuments(data);
        } catch (err) {
            handleError(err);
        } finally {
            setLoading(false);
        }
    }

    useEffect(() => {
        fetchDocuments();
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    function handleError(err: unknown) {
        if (err instanceof ApiError && err.status === 401) {
            logout();
            return;
        }
        setError(err instanceof ApiError ? err.message : "Something went wrong.");
    }

    async function handleUpload(e: ChangeEvent<HTMLInputElement>) {
        const file = e.target.files?.[0];
        e.target.value = "";
        if (!file) return;

        setUploading(true);
        setError(null);
        try {
            const formData = new FormData();
            formData.append("file", file);
            await apiRequest<PdfExtractionResponse>("/api/documents/upload", {
                method: "POST",
                token,
                body: formData,
                isFormData: true,
            });
            await fetchDocuments();
        } catch (err) {
            handleError(err);
        } finally {
            setUploading(false);
        }
    }

    async function handleDelete(id: number, e: React.MouseEvent) {
        e.stopPropagation();
        try {
            await apiRequest(`/api/documents/${id}`, { method: "DELETE", token });
            setDocuments((docs) => docs.filter((d) => d.id !== id));
        } catch (err) {
            handleError(err);
        }
    }

    return (
        <>
            <div className="topbar">
                <span className="brand">Knowledge Base</span>
                <span className="who">
          {user?.email}
                    <button onClick={logout}>Sign out</button>
        </span>
            </div>

            <div className="page-content">
                <div className="docs-head">
                    <h2>Your documents</h2>
                    <label className="upload-btn">
                        {uploading ? "Uploading..." : "+ Upload a PDF"}
                        <input type="file" accept="application/pdf" onChange={handleUpload} disabled={uploading} />
                    </label>
                </div>

                {error && <p className="error-text">{error}</p>}

                {loading ? (
                    <p className="loading-text">Loading...</p>
                ) : documents.length === 0 ? (
                    <p className="empty-state">No documents yet. Upload a PDF to get started.</p>
                ) : (
                    documents.map((doc) => (
                        <button key={doc.id} className="doc-row" onClick={() => onOpenDocument(doc)}>
                            <div className="meta">
                                <div className="fname">{doc.title || doc.fileName}</div>
                                <div className="sub">uploaded {new Date(doc.createdAt).toLocaleDateString()}</div>
                            </div>
                            <div className="open">Open →</div>
                            <button className="del" onClick={(e) => handleDelete(doc.id, e)} aria-label="Delete document">×</button>
                        </button>
                    ))
                )}
            </div>
        </>
    );
}