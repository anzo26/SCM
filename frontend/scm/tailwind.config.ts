import type { Config } from "tailwindcss";

const config: Config = {
  darkMode: "class",
  content: [
    "./src/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: "#007BFF",
        secondary: "#FFFFFF",
        accent: "#F8F9FA",
        background: "#F2F2F2",
        danger: "#EA3323"
      },
      width: {
        "150px": "150px",
        "400px": "400px",
        "600px": "600px",
      },
      borderRadius: {
        '8': '8px',
      },
      borderWidth: {
        '1px': '1px',
      }
    },
  },
  plugins: [require('daisyui')],
  daisyui: {
    themes: ["light"],
  },
};

export default config;
