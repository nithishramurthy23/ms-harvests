// One place for the shop's catalogue: categories, products, baskets and
// today's prices, so every page shows the same numbers.
import { Apple, Carrot, Cherry, Egg, Leaf, Milk } from "lucide-react";
import { categoryProducts } from "./products";
import { bundles } from "./bundles";
import { getTodayDiscount } from "../utils/discounts";
import { calculateBundlePrice } from "../utils/bundleUtils";

export const categories = [
  {
    slug: "vegetables",
    name: "Vegetables",
    icon: Carrot,
    tint: "bg-green-100 text-green-700",
    description: "Hill-grown potatoes, carrots, beetroot and greens.",
    about:
      "Grown in the cool hill climate around Ooty and packed when you order.",
    tips: ["Keep roots in a cool, dark place, not the fridge.", "Wrap greens in a damp cloth and use within 3 days."],
  },
  {
    slug: "fruits",
    name: "Fruits",
    icon: Apple,
    tint: "bg-clay-100 text-clay-600",
    description: "Plums, oranges and berries from the Nilgiris.",
    about:
      "From orchards around Ooty. Hill fruit ripens slowly, so colour and size can vary a little from piece to piece.",
    tips: ["Leave plums out a day to soften.", "Keep strawberries unwashed in the fridge until you eat them."],
  },
  {
    slug: "herbs-&-spices",
    name: "Herbs",
    icon: Leaf,
    tint: "bg-green-50 text-green-600",
    description: "Coriander, curry leaves and mint by the bunch.",
    about: "Sold by the bunch and packed with the rest of your order.",
    tips: ["Stand the bunch in a glass of water.", "Curry leaves keep for weeks once dried in the shade."],
  },
  {
    slug: "dairy-products",
    name: "Dairy",
    icon: Milk,
    tint: "bg-mustard-100 text-bark",
    description: "Milk, curd, paneer, ghee and butter.",
    about: "Milk, curd and butter from dairies near Ooty, kept chilled until your order leaves us.",
    tips: ["Keep refrigerated.", "Boil the milk before drinking."],
  },
  {
    slug: "eggs",
    name: "Eggs",
    icon: Egg,
    tint: "bg-mustard-100 text-clay-600",
    description: "Farm, organic, duck and brown eggs.",
    about: "Shell colour and size vary from box to box, the way they do on a farm.",
    tips: ["Store in the fridge, pointed end down."],
  },
  {
    slug: "honey-&-jams",
    name: "Honey & Jams",
    icon: Cherry,
    tint: "bg-clay-100 text-clay-500",
    description: "Raw honey and small-batch fruit jams.",
    about: "Raw honey and fruit jams, made in small batches.",
    tips: ["Raw honey may crystallise. Warm the jar in water to loosen it.", "Refrigerate jam after opening."],
  },
];

export const allProducts = Object.values(categoryProducts).flat();

export const getCategory = (slug) => categories.find((c) => c.slug === slug);

export const categoryName = (slug) => getCategory(slug)?.name ?? slug;

// Today's price for a single product, with the day's category offer applied.
export const withTodayPrice = (product) => {
  const offer = getTodayDiscount();
  if (offer && product.category === offer.category) {
    const offerPrice = Math.round(product.price - (product.price * offer.percent) / 100);
    return { ...product, mrp: product.price, price: offerPrice, offPercent: offer.percent };
  }
  return product;
};

// A basket priced from its products, with the full (pre-offer) price kept as mrp.
export const withBundlePrice = (bundle) => {
  const offer = getTodayDiscount();
  const mrp = calculateBundlePrice(bundle.bundleItems);
  const price = calculateBundlePrice(bundle.bundleItems, offer?.category, offer?.percent || 0);
  return {
    ...bundle,
    isBundle: true,
    weight: `${bundle.bundleItems.length} items`,
    price,
    ...(price < mrp ? { mrp } : {}),
  };
};

export const pricedProducts = () => allProducts.map(withTodayPrice);
export const pricedBundles = () => bundles.map(withBundlePrice);

export const findItem = (id) => {
  const bundle = bundles.find((b) => b.id === id);
  if (bundle) return withBundlePrice(bundle);
  const product = allProducts.find((p) => p.id === id);
  return product ? withTodayPrice(product) : null;
};
