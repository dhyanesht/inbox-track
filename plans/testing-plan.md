# Inbox-Track Comprehensive Testing Plan

## Project Overview

**Project:** Inbox-Track (Job Application Tracker)  
**Tech Stack:**
- **Frontend:** React 18 + Vite + TypeScript + shadcn/ui + TanStack Query
- **Backend:** Java Spring Boot 4.0.2 + JPA/Hibernate
- **Database:** PostgreSQL (via Supabase) with Flyway migrations
- **Integrations:** Gmail API, Supabase Edge Functions, Ollama/Groq AI services

---

## Current Testing Assessment

### Existing Tests (Minimal)

| Component | Location | Status |
|-----------|----------|--------|
| Backend Unit Tests | `java/src/test/java/com/dino/inbox_track/` | 2 placeholder tests with no actual assertions |
| Frontend Tests | None | Jest NOT installed (Vitest available but unused) |
| Integration Tests | None | Not implemented |
| E2E Tests | None | Not implemented |
| Coverage Reports | None | Not configured |

### Key Findings

1. **Backend Tests** - The existing `InboxTrackApplicationTests.java` and `ServiceTest.java` are placeholder tests that don't actually test anything meaningful
2. **Frontend Tests** - Jest will be installed; Vitest is currently available but unused
3. **No Test Infrastructure** - No test databases, mock servers, or CI/CD integration
4. **High-Risk Areas Untested**:
   - AI classification logic (`OllamaService`, `GroqLangChainService`)
   - Email parsing (`EmailParsingService`)
   - Supabase Edge Functions
   - Gmail API integration

---

## Testing Strategy

### Testing Pyramid for Inbox-Track

```
        ┌─────────────┐
        │    E2E      │  ← 10% (Critical user journeys)
        │   Tests     │
       ┌─────────────┐
       │ Integration │  ← 20% (API, DB, External services)
       │   Tests     │
      ┌─────────────┐
      │    Unit     │  ← 70% (Business logic, utilities)
      │   Tests     │
     └─────────────┘
```

### Recommended Test Types

| Test Type | Purpose | Coverage Target |
|-----------|---------|-----------------|
| **Unit Tests** | Test individual components, services, utilities in isolation | 70-80% |
| **Integration Tests** | Test API endpoints, database operations, external service mocks | 15-20% |
| **E2E Tests** | Critical user flows (auth, job creation, email sync) | 5-10% |

---

## Recommended Testing Tools & Frameworks

### Frontend (React + Vite + TypeScript)

| Purpose | Tool | Rationale |
|---------|------|------------|
| Test Runner | **Jest 29.x** | Industry standard, extensive ecosystem, excellent TypeScript support |
| Component Testing | **React Testing Library** | Industry standard, user-centric |
| HTTP Mocking | **MSW (Mock Service Worker)** | Intercept requests, test API layers |
| Coverage | **Jest Coverage** | Built-in with V8/istanbul |
| E2E Testing | **Playwright** | Best for modern React apps, supports MSW |
| Snapshot Testing | **Jest Snapshots** | Built-in snapshot testing support |

### Backend (Java Spring Boot)

| Purpose | Tool | Rationale |
|---------|------|------------|
| Unit Testing | **JUnit 5** | Standard, already included |
| Mocking | **Mockito** | Industry standard, Spring Boot default |
| Assertion | **AssertJ** | Fluent assertions |
| Integration Testing | **Spring Boot Test** + Testcontainers | Docker-based test DB |
| E2E/API Testing | **RestAssured** | REST API testing |
| Coverage | **JaCoCo** | Java coverage standard |

### Database Testing

| Purpose | Tool | Rationale |
|---------|------|------------|
| Test Database | **Testcontainers** | Spin up PostgreSQL in Docker |
| Database Fixtures | **Testcontainers + Flyway** | Run migrations on test DB |
| Query Testing | **H2 Database** (for unit tests) | In-memory, fast |

---

## Test Environment Requirements

### Local Development Environment

| Component | Requirement | Notes |
|-----------|-------------|-------|
| Node.js | v18+ | Frontend testing |
| Java | JDK 17+ | Backend testing |
| Docker | Latest | Testcontainers |
| PostgreSQL | Same version as production | For integration tests |
| Ollama | Running locally (optional) | For AI service tests |

### Test Databases

```
┌─────────────────────────────────────────────────────┐
│              Test Environment Setup                  │
├─────────────────────────────────────────────────────┤
│  Unit Tests        →  H2 In-Memory                  │
│  Integration Tests →  Testcontainers (PostgreSQL)   │
│  E2E Tests         →  Dedicated Test DB            │
└─────────────────────────────────────────────────────┘
```

### Environment Variables for Tests

```bash
# Test-specific overrides
DB_JDBC_URL=jdbc:postgresql://localhost:5432/inbox_track_test
DB_USER=test_user
DB_PASSWORD=test_password
GROQ_API_KEY=test-key
OLLAMA_URL=http://localhost:11434
SPRING_PROFILES_ACTIVE=test
```

---

## Test Scenario Prioritization

### Priority 1: Critical Path (Must Have Before Release)

| Feature | Risk Level | Test Type | Scenarios |
|---------|------------|-----------|-----------|
| **Authentication** | Critical | Unit + E2E | Login, logout, session validation |
| **Job Application CRUD** | Critical | Unit + Integration | Create, read, update, delete applications |
| **Email Sync** | High | Integration | Gmail API connection, email retrieval |
| **AI Classification** | High | Unit + Integration | Job vs non-job classification, status extraction |

### Priority 2: Core Business Logic

| Feature | Risk Level | Test Type | Scenarios |
|---------|------------|-----------|-----------|
| **Status Mapping** | Medium | Unit | Application status transitions |
| **Email Parsing** | Medium | Unit | Extract company, position, date from email |
| **Dashboard Stats** | Medium | Unit + Component | Calculate application counts by status |
| **Supabase Functions** | Medium | Integration | Edge function responses |

### Priority 3: UI/UX Features

| Feature | Risk Level | Test Type | Scenarios |
|---------|------------|-----------|-----------|
| **Application Dialog** | Low | Component | Form validation, submission |
| **Dashboard Components** | Low | Component | Rendering, interactions |
| **Navigation** | Low | Component | Route changes |

---

## Gap Analysis & Recommendations

### Current Gaps

| Gap | Severity | Impact |
|-----|----------|--------|
| **No Frontend Tests** | Critical | UI bugs not caught, regression risk |
| **No Backend Unit Tests** | Critical | Business logic untested |
| **No API Integration Tests** | High | Backend API contract untested |
| **No AI Service Tests** | High | Classification failures undetected |
| **No E2E Tests** | High | Critical flows break silently |
| **No Test Coverage Reports** | Medium | Can't measure test effectiveness |
| **No CI/CD Integration** | Medium | Tests not run automatically |

### Recommendations

1. **Immediate Actions (Phase 1)**
   - Add meaningful unit tests for `JobService`, `OllamaService`, `EmailParsingService`
   - Set up Jest configuration for frontend component tests
   - Configure test database with Testcontainers
   - Add basic component tests for critical UI components

2. **Short-term (Phase 2)**
   - Implement API integration tests for controllers
   - Add MSW for frontend API mocking
   - Set up coverage reporting (JaCoCo + Jest coverage)
   - Create smoke tests for Supabase Edge Functions

3. **Medium-term (Phase 3)**
   - Implement E2E tests with Playwright
   - Add AI service integration tests with mocked Ollama/Groq
   - Set up CI/CD pipeline with test execution
   - Add performance/load tests for critical endpoints

---

## Implementation Phases

### Phase 1: Foundation (Weeks 1-2)

```
✓ Configure test infrastructure
  ├── Jest setup with React Testing Library
  ├── MSW for API mocking
  ├── Testcontainers for PostgreSQL
  ├── JaCoCo plugin configuration
  └── Test application.yaml profiles

✓ Write unit tests for core services
  ├── JobService (CRUD operations)
  ├── EmailParsingService (parsing logic)
  ├── OllamaService (classification, fallback)
  └── Status mapping utilities
```

**Deliverables:**
- Test configuration files
- 20+ unit tests for backend services
- 10+ component tests for frontend

### Phase 2: Integration (Weeks 3-4)

```
✓ API Integration Tests
  ├── GmailController endpoints
  ├── JobController CRUD endpoints
  └── LangChainController endpoints

✓ Frontend Integration Tests
  ├── ApplicationsList component
  ├── ApplicationDialog form
  └── DashboardStats calculations

✓ Edge Function Tests
  ├── parse-job-content
  ├── categorize-email
  └── sync-emails (mocked Gmail)
```

**Deliverables:**
- 15+ API integration tests
- 10+ frontend integration tests
- Edge function test utilities
- Coverage reports > 50%

### Phase 3: E2E & Automation (Weeks 5-6)

```
✓ E2E Test Scenarios
  ├── User authentication flow
  ├── Create new job application
  ├── Sync emails from Gmail
  ├── Update application status
  └── Dashboard data display

✓ CI/CD Integration
  ├── GitHub Actions workflow
  ├── Test execution on PR
  ├── Coverage gates
  └── Test reports
```

**Deliverables:**
- 10+ E2E test scenarios
- CI/CD pipeline with test stages
- Coverage reports > 70%
- Test documentation

---

## Coverage Targets

| Layer | Initial Target | Long-term Target |
|-------|-----------------|------------------|
| **Business Logic** | 80% | 90% |
| **Controllers/API** | 60% | 80% |
| **Frontend Components** | 50% | 70% |
| **Overall** | 60% | 75% |

---

## Next Steps

1. **Approve this plan** - Confirm testing strategy and priorities
2. **Set up test infrastructure** - Configure Jest, Testcontainers, JaCoCo
3. **Begin Phase 1** - Start with critical path unit tests
4. **Establish CI/CD** - Add test execution to build pipeline
5. **Iterate** - Review coverage, add tests based on production issues

---

*Generated: 2026-03-07*
*Project: Inbox-Track*
*Author: Architect Mode Analysis*
