import React, { useState, useRef, useEffect } from "react";
import logo from "../assets/logo.png";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { ShoppingBasket, Menu, X } from "lucide-react";
import { useCart } from "./CartContext";
import Cart from "../components/Cart";
import SearchBar from "../components/SearchBar";
import UserDropdown from "./UserDropdown";

export default function Navbar() {
  const [isOpen, setIsOpen] = useState(false);
  const [showCart, setShowCart] = useState(false);
  const cartContainerRef = useRef(null);
  const location = useLocation();
  const { state } = useCart();

  const isActive = (path) => location.pathname === path;

  const navLinks = [
    { path: "/", label: "Home" },
    { path: "/products", label: "Products" },
    { path: "/about", label: "About" },
    { path: "/contact", label: "Contact" },
  ];

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (
        cartContainerRef.current &&
        !cartContainerRef.current.contains(e.target)
      ) {
        setShowCart(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  return (
    <nav className="bg-green-600 fixed w-full z-50">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
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

          {/* Desktop Search */}
          <div className="hidden md:block flex-1 max-w-md mx-8">
            <div className="bg-green-500 bg-opacity-50 rounded-lg">
              <SearchBar />
            </div>
          </div>

          {/* Desktop Nav Links */}
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

          {/* Right Controls */}
          <div className="flex items-center gap-4">
            {/* User Dropdown */}
            <UserDropdown />

            {/* Cart */}
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

            {/* Mobile Menu Button */}
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

      {/* Mobile Nav Menu */}
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
