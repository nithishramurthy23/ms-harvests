import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { ROLES } from "../../utils/roles";
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

// Owner gives people roles. The backend refuses changing your own role and
// demoting the last owner; the page just shows its message.
export function OwnerPeoplePage() {
  const { user: me } = useAuth();
  const [users, setUsers] = useState(null);
  const [error, setError] = useState("");

  useEffect(() => {
    adminApi.users().then(setUsers).catch((err) => setError(errorMessage(err, "Couldn't load people.")));
  }, []);

  const changeRole = async (person, role) => {
    setError("");
    try {
      const updated = await adminApi.setRole(person.id, role);
      setUsers((list) => list.map((u) => (u.id === updated.id ? updated : u)));
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  if (!users) return error ? <p className="text-clay-600">{error}</p> : <p className="text-bark/60">Loading…</p>;

  return (
    <section>
      <div className="mb-4 grid gap-2 text-sm text-bark/65 sm:grid-cols-2 lg:grid-cols-4">
        {ROLES.map((r) => (
          <p key={r.value}>
            <strong className="text-bark">{r.label}:</strong> {r.hint}
          </p>
        ))}
      </div>
      {error && <p className="mb-4 text-sm text-clay-600">{error}</p>}
      <ul className="divide-y divide-bark/10 rounded-2xl border border-bark/10 bg-paper">
        {users.map((person) => (
          <li key={person.id} className="flex flex-wrap items-center gap-3 px-4 py-3">
            <div className="min-w-[12rem] flex-1">
              <p className="font-medium">
                {person.name}
                {person.id === me?.id && <span className="ml-2 text-xs text-bark/50">(you)</span>}
                {!person.verified && <span className="ml-2 text-xs text-bark/50">not verified</span>}
              </p>
              <p className="text-sm text-bark/55">{person.email}</p>
            </div>
            <label className="sr-only" htmlFor={`role-${person.id}`}>
              Role for {person.email}
            </label>
            <select
              id={`role-${person.id}`}
              value={person.role}
              disabled={person.id === me?.id}
              onChange={(e) => changeRole(person, e.target.value)}
              className="h-10 rounded-full border border-bark/15 bg-cream px-4 text-sm disabled:opacity-60"
            >
              {ROLES.map((r) => (
                <option key={r.value} value={r.value}>
                  {r.label}
                </option>
              ))}
            </select>
          </li>
        ))}
      </ul>
    </section>
  );
}
