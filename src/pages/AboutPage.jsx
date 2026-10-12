import { Banknote, MapPin, PackageCheck, Phone } from "lucide-react";
import { Link } from "react-router-dom";
import ProductImage from "../components/ProductImage";

const promises = [
  {
    icon: MapPin,
    title: "Grown nearby",
    description: "Our produce comes from farms in the hills around Ooty, not from a distant warehouse.",
  },
  {
    icon: PackageCheck,
    title: "Packed to order",
    description: "Nothing is packed before you order it, so what you get hasn't been sitting on a shelf.",
  },
  {
    icon: Banknote,
    title: "Cash on delivery",
    description: "You pay when your order arrives and you've seen it, never before.",
  },
  {
    icon: Phone,
    title: "A real person",
    description: "Questions about an order? Call or email us and you'll talk to the people who packed it.",
  },
];

export default function AboutPage() {
  return (
    <div className="page">
      <div className="grid items-center gap-10 py-6 md:grid-cols-2 md:py-10">
        <div>
          <p className="eyebrow mb-3">Our story</p>
          <h1 className="text-4xl font-medium leading-tight text-green-900 sm:text-5xl">
            From Anikorai village to your kitchen.
          </h1>
          <p className="mt-6 text-lg text-bark/70">
            MS Fresh Harvest began with a simple idea: everyone deserves access to fresh, healthy
            produce. We started by partnering with three local farms and have since grown to work
            with over 50 farmers in the region.
          </p>
          <p className="mt-4 text-lg text-bark/70">
            Today we serve families across the Nilgiris while staying close to the farms we buy
            from.
          </p>
        </div>
        <ProductImage
          src="https://im.hunt.in/cg/Ooty/City-Guide/Carrot-Farming-in-Ooty.jpg"
          alt="Carrot fields near Ooty"
          className="aspect-[4/3] w-full rounded-[2rem]"
        />
      </div>

      <h2 className="mb-6 mt-12 text-2xl text-green-900 sm:text-3xl">What you can count on</h2>
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {promises.map(({ icon: Icon, title, description }) => (
          <div key={title} className="rounded-2xl border border-bark/10 bg-paper p-6">
            <span className="mb-4 flex h-11 w-11 items-center justify-center rounded-full bg-green-100 text-green-700">
              <Icon className="h-5 w-5" />
            </span>
            <h3 className="font-sans text-lg font-medium">{title}</h3>
            <p className="mt-2 text-bark/65">{description}</p>
          </div>
        ))}
      </div>

      <div className="mt-12 text-center">
        <Link to="/products" className="btn-primary">
          Start shopping
        </Link>
      </div>
    </div>
  );
}
