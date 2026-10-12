// src/pages/ProductsPage.js
import { ShoppingCart } from "lucide-react";
import { useNavigate } from "react-router-dom";
import { useCart } from "../context/CartContext";
import { categoryProducts } from "../data/products";
import { getTodayDiscount } from "../utils/discounts";
import {
  calculateBundlePrice,
  calculateBundleWeight,
} from "../utils/bundleUtils";

// Flattened product list for lookup
const allProducts = Object.values(categoryProducts).flat();

// Get today's discount info
const todayDiscount = getTodayDiscount();
const discountCategory = todayDiscount?.category;
const discountPercent = todayDiscount?.percent || 0;

// Define bundle list
export const bundles = [
  {
    id: 1,
    name: "Organic Vegetables Bundle",
    image:
      "https://thumbs.dreamstime.com/b/selection-includes-carrot-potato-cucumber-tomato-cabbage-lettuce-beetroot-onion-garlic-radish-dill-parsley-group-fresh-raw-243748746.jpg",
    category: "vegetables",
    bundleItems: [201, 202, 203, 204, 205, 206, 207, 208],
  },
  {
    id: 2,
    name: "Fresh Fruits Package",
    image:
      "https://static.vecteezy.com/system/resources/previews/000/737/013/large_2x/fruits-mango-lemon-plum-grape-pear-orange-apple-banana-strawberry-photo.jpg",
    category: "fruits",
    bundleItems: [101, 102, 103, 104, 105, 106, 108],
  },
  {
    id: 3,
    name: "Farm Fresh Eggs",
    image:
      "https://images.unsplash.com/photo-1582722872445-44dc5f7e3c8f?ixlib=rb-4.0.3",
    category: "eggs",
    bundleItems: [801, 802, 803, 804],
  },
  {
    id: 4,
    name: "Organic Honey",
    image: "https://images.unsplash.com/photo-1558642452-9d2a7deb7f62",
    category: "honey-&-jams",
    bundleItems: [1101, 1102, 1103, 1104, 1105],
  },
  {
    id: 5,
    name: "Fresh Milk",
    image:
      "https://images.unsplash.com/photo-1550583724-b2692b85b150?ixlib=rb-4.0.3",
    category: "dairy-products",
    bundleItems: [701, 702, 703, 704, 705, 706],
  },
];

export default function ProductsPage() {
  const navigate = useNavigate();
  const { dispatch } = useCart();

  const handleAddToCart = (bundle) => {
    const discountedPrice = calculateBundlePrice(
      bundle.bundleItems,
      discountCategory,
      discountPercent
    );
    dispatch({
      type: "ADD_ITEM",
      payload: {
        ...bundle,
        price: discountedPrice,
        quantity: 1,
      },
    });
  };

  return (
    <div className="py-24 px-4 max-w-7xl mx-auto">
      <h1 className="text-4xl font-bold mb-8">All Products</h1>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-8">
        {bundles.map((bundle) => {
          const originalPrice = bundle.bundleItems.reduce((total, itemId) => {
            const item = allProducts.find((p) => p.id === itemId);
            return total + (item?.price || 0);
          }, 0);

          const discountedPrice = calculateBundlePrice(
            bundle.bundleItems,
            discountCategory,
            discountPercent
          );
          const weight = calculateBundleWeight(bundle.bundleItems);
          const isDiscounted = discountedPrice < originalPrice;

          return (
            <div
              key={bundle.id}
              className="bg-white rounded-lg shadow-md overflow-hidden"
            >
              <div
                className="h-64 overflow-hidden cursor-pointer"
                onClick={() => navigate(`/product/${bundle.id}`)}
              >
                <img
                  src={bundle.image}
                  alt={bundle.name}
                  className="w-full h-full object-cover hover:scale-110 transition-transform duration-300"
                />
              </div>
              <div className="p-6">
                <div className="flex justify-between items-start mb-2">
                  <div>
                    <h3
                      className="font-semibold text-xl mb-1 cursor-pointer hover:text-green-600"
                      onClick={() => navigate(`/product/${bundle.id}`)}
                    >
                      {bundle.name}
                    </h3>
                    <span className="text-sm text-gray-500">
                      {bundle.category}
                    </span>
                    <p className="text-sm text-gray-500">{weight}</p>

                    <div className="mt-1">
                      {isDiscounted ? (
                        <>
                          <p className="text-lg">
                            <span className="text-gray-500 line-through mr-2">
                              ₹{originalPrice}
                            </span>
                            <span className="text-green-600 font-semibold">
                              ₹{discountedPrice}
                            </span>
                          </p>
                          <p className="text-sm text-green-600 font-medium">
                            🎉 Save{" "}
                            {Math.round(
                              ((originalPrice - discountedPrice) /
                                originalPrice) *
                                100
                            )}
                            %
                          </p>
                        </>
                      ) : (
                        <p className="text-green-600 font-semibold text-lg">
                          ₹{originalPrice}
                        </p>
                      )}
                    </div>
                  </div>
                  <span className="text-green-600 font-bold text-xl">
                    ₹{discountedPrice}
                  </span>
                </div>
                <button
                  onClick={() => handleAddToCart(bundle)}
                  className="mt-4 w-full bg-green-600 text-white py-3 rounded-lg hover:bg-green-700 transition-colors flex items-center justify-center gap-2"
                >
                  <ShoppingCart className="h-5 w-5" />
                  Add to Cart
                </button>
              </div>
            </div>
          );
        })}
      </div>
    </div>
  );
}
