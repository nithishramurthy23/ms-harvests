import { useState } from "react";
import { Search } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { categoryName, pricedProducts } from "../data/catalog";
import ProductImage from "./ProductImage";

// Search box with instant suggestions. Enter shows every match on the shop page.
export default function SearchBar({ onNavigate }) {
  const [query, setQuery] = useState("");
  const [showResults, setShowResults] = useState(false);
  const navigate = useNavigate();

  const term = query.trim().toLowerCase();
  const matches = term
    ? pricedProducts()
        .filter(
          (p) =>
            p.name.toLowerCase().includes(term) ||
            categoryName(p.category).toLowerCase().includes(term)
        )
        .slice(0, 6)
    : [];

  const go = (path) => {
    navigate(path);
    setShowResults(false);
    setQuery("");
    onNavigate?.();
  };

  const handleSearch = (e) => {
    e.preventDefault();
    if (term) go(`/products?q=${encodeURIComponent(query.trim())}`);
  };

  return (
    <div className="relative">
      <form onSubmit={handleSearch} role="search">
        <label htmlFor="site-search" className="sr-only">
          Search products
        </label>
        <input
          id="site-search"
          type="search"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setShowResults(true);
          }}
          onFocus={() => setShowResults(true)}
          onBlur={() => setShowResults(false)}
          onKeyDown={(e) => e.key === "Escape" && setShowResults(false)}
          placeholder="Search carrots, honey, paneer…"
          autoComplete="off"
          className="h-11 w-full rounded-full border border-bark/15 bg-cream pl-11 pr-4 text-[15px] text-bark placeholder:text-bark/45 focus:border-green-600 focus:bg-paper focus:outline-none focus:ring-2 focus:ring-green-200"
        />
        <Search className="pointer-events-none absolute left-4 top-3 h-5 w-5 text-bark/45" />
      </form>

      {showResults && term && (
        <div
          className="absolute z-50 mt-2 w-full overflow-hidden rounded-2xl border border-bark/10 bg-paper shadow-xl"
          onMouseDown={(e) => e.preventDefault()}
        >
          {matches.length > 0 ? (
            <ul>
              {matches.map((product) => (
                <li key={product.id}>
                  <button
                    type="button"
                    onClick={() => go(`/product/${product.id}`)}
                    className="flex w-full items-center gap-3 px-4 py-2.5 text-left hover:bg-green-50"
                  >
                    <ProductImage src={product.image} alt="" className="h-9 w-9 rounded-lg" />
                    <span className="flex-1">
                      <span className="block text-sm font-medium">{product.name}</span>
                      <span className="block text-xs text-bark/55">
                        {categoryName(product.category)} · {product.weight}
                      </span>
                    </span>
                    <span className="text-sm font-bold">₹{product.price}</span>
                  </button>
                </li>
              ))}
              <li>
                <button
                  type="button"
                  onClick={handleSearch}
                  className="w-full border-t border-bark/10 px-4 py-2.5 text-left text-sm font-medium text-green-700 hover:bg-green-50"
                >
                  See all results for “{query.trim()}”
                </button>
              </li>
            </ul>
          ) : (
            <p className="px-4 py-3 text-sm text-bark/60">
              Nothing matches “{query.trim()}”. Try a vegetable or fruit name.
            </p>
          )}
        </div>
      )}
    </div>
  );
}
