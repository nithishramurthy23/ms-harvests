import { afterEach, describe, expect, it, vi } from "vitest";
import { applyDiscount, applyDiscountToList, getTodayDiscount } from "./discounts";

// 2026-10-12 is a Monday, 2026-10-18 a Sunday
const MONDAY = new Date(2026, 9, 12, 10, 0);
const SUNDAY = new Date(2026, 9, 18, 10, 0);

describe("daily discounts", () => {
  afterEach(() => {
    vi.useRealTimers();
  });

  it("has a vegetables offer on Monday", () => {
    vi.useFakeTimers();
    vi.setSystemTime(MONDAY);

    expect(getTodayDiscount()).toEqual({ category: "vegetables", percent: 15 });
  });

  it("has no offer at the weekend", () => {
    vi.useFakeTimers();
    vi.setSystemTime(SUNDAY);

    expect(getTodayDiscount()).toBeNull();
    const carrots = { price: 40, category: "vegetables" };
    expect(applyDiscount(carrots)).toBe(carrots);
  });

  it("adds an offer price to products in today's category", () => {
    vi.useFakeTimers();
    vi.setSystemTime(MONDAY);

    const [carrots, plums] = applyDiscountToList([
      { price: 40, category: "Vegetables" },
      { price: 199, category: "fruits" },
    ]);

    expect(carrots.offerPrice).toBe(34);
    expect(plums.offerPrice).toBeUndefined();
  });
});
