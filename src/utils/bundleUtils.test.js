import { describe, expect, it, vi } from "vitest";
import {
  calculateBundlePrice,
  calculateBundleWeight,
  getDiscountedPrice,
} from "./bundleUtils";

vi.mock("../data/products", () => ({
  categoryProducts: {
    fruits: [
      { id: 101, price: 199, weight: "1 kg", category: "fruits" },
      { id: 102, price: 60, weight: "500g", category: "fruits" },
    ],
    dairy: [
      { id: 701, price: 70, weight: "1 L", category: "dairy-products" },
      { id: 702, price: 40, weight: "500 ml", category: "dairy-products" },
    ],
    eggs: [{ id: 801, price: 90, weight: "6 pcs", category: "eggs" }],
  },
}));

describe("getDiscountedPrice", () => {
  it("applies the discount only to the matching category", () => {
    const plums = { price: 199, category: "fruits" };

    expect(getDiscountedPrice(plums, "fruits", 10)).toBe(179);
    expect(getDiscountedPrice(plums, "vegetables", 10)).toBe(199);
  });

  it("ignores the category's letter case", () => {
    expect(getDiscountedPrice({ price: 100, category: "Fruits" }, "fruits", 15)).toBe(85);
  });
});

describe("calculateBundlePrice", () => {
  it("adds up the products in the bundle", () => {
    expect(calculateBundlePrice([101, 102])).toBe(259);
  });

  it("discounts only the items in today's category", () => {
    // 10% off fruits: 179 + 54, milk stays 70
    expect(calculateBundlePrice([101, 102, 701], "fruits", 10)).toBe(303);
  });

  it("skips unknown product ids", () => {
    expect(calculateBundlePrice([101, 12345])).toBe(199);
  });
});

describe("calculateBundleWeight", () => {
  it("adds grams and kilograms", () => {
    expect(calculateBundleWeight([101, 102])).toBe("1.5 kg");
    expect(calculateBundleWeight([102])).toBe("500 g");
  });

  it("adds litres and millilitres", () => {
    expect(calculateBundleWeight([701, 702])).toBe("1.5 L");
    expect(calculateBundleWeight([702])).toBe("500 ml");
  });

  it("counts pieces when any item is sold by the piece", () => {
    expect(calculateBundleWeight([801, 101])).toBe("2 pcs");
  });
});
