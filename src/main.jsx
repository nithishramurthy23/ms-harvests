import React from "react";
import ReactDOM from "react-dom/client";
import App from "./App";
import "./index.css";

// Get the root element from HTML
const rootElement = document.getElementById("root");

if (!rootElement) {
  throw new Error("❌ Root element with id='root' not found in index.html");
}

// Render the React application
ReactDOM.createRoot(rootElement).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
);
