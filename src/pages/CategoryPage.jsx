import { useParams } from "react-router-dom";
import { getCategory } from "../data/catalog";
import ProductsPage from "./ProductsPage";
import NotFoundPage from "./NotFoundPage";

export default function CategoryPage() {
  const { categoryName } = useParams();
  return getCategory(categoryName) ? <ProductsPage category={categoryName} /> : <NotFoundPage />;
}
