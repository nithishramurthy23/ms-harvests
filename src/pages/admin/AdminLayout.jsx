import { NavLink, Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { canManageStore, isOwner } from "../../utils/roles";

// Shell for the store dashboard: only admins and owners get in, and only
// owners see the Overview and People tabs.
export default function AdminLayout() {
  const { user, loading, isAuthenticated } = useAuth();

  if (loading) return <p className="page text-bark/60">Loading…</p>;
  if (!isAuthenticated) return <Navigate to="/auth" replace />;
  if (!canManageStore(user)) {
    return (
      <div className="page text-center">
        <h1 className="text-3xl text-green-900">Staff only</h1>
        <p className="mt-2 text-bark/65">This part of the site is for the shop&apos;s admins and owner.</p>
      </div>
    );
  }

  const tab = ({ isActive }) =>
    `flex-none rounded-full px-4 py-2 text-sm font-medium transition-colors ${
      isActive ? "bg-green-700 text-white" : "text-bark/75 hover:bg-bark/5"
    }`;

  return (
    <div className="page">
      <header className="mb-6 flex flex-wrap items-end justify-between gap-4">
        <div>
          <p className="eyebrow mb-2">{isOwner(user) ? "Owner" : "Admin"}</p>
          <h1 className="text-4xl font-medium text-green-900">Store</h1>
        </div>
        <nav className="-mx-1 flex gap-1 overflow-x-auto px-1" aria-label="Store sections">
          {isOwner(user) && (
            <NavLink to="/admin/overview" className={tab}>
              Overview
            </NavLink>
          )}
          <NavLink to="/admin/products" className={tab}>
            Products &amp; stock
          </NavLink>
          <NavLink to="/admin/orders" className={tab}>
            Orders
          </NavLink>
          {isOwner(user) && (
            <NavLink to="/admin/people" className={tab}>
              People
            </NavLink>
          )}
        </nav>
      </header>
      <Outlet />
    </div>
  );
}

// /admin lands owners on the overview and admins on products.
export function AdminHome() {
  const { user } = useAuth();
  return <Navigate to={isOwner(user) ? "/admin/overview" : "/admin/products"} replace />;
}
