import { Link } from "react-router-dom";
import { Banknote, PackageCheck, ShoppingBasket } from "lucide-react";
import Hero from "../components/Hero";
import Categories from "../components/Categories";
import FeaturedProducts from "../components/FeaturedProducts";

const steps = [
  {
    icon: ShoppingBasket,
    title: "Fill your basket",
    text: "Pick by the kilo or the bunch, or choose a ready-made weekly basket.",
  },
  {
    icon: PackageCheck,
    title: "We pack it in Ooty",
    text: "Your order is packed fresh and confirmed with a one-time code sent to your email.",
  },
  {
    icon: Banknote,
    title: "Pay when it arrives",
    text: "Cash on delivery. No cards, no wallets, nothing to pay up front.",
  },
];

export default function HomePage() {
  return (
    <>
      <Hero />
      <Categories />
      <FeaturedProducts />

      <section className="mx-auto max-w-7xl px-4 py-14 sm:px-6 lg:px-8">
        <h2 className="mb-8 text-2xl text-green-900 sm:text-3xl">How ordering works</h2>
        <ol className="grid gap-6 md:grid-cols-3">
          {steps.map(({ icon: Icon, title, text }, i) => (
            <li key={title} className="flex gap-4">
              <span className="flex h-12 w-12 flex-none items-center justify-center rounded-full bg-mustard-100 text-clay-600">
                <Icon className="h-5 w-5" />
              </span>
              <div>
                <p className="text-xs font-bold text-bark/40">Step {i + 1}</p>
                <h3 className="font-sans text-lg font-medium">{title}</h3>
                <p className="mt-1 text-bark/65">{text}</p>
              </div>
            </li>
          ))}
        </ol>
      </section>

      <section className="mx-auto mb-16 max-w-7xl px-4 sm:px-6 lg:px-8">
        <div className="rounded-[2rem] bg-green-800 px-6 py-12 text-center text-green-50 sm:px-12">
          <h2 className="mx-auto max-w-2xl text-3xl font-medium text-white sm:text-4xl">
            A small shop in Anikorai village, selling what the hills grow.
          </h2>
          <Link to="/about" className="mt-6 inline-block font-medium text-mustard-400 underline-offset-4 hover:underline">
            Read our story
          </Link>
        </div>
      </section>
    </>
  );
}
