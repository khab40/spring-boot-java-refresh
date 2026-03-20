import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { ProductCard } from "./product-card";

describe("ProductCard", () => {
  it("renders product details and emits add-to-cart actions", () => {
    const onAddToCart = vi.fn();
    const onQuantityChange = vi.fn();
    const onRemoveFromCart = vi.fn();

    render(
      <ProductCard
        product={{
          id: 42,
          catalogItemId: 8,
          code: "OPTIONS-PRO",
          name: "Options Pro",
          description: "Professional options package",
          price: 79.99,
          currency: "usd",
          accessType: "ONE_TIME_PURCHASE",
          billingInterval: "ONE_TIME",
          batchDownloadLimitMb: 500,
          realtimeSubscriptionLimit: 0,
          maxRealtimePayloadKb: 0
        }}
        quantity={2}
        cartQuantity={2}
        onQuantityChange={onQuantityChange}
        onAddToCart={onAddToCart}
        onRemoveFromCart={onRemoveFromCart}
      />
    );

    expect(screen.getByText("Options Pro")).toBeInTheDocument();
    expect(screen.getByDisplayValue("2")).toBeInTheDocument();
    expect(screen.getByText("In cart: 2")).toBeInTheDocument();

    fireEvent.click(screen.getByText("Update cart"));
    expect(onAddToCart).toHaveBeenCalledTimes(1);

    fireEvent.click(screen.getByText("Remove from cart"));
    expect(onRemoveFromCart).toHaveBeenCalledWith(42);
  });
});
