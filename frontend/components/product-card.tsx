"use client";

import { DataProduct } from "../lib/types";
import { describeProductMode, formatMoney } from "../lib/format";

type ProductCardProps = {
  product: DataProduct;
  quantity: number;
  onQuantityChange: (productId: number, quantity: number) => void;
  onCheckout: (product: DataProduct) => void;
  onUsageAction?: (product: DataProduct) => void;
  disabled?: boolean;
};

export function ProductCard({
  product,
  quantity,
  onQuantityChange,
  onCheckout,
  onUsageAction,
  disabled
}: ProductCardProps) {
  const total = Number(product.price) * quantity;
  const actionLabel = product.accessType === "SUBSCRIPTION" ? "Record stream usage" : "Record download usage";

  return (
    <article className="product-card">
      <div className="pill-row">
        <span className="pill">{product.accessType}</span>
        <span className="pill">{product.billingInterval}</span>
      </div>
      <strong>{product.name}</strong>
      <div className="helper">{product.code}</div>
      <div className="product-price">{formatMoney(product.price, product.currency)}</div>
      <div className="product-meta">{describeProductMode(product)}</div>
      <div className="meta-list">
        <span>{product.description || "No description provided."}</span>
        <span>Batch limit: {product.batchDownloadLimitMb ?? "Unlimited"} MB</span>
        <span>Realtime channels: {product.realtimeSubscriptionLimit ?? "Unlimited"}</span>
        <span>Payload cap: {product.maxRealtimePayloadKb ?? "Unlimited"} KB</span>
      </div>
      <div className="form-row">
        <div className="field">
          <label htmlFor={`quantity-${product.id}`}>Volume units</label>
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
        <button className="button" onClick={() => onCheckout(product)} disabled={disabled}>
          Checkout
        </button>
        {onUsageAction ? (
          <button className="ghost-button" onClick={() => onUsageAction(product)} disabled={disabled}>
            {actionLabel}
          </button>
        ) : null}
      </div>
    </article>
  );
}
