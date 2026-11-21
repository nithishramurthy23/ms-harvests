// The shop's catalogue. Aisle text and icons live here; products, prices and
// stock come from the backend (GET /api/products) and are combined with the
// baskets by buildCatalog(), so every page shows the same numbers.
import { Apple, Carrot, Cherry, Egg, Leaf, Milk } from "lucide-react";
import { bundles as basketDefinitions } from "./bundles";

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

export const getCategory = (slug) => categories.find((c) => c.slug === slug);

export const categoryName = (slug) => getCategory(slug)?.name ?? slug;

// The backend sends `image`, `price`, `mrp` (only when discounted), `offPercent`
// and `stock`. Numbers arrive as JSON numbers, but keep them tidy for display.
export const toShopProduct = (p) => ({
  ...p,
  price: Number(p.price),
  mrp: p.mrp == null ? undefined : Number(p.mrp),
  offPercent: p.offPercent == null ? undefined : Number(p.offPercent),
  stock: p.stock ?? 0,
  inStock: p.inStock ?? (p.stock ?? 0) > 0,
});

// A basket costs the sum of today's prices of what's inside, and can only be
// bought while everything inside is in stock.
export const priceBundle = (bundle, productsById) => {
  const contents = bundle.bundleItems.map((id) => productsById.get(id)).filter(Boolean);
  const price = contents.reduce((sum, p) => sum + p.price, 0);
  const mrp = contents.reduce((sum, p) => sum + (p.mrp ?? p.price), 0);
  const complete = contents.length === bundle.bundleItems.length;
  const stock = complete && contents.length > 0 ? Math.min(...contents.map((p) => p.stock)) : 0;
  return {
    ...bundle,
    isBundle: true,
    weight: `${bundle.bundleItems.length} items`,
    price,
    ...(price < mrp ? { mrp } : {}),
    stock,
    inStock: stock > 0,
  };
};

// Everything a page needs from the catalogue, built once per API response.
export const buildCatalog = (apiProducts = [], offer = null, bundles = basketDefinitions) => {
  const products = apiProducts.map(toShopProduct);
  const productsById = new Map(products.map((p) => [p.id, p]));
  const pricedBundles = bundles.map((b) => priceBundle(b, productsById));
  const bundlesById = new Map(pricedBundles.map((b) => [b.id, b]));

  return {
    products,
    bundles: pricedBundles,
    offer: offer && offer.category ? { category: offer.category, percent: Number(offer.percent) } : null,
    productIds: new Set(productsById.keys()),
    findItem: (id) => bundlesById.get(id) ?? productsById.get(id) ?? null,
    // Unknown items (catalogue not loaded yet) are not capped.
    stockOf: (id) => (bundlesById.get(id) ?? productsById.get(id))?.stock ?? Infinity,
  };
};
