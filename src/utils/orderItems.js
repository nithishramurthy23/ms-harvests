// src/utils/orderItems.js
import { categoryProducts } from "../data/products";

const productIds = new Set(
  Object.values(categoryProducts)
    .flat()
    .map((p) => p.id)
);

// Turn cart items into the backend's order items. A bundle becomes one line per
// product it contains (same as how its price is calculated in bundleUtils), and
// repeated products are merged.
export const toOrderItems = (cartItems) => {
  const quantities = new Map();
  const add = (productId, quantity) =>
    quantities.set(productId, (quantities.get(productId) || 0) + quantity);

  for (const item of cartItems) {
    if (Array.isArray(item.bundleItems)) {
      item.bundleItems
        .filter((id) => productIds.has(id))
        .forEach((id) => add(id, item.quantity));
    } else {
      add(item.id, item.quantity);
    }
  }

  return [...quantities].map(([productId, quantity]) => ({ productId, quantity }));
};
