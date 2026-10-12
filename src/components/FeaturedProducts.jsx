import { Link } from "react-router-dom";
import { ArrowRight } from "lucide-react";
import ProductCard from "./ProductCard";
import { categoryName, pricedBundles, pricedProducts } from "../data/catalog";
import { getTodayDiscount } from "../utils/discounts";

// Picks for the home page: today's offer category if there is one, otherwise
// a mix from every aisle.
const WEEKEND_PICKS = [101, 202, 207, 601, 701, 703, 801, 1101];

export default function FeaturedProducts() {
  const offer = getTodayDiscount();
  const products = pricedProducts();
  const picks = offer
    ? products.filter((p) => p.category === offer.category).slice(0, 8)
    : WEEKEND_PICKS.map((id) => products.find((p) => p.id === id)).filter(Boolean);

  return (
    <>
      <section className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="mb-6 flex items-end justify-between gap-4">
          <div>
            <p className="eyebrow mb-2">{offer ? "Today's offer" : "This week"}</p>
            <h2 className="text-2xl text-green-900 sm:text-3xl">
              {offer ? `${categoryName(offer.category)}, ${offer.percent}% off until midnight` : "Picked for this week"}
            </h2>
          </div>
          <Link
            to={offer ? `/category/${offer.category}` : "/products"}
            className="hidden flex-none items-center gap-1 font-medium text-green-700 hover:text-green-900 sm:flex"
          >
            See all <ArrowRight className="h-4 w-4" />
          </Link>
        </div>
        <div className="grid grid-cols-2 gap-3 sm:gap-5 md:grid-cols-3 lg:grid-cols-4">
          {picks.map((item) => (
            <ProductCard key={item.id} item={item} />
          ))}
        </div>
      </section>

      <section className="bg-green-50/70 py-14">
        <div className="mx-auto max-w-7xl px-4 sm:px-6 lg:px-8">
          <p className="eyebrow mb-2">Less deciding, more cooking</p>
          <h2 className="mb-6 text-2xl text-green-900 sm:text-3xl">Weekly baskets</h2>
          <div className="grid grid-cols-2 gap-3 sm:gap-5 lg:grid-cols-5">
            {pricedBundles().map((item) => (
              <ProductCard key={item.id} item={item} />
            ))}
          </div>
        </div>
      </section>
    </>
  );
}
