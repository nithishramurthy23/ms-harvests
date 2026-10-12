import { Link } from "react-router-dom";
import { categories } from "../data/catalog";

export default function Categories() {
  return (
    <section className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
      <h2 className="mb-6 text-2xl text-green-900 sm:text-3xl">Shop by aisle</h2>
      <div className="grid grid-cols-3 gap-3 sm:gap-4 md:grid-cols-6">
        {categories.map(({ slug, name, icon: Icon, tint }) => (
          <Link
            key={slug}
            to={`/category/${slug}`}
            className="group flex flex-col items-center gap-3 rounded-2xl border border-bark/10 bg-paper px-2 py-5 text-center transition-colors hover:border-green-600"
          >
            <span className={`flex h-14 w-14 items-center justify-center rounded-full ${tint} transition-transform group-hover:scale-105`}>
              <Icon className="h-6 w-6" strokeWidth={1.75} />
            </span>
            <span className="text-sm font-medium leading-tight">{name}</span>
          </Link>
        ))}
      </div>
    </section>
  );
}
