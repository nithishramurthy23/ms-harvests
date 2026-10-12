import { Mail, Phone, MapPin } from "lucide-react";
import { Link } from "react-router-dom";
import { categories } from "../data/catalog";

export default function Footer() {
  return (
    <footer className="bg-green-900 text-green-100">
      <div className="mx-auto max-w-7xl px-4 pb-24 pt-14 sm:px-6 sm:pb-10 lg:px-8">
        <div className="grid gap-10 md:grid-cols-[1.4fr_1fr_1fr_1.4fr]">
          <div>
            <p className="font-display text-2xl font-bold text-white">MS Fresh Harvest</p>
            <p className="mt-3 max-w-xs text-green-200">
              Produce, dairy and honey from the farms around Ooty, packed by hand and paid for at
              your door.
            </p>
          </div>

          <div>
            <p className="mb-3 font-medium text-white">Shop</p>
            <ul className="space-y-2 text-green-200">
              {categories.map((c) => (
                <li key={c.slug}>
                  <Link to={`/category/${c.slug}`} className="hover:text-white">
                    {c.name}
                  </Link>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <p className="mb-3 font-medium text-white">The shop</p>
            <ul className="space-y-2 text-green-200">
              <li><Link to="/about" className="hover:text-white">Our farms</Link></li>
              <li><Link to="/contact" className="hover:text-white">Contact</Link></li>
              <li><Link to="/orders" className="hover:text-white">Your orders</Link></li>
            </ul>
          </div>

          <div>
            <p className="mb-3 font-medium text-white">Talk to us</p>
            <ul className="space-y-3 text-green-200">
              <li>
                <a href="tel:+919487027142" className="flex items-center gap-2 hover:text-white">
                  <Phone className="h-4 w-4 flex-none" /> +91 94870 27142
                </a>
              </li>
              <li>
                <a href="mailto:msharvests@gmail.com" className="flex items-center gap-2 break-all hover:text-white">
                  <Mail className="h-4 w-4 flex-none" /> msharvests@gmail.com
                </a>
              </li>
              <li className="flex items-start gap-2">
                <MapPin className="mt-1 h-4 w-4 flex-none" />
                <span>2/445, Anikorai Village, Ooty, The Nilgiris 643206</span>
              </li>
            </ul>
          </div>
        </div>

        <p className="mt-12 border-t border-green-800 pt-6 text-sm text-green-300">
          © {new Date().getFullYear()} MS Fresh Harvest, Ooty.
        </p>
      </div>
    </footer>
  );
}
