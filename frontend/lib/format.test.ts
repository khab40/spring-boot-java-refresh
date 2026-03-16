import { describe, expect, it } from "vitest";
import { describeProductMode, formatDate, formatMoney } from "./format";

describe("format helpers", () => {
  it("formats money in USD", () => {
    expect(formatMoney(49.99, "usd")).toContain("$49.99");
  });

  it("formats empty dates defensively", () => {
    expect(formatDate()).toBe("Not available");
  });

  it("describes subscription products clearly", () => {
    expect(
      describeProductMode({
        id: 1,
        code: "FX-STREAM",
        name: "FX Stream",
        price: 12,
        currency: "usd",
        accessType: "SUBSCRIPTION",
        billingInterval: "MONTHLY"
      })
    ).toBe("Recurring live subscription");
  });
});
