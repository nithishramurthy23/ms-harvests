import { Link } from "react-router-dom";
import { Sprout } from "lucide-react";

export default function NotFoundPage() {
  return (
    <div className="page flex flex-col items-center py-24 text-center">
      <Sprout className="h-14 w-14 text-green-300" strokeWidth={1.25} />
      <h1 className="mt-4 text-3xl font-medium text-green-900">This page didn&apos;t sprout.</h1>
      <p className="mt-2 text-bark/65">The link may be old, or the item is out of season.</p>
      <Link to="/products" className="btn-primary mt-8">
        Back to the shop
      </Link>
    </div>
  );
}
