# gitops-app

A minimal Java Spring Boot application built as the deployment target for a GitOps + Observability pipeline. The real focus of this project is the infrastructure and automation around it: a Jenkins CI pipeline, Docker image builds, and GitOps-based deployments via ArgoCD.

---

## Endpoints

| Method | Endpoint | Description | Response |
|---|---|---|---|
| GET | `/health` | Custom lightweight health check. Used for load balancer pings and smoke tests after deployment. | `{"status": "ok"}` |
| GET | `/hello` | Simple greeting endpoint to verify the app is serving responses. | `{"message": "Hello from GitOps pipeline!"}` |
| GET | `/actuator/health` | Spring Boot Actuator built-in health endpoint. Reports deeper health status of the application. Used for Kubernetes liveness and readiness probes. | `{"status": "UP"}` |
| GET | `/actuator/prometheus` | Exposes application metrics in Prometheus exposition format. Scraped by Prometheus in Phase 2. | Prometheus metrics text |
| GET | `/actuator/*` | All other Spring Boot Actuator endpoints (info, env, beans, etc.) are exposed for development visibility. | Varies |

---

## Tech Stack

| Component | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2.5 |
| Build Tool | Maven |
| Metrics | Micrometer + Prometheus Registry |
| Runtime | Spring Boot Embedded Tomcat |
| Container | Docker (Eclipse Temurin 17 JRE) |

---

## CI/CD Pipeline — Jenkins Requirements

### Installed Software on Jenkins Agent

| Software | Purpose |
|---|---|
| Java 17 | Run Maven and compile the app |
| Maven 3.9+ | Build and test the application |
| Docker | Build and push container images |
| Git | Checkout source code and push to config repo |
| `yq` | YAML-aware image tag update in config repo |


---

### Jenkins Credentials

Navigate to `Manage Jenkins → Credentials → Global → Add Credential` and create the following:

| Credential ID | Type | Value | Used In |
|---|---|---|---|
| `github-app-repo` | Username with Password | GitHub username + Fine-grained PAT with `Contents: Read` on `gitops-app` repo | Checkout stage |
| `dockerhub-credentials` | Username with Password | Docker Hub username + password or access token | Docker Push stage |
| `github-token` | Secret Text | Fine-grained PAT with `Contents: Read and Write` on `gitops-config` repo | Update Config Repo stage |
| `git-user-email` | Secret Text | Email to use for git commits in the config repo (e.g. `ci-bot@example.com`) | Update Config Repo stage |
| `git-user-name` | Secret Text | Name to use for git commits in the config repo (e.g. `Jenkins CI`) | Update Config Repo stage |

---

### GitHub PAT Permissions (Fine-grained)

**For `gitops-app` repo (Checkout):**

| Permission | Level |
|---|---|
| Contents | Read-only |
| Metadata | Read-only |

**For `gitops-config` repo (Config update):**

| Permission | Level |
|---|---|
| Contents | Read and Write |
| Metadata | Read-only |

---

### Pipeline Stages

```
Checkout → Maven Build → Unit Tests → SonarQube → Docker Build → Docker Push → Update Config Repo
```

| Stage | Description |
|---|---|
| Checkout | Clones the app repo using `github-app-repo` credential |
| Maven Build | Compiles the app and produces the fat JAR |
| Unit Tests | Runs the test suite and publishes JUnit results to Jenkins |
| SonarQube Analysis | disabled |
| Docker Build | Builds the image tagged with the Git commit SHA |
| Docker Push | Pushes image to Docker Hub, cleans up local image after |
| Update Config Repo | Clones `gitops-config`, updates `environments/dev/values.yaml` with new image tag, commits and pushes |

---

## Repository Structure

```
gitops-app/
├── Dockerfile                          # Multi-stage build
├── Jenkinsfile                         # Declarative CI/CD pipeline
├── pom.xml                             # Maven build config
└── src/
    └── main/
        ├── java/
        │   └── com/gitops/app/
        │       ├── Application.java        # Spring Boot entry point
        │       └── HelloController.java    # REST endpoints
        └── resources/
            └── application.properties     # App configuration
```

---

## Related Repositories

| Repository | Purpose |
|---|---|
| `gitops-app` | This repo — application source code and pipeline |
| `gitops-config` | Helm values and environment manifests — watched by ArgoCD |

---