import api from "../../api/axiosInstance";

// Thin wrappers over the admin and owner endpoints. Errors keep the backend's
// {message, fields} body so forms can show it.
export const errorMessage = (err, fallback = "Something went wrong. Please try again.") =>
  err?.response?.data?.message || fallback;

export const adminApi = {
  products: () => api.get("/admin/products").then((r) => r.data),
  categories: () => api.get("/admin/categories").then((r) => r.data),
  createProduct: (body) => api.post("/admin/products", body).then((r) => r.data),
  updateProduct: (id, body) => api.put(`/admin/products/${id}`, body).then((r) => r.data),
  setActive: (id, active) => api.patch(`/admin/products/${id}/active`, { active }).then((r) => r.data),
  changeStock: (id, change, reason) => api.post(`/admin/products/${id}/stock`, { change, reason }).then((r) => r.data),
  stockHistory: (id) => api.get(`/admin/products/${id}/stock`).then((r) => r.data),
  orders: (status) => api.get("/admin/orders", { params: status ? { status } : {} }).then((r) => r.data),
  setOrderStatus: (id, status) => api.patch(`/admin/orders/${id}/status`, { status }).then((r) => r.data),
  users: (q) => api.get("/admin/users", { params: q ? { q } : {} }).then((r) => r.data),
  setRole: (id, role) => api.patch(`/admin/users/${id}/role`, { role }).then((r) => r.data),
  roleChanges: () => api.get("/admin/role-changes").then((r) => r.data),
  summary: () => api.get("/owner/summary").then((r) => r.data),
};
