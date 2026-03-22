"use client";

import { useEffect, useMemo, useRef, useState } from "react";
import { api, buildSession } from "../lib/api";
import { describeProductMode, describeStorage, formatDate, formatMoney } from "../lib/format";
import { loadPendingCheckoutId, loadSession, savePendingCheckoutId, saveSession } from "../lib/storage";
import {
  AdminDashboard,
  CatalogItem,
  CatalogFilters,
  DataProduct,
  Entitlement,
  MarketData,
  MarketDataRuntimeStatus,
  OtdDelivery,
  PaymentTransaction,
  SessionState,
  UserProfile
} from "../lib/types";
import { ProductCard } from "./product-card";

type AuthMode = "signin" | "signup";

type AuthForm = {
  email: string;
  password: string;
  firstName: string;
  lastName: string;
  company: string;
  country: string;
  phoneNumber: string;
};

type AdminCatalogItemForm = {
  code: string;
  name: string;
  summary: string;
  description: string;
  marketDataType: CatalogItem["marketDataType"];
  storageSystem: CatalogItem["storageSystem"];
  deliveryApiPath: string;
  lakeQueryReference: string;
  sampleSymbols: string;
  coverageStartDate: string;
  coverageEndDate: string;
};

type AdminProductForm = {
  catalogItemId: string;
  code: string;
  name: string;
  description: string;
  price: string;
  minimumPrice: string;
  includedSymbols: string;
  includedDays: string;
  pricePerAdditionalSymbol: string;
  pricePerAdditionalDay: string;
  fullUniverseSymbolCount: string;
  currency: string;
  accessType: "ONE_TIME_PURCHASE" | "SUBSCRIPTION";
  billingInterval: "ONE_TIME" | "MONTHLY" | "YEARLY";
  batchDownloadLimitMb: string;
  realtimeSubscriptionLimit: string;
  maxRealtimePayloadKb: string;
};

type CatalogSelectionPayload = {
  symbol: string | null;
  availableFrom: string | null;
  availableTo: string | null;
};

type MarketDataForm = {
  symbol: string;
  price: string;
  volume: string;
  timestamp: string;
  dataType: MarketData["dataType"];
};

type CartEntry = {
  catalogItemId: number;
  catalogItemName: string;
  product: DataProduct;
  quantity: number;
  quotedUnitPrice: number;
  selection: CatalogSelectionPayload;
  selectionSummary: string;
};

type ProfileForm = {
  firstName: string;
  lastName: string;
  company: string;
  country: string;
  phoneNumber: string;
};

type AdminUserForm = {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  company: string;
  country: string;
  phoneNumber: string;
  role: UserProfile["role"];
  emailVerified: boolean;
};

type AdminCreateUserForm = {
  email: string;
  firstName: string;
  lastName: string;
  password: string;
  company: string;
  country: string;
  phoneNumber: string;
};

type OtdQueryForm = {
  productId: string;
  sql: string;
};

const defaultCatalogFilters: CatalogFilters = {
  symbol: "*",
  availableFrom: "",
  availableTo: "",
  marketDataType: "",
  storageSystem: "",
  accessType: "",
  billingInterval: ""
};

type AccessSummary = {
  key: number;
  product: DataProduct;
  purchasedUnits: number;
  batchDownloadUsedMb: number;
  realtimeSubscriptionsUsed: number;
  payloadKilobytesUsed: number;
  grantedAt: string;
  expiresAt?: string | null;
  entitlementCount: number;
};

const defaultAuthForm: AuthForm = {
  email: "",
  password: "",
  firstName: "",
  lastName: "",
  company: "",
  country: "",
  phoneNumber: ""
};

const defaultCatalogItemForm: AdminCatalogItemForm = {
  code: "",
  name: "",
  summary: "",
  description: "",
  marketDataType: "QUOTE",
  storageSystem: "DELTA_LAKE",
  deliveryApiPath: "/api/market-data/query",
  lakeQueryReference: "lake.market_quotes",
  sampleSymbols: "AAPL,MSFT,NVDA",
  coverageStartDate: "2026-01-01",
  coverageEndDate: ""
};

const defaultProductForm: AdminProductForm = {
  catalogItemId: "",
  code: "",
  name: "",
  description: "",
  price: "49.99",
  minimumPrice: "49.99",
  includedSymbols: "1",
  includedDays: "1",
  pricePerAdditionalSymbol: "2.50",
  pricePerAdditionalDay: "0.50",
  fullUniverseSymbolCount: "250",
  currency: "usd",
  accessType: "ONE_TIME_PURCHASE",
  billingInterval: "ONE_TIME",
  batchDownloadLimitMb: "500",
  realtimeSubscriptionLimit: "1",
  maxRealtimePayloadKb: "1024"
};

const defaultMarketDataForm: MarketDataForm = {
  symbol: "AAPL",
  price: "189.42",
  volume: "1000",
  timestamp: new Date().toISOString().slice(0, 16),
  dataType: "QUOTE"
};

const defaultProfileForm: ProfileForm = {
  firstName: "",
  lastName: "",
  company: "",
  country: "",
  phoneNumber: ""
};

const defaultOtdQueryForm: OtdQueryForm = {
  productId: "",
  sql: "SELECT * FROM market_data ORDER BY timestamp DESC LIMIT 100"
};

const defaultAdminUserForm: AdminUserForm = {
  id: "",
  email: "",
  firstName: "",
  lastName: "",
  company: "",
  country: "",
  phoneNumber: "",
  role: "USER",
  emailVerified: false
};

const defaultCreateUserForm: AdminCreateUserForm = {
  email: "",
  firstName: "",
  lastName: "",
  password: "",
  company: "",
  country: "",
  phoneNumber: ""
};

export function MarketDataLakeShell() {
  const [authMode, setAuthMode] = useState<AuthMode>("signin");
  const [authForm, setAuthForm] = useState<AuthForm>(defaultAuthForm);
  const [session, setSession] = useState<SessionState | null>(null);
  const [catalogItems, setCatalogItems] = useState<CatalogItem[]>([]);
  const [catalogFilters, setCatalogFilters] = useState<CatalogFilters>(defaultCatalogFilters);
  const [selectedCatalogItemId, setSelectedCatalogItemId] = useState<number | null>(null);
  const [marketData, setMarketData] = useState<MarketData[]>([]);
  const [marketDataRuntime, setMarketDataRuntime] = useState<MarketDataRuntimeStatus | null>(null);
  const [entitlements, setEntitlements] = useState<Entitlement[]>([]);
  const [payments, setPayments] = useState<PaymentTransaction[]>([]);
  const [otdDeliveries, setOtdDeliveries] = useState<OtdDelivery[]>([]);
  const [dashboard, setDashboard] = useState<AdminDashboard | null>(null);
  const [adminUsers, setAdminUsers] = useState<UserProfile[]>([]);
  const [selectedAdminUserId, setSelectedAdminUserId] = useState<number | null>(null);
  const [selectedAdminUserPayments, setSelectedAdminUserPayments] = useState<PaymentTransaction[]>([]);
  const [selectedAdminUserEntitlements, setSelectedAdminUserEntitlements] = useState<Entitlement[]>([]);
  const [quantities, setQuantities] = useState<Record<number, number>>({});
  const [cart, setCart] = useState<Record<number, CartEntry>>({});
  const [selectedAccessProductId, setSelectedAccessProductId] = useState<number | null>(null);
  const [selectedPaymentId, setSelectedPaymentId] = useState<number | null>(null);
  const [profileForm, setProfileForm] = useState<ProfileForm>(defaultProfileForm);
  const [isUserDrawerOpen, setIsUserDrawerOpen] = useState(false);
  const [checkoutStatus, setCheckoutStatus] = useState("");
  const [lastTransaction, setLastTransaction] = useState<PaymentTransaction | null>(null);
  const [message, setMessage] = useState("Loading the Market Data Lake catalog.");
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [catalogItemForm, setCatalogItemForm] = useState<AdminCatalogItemForm>(defaultCatalogItemForm);
  const [productForm, setProductForm] = useState<AdminProductForm>(defaultProductForm);
  const [editingProductId, setEditingProductId] = useState("0");
  const [adminUserForm, setAdminUserForm] = useState<AdminUserForm>(defaultAdminUserForm);
  const [createUserForm, setCreateUserForm] = useState<AdminCreateUserForm>(defaultCreateUserForm);
  const [marketDataForm, setMarketDataForm] = useState<MarketDataForm>(defaultMarketDataForm);
  const [otdQueryForm, setOtdQueryForm] = useState<OtdQueryForm>(defaultOtdQueryForm);
  const privateRequestVersion = useRef(0);
  const passwordTooShort = authForm.password.length > 0 && authForm.password.length < 8;
  const profile: UserProfile | null | undefined = session?.profile;

  useEffect(() => {
    const stored = loadSession();
    if (stored) {
      setSession(stored);
    }

    if (typeof window !== "undefined") {
      const params = new URLSearchParams(window.location.search);
      const authError = params.get("authError");
      const authSuccess = params.get("authSuccess");
      const messageParam = params.get("message");
      const checkoutState = params.get("checkout");

      if (authError) {
        setError(messageParam || "Google sign-in failed.");
      } else if (authSuccess === "google") {
        setMessage(messageParam || "Signed in with Google successfully.");
      } else if (checkoutState === "success") {
        setMessage("Stripe checkout completed. Refreshing purchase state...");
      } else if (checkoutState === "cancelled") {
        setMessage("Stripe checkout was cancelled.");
      }
    }
  }, []);

  useEffect(() => {
    void refreshPublicData(defaultCatalogFilters, { retries: 3, retryDelayMs: 1200 });
  }, []);

  useEffect(() => {
    const requestVersion = ++privateRequestVersion.current;

    if (!session?.accessToken) {
      setEntitlements([]);
      setPayments([]);
      setOtdDeliveries([]);
      setDashboard(null);
      setAdminUsers([]);
      setSelectedAdminUserPayments([]);
      setSelectedAdminUserEntitlements([]);
      saveSession(null);
      return;
    }

    saveSession(session);
    void refreshPrivateData(session, requestVersion);
  }, [session]);

  useEffect(() => {
    if (typeof window === "undefined" || !session?.accessToken) {
      return;
    }

    const params = new URLSearchParams(window.location.search);
    const checkoutState = params.get("checkout");
    const pendingCheckoutId = loadPendingCheckoutId();
    if ((checkoutState !== "success" && checkoutState !== "cancelled") || !pendingCheckoutId) {
      return;
    }

    void (async () => {
      try {
        const transaction = await pollTransactionForCompletion(pendingCheckoutId, session.accessToken);
        setLastTransaction(transaction);
        setCheckoutStatus(`Payment status: ${transaction.status}`);
        if (transaction.status === "SUCCEEDED") {
          setEntitlements(await api.myEntitlements(session.accessToken));
          setPayments(await api.myPayments(session.accessToken));
          setCart({});
          savePendingCheckoutId(null);
          setMessage("Payment completed and entitlements refreshed.");
        } else if (transaction.status === "FAILED") {
          savePendingCheckoutId(null);
          setError(transaction.errorMessage || "Stripe checkout did not complete successfully.");
        } else {
          setMessage("Stripe checkout was created. Waiting for payment confirmation...");
        }
      } catch (requestError) {
        setError(getErrorMessage(requestError));
      } finally {
        const cleaned = new URL(window.location.href);
        cleaned.searchParams.delete("checkout");
        window.history.replaceState({}, "", cleaned.toString());
      }
    })();
  }, [session]);

  const selectedCatalogItem = useMemo(
    () => catalogItems.find((item) => item.id === selectedCatalogItemId) ?? catalogItems[0] ?? null,
    [catalogItems, selectedCatalogItemId]
  );
  const activeEntitlements = useMemo(
    () => entitlements.filter((entitlement) => entitlement.status === "ACTIVE"),
    [entitlements]
  );
  const accessSummaries = useMemo(() => aggregateEntitlements(activeEntitlements), [activeEntitlements]);
  const selectedAccess = useMemo(
    () => accessSummaries.find((entry) => entry.key === selectedAccessProductId) ?? accessSummaries[0] ?? null,
    [accessSummaries, selectedAccessProductId]
  );
  const selectedPayment = useMemo(
    () => payments.find((payment) => payment.id === selectedPaymentId) ?? payments[0] ?? null,
    [payments, selectedPaymentId]
  );
  const selectedAdminUser = useMemo(
    () => adminUsers.find((user) => user.id === selectedAdminUserId) ?? adminUsers[0] ?? null,
    [adminUsers, selectedAdminUserId]
  );
  const otdEligibleOffers = useMemo(
    () => selectedCatalogItem?.offers.filter((offer) => offer.accessType === "ONE_TIME_PURCHASE") ?? [],
    [selectedCatalogItem]
  );
  const selectedCatalogDeliveries = useMemo(
    () =>
      otdDeliveries.filter((delivery) =>
        selectedCatalogItem?.offers.some((offer) => offer.id === delivery.productId)
      ),
    [otdDeliveries, selectedCatalogItem]
  );

  const cartEntries = useMemo(() => Object.values(cart), [cart]);
  const cartTotal = useMemo(
    () => cartEntries.reduce((sum, entry) => sum + entry.quotedUnitPrice * entry.quantity, 0),
    [cartEntries]
  );
  const editableProducts = useMemo(
    () =>
      catalogItems.flatMap((item) =>
        item.offers.map((offer) => ({
          ...offer,
          catalogItemLabel: item.name
        }))
      ),
    [catalogItems]
  );

  useEffect(() => {
    if (!selectedAdminUser) {
      setAdminUserForm(defaultAdminUserForm);
      return;
    }

    setAdminUserForm(toAdminUserForm(selectedAdminUser));
  }, [selectedAdminUser]);

  useEffect(() => {
    if (!session?.accessToken || session.profile?.role !== "ADMIN" || !selectedAdminUserId) {
      setSelectedAdminUserPayments([]);
      setSelectedAdminUserEntitlements([]);
      return;
    }

    let cancelled = false;
    void (async () => {
      try {
        const [userPayments, userEntitlements] = await Promise.all([
          api.userPaymentsAdmin(selectedAdminUserId, session.accessToken),
          api.userEntitlementsAdmin(selectedAdminUserId, session.accessToken)
        ]);
        if (cancelled) {
          return;
        }
        setSelectedAdminUserPayments(userPayments);
        setSelectedAdminUserEntitlements(userEntitlements);
      } catch (requestError) {
        if (!cancelled) {
          setError(getErrorMessage(requestError));
        }
      }
    })();

    return () => {
      cancelled = true;
    };
  }, [session?.accessToken, session?.profile?.role, selectedAdminUserId]);

  useEffect(() => {
    if (!selectedCatalogItemId && catalogItems.length > 0) {
      setSelectedCatalogItemId(catalogItems[0].id);
    }
    if (!productForm.catalogItemId && catalogItems.length > 0) {
      setProductForm((current) => ({ ...current, catalogItemId: String(catalogItems[0].id) }));
    }
  }, [catalogItems, selectedCatalogItemId, productForm.catalogItemId]);

  useEffect(() => {
    if (!selectedCatalogItem) {
      return;
    }

    const preferredOffer = selectedCatalogItem.offers.find((offer) => offer.accessType === "ONE_TIME_PURCHASE") ?? selectedCatalogItem.offers[0];
    const defaultSymbol = selectedCatalogItem.sampleSymbols?.split(",")[0]?.trim() || catalogFilters.symbol.trim() || "AAPL";
    setOtdQueryForm({
      productId: preferredOffer ? String(preferredOffer.id) : "",
      sql: `SELECT * FROM market_data WHERE symbol = '${defaultSymbol === "*" ? "AAPL" : defaultSymbol}' ORDER BY timestamp DESC LIMIT 100`
    });
  }, [selectedCatalogItem]);

  useEffect(() => {
    if (!profile) {
      setProfileForm(defaultProfileForm);
      return;
    }

    setProfileForm({
      firstName: profile.firstName || "",
      lastName: profile.lastName || "",
      company: profile.company || "",
      country: profile.country || "",
      phoneNumber: profile.phoneNumber || ""
    });
  }, [profile]);

  useEffect(() => {
    if (!selectedAccessProductId && accessSummaries.length > 0) {
      setSelectedAccessProductId(accessSummaries[0].key);
    }
  }, [accessSummaries, selectedAccessProductId]);

  useEffect(() => {
    if (!selectedPaymentId && payments.length > 0) {
      setSelectedPaymentId(payments[0].id);
    }
  }, [payments, selectedPaymentId]);

  async function refreshPublicData(
    filters: CatalogFilters = catalogFilters,
    options: { retries?: number; retryDelayMs?: number } = {}
  ) {
    const retries = options.retries ?? 0;
    const retryDelayMs = options.retryDelayMs ?? 0;

    try {
      const items = await withRetries(() => api.catalogItems(filters), retries, retryDelayMs);
      const [feedResult, runtimeResult] = await Promise.allSettled([api.marketData(), api.marketDataRuntime()]);

      setCatalogItems(items);
      if (feedResult.status === "fulfilled") {
        setMarketData(feedResult.value);
      }
      if (runtimeResult.status === "fulfilled") {
        setMarketDataRuntime(runtimeResult.value);
      }

      const supplementalFailures = [feedResult, runtimeResult].filter((result) => result.status === "rejected").length;
      setError("");
      setMessage(
        items.length > 0
          ? supplementalFailures > 0
            ? `Catalog metadata is live. ${items.length} dataset${items.length === 1 ? "" : "s"} matched the current search. Supplemental runtime panels are still reconnecting.`
            : `Catalog metadata is live. ${items.length} dataset${items.length === 1 ? "" : "s"} matched the current search.`
          : "No catalog items matched the current search."
      );
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  }

  async function handleCatalogSearch() {
    setBusy(true);
    setError("");
    try {
      await refreshPublicData(catalogFilters);
    } finally {
      setBusy(false);
    }
  }

  async function handleCatalogFilterReset() {
    setCatalogFilters(defaultCatalogFilters);
    setBusy(true);
    setError("");
    try {
      await refreshPublicData(defaultCatalogFilters);
    } finally {
      setBusy(false);
    }
  }

  async function refreshPrivateData(nextSession: SessionState, requestVersion: number) {
    try {
      const profile = await api.me(nextSession.accessToken);
      if (privateRequestVersion.current !== requestVersion) {
        return;
      }

      const updatedSession = { ...nextSession, profile };
      setSession(updatedSession);

      const userEntitlements = await api.myEntitlements(nextSession.accessToken);
      if (privateRequestVersion.current !== requestVersion) {
        return;
      }
      setEntitlements(userEntitlements);

      const userPayments = await api.myPayments(nextSession.accessToken);
      if (privateRequestVersion.current !== requestVersion) {
        return;
      }
      setPayments(userPayments);

      const deliveries = await api.myOtdDeliveries(nextSession.accessToken);
      if (privateRequestVersion.current !== requestVersion) {
        return;
      }
      setOtdDeliveries(deliveries);

      if (profile.role === "ADMIN") {
        const adminDashboard = await api.adminDashboard(nextSession.accessToken);
        if (privateRequestVersion.current !== requestVersion) {
          return;
        }
        setDashboard(adminDashboard);

        const users = await api.users(nextSession.accessToken);
        if (privateRequestVersion.current !== requestVersion) {
          return;
        }
        setAdminUsers(users);
        if (users.length > 0 && !users.some((user) => user.id === selectedAdminUserId)) {
          setSelectedAdminUserId(users[0].id);
        }
      } else {
        setDashboard(null);
        setAdminUsers([]);
      }
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  }

  async function handleAuthSubmit() {
    if (passwordTooShort) {
      setError("Password must be at least 8 characters long.");
      setMessage("");
      return;
    }

    setBusy(true);
    setError("");
    setMessage("");

    try {
      const payload =
        authMode === "signup"
          ? authForm
          : {
              email: authForm.email,
              password: authForm.password
            };
      const response = authMode === "signup" ? await api.register(payload) : await api.login(payload);
      if (response.accessToken && response.apiKey) {
        const nextSession = await buildSession(response);
        setSession(nextSession);
      } else {
        setSession(null);
      }
      setMessage(
        response.message || (authMode === "signup" ? "Account created. Check your inbox to verify the user." : "Signed in successfully.")
      );
      setAuthForm(defaultAuthForm);
      if (authMode === "signup") {
        setAuthMode("signin");
      }
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  async function handleLogout() {
    privateRequestVersion.current += 1;

    if (session?.accessToken) {
      try {
        await api.logout(session.accessToken);
      } catch {
        // Client-side logout still clears the session.
      }
    }

    setSession(null);
    setEntitlements([]);
    setPayments([]);
    setDashboard(null);
    setCheckoutStatus("");
    setLastTransaction(null);
    setMessage("Signed out.");
  }

  async function handleProfileSave() {
    if (!session?.accessToken || !profile) {
      return;
    }

    setBusy(true);
    setError("");
    try {
      const updatedProfile = await api.updateMe(
        {
          firstName: profileForm.firstName.trim(),
          lastName: profileForm.lastName.trim(),
          company: profileForm.company.trim() || null,
          country: profileForm.country.trim() || null,
          phoneNumber: profileForm.phoneNumber.trim() || null
        },
        session.accessToken
      );
      setSession((current) => (current ? { ...current, profile: updatedProfile } : current));
      setMessage("User profile updated.");
      setIsUserDrawerOpen(false);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  function handleAddToCart(product: DataProduct) {
    const quantity = quantities[product.id] ?? 1;
    const owningCatalogItem = catalogItems.find((item) => item.id === product.catalogItemId);
    if (!owningCatalogItem) {
      setError("Unable to resolve the selected catalog item for this offer.");
      return;
    }

    const existingEntries = Object.values(cart);
    if (existingEntries.length > 0) {
      const firstProduct = existingEntries[0].product;
      const mixedAccessType = firstProduct.accessType !== product.accessType;
      const mixedSubscriptionCadence =
        firstProduct.accessType === "SUBSCRIPTION" &&
        product.accessType === "SUBSCRIPTION" &&
        firstProduct.billingInterval !== product.billingInterval;
      const mixedCurrency = firstProduct.currency.toLowerCase() !== product.currency.toLowerCase();
      if (mixedAccessType || mixedSubscriptionCadence || mixedCurrency) {
        setError(
          "This checkout supports one pricing mode per cart. Use either one-time offers or same-interval subscriptions in the same currency."
        );
        return;
      }
    }

    setCart((current) => ({
      ...current,
      [product.id]: {
        catalogItemId: owningCatalogItem.id,
        catalogItemName: owningCatalogItem.name,
        product,
        quantity,
        quotedUnitPrice: getQuotedUnitPrice(product),
        selection: buildCatalogSelectionPayload(catalogFilters),
        selectionSummary: product.quotedSelectionSummary || owningCatalogItem.selectionSummary || "Default catalog selection"
      }
    }));
    setQuantities((current) => ({
      ...current,
      [product.id]: quantity
    }));
    setMessage(`${product.name} was ${cart[product.id] ? "updated in" : "added to"} the cart.`);
    setError("");
  }

  function updateCartQuantity(productId: number, quantity: number) {
    const normalizedQuantity = Math.max(1, quantity);
    setQuantities((current) => ({
      ...current,
      [productId]: normalizedQuantity
    }));
    setCart((current) => {
      const entry = current[productId];
      if (!entry) {
        return current;
      }
      return {
        ...current,
        [productId]: {
          ...entry,
          quantity: normalizedQuantity
        }
      };
    });
  }

  function removeFromCart(productId: number) {
    const removedEntry = cart[productId];
    setCart((current) => {
      const next = { ...current };
      delete next[productId];
      return next;
    });
    setMessage(removedEntry ? `${removedEntry.product.name} was removed from the cart.` : "Offer removed from the cart.");
  }

  function clearCart() {
    setCart({});
    setMessage("Shopping cart cleared.");
  }

  async function handleCartCheckout() {
    if (!session?.accessToken) {
      setError("Sign in before checkout.");
      return;
    }
    if (cartEntries.length === 0) {
      setError("Add at least one catalog offer to the cart before checkout.");
      return;
    }

    setBusy(true);
    setError("");
    setCheckoutStatus("Creating Stripe checkout for the current cart...");

    try {
      const origin = typeof window === "undefined" ? "http://localhost:3000" : window.location.origin;
      const transaction = await api.checkout(
        {
          userId: session.userId,
          items: cartEntries.map((entry) => ({
            productId: entry.product.id,
            quantity: entry.quantity,
            selection: entry.selection
          })),
          successUrl: `${origin}/?checkout=success`,
          cancelUrl: `${origin}/?checkout=cancelled`
        },
        session.accessToken
      );

      setLastTransaction(transaction);
      savePendingCheckoutId(transaction.id);
      const resolvedTransaction = await pollTransaction(transaction.id, session.accessToken);
      setLastTransaction(resolvedTransaction);
      setCheckoutStatus(`Payment status: ${resolvedTransaction.status}`);

      if (resolvedTransaction.checkoutUrl && typeof window !== "undefined") {
        setMessage("Stripe Checkout is ready. Redirecting now...");
        window.location.href = resolvedTransaction.checkoutUrl;
      }
    } catch (requestError) {
      setError(getErrorMessage(requestError));
      setCheckoutStatus("");
      savePendingCheckoutId(null);
    } finally {
      setBusy(false);
    }
  }

  async function pollTransaction(transactionId: number, token: string) {
    for (let attempt = 0; attempt < 10; attempt += 1) {
      const transaction = await api.paymentStatus(transactionId, token);
      if (transaction.checkoutUrl || transaction.status === "FAILED") {
        return transaction;
      }
      await new Promise((resolve) => window.setTimeout(resolve, 1500));
    }

    return api.paymentStatus(transactionId, token);
  }

  async function pollTransactionForCompletion(transactionId: number, token: string) {
    for (let attempt = 0; attempt < 12; attempt += 1) {
      const transaction = await api.paymentStatus(transactionId, token);
      if (transaction.status === "SUCCEEDED" || transaction.status === "FAILED") {
        return transaction;
      }
      await new Promise((resolve) => window.setTimeout(resolve, 1500));
    }

    return api.paymentStatus(transactionId, token);
  }

  async function handleUsage(product: DataProduct) {
    if (product.accessType === "ONE_TIME_PURCHASE") {
      await handleOtdDelivery(product.id);
      return;
    }

    if (!session?.apiKey) {
      setError("You need an API key before recording usage.");
      return;
    }

    setBusy(true);
    setError("");

    try {
      const isSubscription = product.accessType === "SUBSCRIPTION";
      await api.usage({
        apiKey: session.apiKey,
        productId: product.id,
        usageType: isSubscription ? "REALTIME_SUBSCRIPTION" : "BATCH_DOWNLOAD",
        megabytesUsed: isSubscription ? 0 : quantities[product.id] ?? 1,
        realtimeSubscriptionsUsed: isSubscription ? 1 : 0,
        payloadKilobytesUsed: isSubscription ? 128 : 0,
        requestCount: quantities[product.id] ?? 1,
        notes: `Recorded from Market Data Lake UI for ${product.code}`
      });
      setMessage(`Delivery usage recorded for ${product.code}.`);
      if (session.accessToken) {
        setEntitlements(await api.myEntitlements(session.accessToken));
      }
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  async function handleOtdDelivery(productIdOverride?: number) {
    if (!session?.accessToken) {
      setError("Sign in before running an OTD delivery.");
      return;
    }
    const resolvedProductId = productIdOverride ?? Number(otdQueryForm.productId);
    if (!resolvedProductId) {
      setError("Select a one-time offer before running the OTD delivery.");
      return;
    }

    setBusy(true);
    setError("");
    try {
      const delivery = await api.createOtdDelivery(
        {
          productId: resolvedProductId,
          sql: otdQueryForm.sql
        },
        session.accessToken
      );
      setOtdDeliveries((current) => [delivery, ...current]);
      setEntitlements(await api.myEntitlements(session.accessToken));
      setMessage(`OTD delivery created for ${delivery.productCode}. Signed download link is ready.`);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  async function handleCreateCatalogItem() {
    if (!session?.accessToken) {
      return;
    }

    setBusy(true);
    setError("");
    try {
      const createdItem = await api.createCatalogItem(
        {
          ...catalogItemForm,
          coverageStartDate: catalogItemForm.coverageStartDate || null,
          coverageEndDate: catalogItemForm.coverageEndDate || null
        },
        session.accessToken
      );
      setCatalogItemForm(defaultCatalogItemForm);
      await refreshPublicData();
      setSelectedCatalogItemId(createdItem.id);
      setProductForm((current) => ({ ...current, catalogItemId: String(createdItem.id) }));
      setMessage("Catalog item created.");
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  async function handleSaveProduct() {
    if (!session?.accessToken) {
      return;
    }

    setBusy(true);
    setError("");
    try {
      const payload = {
        ...productForm,
        catalogItemId: Number(productForm.catalogItemId),
        price: Number(productForm.price),
        minimumPrice: Number(productForm.minimumPrice),
        includedSymbols: Number(productForm.includedSymbols),
        includedDays: Number(productForm.includedDays),
        pricePerAdditionalSymbol: Number(productForm.pricePerAdditionalSymbol),
        pricePerAdditionalDay: Number(productForm.pricePerAdditionalDay),
        fullUniverseSymbolCount: Number(productForm.fullUniverseSymbolCount),
        batchDownloadLimitMb: toOptionalNumber(productForm.batchDownloadLimitMb),
        realtimeSubscriptionLimit: toOptionalNumber(productForm.realtimeSubscriptionLimit),
        maxRealtimePayloadKb: toOptionalNumber(productForm.maxRealtimePayloadKb)
      };
      if (editingProductId !== "0") {
        await api.updateProduct(Number(editingProductId), payload, session.accessToken);
      } else {
        await api.createProduct(payload, session.accessToken);
      }
      setProductForm((current) => ({
        ...defaultProductForm,
        catalogItemId: current.catalogItemId
      }));
      setEditingProductId("0");
      await refreshPublicData();
      if (session.profile?.role === "ADMIN") {
        setDashboard(await api.adminDashboard(session.accessToken));
      }
      setMessage(editingProductId !== "0" ? "Sellable offer repriced." : "Sellable offer created.");
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  async function handleCreateMarketData() {
    if (!session?.accessToken) {
      return;
    }

    setBusy(true);
    setError("");
    try {
      await api.createMarketData(
        {
          ...marketDataForm,
          price: Number(marketDataForm.price),
          volume: Number(marketDataForm.volume),
          timestamp: new Date(marketDataForm.timestamp).toISOString()
        },
        session.accessToken
      );
      setMarketDataForm(defaultMarketDataForm);
      await refreshPublicData();
      setMessage("Preview market-data row published into the stub runtime.");
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  async function handleAdminUserSave() {
    if (!session?.accessToken || !adminUserForm.id) {
      return;
    }

    setBusy(true);
    setError("");
    try {
      const updatedUser = await api.updateUser(
        Number(adminUserForm.id),
        {
          email: adminUserForm.email.trim(),
          firstName: adminUserForm.firstName.trim(),
          lastName: adminUserForm.lastName.trim(),
          company: adminUserForm.company.trim() || null,
          country: adminUserForm.country.trim() || null,
          phoneNumber: adminUserForm.phoneNumber.trim() || null,
          role: adminUserForm.role,
          emailVerified: adminUserForm.emailVerified
        },
        session.accessToken
      );
      setAdminUsers((current) => current.map((user) => (user.id === updatedUser.id ? updatedUser : user)));
      setMessage(`User ${updatedUser.email} updated.`);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  async function handleAdminRoleQuickChange(role: UserProfile["role"]) {
    if (!session?.accessToken || !selectedAdminUser) {
      return;
    }

    setBusy(true);
    setError("");
    try {
      const updatedUser = await api.updateUserRole(selectedAdminUser.id, role, session.accessToken);
      setAdminUsers((current) => current.map((user) => (user.id === updatedUser.id ? updatedUser : user)));
      setSelectedAdminUserId(updatedUser.id);
      setMessage(`Role updated for ${updatedUser.email}.`);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  async function handleAdminCreateUser() {
    if (!session?.accessToken) {
      return;
    }

    setBusy(true);
    setError("");
    try {
      const createdUser = await api.createUserAdmin(
        {
          email: createUserForm.email.trim(),
          firstName: createUserForm.firstName.trim(),
          lastName: createUserForm.lastName.trim(),
          password: createUserForm.password,
          company: createUserForm.company.trim() || null,
          country: createUserForm.country.trim() || null,
          phoneNumber: createUserForm.phoneNumber.trim() || null
        },
        session.accessToken
      );
      setAdminUsers((current) => [createdUser, ...current]);
      setSelectedAdminUserId(createdUser.id);
      setCreateUserForm(defaultCreateUserForm);
      if (session.profile?.role === "ADMIN") {
        setDashboard(await api.adminDashboard(session.accessToken));
      }
      setMessage(`User ${createdUser.email} created.`);
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  const groupedMarketData = groupMarketData(marketData);

  return (
    <main className="page-shell">
      <section className="terminal-header">
        <div className="terminal-header-main">
          <div className="terminal-kicker">Market Data Lake / Operator Console</div>
          <h1>Data Catalog</h1>
          <p>
            Query available datasets, inspect coverage and delivery references, then move directly into commercial
            packaging and checkout.
          </p>
        </div>
        <div className="terminal-header-side">
          <button className="user-chip" onClick={() => setIsUserDrawerOpen(true)}>
            <span className="user-chip-label">User</span>
            <strong>{profile ? `${profile.firstName} ${profile.lastName}` : "Sign in"}</strong>
          </button>
          <div className="terminal-user-meta">
            {profile ? `${profile.email} / ${profile.authProvider}` : "Authentication required for access and payments"}
          </div>
          {session?.apiKey ? (
            <div className="token-block compact-token">
              API key
              <code>{session.apiKey}</code>
            </div>
          ) : null}
        </div>
      </section>

      <section className="console-stats">
        <div className="console-stat">
          <span>datasets</span>
          <strong>{catalogItems.length}</strong>
        </div>
        <div className="console-stat">
          <span>cart</span>
          <strong>{cartEntries.length}</strong>
        </div>
        <div className="console-stat">
          <span>access</span>
          <strong>{accessSummaries.length}</strong>
        </div>
        <div className="console-stat">
          <span>payments</span>
          <strong>{payments.length}</strong>
        </div>
      </section>

      {error ? <div className="status error">{error}</div> : null}
      {message ? <div className="status info">{message}</div> : null}
      {checkoutStatus ? <div className="status warn">{checkoutStatus}</div> : null}

      {isUserDrawerOpen ? (
        <div className="drawer-backdrop" onClick={() => setIsUserDrawerOpen(false)}>
          <aside className="user-drawer" onClick={(event) => event.stopPropagation()}>
            <div className="section-header">
              <div>
                <div className="eyebrow">User</div>
                <h2>{profile ? "Identity and session" : "Sign in or create account"}</h2>
                <div className="panel-intro">
                  {profile
                    ? "Update user details, inspect sign-in state, or sign out."
                    : "Keep auth, verification, and Google sign-in out of the main storefront flow."}
                </div>
              </div>
              <button className="ghost-button" onClick={() => setIsUserDrawerOpen(false)}>
                Close
              </button>
            </div>
            {!profile ? (
              <div className="form-grid">
                <div className="pill-row">
                  <button className={authMode === "signin" ? "button" : "ghost-button"} onClick={() => setAuthMode("signin")}>
                    Sign in
                  </button>
                  <button className={authMode === "signup" ? "button" : "ghost-button"} onClick={() => setAuthMode("signup")}>
                    Sign up
                  </button>
                </div>
                <Field label="Email" value={authForm.email} onChange={(value) => setAuthForm({ ...authForm, email: value })} />
                {authMode === "signup" ? (
                  <div className="form-row">
                    <Field label="First name" value={authForm.firstName} onChange={(value) => setAuthForm({ ...authForm, firstName: value })} />
                    <Field label="Last name" value={authForm.lastName} onChange={(value) => setAuthForm({ ...authForm, lastName: value })} />
                  </div>
                ) : null}
                <Field label="Password" value={authForm.password} onChange={(value) => setAuthForm({ ...authForm, password: value })} type="password" />
                {passwordTooShort ? (
                  <div className="helper" role="alert">
                    Password must be at least 8 characters long.
                  </div>
                ) : null}
                {authMode === "signup" ? (
                  <>
                    <div className="form-row">
                      <Field label="Company" value={authForm.company} onChange={(value) => setAuthForm({ ...authForm, company: value })} />
                      <Field label="Country" value={authForm.country} onChange={(value) => setAuthForm({ ...authForm, country: value })} />
                    </div>
                    <Field label="Phone number" value={authForm.phoneNumber} onChange={(value) => setAuthForm({ ...authForm, phoneNumber: value })} />
                  </>
                ) : null}
                <div className="actions">
                  <button className="button" onClick={handleAuthSubmit} disabled={busy}>
                    {authMode === "signup" ? "Create account" : "Sign in"}
                  </button>
                  <button
                    className="ghost-button"
                    onClick={() => {
                      window.location.href = api.googleLoginUrl();
                    }}
                    disabled={busy}
                  >
                    Continue with Google
                  </button>
                </div>
              </div>
            ) : (
              <div className="form-grid">
                <div className="card compact-card">
                  <div className="meta-list">
                    <span>{profile.email}</span>
                    <span>Role: {profile.role}</span>
                    <span>Provider: {profile.authProvider}</span>
                    <span>Email verified: {profile.emailVerified ? "Yes" : "No"}</span>
                  </div>
                </div>
                <div className="form-row">
                  <Field label="First name" value={profileForm.firstName} onChange={(value) => setProfileForm({ ...profileForm, firstName: value })} />
                  <Field label="Last name" value={profileForm.lastName} onChange={(value) => setProfileForm({ ...profileForm, lastName: value })} />
                </div>
                <div className="form-row">
                  <Field label="Company" value={profileForm.company} onChange={(value) => setProfileForm({ ...profileForm, company: value })} />
                  <Field label="Country" value={profileForm.country} onChange={(value) => setProfileForm({ ...profileForm, country: value })} />
                </div>
                <Field label="Phone number" value={profileForm.phoneNumber} onChange={(value) => setProfileForm({ ...profileForm, phoneNumber: value })} />
                {session?.apiKey ? (
                  <div className="token-block compact-token">
                    API key
                    <code>{session.apiKey}</code>
                  </div>
                ) : null}
                <div className="actions">
                  <button className="button" onClick={handleProfileSave} disabled={busy}>
                    Save profile
                  </button>
                  <button className="danger-button" onClick={handleLogout}>
                    Sign out
                  </button>
                </div>
              </div>
            )}
          </aside>
        </div>
      ) : null}

      <section className="grid dense-two-column">
        <div className="panel">
          <div className="section-header">
            <div>
              <h2>Your Access</h2>
              <div className="panel-intro">Active access rights are merged by offer, so repeat purchases do not render as duplicates.</div>
            </div>
          </div>
          {profile ? (
            accessSummaries.length > 0 ? (
              <div className="split-panel">
                <div className="catalog-list compact-list">
                  {accessSummaries.map((entry) => (
                    <button
                      key={entry.key}
                      className={`catalog-list-item ${selectedAccess?.key === entry.key ? "catalog-list-item-active" : ""}`}
                      onClick={() => setSelectedAccessProductId(entry.key)}
                    >
                      <span className="catalog-list-title">{entry.product.name}</span>
                      <span className="helper">{describeProductMode(entry.product)}</span>
                      <span className="helper">{entry.purchasedUnits} unit{entry.purchasedUnits === 1 ? "" : "s"} active</span>
                    </button>
                  ))}
                </div>
                <div className="card compact-card">
                  {selectedAccess ? (
                    <>
                      <div className="pill-row">
                        <span className="pill">{selectedAccess.product.code}</span>
                        <span className="pill">{selectedAccess.product.accessType}</span>
                      </div>
                      <strong>{selectedAccess.product.name}</strong>
                      <div className="meta-list compact-meta">
                        <span>Purchased units: {selectedAccess.purchasedUnits}</span>
                        <span>Batch used: {selectedAccess.batchDownloadUsedMb} MB</span>
                        <span>Realtime used: {selectedAccess.realtimeSubscriptionsUsed}</span>
                        <span>Payload used: {selectedAccess.payloadKilobytesUsed} KB</span>
                        <span>Granted: {formatDate(selectedAccess.grantedAt)}</span>
                        <span>Expires: {selectedAccess.expiresAt ? formatDate(selectedAccess.expiresAt) : "Open-ended"}</span>
                        <span>Active entitlement rows merged: {selectedAccess.entitlementCount}</span>
                      </div>
                    </>
                  ) : null}
                </div>
              </div>
            ) : (
              <div className="helper">No active entitlements yet. Complete checkout successfully to grant access.</div>
            )
          ) : (
            <div className="helper">Sign in to view active entitlements and quota usage.</div>
          )}
        </div>

        <div className="panel">
          <div className="section-header">
            <div>
              <h2>Shopping Cart</h2>
              <div className="panel-intro">Keep cart and checkout visible while browsing the catalog.</div>
            </div>
          </div>
          {cartEntries.length > 0 ? (
            <div className="activity-list compact-activity-list">
                {cartEntries.map((entry) => (
                  <div className="activity-card compact-card" key={entry.product.id}>
                  <div className="compact-card-header">
                    <strong>{entry.product.name}</strong>
                    <span className="pill">{formatMoney(entry.quotedUnitPrice * entry.quantity, entry.product.currency)}</span>
                  </div>
                  <div className="meta-list compact-meta">
                    <span>{entry.catalogItemName}</span>
                    <span>{describeProductMode(entry.product)}</span>
                    <span>{entry.selectionSummary}</span>
                  </div>
                  <div className="inline-form-row">
                    <Field
                      label="Units"
                      value={String(entry.quantity)}
                      onChange={(value) => updateCartQuantity(entry.product.id, Math.max(1, Number(value) || 1))}
                      type="number"
                    />
                    <div className="actions compact-actions">
                      <button className="ghost-button" onClick={() => updateCartQuantity(entry.product.id, entry.quantity - 1)} disabled={entry.quantity <= 1}>
                        -1
                      </button>
                      <button className="ghost-button" onClick={() => updateCartQuantity(entry.product.id, entry.quantity + 1)}>
                        +1
                      </button>
                      <button className="ghost-button" onClick={() => removeFromCart(entry.product.id)}>
                        Remove
                      </button>
                    </div>
                  </div>
                </div>
              ))}
              <div className="card compact-card checkout-summary-card">
                <div className="meta-list compact-meta">
                  <span>Cart lines: {cartEntries.length}</span>
                  <span>Total: {formatMoney(cartTotal, cartEntries[0]?.product.currency || "usd")}</span>
                  <span>{cartEntries[0]?.product.accessType === "SUBSCRIPTION" ? "Subscription checkout" : "One-time checkout"}</span>
                </div>
                <div className="actions">
                  <button className="button" onClick={handleCartCheckout} disabled={busy || cartEntries.length === 0}>
                    Checkout with Stripe
                  </button>
                  <button className="ghost-button" onClick={clearCart} disabled={busy || cartEntries.length === 0}>
                    Clear cart
                  </button>
                </div>
              </div>
            </div>
          ) : (
            <div className="helper">The cart is empty. Add an offer from the catalog to prepare checkout.</div>
          )}
          {lastTransaction?.checkoutUrl ? (
            <div className="card compact-card mt-3">
              <strong>Stripe checkout ready</strong>
              <div className="helper">If the redirect was blocked, open the hosted checkout manually.</div>
              <div className="actions">
                <a className="button" href={lastTransaction.checkoutUrl} target="_blank" rel="noreferrer">
                  Open Stripe Checkout
                </a>
              </div>
            </div>
          ) : null}
        </div>
      </section>

      <section className="grid catalog-layout">
        <div className="panel">
          <div className="section-header">
            <div>
              <h2>Data Catalog</h2>
              <div className="panel-intro">
                Catalog items represent what is available in the lake, independent from pricing and purchase packaging.
              </div>
            </div>
          </div>
          <div className="card compact-card catalog-search-card">
            <div className="catalog-search-header">
              <div>
                <strong>Dataset search</strong>
                <div className="helper">Filter inventory by symbol coverage, dataset type, storage, and offer packaging.</div>
              </div>
              <div className="catalog-search-summary">
                <span>{catalogItems.length} match{catalogItems.length === 1 ? "" : "es"}</span>
              </div>
            </div>
            <div className="catalog-search-grid">
              <Field
                label="Symbol or *"
                value={catalogFilters.symbol}
                onChange={(value) => setCatalogFilters((current) => ({ ...current, symbol: value }))}
                placeholder="AAPL or *"
                className="field-span-2"
              />
              <Field
                label="Start datetime"
                value={catalogFilters.availableFrom}
                onChange={(value) => setCatalogFilters((current) => ({ ...current, availableFrom: value }))}
                type="datetime-local"
              />
              <Field
                label="End datetime"
                value={catalogFilters.availableTo}
                onChange={(value) => setCatalogFilters((current) => ({ ...current, availableTo: value }))}
                type="datetime-local"
              />
            </div>
            <div className="catalog-search-grid">
              <SelectField
                label="Data type"
                value={catalogFilters.marketDataType}
                options={[":Any data type", "QUOTE:QUOTE", "TICK:TICK", "NEWS:NEWS", "FUNDAMENTALS:FUNDAMENTALS", "CRYPTO:CRYPTO", "OTHER:OTHER"]}
                onChange={(value) => setCatalogFilters((current) => ({ ...current, marketDataType: value as CatalogFilters["marketDataType"] }))}
                optionValueMode="split-id"
              />
              <SelectField
                label="Storage"
                value={catalogFilters.storageSystem}
                options={[":Any storage", "DELTA_LAKE:DELTA_LAKE", "ICEBERG:ICEBERG", "STUB:STUB", "OTHER:OTHER"]}
                onChange={(value) => setCatalogFilters((current) => ({ ...current, storageSystem: value as CatalogFilters["storageSystem"] }))}
                optionValueMode="split-id"
              />
              <SelectField
                label="Offer mode"
                value={catalogFilters.accessType}
                options={[":Any offer mode", "ONE_TIME_PURCHASE:One-time purchase", "SUBSCRIPTION:Subscription"]}
                onChange={(value) => setCatalogFilters((current) => ({ ...current, accessType: value as CatalogFilters["accessType"] }))}
                optionValueMode="split-id"
              />
              <SelectField
                label="Billing"
                value={catalogFilters.billingInterval}
                options={[":Any billing", "ONE_TIME:One-time", "MONTHLY:Monthly", "YEARLY:Yearly"]}
                onChange={(value) => setCatalogFilters((current) => ({ ...current, billingInterval: value as CatalogFilters["billingInterval"] }))}
                optionValueMode="split-id"
              />
            </div>
            <div className="actions catalog-search-actions">
              <button className="button" onClick={handleCatalogSearch} disabled={busy}>
                Search catalog
              </button>
              <button className="ghost-button" onClick={handleCatalogFilterReset} disabled={busy}>
                Clear filters
              </button>
            </div>
          </div>
          <div className="catalog-table-head">
            <span>Dataset</span>
            <span>Type</span>
            <span>Offers</span>
          </div>
          <div className="catalog-list catalog-data-list">
            {catalogItems.map((item) => (
              <button
                key={item.id}
                className={`catalog-list-item ${selectedCatalogItem?.id === item.id ? "catalog-list-item-active" : ""}`}
                onClick={() => setSelectedCatalogItemId(item.id)}
              >
                <span className="catalog-list-main">
                  <span className="catalog-list-title">{item.name}</span>
                  <span className="catalog-list-code">{item.code}</span>
                </span>
                <span className="catalog-list-secondary">
                  {item.marketDataType} · {describeStorage(item.storageSystem)}
                </span>
                <span className="catalog-list-tertiary">
                  {item.offers.length} linked offer{item.offers.length === 1 ? "" : "s"} ·{" "}
                  {item.offers[0]?.quotedPrice != null ? formatMoney(item.offers[0].quotedPrice, item.offers[0].currency) : "No quote"}
                </span>
              </button>
            ))}
          </div>
        </div>

        <div className="panel">
          <div className="section-header">
            <div>
              <h2>Catalog Details</h2>
              <div className="panel-intro">Click a catalog item to inspect its lake metadata, then choose an offer to add to the cart.</div>
            </div>
          </div>
          {selectedCatalogItem ? (
            <div className="grid">
              <div className="card catalog-detail-card">
                <div className="catalog-detail-header">
                  <div>
                    <div className="pill-row">
                      <span className="pill">{selectedCatalogItem.marketDataType}</span>
                      <span className="pill">{describeStorage(selectedCatalogItem.storageSystem)}</span>
                      <span className="pill">{selectedCatalogItem.code}</span>
                    </div>
                    <strong>{selectedCatalogItem.name}</strong>
                  </div>
                  <div className="catalog-detail-metrics">
                    <span>{selectedCatalogItem.offers.length} offers</span>
                    <span>{selectedCatalogItem.sampleSymbols ? "Sample symbols ready" : "No sample symbols"}</span>
                  </div>
                </div>
                <div className="meta-list">
                  <span>{selectedCatalogItem.summary || "No summary provided."}</span>
                  <span>{selectedCatalogItem.description || "No detailed description provided."}</span>
                  <span>Selected dataset definition: {selectedCatalogItem.selectionSummary || "Default catalog selection"}</span>
                  <span>Delivery API: {selectedCatalogItem.deliveryApiPath || "Not assigned"}</span>
                  <span>Lake query reference: {selectedCatalogItem.lakeQueryReference || "Not assigned"}</span>
                  <span>Sample symbols: {selectedCatalogItem.sampleSymbols || "Not assigned"}</span>
                  <span>
                    Coverage: {selectedCatalogItem.coverageStartDate || "Unknown"} to {selectedCatalogItem.coverageEndDate || "Open"}
                  </span>
                </div>
              </div>

              <div>
                <div className="section-header">
                  <div>
                    <h3>Purchase or Subscribe</h3>
                    <div className="helper">Offers define how this dataset is sold through the data shop.</div>
                  </div>
                </div>
                <div className="catalog-grid">
                  {selectedCatalogItem.offers.map((product) => (
                    <ProductCard
                      key={product.id}
                      product={product}
                      quantity={quantities[product.id] ?? 1}
                      cartQuantity={cart[product.id]?.quantity ?? 0}
                      onQuantityChange={(productId, quantity) => setQuantities((current) => ({ ...current, [productId]: quantity }))}
                      onAddToCart={handleAddToCart}
                      onRemoveFromCart={removeFromCart}
                      onUsageAction={profile ? handleUsage : undefined}
                      disabled={busy}
                    />
                  ))}
                </div>
                {selectedCatalogItem.offers.length === 0 ? (
                  <div className="helper">No sellable offers are linked to this catalog item yet.</div>
                ) : null}
              </div>

              <div className="card catalog-detail-card">
                <div className="section-header">
                  <div>
                    <h3>OTD Query Console</h3>
                    <div className="helper">Run a simple SQL query, export results to FSS as Parquet, and receive signed delivery links.</div>
                  </div>
                </div>
                {profile ? (
                  otdEligibleOffers.length > 0 ? (
                    <div className="form-grid">
                      <SelectField
                        label="Paid one-time offer"
                        value={otdQueryForm.productId}
                        options={otdEligibleOffers.map((offer) => `${offer.id}:${offer.name}`)}
                        onChange={(value) => setOtdQueryForm((current) => ({ ...current, productId: value }))}
                        optionValueMode="split-id"
                      />
                      <Field
                        label="SQL query"
                        value={otdQueryForm.sql}
                        onChange={(value) => setOtdQueryForm((current) => ({ ...current, sql: value }))}
                        textarea
                      />
                      <div className="actions">
                        <button className="button" onClick={() => void handleOtdDelivery()} disabled={busy || !otdQueryForm.productId}>
                          Run OTD delivery
                        </button>
                      </div>
                      <div className="card compact-card">
                        <strong>Simulated Data Delivery link</strong>
                        {selectedCatalogDeliveries.length > 0 ? (
                          <div className="activity-list compact-activity-list">
                            {selectedCatalogDeliveries.slice(0, 3).map((delivery) => (
                              <div className="activity-card compact-card" key={delivery.deliveryId}>
                                <div className="meta-list compact-meta">
                                  <span>{delivery.productCode}</span>
                                  <span>{delivery.rowCount} rows · {delivery.fileCount} file{delivery.fileCount === 1 ? "" : "s"}</span>
                                  <span>Consumed {delivery.consumedMegabytes} MB</span>
                                  <span>Created {formatDate(delivery.createdAt)}</span>
                                </div>
                                <div className="actions">
                                  {delivery.files.map((file) => (
                                    <a className="button" href={file.signedUrl} target="_blank" rel="noreferrer" key={file.objectKey}>
                                      {file.fileName}
                                    </a>
                                  ))}
                                </div>
                              </div>
                            ))}
                          </div>
                        ) : (
                          <div className="helper">No simulated delivery generated for this dataset yet.</div>
                        )}
                      </div>
                    </div>
                  ) : (
                    <div className="helper">This dataset does not currently have a one-time delivery offer linked to it.</div>
                  )
                ) : (
                  <div className="helper">Sign in and complete checkout before running an OTD delivery.</div>
                )}
              </div>
            </div>
          ) : (
            <div className="helper">No catalog items available yet.</div>
          )}
        </div>
      </section>

      <section className="panel">
        <div className="section-header">
          <div>
            <h2>Purchase History</h2>
            <div className="panel-intro">Raw checkout history stays separate from active entitlements and uses the same compact list-detail pattern as the catalog.</div>
          </div>
        </div>
        {profile ? (
          payments.length > 0 ? (
            <div className="split-panel">
              <div className="catalog-list compact-list">
                {payments.map((payment) => (
                  <button
                    key={payment.id}
                    className={`catalog-list-item ${selectedPayment?.id === payment.id ? "catalog-list-item-active" : ""}`}
                    onClick={() => setSelectedPaymentId(payment.id)}
                  >
                    <span className="catalog-list-title">
                      {payment.items?.map((item) => `${item.product.code} x${item.quantity}`).join(", ") || payment.product.name}
                    </span>
                    <span className="helper">{payment.status} · {formatMoney(payment.amount, payment.currency)}</span>
                    <span className="helper">{formatDate(payment.createdAt)}</span>
                  </button>
                ))}
              </div>
              <div className="card compact-card">
                {selectedPayment ? (
                  <>
                    <div className="pill-row">
                      <span className="pill">#{selectedPayment.id}</span>
                      <span className="pill">{selectedPayment.status}</span>
                    </div>
                    <strong>
                      {selectedPayment.items?.map((item) => `${item.product.code} x${item.quantity}`).join(", ") || selectedPayment.product.name}
                    </strong>
                    <div className="meta-list compact-meta">
                      <span>Total: {formatMoney(selectedPayment.amount, selectedPayment.currency)}</span>
                      <span>Created: {formatDate(selectedPayment.createdAt)}</span>
                      <span>Updated: {formatDate(selectedPayment.updatedAt)}</span>
                      <span>
                        Items:{" "}
                        {selectedPayment.items?.map((item) => `${item.product.name} x${item.quantity}`).join(", ") || selectedPayment.product.name}
                      </span>
                      <span>{selectedPayment.errorMessage ? `Error: ${selectedPayment.errorMessage}` : "Stripe checkout recorded."}</span>
                    </div>
                  </>
                ) : null}
              </div>
            </div>
          ) : (
            <div className="helper">No purchase history yet for this user.</div>
          )
        ) : (
          <div className="helper">Sign in to inspect checkout and payment history.</div>
        )}
      </section>

      <section className="panel" id="market-data">
        <div className="section-header">
          <div>
            <h2>Market Data Preview</h2>
            <div className="panel-intro">
              Market data rows remain separate from the catalog because they represent runtime content, not sellable metadata.
            </div>
          </div>
        </div>
        {marketDataRuntime ? <div className="status warn">{marketDataRuntime.message}</div> : null}
        <div className="market-grid">
          {Object.entries(groupedMarketData).map(([dataType, rows]) => (
            <div className="market-card" key={dataType}>
              <strong>{dataType}</strong>
              <div className="meta-list">
                {rows.slice(0, 5).map((row) => (
                  <span key={row.id}>
                    {row.symbol} at {formatMoney(row.price, "usd")} on {row.marketDate}
                  </span>
                ))}
              </div>
            </div>
          ))}
        </div>
      </section>

      {profile?.role === "ADMIN" ? (
        <section className="grid">
          <div className="panel">
            <div className="section-header">
              <div>
                <h2>Administration</h2>
                <div className="panel-intro">Manage catalog metadata, linked offers, preview rows, and audit activity.</div>
              </div>
            </div>
            <div className="stats-grid">
              <StatCard label="Users" value={dashboard?.totalUsers ?? 0} />
              <StatCard label="Offers" value={dashboard?.totalProducts ?? 0} />
              <StatCard label="Payments" value={dashboard?.totalPayments ?? 0} />
              <StatCard label="API keys" value={dashboard?.totalApiKeys ?? 0} />
              <StatCard label="Usage events" value={dashboard?.totalUsageRecords ?? 0} />
              <StatCard label="Active entitlements" value={dashboard?.activeEntitlements ?? 0} />
            </div>
          </div>

          <div className="columns">
            <div className="panel">
              <h3>Create catalog item</h3>
              <div className="form-grid">
                <div className="form-row">
                  <Field label="Code" value={catalogItemForm.code} onChange={(value) => setCatalogItemForm({ ...catalogItemForm, code: value })} />
                  <Field label="Name" value={catalogItemForm.name} onChange={(value) => setCatalogItemForm({ ...catalogItemForm, name: value })} />
                </div>
                <Field label="Summary" value={catalogItemForm.summary} onChange={(value) => setCatalogItemForm({ ...catalogItemForm, summary: value })} />
                <Field
                  label="Description"
                  value={catalogItemForm.description}
                  onChange={(value) => setCatalogItemForm({ ...catalogItemForm, description: value })}
                  textarea
                />
                <div className="form-row">
                  <SelectField
                    label="Market data type"
                    value={catalogItemForm.marketDataType}
                    onChange={(value) => setCatalogItemForm({ ...catalogItemForm, marketDataType: value as CatalogItem["marketDataType"] })}
                    options={["QUOTE", "TICK", "NEWS", "FUNDAMENTALS", "CRYPTO", "OTHER"]}
                  />
                  <SelectField
                    label="Storage system"
                    value={catalogItemForm.storageSystem}
                    onChange={(value) => setCatalogItemForm({ ...catalogItemForm, storageSystem: value as CatalogItem["storageSystem"] })}
                    options={["DELTA_LAKE", "ICEBERG", "STUB", "OTHER"]}
                  />
                </div>
                <div className="form-row">
                  <Field
                    label="Delivery API path"
                    value={catalogItemForm.deliveryApiPath}
                    onChange={(value) => setCatalogItemForm({ ...catalogItemForm, deliveryApiPath: value })}
                  />
                  <Field
                    label="Lake query reference"
                    value={catalogItemForm.lakeQueryReference}
                    onChange={(value) => setCatalogItemForm({ ...catalogItemForm, lakeQueryReference: value })}
                  />
                </div>
                <Field
                  label="Sample symbols"
                  value={catalogItemForm.sampleSymbols}
                  onChange={(value) => setCatalogItemForm({ ...catalogItemForm, sampleSymbols: value })}
                />
                <div className="form-row">
                  <Field
                    label="Coverage start"
                    value={catalogItemForm.coverageStartDate}
                    onChange={(value) => setCatalogItemForm({ ...catalogItemForm, coverageStartDate: value })}
                    type="date"
                  />
                  <Field
                    label="Coverage end"
                    value={catalogItemForm.coverageEndDate}
                    onChange={(value) => setCatalogItemForm({ ...catalogItemForm, coverageEndDate: value })}
                    type="date"
                  />
                </div>
                <button className="button" onClick={handleCreateCatalogItem} disabled={busy}>
                  Publish catalog item
                </button>
              </div>
            </div>

            <div className="panel">
              <h3>Manage sellable offer</h3>
              <div className="form-grid">
                <SelectField
                  label="Offer editor"
                  value={editingProductId}
                  onChange={(value) => {
                    setEditingProductId(value);
                    const selectedProduct = editableProducts.find((product) => String(product.id) === value);
                    setProductForm(selectedProduct ? toAdminProductForm(selectedProduct) : defaultProductForm);
                  }}
                  options={["0:Create new offer", ...editableProducts.map((product) => `${product.id}:${product.name} (${product.catalogItemLabel})`)]}
                  optionValueMode="split-id"
                />
                <SelectField
                  label="Catalog item"
                  value={productForm.catalogItemId}
                  onChange={(value) => setProductForm({ ...productForm, catalogItemId: value })}
                  options={catalogItems.map((item) => `${item.id}:${item.name}`)}
                  optionValueMode="split-id"
                />
                <div className="form-row">
                  <Field label="Offer code" value={productForm.code} onChange={(value) => setProductForm({ ...productForm, code: value })} />
                  <Field label="Offer name" value={productForm.name} onChange={(value) => setProductForm({ ...productForm, name: value })} />
                </div>
                <Field label="Description" value={productForm.description} onChange={(value) => setProductForm({ ...productForm, description: value })} textarea />
                <div className="form-row">
                  <Field label="Base price" value={productForm.price} onChange={(value) => setProductForm({ ...productForm, price: value })} />
                  <Field label="Minimum price" value={productForm.minimumPrice} onChange={(value) => setProductForm({ ...productForm, minimumPrice: value })} />
                  <Field label="Currency" value={productForm.currency} onChange={(value) => setProductForm({ ...productForm, currency: value })} />
                </div>
                <div className="form-row">
                  <Field label="Included symbols" value={productForm.includedSymbols} onChange={(value) => setProductForm({ ...productForm, includedSymbols: value })} />
                  <Field label="Included days" value={productForm.includedDays} onChange={(value) => setProductForm({ ...productForm, includedDays: value })} />
                  <Field
                    label="Universe symbols"
                    value={productForm.fullUniverseSymbolCount}
                    onChange={(value) => setProductForm({ ...productForm, fullUniverseSymbolCount: value })}
                  />
                </div>
                <div className="form-row">
                  <Field
                    label="Price per extra symbol"
                    value={productForm.pricePerAdditionalSymbol}
                    onChange={(value) => setProductForm({ ...productForm, pricePerAdditionalSymbol: value })}
                  />
                  <Field
                    label="Price per extra day"
                    value={productForm.pricePerAdditionalDay}
                    onChange={(value) => setProductForm({ ...productForm, pricePerAdditionalDay: value })}
                  />
                </div>
                <div className="form-row">
                  <SelectField
                    label="Access type"
                    value={productForm.accessType}
                    onChange={(value) => {
                      const nextAccessType = value as AdminProductForm["accessType"];
                      setProductForm({
                        ...productForm,
                        accessType: nextAccessType,
                        billingInterval: nextAccessType === "ONE_TIME_PURCHASE" ? "ONE_TIME" : productForm.billingInterval
                      });
                    }}
                    options={["ONE_TIME_PURCHASE", "SUBSCRIPTION"]}
                  />
                  <SelectField
                    label="Billing interval"
                    value={productForm.billingInterval}
                    onChange={(value) => setProductForm({ ...productForm, billingInterval: value as AdminProductForm["billingInterval"] })}
                    options={productForm.accessType === "SUBSCRIPTION" ? ["MONTHLY", "YEARLY"] : ["ONE_TIME"]}
                  />
                </div>
                <div className="form-row">
                  <Field
                    label="Batch limit MB"
                    value={productForm.batchDownloadLimitMb}
                    onChange={(value) => setProductForm({ ...productForm, batchDownloadLimitMb: value })}
                  />
                  <Field
                    label="Realtime subscriptions"
                    value={productForm.realtimeSubscriptionLimit}
                    onChange={(value) => setProductForm({ ...productForm, realtimeSubscriptionLimit: value })}
                  />
                </div>
                <Field
                  label="Max realtime payload KB"
                  value={productForm.maxRealtimePayloadKb}
                  onChange={(value) => setProductForm({ ...productForm, maxRealtimePayloadKb: value })}
                />
                <div className="actions">
                  <button className="button" onClick={handleSaveProduct} disabled={busy || !productForm.catalogItemId}>
                    {editingProductId === "0" ? "Publish offer" : "Update offer pricing"}
                  </button>
                  {editingProductId !== "0" ? (
                    <button
                      className="ghost-button"
                      onClick={() => {
                        setEditingProductId("0");
                        setProductForm(defaultProductForm);
                      }}
                      disabled={busy}
                    >
                      Reset editor
                    </button>
                  ) : null}
                </div>
              </div>
            </div>
          </div>

          <div className="columns">
            <div className="panel">
              <h3>Publish preview market data</h3>
              <div className="form-grid">
                <div className="form-row">
                  <Field label="Symbol" value={marketDataForm.symbol} onChange={(value) => setMarketDataForm({ ...marketDataForm, symbol: value })} />
                  <SelectField
                    label="Data type"
                    value={marketDataForm.dataType}
                    onChange={(value) => setMarketDataForm({ ...marketDataForm, dataType: value as MarketDataForm["dataType"] })}
                    options={["QUOTE", "TICK", "NEWS", "FUNDAMENTALS", "CRYPTO", "OTHER"]}
                  />
                </div>
                <div className="form-row">
                  <Field label="Price" value={marketDataForm.price} onChange={(value) => setMarketDataForm({ ...marketDataForm, price: value })} />
                  <Field label="Volume" value={marketDataForm.volume} onChange={(value) => setMarketDataForm({ ...marketDataForm, volume: value })} />
                </div>
                <Field
                  label="Timestamp"
                  value={marketDataForm.timestamp}
                  onChange={(value) => setMarketDataForm({ ...marketDataForm, timestamp: value })}
                  type="datetime-local"
                />
                <button className="button" onClick={handleCreateMarketData} disabled={busy}>
                  Save preview row
                </button>
              </div>
            </div>

            <div className="panel">
              <div className="section-header">
                <div>
                  <h3>User management</h3>
                  <div className="helper">Edit user profile fields, promote or demote roles, and create local users.</div>
                </div>
              </div>
              <div className="split-panel">
                <div className="catalog-list compact-list">
                  {adminUsers.map((user) => (
                    <button
                      key={user.id}
                      className={`catalog-list-item ${selectedAdminUser?.id === user.id ? "catalog-list-item-active" : ""}`}
                      onClick={() => setSelectedAdminUserId(user.id)}
                    >
                      <span className="catalog-list-title">{user.email}</span>
                      <span className="helper">
                        {user.firstName} {user.lastName}
                      </span>
                      <span className="helper">
                        {user.role} · {user.emailVerified ? "Verified" : "Unverified"}
                      </span>
                    </button>
                  ))}
                </div>
                <div className="form-grid">
                  {selectedAdminUser ? (
                    <>
                      <div className="pill-row">
                        <span className="pill">#{selectedAdminUser.id}</span>
                        <span className="pill">{selectedAdminUser.authProvider}</span>
                        <span className="pill">{selectedAdminUser.role}</span>
                      </div>
                      <Field label="Email" value={adminUserForm.email} onChange={(value) => setAdminUserForm({ ...adminUserForm, email: value })} />
                      <div className="form-row">
                        <Field
                          label="First name"
                          value={adminUserForm.firstName}
                          onChange={(value) => setAdminUserForm({ ...adminUserForm, firstName: value })}
                        />
                        <Field
                          label="Last name"
                          value={adminUserForm.lastName}
                          onChange={(value) => setAdminUserForm({ ...adminUserForm, lastName: value })}
                        />
                      </div>
                      <div className="form-row">
                        <Field label="Company" value={adminUserForm.company} onChange={(value) => setAdminUserForm({ ...adminUserForm, company: value })} />
                        <Field label="Country" value={adminUserForm.country} onChange={(value) => setAdminUserForm({ ...adminUserForm, country: value })} />
                      </div>
                      <Field
                        label="Phone number"
                        value={adminUserForm.phoneNumber}
                        onChange={(value) => setAdminUserForm({ ...adminUserForm, phoneNumber: value })}
                      />
                      <div className="form-row">
                        <SelectField
                          label="Role"
                          value={adminUserForm.role}
                          onChange={(value) => setAdminUserForm({ ...adminUserForm, role: value as UserProfile["role"] })}
                          options={["USER", "ADMIN"]}
                        />
                        <SelectField
                          label="Email status"
                          value={adminUserForm.emailVerified ? "VERIFIED" : "UNVERIFIED"}
                          onChange={(value) => setAdminUserForm({ ...adminUserForm, emailVerified: value === "VERIFIED" })}
                          options={["VERIFIED", "UNVERIFIED"]}
                        />
                      </div>
                      <div className="actions">
                        <button className="button" onClick={handleAdminUserSave} disabled={busy}>
                          Save user
                        </button>
                        <button className="ghost-button" onClick={() => void handleAdminRoleQuickChange("ADMIN")} disabled={busy}>
                          Promote to admin
                        </button>
                        <button className="ghost-button" onClick={() => void handleAdminRoleQuickChange("USER")} disabled={busy}>
                          Set as user
                        </button>
                      </div>
                    </>
                  ) : (
                    <div className="helper">No users available yet.</div>
                  )}
                </div>
              </div>
              <div className="card compact-card mt-3">
                <strong>Create local user</strong>
                <div className="form-grid">
                  <Field label="Email" value={createUserForm.email} onChange={(value) => setCreateUserForm({ ...createUserForm, email: value })} />
                  <div className="form-row">
                    <Field
                      label="First name"
                      value={createUserForm.firstName}
                      onChange={(value) => setCreateUserForm({ ...createUserForm, firstName: value })}
                    />
                    <Field
                      label="Last name"
                      value={createUserForm.lastName}
                      onChange={(value) => setCreateUserForm({ ...createUserForm, lastName: value })}
                    />
                  </div>
                  <Field
                    label="Password"
                    value={createUserForm.password}
                    onChange={(value) => setCreateUserForm({ ...createUserForm, password: value })}
                    type="password"
                  />
                  <div className="form-row">
                    <Field label="Company" value={createUserForm.company} onChange={(value) => setCreateUserForm({ ...createUserForm, company: value })} />
                    <Field label="Country" value={createUserForm.country} onChange={(value) => setCreateUserForm({ ...createUserForm, country: value })} />
                  </div>
                  <Field
                    label="Phone number"
                    value={createUserForm.phoneNumber}
                    onChange={(value) => setCreateUserForm({ ...createUserForm, phoneNumber: value })}
                  />
                  <button className="button" onClick={handleAdminCreateUser} disabled={busy}>
                    Create user
                  </button>
                </div>
              </div>
              <div className="grid mt-3">
                <div className="card compact-card">
                  <strong>Persisted purchase history</strong>
                  {selectedAdminUserPayments.length > 0 ? (
                    <div className="activity-list compact-activity-list">
                      {selectedAdminUserPayments.slice(0, 6).map((payment) => (
                        <div className="activity-card compact-card" key={payment.id}>
                          <div className="compact-card-header">
                            <strong>#{payment.id}</strong>
                            <span className="pill">{payment.status}</span>
                          </div>
                          <div className="meta-list compact-meta">
                            <span>{formatMoney(payment.amount, payment.currency)}</span>
                            <span>{formatDate(payment.createdAt)}</span>
                            <span>
                              {payment.items?.map((item) => `${item.product.code} x${item.quantity}`).join(", ") || payment.product?.code || "N/A"}
                            </span>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="helper">No persisted purchases found for the selected user.</div>
                  )}
                </div>
                <div className="card compact-card">
                  <strong>Persisted entitlements</strong>
                  {selectedAdminUserEntitlements.length > 0 ? (
                    <div className="activity-list compact-activity-list">
                      {selectedAdminUserEntitlements.map((entitlement) => (
                        <div className="activity-card compact-card" key={entitlement.id}>
                          <div className="compact-card-header">
                            <strong>{entitlement.product.code}</strong>
                            <span className="pill">{entitlement.status}</span>
                          </div>
                          <div className="meta-list compact-meta">
                            <span>{entitlement.accessType}</span>
                            <span>Units: {entitlement.purchasedUnits}</span>
                            <span>Granted: {formatDate(entitlement.grantedAt)}</span>
                            <span>Expires: {entitlement.expiresAt ? formatDate(entitlement.expiresAt) : "Open-ended"}</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <div className="helper">No persisted entitlements found for the selected user.</div>
                  )}
                </div>
              </div>
            </div>

            <div className="table-card">
              <div className="header-bar">
                <div>
                  <h3>Recent payment activity</h3>
                  <div className="helper">Administrative audit view of Stripe checkout creation and payment state.</div>
                </div>
              </div>
              <div className="activity-list">
                {dashboard?.recentPayments.map((payment) => (
                  <div className="activity-card" key={payment.id}>
                    <strong>{payment.productCode}</strong>
                    <div className="meta-list">
                      <span>{payment.userEmail}</span>
                      <span>{payment.status}</span>
                      <span>{formatMoney(payment.amount, payment.currency)}</span>
                      <span>{formatDate(payment.createdAt)}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </section>
      ) : null}

      {lastTransaction ? (
        <section className="panel">
          <h2>Latest checkout transaction</h2>
          <div className="card">
            <div className="meta-list">
              <span>ID: {lastTransaction.id}</span>
              <span>Status: {lastTransaction.status}</span>
              <span>Amount: {formatMoney(lastTransaction.amount, lastTransaction.currency)}</span>
              <span>
                Items:{" "}
                {lastTransaction.items?.map((item) => `${item.product.code} x${item.quantity}`).join(", ") || lastTransaction.product.name}
              </span>
              <span>Stripe checkout URL: {lastTransaction.checkoutUrl || "Pending creation"}</span>
              <span>Error: {lastTransaction.errorMessage || "None"}</span>
            </div>
          </div>
        </section>
      ) : null}
    </main>
  );
}

function groupMarketData(rows: MarketData[]) {
  return rows.reduce<Record<string, MarketData[]>>((accumulator, row) => {
    accumulator[row.dataType] = accumulator[row.dataType] ?? [];
    accumulator[row.dataType].push(row);
    return accumulator;
  }, {});
}

function aggregateEntitlements(rows: Entitlement[]): AccessSummary[] {
  return Object.values(
    rows.reduce<Record<number, AccessSummary>>((accumulator, entitlement) => {
      const key = entitlement.product.id;
      const existing = accumulator[key];
      if (!existing) {
        accumulator[key] = {
          key,
          product: entitlement.product,
          purchasedUnits: entitlement.purchasedUnits,
          batchDownloadUsedMb: entitlement.batchDownloadUsedMb,
          realtimeSubscriptionsUsed: entitlement.realtimeSubscriptionsUsed,
          payloadKilobytesUsed: entitlement.payloadKilobytesUsed,
          grantedAt: entitlement.grantedAt,
          expiresAt: entitlement.expiresAt,
          entitlementCount: 1
        };
        return accumulator;
      }

      existing.purchasedUnits += entitlement.purchasedUnits;
      existing.batchDownloadUsedMb += entitlement.batchDownloadUsedMb;
      existing.realtimeSubscriptionsUsed += entitlement.realtimeSubscriptionsUsed;
      existing.payloadKilobytesUsed += entitlement.payloadKilobytesUsed;
      existing.entitlementCount += 1;

      if (new Date(entitlement.grantedAt).getTime() > new Date(existing.grantedAt).getTime()) {
        existing.grantedAt = entitlement.grantedAt;
      }

      if (!existing.expiresAt || (entitlement.expiresAt && new Date(entitlement.expiresAt).getTime() > new Date(existing.expiresAt).getTime())) {
        existing.expiresAt = entitlement.expiresAt;
      }

      return accumulator;
    }, {})
  );
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  return "Unexpected request failure.";
}

async function withRetries<T>(operation: () => Promise<T>, retries: number, retryDelayMs: number) {
  let lastError: unknown;

  for (let attempt = 0; attempt <= retries; attempt += 1) {
    try {
      return await operation();
    } catch (error) {
      lastError = error;
      if (attempt === retries) {
        break;
      }
      if (retryDelayMs > 0) {
        await delay(retryDelayMs);
      }
    }
  }

  throw lastError;
}

function delay(ms: number) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

function getQuotedUnitPrice(product: DataProduct) {
  return Number(product.quotedPrice ?? product.price);
}

function buildCatalogSelectionPayload(filters: CatalogFilters): CatalogSelectionPayload {
  return {
    symbol: filters.symbol.trim() || null,
    availableFrom: filters.availableFrom || null,
    availableTo: filters.availableTo || null
  };
}

function toAdminProductForm(product: DataProduct): AdminProductForm {
  return {
    catalogItemId: String(product.catalogItemId ?? ""),
    code: product.code,
    name: product.name,
    description: product.description || "",
    price: String(product.price ?? ""),
    minimumPrice: String(product.minimumPrice ?? product.price ?? ""),
    includedSymbols: String(product.includedSymbols ?? 1),
    includedDays: String(product.includedDays ?? 1),
    pricePerAdditionalSymbol: String(product.pricePerAdditionalSymbol ?? 0),
    pricePerAdditionalDay: String(product.pricePerAdditionalDay ?? 0),
    fullUniverseSymbolCount: String(product.fullUniverseSymbolCount ?? product.includedSymbols ?? 1),
    currency: product.currency,
    accessType: product.accessType,
    billingInterval: product.billingInterval,
    batchDownloadLimitMb: product.batchDownloadLimitMb == null ? "" : String(product.batchDownloadLimitMb),
    realtimeSubscriptionLimit: product.realtimeSubscriptionLimit == null ? "" : String(product.realtimeSubscriptionLimit),
    maxRealtimePayloadKb: product.maxRealtimePayloadKb == null ? "" : String(product.maxRealtimePayloadKb)
  };
}

function toAdminUserForm(user: UserProfile): AdminUserForm {
  return {
    id: String(user.id),
    email: user.email,
    firstName: user.firstName,
    lastName: user.lastName,
    company: user.company || "",
    country: user.country || "",
    phoneNumber: user.phoneNumber || "",
    role: user.role,
    emailVerified: user.emailVerified
  };
}

function toOptionalNumber(value: string) {
  if (!value.trim()) {
    return null;
  }
  return Number(value);
}

function StatCard({ label, value }: { label: string; value: number }) {
  return (
    <div className="stat-card">
      <span className="helper">{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function Field({
  label,
  value,
  onChange,
  textarea,
  type = "text",
  placeholder,
  className
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  textarea?: boolean;
  type?: string;
  placeholder?: string;
  className?: string;
}) {
  return (
    <div className={`field${className ? ` ${className}` : ""}`}>
      <label>{label}</label>
      {textarea ? (
        <textarea value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} />
      ) : (
        <input type={type} value={value} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} />
      )}
    </div>
  );
}

function SelectField({
  label,
  value,
  options,
  onChange,
  optionValueMode
}: {
  label: string;
  value: string;
  options: string[];
  onChange: (value: string) => void;
  optionValueMode?: "plain" | "split-id";
}) {
  return (
    <div className="field">
      <label>{label}</label>
      <select
        value={value}
        onChange={(event) =>
          onChange(optionValueMode === "split-id" ? event.target.value.split(":")[0] : event.target.value)
        }
      >
        {options.map((option) => (
          <option key={option} value={optionValueMode === "split-id" ? option.split(":")[0] : option}>
            {optionValueMode === "split-id" ? option.split(":").slice(1).join(":") : option}
          </option>
        ))}
      </select>
    </div>
  );
}
