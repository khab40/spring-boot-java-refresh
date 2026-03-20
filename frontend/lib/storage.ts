import { SessionState } from "./types";

const SESSION_KEY = "mdl-session";
const PENDING_CHECKOUT_KEY = "mdl-pending-checkout";

export function loadSession(): SessionState | null {
  if (typeof window === "undefined") {
    return null;
  }

  const raw = window.localStorage.getItem(SESSION_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as SessionState;
  } catch {
    return null;
  }
}

export function saveSession(session: SessionState | null) {
  if (typeof window === "undefined") {
    return;
  }

  if (!session) {
    window.localStorage.removeItem(SESSION_KEY);
    return;
  }

  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session));
}

export function loadPendingCheckoutId(): number | null {
  if (typeof window === "undefined") {
    return null;
  }

  const raw = window.localStorage.getItem(PENDING_CHECKOUT_KEY);
  if (!raw) {
    return null;
  }

  const parsed = Number(raw);
  return Number.isFinite(parsed) ? parsed : null;
}

export function savePendingCheckoutId(paymentId: number | null) {
  if (typeof window === "undefined") {
    return;
  }

  if (paymentId == null) {
    window.localStorage.removeItem(PENDING_CHECKOUT_KEY);
    return;
  }

  window.localStorage.setItem(PENDING_CHECKOUT_KEY, String(paymentId));
}
