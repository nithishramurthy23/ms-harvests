import { createContext, useContext, useState, useEffect } from "react";
import PropTypes from "prop-types";
import api from "../api/axiosInstance";

// Holds the logged-in user and token, and the login/signup/OTP calls every page uses.
const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState("");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [message, setMessage] = useState(null);

  // Sends (or stops sending) the JWT with every API request.
  const setAuthHeaders = (authToken) => {
    if (authToken) {
      api.defaults.headers.common["Authorization"] = `Bearer ${authToken}`;
    } else {
      delete api.defaults.headers.common["Authorization"];
    }
  };

  // Forgets the session everywhere: storage, request headers and state.
  const clearAuth = () => {
    localStorage.removeItem("user");
    localStorage.removeItem("token");
    setAuthHeaders(null);
    setUser(null);
    setToken("");
    setError(null);
    setMessage(null);
  };

  // On page load, restore the session from localStorage and check the token is still valid.
  useEffect(() => {
    const initializeAuth = async () => {
      try {
        const storedToken = localStorage.getItem("token");
        if (storedToken) {
          setAuthHeaders(storedToken);
          const { data } = await api.get("/auth/me");
          setUser(data);
          setToken(storedToken);
        } else {
          clearAuth();
        }
      } catch (err) {
        console.error("Auth init error:", err);
        clearAuth();
        setError("Session expired or invalid. Please log in again.");
      } finally {
        setLoading(false);
      }
    };

    initializeAuth();
  }, []);

  // Password login; stores the returned token and user.
  const login = async (email, password) => {
    setLoading(true);
    setError(null);
    setMessage(null);
    try {
      setAuthHeaders(null);

      const payload = { email, password };
      const { data } = await api.post("/auth/login", payload);

      setUser(data);
      setToken(data.token);
      setAuthHeaders(data.token);

      localStorage.setItem("user", JSON.stringify(data));
      localStorage.setItem("token", data.token);

      return { success: true, user: data, message: "Login successful!" };
    } catch (err) {
      const errorMsg = err.response?.data?.message || "Login failed";
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setLoading(false);
    }
  };

  // Sign in with Google: trades Google's ID token for our own JWT.
  // New Google users get an account automatically.
  const loginWithGoogle = async (credential) => {
    setLoading(true);
    setError(null);
    setMessage(null);
    try {
      setAuthHeaders(null);

      const { data } = await api.post("/auth/google", { credential });

      setUser(data);
      setToken(data.token);
      setAuthHeaders(data.token);

      localStorage.setItem("user", JSON.stringify(data));
      localStorage.setItem("token", data.token);

      return { success: true, user: data, message: "Login successful!" };
    } catch (err) {
      const errorMsg = err.response?.data?.message || "Google sign-in failed";
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setLoading(false);
    }
  };

  // Creates the account; the backend emails an activation OTP.
  const signup = async (signupData) => {
    setLoading(true);
    setError(null);
    setMessage(null);
    try {
      const { data } = await api.post("/auth/register", signupData);
      setMessage(data.message || "Registration successful!");
      return { success: true, message: data.message };
    } catch (err) {
      const errorMsg = err.response?.data?.message || "Registration failed";
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setLoading(false);
    }
  };

  // Activates the account with the emailed OTP and logs in.
  const verifyOtp = async (email, otp) => {
    setLoading(true);
    setError(null);
    setMessage(null);
    try {
      setAuthHeaders(null);

      const payload = { email, otp };
      const { data } = await api.post("/auth/verify-otp", payload);

      setUser(data);
      setToken(data.token);
      setAuthHeaders(data.token);

      localStorage.setItem("user", JSON.stringify(data));
      localStorage.setItem("token", data.token);

      return {
        success: true,
        user: data,
        message: "Account verified and logged in!",
      };
    } catch (err) {
      const errorMsg = err.response?.data?.message || "OTP verification failed";
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setLoading(false);
    }
  };

  // Emails a login OTP.
  const sendOtp = async (email) => {
    setLoading(true);
    setError(null);
    setMessage(null);
    try {
      const { data } = await api.post("/auth/send-otp", { email });
      setMessage(data.message || "OTP sent successfully.");
      return { success: true, message: data.message };
    } catch (err) {
      const errorMsg = err.response?.data?.message || "Failed to send OTP.";
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setLoading(false);
    }
  };

  // Emails a password-reset OTP.
  const forgotPassword = async (email) => {
    setLoading(true);
    setError(null);
    setMessage(null);
    try {
      const { data } = await api.post("/auth/forgot-password", { email });
      setMessage(data.message || "Password reset OTP sent to your email.");
      return { success: true, message: data.message };
    } catch (err) {
      const errorMsg =
        err.response?.data?.message || "Failed to send reset OTP.";
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setLoading(false);
    }
  };

  // Sets a new password using the reset OTP.
  const resetPassword = async (email, otp, newPassword) => {
    setLoading(true);
    setError(null);
    setMessage(null);
    try {
      const { data } = await api.post("/auth/reset-password", {
        email,
        otp,
        newPassword,
      });
      setMessage(data.message || "Password reset successfully!");
      return { success: true, message: data.message };
    } catch (err) {
      const errorMsg =
        err.response?.data?.message || "Failed to reset password.";
      setError(errorMsg);
      return { success: false, error: errorMsg };
    } finally {
      setLoading(false);
    }
  };

  // Keeps the stored user in sync after a profile edit.
  const updateUser = (updatedUser) => {
    setUser(updatedUser);
    localStorage.setItem("user", JSON.stringify(updatedUser));
  };

  // Ends the session on this device.
  const logout = () => {
    clearAuth();
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        loading,
        error,
        message,
        isAuthenticated: !!token,
        login,
        loginWithGoogle,
        signup,
        verifyOtp, // New function for OTP verification
        sendOtp,
        forgotPassword,
        resetPassword,
        logout,
        updateUser,
        setError,
        setMessage,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

AuthProvider.propTypes = {
  children: PropTypes.node.isRequired,
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};
