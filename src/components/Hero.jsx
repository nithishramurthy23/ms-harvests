import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import { X } from "lucide-react";
import { getTodayDiscount, applyDiscountToList } from "../utils/discounts";
import { categoryProducts } from "../data/products";

export default function Hero() {
  const navigate = useNavigate();
  const [showOffer, setShowOffer] = useState(true);
  const [minimized, setMinimized] = useState(false);
  const [highlightProduct, setHighlightProduct] = useState(null);

  useEffect(() => {
    const todayDiscount = getTodayDiscount();
    if (!todayDiscount) return;

    const discountedProducts = applyDiscountToList(
      categoryProducts[todayDiscount.category] || []
    );
    if (discountedProducts.length > 0) {
      setHighlightProduct(discountedProducts[0]); // You could randomize or cycle here
    }
  }, []);

  return (
    <div className="relative h-[600px] flex items-center justify-center">
      {/* 🌄 Background */}
      <div
        className="absolute inset-0 z-0"
        style={{
          backgroundImage:
            'url("https://im.hunt.in/cg/Ooty/City-Guide/Carrot-Farming-in-Ooty.jpg")',
          backgroundSize: "cover",
          backgroundPosition: "center",
        }}
      >
        <div className="absolute inset-0 bg-black opacity-40"></div>
      </div>

      {/* 🧾 Minimized Button */}
      {minimized && (
        <div className="absolute bottom-4 left-4 z-20">
          <button
            onClick={() => {
              setShowOffer(true);
              setMinimized(false);
            }}
            className="bg-green-600 text-white px-4 py-2 rounded-full shadow-md hover:bg-green-700 transition"
          >
            🌟 Show Today&apos;s Deal
          </button>
        </div>
      )}

      {/* 💬 Offer Card */}
      {showOffer && highlightProduct && (
        <div className="relative z-10 text-center px-4">
          <h1 className="text-4xl md:text-6xl font-bold text-white mb-6">
            From Harvest to Your Table, Fresh Every Time.
          </h1>
          <p className="text-xl text-white mb-8 max-w-2xl mx-auto">
            Discover nature&apos;s finest selection of organic produce, delivered
            fresh to your doorstep.
          </p>

          <div className="relative bg-white p-4 rounded-lg shadow-md max-w-sm mx-auto mb-6">
            {/* ❌ Close Button */}
            <button
              onClick={() => {
                setShowOffer(false);
                setMinimized(true);
              }}
              className="absolute top-2 right-2 text-gray-400 hover:text-red-500"
              aria-label="Close"
            >
              <X size={16} />
            </button>

            <div className="text-sm text-red-600 font-semibold mb-1">
              🔥 {highlightProduct.name} Discount!
            </div>
            <div className="flex items-center gap-2">
              <img
                src={highlightProduct.image}
                alt={highlightProduct.name}
                className="w-16 h-16 object-cover rounded"
              />
              <div>
                <p className="text-gray-700 font-semibold">
                  {highlightProduct.name}
                </p>
                <p className="text-gray-500 line-through">
                  ₹{highlightProduct.price}
                </p>
                <p className="text-green-600 font-bold">
                  ₹{highlightProduct.offerPrice}
                </p>
              </div>
            </div>
            <button
              onClick={() => navigate(`/product/${highlightProduct.id}`)}
              className="mt-4 w-full bg-green-600 text-white py-2 rounded hover:bg-green-700 transition-colors text-sm"
            >
              Buy Now
            </button>
          </div>

          {/* 🛍️ Shop Now Button */}
          <button
            onClick={() => navigate("/products")}
            className="bg-green-600 text-white px-8 py-3 rounded-full text-lg font-semibold hover:bg-green-700 transition-colors"
          >
            Shop Now
          </button>
        </div>
      )}
    </div>
  );
}
