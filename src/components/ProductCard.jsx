import { Link } from "react-router-dom";
import ProductImage from "./ProductImage";
import AddToCartButton from "./AddToCartButton";
import Price from "./Price";

// Grid card used on every listing. Expects an item that already has today's
// price (see data/catalog.js).
export default function ProductCard({ item }) {
  const offPercent =
    item.offPercent ?? (item.mrp ? Math.round(((item.mrp - item.price) / item.mrp) * 100) : 0);

  return (
    <article className="group flex flex-col overflow-hidden rounded-2xl border border-bark/10 bg-paper transition-shadow hover:shadow-[0_8px_24px_-12px_rgba(45,42,36,0.25)]">
      <Link to={`/product/${item.id}`} className="relative block" tabIndex={-1} aria-hidden="true">
        <ProductImage
          src={item.image}
          alt={item.name}
          className="aspect-[4/3] w-full transition-transform duration-500 group-hover:scale-[1.03]"
        />
        {offPercent > 0 && (
          <span className="absolute left-3 top-3 rounded-full bg-clay-500 px-2.5 py-1 text-xs font-bold text-white">
            {offPercent}% off today
          </span>
        )}
      </Link>
      <div className="flex flex-1 flex-col gap-1 p-3 sm:p-4">
        <p className="text-xs text-bark/55">{item.weight}</p>
        <Link
          to={`/product/${item.id}`}
          className="line-clamp-2 font-medium leading-snug text-bark hover:text-green-700"
        >
          {item.name}
        </Link>
        <div className="mt-auto flex flex-col gap-3 pt-3">
          <Price price={item.price} mrp={item.mrp} />
          <AddToCartButton item={item} size="sm" />
        </div>
      </div>
    </article>
  );
}
