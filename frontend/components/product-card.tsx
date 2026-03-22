"use client";

import { DataProduct } from "../lib/types";
import { describeProductMode, formatMoney } from "../lib/format";

type ProductCardProps = {
  product: DataProduct;
  quantity: number;
  cartQuantity?: number;
  onQuantityChange: (productId: number, quantity: number) => void;
  onAddToCart: (product: DataProduct) => void;
  onRemoveFromCart?: (productId: number) => void;
  onUsageAction?: (product: DataProduct) => void;
  disabled?: boolean;
};

export function ProductCard({
  product,
  quantity,
  cartQuantity = 0,
  onQuantityChange,
  onAddToCart,
  onRemoveFromCart,
  onUsageAction,
  disabled
}: ProductCardProps) {
  const quotedUnitPrice = Number(product.quotedPrice ?? product.price);
  const total = quotedUnitPrice * quantity;
  const actionLabel =
    product.accessType === "SUBSCRIPTION" ? "Simulate stream delivery" : "Simulate dataset delivery";
  const isInCart = cartQuantity > 0;
  const addButtonLabel = isInCart ? "Update cart" : "Add to cart";

  return (
    <article className="product-card">
      <div className="product-card-header">
        <div>
          <div className="pill-row">
            <span className="pill">{product.accessType}</span>
            <span className="pill">{product.billingInterval}</span>
            {isInCart ? <span className="pill cart-pill">In cart: {cartQuantity}</span> : null}
          </div>
          <strong>{product.name}</strong>
          <div className="helper">{product.code}</div>
        </div>
        <div className="product-price-block">
          <div className="product-price">{formatMoney(quotedUnitPrice, product.currency)}</div>
          <div className="product-price-caption">
            {product.quotedPrice != null ? "dynamic quote for current selection" : "per commercial unit"}
          </div>
        </div>
      </div>
      <div className="product-meta">{describeProductMode(product)}</div>
      <div className="meta-list">
        <span>{product.description || "No description provided."}</span>
        <span>Selection: {product.quotedSelectionSummary || "Default catalog selection"}</span>
        <span>{product.quotedPricingSummary || `Base price: ${formatMoney(product.price, product.currency)}`}</span>
        <span>Batch limit: {product.batchDownloadLimitMb ?? "Unlimited"} MB</span>
        <span>Realtime channels: {product.realtimeSubscriptionLimit ?? "Unlimited"}</span>
        <span>Payload cap: {product.maxRealtimePayloadKb ?? "Unlimited"} KB</span>
      </div>
      <div className="form-row product-card-controls">
        <div className="field">
          <label htmlFor={`quantity-${product.id}`}>License units</label>
          <input
            id={`quantity-${product.id}`}
            min={1}
            type="number"
            value={quantity}
            onChange={(event) => onQuantityChange(product.id, Math.max(1, Number(event.target.value) || 1))}
          />
        </div>
        <div className="field">
          <label>Estimated total</label>
          <input value={formatMoney(total, product.currency)} readOnly />
        </div>
      </div>
      <div className="actions">
        <button className="button" onClick={() => onAddToCart(product)} disabled={disabled}>
          {addButtonLabel}
        </button>
        {onRemoveFromCart && isInCart ? (
          <button className="ghost-button" onClick={() => onRemoveFromCart(product.id)} disabled={disabled}>
            Remove from cart
          </button>
        ) : null}
        {onUsageAction ? (
          <button className="ghost-button" onClick={() => onUsageAction(product)} disabled={disabled}>
            {actionLabel}
          </button>
        ) : null}
      </div>
    </article>
  );
}
