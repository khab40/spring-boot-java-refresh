import { DataProduct } from "./types";

export function formatMoney(value: number | string, currency: string) {
  return new Intl.NumberFormat("en-US", {
    style: "currency",
    currency: (currency || "USD").toUpperCase()
  }).format(Number(value));
}

export function formatDate(value?: string | null) {
  if (!value) {
    return "Not available";
  }

  return new Intl.DateTimeFormat("en-US", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
}

export function describeProductMode(product: DataProduct) {
  if (product.accessType === "SUBSCRIPTION") {
    return product.billingInterval === "YEARLY"
      ? "Yearly stream subscription"
      : "Recurring live subscription";
  }

  return "One-time dataset purchase";
}

export function describeStorage(storageSystem: string) {
  return storageSystem
    .split("_")
    .map((token) => token.charAt(0) + token.slice(1).toLowerCase())
    .join(" ");
}
