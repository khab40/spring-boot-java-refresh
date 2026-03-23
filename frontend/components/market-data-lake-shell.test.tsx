import { fireEvent, render, screen, waitFor, within } from "@testing-library/react";
import { beforeEach, describe, expect, it, vi } from "vitest";
const {
  apiMock,
  buildSessionMock,
  loadSessionMock,
  saveSessionMock,
  loadPendingCheckoutIdMock,
  savePendingCheckoutIdMock
} = vi.hoisted(() => ({
  apiMock: {
    register: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
    googleLoginUrl: vi.fn(() => "http://localhost:8080/oauth2/authorization/google"),
    me: vi.fn(),
    updateMe: vi.fn(),
    users: vi.fn(),
    updateUser: vi.fn(),
    updateUserRole: vi.fn(),
    createUserAdmin: vi.fn(),
    userEntitlementsAdmin: vi.fn(),
    userPaymentsAdmin: vi.fn(),
    myEntitlements: vi.fn(),
    myPayments: vi.fn(),
    myOtdDeliveries: vi.fn(),
    catalogItems: vi.fn(),
    products: vi.fn(),
    marketData: vi.fn(),
    marketDataRuntime: vi.fn(),
    checkout: vi.fn(),
    paymentStatus: vi.fn(),
    usage: vi.fn(),
    createOtdDelivery: vi.fn(),
    adminDashboard: vi.fn(),
    createCatalogItem: vi.fn(),
    createProduct: vi.fn(),
    updateProduct: vi.fn(),
    createMarketData: vi.fn()
  },
  buildSessionMock: vi.fn(),
  loadSessionMock: vi.fn(),
  saveSessionMock: vi.fn(),
  loadPendingCheckoutIdMock: vi.fn(),
  savePendingCheckoutIdMock: vi.fn()
}));

vi.mock("../lib/api", () => ({
  api: apiMock,
  buildSession: buildSessionMock
}));

vi.mock("../lib/storage", () => ({
  loadSession: loadSessionMock,
  saveSession: saveSessionMock,
  loadPendingCheckoutId: loadPendingCheckoutIdMock,
  savePendingCheckoutId: savePendingCheckoutIdMock
}));

import { MarketDataLakeShell } from "./market-data-lake-shell";

describe("MarketDataLakeShell auth session handling", () => {
  beforeEach(() => {
    vi.clearAllMocks();

    loadPendingCheckoutIdMock.mockReturnValue(null);
    apiMock.catalogItems.mockResolvedValue([]);
    apiMock.marketData.mockResolvedValue([]);
    apiMock.marketDataRuntime.mockResolvedValue({
      mode: "stub",
      stubbed: true,
      message: "Stub runtime"
    });
    apiMock.myEntitlements.mockResolvedValue([]);
    apiMock.myPayments.mockResolvedValue([]);
    apiMock.myOtdDeliveries.mockResolvedValue([]);
    apiMock.logout.mockResolvedValue(undefined);
  });

  it("clears the previous user session when a new sign-in attempt fails", async () => {
    loadSessionMock.mockReturnValue({
      accessToken: "old-token",
      apiKey: "old-key",
      userId: 52,
      email: "alexey.khabalov@gmail.com",
      profile: {
        id: 52,
        email: "alexey.khabalov@gmail.com",
        firstName: "Alexey",
        lastName: "Khabalov",
        role: "USER",
        authProvider: "GOOGLE",
        emailVerified: true
      }
    });
    apiMock.me.mockResolvedValue({
      id: 52,
      email: "alexey.khabalov@gmail.com",
      firstName: "Alexey",
      lastName: "Khabalov",
      role: "USER",
      authProvider: "GOOGLE",
      emailVerified: true
    });
    apiMock.login.mockRejectedValue(new Error("Invalid email or password."));

    const { container } = render(<MarketDataLakeShell />);

    await waitFor(() => {
      expect(screen.getAllByRole("button", { name: /user/i }).length).toBeGreaterThan(0);
    });

    fireEvent.click(screen.getAllByRole("button", { name: /user/i })[0]);
    const drawer = screen.getByRole("heading", { name: /sign in or create account/i }).closest("aside");
    expect(drawer).not.toBeNull();
    const drawerQueries = within(drawer!);

    await waitFor(() => {
      expect(drawerQueries.getAllByRole("button", { name: /^sign in$/i }).length).toBeGreaterThan(0);
    });

    saveSessionMock.mockClear();

    const emailInput = container.querySelector('input[type="text"]') as HTMLInputElement | null;
    const passwordInput = container.querySelector('input[type="password"]') as HTMLInputElement | null;

    expect(emailInput).not.toBeNull();
    expect(passwordInput).not.toBeNull();

    fireEvent.change(emailInput!, { target: { value: "test@gmail.com" } });
    fireEvent.change(passwordInput!, { target: { value: "wrong-password" } });
    fireEvent.click(drawerQueries.getAllByRole("button", { name: /^sign in$/i }).at(-1)!);

    await waitFor(() => {
      expect(apiMock.login).toHaveBeenCalledWith({
        email: "test@gmail.com",
        password: "wrong-password"
      });
    });

    await waitFor(() => {
      expect(saveSessionMock).toHaveBeenCalledWith(null);
    });

    expect(screen.queryByText(/alexey khabalov/i)).not.toBeInTheDocument();
    expect(screen.getByText("Invalid email or password.")).toBeInTheDocument();
  });

  it("clears a stale stored session when profile hydration fails", async () => {
    loadSessionMock.mockReturnValue({
      accessToken: "stale-token",
      apiKey: "stale-key",
      userId: 52,
      email: "alexey.khabalov@gmail.com",
      profile: null
    });
    apiMock.me.mockRejectedValue(new Error("Session restore timed out."));

    render(<MarketDataLakeShell />);

    await waitFor(() => {
      expect(saveSessionMock).toHaveBeenCalledWith(null);
    });

    expect(screen.queryByText(/Authenticated session detected/i)).not.toBeInTheDocument();
    fireEvent.click(screen.getAllByRole("button", { name: /user/i })[0]);
    const drawer = screen.getByRole("heading", { name: /sign in or create account/i }).closest("aside");
    expect(drawer).not.toBeNull();
    expect(within(drawer!).getAllByRole("button", { name: /^sign in$/i }).length).toBeGreaterThan(0);
  });
});
