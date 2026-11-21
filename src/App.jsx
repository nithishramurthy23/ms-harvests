// src/App.jsx

import { BrowserRouter as Router, Routes, Route, Navigate, useLocation } from "react-router-dom";
import Navbar from "./components/Navbar";
import Footer from "./components/Footer";
import HomePage from "./pages/HomePage";
import ProductsPage from "./pages/ProductsPage";
import AboutPage from "./pages/AboutPage";
import ContactPage from "./pages/ContactPage";
import CategoryPage from "./pages/CategoryPage";
import ProductPage from "./pages/ProductPage";
import AuthPage from "./pages/AuthPage";
import CheckoutPage from "./pages/CheckoutPage";
import ProfilePage from "./pages/ProfilePage";
import OrderHistoryPage from "./pages/OrderHistoryPage";
import ProfileEditPage from "./pages/ProfileEditPage";
import NotFoundPage from "./pages/NotFoundPage";
import AdminLayout, { AdminHome } from "./pages/admin/AdminLayout";
import AdminProductsPage from "./pages/admin/AdminProductsPage";
import AdminOrdersPage from "./pages/admin/AdminOrdersPage";
import { OwnerSummaryPage } from "./pages/admin/OwnerPages";
import PeoplePage from "./pages/admin/PeoplePage";
import ScrollToTop from "./components/ScrollToTop";

import { CartProvider } from "./context/CartContext";
import { AuthProvider } from "./context/AuthContext";
import { CatalogProvider } from "./context/CatalogContext";

// Old /search?q= links land on the shop page with the same query.
function SearchRedirect() {
  const { search } = useLocation();
  return <Navigate to={`/products${search}`} replace />;
}

// Top-level routes. Auth and cart state are provided to every page from here.
function App() {
  return (
    <AuthProvider>
      <CatalogProvider>
        <CartProvider>
          <Router>
            <ScrollToTop />
            <div className="min-h-screen flex flex-col">
              <Navbar />
              <main className="flex-grow">
                <Routes>
                  <Route path="/" element={<HomePage />} />
                  <Route path="/products" element={<ProductsPage />} />
                  <Route path="/about" element={<AboutPage />} />
                  <Route path="/contact" element={<ContactPage />} />
                  <Route
                    path="/category/:categoryName"
                    element={<CategoryPage />}
                  />
                  <Route path="/product/:productId" element={<ProductPage />} />
                  <Route path="/auth" element={<AuthPage />} />
                  <Route path="/checkout" element={<CheckoutPage />} />
                  <Route path="/profile" element={<ProfilePage />} />
                  <Route path="/orders" element={<OrderHistoryPage />} />
                  <Route path="/profile/edit" element={<ProfileEditPage />} />
                  <Route path="/search" element={<SearchRedirect />} />
                  <Route path="/login" element={<Navigate to="/auth" replace />} />
                  <Route path="/admin" element={<AdminLayout />}>
                    <Route index element={<AdminHome />} />
                    <Route path="overview" element={<OwnerSummaryPage />} />
                    <Route path="products" element={<AdminProductsPage />} />
                    <Route path="orders" element={<AdminOrdersPage />} />
                    <Route path="people" element={<PeoplePage />} />
                  </Route>
                  <Route path="*" element={<NotFoundPage />} />
                </Routes>
              </main>
              <Footer />
            </div>
          </Router>
        </CartProvider>
      </CatalogProvider>
    </AuthProvider>
  );
}

export default App;
