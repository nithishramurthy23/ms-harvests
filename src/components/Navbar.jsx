import React, { useState, useRef, useEffect } from "react";
import logo from "../assets/logo.png";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { ShoppingBasket, Menu, X, User, LogOut } from "lucide-react";
import { useCart } from "../context/CartContext";
import { useAuth } from "../context/AuthContext";
import Cart from "./Cart";
import SearchBar from "./SearchBar";

export default function Navbar() {
  const [isOpen, setIsOpen] = useState(false);
  const [showCart, setShowCart] = useState(false);
  const [showUserDropdown, setShowUserDropdown] = useState(false);

  const cartContainerRef = useRef(null);
  const userDropdownRef = useRef(null);

  const location = useLocation();
  const navigate = useNavigate();
  const { state } = useCart();
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
    navigate("/");
    setShowUserDropdown(false);
  };

  const isActive = (path) => location.pathname === path;

  const navLinks = [
    { path: "/", label: "Home" },
    { path: "/products", label: "Products" },
    { path: "/about", label: "About" },
    { path: "/contact", label: "Contact" },
  ];

  // Close cart/user dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (e) => {
      if (
        cartContainerRef.current &&
        !cartContainerRef.current.contains(e.target)
      ) {
        setShowCart(false);
      }

      if (
        userDropdownRef.current &&
        !userDropdownRef.current.contains(e.target)
      ) {
        setShowUserDropdown(false);
      }
    };

    document.addEventListener("mousedown", handleClickOutside);
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, []);

  return (
    <nav className="bg-green-600 fixed w-full z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          <div className="flex items-center">
            <Link to="/" className="flex items-center gap-2">
              <img
                src={logo}
                alt="MS Farm Fresh Logo"
                className="w-10 h-auto rounded-full"
              />
              <span className="text-white text-xl font-bold">
                MS Fresh Harvest
              </span>
            </Link>
          </div>

          <div className="hidden md:block flex-1 max-w-md mx-8">
            <div className="bg-green-500 bg-opacity-50 rounded-lg">
              <SearchBar />
            </div>
          </div>

          <div className="hidden md:block">
            <div className="ml-10 flex items-baseline space-x-4">
              {navLinks.map((link) => (
                <Link
                  key={link.path}
                  to={link.path}
                  className={`px-3 py-2 rounded-md text-white ${
                    isActive(link.path) ? "bg-green-700" : "hover:bg-green-500"
                  }`}
                >
                  {link.label}
                </Link>
              ))}
            </div>
          </div>

          <div className="flex items-center gap-4">
            {user ? (
              <div
                className="relative"
                ref={userDropdownRef}
                onMouseEnter={() => setShowUserDropdown(true)}
                onMouseLeave={() => setShowUserDropdown(false)}
              >
                <button
                  className="text-white p-2 hover:bg-green-500 rounded-full flex items-center gap-2"
                  onClick={() => setShowUserDropdown((prev) => !prev)}
                >
                  <User className="h-6 w-6" />
                  <span className="hidden md:inline">{user.name}</span>
                </button>

                <div
                  className={`absolute right-0 w-48 mt-2 py-2 bg-white rounded-lg shadow-xl z-50 transition-all duration-200 ease-out
                  ${
                    showUserDropdown
                      ? "opacity-100 visible translate-y-0"
                      : "opacity-0 invisible -translate-y-1"
                  }
                `}
                >
                  <Link
                    to="/profile"
                    className="block px-4 py-2 text-gray-800 hover:bg-gray-100"
                    onClick={() => setShowUserDropdown(false)}
                  >
                    Profile
                  </Link>
                  <Link
                    to="/orders"
                    className="block px-4 py-2 text-gray-800 hover:bg-gray-100"
                    onClick={() => setShowUserDropdown(false)}
                  >
                    Orders
                  </Link>
                  <button
                    onClick={handleLogout}
                    className="block px-4 py-2 text-gray-800 hover:bg-gray-100 w-full text-left flex items-center gap-2"
                  >
                    <LogOut className="h-4 w-4" />
                    Logout
                  </button>
                </div>
              </div>
            ) : (
              <Link
                to="/auth"
                className="text-white hover:bg-green-500 px-4 py-2 rounded-md"
              >
                Login
              </Link>
            )}

            <div className="relative" ref={cartContainerRef}>
              <button
                onClick={() => setShowCart(!showCart)}
                className="text-white p-2 hover:bg-green-500 rounded-full relative"
              >
                <ShoppingBasket className="h-6 w-6" />
                {state.items.length > 0 && (
                  <span className="absolute -top-1 -right-1 bg-red-500 text-white text-xs w-5 h-5 rounded-full flex items-center justify-center">
                    {state.items.length}
                  </span>
                )}
              </button>

              {showCart && (
                <div className="absolute right-0 mt-2 w-80 z-50">
                  <Cart onClose={() => setShowCart(false)} />
                </div>
              )}
            </div>

            <div className="md:hidden">
              <button
                onClick={() => setIsOpen(!isOpen)}
                className="text-white hover:bg-green-500 p-2 rounded-md"
              >
                {isOpen ? (
                  <X className="h-6 w-6" />
                ) : (
                  <Menu className="h-6 w-6" />
                )}
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      {isOpen && (
        <div className="md:hidden">
          <div className="px-2 pt-2 pb-3 space-y-1 sm:px-3 bg-green-600">
            <div className="px-2 pb-3">
              <SearchBar />
            </div>
            {navLinks.map((link) => (
              <Link
                key={link.path}
                to={link.path}
                className={`block px-3 py-2 rounded-md text-white ${
                  isActive(link.path) ? "bg-green-700" : "hover:bg-green-500"
                }`}
                onClick={() => setIsOpen(false)}
              >
                {link.label}
              </Link>
            ))}
          </div>
        </div>
      )}
    </nav>
  );
}
