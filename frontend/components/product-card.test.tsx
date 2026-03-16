import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ProductCard } from "./product-card";

describe("ProductCard", () => {
  it("renders product details and emits checkout actions", () => {
    const onCheckout = vi.fn();
    const onQuantityChange = vi.fn();

    render(
      <ProductCard
        product={{
          id: 42,
          code: "OPTIONS-PRO",
          name: "Options Pro",
          description: "Professional options package",
          price: 79.99,
          currency: "usd",
          accessType: "ONE_TIME",
          billingInterval: "ONE_TIME",
          batchDownloadLimitMb: 500,
          realtimeSubscriptionLimit: 0,
          maxRealtimePayloadKb: 0
        }}
        quantity={2}
        onQuantityChange={onQuantityChange}
        onCheckout={onCheckout}
      />
    );

    expect(screen.getByText("Options Pro")).toBeInTheDocument();
    expect(screen.getByDisplayValue("2")).toBeInTheDocument();

    fireEvent.click(screen.getByText("Checkout"));
    expect(onCheckout).toHaveBeenCalledTimes(1);
  });
});
