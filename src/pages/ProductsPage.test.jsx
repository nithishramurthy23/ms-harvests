import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it } from "vitest";
import { CartProvider } from "../context/CartContext";
import { CatalogProvider } from "../context/CatalogContext";
import { fixtureProducts, loadFixture } from "../testing/catalogFixture";
import ProductsPage from "./ProductsPage";

const renderShop = async (url = "/products", products = fixtureProducts) => {
  render(
    <MemoryRouter initialEntries={[url]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <CatalogProvider load={loadFixture(products)}>
        <CartProvider>
          <Routes>
            <Route path="/products" element={<ProductsPage />} />
          </Routes>
        </CartProvider>
      </CatalogProvider>
    </MemoryRouter>
  );
  await screen.findByRole("heading", { name: "The shop" });
  await screen.findAllByRole("article");
};

const withStock = (id, stock) =>
  fixtureProducts.map((p) => (p.id === id ? { ...p, stock, inStock: stock > 0 } : p));

describe("ProductsPage", () => {
  it("lists every product, not just the baskets", async () => {
    await renderShop();

    expect(screen.getByRole("link", { name: "Fresh Carrots" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Raw Honey" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Weekly baskets" })).toBeInTheDocument();
  });

  it("filters by the search in the URL", async () => {
    await renderShop("/products?q=paneer");

    expect(screen.getByText(/result for/)).toHaveTextContent("1 result for “paneer”");
    expect(screen.queryByRole("link", { name: "Fresh Carrots" })).not.toBeInTheDocument();
  });

  it("filters by aisle", async () => {
    const user = userEvent.setup();
    await renderShop();

    await user.click(screen.getByRole("button", { name: "Eggs" }));

    expect(screen.getByRole("link", { name: "Duck Eggs" })).toBeInTheDocument();
    expect(screen.queryByRole("link", { name: "Raw Honey" })).not.toBeInTheDocument();
  });

  it("turns Add into a quantity stepper", async () => {
    const user = userEvent.setup();
    await renderShop("/products?q=duck");

    await user.click(screen.getByRole("button", { name: "Add Duck Eggs to cart" }));
    await user.click(screen.getByRole("button", { name: "Add one more Duck Eggs" }));

    const card = screen.getByRole("link", { name: "Duck Eggs" }).closest("article");
    expect(within(card).getByText("2")).toBeInTheDocument();
  });

  it("shows sold-out products as out of stock and won't add them", async () => {
    await renderShop("/products?q=duck", withStock(803, 0));

    const card = screen.getByRole("link", { name: "Duck Eggs" }).closest("article");
    expect(within(card).getAllByText(/out of stock/i).length).toBeGreaterThan(0);
    expect(within(card).getByRole("button", { name: "Duck Eggs is out of stock" })).toBeDisabled();
  });

  it("stops the stepper at the stock that's left", async () => {
    const user = userEvent.setup();
    await renderShop("/products?q=duck", withStock(803, 2));

    const card = screen.getByRole("link", { name: "Duck Eggs" }).closest("article");
    expect(within(card).getByText("Only 2 left")).toBeInTheDocument();

    await user.click(within(card).getByRole("button", { name: "Add Duck Eggs to cart" }));
    await user.click(within(card).getByRole("button", { name: "Add one more Duck Eggs" }));

    expect(within(card).getByText("2")).toBeInTheDocument();
    expect(within(card).getByRole("button", { name: "Add one more Duck Eggs" })).toBeDisabled();
  });

  it("explains when the catalogue can't load", async () => {
    render(
      <MemoryRouter initialEntries={["/products"]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <CatalogProvider load={() => Promise.reject(new Error("down"))}>
          <CartProvider>
            <ProductsPage />
          </CartProvider>
        </CatalogProvider>
      </MemoryRouter>
    );

    expect(await screen.findByText(/couldn.t load the shop/i)).toBeInTheDocument();
  });
});
