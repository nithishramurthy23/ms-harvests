import { act, renderHook } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";
import { CartProvider, useCart } from "./CartContext";

const plums = { id: 101, name: "Ooty Plums", price: 199 };
const bananas = { id: 102, name: "Organic Bananas", price: 49 };

const renderCart = () => renderHook(() => useCart(), { wrapper: CartProvider });

describe("CartContext", () => {
  it("adds items and keeps the total in sync", () => {
    const { result } = renderCart();

    act(() => {
      result.current.dispatch({ type: "ADD_ITEM", payload: plums });
      result.current.dispatch({ type: "ADD_ITEM", payload: bananas });
      result.current.dispatch({ type: "ADD_ITEM", payload: plums });
    });

    expect(result.current.state.items).toHaveLength(2);
    expect(result.current.state.items[0].quantity).toBe(2);
    expect(result.current.state.total).toBe(199 * 2 + 49);
  });

  it("updates the total when the quantity changes", () => {
    const { result } = renderCart();

    act(() => {
      result.current.dispatch({ type: "ADD_ITEM", payload: bananas });
      result.current.dispatch({ type: "UPDATE_QUANTITY", payload: { id: 102, quantity: 4 } });
    });

    expect(result.current.state.total).toBe(196);

    act(() => {
      result.current.dispatch({ type: "UPDATE_QUANTITY", payload: { id: 102, quantity: 1 } });
    });

    expect(result.current.state.total).toBe(49);
  });

  it("removes an item with its whole quantity", () => {
    const { result } = renderCart();

    act(() => {
      result.current.dispatch({ type: "ADD_ITEM", payload: plums });
      result.current.dispatch({ type: "UPDATE_QUANTITY", payload: { id: 101, quantity: 3 } });
      result.current.dispatch({ type: "ADD_ITEM", payload: bananas });
      result.current.dispatch({ type: "REMOVE_ITEM", payload: 101 });
    });

    expect(result.current.state.items.map((item) => item.id)).toEqual([102]);
    expect(result.current.state.total).toBe(49);
  });

  it("ignores updates for items that are not in the cart", () => {
    const { result } = renderCart();
    const before = result.current.state;

    act(() => {
      result.current.dispatch({ type: "UPDATE_QUANTITY", payload: { id: 999, quantity: 2 } });
    });

    expect(result.current.state).toBe(before);
  });

  it("empties the cart", () => {
    const { result } = renderCart();

    act(() => {
      result.current.dispatch({ type: "ADD_ITEM", payload: plums });
      result.current.dispatch({ type: "CLEAR_CART" });
    });

    expect(result.current.state).toEqual({ items: [], total: 0 });
  });

  it("throws a clear error when used outside the provider", () => {
    // React logs the thrown error; keep the test output clean
    vi.spyOn(console, "error").mockImplementation(() => {});

    expect(() => renderHook(() => useCart())).toThrow(
      "useCart must be used within a CartProvider"
    );

    console.error.mockRestore();
  });
});

describe("CartContext persistence", () => {
  it("adds the requested quantity in one go", () => {
    const { result } = renderCart();

    act(() => {
      result.current.dispatch({ type: "ADD_ITEM", payload: { ...plums, quantity: 3 } });
    });

    expect(result.current.state.items[0].quantity).toBe(3);
    expect(result.current.state.total).toBe(597);
    expect(result.current.itemCount).toBe(3);
  });

  it("keeps the cart after a reload", () => {
    const first = renderCart();
    act(() => {
      first.result.current.dispatch({ type: "ADD_ITEM", payload: bananas });
    });
    first.unmount();

    const { result } = renderCart();

    expect(result.current.state.items.map((item) => item.id)).toEqual([102]);
    expect(result.current.quantityOf(102)).toBe(1);
  });

  it("starts empty when the saved cart is unreadable", () => {
    localStorage.setItem("cart", "{not json");

    const { result } = renderCart();

    expect(result.current.state).toEqual({ items: [], total: 0 });
  });
});
