import { useEffect, useState } from "react";
import api from "../api/axiosInstance";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { Package, Truck, CheckCircle, XCircle } from "lucide-react"; // Icons for status

// Lists the logged-in user's past orders.
export default function OrderHistoryPage() {
  const { token, isAuthenticated, logout } = useAuth();
  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const navigate = useNavigate();

  useEffect(() => {
    const fetchOrders = async () => {
      if (!isAuthenticated || !token) {
        setLoading(false);
        setError("Please log in to view your orders.");
        return;
      }

      try {
        setLoading(true);
        // The shared api client adds the auth token automatically
        const response = await api.get("/orders/history");
        setOrders(response.data);
      } catch (err) {
        console.error("Failed to fetch order history:", err);
        setError(
          err.response?.data?.message || "Failed to load order history."
        );
        if (err.response?.status === 401) {
          logout(); // Log out if token is expired/invalid
          navigate("/auth"); // Redirect to auth page
        }
      } finally {
        setLoading(false);
      }
    };

    fetchOrders();
  }, [isAuthenticated, token, logout, navigate]); // Re-fetch if auth state changes

  if (!isAuthenticated) {
    return (
      <div className="py-24 px-4 text-center">
        <h1 className="text-2xl font-bold mb-4">You are not logged in.</h1>
        <p className="text-gray-600 mb-4">
          Please log in to view your order history.
        </p>
        <button
          onClick={() => navigate("/auth")}
          className="text-green-600 hover:text-green-700 font-medium"
        >
          Go to Login
        </button>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="py-24 px-4 text-center">
        <h1 className="text-2xl font-bold text-gray-900">
          Loading Order History...
        </h1>
        <p className="text-gray-600">Please wait.</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="py-24 px-4 text-center">
        <h1 className="text-2xl font-bold text-red-600">
          Error Loading Orders
        </h1>
        <p className="text-red-500">{error}</p>
        <button
          onClick={() => window.location.reload()}
          className="mt-4 px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
        >
          Try Again
        </button>
      </div>
    );
  }

  if (orders.length === 0) {
    return (
      <div className="py-24 px-4 text-center">
        <h1 className="text-2xl font-bold text-gray-900">No Orders Found</h1>
        <p className="text-gray-600">You haven&apos;t placed any orders yet.</p>
        <button
          onClick={() => navigate("/products")}
          className="mt-4 px-4 py-2 bg-green-600 text-white rounded hover:bg-green-700"
        >
          Start Shopping
        </button>
      </div>
    );
  }

  const getStatusIcon = (status) => {
    switch (status.toLowerCase()) {
      case "pending":
        return <Package className="w-5 h-5 text-yellow-500" />;
      case "processing":
        return <Truck className="w-5 h-5 text-blue-500" />;
      case "shipped":
        return <Truck className="w-5 h-5 text-indigo-500" />;
      case "delivered":
        return <CheckCircle className="w-5 h-5 text-green-500" />;
      case "cancelled":
        return <XCircle className="w-5 h-5 text-red-500" />;
      default:
        return null;
    }
  };

  return (
    <div className="py-24 px-4">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-3xl font-bold mb-8 text-gray-900">
          Your Order History
        </h1>

        <div className="space-y-6">
          {orders.map((order) => (
            <div key={order.id} className="bg-white shadow-md rounded-lg p-6">
              <div className="flex justify-between items-center mb-4">
                <h2 className="text-xl font-semibold text-gray-800">
                  Order #{order.id}
                </h2>
                <div className="flex items-center gap-2">
                  {getStatusIcon(order.status)}
                  <span
                    className={`font-medium ${
                      order.status === "PENDING"
                        ? "text-yellow-600"
                        : order.status === "PROCESSING"
                        ? "text-blue-600"
                        : order.status === "SHIPPED"
                        ? "text-indigo-600"
                        : order.status === "DELIVERED"
                        ? "text-green-600"
                        : order.status === "CANCELLED"
                        ? "text-red-600"
                        : "text-gray-600"
                    }`}
                  >
                    {order.status}
                  </span>
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-gray-700 mb-4">
                <div>
                  <p>
                    <span className="font-medium">Order Date:</span>{" "}
                    {new Date(order.orderDate).toLocaleDateString()}
                  </p>
                  <p>
                    <span className="font-medium">Total Amount:</span> ₹
                    {order.totalAmount.toFixed(2)}
                  </p>
                  <p>
                    <span className="font-medium">Payment:</span>{" "}
                    {order.paymentMethod}
                  </p>
                </div>
                <div>
                  <p>
                    <span className="font-medium">Shipping To:</span>{" "}
                    {order.shippingAddress}
                  </p>
                  {/* Assuming your OrderDTO has other shipping fields if needed */}
                </div>
              </div>

              <div className="mt-4 border-t pt-4">
                <h3 className="font-semibold text-gray-800 mb-3">Items:</h3>
                <div className="space-y-2">
                  {order.orderItems &&
                    order.orderItems.map((item) => (
                      <div
                        key={item.id}
                        className="flex justify-between items-center text-sm text-gray-600"
                      >
                        <div className="flex items-center gap-2">
                          {item.productImageUrl && (
                            <img
                              src={item.productImageUrl}
                              alt={item.productName}
                              className="w-10 h-10 object-cover rounded"
                            />
                          )}
                          <span>
                            {item.productName} x {item.quantity}
                          </span>
                        </div>
                        <span>₹{item.productPrice.toFixed(2)}</span>
                      </div>
                    ))}
                </div>
              </div>

              <div className="mt-6 text-right">
                <button
                  onClick={() => navigate(`/orders/${order.id}`)} // Navigate to individual order details page
                  className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 transition"
                >
                  View Details
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
