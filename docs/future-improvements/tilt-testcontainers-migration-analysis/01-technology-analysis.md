# Technology Analysis: Tilt.dev & Testcontainers for cpp-context-listing

## 1. Executive Summary

This document analyses the suitability of **Tilt.dev** and **Testcontainers** as replacements for the current `cpp-developers-docker`-based integration test and local development workflow for the `cpp-context-listing` microservice.

| Criterion | Tilt.dev | Testcontainers |
|-----------|----------|----------------|
| **Primary purpose** | Local development orchestration with live-reload | Programmatic test-scoped container lifecycle |
| **Removes cpp-developers-docker?** | Yes (fully) | Yes (for integration tests only) |
| **CI/CD suitability** | Moderate (requires K8s in CI) | Excellent (Docker-only, no K8s) |
| **Team adoption effort** | Medium-High | Low-Medium |
| **Best fit for listing** | Development workflow | Integration testing |

---

## 2. Current State — What cpp-developers-docker Provides

The `listing` context's `runIntegrationTests.sh` depends on cpp-developers-docker for:

| Service | Purpose | Config |
|---------|---------|--------|
| **PostgreSQL 15** | 3 databases: `listingeventstore`, `listingviewstore`, `listingsystem` | User: `listing` / Pass: `listing` |
| **Apache Artemis 2.18** | JMS message broker for event topics (`listing.event`, `public.event`) | Port 61616 |
| **Wildfly 26.1.3** | Java EE application server for WAR deployment | Port 9080 (via HAProxy 8080) |
| **HAProxy 2.0** | Reverse proxy with URL rewriting | Port 8080 → Wildfly 9080 |
| **Liquibase Runner** | One-shot container for schema migrations | 6 separate migration sets |
| **Shell functions** | `buildWars`, `deployWars`, `healthchecks`, `integrationTests`, `runLiquibase` | Sourced from cpp-developers-docker scripts |

### Current Execution Flow
```
runIntegrationTests.sh
  → loginToDockerContainerRegistry
  → buildWars (mvn install)
  → undeployWarsFromDocker (rm WARs from container)
  → buildAndStartContainers (docker compose down -v → up -d)
  → runLiquibase (6 migrations: eventstore, viewstore, system, event-buffer, event-tracking, aggregate-snapshot)
  → deployWiremock
  → deployWars (docker cp WAR into Wildfly container)
  → healthchecks (poll /internal/metrics/ping + /internal/healthchecks/all)
  → integrationTests (mvn verify -Plisting-integration-test)
```

---

## 3. Technology Analysis: Tilt.dev

### 3.1 What Is Tilt.dev?

Tilt.dev is a local Kubernetes development tool that watches source files, builds container images, and deploys them to a local K8s cluster (kind, minikube, Docker Desktop K8s). It provides:

- **Live-reload**: File changes trigger automatic rebuild + redeploy
- **Dashboard**: Web UI showing service status, logs, and build progress
- **Multi-service orchestration**: Manage all dependent services from a single `Tiltfile`
- **Resource dependencies**: Define startup ordering between services

### 3.2 Suitability for cpp-context-listing

| Aspect | Assessment | Notes |
|--------|------------|-------|
| **PostgreSQL** | Excellent | K8s StatefulSet or Helm chart |
| **Artemis** | Good | Official Docker image, K8s Deployment |
| **Wildfly + WAR** | Good | Custom image with WAR, live-sync for class files |
| **HAProxy** | Good | K8s Service handles routing; HAProxy as optional Deployment |
| **Liquibase** | Good | K8s Job or init-container |
| **WireMock** | Excellent | Lightweight container, K8s Deployment |
| **6-database Liquibase** | Moderate | Needs orchestration for 6 separate migration sets |
| **Live development** | Excellent | Primary strength of Tilt |
| **CI/CD integration** | Moderate | Needs K8s in CI (kind + ctlptl) |

### 3.3 Benefits for listing

1. **Live-reload during development**: Change Java source → automatic WAR rebuild → auto-deploy to Wildfly → no manual restart cycle
2. **Self-contained**: No external repo dependency; everything declared in `Tiltfile`
3. **Dashboard visibility**: Real-time logs, build status, health for all services
4. **Production-like**: Runs on K8s, closer to actual AKS deployment topology
5. **Multi-context development**: Easy to add hearing, progression, etc. alongside listing

### 3.4 Drawbacks for listing

1. **K8s prerequisite**: Every developer needs kind/minikube + kubectl + ctlptl
2. **Resource-heavy**: K8s cluster + PostgreSQL + Artemis + Wildfly = significant RAM/CPU
3. **Learning curve**: Starlark scripting, K8s manifests, Helm chart knowledge
4. **Slower cold start**: K8s pod scheduling + image pulls vs. Docker Compose
5. **CI complexity**: Azure DevOps pipelines need kind cluster setup

---

## 4. Technology Analysis: Testcontainers

### 4.1 What Is Testcontainers?

Testcontainers is a Java library that provides lightweight, throwaway instances of Docker containers for integration testing. Containers are managed programmatically from JUnit test code:

- **JUnit 5 integration**: `@Testcontainers` + `@Container` annotations
- **Module library**: Pre-built modules for PostgreSQL, Kafka, etc.
- **GenericContainer**: Run any Docker image as a container
- **Network isolation**: Containers get their own Docker network per test suite
- **Automatic cleanup**: Containers destroyed when tests complete

### 4.2 Suitability for cpp-context-listing

| Aspect | Assessment | Notes |
|--------|------------|-------|
| **PostgreSQL** | Excellent | `PostgreSQLContainer` module with built-in Liquibase support |
| **Artemis** | Good | `GenericContainer` with official Artemis image |
| **Wildfly + WAR** | Moderate | `GenericContainer` + volume-mount WAR; or embedded Arquillian |
| **HAProxy** | Not needed | Direct container-to-container networking via `Network.newNetwork()` |
| **Liquibase** | Excellent | Run from test setup, or via JDBC container module |
| **WireMock** | Excellent | Dedicated WireMock container module available |
| **6-database Liquibase** | Good | Programmatic: create 3 databases on same PostgreSQL container, run migrations |
| **CI/CD integration** | Excellent | Only needs Docker daemon; works natively in Maven |
| **Test isolation** | Excellent | Fresh containers per test class or suite |

### 4.3 Benefits for listing

1. **Zero external dependencies**: No `cpp-developers-docker`, no `CPP_DOCKER_DIR`, no shell scripts
2. **Self-contained tests**: All infrastructure declared in Java test code alongside the tests
3. **CI/CD native**: Works anywhere Docker runs — Azure DevOps, GitHub Actions, local
4. **Test isolation**: Each test run gets fresh containers → no flaky shared state
5. **Dynamic port allocation**: No port conflicts; containers use random available ports
6. **Faster feedback**: Can start only the containers each test class needs
7. **Version-locked**: Container versions pinned in `pom.xml`, not in external repo

### 4.4 Drawbacks for listing

1. **Not a development tool**: Does not provide live-reload or development workflow
2. **Full-stack complexity**: Running Wildfly + WAR deployment inside Testcontainers requires custom container setup
3. **Test startup time**: Starting PostgreSQL + Artemis + Wildfly per test suite adds ~30-60s
4. **Resource consumption**: Docker containers during test execution (mitigated by reusable containers)
5. **Learning curve**: Developers need to understand Testcontainers API and container networking

---

## 5. Key Questions Answered

### 5.1 Will this remove the dependency on cpp-developers-docker?

| Technology | Answer |
|-----------|--------|
| **Tilt.dev** | **Yes, fully.** All services (PostgreSQL, Artemis, Wildfly, HAProxy, Liquibase) are defined in the Tiltfile using K8s manifests. The `runIntegrationTests.sh` script and all sourced functions from cpp-developers-docker become unnecessary. |
| **Testcontainers** | **Yes, for integration tests.** All containers are declared in Java test code. The `runIntegrationTests.sh` script is replaced by `mvn verify -Plisting-integration-test`. However, Testcontainers does not replace the development workflow — developers still need some way to run the service locally for manual testing. |

**Recommendation**: For complete removal of cpp-developers-docker, use **Testcontainers for tests** and a simple **docker-compose.yml within the listing repo** for local development.

### 5.2 Is there anything cpp-developers-docker can do that these technologies cannot?

| Capability | cpp-developers-docker | Tilt.dev | Testcontainers |
|-----------|----------------------|----------|----------------|
| Shared PostgreSQL for all 70+ contexts | Yes (init.sql creates all users/databases) | No (scoped to declared contexts) | No (test-scoped) |
| Multi-context WAR deployment in same Wildfly | Yes (deploy multiple WARs) | Yes (with configuration) | Not practical |
| Elasticsearch profile | Yes (`--profile es`) | Yes (K8s Deployment) | Yes (ElasticsearchContainer) |
| Docmosis profile | Yes (`--profile docmosis`) | Yes (K8s Deployment) | Yes (GenericContainer) |
| Alfresco stack | Yes (`--profile alfresco`) | Yes (complex, 6 containers) | Yes (complex setup) |
| HAProxy URL rewriting | Yes (haproxy.cfg) | K8s Ingress replaces this | Not needed (direct access) |
| Remote debug (JDWP 8787) | Yes | Yes (K8s port-forward) | N/A (test context) |
| Cross-context event flow testing | Yes (deploy multiple WARs, same Artemis) | Yes (multi-resource Tiltfile) | Possible but complex |

**Gaps**:
- **Tilt.dev**: Cannot replicate the "shared environment for 70+ contexts" model, but this is actually a benefit — isolation prevents cross-context interference.
- **Testcontainers**: Not suited for manual/exploratory development workflows. Does not provide a persistent running environment.
- **Neither**: The existing `socat` bridge (Wildfly calling itself via HAProxy on localhost:8080) requires careful network configuration in both technologies.

### 5.3 Is it going to be easy for team use?

#### Tilt.dev — Team Adoption Assessment

| Factor | Difficulty | Notes |
|--------|-----------|-------|
| **Prerequisites** | Medium | Install: Tilt CLI, kind/minikube, kubectl, ctlptl, Docker |
| **Daily workflow** | Easy | `tilt up` → dashboard → code → auto-reload |
| **Troubleshooting** | Medium | K8s knowledge needed for pod issues, networking |
| **Onboarding** | Medium | 1-2 hour setup + walkthrough needed |
| **Maintenance** | Medium | K8s manifests + Tiltfile need updating with infra changes |

**Verdict**: Medium difficulty. Developers comfortable with Docker will need to additionally learn K8s concepts. The Tilt dashboard significantly lowers the barrier once set up.

#### Testcontainers — Team Adoption Assessment

| Factor | Difficulty | Notes |
|--------|-----------|-------|
| **Prerequisites** | Low | Only Docker Desktop + Maven (already required) |
| **Daily workflow** | Easy | `mvn verify -Plisting-integration-test` — no external setup |
| **Troubleshooting** | Low | Standard Java debugging; container logs accessible programmatically |
| **Onboarding** | Low | Familiar JUnit patterns; no new CLI tools |
| **Maintenance** | Low | Container versions in pom.xml; test setup in Java code |

**Verdict**: Low difficulty. The Java-native API means no new tooling beyond what developers already use. Tests are self-documenting and version-controlled alongside the code.

---

## 6. Recommendation

### For Integration Testing: **Testcontainers** (Strong Recommendation)

Testcontainers is the better fit for replacing `runIntegrationTests.sh` and the cpp-developers-docker dependency for test execution:

- Completely self-contained within the listing repository
- Works identically on developer machines and CI/CD pipelines
- Lower adoption barrier for the Java development team
- Test isolation eliminates flaky test issues from shared state

### For Local Development: **Docker Compose** (in-repo) or **Tilt.dev** (if K8s workflow desired)

- A simple `docker-compose.yml` within the listing repo covers most local development needs
- Tilt.dev is worth investing in only if the team wants K8s-native development with live-reload across multiple contexts

### Phased Approach

| Phase | Action | Effort |
|-------|--------|--------|
| **Phase 1** | Adopt Testcontainers for integration tests | 2-3 weeks |
| **Phase 2** | Add in-repo `docker-compose.yml` for local dev | 1 week |
| **Phase 3** | Evaluate Tilt.dev for multi-context development | 2-4 weeks |

---

## 7. Comparison Matrix

| Criterion | cpp-developers-docker | Tilt.dev | Testcontainers |
|-----------|----------------------|----------|----------------|
| Setup complexity | High (clone repo, set env vars, learn scripts) | Medium (K8s + Tilt CLI) | Low (Maven dependency) |
| CI/CD readiness | Poor (needs Docker Compose in pipeline) | Moderate (needs kind in pipeline) | Excellent (Docker only) |
| Test isolation | Poor (shared containers across runs) | Good (K8s namespace isolation) | Excellent (fresh per test) |
| Cold start time | ~3-5 min (full compose up) | ~2-4 min (K8s pods) | ~1-2 min (per test suite) |
| Resource usage | High (all services always running) | High (K8s overhead) | Moderate (only needed containers) |
| Live development | Manual (rebuild + redeploy) | Excellent (auto-reload) | N/A |
| Version control | External repo (version drift risk) | In-repo (Tiltfile + manifests) | In-repo (pom.xml + Java code) |
| Cross-context | Built-in (70+ contexts) | Configurable (Tiltfile resources) | Complex (multiple container setups) |
| Debugging | Docker exec + logs | K8s port-forward + dashboard | Java IDE debugger + container logs |
| Team familiarity | Known (current) | New technology | Familiar (Java + JUnit) |
