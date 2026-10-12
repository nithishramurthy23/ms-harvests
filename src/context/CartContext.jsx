import { createContext, useContext, useEffect, useReducer, useState } from "react";

// Shopping cart shared by all pages. It lives in the browser (saved to
// localStorage so a refresh keeps it); the server sees the items for the
// first time at checkout.
const STORAGE_KEY = "cart";
const CartContext = createContext(null);

// Every cart change goes through this reducer, which also keeps the running total.
const cartReducer = (state, action) => {
  switch (action.type) {
    case "ADD_ITEM": {
      const quantity = action.payload.quantity || 1;
      const existingItem = state.items.find(
        (item) => item.id === action.payload.id
      );
      if (existingItem) {
        return {
          ...state,
          items: state.items.map((item) =>
            item.id === action.payload.id
              ? { ...item, quantity: item.quantity + quantity }
              : item
          ),
          total: state.total + action.payload.price * quantity,
        };
      }
      return {
        ...state,
        items: [...state.items, { ...action.payload, quantity }],
        total: state.total + action.payload.price * quantity,
      };
    }
    case "REMOVE_ITEM": {
      const itemToRemove = state.items.find(
        (item) => item.id === action.payload
      );
      return {
        ...state,
        items: state.items.filter((item) => item.id !== action.payload),
        total:
          state.total -
          (itemToRemove ? itemToRemove.price * itemToRemove.quantity : 0),
      };
    }
    case "UPDATE_QUANTITY": {
      const item = state.items.find((item) => item.id === action.payload.id);
      if (!item) return state;
      const quantityDiff = action.payload.quantity - item.quantity;
      return {
        ...state,
        items: state.items.map((item) =>
          item.id === action.payload.id
            ? { ...item, quantity: action.payload.quantity }
            : item
        ),
        total: state.total + item.price * quantityDiff,
      };
    }
    case "CLEAR_CART":
      return { items: [], total: 0 };
    default:
      return state;
  }
};

const loadCart = () => {
  try {
    const saved = JSON.parse(localStorage.getItem(STORAGE_KEY));
    if (saved && Array.isArray(saved.items)) return saved;
  } catch {
    // ignore a corrupt or unavailable cart and start empty
  }
  return { items: [], total: 0 };
};

export const CartProvider = ({ children }) => {
  const [state, dispatch] = useReducer(cartReducer, undefined, loadCart);
  const [isOpen, setIsOpen] = useState(false);

  useEffect(() => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
      // private mode or storage full: the cart still works for this visit
    }
  }, [state]);

  const itemCount = state.items.reduce((n, item) => n + item.quantity, 0);
  const quantityOf = (id) => state.items.find((item) => item.id === id)?.quantity || 0;

  return (
    <CartContext.Provider
      value={{
        state,
        dispatch,
        itemCount,
        quantityOf,
        isOpen,
        openCart: () => setIsOpen(true),
        closeCart: () => setIsOpen(false),
      }}
    >
      {children}
    </CartContext.Provider>
  );
};

export const useCart = () => {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error("useCart must be used within a CartProvider");
  }
  return context;
};
