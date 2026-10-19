import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Movie Finder Web - Discover the movie behind the scene",
  description: "Identify movies from live camera feeds or picture scenes using Gemini 3.5 Flash and shared Supabase cloud storage.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="en" className="dark">
      <body className="bg-obsidian text-white antialiased">
        {children}
      </body>
    </html>
  );
}
