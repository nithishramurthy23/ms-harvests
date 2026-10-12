import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { CartProvider } from "../context/CartContext";
import ProductsPage from "./ProductsPage";

const renderShop = (url = "/products") =>
  render(
    <MemoryRouter initialEntries={[url]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <CartProvider>
        <Routes>
          <Route path="/products" element={<ProductsPage />} />
        </Routes>
      </CartProvider>
    </MemoryRouter>
  );

describe("ProductsPage", () => {
  it("lists every product, not just the baskets", () => {
    renderShop();

    expect(screen.getByRole("link", { name: "Fresh Carrots" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Raw Honey" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Weekly baskets" })).toBeInTheDocument();
  });

  it("filters by the search in the URL", () => {
    renderShop("/products?q=paneer");

    expect(screen.getByText(/result for/)).toHaveTextContent("1 result for “paneer”");
    expect(screen.queryByRole("link", { name: "Fresh Carrots" })).not.toBeInTheDocument();
  });

  it("filters by aisle", async () => {
    const user = userEvent.setup();
    renderShop();

    await user.click(screen.getByRole("button", { name: "Eggs" }));

    expect(screen.getByRole("link", { name: "Duck Eggs" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Raw Honey" })).not.toBeInTheDocument();
  });

  it("turns Add into a quantity stepper", async () => {
    const user = userEvent.setup();
    renderShop("/products?q=duck");

    await user.click(screen.getByRole("button", { name: "Add Duck Eggs to cart" }));
    await user.click(screen.getByRole("button", { name: "Add one more Duck Eggs" }));

    const card = screen.getByRole("link", { name: "Duck Eggs" }).closest("article");
    expect(within(card).getByText("2")).toBeInTheDocument();
  });
});
