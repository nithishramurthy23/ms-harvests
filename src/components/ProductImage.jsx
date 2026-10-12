import { useState } from "react";
import { Sprout } from "lucide-react";

// Product photo with a calm fallback tile, so a broken image link shows a
// placeholder instead of the browser's broken-image icon.
export default function ProductImage({ src, alt, className = "" }) {
  const [failed, setFailed] = useState(false);

  if (!src || failed) {
    return (
      <div
        role="img"
        aria-label={alt}
        className={`flex items-center justify-center bg-green-50 text-green-300 ${className}`}
      >
        <Sprout className="h-1/3 w-1/3 max-h-16 max-w-16" strokeWidth={1.25} />
      </div>
    );
  }

  return (
    <img
      src={src}
      alt={alt}
      loading="lazy"
      onError={() => setFailed(true)}
      className={`object-cover ${className}`}
    />
  );
}
