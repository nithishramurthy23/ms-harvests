import { useEffect, useState } from "react";
import { Link, useSearchParams } from "react-router-dom";
import { X } from "lucide-react";
import ProductCard from "../components/ProductCard";
import { categories, getCategory } from "../data/catalog";
import { useCatalog } from "../context/CatalogContext";
import { searchProducts } from "../api/search";

const SORTS = [
  { value: "", label: "Best match" },
  { value: "popular", label: "Popular" },
  { value: "price-asc", label: "Price: low to high" },
  { value: "price-desc", label: "Price: high to low" },
  { value: "discount", label: "Biggest discount" },
  { value: "name", label: "Name" },
];
const PAGE_SIZE = 24;

// The shop: search, aisles, price and stock filters, sorting and pages. All of
// it lives in the URL (?q=carrot&aisle=vegetables&max=100&instock=1&sort=price-asc&page=2),
// so a filtered page can be shared or bookmarked. Also used for /category/:slug.
export default function ProductsPage({ category: fixedCategory }) {
  const [params, setParams] = useSearchParams();
  const catalog = useCatalog();

  const query = params.get("q")?.trim() ?? "";
  const activeCategory = fixedCategory ?? params.get("aisle") ?? "";
  const sort = params.get("sort") ?? "";
  const minPrice = params.get("min") ?? "";
  const maxPrice = params.get("max") ?? "";
  const inStockOnly = params.get("instock") === "1";
  const page = Math.max(1, Number(params.get("page")) || 1);
  const aisle = getCategory(activeCategory);

  const [result, setResult] = useState(null);
  const [status, setStatus] = useState("loading");
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setStatus("loading");
    searchProducts(
      { q: query, category: activeCategory, minPrice, maxPrice, inStock: inStockOnly, sort, page: page - 1, size: PAGE_SIZE },
      controller.signal
    )
      .then((data) => {
        setResult(data);
        setStatus("ready");
      })
      .catch((err) => {
        if (err?.name !== "CanceledError" && err?.name !== "AbortError") setStatus("error");
      });
    return () => controller.abort();
    // catalog.products changes after checkout, so stock shown here stays current
  }, [query, activeCategory, minPrice, maxPrice, inStockOnly, sort, page, retry, catalog.products]);

  // Any filter change goes back to page 1.
  const setParam = (key, value) => {
    const next = new URLSearchParams(params);
    if (value) next.set(key, value);
    else next.delete(key);
    if (key !== "page") next.delete("page");
    setParams(next, { replace: key !== "page" });
  };

  const clearFilters = () => {
    const next = new URLSearchParams();
    if (query) next.set("q", query);
    setParams(next, { replace: true });
  };

  const products = result?.items ?? [];
  const filtersOn = Boolean(minPrice || maxPrice || inStockOnly || (!fixedCategory && activeCategory));
  const baskets = catalog.bundles.filter((b) => !activeCategory || b.category === activeCategory);
  const showBaskets = !query && !minPrice && !maxPrice && page === 1 && baskets.length > 0;
  const count = (slug) => result?.categoryCounts?.[slug] ?? 0;
  const allCount = result ? Object.values(result.categoryCounts ?? {}).reduce((a, b) => a + b, 0) : null;

  const chip = (active) =>
    `flex-none rounded-full border px-4 py-2 text-sm transition-colors ${
      active ? "border-green-700 bg-green-700 text-white" : "border-bark/15 bg-paper hover:border-green-600"
    }`;
  const countBadge = (n) => (result ? <span className="ml-1 opacity-60 tabular-nums">{n}</span> : null);

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
        <div className="-my-1 flex gap-2 overflow-x-auto py-1 [scrollbar-width:none]">
          {fixedCategory ? (
            <Link to="/products" className={chip(false)}>
              All aisles
            </Link>
          ) : (
            <button onClick={() => setParam("aisle", "")} className={chip(!activeCategory)}>
              All{countBadge(allCount)}
            </button>
          )}
          {categories.map((c) =>
            fixedCategory ? (
              <Link key={c.slug} to={`/category/${c.slug}`} className={chip(c.slug === activeCategory)}>
                {c.name}
              </Link>
            ) : (
              <button
                key={c.slug}
                onClick={() => setParam("aisle", c.slug)}
                className={chip(c.slug === activeCategory)}
                aria-label={c.name}
              >
                {c.name}
                {countBadge(count(c.slug))}
              </button>
            )
          )}
        </div>

        <div className="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2 text-sm">
          <label className="flex items-center gap-2">
            <span className="text-bark/65">Sort</span>
            <select
              value={sort}
              onChange={(e) => setParam("sort", e.target.value)}
              className="h-9 rounded-full border border-bark/15 bg-paper px-3"
              aria-label="Sort by"
            >
              {SORTS.map((s) => (
                <option key={s.value} value={s.value}>
                  {s.label}
                </option>
              ))}
            </select>
          </label>

          <PriceFilter
            min={minPrice}
            max={maxPrice}
            hintLow={result?.lowestPrice}
            hintHigh={result?.highestPrice}
            onChange={(min, max) => {
              const next = new URLSearchParams(params);
              min ? next.set("min", min) : next.delete("min");
              max ? next.set("max", max) : next.delete("max");
              next.delete("page");
              setParams(next, { replace: true });
            }}
          />

          <label className="flex items-center gap-2">
            <input
              type="checkbox"
              checked={inStockOnly}
              onChange={(e) => setParam("instock", e.target.checked ? "1" : "")}
              className="h-4 w-4 accent-green-700"
            />
            In stock only
          </label>

          {filtersOn && (
            <button onClick={clearFilters} className="text-green-700 underline-offset-2 hover:underline">
              Clear filters
            </button>
          )}
        </div>
      </div>

      {query && result && (
        <div className="mb-6 flex flex-wrap items-center gap-3">
          <p className="text-bark/70">
            {result.totalItems} {result.totalItems === 1 ? "result" : "results"} for <strong>“{query}”</strong>
          </p>
          <button
            onClick={() => setParam("q", "")}
            className="inline-flex items-center gap-1 rounded-full bg-bark/5 px-3 py-1 text-sm hover:bg-bark/10"
          >
            Clear search <X className="h-3.5 w-3.5" />
          </button>
        </div>
      )}

      {status === "loading" && !result ? (
        <p className="py-16 text-center text-bark/60">Loading this week&apos;s harvest…</p>
      ) : status === "error" ? (
        <div className="rounded-2xl border border-dashed border-bark/20 px-6 py-16 text-center">
          <p className="font-medium">We couldn&apos;t load the shop just now.</p>
          <button onClick={() => setRetry((n) => n + 1)} className="btn-ghost mt-4">
            Try again
          </button>
        </div>
      ) : products.length > 0 ? (
        <>
          <div
            className={`grid grid-cols-2 gap-3 transition-opacity sm:gap-5 md:grid-cols-3 lg:grid-cols-4 ${
              status === "loading" ? "opacity-60" : ""
            }`}
          >
            {products.map((item) => (
              <ProductCard key={item.id} item={item} />
            ))}
          </div>
          {result.totalPages > 1 && (
            <nav className="mt-8 flex items-center justify-center gap-3" aria-label="Pages">
              <button
                onClick={() => setParam("page", String(page - 1))}
                disabled={page <= 1}
                className="btn-ghost px-4 py-2 disabled:opacity-40"
              >
                Previous
              </button>
              <span className="text-sm text-bark/65">
                Page {page} of {result.totalPages}
              </span>
              <button
                onClick={() => setParam("page", String(page + 1))}
                disabled={page >= result.totalPages}
                className="btn-ghost px-4 py-2 disabled:opacity-40"
              >
                Next
              </button>
            </nav>
          )}
        </>
      ) : (
        <div className="rounded-2xl border border-dashed border-bark/20 px-6 py-16 text-center">
          <p className="font-medium">We don&apos;t have anything like that this week.</p>
          {result?.didYouMean ? (
            <p className="mt-1 text-bark/60">
              Did you mean{" "}
              <button onClick={() => setParam("q", result.didYouMean)} className="font-medium text-green-700 underline">
                {result.didYouMean}
              </button>
              ?
            </p>
          ) : (
            <p className="mt-1 text-bark/60">
              {filtersOn ? "Try removing a filter." : "Try another word, or browse an aisle above."}
            </p>
          )}
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

// Min and max price. Applied on Enter or when leaving the box, not on every key.
function PriceFilter({ min, max, hintLow, hintHigh, onChange }) {
  const [low, setLow] = useState(min);
  const [high, setHigh] = useState(max);
  useEffect(() => setLow(min), [min]);
  useEffect(() => setHigh(max), [max]);

  const apply = () => {
    if (low !== min || high !== max) onChange(low, high);
  };
  const box = "h-9 w-20 rounded-full border border-bark/15 bg-paper px-3 tabular-nums";

  return (
    <form
      className="flex items-center gap-2"
      onSubmit={(e) => {
        e.preventDefault();
        apply();
      }}
    >
      <span className="text-bark/65">Price ₹</span>
      <input
        type="number"
        min="0"
        inputMode="numeric"
        value={low}
        onChange={(e) => setLow(e.target.value)}
        onBlur={apply}
        placeholder={hintLow != null ? String(Math.floor(hintLow)) : "Min"}
        aria-label="Lowest price"
        className={box}
      />
      <span className="text-bark/45">to</span>
      <input
        type="number"
        min="0"
        inputMode="numeric"
        value={high}
        onChange={(e) => setHigh(e.target.value)}
        onBlur={apply}
        placeholder={hintHigh != null ? String(Math.ceil(hintHigh)) : "Max"}
        aria-label="Highest price"
        className={box}
      />
      <button type="submit" className="sr-only">
        Apply price
      </button>
    </form>
  );
}
