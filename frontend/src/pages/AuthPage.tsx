import { useState } from "react";
import type { FormEvent } from "react";
import { useAuth } from "../context/AuthContext";
import { ApiError } from "../api/client";

export function AuthPage() {
    const { login, register } = useAuth();
    const [mode, setMode] = useState<"signin" | "register">("signin");
    const [name, setName] = useState("");
    const [email, setEmail] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    async function handleSubmit(e: FormEvent) {
        e.preventDefault();
        setError(null);
        setLoading(true);
        try {
            if (mode === "signin") {
                await login(email, password);
            } else {
                await register(name, email, password);
            }
        } catch (err) {
            setError(err instanceof ApiError ? err.message : "Something went wrong. Try again.");
        } finally {
            setLoading(false);
        }
    }

    return (
        <div className="page-content">
            <div className="auth-wrap">
                <div className="auth-tabs">
                    <button
                        type="button"
                        className={`auth-tab ${mode === "signin" ? "active" : ""}`}
                        onClick={() => { setMode("signin"); setError(null); }}
                    >
                        Sign in
                    </button>
                    <button
                        type="button"
                        className={`auth-tab ${mode === "register" ? "active" : ""}`}
                        onClick={() => { setMode("register"); setError(null); }}
                    >
                        Create account
                    </button>
                </div>

                <form onSubmit={handleSubmit}>
                    {mode === "register" && (
                        <div className="field">
                            <label htmlFor="name">Name</label>
                            <input id="name" value={name} onChange={(e) => setName(e.target.value)} required />
                        </div>
                    )}
                    <div className="field">
                        <label htmlFor="email">Email</label>
                        <input id="email" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
                    </div>
                    <div className="field">
                        <label htmlFor="password">Password</label>
                        <input id="password" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required minLength={8} />
                    </div>

                    <button className="btn" type="submit" disabled={loading}>
                        {loading ? "Please wait..." : mode === "signin" ? "Sign in" : "Create account"}
                    </button>

                    {error && <p className="error-text">{error}</p>}
                </form>

                <p className="hint-text">
                    {mode === "signin" ? (
                        <>No account? <button type="button" onClick={() => setMode("register")}>Create one</button></>
                    ) : (
                        <>Already have an account? <button type="button" onClick={() => setMode("signin")}>Sign in</button></>
                    )}
                </p>
            </div>
        </div>
    );
}