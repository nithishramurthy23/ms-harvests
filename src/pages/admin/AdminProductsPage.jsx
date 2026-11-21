import { useCallback, useEffect, useState } from "react";
import { Eye, EyeOff, History, Pencil, Plus } from "lucide-react";
import { useCatalog } from "../../context/CatalogContext";
import ProductImage from "../../components/ProductImage";
import { adminApi, errorMessage } from "./adminApi";

const LOW_STOCK = 5;

// Products and stock: add, edit, hide, and restock, with a history per product.
export default function AdminProductsPage() {
  const { reload: reloadShop } = useCatalog();
  const [products, setProducts] = useState([]);
  const [categories, setCategories] = useState([]);
  const [status, setStatus] = useState("loading");
  const [filter, setFilter] = useState("");
  const [editing, setEditing] = useState(null); // null, "new" or a product

  const load = useCallback(async () => {
    try {
      const [p, c] = await Promise.all([adminApi.products(), adminApi.categories()]);
      setProducts(p);
      setCategories(c);
      setStatus("ready");
    } catch (err) {
      setStatus(errorMessage(err, "Couldn't load products."));
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  // Swap in the updated product and refresh what customers see.
  const replace = (updated) => {
    setProducts((list) => {
      const exists = list.some((p) => p.id === updated.id);
      return exists ? list.map((p) => (p.id === updated.id ? updated : p)) : [...list, updated];
    });
    reloadShop();
  };

  if (status === "loading") return <p className="text-bark/60">Loading products…</p>;
  if (status !== "ready") return <p className="text-clay-600">{status}</p>;

  const term = filter.trim().toLowerCase();
  const shown = products.filter((p) => !term || p.name.toLowerCase().includes(term));
  const soldOut = products.filter((p) => p.active && p.stock === 0).length;

  return (
    <section>
      <div className="mb-4 flex flex-wrap items-center gap-3">
        <input
          type="search"
          value={filter}
          onChange={(e) => setFilter(e.target.value)}
          placeholder="Find a product"
          aria-label="Find a product"
          className="h-10 min-w-0 basis-full rounded-full border border-bark/15 bg-paper px-4 text-sm sm:max-w-xs sm:flex-1 sm:basis-auto"
        />
        <p className="text-sm text-bark/60">
          {products.length} products{soldOut > 0 && <span className="text-clay-600"> · {soldOut} sold out</span>}
        </p>
        <button onClick={() => setEditing("new")} className="btn-primary ml-auto px-4 py-2 text-sm">
          <Plus className="h-4 w-4" /> Add product
        </button>
      </div>

      {editing && (
        <ProductForm
          product={editing === "new" ? null : editing}
          categories={categories}
          onSaved={(saved) => {
            replace(saved);
            setEditing(null);
          }}
          onCancel={() => setEditing(null)}
        />
      )}

      <ul className="divide-y divide-bark/10 rounded-2xl border border-bark/10 bg-paper">
        {shown.map((product) => (
          <ProductRow key={product.id} product={product} onChange={replace} onEdit={() => setEditing(product)} />
        ))}
        {shown.length === 0 && <li className="px-4 py-8 text-center text-bark/60">No products match.</li>}
      </ul>
    </section>
  );
}

function StockBadge({ stock }) {
  const tone =
    stock === 0 ? "bg-clay-100 text-clay-600" : stock <= LOW_STOCK ? "bg-mustard-100 text-bark" : "bg-green-50 text-green-700";
  return (
    <span className={`rounded-full px-2.5 py-0.5 text-xs font-bold tabular-nums ${tone}`}>
      {stock === 0 ? "Sold out" : `${stock} in stock`}
    </span>
  );
}

function ProductRow({ product, onChange, onEdit }) {
  const [amount, setAmount] = useState("");
  const [reason, setReason] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [history, setHistory] = useState(null);

  const changeStock = async (sign) => {
    const qty = Number.parseInt(amount, 10);
    if (!qty || qty < 1) {
      setError("Enter how many units.");
      return;
    }
    setBusy(true);
    setError("");
    try {
      const updated = await adminApi.changeStock(product.id, sign * qty, reason);
      onChange(updated);
      setAmount("");
      setReason("");
      if (history) setHistory(await adminApi.stockHistory(product.id));
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const toggleActive = async () => {
    setBusy(true);
    try {
      onChange(await adminApi.setActive(product.id, !product.active));
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setBusy(false);
    }
  };

  const toggleHistory = async () => {
    if (history) {
      setHistory(null);
      return;
    }
    try {
      setHistory(await adminApi.stockHistory(product.id));
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <li className={`px-4 py-4 ${product.active ? "" : "bg-bark/[0.03]"}`}>
      <div className="flex flex-wrap items-center gap-x-4 gap-y-3">
        <ProductImage src={product.imageUrl} alt="" className="h-12 w-12 flex-none rounded-xl" />
        <div className="min-w-[10rem] flex-1">
          <p className="font-medium leading-snug">
            {product.name}
            {!product.active && (
              <span className="ml-2 rounded-full bg-bark/10 px-2 py-0.5 text-xs font-medium text-bark/70">Hidden</span>
            )}
          </p>
          <p className="text-sm text-bark/55">
            {product.categoryName} · {product.weight || "—"} · ₹{Number(product.price)}
          </p>
        </div>
        <StockBadge stock={product.stock} />

        <form
          className="flex flex-wrap items-center gap-2"
          onSubmit={(e) => {
            e.preventDefault();
            changeStock(1);
          }}
        >
          <input
            type="number"
            min="1"
            inputMode="numeric"
            value={amount}
            onChange={(e) => setAmount(e.target.value)}
            placeholder="Qty"
            aria-label={`Units to add or remove for ${product.name}`}
            className="h-9 w-20 rounded-full border border-bark/15 bg-cream px-3 text-sm"
          />
          <input
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="Reason (optional)"
            aria-label={`Reason for ${product.name} stock change`}
            className="h-9 w-40 rounded-full border border-bark/15 bg-cream px-3 text-sm"
          />
          <button type="submit" disabled={busy} className="h-9 rounded-full bg-green-700 px-3 text-sm font-medium text-white hover:bg-green-800 disabled:opacity-50">
            Add
          </button>
          <button
            type="button"
            disabled={busy}
            onClick={() => changeStock(-1)}
            className="h-9 rounded-full border border-bark/15 px-3 text-sm font-medium hover:bg-bark/5 disabled:opacity-50"
          >
            Remove
          </button>
        </form>

        <div className="flex gap-1">
          <IconButton label={`Edit ${product.name}`} onClick={onEdit}>
            <Pencil className="h-4 w-4" />
          </IconButton>
          <IconButton label={product.active ? `Hide ${product.name}` : `Show ${product.name}`} onClick={toggleActive} disabled={busy}>
            {product.active ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
          </IconButton>
          <IconButton label={`Stock history for ${product.name}`} onClick={toggleHistory} pressed={!!history}>
            <History className="h-4 w-4" />
          </IconButton>
        </div>
      </div>

      {error && <p className="mt-2 text-sm text-clay-600">{error}</p>}

      {history && (
        <div className="mt-3 rounded-xl bg-cream px-4 py-3 text-sm">
          {history.length === 0 ? (
            <p className="text-bark/60">No stock changes recorded yet.</p>
          ) : (
            <ul className="space-y-1">
              {history.map((m) => (
                <li key={m.id} className="flex flex-wrap gap-x-3">
                  <span className={`w-12 font-bold tabular-nums ${m.change > 0 ? "text-green-700" : "text-clay-600"}`}>
                    {m.change > 0 ? `+${m.change}` : m.change}
                  </span>
                  <span className="w-16 tabular-nums text-bark/60">→ {m.stockAfter}</span>
                  <span className="flex-1">{m.reason}</span>
                  <span className="text-bark/50">
                    {m.changedBy} · {new Date(m.changedAt).toLocaleString()}
                  </span>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </li>
  );
}

function IconButton({ label, onClick, disabled, pressed, children }) {
  return (
    <button
      type="button"
      onClick={onClick}
      disabled={disabled}
      aria-label={label}
      title={label}
      aria-pressed={pressed}
      className={`rounded-full p-2 text-bark/70 hover:bg-bark/5 disabled:opacity-50 ${pressed ? "bg-bark/10" : ""}`}
    >
      {children}
    </button>
  );
}

const emptyForm = { name: "", categoryId: "", price: "", weight: "", imageUrl: "", description: "", initialStock: "" };

function ProductForm({ product, categories, onSaved, onCancel }) {
  const isNew = !product;
  const [form, setForm] = useState(
    isNew
      ? { ...emptyForm, categoryId: categories[0]?.id ?? "" }
      : {
          name: product.name,
          categoryId: product.categoryId ?? "",
          price: String(product.price),
          weight: product.weight ?? "",
          imageUrl: product.imageUrl ?? "",
          description: product.description ?? "",
        }
  );
  const [errors, setErrors] = useState({});
  const [message, setMessage] = useState("");
  const [saving, setSaving] = useState(false);

  const set = (key) => (e) => setForm({ ...form, [key]: e.target.value });

  const submit = async (e) => {
    e.preventDefault();
    setSaving(true);
    setErrors({});
    setMessage("");
    const body = {
      ...form,
      categoryId: form.categoryId ? Number(form.categoryId) : null,
      price: form.price === "" ? null : form.price,
      initialStock: isNew ? Number(form.initialStock || 0) : undefined,
    };
    try {
      onSaved(isNew ? await adminApi.createProduct(body) : await adminApi.updateProduct(product.id, body));
    } catch (err) {
      setErrors(err?.response?.data?.fields ?? {});
      setMessage(errorMessage(err));
    } finally {
      setSaving(false);
    }
  };

  const field = (key, label, input) => (
    <label className="block text-sm">
      <span className="mb-1 block font-medium">{label}</span>
      {input}
      {errors[key] && <span className="mt-1 block text-clay-600">{errors[key]}</span>}
    </label>
  );
  const inputClass = "h-10 w-full rounded-xl border border-bark/15 bg-cream px-3";

  return (
    <form onSubmit={submit} className="mb-6 rounded-2xl border border-green-600/40 bg-paper p-5">
      <h2 className="mb-4 text-xl text-green-900">{isNew ? "Add a product" : `Edit ${product.name}`}</h2>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
        {field("name", "Name", <input value={form.name} onChange={set("name")} className={inputClass} required />)}
        {field(
          "categoryId",
          "Category",
          <select value={form.categoryId} onChange={set("categoryId")} className={inputClass} required>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>
        )}
        {field(
          "price",
          "Price (₹)",
          <input type="number" min="0.01" step="0.01" value={form.price} onChange={set("price")} className={inputClass} required />
        )}
        {field("weight", "Pack size", <input value={form.weight} onChange={set("weight")} placeholder="500 g, 1 dozen…" className={inputClass} />)}
        {field("imageUrl", "Image URL", <input type="url" value={form.imageUrl} onChange={set("imageUrl")} className={inputClass} />)}
        {isNew &&
          field(
            "initialStock",
            "Starting stock",
            <input type="number" min="0" value={form.initialStock} onChange={set("initialStock")} placeholder="0" className={inputClass} />
          )}
        <div className="sm:col-span-2 lg:col-span-3">
          {field(
            "description",
            "Description",
            <textarea value={form.description} onChange={set("description")} rows={2} className="w-full rounded-xl border border-bark/15 bg-cream px-3 py-2" />
          )}
        </div>
      </div>
      {!isNew && <p className="mt-3 text-sm text-bark/60">Change stock with Add / Remove on the product row, so it&apos;s logged.</p>}
      {message && <p className="mt-3 text-sm text-clay-600">{message}</p>}
      <div className="mt-4 flex gap-3">
        <button type="submit" disabled={saving} className="btn-primary px-5 py-2">
          {saving ? "Saving…" : isNew ? "Add product" : "Save changes"}
        </button>
        <button type="button" onClick={onCancel} className="btn-ghost px-5 py-2">
          Cancel
        </button>
      </div>
    </form>
  );
}
