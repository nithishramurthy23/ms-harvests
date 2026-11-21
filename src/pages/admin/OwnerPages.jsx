import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { adminApi, errorMessage } from "./adminApi";

const STATUS_LABEL = {
  PENDING: "New",
  PROCESSING: "Packing",
  SHIPPED: "Out for delivery",
  DELIVERED: "Delivered",
  CANCELLED: "Cancelled",
};

// Owner's overview: order counts, money, and what needs restocking.
export function OwnerSummaryPage() {
  const [summary, setSummary] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    adminApi.summary().then(setSummary).catch((err) => setError(errorMessage(err, "Couldn't load the summary.")));
  }, []);

  if (error) return <p className="text-clay-600">{error}</p>;
  if (!summary) return <p className="text-bark/60">Loading…</p>;

  const tile = (label, value, hint) => (
    <div className="rounded-2xl border border-bark/10 bg-paper p-5">
      <p className="text-sm text-bark/60">{label}</p>
      <p className="mt-1 text-3xl font-bold tabular-nums">{value}</p>
      {hint && <p className="mt-1 text-xs text-bark/50">{hint}</p>}
    </div>
  );

  return (
    <section className="space-y-8">
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {tile("Collected", `₹${Number(summary.deliveredRevenue).toLocaleString("en-IN")}`, "Delivered orders, cash on delivery")}
        {tile("To collect", `₹${Number(summary.openRevenue).toLocaleString("en-IN")}`, "New, packing and out for delivery")}
        {tile("New orders", summary.ordersByStatus.PENDING, "Waiting to be packed")}
        {tile("Customers", summary.customers, "Users and members")}
      </div>

      <div>
        <h2 className="mb-3 text-xl text-green-900">Orders by status</h2>
        <div className="flex flex-wrap gap-2">
          {Object.entries(STATUS_LABEL).map(([status, label]) => (
            <span key={status} className="rounded-full border border-bark/10 bg-paper px-4 py-2 text-sm">
              {label} <strong className="ml-1 tabular-nums">{summary.ordersByStatus[status] ?? 0}</strong>
            </span>
          ))}
        </div>
      </div>

      <div>
        <h2 className="mb-3 text-xl text-green-900">
          Running low <span className="text-base text-bark/50">({summary.lowStockThreshold} or fewer left)</span>
        </h2>
        {summary.lowStock.length === 0 ? (
          <p className="text-bark/60">Everything is well stocked.</p>
        ) : (
          <ul className="divide-y divide-bark/10 rounded-2xl border border-bark/10 bg-paper">
            {summary.lowStock.map((p) => (
              <li key={p.id} className="flex items-center justify-between px-4 py-3">
                <span>{p.name}</span>
                <span className={`text-sm font-bold ${p.stock === 0 ? "text-clay-600" : "text-bark/70"}`}>
                  {p.stock === 0 ? "Sold out" : `${p.stock} left`}
                </span>
              </li>
            ))}
          </ul>
        )}
        <Link to="/admin/products" className="mt-3 inline-block text-sm font-medium text-green-700 hover:text-green-900">
          Restock in Products &amp; stock →
        </Link>
      </div>
    </section>
  );
}
