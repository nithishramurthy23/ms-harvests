import { useCart } from '../context/CartContext';
import { ShoppingCart } from 'lucide-react';
import { bundles } from '../data/bundles';
import { calculateBundlePrice, calculateBundleWeight } from '../utils/bundleUtils';
import { getTodayDiscount } from '../utils/discounts';

// Featured items are the first four bundles, priced the same way as on the
// products page so the cart, checkout and server all agree.
const products = bundles.slice(0, 4);

export default function FeaturedProducts() {
  const { dispatch } = useCart();
  const todayDiscount = getTodayDiscount();
  const priceOf = (bundle) =>
    calculateBundlePrice(bundle.bundleItems, todayDiscount?.category, todayDiscount?.percent || 0);

  return (
    <section className="py-16 px-4 max-w-7xl mx-auto">
      <h2 className="text-3xl font-bold text-center mb-12">Featured Products</h2>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-8">
        {products.map((product) => (
          <div key={product.id} className="bg-white rounded-lg shadow-md overflow-hidden hover:shadow-xl transition-shadow">
            <div className="h-48 overflow-hidden">
              <img
                src={product.image}
                alt={product.name}
                className="w-full h-full object-cover hover:scale-110 transition-transform duration-300"
              />
            </div>
            <div className="p-4">
              <h3 className="font-semibold text-lg mb-2">{product.name}</h3>
              <p className="text-gray-500 text-sm">{calculateBundleWeight(product.bundleItems)}</p>
              <p className="text-green-600 font-bold">₹{priceOf(product)}</p>
              <button
                onClick={() => dispatch({ type: 'ADD_ITEM', payload: { ...product, price: priceOf(product) } })}
                className="mt-4 w-full bg-green-600 text-white py-2 rounded hover:bg-green-700 transition-colors flex items-center justify-center gap-2"
              >
                <ShoppingCart className="h-5 w-5" />
                Add to Cart
              </button>
            </div>
          </div>
        ))}
      </div>
    </section>
  );
}
