import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { useEffect } from "react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { CartProvider, useCart } from "../context/CartContext";
import Cart from "./Cart";

const plums = { id: 101, name: "Ooty Plums", price: 199, image: "plums.jpg" };

// Puts items in the cart before the Cart component renders.
function Prefill({ items, children }) {
  const { dispatch } = useCart();
  useEffect(() => {
    items.forEach((item) => dispatch({ type: "ADD_ITEM", payload: item }));
  }, [dispatch, items]);
  return children;
}

function renderCart(items = [], onClose = vi.fn()) {
  render(
    <MemoryRouter
      initialEntries={["/"]}
      future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
    >
      <CartProvider>
        <Prefill items={items}>
          <Routes>
            <Route path="/" element={<Cart onClose={onClose} />} />
            <Route path="/checkout" element={<p>Checkout page</p>} />
          </Routes>
        </Prefill>
      </CartProvider>
    </MemoryRouter>
  );
  return { onClose };
}

describe("Cart", () => {
  it("shows an empty state", () => {
    renderCart();

    expect(screen.getByText("Your cart is empty")).toBeInTheDocument();
  });

  it("changes quantity and total with the + and - buttons", async () => {
    const user = userEvent.setup();
    renderCart([plums]);

    await user.click(screen.getByRole("button", { name: "Increase Ooty Plums" }));

    expect(screen.getByText("2")).toBeInTheDocument();
    expect(screen.getByText("₹398")).toBeInTheDocument();

    await user.click(screen.getByRole("button", { name: "Decrease Ooty Plums" }));
    await user.click(screen.getByRole("button", { name: "Decrease Ooty Plums" }));

    expect(screen.getByText("Your cart is empty")).toBeInTheDocument();
  });

  it("removes an item", async () => {
    const user = userEvent.setup();
    renderCart([plums]);

    await user.click(screen.getByRole("button", { name: "Remove Ooty Plums" }));

    expect(screen.getByText("Your cart is empty")).toBeInTheDocument();
  });

  it("goes to checkout and closes", async () => {
    const user = userEvent.setup();
    const { onClose } = renderCart([plums]);

    await user.click(screen.getByRole("button", { name: "Proceed to Checkout" }));

    expect(screen.getByText("Checkout page")).toBeInTheDocument();
    expect(onClose).toHaveBeenCalled();
  });

  it("closes on Escape", async () => {
    const user = userEvent.setup();
    const { onClose } = renderCart([plums]);

    await user.keyboard("{Escape}");

    expect(onClose).toHaveBeenCalled();
  });
});
