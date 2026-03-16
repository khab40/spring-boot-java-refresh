# ADR-0003: Use Java 21 LTS with Dockerized Build, Test, and Runtime Workflows
Status: Accepted  
Date: 2026-03-16

## Context
The application needs a current LTS Java baseline and reproducible execution across developer machines where host Java versions may vary.

## Options Considered
1. Java 21 LTS with Dockerized scripts for build, test, and runtime
2. Java 17 with host-only Maven workflows
3. Newer non-LTS Java release with host-only workflows

## Decision
Java 21 LTS is the runtime and compilation target, and Docker helper scripts are used for standardized build, test, and application startup flows.

## Consequences
Positive:
- Current LTS baseline for language and platform support
- Reproducible Java version across environments
- Simple onboarding through scripted Docker workflows

Negative:
- Docker becomes a hard dependency for the verified workflow
- Build times can increase due to container startup and image downloads

## Related Documents
- [Architecture Overview](../../ARCHITECTURE.md)
- [ADR-0003 Diagram](../diagrams/adr-0003-java-docker-workflow.md)
