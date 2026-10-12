import React, { useState, useEffect } from "react";
import Hero from "../components/Hero";
import Categories from "../components/Categories";
import FeaturedProducts from "../components/FeaturedProducts";
import { useAuth } from "../context/AuthContext"; // Import useAuth
import { API_BASE_URL } from "../api/axiosInstance";

export default function HomePage() {
  const [backendMessage, setBackendMessage] = useState("");
  const [error, setError] = useState(null);
  const [loading, setLoading] = useState(true);
  const { user } = useAuth(); // Get user from auth context

  useEffect(() => {
    // Function to fetch data from the backend
    const fetchBackendMessage = async () => {
      try {
        const response = await fetch(`${API_BASE_URL}/status`);
        if (!response.ok) {
          throw new Error(`HTTP error! status: ${response.status}`);
        }
        const data = await response.text(); // Use .text() because HomeController returns a String
        setBackendMessage(data);
      } catch (error) {
        console.error("Error fetching backend message:", error);
        setError(error.message);
      } finally {
        setLoading(false);
      }
    };

    fetchBackendMessage();
  }, []); // The empty array ensures this effect runs only once after the initial render

  return (
    <>
      <Hero />

      {/* Conditional Welcome Message for logged-in users */}
      {user && (
        <section className="py-8 px-4 text-center bg-gray-50">
          <h2 className="text-3xl font-bold text-gray-900 mb-2">
            Welcome, {user.name}!
          </h2>
          {/* Change to a link that redirects to the categories section */}
          <a
            href="#categories"
            className="text-xl text-green-600 hover:underline"
          >
            Check out our fresh products below.
          </a>
        </section>
      )}

      {/* Add an id to the Categories component's container to enable scrolling */}
      <div id="categories">
        <Categories />
      </div>
      <FeaturedProducts />

      <section
        style={{
          padding: "20px",
          textAlign: "center",
          marginTop: "40px",
          borderTop: "1px solid #eee",
        }}
      >
        <h2>Backend Status</h2>
        {loading && <p>Loading message from backend...</p>}
        {error && <p style={{ color: "red" }}>Error: {error}</p>}
        {!loading && !error && (
          <p>
            <strong>Message from Backend:</strong> {backendMessage}
          </p>
        )}
      </section>
    </>
  );
}
