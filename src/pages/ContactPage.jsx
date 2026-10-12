import { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import { API_BASE_URL } from "../api/axiosInstance";

export default function ContactPage() {
  // Use the useAuth hook to get the user and authentication status
  const { user, isAuthenticated } = useAuth();
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    message: "",
  });
  const [status, setStatus] = useState({ message: null, isError: false });
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errors, setErrors] = useState({});

  // useEffect to automatically populate the form for logged-in users
  useEffect(() => {
    if (isAuthenticated && user) {
      setFormData({
        name: user.name,
        email: user.email,
        message: "",
      });
    } else {
      setFormData({
        name: "",
        email: "",
        message: "",
      });
    }
  }, [isAuthenticated, user]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (errors[name]) {
      setErrors((prev) => ({ ...prev, [name]: "" }));
    }
  };

  const validateForm = () => {
    const newErrors = {};
    // Only validate name and email for guests
    if (!isAuthenticated) {
      if (!formData.name.trim()) newErrors.name = "Name is required";
      if (!formData.email.trim()) {
        newErrors.email = "Email is required";
      } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(formData.email)) {
        newErrors.email = "Please enter a valid email";
      }
    }
    if (!formData.message.trim()) newErrors.message = "Message is required";
    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const resetForm = () => {
    setFormData({
      name: user?.name || "",
      email: user?.email || "",
      message: "",
    });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!validateForm()) return;

    setIsSubmitting(true);
    setStatus({ message: "Sending your message...", isError: false });

    // Use user data from context if available, otherwise use form data
    const payload = {
      name: isAuthenticated ? user.name : formData.name,
      email: isAuthenticated ? user.email : formData.email,
      message: formData.message,
    };

    try {
      const response = await fetch(`${API_BASE_URL}/contact`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Requested-With": "XMLHttpRequest",
        },
        body: JSON.stringify(payload),
        credentials: "omit",
      });

      if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        if (response.status === 400 && Object.keys(errorData).length > 0) {
          setErrors(errorData);
          throw new Error("Validation failed. Please check your input.");
        }
        throw new Error(
          errorData.message || `Server responded with status ${response.status}`
        );
      }

      const responseText = await response.text();
      setStatus({
        message: responseText || "Message sent successfully!",
        isError: false,
      });
      resetForm();
    } catch (error) {
      console.error("Submission error:", error);
      setStatus({
        message:
          error.message || "Failed to send message. Please try again later.",
        isError: true,
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  useEffect(() => {
    if (status.message) {
      const timer = setTimeout(() => {
        setStatus({ message: null, isError: false });
      }, 5000);
      return () => clearTimeout(timer);
    }
  }, [status.message]);

  return (
    <div className="max-w-4xl mx-auto px-4 py-12">
      <div className="bg-white rounded-lg shadow-md p-8">
        <h1 className="text-3xl font-bold text-gray-800 mb-6">Contact Us</h1>

        <div className="grid md:grid-cols-2 gap-8">
          <div>
            <h2 className="text-xl font-semibold mb-4">Our Information</h2>
            <ul className="space-y-3 text-gray-600">
              <li className="flex items-start">
                <span className="mr-2">📧</span>
                <a
                  href="mailto:msharvests@gmail.com"
                  className="hover:text-blue-600"
                >
                  msharvests@gmail.com
                </a>
              </li>
              <li className="flex items-start">
                <span className="mr-2">📞</span>
                +91 9487-027-142
              </li>
              <li className="flex items-start">
                <span className="mr-2">📍</span>
                2/445, Anikorai Village, Doty, The Nilgiris, 643206
              </li>
              <li className="flex items-start">
                <span className="mr-2">🕘</span>
                Mon - Fri: 9 AM - 6 PM
              </li>
            </ul>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            {/* Conditional Greeting or Heading */}
            {isAuthenticated ? (
              <h2 className="text-xl font-semibold">Hello, {user.name}!</h2>
            ) : (
              <h2 className="text-xl font-semibold">Send us a message</h2>
            )}

            {/* Conditionally render Name and Email fields */}
            {!isAuthenticated && (
              <>
                <div>
                  <label
                    htmlFor="name"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    Name
                  </label>
                  <input
                    type="text"
                    id="name"
                    name="name"
                    required
                    className={`w-full px-4 py-2 border rounded-md focus:ring-2 focus:ring-green-500 focus:border-transparent ${
                      errors.name ? "border-red-500" : "border-gray-300"
                    }`}
                    value={formData.name}
                    onChange={handleChange}
                  />
                  {errors.name && (
                    <p className="mt-1 text-sm text-red-600">{errors.name}</p>
                  )}
                </div>
                <div>
                  <label
                    htmlFor="email"
                    className="block text-sm font-medium text-gray-700 mb-1"
                  >
                    Email
                  </label>
                  <input
                    type="email"
                    id="email"
                    name="email"
                    required
                    className={`w-full px-4 py-2 border rounded-md focus:ring-2 focus:ring-green-500 focus:border-transparent ${
                      errors.email ? "border-red-500" : "border-gray-300"
                    }`}
                    value={formData.email}
                    onChange={handleChange}
                  />
                  {errors.email && (
                    <p className="mt-1 text-sm text-red-600">{errors.email}</p>
                  )}
                </div>
              </>
            )}

            <div>
              <label
                htmlFor="message"
                className="block text-sm font-medium text-gray-700 mb-1"
              >
                Message
              </label>
              <textarea
                id="message"
                name="message"
                rows="4"
                required
                className={`w-full px-4 py-2 border rounded-md focus:ring-2 focus:ring-green-500 focus:border-transparent ${
                  errors.message ? "border-red-500" : "border-gray-300"
                }`}
                value={formData.message}
                onChange={handleChange}
              ></textarea>
              {errors.message && (
                <p className="mt-1 text-sm text-red-600">{errors.message}</p>
              )}
            </div>

            <button
              type="submit"
              disabled={isSubmitting}
              className={`w-full py-3 px-4 rounded-md font-medium transition-colors ${
                isSubmitting
                  ? "bg-gray-400 cursor-not-allowed"
                  : "bg-green-600 hover:bg-green-700 text-white"
              }`}
            >
              {isSubmitting ? (
                <>
                  <span className="inline-block animate-spin mr-2">↻</span>
                  Sending...
                </>
              ) : (
                "Send Message"
              )}
            </button>

            {status.message && (
              <div
                className={`p-3 rounded-md ${
                  status.isError
                    ? "bg-red-100 text-red-700"
                    : "bg-green-100 text-green-700"
                }`}
              >
                {status.message}
              </div>
            )}
          </form>
        </div>
      </div>
    </div>
  );
}
