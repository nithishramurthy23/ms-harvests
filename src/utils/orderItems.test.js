import { describe, expect, it } from "vitest";
import { toOrderItems } from "./orderItems";

describe("toOrderItems", () => {
  it("passes single products through", () => {
    expect(toOrderItems([{ id: 101, quantity: 2 }])).toEqual([
      { productId: 101, quantity: 2 },
    ]);
  });

  it("expands a bundle into one line per product", () => {
    const bundle = { id: 1, bundleItems: [101, 201], quantity: 3 };

    expect(toOrderItems([bundle])).toEqual([
      { productId: 101, quantity: 3 },
      { productId: 201, quantity: 3 },
    ]);
  });

  it("merges a product bought on its own and inside a bundle", () => {
    const cart = [
      { id: 101, quantity: 1 },
      { id: 1, bundleItems: [101, 102], quantity: 2 },
    ];

    expect(toOrderItems(cart)).toEqual([
      { productId: 101, quantity: 3 },
      { productId: 102, quantity: 2 },
    ]);
  });

  it("drops bundle entries that are not real products", () => {
    const bundle = { id: 1, bundleItems: [101, 999], quantity: 1 };

    expect(toOrderItems([bundle], new Set([101, 102]))).toEqual([{ productId: 101, quantity: 1 }]);
  });

  it("returns nothing for an empty cart", () => {
    expect(toOrderItems([])).toEqual([]);
  });
});
