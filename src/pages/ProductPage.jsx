import { useState } from "react";
import { Link, useParams } from "react-router-dom";
import { Banknote, ChevronRight, Minus, Plus } from "lucide-react";
import { useCart } from "../context/CartContext";
import { categoryName, findItem, getCategory, pricedProducts } from "../data/catalog";
import ProductImage from "../components/ProductImage";
import ProductCard from "../components/ProductCard";
import Price from "../components/Price";
import NotFoundPage from "./NotFoundPage";

// Detail page for one product or basket, with today's offer applied.
export default function ProductPage() {
  const { productId } = useParams();
  const { dispatch, quantityOf, openCart } = useCart();
  const [quantity, setQuantity] = useState(1);

  const item = findItem(Number(productId));
  if (!item) return <NotFoundPage />;

  const aisle = getCategory(item.category);
  const products = pricedProducts();
  const contents = item.isBundle
    ? item.bundleItems.map((id) => products.find((p) => p.id === id)).filter(Boolean)
    : [];
  const related = products
    .filter((p) => p.category === item.category && p.id !== item.id && !contents.includes(p))
    .slice(0, 4);
  const inCart = quantityOf(item.id);

  const handleAddToCart = () => {
    dispatch({ type: "ADD_ITEM", payload: { ...item, quantity } });
    setQuantity(1);
    openCart();
  };

  return (
    <div className="page">
      <nav aria-label="Breadcrumb" className="mb-6 flex items-center gap-1 text-sm text-bark/55">
        <Link to="/products" className="hover:text-green-700">
          Shop
        </Link>
        <ChevronRight className="h-4 w-4" />
        <Link to={`/category/${item.category}`} className="hover:text-green-700">
          {categoryName(item.category)}
        </Link>
        <ChevronRight className="h-4 w-4" />
        <span className="truncate text-bark/80">{item.name}</span>
      </nav>

      <div className="grid gap-8 md:grid-cols-2 lg:gap-14">
        <ProductImage src={item.image} alt={item.name} className="aspect-square w-full rounded-[2rem]" />

        <div>
          <p className="eyebrow mb-3">{item.isBundle ? "Weekly basket" : categoryName(item.category)}</p>
          <h1 className="text-4xl font-medium leading-tight text-green-900 sm:text-5xl">{item.name}</h1>
          <p className="mt-2 text-bark/60">{item.weight}</p>

          <div className="mt-6 flex items-center gap-3">
            <Price price={item.price} mrp={item.mrp} className="text-3xl" />
            {item.mrp && (
              <span className="rounded-full bg-clay-100 px-3 py-1 text-sm font-medium text-clay-600">
                Today&apos;s offer
              </span>
            )}
          </div>

          <p className="mt-6 text-lg leading-relaxed text-bark/75">{item.blurb ?? aisle?.about}</p>

          <div className="mt-8 flex flex-wrap items-center gap-3">
            <div className="flex h-12 items-center rounded-full border border-bark/15 bg-paper">
              <button
                onClick={() => setQuantity(Math.max(1, quantity - 1))}
                aria-label="Decrease quantity"
                className="flex h-full w-12 items-center justify-center rounded-full hover:bg-bark/5"
              >
                <Minus className="h-4 w-4" />
              </button>
              <span className="w-8 text-center font-medium tabular-nums">{quantity}</span>
              <button
                onClick={() => setQuantity(quantity + 1)}
                aria-label="Increase quantity"
                className="flex h-full w-12 items-center justify-center rounded-full hover:bg-bark/5"
              >
                <Plus className="h-4 w-4" />
              </button>
            </div>
            <button onClick={handleAddToCart} className="btn-primary h-12 flex-1 sm:flex-none sm:px-10">
              Add to basket · ₹{item.price * quantity}
            </button>
          </div>
          {inCart > 0 && <p className="mt-3 text-sm text-green-700">{inCart} already in your basket.</p>}

          <div className="mt-8 space-y-4 rounded-2xl bg-paper p-5 ring-1 ring-bark/10">
            <p className="flex items-start gap-3 text-sm text-bark/75">
              <Banknote className="mt-0.5 h-5 w-5 flex-none text-green-700" />
              Cash on delivery. You pay when the order reaches you.
            </p>
            {aisle?.tips?.length > 0 && (
              <div>
                <p className="mb-1 text-sm font-medium">Keeping it fresh</p>
                <ul className="list-disc space-y-1 pl-5 text-sm text-bark/70">
                  {aisle.tips.map((tip) => (
                    <li key={tip}>{tip}</li>
                  ))}
                </ul>
              </div>
            )}
          </div>
        </div>
      </div>

      {contents.length > 0 && (
        <section className="mt-16">
          <h2 className="mb-6 text-2xl text-green-900">What&apos;s in the basket</h2>
          <ul className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            {contents.map((p) => (
              <li key={p.id}>
                <Link
                  to={`/product/${p.id}`}
                  className="flex items-center gap-3 rounded-2xl border border-bark/10 bg-paper p-3 hover:border-green-600"
                >
                  <ProductImage src={p.image} alt="" className="h-14 w-14 flex-none rounded-xl" />
                  <span className="flex-1">
                    <span className="block font-medium leading-snug">{p.name}</span>
                    <span className="text-sm text-bark/55">{p.weight}</span>
                  </span>
                </Link>
              </li>
            ))}
          </ul>
        </section>
      )}

      {related.length > 0 && (
        <section className="mt-16">
          <h2 className="mb-6 text-2xl text-green-900">More from {categoryName(item.category).toLowerCase()}</h2>
          <div className="grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-4">
            {related.map((p) => (
              <ProductCard key={p.id} item={p} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
