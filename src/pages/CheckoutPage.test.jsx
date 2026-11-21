import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

const auth = { user: null, isAuthenticated: false, token: "" };
const cart = {
  state: { items: [{ id: 102, name: "Organic Bananas", price: 49, quantity: 2 }], total: 98 },
  dispatch: vi.fn(),
};

vi.mock("../api/axiosInstance", () => ({ default: { get: vi.fn(), post: vi.fn() } }));
vi.mock("../context/AuthContext", () => ({ useAuth: () => auth }));
vi.mock("../context/CartContext", () => ({ useCart: () => cart }));
vi.mock("../context/CatalogContext", () => ({ useCatalog: () => ({ productIds: new Set(), reload: vi.fn() }) }));
vi.mock("../utils/payments", () => ({
  newCheckoutKey: () => "key",
  payForOrder: vi.fn(),
  paymentConfig: () => Promise.resolve({ onlineEnabled: false }),
}));

import CheckoutPage from "./CheckoutPage";

const page = () => (
  <MemoryRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
    <CheckoutPage />
  </MemoryRouter>
);
const field = (name) => document.querySelector(`input[name="${name}"]`);

beforeEach(() => {
  Object.assign(auth, { user: null, isAuthenticated: false, token: "" });
});

describe("CheckoutPage", () => {
  it("fills in the customer's details once a reloaded page has restored the login", async () => {
    // On a reload the page renders before the saved session is restored.
    const { rerender } = render(page());
    expect(field("name").value).toBe("");

    Object.assign(auth, {
      user: { name: "Priya Sharma", email: "priya@example.com", address: "14 Lake View Road", phone: "9876543210" },
      isAuthenticated: true,
      token: "abc",
    });
    rerender(page());

    await waitFor(() => expect(field("name").value).toBe("Priya Sharma"));
    expect(field("email").value).toBe("priya@example.com");
    expect(field("address").value).toBe("14 Lake View Road");
    expect(field("phone").value).toBe("9876543210");
    // name and email can't be typed in when logged in, so they must be filled
    expect(field("name").readOnly).toBe(true);
  });

  it("fills the details straight away when the login is already there", async () => {
    auth.user = { name: "Priya Sharma", email: "priya@example.com", address: "Home address", phone: "1" };
    Object.assign(auth, { isAuthenticated: true, token: "abc" });
    render(page());

    expect(await screen.findByDisplayValue("Home address")).toBeInTheDocument();
  });
});
