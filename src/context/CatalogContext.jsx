import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import api from "../api/axiosInstance";
import { buildCatalog } from "../data/catalog";

// Loads products, prices and stock from the backend once, and shares them with
// every page. Without a provider (some tests) pages see an empty catalogue.
const emptyCatalog = { ...buildCatalog([], null, []), status: "ready", reload: () => {} };
const CatalogContext = createContext(emptyCatalog);

// Two requests in parallel: every product with today's price, and today's offer
// (the backend answers 204 No Content when there isn't one).
export const fetchCatalog = async () => {
  const [products, offer] = await Promise.all([api.get("/products"), api.get("/offers/today")]);
  return { products: products.data, offer: offer.status === 200 ? offer.data : null };
};

export function CatalogProvider({ children, load = fetchCatalog }) {
  const [data, setData] = useState({ status: "loading", products: [], offer: null });

  // Called on start, and again after an order, because stock has changed.
  const reload = useCallback(async () => {
    try {
      const { products, offer } = await load();
      setData({ status: "ready", products, offer });
    } catch (err) {
      console.error("Failed to load the catalogue:", err);
      setData((prev) => ({ ...prev, status: "error" }));
    }
  }, [load]);

  useEffect(() => {
    reload();
  }, [reload]);

  // buildCatalog turns the flat product list into what pages need: priced baskets,
  // lookups by id and stock checks.
  // useMemo means that only happens when the data actually changes.
  const value = useMemo(
    () => ({ ...buildCatalog(data.products, data.offer), status: data.status, reload }),
    [data, reload]
  );

  return <CatalogContext.Provider value={value}>{children}</CatalogContext.Provider>;
}

export const useCatalog = () => useContext(CatalogContext);
