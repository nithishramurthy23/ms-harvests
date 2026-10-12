import { useEffect, useRef } from "react";
import { useCart } from "../context/CartContext";
import { X, Plus, Minus, ShoppingBasket } from "lucide-react";
import { useNavigate } from "react-router-dom";
import ProductImage from "./ProductImage";

// Contents of the cart drawer: line items with quantity controls, the total
// and the way to checkout.
export default function Cart({ onClose }) {
  const { state, dispatch } = useCart();
  const navigate = useNavigate();
  const cartRef = useRef();

  const updateQuantity = (id, quantity) => {
    if (quantity < 1) {
      dispatch({ type: "REMOVE_ITEM", payload: id });
      return;
    }
    dispatch({ type: "UPDATE_QUANTITY", payload: { id, quantity } });
  };

  // Close on outside click and on Escape
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (cartRef.current && !cartRef.current.contains(e.target)) {
        onClose?.();
      }
    };

    const handleEscape = (e) => {
      if (e.key === "Escape") {
        onClose?.();
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    document.addEventListener("keydown", handleEscape);

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      document.removeEventListener("keydown", handleEscape);
    };
  }, [onClose]);

  const goTo = (path) => {
    navigate(path);
    onClose?.();
  };

  return (
    <div ref={cartRef} className="flex h-full flex-col bg-paper">
      <div className="flex items-center justify-between border-b border-bark/10 px-5 py-4">
        <h2 className="text-xl">Your basket</h2>
        <button
          onClick={() => onClose?.()}
          aria-label="Close cart"
          className="rounded-full p-2 text-bark/60 hover:bg-bark/5"
        >
          <X className="h-5 w-5" />
        </button>
      </div>

      {state.items.length === 0 ? (
        <div className="flex flex-1 flex-col items-center justify-center gap-3 px-8 text-center">
          <ShoppingBasket className="h-12 w-12 text-green-300" strokeWidth={1.25} />
          <p className="font-medium">Your cart is empty</p>
          <p className="text-sm text-bark/60">Vegetables, fruit and dairy from the hills are a tap away.</p>
          <button onClick={() => goTo("/products")} className="btn-primary mt-2">
            Browse the shop
          </button>
        </div>
      ) : (
        <>
          <ul className="flex-1 divide-y divide-bark/10 overflow-y-auto px-5">
            {state.items.map((item) => (
              <li key={item.id} className="flex gap-4 py-4">
                <ProductImage src={item.image} alt={item.name} className="h-16 w-16 flex-none rounded-xl" />
                <div className="min-w-0 flex-1">
                  <div className="flex items-start justify-between gap-2">
                    <h3 className="font-sans font-medium leading-snug">{item.name}</h3>
                    <button
                      onClick={() => dispatch({ type: "REMOVE_ITEM", payload: item.id })}
                      aria-label={`Remove ${item.name}`}
                      className="-mr-1 rounded-full p-1 text-bark/40 hover:bg-bark/5 hover:text-bark"
                    >
                      <X className="h-4 w-4" />
                    </button>
                  </div>
                  <p className="text-sm text-bark/55">
                    {item.weight ? `${item.weight} · ` : ""}₹{item.price} each
                  </p>
                  <div className="mt-2 inline-flex items-center rounded-full border border-bark/15">
                    <button
                      onClick={() => updateQuantity(item.id, item.quantity - 1)}
                      aria-label={`Decrease ${item.name}`}
                      className="rounded-full p-1.5 hover:bg-bark/5"
                    >
                      <Minus className="h-4 w-4" />
                    </button>
                    <span className="w-8 text-center text-sm font-medium tabular-nums">{item.quantity}</span>
                    <button
                      onClick={() => updateQuantity(item.id, item.quantity + 1)}
                      aria-label={`Increase ${item.name}`}
                      className="rounded-full p-1.5 hover:bg-bark/5"
                    >
                      <Plus className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </li>
            ))}
          </ul>

          <div className="space-y-3 border-t border-bark/10 bg-cream px-5 py-5">
            <div className="flex items-baseline justify-between">
              <span className="text-bark/70">Total</span>
              <span className="text-xl font-bold">₹{state.total}</span>
            </div>
            <p className="text-xs text-bark/55">Cash on delivery. You pay when the order reaches you.</p>
            <button onClick={() => goTo("/checkout")} className="btn-primary w-full">
              Proceed to Checkout
            </button>
          </div>
        </>
      )}
    </div>
  );
}
