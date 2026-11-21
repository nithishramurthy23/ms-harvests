// "Out of stock" or "Only 3 left" under a product. Says nothing when there's plenty.
const LOW_STOCK = 5;

export default function StockNote({ item, className = "" }) {
  if (!item.inStock) {
    return <p className={`text-sm font-medium text-clay-600 ${className}`}>Out of stock</p>;
  }
  if (item.stock <= LOW_STOCK) {
    return <p className={`text-sm text-clay-600 ${className}`}>Only {item.stock} left</p>;
  }
  return null;
}
