import api from "./axiosInstance";
import { toShopProduct } from "../data/catalog";

// GET /api/products/search. Every parameter is optional; empty ones aren't sent.
// The server applies today's prices, so price filters and sorting match what
// the customer sees.
export async function searchProducts({ q, category, minPrice, maxPrice, inStock, sort, page = 0, size = 24 } = {}, signal) {
  const params = { page, size };
  if (q) params.q = q;
  if (category) params.category = category;
  if (minPrice !== "" && minPrice != null) params.minPrice = minPrice;
  if (maxPrice !== "" && maxPrice != null) params.maxPrice = maxPrice;
  if (inStock) params.inStock = true;
  if (sort) params.sort = sort;

  const { data } = await api.get("/products/search", { params, signal });
  return { ...data, items: data.items.map(toShopProduct) };
}
