import { useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { ShoppingCart, ArrowLeft, Plus, Minus } from "lucide-react";
import { categoryProducts } from "../data/products";
import { useCart } from "../context/CartContext";
import { bundles } from "../data/bundles";
import {
  calculateBundlePrice,
  calculateBundleWeight,
} from "../utils/bundleUtils";
import { getTodayDiscount } from "../utils/discounts";

export default function ProductPage() {
  const { productId } = useParams();
  const navigate = useNavigate();
  const { dispatch } = useCart();
  const [quantity, setQuantity] = useState(1);

  const todayDiscount = getTodayDiscount();
  const discountCategory = todayDiscount?.category;
  const discountPercent = todayDiscount?.percent || 0;

  const allProducts = [...Object.values(categoryProducts).flat(), ...bundles];
  const product = allProducts.find((p) => p.id === Number(productId));

  if (!product) {
    return (
      <div className="py-24 px-4 text-center">
        <h1 className="text-2xl font-bold text-gray-900">Product not found</h1>
      </div>
    );
  }

  const isBundle = product.bundleItems && product.bundleItems.length > 0;

  const discountedPrice =
    !isBundle && product.category?.toLowerCase() === discountCategory
      ? Math.round(product.price - (product.price * discountPercent) / 100)
      : null;

  const effectivePrice = isBundle
    ? calculateBundlePrice(
        product.bundleItems,
        discountCategory,
        discountPercent
      )
    : discountedPrice || product.price;

  const displayWeight = isBundle
    ? calculateBundleWeight(product.bundleItems)
    : product.weight;

  const totalPrice = effectivePrice * quantity;

  const bundledProducts = isBundle
    ? product.bundleItems
        .map((id) => allProducts.find((p) => p.id === id))
        .filter(Boolean)
    : [];

  const handleAddToCart = () => {
    dispatch({
      type: "ADD_ITEM",
      payload: {
        ...product,
        isBundle,
        price: effectivePrice,
        quantity,
      },
    });
  };

  return (
    <div className="py-24 px-4">
      <div className="max-w-7xl mx-auto">
        <button
          onClick={() => navigate(-1)}
          className="flex items-center gap-2 text-green-600 hover:text-green-700 mb-8"
        >
          <ArrowLeft className="w-5 h-5" />
          Back
        </button>

        <div className="grid md:grid-cols-2 gap-12">
          <div className="aspect-square overflow-hidden rounded-lg shadow-lg">
            <img
              src={product.image}
              alt={product.name}
              className="w-full h-full object-cover hover:scale-105 transition-transform duration-300"
            />
          </div>

          <div className="space-y-8">
            <div>
              <h1 className="text-4xl font-bold mb-4">{product.name}</h1>
              <div className="flex items-baseline gap-2 mb-4">
                <div className="text-3xl font-bold">
                  {discountedPrice && !isBundle ? (
                    <div className="flex items-center gap-3">
                      <span className="text-gray-500 line-through">
                        ₹{product.price}
                      </span>
                      <span className="text-green-600">₹{discountedPrice}</span>
                    </div>
                  ) : (
                    <span className="text-green-600">₹{effectivePrice}</span>
                  )}
                </div>

                {quantity > 1 && (
                  <span className="text-gray-500 text-sm">
                    ({quantity} × ₹{effectivePrice})
                  </span>
                )}
              </div>
              <p className="text-gray-600 text-lg mb-2">
                Weight: {displayWeight}
              </p>
            </div>

            <div className="border-t border-b py-6">
              <h2 className="text-xl font-semibold mb-4">
                Product Description
              </h2>
              <p className="text-gray-600 leading-relaxed">
                {isBundle ? (
                  <>
                    Our <strong>{product.name}</strong> includes a curated
                    selection of fresh, high-quality items sourced from trusted
                    local farms. This bundle offers great value and variety for
                    your daily meals.
                  </>
                ) : (
                  <>
                    Our <strong>{product.name}</strong> is carefully selected
                    from local farms to ensure the highest quality. Each product
                    undergoes strict quality control to maintain freshness and
                    nutritional value. Perfect for your daily cooking needs and
                    healthy lifestyle.
                  </>
                )}
              </p>
            </div>

            <div className="space-y-6">
              <div className="flex items-center gap-4">
                <span className="text-lg font-medium">Quantity:</span>
                <div className="flex items-center gap-4 border rounded-lg p-2">
                  <button
                    onClick={() => setQuantity(Math.max(1, quantity - 1))}
                    className="p-1 hover:bg-gray-100 rounded-full"
                  >
                    <Minus className="w-5 h-5" />
                  </button>
                  <span className="w-12 text-center text-lg">{quantity}</span>
                  <button
                    onClick={() => setQuantity(quantity + 1)}
                    className="p-1 hover:bg-gray-100 rounded-full"
                  >
                    <Plus className="w-5 h-5" />
                  </button>
                </div>
              </div>

              <button
                onClick={handleAddToCart}
                className="w-full bg-green-600 text-white py-4 rounded-lg hover:bg-green-700 flex items-center justify-center gap-3 text-lg font-medium"
              >
                <ShoppingCart className="w-6 h-6" />
                Add to Cart (₹{totalPrice})
              </button>
            </div>

            <div className="bg-green-50 p-6 rounded-lg">
              <h3 className="font-semibold mb-2">Product Features:</h3>
              <ul className="list-disc list-inside space-y-2 text-gray-600">
                <li>100% Organic</li>
                <li>Locally Sourced</li>
                <li>Fresh Harvest</li>
                <li>Quality Guaranteed</li>
              </ul>
            </div>
          </div>
        </div>

        {isBundle && bundledProducts.length > 0 && (
          <div className="max-w-7xl mx-auto mt-12">
            <h2 className="text-2xl font-semibold mb-6">
              Included in this Bundle:
            </h2>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
              {bundledProducts.map((item) => (
                <div
                  key={item.id}
                  className="border rounded-lg p-4 hover:shadow-md transition-shadow"
                >
                  <img
                    src={item.image}
                    alt={item.name}
                    className="w-full h-32 object-cover rounded-md mb-2"
                  />
                  <h3 className="font-medium">{item.name}</h3>
                  <p className="text-sm text-gray-600">{item.weight}</p>
                  <p className="text-green-600 font-medium">₹{item.price}</p>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
