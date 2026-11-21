import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { beforeEach, describe, expect, it, vi } from "vitest";

// Who is logged in, per test
let currentUser = null;
vi.mock("../../context/AuthContext", () => ({
  useAuth: () => ({ user: currentUser, loading: false, isAuthenticated: !!currentUser }),
}));

vi.mock("./adminApi", async (importOriginal) => {
  const actual = await importOriginal();
  return {
    ...actual,
    adminApi: {
      products: vi.fn(),
      categories: vi.fn(),
      changeStock: vi.fn(),
      stockHistory: vi.fn(),
      setActive: vi.fn(),
      orders: vi.fn(),
      setOrderStatus: vi.fn(),
    },
  };
});

import { adminApi } from "./adminApi";
import AdminLayout from "./AdminLayout";
import AdminProductsPage from "./AdminProductsPage";
import AdminOrdersPage from "./AdminOrdersPage";

const kiwi = {
  id: 7, name: "Kiwi", categoryId: 1, categoryName: "Fruits", price: 149, weight: "500 g",
  imageUrl: null, description: null, stock: 3, active: true,
};

const renderAdmin = (page, path = "/admin/products") =>
  render(
    <MemoryRouter initialEntries={[path]} future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <Routes>
        <Route path="/admin" element={<AdminLayout />}>
          <Route path="products" element={page} />
          <Route path="orders" element={page} />
        </Route>
        <Route path="/auth" element={<p>Login page</p>} />
      </Routes>
    </MemoryRouter>
  );

beforeEach(() => {
  vi.clearAllMocks();
  adminApi.products.mockResolvedValue([kiwi]);
  adminApi.categories.mockResolvedValue([{ id: 1, name: "Fruits", slug: "fruits" }]);
});

describe("store dashboard access", () => {
  it("keeps users and members out", () => {
    for (const role of ["USER", "MEMBER"]) {
      currentUser = { id: 1, name: "Asha", role };
      const { unmount } = renderAdmin(<AdminProductsPage />);
      expect(screen.getByRole("heading", { name: "Staff only" })).toBeInTheDocument();
      expect(adminApi.products).not.toHaveBeenCalled();
      unmount();
    }
  });

  it("sends logged-out visitors to log in", () => {
    currentUser = null;
    renderAdmin(<AdminProductsPage />);
    expect(screen.getByText("Login page")).toBeInTheDocument();
  });

  it("shows the owner-only tabs to the owner but not to admins", async () => {
    currentUser = { id: 1, name: "Admin", role: "ADMIN" };
    const { unmount } = renderAdmin(<AdminProductsPage />);
    await screen.findByText("Kiwi");
    expect(screen.queryByRole("link", { name: "People" })).not.toBeInTheDocument();
    unmount();

    currentUser = { id: 2, name: "Owner", role: "OWNER" };
    renderAdmin(<AdminProductsPage />);
    await screen.findByText("Kiwi");
    expect(screen.getByRole("link", { name: "People" })).toBeInTheDocument();
    expect(screen.getByRole("link", { name: "Overview" })).toBeInTheDocument();
  });
});

describe("products and stock", () => {
  beforeEach(() => {
    currentUser = { id: 1, name: "Admin", role: "ADMIN" };
  });

  it("restocks a product and shows the new count", async () => {
    const user = userEvent.setup();
    adminApi.changeStock.mockResolvedValue({ ...kiwi, stock: 13 });
    renderAdmin(<AdminProductsPage />);

    expect(await screen.findByText("3 in stock")).toBeInTheDocument();
    await user.type(screen.getByLabelText("Units to add or remove for Kiwi"), "10");
    await user.type(screen.getByLabelText("Reason for Kiwi stock change"), "Harvest");
    await user.click(screen.getByRole("button", { name: "Add" }));

    expect(adminApi.changeStock).toHaveBeenCalledWith(7, 10, "Harvest");
    expect(await screen.findByText("13 in stock")).toBeInTheDocument();
  });

  it("shows the server's message when removing more than there is", async () => {
    const user = userEvent.setup();
    adminApi.changeStock.mockRejectedValue({ response: { data: { message: "Only 3 in stock, can't remove 5." } } });
    renderAdmin(<AdminProductsPage />);

    await user.type(await screen.findByLabelText("Units to add or remove for Kiwi"), "5");
    await user.click(screen.getByRole("button", { name: "Remove" }));

    expect(adminApi.changeStock).toHaveBeenCalledWith(7, -5, "");
    expect(await screen.findByText("Only 3 in stock, can't remove 5.")).toBeInTheDocument();
  });

  it("marks sold-out products", async () => {
    adminApi.products.mockResolvedValue([{ ...kiwi, stock: 0 }]);
    renderAdmin(<AdminProductsPage />);
    expect(await screen.findByText("Sold out")).toBeInTheDocument();
  });
});

describe("orders", () => {
  beforeEach(() => {
    currentUser = { id: 1, name: "Admin", role: "ADMIN" };
  });

  it("moves a new order to packing", async () => {
    const user = userEvent.setup();
    const order = {
      id: 41, customerName: "Asha", customerEmail: "asha@example.com", orderDate: "2026-11-21T10:00:00",
      status: "PENDING", nextStatuses: ["PROCESSING", "CANCELLED"], totalAmount: 298, paymentMethod: "cod",
      shippingAddress: "12 MG Road", items: [{ id: 1, quantity: 2, productName: "Kiwi", productWeight: "500 g" }],
    };
    adminApi.orders.mockResolvedValue([order]);
    adminApi.setOrderStatus.mockResolvedValue({ ...order, status: "PROCESSING", nextStatuses: ["SHIPPED", "CANCELLED"] });
    renderAdmin(<AdminOrdersPage />, "/admin/orders");

    const card = (await screen.findByText(/Order #41/)).closest("li");
    expect(within(card).getByText("2 × Kiwi (500 g)")).toBeInTheDocument();
    await user.click(within(card).getByRole("button", { name: "Start packing" }));

    expect(adminApi.setOrderStatus).toHaveBeenCalledWith(41, "PROCESSING");
    // the "New" filter is on, so the order leaves the list
    expect(await screen.findByText("No new orders.")).toBeInTheDocument();
  });
});
