import { useState, useRef, useEffect } from "react";
import logo from "../assets/logo.png";
import { Link, NavLink, useLocation, useNavigate } from "react-router-dom";
import { ShoppingBasket, Menu, X, User, LogOut, Package } from "lucide-react";
import { useCart } from "../context/CartContext";
import { useAuth } from "../context/AuthContext";
import Cart from "./Cart";
import SearchBar from "./SearchBar";

const navLinks = [
  { path: "/products", label: "Shop" },
  { path: "/category/vegetables", label: "Vegetables" },
  { path: "/category/fruits", label: "Fruits" },
  { path: "/about", label: "Our farms" },
  { path: "/contact", label: "Contact" },
];

export default function Navbar() {
  const [menuOpen, setMenuOpen] = useState(false);
  const [showUserDropdown, setShowUserDropdown] = useState(false);
  const userDropdownRef = useRef(null);

  const location = useLocation();
  const navigate = useNavigate();
  const { state, itemCount, isOpen: cartOpen, openCart, closeCart } = useCart();
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
    navigate("/");
    setShowUserDropdown(false);
  };

  // Close menus when the page changes
  useEffect(() => {
    setMenuOpen(false);
    setShowUserDropdown(false);
  }, [location.pathname]);

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (userDropdownRef.current && !userDropdownRef.current.contains(e.target)) {
        setShowUserDropdown(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Stop the page scrolling behind the open cart drawer
  useEffect(() => {
    document.body.style.overflow = cartOpen ? "hidden" : "";
    return () => {
      document.body.style.overflow = "";
    };
  }, [cartOpen]);

  const linkClass = ({ isActive }) =>
    `rounded-full px-3 py-2 text-[15px] transition-colors ${
      isActive ? "bg-green-100 font-medium text-green-800" : "text-bark/75 hover:text-bark"
    }`;

  return (
    <>
      <div className="bg-green-800 px-4 py-2 text-center text-xs text-green-50 sm:text-sm">
        Grown in the Nilgiris · Packed in Ooty · Cash on delivery
      </div>

      <header className="sticky top-0 z-40 border-b border-bark/10 bg-paper/95 backdrop-blur">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <div className="flex h-16 items-center gap-4 lg:gap-8">
            <Link to="/" className="flex flex-none items-center gap-2.5">
              <img src={logo} alt="" className="h-10 w-10 rounded-full ring-1 ring-bark/10" />
              <span className="leading-none">
                <span className="block font-display text-lg font-bold text-green-800 sm:text-xl">
                  MS Fresh Harvest
                </span>
                <span className="hidden text-[11px] uppercase tracking-[0.16em] text-bark/50 sm:block">
                  Ooty · The Nilgiris
                </span>
              </span>
            </Link>

            <div className="hidden max-w-md flex-1 md:block">
              <SearchBar />
            </div>

            <nav className="hidden items-center gap-1 lg:flex" aria-label="Main">
              {navLinks.map((link) => (
                <NavLink key={link.path} to={link.path} className={linkClass}>
                  {link.label}
                </NavLink>
              ))}
            </nav>

            <div className="ml-auto flex items-center gap-1 sm:gap-2">
              {user ? (
                <div className="relative" ref={userDropdownRef}>
                  <button
                    className="flex items-center gap-2 rounded-full p-2 text-bark/80 hover:bg-bark/5"
                    onClick={() => setShowUserDropdown((prev) => !prev)}
                    aria-expanded={showUserDropdown}
                    aria-label="Your account"
                  >
                    <User className="h-5 w-5" />
                    <span className="hidden max-w-[8rem] truncate text-sm md:inline">
                      {user.name?.split(" ")[0]}
                    </span>
                  </button>
                  {showUserDropdown && (
                    <div className="absolute right-0 z-50 mt-2 w-48 overflow-hidden rounded-xl border border-bark/10 bg-paper py-1 shadow-lg">
                      <Link to="/profile" className="flex items-center gap-2 px-4 py-2 text-sm hover:bg-green-50">
                        <User className="h-4 w-4" /> Profile
                      </Link>
                      <Link to="/orders" className="flex items-center gap-2 px-4 py-2 text-sm hover:bg-green-50">
                        <Package className="h-4 w-4" /> Orders
                      </Link>
                      <button
                        onClick={handleLogout}
                        className="flex w-full items-center gap-2 px-4 py-2 text-left text-sm hover:bg-green-50"
                      >
                        <LogOut className="h-4 w-4" /> Log out
                      </button>
                    </div>
                  )}
                </div>
              ) : (
                <Link to="/auth" className="whitespace-nowrap rounded-full px-3 py-2 text-sm font-medium text-bark/80 hover:bg-bark/5">
                  Log in
                </Link>
              )}

              <button
                onClick={openCart}
                aria-label={`Open cart, ${itemCount} items`}
                className="flex items-center gap-2 rounded-full bg-green-700 py-2 pl-3 pr-4 text-white transition-colors hover:bg-green-800"
              >
                <ShoppingBasket className="h-5 w-5" />
                <span key={itemCount} className="text-sm font-bold tabular-nums motion-safe:animate-pop">
                  {itemCount}
                </span>
                {state.total > 0 && (
                  <span className="hidden border-l border-white/30 pl-2 text-sm sm:inline">₹{state.total}</span>
                )}
              </button>

              <button
                onClick={() => setMenuOpen((open) => !open)}
                aria-label={menuOpen ? "Close menu" : "Open menu"}
                aria-expanded={menuOpen}
                className="rounded-full p-2 text-bark/80 hover:bg-bark/5 lg:hidden"
              >
                {menuOpen ? <X className="h-6 w-6" /> : <Menu className="h-6 w-6" />}
              </button>
            </div>
          </div>

          {/* Search stays visible on phones: it's how most grocery visits start */}
          <div className="pb-3 md:hidden">
            <SearchBar />
          </div>
        </div>

        {menuOpen && (
          <nav className="border-t border-bark/10 bg-paper px-4 py-3 lg:hidden" aria-label="Mobile">
            {navLinks.map((link) => (
              <NavLink
                key={link.path}
                to={link.path}
                className={({ isActive }) =>
                  `block rounded-xl px-3 py-3 ${isActive ? "bg-green-100 font-medium text-green-800" : "text-bark/80"}`
                }
              >
                {link.label}
              </NavLink>
            ))}
          </nav>
        )}
      </header>

      {cartOpen && (
        <div className="fixed inset-0 z-50" role="dialog" aria-modal="true" aria-label="Cart">
          <div className="absolute inset-0 bg-bark/40" />
          <div className="absolute inset-y-0 right-0 w-full max-w-md shadow-2xl motion-safe:animate-slide-in">
            <Cart onClose={closeCart} />
          </div>
        </div>
      )}

      {/* Phones: a sticky bar to reach the basket from anywhere */}
      {itemCount > 0 && !cartOpen && location.pathname !== "/checkout" && (
        <button
          onClick={openCart}
          className="fixed inset-x-3 bottom-3 z-30 flex items-center justify-between rounded-2xl bg-green-800 px-5 py-3.5 text-white shadow-xl sm:hidden"
        >
          <span className="text-sm">
            {itemCount} {itemCount === 1 ? "item" : "items"} · <strong>₹{state.total}</strong>
          </span>
          <span className="flex items-center gap-1 font-medium">
            View basket <ShoppingBasket className="h-4 w-4" />
          </span>
        </button>
      )}
    </>
  );
}
