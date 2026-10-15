import { createContext, useContext, useEffect, useState } from "react";
import type { ReactNode } from "react";
import { apiRequest } from "../api/client";
import type { AuthResponse } from "../types";

interface AuthUser {
    userId: number;
    name: string;
    email: string;
}

interface AuthContextValue {
    token: string | null;
    user: AuthUser | null;
    login: (email: string, password: string) => Promise<void>;
    register: (name: string, email: string, password: string) => Promise<void>;
    logout: () => void;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

const STORAGE_KEY = "kb_auth";

export function AuthProvider({ children }: { children: ReactNode }) {
    const [token, setToken] = useState<string | null>(null);
    const [user, setUser] = useState<AuthUser | null>(null);

    useEffect(() => {
        const stored = localStorage.getItem(STORAGE_KEY);
        if (stored) {
            try {
                const parsed = JSON.parse(stored) as { token: string; user: AuthUser };
                setToken(parsed.token);
                setUser(parsed.user);
            } catch {
                localStorage.removeItem(STORAGE_KEY);
            }
        }
    }, []);

    function persist(authResponse: AuthResponse) {
        const authUser: AuthUser = {
            userId: authResponse.userId,
            name: authResponse.name,
            email: authResponse.email,
        };
        setToken(authResponse.token);
        setUser(authUser);
        localStorage.setItem(STORAGE_KEY, JSON.stringify({ token: authResponse.token, user: authUser }));
    }

    async function login(email: string, password: string) {
        const response = await apiRequest<AuthResponse>("/api/auth/login", {
            method: "POST",
            body: { email, password },
        });
        persist(response);
    }

    async function register(name: string, email: string, password: string) {
        await apiRequest("/api/auth/register", {
            method: "POST",
            body: { name, email, password },
        });
        // Registration doesn't return a token, so log in right after.
        await login(email, password);
    }

    function logout() {
        setToken(null);
        setUser(null);
        localStorage.removeItem(STORAGE_KEY);
    }

    return (
        <AuthContext.Provider value={{ token, user, login, register, logout }}>
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const ctx = useContext(AuthContext);
    if (!ctx) throw new Error("useAuth must be used within AuthProvider");
    return ctx;
}