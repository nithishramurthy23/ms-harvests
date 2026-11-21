import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";
import { CartProvider } from "../context/CartContext";
import { CatalogProvider } from "../context/CatalogContext";
import { fakeSearch, fixtureProducts, loadFixture } from "../testing/catalogFixture";

vi.mock("../api/search", () => ({ searchProducts: vi.fn() }));

import { searchProducts } from "../api/search";
import ProductsPage from "./ProductsPage";

beforeEach(() => {
  searchProducts.mockReset();
  searchProducts.mockImplementation(fakeSearch(fixtureProducts));
});

const renderShop = async (url = "/products", products = fixtureProducts) => {
  searchProducts.mockImplementation(fakeSearch(products));
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
    expect(screen.getByRole("link", { name: "Fresh Paneer" })).toBeInTheDocument();
    expect(screen.getByText(/Page 1 of/)).toBeInTheDocument(); // the rest is on the next page
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

  it("sends the filters from the URL to the server", async () => {
    searchProducts.mockImplementation(fakeSearch(fixtureProducts));
    render(
      <MemoryRouter
        initialEntries={["/products?q=eggs&aisle=eggs&min=50&max=150&instock=1&sort=price-asc"]}
        future={{ v7_startTransition: true, v7_relativeSplatPath: true }}
      >
        <CatalogProvider load={loadFixture(fixtureProducts)}>
          <CartProvider>
            <ProductsPage />
          </CartProvider>
        </CatalogProvider>
      </MemoryRouter>
    );
    await screen.findAllByRole("article");

    expect(searchProducts).toHaveBeenLastCalledWith(
      { q: "eggs", category: "eggs", minPrice: "50", maxPrice: "150", inStock: true, sort: "price-asc", page: 0, size: 24 },
      expect.anything()
    );
    expect(screen.getByRole("button", { name: "Clear filters" })).toBeInTheDocument();
  });

  it("changes the sort and the in-stock filter", async () => {
    const user = userEvent.setup();
    await renderShop();

    await user.selectOptions(screen.getByLabelText("Sort by"), "price-desc");
    expect(searchProducts).toHaveBeenLastCalledWith(expect.objectContaining({ sort: "price-desc" }), expect.anything());

    await user.click(screen.getByLabelText("In stock only"));
    expect(searchProducts).toHaveBeenLastCalledWith(expect.objectContaining({ inStock: true }), expect.anything());
  });

  it("offers a spelling fix when nothing matches", async () => {
    const user = userEvent.setup();
    render(
      <MemoryRouter initialEntries={["/products?q=paner"]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <CatalogProvider load={loadFixture(fixtureProducts)}>
          <CartProvider>
            <ProductsPage />
          </CartProvider>
        </CatalogProvider>
      </MemoryRouter>
    );

    await user.click(await screen.findByRole("button", { name: "paneer" }));
    expect(await screen.findByRole("link", { name: "Fresh Paneer" })).toBeInTheDocument();
  });

  it("pages through long results", async () => {
    const user = userEvent.setup();
    const many = Array.from({ length: 30 }, (_, i) => ({ ...fixtureProducts[0], id: 5000 + i, name: `Plum ${i + 1}` }));
    await renderShop("/products?q=plum", many);

    expect(screen.getByText("Page 1 of 2")).toBeInTheDocument();
    await user.click(screen.getByRole("button", { name: "Next" }));
    expect(await screen.findByText("Page 2 of 2")).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Plum 30" })).toBeInTheDocument();
  });

  it("explains when the catalogue can't load", async () => {
    searchProducts.mockRejectedValue(new Error("down"));
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
