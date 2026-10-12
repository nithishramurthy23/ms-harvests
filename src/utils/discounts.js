// utils/discounts.js

// Get today's discount rule
export const getTodayDiscount = () => {
  const day = new Date().toLocaleDateString("en-US", { weekday: "long" });

  const discounts = {
    Monday: { category: "vegetables", percent: 15 },
    Tuesday: { category: "fruits", percent: 10 },
    Wednesday: { category: "dairy-products", percent: 12 },
    Thursday: { category: "herbs-&-spices", percent: 20 },
    Friday: { category: "honey-&-jams", percent: 18 },
  };

  return discounts[day] || null;
};

// Apply discount to a single product if applicable
export const applyDiscount = (product) => {
  const todayDiscount = getTodayDiscount();

  if (!todayDiscount) return product;

  const normalizedCategory = product.category?.toLowerCase?.();

  if (normalizedCategory === todayDiscount.category) {
    const discountAmount = (product.price * todayDiscount.percent) / 100;
    const discountedPrice = Math.round(product.price - discountAmount);
    return {
      ...product,
      offerPrice: discountedPrice,
    };
  }

  return product;
};

// Utility to apply discount to a list of products
export const applyDiscountToList = (products) => {
  return products.map(applyDiscount);
};
