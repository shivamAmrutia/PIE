import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "PIE — Portfolio Intelligence Engine",
  description: "Evidence-backed advisor intelligence for wealth management",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
