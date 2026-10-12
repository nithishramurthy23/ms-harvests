// Selling price, with the struck-through full price when there's an offer.
export default function Price({ price, mrp, className = "" }) {
  return (
    <span className={`inline-flex items-baseline gap-2 ${className}`}>
      <span className="font-bold text-bark">₹{price}</span>
      {mrp && mrp > price && (
        <span className="text-sm text-bark/45 line-through">₹{mrp}</span>
      )}
    </span>
  );
}
