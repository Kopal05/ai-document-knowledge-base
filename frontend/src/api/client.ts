const BASE_URL = import.meta.env.VITE_API_BASE_URL as string;

export class ApiError extends Error {
    status: number;
    constructor(status: number, message: string) {
        super(message);
        this.status = status;
    }
}

interface RequestOptions {
    method?: string;
    token?: string | null;
    body?: unknown;
    isFormData?: boolean;
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
    const { method = "GET", token, body, isFormData = false } = options;

    const headers: Record<string, string> = {};
    if (token) headers["Authorization"] = `Bearer ${token}`;
    if (body && !isFormData) headers["Content-Type"] = "application/json";

    const response = await fetch(`${BASE_URL}${path}`, {
        method,
        headers,
        body: isFormData ? (body as FormData) : body ? JSON.stringify(body) : undefined,
    });

    if (response.status === 204) {
        return undefined as T;
    }

    const data = await response.json().catch(() => null);

    if (!response.ok) {
        const message = (data && data.message) || `Request failed with status ${response.status}`;
        throw new ApiError(response.status, message);
    }

    return data as T;
}