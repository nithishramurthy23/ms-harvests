import { useEffect, useRef, useState } from "react";
import PropTypes from "prop-types";
import api from "../api/axiosInstance";

const GOOGLE_SCRIPT_URL = "https://accounts.google.com/gsi/client";

// Loads Google's sign-in script once, however many times the button mounts.
let scriptPromise = null;
const loadGoogleScript = () => {
  if (window.google?.accounts?.id) return Promise.resolve();
  if (!scriptPromise) {
    scriptPromise = new Promise((resolve, reject) => {
      const script = document.createElement("script");
      script.src = GOOGLE_SCRIPT_URL;
      script.async = true;
      script.onload = resolve;
      script.onerror = () => {
        scriptPromise = null;
        reject(new Error("Could not load Google sign-in"));
      };
      document.head.appendChild(script);
    });
  }
  return scriptPromise;
};

// "Sign in with Google" button. Google shows its own popup and hands us an ID
// token ("credential"); onCredential sends it to our backend. Renders nothing
// when the backend says Google sign-in isn't set up (no GOOGLE_CLIENT_ID).
export default function GoogleSignInButton({ onCredential, text = "signin_with" }) {
  const buttonRef = useRef(null);
  const [clientId, setClientId] = useState(null);

  // Keep the latest callback without re-initialising Google on every render.
  const onCredentialRef = useRef(onCredential);
  onCredentialRef.current = onCredential;

  useEffect(() => {
    let cancelled = false;
    api
      .get("/auth/google/config")
      .then(({ data }) => {
        if (!cancelled && data.enabled && data.clientId) setClientId(data.clientId);
      })
      .catch(() => {}); // backend down or older version: just no Google button
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    if (!clientId) return;
    let cancelled = false;
    loadGoogleScript()
      .then(() => {
        if (cancelled || !buttonRef.current) return;
        window.google.accounts.id.initialize({
          client_id: clientId,
          callback: (response) => onCredentialRef.current(response.credential),
        });
        window.google.accounts.id.renderButton(buttonRef.current, {
          theme: "outline",
          size: "large",
          text,
          width: buttonRef.current.offsetWidth || 320,
        });
      })
      .catch((err) => console.error(err));
    return () => {
      cancelled = true;
    };
  }, [clientId, text]);

  if (!clientId) return null;

  return (
    <div className="space-y-3">
      <div className="flex items-center gap-3 text-sm text-gray-500">
        <span className="h-px flex-1 bg-gray-200" />
        or
        <span className="h-px flex-1 bg-gray-200" />
      </div>
      <div ref={buttonRef} className="flex justify-center" data-testid="google-button" />
    </div>
  );
}

GoogleSignInButton.propTypes = {
  onCredential: PropTypes.func.isRequired,
  text: PropTypes.oneOf(["signin_with", "signup_with", "continue_with"]),
};
