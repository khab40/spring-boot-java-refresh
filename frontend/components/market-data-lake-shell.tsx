"use client";

import { useEffect, useRef, useState } from "react";
import { api, buildSession } from "../lib/api";
import { describeProductMode, formatDate, formatMoney } from "../lib/format";
import { loadSession, saveSession } from "../lib/storage";
import {
  AdminDashboard,
  DataProduct,
  Entitlement,
  MarketData,
  MarketDataRuntimeStatus,
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

type AdminProductForm = {
  code: string;
  name: string;
  description: string;
  price: string;
  currency: string;
  accessType: "ONE_TIME" | "SUBSCRIPTION";
  billingInterval: "ONE_TIME" | "MONTHLY" | "YEARLY";
  batchDownloadLimitMb: string;
  realtimeSubscriptionLimit: string;
  maxRealtimePayloadKb: string;
};

type MarketDataForm = {
  symbol: string;
  price: string;
  volume: string;
  timestamp: string;
  dataType: MarketData["dataType"];
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

const defaultProductForm: AdminProductForm = {
  code: "",
  name: "",
  description: "",
  price: "49.99",
  currency: "usd",
  accessType: "ONE_TIME",
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
  dataType: "TICK"
};

export function MarketDataLakeShell() {
  const [authMode, setAuthMode] = useState<AuthMode>("signin");
  const [authForm, setAuthForm] = useState<AuthForm>(defaultAuthForm);
  const [session, setSession] = useState<SessionState | null>(null);
  const [products, setProducts] = useState<DataProduct[]>([]);
  const [marketData, setMarketData] = useState<MarketData[]>([]);
  const [marketDataRuntime, setMarketDataRuntime] = useState<MarketDataRuntimeStatus | null>(null);
  const [entitlements, setEntitlements] = useState<Entitlement[]>([]);
  const [dashboard, setDashboard] = useState<AdminDashboard | null>(null);
  const [quantities, setQuantities] = useState<Record<number, number>>({});
  const [checkoutStatus, setCheckoutStatus] = useState<string>("");
  const [lastTransaction, setLastTransaction] = useState<PaymentTransaction | null>(null);
  const [message, setMessage] = useState<string>("Loading catalog and market data.");
  const [error, setError] = useState<string>("");
  const [busy, setBusy] = useState(false);
  const [productForm, setProductForm] = useState<AdminProductForm>(defaultProductForm);
  const [marketDataForm, setMarketDataForm] = useState<MarketDataForm>(defaultMarketDataForm);
  const privateRequestVersion = useRef(0);
  const passwordTooShort = authForm.password.length > 0 && authForm.password.length < 8;

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

      if (authError) {
        setError(messageParam || "Google sign-in failed.");
      } else if (authSuccess === "google") {
        setMessage(messageParam || "Signed in with Google successfully.");
      }
    }
  }, []);

  useEffect(() => {
    void refreshPublicData();
  }, []);

  useEffect(() => {
    const requestVersion = ++privateRequestVersion.current;

    if (!session?.accessToken) {
      setEntitlements([]);
      setDashboard(null);
      saveSession(null);
      return;
    }

    saveSession(session);
    void refreshPrivateData(session, requestVersion);
  }, [session]);

  async function refreshPublicData() {
    try {
      const [catalog, feed, runtime] = await Promise.all([
        api.products(),
        api.marketData(),
        api.marketDataRuntime()
      ]);
      setProducts(catalog);
      setMarketData(feed);
      setMarketDataRuntime(runtime);
      setMessage("Catalog is live and market-data previews are available.");
    } catch (requestError) {
      setError(getErrorMessage(requestError));
    }
  }

  async function refreshPrivateData(nextSession: SessionState, requestVersion: number) {
    try {
      const profile = await api.me(nextSession.accessToken);
      if (privateRequestVersion.current !== requestVersion) {
        return;
      }

      const updatedSession = {
        ...nextSession,
        profile
      };
      setSession(updatedSession);

      const userEntitlements = await api.myEntitlements(nextSession.accessToken);
      if (privateRequestVersion.current !== requestVersion) {
        return;
      }
      setEntitlements(userEntitlements);

      if (profile.role === "ADMIN") {
        const adminDashboard = await api.adminDashboard(nextSession.accessToken);
        if (privateRequestVersion.current !== requestVersion) {
          return;
        }
        setDashboard(adminDashboard);
      } else {
        setDashboard(null);
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
      setMessage(response.message || (authMode === "signup" ? "Account created." : "Signed in successfully."));
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
        // Logout is client-side only for now.
      }
    }

    setSession(null);
    setEntitlements([]);
    setDashboard(null);
    setCheckoutStatus("");
    setLastTransaction(null);
    setMessage("Signed out.");
  }

  async function handleCheckout(product: DataProduct) {
    if (!session?.accessToken) {
      setError("Sign in before checkout.");
      return;
    }

    setBusy(true);
    setError("");
      setCheckoutStatus(`Creating Stripe checkout for ${product.code}...`);

    try {
      const origin = typeof window === "undefined" ? "http://localhost:3000" : window.location.origin;
      const transaction = await api.checkout(
        {
          userId: session.userId,
          productId: product.id,
          successUrl: `${origin}/?checkout=success`,
          cancelUrl: `${origin}/?checkout=cancelled`
        },
        session.accessToken
      );

      setLastTransaction(transaction);
      const resolvedTransaction = await pollTransaction(transaction.id, session.accessToken);
      setLastTransaction(resolvedTransaction);
      setCheckoutStatus(`Payment status: ${resolvedTransaction.status}`);

      if (resolvedTransaction.checkoutUrl && typeof window !== "undefined") {
        window.open(resolvedTransaction.checkoutUrl, "_blank", "noopener,noreferrer");
      }
    } catch (requestError) {
      setError(getErrorMessage(requestError));
      setCheckoutStatus("");
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

  async function handleUsage(product: DataProduct) {
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

  async function handleCreateProduct() {
    if (!session?.accessToken) {
      return;
    }

    setBusy(true);
    setError("");
    try {
      await api.createProduct(
        {
          ...productForm,
          price: Number(productForm.price),
          batchDownloadLimitMb: Number(productForm.batchDownloadLimitMb),
          realtimeSubscriptionLimit: Number(productForm.realtimeSubscriptionLimit),
          maxRealtimePayloadKb: Number(productForm.maxRealtimePayloadKb)
        },
        session.accessToken
      );
      setProductForm(defaultProductForm);
      await refreshPublicData();
      if (session.profile?.role === "ADMIN") {
        setDashboard(await api.adminDashboard(session.accessToken));
      }
      setMessage("Catalog product created.");
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

  const profile: UserProfile | null | undefined = session?.profile;
  const groupedMarketData = groupMarketData(marketData);

  return (
    <main className="page-shell">
      <section className="hero">
        <div className="hero-card">
          <div className="eyebrow">Market Data Lake</div>
          <h1>Reactive trading-data storefront with checkout, access control, and admin audit.</h1>
          <p>
            Browse data products, compare streaming versus one-time delivery, sign up for access, launch Stripe
            checkout, and manage catalog or audit flows from a separate web container wired to the Java backend.
          </p>
          <div className="hero-badges">
            <span className="badge">Next.js UI container</span>
            <span className="badge">Java API backend</span>
            <span className="badge">Stripe sandbox checkout</span>
            <span className="badge">Stub market-data preview</span>
          </div>
        </div>
        <div className="hero-card">
          <div className="eyebrow">Session</div>
          <h2>{profile ? `${profile.firstName} ${profile.lastName}` : "No active session"}</h2>
          <p>
            {profile
              ? `${profile.email} is signed in with role ${profile.role}. API key based downstream usage is available from the UI.`
              : "Register or sign in to unlock checkout, entitlement tracking, and administration views."}
          </p>
          <div className="actions">
            {profile ? (
              <button className="danger-button" onClick={handleLogout}>
                Sign out
              </button>
            ) : (
              <>
                <button className="button" onClick={() => setAuthMode("signin")}>
                  Sign in
                </button>
                <button className="secondary-button" onClick={() => setAuthMode("signup")}>
                  Create account
                </button>
              </>
            )}
          </div>
          {session?.apiKey ? (
            <div className="token-block">
              API key
              <code>{session.apiKey}</code>
            </div>
          ) : null}
        </div>
      </section>

      {error ? <div className="status error">{error}</div> : null}
      {message ? <div className="status info">{message}</div> : null}
      {checkoutStatus ? <div className="status warn">{checkoutStatus}</div> : null}

      <section className="columns">
        <div className="panel">
          <div className="section-header">
            <div>
              <h2>Identity</h2>
              <div className="panel-intro">Sign up, sign in, and keep the browser session synced with JWT and API key state.</div>
            </div>
          </div>
          {!profile ? (
            <div className="auth-grid">
              <div className="form-card">
                <div className="pill-row">
                  <button className={authMode === "signin" ? "button" : "ghost-button"} onClick={() => setAuthMode("signin")}>
                    Sign in
                  </button>
                  <button className={authMode === "signup" ? "button" : "ghost-button"} onClick={() => setAuthMode("signup")}>
                    Sign up
                  </button>
                </div>
                <div className="form-grid">
                  <div className="field">
                    <label>Email</label>
                    <input
                      value={authForm.email}
                      onChange={(event) => setAuthForm({ ...authForm, email: event.target.value })}
                    />
                  </div>
                  {authMode === "signup" ? (
                    <div className="form-row">
                      <div className="field">
                        <label>First name</label>
                        <input
                          value={authForm.firstName}
                          onChange={(event) => setAuthForm({ ...authForm, firstName: event.target.value })}
                        />
                      </div>
                      <div className="field">
                        <label>Last name</label>
                        <input
                          value={authForm.lastName}
                          onChange={(event) => setAuthForm({ ...authForm, lastName: event.target.value })}
                        />
                      </div>
                    </div>
                  ) : null}
                  <div className="field">
                    <label>Password</label>
                    <input
                      type="password"
                      value={authForm.password}
                      onChange={(event) => setAuthForm({ ...authForm, password: event.target.value })}
                    />
                    {passwordTooShort ? (
                      <div className="helper" role="alert">
                        Password must be at least 8 characters long.
                      </div>
                    ) : null}
                  </div>
                  {authMode === "signup" ? (
                    <>
                      <div className="form-row">
                        <div className="field">
                          <label>Company</label>
                          <input
                            value={authForm.company}
                            onChange={(event) => setAuthForm({ ...authForm, company: event.target.value })}
                          />
                        </div>
                        <div className="field">
                          <label>Country</label>
                          <input
                            value={authForm.country}
                            onChange={(event) => setAuthForm({ ...authForm, country: event.target.value })}
                          />
                        </div>
                      </div>
                      <div className="field">
                        <label>Phone number</label>
                        <input
                          value={authForm.phoneNumber}
                          onChange={(event) => setAuthForm({ ...authForm, phoneNumber: event.target.value })}
                        />
                      </div>
                    </>
                  ) : null}
                  <button className="button" onClick={handleAuthSubmit} disabled={busy}>
                    {authMode === "signup" ? "Create account and send verification email" : "Sign in and issue API key"}
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
              <div className="form-card">
                <strong>What this session unlocks</strong>
                <div className="meta-list">
                  <span>Catalog and market data browsing remain open for evaluation.</span>
                  <span>Authenticated users can start Stripe checkout and track entitlements.</span>
                  <span>API key based usage recording supports stream and batch quota control.</span>
                  <span>Admin users get dashboard, product creation, and audit access.</span>
                </div>
              </div>
            </div>
          ) : (
            <div className="card">
              <strong>{profile.firstName} {profile.lastName}</strong>
              <div className="meta-list">
                <span>{profile.email}</span>
                <span>Role: {profile.role}</span>
                <span>Sign-in: {profile.authProvider}</span>
                <span>Company: {profile.company || "n/a"}</span>
                <span>Country: {profile.country || "n/a"}</span>
              </div>
            </div>
          )}
        </div>

        <div className="panel">
          <div className="section-header">
            <div>
              <h2>Your Access</h2>
              <div className="panel-intro">Purchased or subscribed products appear here with current usage consumption.</div>
            </div>
          </div>
          {profile ? (
            entitlements.length > 0 ? (
              <div className="audit-grid">
                {entitlements.map((entitlement) => (
                  <div className="activity-card" key={entitlement.id}>
                    <strong>{entitlement.product.name}</strong>
                    <div className="meta-list">
                      <span>Status: {entitlement.status}</span>
                      <span>Mode: {describeProductMode(entitlement.product)}</span>
                      <span>Batch used: {entitlement.batchDownloadUsedMb} MB</span>
                      <span>Realtime used: {entitlement.realtimeSubscriptionsUsed}</span>
                      <span>Payload used: {entitlement.payloadKilobytesUsed} KB</span>
                      <span>Granted: {formatDate(entitlement.grantedAt)}</span>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="helper">No entitlements yet. Complete checkout to grant access.</div>
            )
          ) : (
            <div className="helper">Sign in to view purchased products, subscription entitlements, and quota usage.</div>
          )}
        </div>
      </section>

      <section className="panel" id="catalog">
        <div className="section-header">
          <div>
            <h2>Data Catalog</h2>
            <div className="panel-intro">
              Explore unit pricing, delivery mode, and quota limits before launching Stripe checkout.
            </div>
          </div>
        </div>
        <div className="catalog-grid">
          {products.map((product) => (
            <ProductCard
              key={product.id}
              product={product}
              quantity={quantities[product.id] ?? 1}
              onQuantityChange={(productId, quantity) =>
                setQuantities((current) => ({ ...current, [productId]: quantity }))
              }
              onCheckout={handleCheckout}
              onUsageAction={profile ? handleUsage : undefined}
              disabled={busy}
            />
          ))}
        </div>
      </section>

      <section className="panel" id="market-data">
        <div className="section-header">
          <div>
            <h2>Market Data Preview</h2>
            <div className="panel-intro">
              Preview-only market-data cards are currently served from a stub runtime while the team focuses on shop
              flows, Stripe, and entitlement UX.
            </div>
          </div>
        </div>
        {marketDataRuntime ? (
          <div className="status warn">
            {marketDataRuntime.message}
          </div>
        ) : null}
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
                <div className="panel-intro">Control catalog and market data inputs, then audit operational activity.</div>
              </div>
            </div>
            <div className="stats-grid">
              <StatCard label="Users" value={dashboard?.totalUsers ?? 0} />
              <StatCard label="Products" value={dashboard?.totalProducts ?? 0} />
              <StatCard label="Payments" value={dashboard?.totalPayments ?? 0} />
              <StatCard label="API keys" value={dashboard?.totalApiKeys ?? 0} />
              <StatCard label="Usage events" value={dashboard?.totalUsageRecords ?? 0} />
              <StatCard label="Active entitlements" value={dashboard?.activeEntitlements ?? 0} />
            </div>
          </div>

          <div className="columns">
            <div className="panel">
              <h3>Create catalog product</h3>
              <div className="form-grid">
                <div className="form-row">
                  <Field label="Code" value={productForm.code} onChange={(value) => setProductForm({ ...productForm, code: value })} />
                  <Field label="Name" value={productForm.name} onChange={(value) => setProductForm({ ...productForm, name: value })} />
                </div>
                <Field label="Description" value={productForm.description} onChange={(value) => setProductForm({ ...productForm, description: value })} textarea />
                <div className="form-row">
                  <Field label="Price" value={productForm.price} onChange={(value) => setProductForm({ ...productForm, price: value })} />
                  <Field label="Currency" value={productForm.currency} onChange={(value) => setProductForm({ ...productForm, currency: value })} />
                </div>
                <div className="form-row">
                  <SelectField
                    label="Access type"
                    value={productForm.accessType}
                    onChange={(value) => setProductForm({ ...productForm, accessType: value as AdminProductForm["accessType"] })}
                    options={["ONE_TIME", "SUBSCRIPTION"]}
                  />
                  <SelectField
                    label="Billing interval"
                    value={productForm.billingInterval}
                    onChange={(value) => setProductForm({ ...productForm, billingInterval: value as AdminProductForm["billingInterval"] })}
                    options={["ONE_TIME", "MONTHLY", "YEARLY"]}
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
                <button className="button" onClick={handleCreateProduct} disabled={busy}>
                  Publish product
                </button>
              </div>
            </div>

            <div className="panel">
              <h3>Publish preview market data</h3>
              <div className="form-grid">
                <div className="form-row">
                  <Field label="Symbol" value={marketDataForm.symbol} onChange={(value) => setMarketDataForm({ ...marketDataForm, symbol: value })} />
                  <SelectField
                    label="Data type"
                    value={marketDataForm.dataType}
                    onChange={(value) => setMarketDataForm({ ...marketDataForm, dataType: value as MarketDataForm["dataType"] })}
                    options={["TICK", "NEWS", "FUNDAMENTALS", "CRYPTO", "OTHER"]}
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

          <div className="table-card">
            <div className="header-bar">
              <div>
                <h3>Recent usage activity</h3>
                <div className="helper">Audit stream and batch consumption linked to entitlements.</div>
              </div>
            </div>
            <div className="activity-list">
              {dashboard?.recentUsage.map((usage) => (
                <div className="activity-card" key={usage.id}>
                  <strong>{usage.productCode}</strong>
                  <div className="meta-list">
                    <span>{usage.userEmail}</span>
                    <span>{usage.usageType}</span>
                    <span>Requests: {usage.requestCount}</span>
                    <span>MB: {usage.megabytesUsed}</span>
                    <span>Occurred: {formatDate(usage.occurredAt)}</span>
                  </div>
                </div>
              ))}
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
              <span>Product: {lastTransaction.product.name}</span>
              <span>Amount: {formatMoney(lastTransaction.amount, lastTransaction.currency)}</span>
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

function getErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  return "Unexpected request failure.";
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
  type = "text"
}: {
  label: string;
  value: string;
  onChange: (value: string) => void;
  textarea?: boolean;
  type?: string;
}) {
  return (
    <div className="field">
      <label>{label}</label>
      {textarea ? (
        <textarea value={value} onChange={(event) => onChange(event.target.value)} />
      ) : (
        <input type={type} value={value} onChange={(event) => onChange(event.target.value)} />
      )}
    </div>
  );
}

function SelectField({
  label,
  value,
  options,
  onChange
}: {
  label: string;
  value: string;
  options: string[];
  onChange: (value: string) => void;
}) {
  return (
    <div className="field">
      <label>{label}</label>
      <select value={value} onChange={(event) => onChange(event.target.value)}>
        {options.map((option) => (
          <option key={option} value={option}>
            {option}
          </option>
        ))}
      </select>
    </div>
  );
}
