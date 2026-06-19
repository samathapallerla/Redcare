# GitHub Repository Scorer

A Spring Boot REST API that searches GitHub repositories and ranks them by a composite popularity score based on stars, forks, and recency.

---

## Table of Contents

- [Overview](#overview)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Scoring Algorithm](#scoring-algorithm)
- [Error Handling](#error-handling)
- [Testing](#testing)

---

## Overview

Given a search filter (programming language and/or creation date), the service queries the GitHub Search API, scores each repository on a 0–100 scale, and returns the results sorted highest-score first.

---

## Architecture

```
RepositoryController          (HTTP layer — validates input, delegates)
       │
GithubSearchService           (orchestration — builds query, calls client, triggers scoring)
       │                 │
GithubClientImpl         ScoringService
(GitHub REST API)        (scoring formula + sort)
```

**Package layout:**

```
com.github.reposcore
├── client/         GithubClient interface + GithubClientImpl
├── controller/     RepositoryController
├── dto/            SearchCriteria, SearchResult (sealed: ScoredRepository | Failure)
├── exception/      GithubApiException, GlobalExceptionHandler
└── service/        GithubSearchService, ScoringService
```

Key design decisions:
- `GithubClient` is an interface — the implementation is hidden behind it, making it trivial to mock in tests or swap to a reactive client later.
- `SearchResult` is a **sealed interface** with two permitted subtypes (`ScoredRepository`, `Failure`), giving exhaustive type-safe handling at the HTTP boundary.
- The controller and service layers are intentionally thin — the controller only validates HTTP concerns, and the service only orchestrates; scoring logic lives exclusively in `ScoringService`.

---

## Tech Stack

| Concern | Library |
|---|---|
| Framework | Spring Boot 3.5 |
| HTTP client | Spring `RestClient` |
| Resilience | Resilience4j circuit breaker |
| API docs | SpringDoc OpenAPI (Swagger UI) |
| Testing | JUnit 5, Mockito, MockMvc |
| Java | Java 25 (virtual threads enabled) |

---

## Getting Started

### Prerequisites

- Java 25+
- Maven 3.9+

### Run

```bash
mvn spring-boot:run
```

The server starts on `http://localhost:8080`.

### Swagger UI

```
http://localhost:8080/swagger-ui.html
```

### OpenAPI spec

```
http://localhost:8080/api-docs
```

---

## Configuration

All configuration lives in `src/main/resources/application.yml`.

```yaml
github:
  base-url: https://api.github.com
```


### Circuit breaker

```yaml
resilience4j:
  circuit-breaker:
    instances:
      githubClient:
        sliding-window-size: 10              # requests in the window
        failure-rate-threshold: 50           # open circuit at 50% failure rate
        wait-duration-in-open-state: 30s     # retry after 30 seconds
        permitted-number-of-calls-in-half-open-state: 3
```

When the circuit opens, the API returns `503 Service Unavailable` with a `Failure` body.

---

## API Reference

### `GET /api/v1/repositories/search`

Search and score GitHub repositories. At least one query parameter is required.

**Query parameters:**

| Parameter | Type | Required | Description |
|---|---|---|---|
| `language` | `string` | one of these is required | Filter by programming language (e.g. `Java`, `Python`) |
| `createdAfter` | `date` (ISO-8601: `YYYY-MM-DD`) | one of these is required | Filter repos created after this date |

**Example requests:**

```bash
# By language
GET /api/v1/repositories/search?language=Java

# By creation date
GET /api/v1/repositories/search?createdAfter=2023-01-01

# Combined
GET /api/v1/repositories/search?language=Java&createdAfter=2023-01-01
```

**Success response `200 OK`:**

```json
[
  {
    "id": 1296269,
    "name": "spring-boot",
    "fullName": "spring-projects/spring-boot",
    "description": "Spring Boot",
    "htmlUrl": "https://github.com/spring-projects/spring-boot",
    "stargazersCount": 75000,
    "forksCount": 40000,
    "updatedAt": "2024-06-01T10:00:00Z",
    "createdAt": "2012-10-19T00:00:00Z",
    "language": "Java",
    "popularityScore": 95.0
  }
]
```

Results are sorted by `popularityScore` descending.

**Error responses:**

| Status | Cause |
|---|---|
| `400 Bad Request` | Neither `language` nor `createdAfter` provided, or invalid date format |
| `502 Bad Gateway` | GitHub API returned a 4xx or 5xx error |
| `503 Service Unavailable` | Circuit breaker is open (GitHub API unreachable) |

All error responses share the same body shape:

```json
{
  "status": 400,
  "title": "Bad Request",
  "detail": "At least one of 'language' or 'createdAfter' must be provided"
}
```

---

## Scoring Algorithm

Each repository receives a score from **0 to 100** computed as a weighted sum of three normalized signals:

```
score = (starsScore × 0.5 + forksScore × 0.3 + recencyScore × 0.2) × 100
```

| Signal | Weight | Formula | Rationale |
|---|---|---|---|
| Stars | 50% | `min(stars / 10,000, 1.0)` | Caps at 10k stars = full weight |
| Forks | 30% | `min(forks / 2,000, 1.0)` | Caps at 2k forks = full weight |
| Recency | 20% | `max(0, 1 − daysSinceUpdate / 365)` | Full weight if updated today; 0 if not updated in a year |

Each signal is normalized to **[0, 1]** before weighting so the final score is always within **[0, 100]**.

**Examples:**

| Stars | Forks | Last updated | Score |
|---|---|---|---|
| 10,000 | 2,000 | today | 100.0 |
| 0 | 0 | today | 20.0 |
| 0 | 0 | > 1 year ago | 0.0 |
| 5,000 | 1,000 | 6 months ago | 65.0 |

---

## Error Handling

All exceptions are handled centrally in `GlobalExceptionHandler` and return a consistent `SearchResult.Failure` JSON body:

| Exception | HTTP status |
|---|---|
| `GithubApiException` | Mirrors the upstream GitHub status (e.g. 429 → 429); falls back to 502 |
| `IllegalArgumentException` | 400 Bad Request |
| `ConstraintViolationException` | 400 Bad Request |
| `MethodArgumentTypeMismatchException` | 400 Bad Request |
| `MissingServletRequestParameterException` | 400 Bad Request |

---

## Testing

```bash
mvn test
```

Three test classes cover all layers:

| Test class | Scope | Technique |
|---|---|---|
| `ScoringServiceTest` | Unit | Plain instantiation, no mocks |
| `GithubSearchServiceTest` | Unit | Mockito (`@Mock`, `@InjectMocks`) |
| `RepositoryControllerTest` | Integration slice | `@WebMvcTest` + MockMvc |

`TestFixtures` provides shared factory methods for test data to avoid duplication across test classes.