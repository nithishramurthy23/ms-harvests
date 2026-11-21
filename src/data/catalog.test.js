import { describe, expect, it } from "vitest";
import { buildCatalog, priceBundle } from "./catalog";

const plums = { id: 101, name: "Plums", price: 179, mrp: 199, offPercent: 10, category: "fruits", stock: 5, inStock: true };
const carrots = { id: 201, name: "Carrots", price: 40, category: "vegetables", stock: 0, inStock: false };
const bananas = { id: 102, name: "Bananas", price: 49, category: "fruits", stock: 12, inStock: true };

describe("priceBundle", () => {
  const byId = new Map([plums, carrots, bananas].map((p) => [p.id, p]));

  it("prices a basket as the sum of today's prices and keeps the full price", () => {
    const basket = priceBundle({ id: 1, bundleItems: [101, 102] }, byId);

    expect(basket.price).toBe(228);
    expect(basket.mrp).toBe(248);
    expect(basket.stock).toBe(5);
    expect(basket.inStock).toBe(true);
  });

  it("is out of stock when anything inside is sold out", () => {
    const basket = priceBundle({ id: 2, bundleItems: [102, 201] }, byId);

    expect(basket.inStock).toBe(false);
    expect(basket.mrp).toBeUndefined();
  });

  it("is out of stock when an item is no longer sold", () => {
    expect(priceBundle({ id: 3, bundleItems: [102, 999] }, byId).inStock).toBe(false);
  });
});

describe("buildCatalog", () => {
  it("finds products and baskets, and knows their stock", () => {
    const catalog = buildCatalog([plums, carrots], { category: "fruits", percent: 10 }, [
      { id: 1, bundleItems: [101] },
    ]);

    expect(catalog.findItem(101).name).toBe("Plums");
    expect(catalog.findItem(1).isBundle).toBe(true);
    expect(catalog.stockOf(201)).toBe(0);
    expect(catalog.stockOf(555)).toBe(Infinity);
    expect(catalog.offer).toEqual({ category: "fruits", percent: 10 });
    expect(catalog.productIds.has(201)).toBe(true);
  });
});
