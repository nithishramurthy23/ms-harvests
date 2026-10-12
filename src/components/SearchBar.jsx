import React, { useState } from 'react';
import { Search } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { categoryProducts } from '../data/products';

export default function SearchBar() {
  const [query, setQuery] = useState('');
  const [showResults, setShowResults] = useState(false);
  const navigate = useNavigate();

  const allProducts = Object.values(categoryProducts).flat();

  const filteredProducts = allProducts.filter(product =>
    product.name.toLowerCase().includes(query.toLowerCase())
  ).slice(0, 5);

  const handleSearch = (e) => {
    e.preventDefault();
    if (query) {
      navigate(`/search?q=${encodeURIComponent(query)}`);
      setShowResults(false);
    }
  };

  return (
    <div className="relative">
      <form onSubmit={handleSearch} className="relative">
        <input
          type="text"
          value={query}
          onChange={(e) => {
            setQuery(e.target.value);
            setShowResults(true);
          }}
          onFocus={() => setShowResults(true)}
          placeholder="Search products..."
          className="w-full px-4 py-2 pl-10 rounded-lg bg-transparent border-white border focus:outline-none focus:ring-2 focus:ring-green-300 text-white placeholder-white"
        />
        <Search className="absolute left-3 top-2.5 h-5 w-5 text-white" />
      </form>

      {showResults && query && (
        <div
          className="absolute z-50 w-full mt-1 bg-white rounded-lg shadow-lg border"
          onMouseDown={(e) => e.preventDefault()}
        >
          {filteredProducts.length > 0 ? (
            filteredProducts.map((product) => (
              <div
                key={product.id}
                className="p-2 hover:bg-gray-50 cursor-pointer"
                onClick={() => {
                  navigate(`/product/${product.id}`);
                  setShowResults(false);
                  setQuery('');
                }}
              >
                <div className="flex items-center gap-2">
                  <img
                    src={product.image}
                    alt={product.name}
                    className="w-8 h-8 object-cover rounded"
                  />
                  <span>{product.name}</span>
                </div>
              </div>
            ))
          ) : (
            <div className="p-2 text-gray-500">No products found</div>
          )}
        </div>
      )}
    </div>
  );
}
