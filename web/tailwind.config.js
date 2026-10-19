/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    "./app/**/*.{js,ts,jsx,tsx,mdx}",
    "./pages/**/*.{js,ts,jsx,tsx,mdx}",
    "./components/**/*.{js,ts,jsx,tsx,mdx}",
    "./src/**/*.{js,ts,jsx,tsx,mdx}",
  ],
  theme: {
    extend: {
      colors: {
        obsidian: "#06080D",
        cardDark: "#10121A",
        cardBorder: "#222634",
        electricCyan: "#00E5FF",
        neonMint: "#00E676",
        goldAccent: "#FFB300",
        alertRed: "#FF5252",
        subtleGrey: "#8E93A4"
      }
    },
  },
  plugins: [],
}
