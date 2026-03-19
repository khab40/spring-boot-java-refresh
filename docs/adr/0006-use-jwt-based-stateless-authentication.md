# ADR-0006: Use JWT-Based Stateless Authentication with Password and OAuth2 Login
Status: Accepted  
Date: 2026-03-16

## Context
The application now needs full authentication for management APIs, including secure password storage, login, authorization context, optional external identity login, and stateless request authentication.

## Options Considered
1. Spring Security with BCrypt password hashing, OAuth2 client login, and JWT bearer tokens
2. HTTP session-based authentication only
3. API key only authentication for every endpoint

## Decision
Spring Security is used with BCrypt password hashing for local credentials, OAuth2 client login for external identity providers such as Google, and JWT bearer tokens for stateless authentication of protected APIs.

## Consequences
Positive:
- No server-side session storage is required
- Passwords are stored as hashes rather than plaintext
- External identity login can still converge on the same JWT and API key model
- Clear separation between interactive auth tokens and API keys for data access

Negative:
- JWT signing and validation add configuration and test complexity
- Token revocation is harder than server-side session invalidation

## Related Documents
- [Architecture Overview](../../ARCHITECTURE.md)
- [ADR-0006 Diagram](../diagrams/adr-0006-jwt-auth-flow.md)
