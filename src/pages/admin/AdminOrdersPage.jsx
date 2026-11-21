import { useCallback, useEffect, useState } from "react";
import { useCatalog } from "../../context/CatalogContext";
import { PAYMENT_LABEL } from "../../utils/payments";
import { adminApi, errorMessage } from "./adminApi";

const FILTERS = ["PENDING", "PROCESSING", "SHIPPED", "DELIVERED", "CANCELLED", ""];
const LABEL = {
  PENDING: "New",
  PROCESSING: "Packing",
  SHIPPED: "Out for delivery",
  DELIVERED: "Delivered",
  CANCELLED: "Cancelled",
  "": "All",
};
const ACTION = {
  PROCESSING: "Start packing",
  SHIPPED: "Mark shipped",
  DELIVERED: "Mark delivered",
  CANCELLED: "Cancel order",
};
const TONE = {
  PENDING: "bg-mustard-100 text-bark",
  PROCESSING: "bg-green-50 text-green-700",
  SHIPPED: "bg-green-100 text-green-800",
  DELIVERED: "bg-green-700 text-white",
  CANCELLED: "bg-bark/10 text-bark/60",
};

// Every customer's orders, filtered by status, with buttons for the next step.
export default function AdminOrdersPage() {
  const { reload: reloadShop } = useCatalog();
  const [filter, setFilter] = useState("PENDING");
  const [orders, setOrders] = useState(null);
  const [error, setError] = useState("");

  const load = useCallback(async () => {
    setOrders(null);
    setError("");
    try {
      setOrders(await adminApi.orders(filter));
    } catch (err) {
      setError(errorMessage(err, "Couldn't load orders."));
      setOrders([]);
    }
  }, [filter]);

  useEffect(() => {
    load();
  }, [load]);

  const retryRefund = async (order) => {
    setError("");
    try {
      const { paymentStatus } = await adminApi.retryRefund(order.id);
      setOrders((list) => list.map((o) => (o.id === order.id ? { ...o, paymentStatus } : o)));
      if (paymentStatus !== "REFUNDED") setError(`The refund for order #${order.id} failed again. Try later.`);
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  const move = async (order, next) => {
    if (next === "CANCELLED" && !window.confirm(`Cancel order #${order.id}? Its items go back in stock, and a paid order is refunded.`)) return;
    setError("");
    try {
      const updated = await adminApi.setOrderStatus(order.id, next);
      // leave the filtered list once it no longer matches
      setOrders((list) =>
        filter && updated.status !== filter ? list.filter((o) => o.id !== order.id) : list.map((o) => (o.id === order.id ? updated : o))
      );
      if (next === "CANCELLED") reloadShop();
    } catch (err) {
      setError(errorMessage(err));
    }
  };

  return (
    <section>
      <div className="mb-4 flex gap-2 overflow-x-auto pb-1">
        {FILTERS.map((f) => (
          <button
            key={f || "all"}
            onClick={() => setFilter(f)}
            className={`flex-none rounded-full border px-4 py-1.5 text-sm ${
              filter === f ? "border-green-700 bg-green-700 text-white" : "border-bark/15 bg-paper hover:border-green-600"
            }`}
          >
            {LABEL[f]}
          </button>
        ))}
      </div>

      {error && <p className="mb-4 text-sm text-clay-600">{error}</p>}
      {orders === null && <p className="text-bark/60">Loading orders…</p>}
      {orders?.length === 0 && !error && (
        <p className="rounded-2xl border border-dashed border-bark/20 px-6 py-12 text-center text-bark/60">
          No {LABEL[filter].toLowerCase()} orders.
        </p>
      )}

      <ul className="space-y-4">
        {orders?.map((order) => (
          <li key={order.id} className="rounded-2xl border border-bark/10 bg-paper p-5">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="font-medium">
                  Order #{order.id}
                  <span className={`ml-2 rounded-full px-2.5 py-0.5 text-xs font-bold ${TONE[order.status]}`}>
                    {LABEL[order.status]}
                  </span>
                </p>
                <p className="text-sm text-bark/60">
                  {order.customerName} · {order.customerEmail} · {new Date(order.orderDate).toLocaleString()}
                </p>
              </div>
              <p className="text-xl font-bold">₹{Number(order.totalAmount)}</p>
            </div>

            <ul className="mt-3 text-sm text-bark/80">
              {order.items.map((item) => (
                <li key={item.id}>
                  {item.quantity} × {item.productName}
                  {item.productWeight ? ` (${item.productWeight})` : ""}
                </li>
              ))}
            </ul>
            <p className="mt-2 text-sm text-bark/55">
              {order.shippingAddress} · {order.paymentMethod === "online" ? "Online" : "Cash on delivery"} ·{" "}
              <span className={order.paymentStatus === "REFUND_PENDING" ? "font-bold text-clay-600" : ""}>
                {PAYMENT_LABEL[order.paymentStatus] ?? order.paymentStatus}
              </span>
            </p>
            {order.paymentStatus === "AWAITING_PAYMENT" && order.status === "PENDING" && (
              <p className="mt-2 text-sm text-bark/60">Can be packed once the customer pays.</p>
            )}
            {order.paymentStatus === "REFUND_PENDING" && (
              <button
                onClick={() => retryRefund(order)}
                className="mt-3 rounded-full border border-bark/15 px-4 py-2 text-sm font-medium hover:bg-bark/5"
              >
                Retry refund
              </button>
            )}

            {order.nextStatuses.length > 0 && (
              <div className="mt-4 flex flex-wrap gap-2">
                {order.nextStatuses.map((next) => (
                  <button
                    key={next}
                    onClick={() => move(order, next)}
                    className={
                      next === "CANCELLED"
                        ? "rounded-full border border-bark/15 px-4 py-2 text-sm font-medium text-clay-600 hover:bg-clay-100"
                        : "btn-primary px-4 py-2 text-sm"
                    }
                  >
                    {ACTION[next]}
                  </button>
                ))}
              </div>
            )}
          </li>
        ))}
      </ul>
    </section>
  );
}
