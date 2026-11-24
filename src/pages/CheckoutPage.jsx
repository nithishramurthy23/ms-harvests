import { useEffect, useRef, useState } from "react";
import { useCart } from "../context/CartContext";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import { CheckCircle, CreditCard, Truck } from "lucide-react";
import api from "../api/axiosInstance";
import { toOrderItems } from "../utils/orderItems";
import { useCatalog } from "../context/CatalogContext";
import { newCheckoutKey, payForOrder, paymentConfig } from "../utils/payments";

// Checkout in two steps: fill in shipping details and request an OTP,
// then enter the OTP from the email to place the order. With online payment,
// Razorpay's window opens next; the order is confirmed once the payment is checked.
export default function CheckoutPage() {
  const { state, dispatch } = useCart();
  const { productIds, reload: reloadCatalog } = useCatalog();
  const { user, isAuthenticated, token } = useAuth();
  const navigate = useNavigate();
  const [showSuccess, setShowSuccess] = useState(false);
  const [paymentMethod, setPaymentMethod] = useState("cod");
  const [onlineEnabled, setOnlineEnabled] = useState(false);
  const [successText, setSuccessText] = useState("Order Placed Successfully!");
  // Same key for every try of this checkout, so a retry never makes two orders.
  const checkoutKey = useRef(newCheckoutKey());

  useEffect(() => {
    paymentConfig()
      .then((config) => setOnlineEnabled(config.onlineEnabled))
      .catch(() => setOnlineEnabled(false));
  }, []);
  const [formData, setFormData] = useState({
    name: user?.name || "",
    email: user?.email || "",
    address: user?.address || "",
    city: "",
    pincode: "",
    phone: user?.phone || "",
  });
  // On a reload the page renders before the saved login is restored, so the
  // fields above start empty. Fill in whatever is still empty once it arrives;
  // name and email are read-only when logged in, so they must come from here.
  useEffect(() => {
    if (!user) return;
    setFormData((current) => ({
      ...current,
      name: current.name || user.name || "",
      email: current.email || user.email || "",
      address: current.address || user.address || "",
      phone: current.phone || user.phone || "",
    }));
  }, [user]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const [successMessage, setSuccessMessage] = useState(null);
  const [otpSent, setOtpSent] = useState(false);
  const [orderOtp, setOrderOtp] = useState("");

  const validateForm = () => {
    const requiredFields = [
      "name",
      "email",
      "address",
      "city",
      "pincode",
      "phone",
    ];
    for (const field of requiredFields) {
      if (!formData[field]) {
        setError(`Please fill out the ${field} field.`);
        return false;
      }
    }
    return true;
  };

  const handleChange = (e) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value,
    });
  };

  // Step 1: validate the form and ask the backend to email an order OTP.
  const handleInitiateOrder = async () => {
    setError(null);
    if (!validateForm()) return;

    if (!isAuthenticated) {
      setError("You must be logged in to place an order.");
      return;
    }

    setLoading(true);

    try {
      await api.post(
        "/orders/initiate-order",
        { email: user.email },
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      );
      setOtpSent(true);
      setSuccessMessage("OTP sent to your email for order confirmation.");
    } catch (err) {
      console.error(
        "Initiate order failed:",
        err.response ? err.response.data : err.message
      );
      setError(
        err.response?.data?.message ||
          "Failed to initiate order. Please try again."
      );
    } finally {
      setLoading(false);
    }
  };

  // Step 2: send the order with the OTP. Prices are worked out on the server.
  const handlePlaceOrder = async (e) => {
    e.preventDefault();
    setError(null);
    setSuccessMessage(null);

    if (!validateForm() || !orderOtp) {
      setError("Please enter the OTP to confirm your order.");
      return;
    }

    if (!isAuthenticated) {
      setError("You must be logged in to place an order.");
      return;
    }

    setLoading(true);

    const orderData = {
      shippingInfo: {
        name: formData.name,
        address: formData.address,
        city: formData.city,
        postalCode: formData.pincode,
        phone: formData.phone,
        email: formData.email,
        expectedDeliveryDate: new Date().toISOString().split("T")[0], // Placeholder
      },
      // Bundles are sent as their individual products; the server prices everything
      items: toOrderItems(state.items, productIds.size ? productIds : null),
      paymentMethod,
      orderConfirmationOtp: orderOtp,
    };

    try {
      // The Idempotency-Key header is the same on every try of this checkout. If the
      // first try reached the server but the reply was lost, a retry gets that order back.
      const response = await api.post(
        "/orders/place-order",
        orderData,
        {
          headers: {
            Authorization: `Bearer ${token}`,
            "Idempotency-Key": checkoutKey.current,
          },
        }
      );
      const placed = response.data;
      // An online order comes back with payment details: open Razorpay's window now.
      // payForOrder also sends the result to our server, which checks the signature.
      if (placed.payment) {
        try {
          await payForOrder(placed.orderId, placed.payment, formData);
          setSuccessText("Payment received. Order confirmed!");
        } catch (payErr) {
          // The order is saved and its stock held. It can be paid from "My orders".
          dispatch({ type: "CLEAR_CART" });
          reloadCatalog();
          setError(
            payErr?.dismissed
              ? `Order #${placed.orderId} is saved but not paid yet. You can pay for it from My orders within 30 minutes.`
              : `${payErr.message || "The payment didn't go through."} Order #${placed.orderId} is saved: you can try again from My orders.`
          );
          return;
        }
      }
      setSuccessMessage(placed.message || "Order placed successfully!");
      dispatch({ type: "CLEAR_CART" });
      reloadCatalog(); // stock just changed
      setShowSuccess(true);
      setTimeout(() => {
        navigate("/");
      }, 3000);
    } catch (err) {
      console.error(
        "Order placement failed:",
        err.response ? err.response.data : err.message
      );
      setError(
        err.response?.data?.message ||
          "Failed to place order. Please try again."
      );
    } finally {
      setLoading(false);
    }
  };

  if (showSuccess) {
    return (
      <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 z-50">
        <div className="bg-white rounded-lg p-8 max-w-md w-full mx-4 text-center">
          <CheckCircle className="w-16 h-16 text-green-500 mx-auto mb-4" />
          <h2 className="text-2xl font-bold mb-2">{successText}</h2>
          <p className="text-gray-600 mb-4">
            Thank you for shopping with MS Fresh Harvest.
          </p>
          <p className="text-sm text-gray-500">Redirecting to homepage...</p>
        </div>
      </div>
    );
  }

  if (state.items.length === 0) {
    return (
      <div className="py-10 px-4 text-center">
        <h1 className="text-2xl font-bold mb-4">Your cart is empty</h1>
        <button
          onClick={() => navigate("/")}
          className="text-green-600 hover:text-green-700"
        >
          Continue Shopping
        </button>
      </div>
    );
  }

  return (
    <div className="py-10 px-4">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-3xl font-bold mb-8">Checkout</h1>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-12">
          <div className="space-y-6">
            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-xl font-semibold mb-4">
                Shipping Information
              </h2>
              <form className="space-y-4">
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Name
                    </label>
                    <input
                      type="text"
                      name="name"
                      value={formData.name}
                      onChange={handleChange}
                      required
                      className="w-full px-4 py-2 rounded-lg border focus:ring-2 focus:ring-green-500"
                      readOnly={isAuthenticated}
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Email
                    </label>
                    <input
                      type="email"
                      name="email"
                      value={formData.email}
                      onChange={handleChange}
                      required
                      className="w-full px-4 py-2 rounded-lg border focus:ring-2 focus:ring-green-500"
                      readOnly={isAuthenticated}
                    />
                  </div>
                </div>
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-1">
                    Address
                  </label>
                  <input
                    type="text"
                    name="address"
                    value={formData.address}
                    onChange={handleChange}
                    required
                    className="w-full px-4 py-2 rounded-lg border focus:ring-2 focus:ring-green-500"
                  />
                </div>
                <div className="grid grid-cols-3 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      City
                    </label>
                    <input
                      type="text"
                      name="city"
                      value={formData.city}
                      onChange={handleChange}
                      required
                      className="w-full px-4 py-2 rounded-lg border focus:ring-2 focus:ring-green-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Pincode
                    </label>
                    <input
                      type="text"
                      name="pincode"
                      value={formData.pincode}
                      onChange={handleChange}
                      required
                      className="w-full px-4 py-2 rounded-lg border focus:ring-2 focus:ring-green-500"
                    />
                  </div>
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-1">
                      Phone
                    </label>
                    <input
                      type="tel"
                      name="phone"
                      value={formData.phone}
                      onChange={handleChange}
                      required
                      className="w-full px-4 py-2 rounded-lg border focus:ring-2 focus:ring-green-500"
                    />
                  </div>
                </div>
              </form>
            </div>

            <div className="bg-white rounded-lg shadow-md p-6">
              <h2 className="text-xl font-semibold mb-4">Payment Method</h2>
              {/* Card and UPI details are typed into Razorpay's window, never into this page. */}
              <div className="space-y-3" role="radiogroup" aria-label="Payment method">
                {[
                  { value: "cod", icon: Truck, title: "Cash on Delivery", hint: "Pay when you receive the products." },
                  ...(onlineEnabled
                    ? [{ value: "online", icon: CreditCard, title: "Pay online", hint: "UPI, card or net banking, through Razorpay." }]
                    : []),
                ].map(({ value, icon: Icon, title, hint }) => (
                  <label
                    key={value}
                    className={`flex cursor-pointer items-center gap-3 p-4 rounded-lg border ${
                      paymentMethod === value ? "border-green-500 bg-green-50" : "border-gray-200"
                    }`}
                  >
                    <input
                      type="radio"
                      name="paymentMethod"
                      value={value}
                      checked={paymentMethod === value}
                      onChange={() => setPaymentMethod(value)}
                      disabled={otpSent}
                      className="sr-only"
                    />
                    <Icon className="w-6 h-6 text-green-700" />
                    <div>
                      <span className="block font-medium">{title}</span>
                      <span className="block text-sm text-gray-600">{hint}</span>
                    </div>
                  </label>
                ))}
              </div>
            </div>
          </div>

          <div className="bg-white rounded-lg shadow-md p-6">
            <h2 className="text-xl font-semibold mb-4">Order Summary</h2>
            <div className="space-y-4">
              {state.items.map((item, index) => (
                <div key={index} className="flex justify-between">
                  <span>
                    {item.name} x{item.quantity}
                  </span>
                  <span>₹{item.price * item.quantity}</span>
                </div>
              ))}
              <div className="flex justify-between font-bold">
                <span>Total</span>
                <span>
                  ₹
                  {state.items.reduce(
                    (total, item) => total + item.price * item.quantity,
                    0
                  )}
                </span>
              </div>
            </div>
            {otpSent ? (
              <form onSubmit={handlePlaceOrder} className="mt-8">
                <input
                  type="text"
                  placeholder="Enter OTP"
                  value={orderOtp}
                  onChange={(e) => setOrderOtp(e.target.value)}
                  className="w-full px-4 py-2 rounded-lg border focus:ring-2 focus:ring-green-500 mb-4"
                  required
                />
                <button
                  type="submit"
                  disabled={loading || state.items.length === 0 || !orderOtp}
                  className="w-full bg-green-600 text-white py-3 rounded-lg hover:bg-green-700 transition-colors font-medium text-lg disabled:opacity-50"
                >
                  {loading
                    ? "Placing Order..."
                    : paymentMethod === "online"
                    ? "Confirm & Pay"
                    : "Confirm & Place Order"}
                </button>
              </form>
            ) : (
              <button
                onClick={handleInitiateOrder}
                disabled={loading || state.items.length === 0}
                className="mt-8 w-full bg-green-600 text-white py-3 rounded-lg hover:bg-green-700 transition-colors font-medium text-lg disabled:opacity-50"
              >
                {loading ? "Initiating Order..." : "Place Order"}
              </button>
            )}

            {error && (
              <div className="text-red-500 text-center mt-4">{error}</div>
            )}
            {successMessage && (
              <div className="text-green-600 text-center mt-4">
                {successMessage}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
