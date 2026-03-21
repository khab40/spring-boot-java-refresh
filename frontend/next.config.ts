import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "standalone",
  async rewrites() {
    return [
      {
        source: "/backend/:path*",
        destination: "http://app:8080/:path*"
      }
    ];
  }
};

export default nextConfig;
