"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { buildSession } from "../../../lib/api";
import { saveSession } from "../../../lib/storage";
import { AuthResponse } from "../../../lib/types";

export default function OAuthCallbackPage() {
  const router = useRouter();
  const [status, setStatus] = useState("Completing Google sign-in...");

  useEffect(() => {
    async function completeLogin() {
      const hash = window.location.hash.startsWith("#") ? window.location.hash.slice(1) : "";
      const params = new URLSearchParams(hash);
      const accessToken = params.get("accessToken");
      const apiKey = params.get("apiKey");
      const userId = params.get("userId");
      const email = params.get("email");
      const message = params.get("message") ?? "Signed in with Google successfully.";

      if (!accessToken || !apiKey || !userId || !email) {
        router.replace("/?authError=google-signin-missing-session");
        return;
      }

      const auth: AuthResponse = {
        userId: Number(userId),
        email,
        accessToken,
        apiKey,
        tokenType: "Bearer",
        emailVerified: true,
        message
      };

      try {
        const session = await buildSession(auth);
        saveSession(session);
        router.replace("/?authSuccess=google");
      } catch {
        saveSession(null);
        router.replace("/?authError=google-signin-session-build-failed");
      }
    }

    void completeLogin();
  }, [router]);

  return (
    <main style={{ padding: "4rem 1.5rem", maxWidth: "48rem", margin: "0 auto" }}>
      <h1>Market Data Lake</h1>
      <p>{status}</p>
    </main>
  );
}
