import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

const auth = { token: "", isAuthenticated: false, logout: vi.fn(), user: null };

vi.mock("../api/axiosInstance", () => ({ default: { get: vi.fn(), post: vi.fn() } }));
vi.mock("../context/AuthContext", () => ({ useAuth: () => auth }));
vi.mock("../context/CartContext", () => ({ useCart: () => ({ dispatch: vi.fn(), openCart: vi.fn() }) }));
vi.mock("../context/CatalogContext", () => ({ useCatalog: () => ({ findItem: () => null, reload: vi.fn() }) }));

import api from "../api/axiosInstance";
import OrderHistoryPage from "./OrderHistoryPage";

const order = {
  id: 7,
  status: "DELIVERED",
  paymentMethod: "cod",
  paymentStatus: "PAY_ON_DELIVERY",
  orderDate: "2026-11-12T12:00:00",
  totalAmount: 98,
  shippingAddress: "14 Lake View Road",
  orderItems: [{ id: 1, productId: 102, productName: "Organic Bananas", productPrice: 49, quantity: 2 }],
};

const page = () => (
  <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
    <OrderHistoryPage />
  </MemoryRouter>
);

beforeEach(() => {
  Object.assign(auth, { token: "", isAuthenticated: false });
  api.get.mockReset();
  api.get.mockResolvedValue({ data: [order] });
});

describe("OrderHistoryPage", () => {
  it("shows the orders once a reloaded page has restored the login", async () => {
    // On a reload the page renders before the saved session is restored.
    const { rerender } = render(page());
    expect(screen.getByText("You are not logged in.")).toBeInTheDocument();

    Object.assign(auth, { token: "abc", isAuthenticated: true });
    rerender(page());

    expect(await screen.findByText(/Order #7/)).toBeInTheDocument();
    expect(screen.queryByText("Error Loading Orders")).not.toBeInTheDocument();
    expect(api.get).toHaveBeenCalledWith("/orders/history");
  });

  it("asks a logged-out visitor to log in, without calling the server", () => {
    render(page());

    expect(screen.getByText("You are not logged in.")).toBeInTheDocument();
    expect(api.get).not.toHaveBeenCalled();
  });
});
