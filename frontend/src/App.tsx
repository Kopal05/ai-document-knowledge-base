import { useState } from "react";
import { useAuth } from "./context/AuthContext";
import { AuthPage } from "./pages/AuthPage";
import { DocumentsPage } from "./pages/DocumentsPage";
import { AskPage } from "./pages/AskPage";
import type { DocumentResponse } from "./types";

export default function App() {
  const { token } = useAuth();
  const [selectedDocument, setSelectedDocument] = useState<DocumentResponse | null>(null);

  if (!token) return <AuthPage />;

  if (selectedDocument) {
    return <AskPage document={selectedDocument} onBack={() => setSelectedDocument(null)} />;
  }

  return <DocumentsPage onOpenDocument={setSelectedDocument} />;
}