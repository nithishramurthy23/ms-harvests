// src/utils/orderItems.js

// Turn cart items into the backend's order items. A bundle becomes one line per
// product it contains (the same products its price is the sum of), and
// repeated products are merged. Pass the catalogue's product ids to drop
// basket entries that are no longer sold; the backend rejects unknown ids.
export const toOrderItems = (cartItems, knownProductIds = null) => {
  const quantities = new Map();
  const add = (productId, quantity) =>
    quantities.set(productId, (quantities.get(productId) || 0) + quantity);
  const isKnown = (id) => !knownProductIds || knownProductIds.has(id);

  for (const item of cartItems) {
    if (Array.isArray(item.bundleItems)) {
      item.bundleItems.filter(isKnown).forEach((id) => add(id, item.quantity));
    } else {
      add(item.id, item.quantity);
    }
  }

  return [...quantities].map(([productId, quantity]) => ({ productId, quantity }));
};
