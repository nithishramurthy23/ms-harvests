import { Link } from "react-router-dom";
import { getTodayDiscount } from "../utils/discounts";
import { categoryName } from "../data/catalog";
import { useAuth } from "../context/AuthContext";
import ProductImage from "./ProductImage";

const HERO_IMAGE = "https://im.hunt.in/cg/Ooty/City-Guide/Carrot-Farming-in-Ooty.jpg";

export default function Hero() {
  const { user } = useAuth();
  const offer = getTodayDiscount();

  return (
    <section className="mx-auto grid max-w-7xl items-center gap-10 px-4 pb-12 pt-8 sm:px-6 md:grid-cols-[1.05fr_1fr] md:pt-14 lg:px-8">
      <div>
        <p className="eyebrow mb-4">
          {user ? `Welcome back, ${user.name?.split(" ")[0]}` : "From the Nilgiris hills"}
        </p>
        <h1 className="text-[2.6rem] font-medium leading-[1.05] text-green-900 sm:text-5xl lg:text-[4rem]">
          Vegetables, fruit and dairy from farms around Ooty.
        </h1>
        <p className="mt-5 max-w-lg text-lg text-bark/70">
          Buy by the kilo or pick a weekly basket. We pack your order in Ooty and you pay cash
          when it reaches your door.
        </p>
        <div className="mt-8 flex flex-wrap gap-3">
          <Link to="/products" className="btn-primary">
            Shop the harvest
          </Link>
          <Link to="/products#baskets" className="btn-ghost">
            Weekly baskets
          </Link>
        </div>
      </div>

      <div className="relative">
        <ProductImage
          src={HERO_IMAGE}
          alt="Carrot fields on a hillside near Ooty"
          className="aspect-[4/3] w-full rounded-[2rem] md:aspect-[5/6] lg:aspect-[4/3]"
        />
        {offer && (
          <Link
            to={`/category/${offer.category}`}
            className="absolute -bottom-5 left-4 right-4 flex items-center gap-4 rounded-2xl bg-paper p-4 shadow-xl ring-1 ring-bark/5 transition-transform hover:-translate-y-0.5 sm:left-6 sm:right-auto"
          >
            <span className="flex h-14 w-14 flex-none flex-col items-center justify-center rounded-full bg-clay-500 font-bold leading-none text-white">
              <span className="text-lg">{offer.percent}%</span>
              <span className="text-[10px] uppercase">off</span>
            </span>
            <span>
              <span className="block text-xs uppercase tracking-wider text-bark/50">Today only</span>
              <span className="block font-medium">
                {offer.percent}% off all {categoryName(offer.category).toLowerCase()}
              </span>
            </span>
          </Link>
        )}
      </div>
    </section>
  );
}
