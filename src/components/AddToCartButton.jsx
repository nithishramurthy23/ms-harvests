import { Minus, Plus } from "lucide-react";
import { useCart } from "../context/CartContext";

// "Add" until the item is in the cart, then a - qty + stepper in the same spot,
// so people can build a basket without opening the cart.
export default function AddToCartButton({ item, size = "md" }) {
  const { dispatch, quantityOf } = useCart();
  const quantity = quantityOf(item.id);
  const height = size === "sm" ? "h-9 text-sm" : "h-11";

  if (quantity === 0) {
    return (
      <button
        type="button"
        onClick={() => dispatch({ type: "ADD_ITEM", payload: { ...item, quantity: 1 } })}
        aria-label={`Add ${item.name} to cart`}
        className={`${height} w-full rounded-full border border-green-700 font-medium text-green-700 transition-colors hover:bg-green-700 hover:text-white`}
      >
        Add
      </button>
    );
  }

  const setQuantity = (next) =>
    next < 1
      ? dispatch({ type: "REMOVE_ITEM", payload: item.id })
      : dispatch({ type: "UPDATE_QUANTITY", payload: { id: item.id, quantity: next } });

  return (
    <div className={`${height} flex w-full items-center justify-between rounded-full bg-green-700 text-white`}>
      <button
        type="button"
        onClick={() => setQuantity(quantity - 1)}
        aria-label={`Remove one ${item.name}`}
        className="flex h-full w-11 items-center justify-center rounded-full hover:bg-green-800"
      >
        <Minus className="h-4 w-4" />
      </button>
      <span className="font-medium tabular-nums" aria-live="polite">
        {quantity}
      </span>
      <button
        type="button"
        onClick={() => setQuantity(quantity + 1)}
        aria-label={`Add one more ${item.name}`}
        className="flex h-full w-11 items-center justify-center rounded-full hover:bg-green-800"
      >
        <Plus className="h-4 w-4" />
      </button>
    </div>
  );
}
