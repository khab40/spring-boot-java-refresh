import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "Market Data Lake UI",
  description: "Reactive web UI for Market Data Lake"
};

export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return (
    <html lang="en">
      <body>{children}</body>
    </html>
  );
}
