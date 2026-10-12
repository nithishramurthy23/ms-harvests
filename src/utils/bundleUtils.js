// src/utils/bundleUtils.js
import { categoryProducts } from "../data/products";

const allProducts = Object.values(categoryProducts).flat();

export const getDiscountedPrice = (item, discountCategory, discountPercent) => {
  const itemCategory = item.category?.toLowerCase();
  if (itemCategory === discountCategory) {
    return Math.round(item.price - (item.price * discountPercent) / 100);
  }
  return item.price;
};

export const calculateBundlePrice = (
  bundleItems,
  discountCategory = null,
  discountPercent = 0
) => {
  return bundleItems.reduce((total, itemId) => {
    const item = allProducts.find((p) => p.id === itemId);
    return (
      total +
      (item ? getDiscountedPrice(item, discountCategory, discountPercent) : 0)
    );
  }, 0);
};

export const calculateBundleWeight = (bundleItems) => {
  let totalGrams = 0;
  let totalMilliLitres = 0;
  let hasPieces = false;

  bundleItems.forEach((itemId) => {
    const item = allProducts.find((p) => p.id === itemId);
    if (!item || !item.weight) return;

    const weight = item.weight.toLowerCase();

    if (weight.includes("kg")) totalGrams += parseFloat(weight) * 1000;
    else if (weight.includes("g")) totalGrams += parseFloat(weight);
    else if (weight.includes("l"))
      totalMilliLitres += parseFloat(weight) * 1000;
    else if (weight.includes("ml")) totalMilliLitres += parseFloat(weight);
    else if (weight.includes("pcs")) hasPieces = true;
  });

  if (hasPieces) return `${bundleItems.length} pcs`;
  if (totalMilliLitres)
    return totalMilliLitres >= 1000
      ? `${(totalMilliLitres / 1000).toFixed(1)} L`
      : `${totalMilliLitres} ml`;
  if (totalGrams)
    return totalGrams >= 1000
      ? `${(totalGrams / 1000).toFixed(1)} kg`
      : `${totalGrams} g`;
  return "0 g";
};
