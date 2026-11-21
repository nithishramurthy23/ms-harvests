import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import api from "../api/axiosInstance";
import { buildCatalog } from "../data/catalog";

// Loads products, prices and stock from the backend once, and shares them with
// every page. Without a provider (some tests) pages see an empty catalogue.
const emptyCatalog = { ...buildCatalog([], null, []), status: "ready", reload: () => {} };
const CatalogContext = createContext(emptyCatalog);

export const fetchCatalog = async () => {
  const [products, offer] = await Promise.all([api.get("/products"), api.get("/offers/today")]);
  return { products: products.data, offer: offer.status === 200 ? offer.data : null };
};

export function CatalogProvider({ children, load = fetchCatalog }) {
  const [data, setData] = useState({ status: "loading", products: [], offer: null });

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

  const value = useMemo(
    () => ({ ...buildCatalog(data.products, data.offer), status: data.status, reload }),
    [data, reload]
  );

  return <CatalogContext.Provider value={value}>{children}</CatalogContext.Provider>;
}

export const useCatalog = () => useContext(CatalogContext);
