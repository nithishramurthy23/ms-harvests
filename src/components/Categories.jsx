import React from 'react';
import { useNavigate } from 'react-router-dom';
import { 
  Carrot, Apple, Beef, Milk, Egg, Wheat, 
  Cookie, Flower, Cherry, Sandwich, FlaskConical, Leaf
} from 'lucide-react';

export const categories = [
  {
    name: "Vegetables",
    icon: Carrot,
    color: "text-green-500",
    description: "Fresh, seasonal vegetables",
    items: ["Root Vegetables","Leafy & Fresh", "Aromatic Staples", "Wholesome Variety"]
  },
  {
    name: "Fruits",
    icon: Apple,
    color: "text-red-500",
    description: "Sweet and juicy fruits",
    items: ["Berry & Jammy Picks", "Citrus & Stone Fruits", "Locally Grown", "Tropical Goodness"]
  },
  {
    name: "Herbs & Spices",
    icon: Leaf,
    color: "text-emerald-500",
    description: "Fresh and dried herbs",
    items: ["Coriander Leaves", "Curry Leaves", "Mint Leaves", "Essential Greens"]
  },
  {
    name: "Dairy Products",
    icon: Milk,
    color: "text-blue-400",
    description: "Fresh dairy and alternatives",
    items: ["Fresh Cow & Goat Milk", "Organic Curd", "Pure Ghee & Butter", "Farm-Fresh & Unadulterated"]
  },
  {
    name: "Eggs",
    icon: Egg,
    color: "text-amber-500",
    description: "Farm fresh eggs",
    items: ["Chicken Eggs", "Duck Eggs", "Country Chicken Eggs", "Egg Variety Pack"]
  },
  {
    name: "Honey & Jams",
    icon: Cherry,
    color: "text-amber-600",
    description: "Sweet natural preserves",
    items: ["Fruit Preserves & Raw Honey", "Raw Honey", "Fruit Jams", "Nature's Sweet Jar"]
  },
];

export default function Categories() {
  const navigate = useNavigate();

  return (
    <div className="py-16 bg-white">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <h2 className="text-3xl font-bold text-gray-900 mb-8">Shop by Category</h2>
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-6">
          {categories.map((category) => {
            const Icon = category.icon;
            return (
              <div
                key={category.name}
                onClick={() => navigate(`/category/${category.name.toLowerCase().replace(/\s+/g, '-')}`)}
                className="group bg-white p-6 rounded-lg shadow-sm hover:shadow-md transition-all duration-300 border border-gray-100 hover:border-green-500 cursor-pointer"
              >
                <div className="flex items-center space-x-4 mb-4">
                  <div className={`p-3 rounded-full bg-gray-50 group-hover:${category.color} transition-colors`}>
                    <Icon className={`w-6 h-6 ${category.color}`} />
                  </div>
                  <h3 className="text-lg font-semibold text-gray-900">{category.name}</h3>
                </div>
                <p className="text-sm text-gray-600 mb-4">{category.description}</p>
                <ul className="text-sm text-gray-500 space-y-1">
                  {category.items.map((item) => (
                    <li key={item} className="hover:text-green-600">
                      • {item}
                    </li>
                  ))}
                </ul>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
