import { Link, useSearchParams } from "react-router-dom";
import { X } from "lucide-react";
import ProductCard from "../components/ProductCard";
import { categories, categoryName, getCategory, pricedBundles, pricedProducts } from "../data/catalog";

const sorts = {
  popular: { label: "Popular", compare: () => 0 },
  "price-asc": { label: "Price: low to high", compare: (a, b) => a.price - b.price },
  "price-desc": { label: "Price: high to low", compare: (a, b) => b.price - a.price },
  name: { label: "Name", compare: (a, b) => a.name.localeCompare(b.name) },
};

// The shop: every product, filterable by aisle and search text. Also used for
// /category/:slug, which passes the aisle in.
export default function ProductsPage({ category: fixedCategory }) {
  const [params, setParams] = useSearchParams();
  const query = params.get("q")?.trim() ?? "";
  const sortKey = sorts[params.get("sort")] ? params.get("sort") : "popular";
  const activeCategory = fixedCategory ?? params.get("aisle") ?? "";
  const aisle = getCategory(activeCategory);

  const setParam = (key, value) => {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value);
    else next.delete(key);
    setParams(next, { replace: true });
  };

  const term = query.toLowerCase();
  const products = pricedProducts()
    .filter((p) => !activeCategory || p.category === activeCategory)
    .filter(
      (p) =>
        !term ||
        p.name.toLowerCase().includes(term) ||
        categoryName(p.category).toLowerCase().includes(term)
    )
    .sort(sorts[sortKey].compare);

  const baskets = pricedBundles().filter((b) => !activeCategory || b.category === activeCategory);
  const showBaskets = !term && baskets.length > 0;

  const chip = (active) =>
    `flex-none rounded-full border px-4 py-2 text-sm transition-colors ${
      active ? "border-green-700 bg-green-700 text-white" : "border-bark/15 bg-paper hover:border-green-600"
    }`;

  return (
    <div className="page">
      <header className="mb-6">
        {aisle && <p className="eyebrow mb-2">Aisle</p>}
        <h1 className="text-4xl font-medium text-green-900">{aisle ? aisle.name : "The shop"}</h1>
        <p className="mt-2 max-w-2xl text-bark/65">
          {aisle ? aisle.about : "Everything we have this week, from the farms around Ooty."}
        </p>
      </header>

      <div className="sticky top-[7.5rem] z-20 -mx-4 mb-6 border-b border-bark/10 bg-cream/95 px-4 py-3 backdrop-blur md:top-16">
        <div className="flex items-center gap-3">
          <div className="-my-1 flex flex-1 gap-2 overflow-x-auto py-1 [scrollbar-width:none]">
            {fixedCategory ? (
              <Link to="/products" className={chip(false)}>
                All aisles
              </Link>
            ) : (
              <button onClick={() => setParam("aisle", "")} className={chip(!activeCategory)}>
                All
              </button>
            )}
            {categories.map((c) =>
              fixedCategory ? (
                <Link key={c.slug} to={`/category/${c.slug}`} className={chip(c.slug === activeCategory)}>
                  {c.name}
                </Link>
              ) : (
                <button key={c.slug} onClick={() => setParam("aisle", c.slug)} className={chip(c.slug === activeCategory)}>
                  {c.name}
                </button>
              )
            )}
          </div>
          <label className="sr-only" htmlFor="sort">
            Sort by
          </label>
          <select
            id="sort"
            value={sortKey}
            onChange={(e) => setParam("sort", e.target.value === "popular" ? "" : e.target.value)}
            className="hidden h-10 rounded-full border border-bark/15 bg-paper px-4 text-sm sm:block"
          >
            {Object.entries(sorts).map(([key, { label }]) => (
              <option key={key} value={key}>
                {label}
              </option>
            ))}
          </select>
        </div>
      </div>

      {query && (
        <div className="mb-6 flex flex-wrap items-center gap-3">
          <p className="text-bark/70">
            {products.length} {products.length === 1 ? "result" : "results"} for <strong>“{query}”</strong>
          </p>
          <button
            onClick={() => setParam("q", "")}
            className="inline-flex items-center gap-1 rounded-full bg-bark/5 px-3 py-1 text-sm hover:bg-bark/10"
          >
            Clear search <X className="h-3.5 w-3.5" />
          </button>
        </div>
      )}

      {products.length > 0 ? (
        <div className="grid grid-cols-2 gap-3 sm:gap-5 md:grid-cols-3 lg:grid-cols-4">
          {products.map((item) => (
            <ProductCard key={item.id} item={item} />
          ))}
        </div>
      ) : (
        <div className="rounded-2xl border border-dashed border-bark/20 px-6 py-16 text-center">
          <p className="font-medium">We don&apos;t have anything like that this week.</p>
          <p className="mt-1 text-bark/60">Try another word, or browse an aisle above.</p>
        </div>
      )}

      {showBaskets && (
        <section id="baskets" className="mt-16 scroll-mt-40">
          <h2 className="mb-2 text-2xl text-green-900 sm:text-3xl">Weekly baskets</h2>
          <p className="mb-6 text-bark/65">A bit of everything, priced as the sum of what&apos;s inside.</p>
          <div className="grid grid-cols-2 gap-3 sm:gap-5 md:grid-cols-3 lg:grid-cols-5">
            {baskets.map((item) => (
              <ProductCard key={item.id} item={item} />
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
