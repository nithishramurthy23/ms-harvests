/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,ts,jsx,tsx}"],
  theme: {
    extend: {
      // "green" is redefined as a softer leaf green so every existing
      // green-* class picks up the farm palette without a rename.
      colors: {
        green: {
          50: "#f3f6ec",
          100: "#e4ecd5",
          200: "#c9d9ab",
          300: "#a8c17c",
          400: "#86a656",
          500: "#6a8b3c",
          600: "#506f2c",
          700: "#3f5824",
          800: "#33461f",
          900: "#2a3a1c",
          950: "#151f0d",
        },
        cream: "#faf6ee",
        paper: "#fffdf8",
        bark: "#2d2a24",
        clay: {
          100: "#f8e6dc",
          500: "#c4673f",
          600: "#a9532f",
        },
        mustard: {
          100: "#fbefd0",
          400: "#e3b04b",
        },
      },
      fontFamily: {
        sans: ['"DM Sans"', "system-ui", "sans-serif"],
        display: ['"Fraunces"', "Georgia", "serif"],
      },
      keyframes: {
        "slide-in": { from: { transform: "translateX(100%)" }, to: { transform: "translateX(0)" } },
        "pop": { "0%": { transform: "scale(1)" }, "50%": { transform: "scale(1.25)" }, "100%": { transform: "scale(1)" } },
      },
      animation: {
        "slide-in": "slide-in 220ms ease-out",
        pop: "pop 300ms ease-out",
      },
    },
  },
  plugins: [],
};
