import { useState, useEffect } from "react";
import { useAuth } from "../context/AuthContext";
import { useNavigate } from "react-router-dom";
import GoogleSignInButton from "../components/GoogleSignInButton";

// Login, sign-up, OTP verification and password reset on one page; the flags
// below decide which form is shown.
export default function AuthPage() {
  const [isLogin, setIsLogin] = useState(true);
  const [loginMethod, setLoginMethod] = useState(null);
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
    phone: "",
    address: "",
    otp: "",
    newPassword: "",
  });
  const [passwordError, setPasswordError] = useState("");
  const [showOtpInputBlock, setShowOtpInputBlock] = useState(false);
  const [otpPurpose, setOtpPurpose] = useState("");
  const [showForgotPasswordFlow, setShowForgotPasswordFlow] = useState(false);
  const [showResetPasswordForm, setShowResetPasswordForm] = useState(false);
  const [resendTimer, setResendTimer] = useState(0);
  const [showActivationSuccess, setShowActivationSuccess] = useState(false);
  const [failedOtpAttempts, setFailedOtpAttempts] = useState(0); // Track failed attempts
  const maxOtpAttempts = 2; // Maximum retries allowed

  const navigate = useNavigate();
  const {
    login,
    loginWithGoogle,
    signup,
    sendOtp,
    verifyOtp,
    error,
    message,
    loading,
    user,
    forgotPassword,
    resetPassword,
    setError,
    setMessage,
  } = useAuth();

  useEffect(() => {
    if (user) {
      navigate("/");
    }
  }, [user, navigate]);

  useEffect(() => {
    setError(null);
    setMessage(null);
    setPasswordError("");
    setFormData((prev) => ({
      ...prev,
      otp: "",
      password: "",
      newPassword: "",
    }));
    setShowOtpInputBlock(false);
    setOtpPurpose("");
    setLoginMethod(null);
    setShowResetPasswordForm(false);
    setResendTimer(0);
    setShowActivationSuccess(false);
    setFailedOtpAttempts(0); // Reset failed attempts
  }, [isLogin, showForgotPasswordFlow, setError, setMessage]);

  useEffect(() => {
    let timerId;
    if (resendTimer > 0) {
      timerId = setInterval(() => {
        setResendTimer((prev) => prev - 1);
      }, 1000);
    }
    return () => clearInterval(timerId);
  }, [resendTimer]);

  const handleChange = (e) => {
    const { name, value } = e.target;
    setFormData((prev) => ({ ...prev, [name]: value }));
    if (name === "password" || name === "newPassword") {
      setPasswordError("");
    }
    setError(null);
  };

  const validatePassword = (password) => {
    const pattern =
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@$!%*?&])[A-Za-z\d@$!%*?&]{8,}$/;
    return pattern.test(password);
  };

  // Submits whichever form is showing: forgot password, sign-up or password login.
  const handleFormSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setMessage(null);

    const { name, email, password, phone, address } = formData;

    if (showForgotPasswordFlow) {
      const result = await forgotPassword(email);
      if (result?.success) {
        setMessage("Password reset OTP sent. Please check your email.");
        setShowResetPasswordForm(true);
        setOtpPurpose("passwordReset");
        setResendTimer(60);
      }
    } else if (!isLogin) {
      if (!validatePassword(password)) {
        setPasswordError(
          "Password must be at least 8 characters long and include uppercase, lowercase, number, and special character."
        );
        return;
      }
      const result = await signup({ name, email, password, phone, address });
      if (result?.success) {
        setShowOtpInputBlock(true);
        setOtpPurpose("registration");
        setResendTimer(60);
      }
    } else if (isLogin && loginMethod === "password") {
      const result = await login(email, password);
      if (result?.success) {
        navigate("/");
      }
    }
  };

  // Google handed us an ID token; the backend checks it and logs the user in.
  const handleGoogleCredential = async (credential) => {
    setError(null);
    setMessage(null);
    const result = await loginWithGoogle(credential);
    if (result?.success) {
      navigate("/");
    }
  };

  // Sets a new password with the reset OTP.
  const handleResetPasswordSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setMessage(null);

    const { email, otp, newPassword } = formData;

    if (!validatePassword(newPassword)) {
      setPasswordError(
        "New password must be at least 8 characters long and include uppercase, lowercase, number, and special character."
      );
      return;
    }
    const result = await resetPassword(email, otp, newPassword);
    if (result?.success) {
      setMessage("Password reset successfully! You can now sign in.");
      setIsLogin(true);
      setShowForgotPasswordFlow(false);
      setShowResetPasswordForm(false);
      setShowOtpInputBlock(false);
      setFormData((prev) => ({
        ...prev,
        otp: "",
        password: "",
        newPassword: "",
      }));
      setLoginMethod(null);
      setResendTimer(0);
    }
  };

  // Checks the OTP the user typed in (activation or login).
  const handleVerifyOtpSubmit = async (e) => {
    e.preventDefault();
    setError(null);
    setMessage(null);

    const { email, otp } = formData;
    if (!otp) {
      setError("Please enter the OTP.");
      return;
    }

    const result = await verifyOtp(email, otp);
    if (result?.success) {
      setShowActivationSuccess(true);
      setMessage(
        `Activation successful, ${result.user.name}! You will be redirected shortly.`
      );
      setTimeout(() => navigate("/"), 3000);
    } else {
      const newFailedAttempts = failedOtpAttempts + 1;
      setFailedOtpAttempts(newFailedAttempts);

      if (newFailedAttempts < maxOtpAttempts) {
        setError(
          `Invalid OTP. You have ${
            maxOtpAttempts - newFailedAttempts
          } attempts left.`
        );
        setFormData((prev) => ({ ...prev, otp: "" }));
      } else {
        setError(
          "Maximum OTP attempts exceeded. Please try registering again."
        );
        setShowOtpInputBlock(false);
        setFormData((prev) => ({ ...prev, otp: "" }));
        setResendTimer(0);
        // Optionally, clear the registration state or redirect to signup
        setIsLogin(false);
      }
    }
  };

  // Passwordless login: email the user a one-time code.
  const handleSendOtpForLogin = async (e) => {
    e.preventDefault();
    setError(null);
    setMessage(null);
    const { email } = formData;
    if (!email) {
      setError("Please enter your email to send OTP.");
      return;
    }
    const result = await sendOtp(email);
    if (result?.success) {
      setShowOtpInputBlock(true);
      setOtpPurpose("login");
      setResendTimer(60);
      setFailedOtpAttempts(0); // Reset attempts for new OTP
    } else {
      setError(result?.error || "Failed to send OTP.");
      setMessage(null);
    }
  };

  // Sends a new code once the resend timer runs out.
  const handleResendOtp = async () => {
    setError(null);
    setMessage(null);
    const result = await sendOtp(formData.email);
    if (result?.success) {
      setResendTimer(60);
      setFailedOtpAttempts(0); // Reset attempts on resend
    }
  };

  const selectLoginMethod = (method) => {
    setLoginMethod(method);
    setError(null);
    setMessage(null);
    setFormData((prev) => ({ ...prev, password: "", otp: "" }));
    setShowOtpInputBlock(false);
    setShowResetPasswordForm(false);
    setResendTimer(0);
    setFailedOtpAttempts(0); // Reset attempts when changing method
  };

  const handleBackToLogin = () => {
    setShowForgotPasswordFlow(false);
    setShowResetPasswordForm(false);
    setIsLogin(true);
    setLoginMethod(null);
    setFormData((prev) => ({
      ...prev,
      otp: "",
      password: "",
      newPassword: "",
    }));
    setError(null);
    setMessage(null);
    setResendTimer(0);
    setFailedOtpAttempts(0); // Reset attempts
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 py-12 px-6 sm:px-6 lg:px-8">
      <div className="max-w-md w-full space-y-6 p-8 bg-white shadow-lg rounded-lg">
        <h2 className="text-center text-3xl font-bold text-gray-900">
          {isLogin && !showForgotPasswordFlow
            ? loginMethod === null
              ? "Sign in"
              : loginMethod === "password"
              ? "Sign in with Password"
              : "Sign in with OTP"
            : showForgotPasswordFlow
            ? showResetPasswordForm
              ? "Reset Password"
              : "Forgot Password"
            : "Create a new account"}
        </h2>

        {message && (
          <div
            className={`bg-green-100 border border-green-400 text-green-700 px-4 py-3 rounded ${
              showActivationSuccess ? "text-center font-semibold" : ""
            }`}
            role="alert"
          >
            {message}
          </div>
        )}
        {error && (
          <div
            className="bg-red-100 border border-red-400 text-red-700 px-4 py-3 rounded"
            role="alert"
          >
            {error}
          </div>
        )}

        {showForgotPasswordFlow ? (
          showResetPasswordForm ? (
            <form onSubmit={handleResetPasswordSubmit} className="space-y-4">
              <p className="text-center text-gray-700">
                An OTP has been sent to your email ({formData.email}). Please
                enter it along with your new password.
              </p>
              <input
                type="email"
                name="email"
                placeholder="Email address"
                required
                value={formData.email}
                onChange={handleChange}
                className="block w-full p-3 border rounded-md"
                readOnly
              />
              <input
                type="text"
                name="otp"
                placeholder="Enter OTP"
                required
                value={formData.otp}
                onChange={handleChange}
                className="block w-full p-3 border rounded-md"
              />
              <input
                type="password"
                name="newPassword"
                placeholder="New Password"
                required
                value={formData.newPassword}
                onChange={handleChange}
                className="block w-full p-3 border rounded-md"
              />
              {passwordError && (
                <p className="text-red-600 text-sm">{passwordError}</p>
              )}
              <button
                type="submit"
                disabled={loading}
                className="w-full bg-green-600 text-white py-2 rounded-md"
              >
                {loading ? "Resetting..." : "Reset Password"}
              </button>
              <p className="text-center text-sm">
                <button
                  type="button"
                  onClick={handleResendOtp}
                  disabled={loading || resendTimer > 0}
                  className="text-green-600 hover:underline font-medium"
                >
                  Resend OTP {resendTimer > 0 && `(${resendTimer}s)`}
                </button>
              </p>
              <p className="text-center text-sm">
                <button
                  type="button"
                  onClick={handleBackToLogin}
                  className="text-gray-600 hover:underline font-medium"
                >
                  Back to Login
                </button>
              </p>
            </form>
          ) : (
            <form onSubmit={handleFormSubmit} className="space-y-4">
              <h3 className="text-xl font-semibold text-center mb-4">
                Forgot Password
              </h3>
              <p className="text-center text-gray-700">
                Enter your email to receive a password reset OTP.
              </p>
              <input
                type="email"
                name="email"
                placeholder="Email address"
                required
                value={formData.email}
                onChange={handleChange}
                className="block w-full p-3 border rounded-md"
              />
              <button
                type="submit"
                disabled={loading}
                className="w-full bg-green-600 text-white py-2 rounded-md"
              >
                {loading ? "Sending OTP..." : "Send Reset OTP"}
              </button>
              <p className="text-center text-sm">
                <button
                  type="button"
                  onClick={handleBackToLogin}
                  className="text-gray-600 hover:underline font-medium"
                >
                  Back to Login
                </button>
              </p>
            </form>
          )
        ) : showOtpInputBlock ? (
          <form onSubmit={handleVerifyOtpSubmit} className="space-y-4">
            <p className="text-center text-gray-700">
              An OTP has been sent to your email ({formData.email}). Please
              enter it below to{" "}
              {otpPurpose === "registration" ? "verify your account" : "log in"}
              . This OTP is valid for 1 minutes.
            </p>
            <input
              type="text"
              name="otp"
              placeholder="Enter OTP"
              required
              value={formData.otp}
              onChange={handleChange}
              className="block w-full p-3 border rounded-md"
            />
            <button
              type="submit"
              disabled={loading}
              className="w-full bg-green-600 text-white py-2 rounded-md"
            >
              {loading ? "Verifying..." : "Verify OTP"}
            </button>
            <p className="text-center text-sm">
              Didn&apos;t receive OTP?{" "}
              <button
                type="button"
                onClick={handleResendOtp}
                disabled={loading || resendTimer > 0}
                className="text-green-600 hover:underline font-medium"
              >
                Resend OTP {resendTimer > 0 && `(${resendTimer}s)`}
              </button>
            </p>
            <p className="text-center text-sm">
              <button
                type="button"
                onClick={() => {
                  setLoginMethod(null);
                  setShowOtpInputBlock(false);
                  setFormData((prev) => ({ ...prev, otp: "" }));
                }}
                className="text-gray-600 hover:underline font-medium"
              >
                Back to Method Choice
              </button>
            </p>
          </form>
        ) : (
          <form onSubmit={handleFormSubmit} className="space-y-4">
            {!isLogin && (
              <>
                <input
                  type="text"
                  name="name"
                  placeholder="Full name"
                  required
                  value={formData.name}
                  onChange={handleChange}
                  className="block w-full p-3 border rounded-md"
                />
                <input
                  type="tel"
                  name="phone"
                  placeholder="Phone number"
                  pattern="[0-9]{10}"
                  title="Phone number must be 10 digits"
                  required
                  value={formData.phone}
                  onChange={handleChange}
                  className="block w-full p-3 border rounded-md"
                />
                <textarea
                  name="address"
                  placeholder="Address"
                  required
                  value={formData.address}
                  onChange={handleChange}
                  className="block w-full p-3 border rounded-md"
                ></textarea>
              </>
            )}
            <input
              type="email"
              name="email"
              placeholder="Email address"
              required
              value={formData.email}
              onChange={handleChange}
              className="block w-full p-3 border rounded-md"
            />
            {isLogin && loginMethod === null && (
              <div className="space-y-3">
                <button
                  type="button"
                  onClick={() => selectLoginMethod("password")}
                  className="w-full bg-blue-500 text-white py-2 rounded-md"
                >
                  Login with Password
                </button>
                <button
                  type="button"
                  onClick={() => selectLoginMethod("otp")}
                  className="w-full bg-purple-500 text-white py-2 rounded-md"
                >
                  Login with OTP
                </button>
              </div>
            )}
            {isLogin && loginMethod === "password" && (
              <>
                <input
                  type="password"
                  name="password"
                  placeholder="Password"
                  required
                  value={formData.password}
                  onChange={handleChange}
                  className="block w-full p-3 border rounded-md"
                />
                <button
                  type="submit"
                  disabled={loading}
                  className="w-full bg-green-600 text-white py-2 rounded-md"
                >
                  {loading ? "Signing in..." : "Sign in"}
                </button>
                <p className="text-center text-sm">
                  <button
                    type="button"
                    onClick={() => setShowForgotPasswordFlow(true)}
                    className="text-green-600 hover:underline font-medium"
                  >
                    Forgot Password?
                  </button>
                </p>
                <p className="text-center text-sm">
                  <button
                    type="button"
                    onClick={() => selectLoginMethod(null)}
                    className="text-gray-600 hover:underline font-medium"
                  >
                    Back to Method Choice
                  </button>
                </p>
              </>
            )}
            {isLogin && loginMethod === "otp" && !showOtpInputBlock && (
              <>
                <h3 className="text-xl font-semibold text-center mb-4">
                  Sign in with OTP
                </h3>
                <p className="text-center text-gray-700">
                  Enter your email to receive an OTP for login.
                </p>
                <button
                  type="button"
                  onClick={handleSendOtpForLogin}
                  disabled={loading}
                  className="w-full bg-purple-500 text-white py-2 rounded-md hover:bg-purple-600 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-purple-500 transition duration-150 ease-in-out"
                >
                  {loading ? "Sending OTP..." : "Send OTP"}
                </button>
                <p className="text-center text-sm">
                  <button
                    type="button"
                    onClick={() => selectLoginMethod(null)}
                    className="text-gray-600 hover:underline font-medium"
                  >
                    Back to Method Choice
                  </button>
                </p>
              </>
            )}
            {!isLogin && (
              <input
                type="password"
                name="password"
                placeholder="Password"
                required
                value={formData.password}
                onChange={handleChange}
                className="block w-full p-3 border rounded-md"
              />
            )}
            {!isLogin && passwordError && (
              <p className="text-red-600 text-sm">{passwordError}</p>
            )}
            {!isLogin && (
              <button
                type="submit"
                disabled={loading}
                className="w-full bg-green-600 text-white py-2 rounded-md hover:bg-green-700 focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-green-500 transition duration-150 ease-in-out"
              >
                {loading ? "Signing up..." : "Sign up"}
              </button>
            )}
          </form>
        )}
        {/* Google works for both sign-in and sign-up: a new Google user gets an account. */}
        {!showForgotPasswordFlow &&
          !showOtpInputBlock &&
          (!isLogin || loginMethod === null) && (
            <GoogleSignInButton
              onCredential={handleGoogleCredential}
              text={isLogin ? "signin_with" : "signup_with"}
            />
          )}
        {!showForgotPasswordFlow && (
          <p className="text-center text-sm mt-4">
            {isLogin ? "Don't have an account?" : "Already have an account?"}{" "}
            <button
              type="button"
              onClick={() => {
                setIsLogin(!isLogin);
                setLoginMethod(null);
              }}
              className="text-green-600 hover:underline font-medium"
            >
              {isLogin ? "Sign up" : "Sign in"}
            </button>
          </p>
        )}
      </div>
    </div>
  );
}
