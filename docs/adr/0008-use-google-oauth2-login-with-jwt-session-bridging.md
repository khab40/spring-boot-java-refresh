# ADR-0008: Use Google OAuth2 Login with JWT Session Bridging
Status: Accepted  
Date: 2026-03-19

## Context
The application already supports local email and password authentication with JWTs and API keys. It now also needs Google account sign-in for web users without introducing a separate identity backend or changing downstream authorization and quota logic.

## Options Considered
1. Spring Security OAuth2 login with Google, then bridge the authenticated user into the existing JWT and API key model
2. Build a custom Google token verification flow manually
3. Keep only local email and password authentication

## Decision
Spring Security OAuth2 client login is used for Google authentication. After successful Google login, the application links or creates a local `User`, marks the account verified, then issues the same JWT and API key response model already used by the rest of the application.

## Consequences
Positive:
- Google login reuses the existing user, entitlement, JWT, and API key model
- The UI can support local and Google login without a separate auth backend
- Verified Google identity can bypass the local email verification loop

Negative:
- OAuth2 client configuration adds provider-specific setup and callback handling
- Authentication flows now include redirect-based behavior that must be tested across backend and frontend
- Local and external identities must be reconciled carefully when emails match

## Related Documents
- [Architecture Overview](../../ARCHITECTURE.md)
- [ADR-0006](./0006-use-jwt-based-stateless-authentication.md)
- [ADR-0008 Diagram](../diagrams/adr-0008-google-oauth2-jwt-bridge.md)
