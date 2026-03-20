import {
  AdminDashboard,
  AuthResponse,
  CatalogItem,
  DataProduct,
  Entitlement,
  MarketData,
  MarketDataRuntimeStatus,
  PaymentTransaction,
  SessionState,
  UsageSummary,
  UserProfile
} from "./types";

export const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080";

type HttpMethod = "GET" | "POST";

async function request<T>(path: string, init?: RequestInit, token?: string): Promise<T> {
  const headers = new Headers(init?.headers);
  if (!headers.has("Content-Type") && init?.body) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(`${API_BASE_URL}${path}`, {
    ...init,
    headers,
    cache: "no-store"
  });

  if (!response.ok) {
    const text = await response.text();
    try {
      const parsed = JSON.parse(text) as { message?: string };
      throw new Error(parsed.message || text || `${response.status} ${response.statusText}`);
    } catch {
      throw new Error(text || `${response.status} ${response.statusText}`);
    }
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json() as Promise<T>;
}

function send<T>(path: string, method: HttpMethod, body?: unknown, token?: string) {
  return request<T>(
    path,
    {
      method,
      body: body ? JSON.stringify(body) : undefined
    },
    token
  );
}

export const api = {
  register: (payload: Record<string, unknown>) => send<AuthResponse>("/api/auth/register", "POST", payload),
  login: (payload: Record<string, unknown>) => send<AuthResponse>("/api/auth/login", "POST", payload),
  logout: (token: string) => send<void>("/api/auth/logout", "POST", {}, token),
  googleLoginUrl: () => `${API_BASE_URL}/oauth2/authorization/google`,
  me: (token: string) => request<UserProfile>("/api/auth/me", undefined, token),
  myEntitlements: (token: string) => request<Entitlement[]>("/api/auth/me/entitlements", undefined, token),
  myPayments: (token: string) => request<PaymentTransaction[]>("/api/auth/me/payments", undefined, token),
  catalogItems: () => request<CatalogItem[]>("/api/catalog/items?activeOnly=true"),
  products: () => request<DataProduct[]>("/api/catalog/products?activeOnly=true"),
  marketData: () => request<MarketData[]>("/api/market-data"),
  marketDataRuntime: () => request<MarketDataRuntimeStatus>("/api/market-data/runtime"),
  checkout: (payload: Record<string, unknown>, token: string) =>
    send<PaymentTransaction>("/api/payments/checkout", "POST", payload, token),
  paymentStatus: (paymentId: number, token: string) =>
    request<PaymentTransaction>(`/api/payments/${paymentId}`, undefined, token),
  usage: (payload: Record<string, unknown>) => send<UsageSummary>("/api/access/usage", "POST", payload),
  adminDashboard: (token: string) => request<AdminDashboard>("/api/admin/dashboard", undefined, token),
  createCatalogItem: (payload: Record<string, unknown>, token: string) =>
    send<CatalogItem>("/api/catalog/items", "POST", payload, token),
  createProduct: (payload: Record<string, unknown>, token: string) =>
    send<DataProduct>("/api/catalog/products", "POST", payload, token),
  createMarketData: (payload: Record<string, unknown>, token: string) =>
    send<MarketData>("/api/market-data", "POST", payload, token)
};

export async function buildSession(auth: AuthResponse): Promise<SessionState> {
  if (!auth.accessToken || !auth.apiKey) {
    throw new Error(auth.message || "Authentication response did not include a usable session.");
  }

  const profile = await api.me(auth.accessToken);
  return {
    accessToken: auth.accessToken,
    apiKey: auth.apiKey,
    userId: auth.userId,
    email: auth.email,
    profile
  };
}
