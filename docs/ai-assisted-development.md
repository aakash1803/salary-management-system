# AI-Assisted Development

> I use AI to accelerate implementation and exploration, but I do not accept generated code without understanding and validating it.

---

### How I Work With AI

My workflow when building features with AI assistance follows a disciplined feedback loop:

```text
Understand requirement
→ inspect existing code
→ ask AI for a proposal
→ review the proposal
→ challenge assumptions
→ implement/refine
→ test and verify
→ accept only after understanding the result
```

I treat AI as a rapid prototyping partner and technical pair programmer. While AI accelerates coding, I remain responsible for architectural decisions, code quality, security, performance, and test verification.

---

### Examples From This Project

#### 1. Bulk Current Salary Lookup (Avoiding N+1 Queries)
When building the employee management UI, I noticed that the employee list already displayed **Current Salary** and **Currency** columns, but the employee list API did not initially provide those values. I considered the straightforward option of fetching salary details separately for each employee, but that would introduce an N+1 request pattern as the page size grows. I therefore directed the implementation toward a backend bulk lookup: fetch the salary records for the employee IDs on the current page in one query, select the applicable current salary per employee, and include it in the list response. I reviewed the query and service logic, added tests for the edge cases, and verified the final behavior.

#### 2. Docker Database Seeder Resolution
During containerization, the initial AI proposal for the database seeder container invoked `./gradlew seedDatabase` directly inside the runtime container. When executing `docker compose --profile tools run --rm seeder`, the container timed out trying to download the Gradle distribution wrapper from an external network endpoint at runtime.

Instead of accepting the broken wrapper setup, I analyzed the failure logs, identified the root cause, and instructed AI to refine the seeder Dockerfile stage. The revised design compiles the seeder class during the multi-stage build and executes `com.salarymanagement.seed.DatabaseSeederApplication` directly using Java 21. I verified that the seeder ran as a non-root user, connected to the mounted `sqlite_data` volume, and seeded 10,000 employees and 19,999 salary records deterministically in seconds.

---

### When AI Gets It Wrong

The Docker database seeder workflow was a clear example of AI producing a plausible but flawed initial proposal:

1. **Initial Proposal**: AI generated a seeder container image that ran `./gradlew seedDatabase` at container launch time.
2. **Failure Identified**: Running the container failed with network timeouts because the runtime environment lacked Gradle distribution caching.
3. **Diagnosis**: I inspected the container logs, recognized that running a build tool wrapper inside a lightweight runtime image was bad practice, and identified the runtime dependency flaw.
4. **Correction**: I instructed AI to leverage the existing multi-stage build artifacts and execute the compiled `DatabaseSeederApplication` class directly with Java 21.
5. **Verification**: I reviewed the updated `Dockerfile`, built the image, ran `docker compose --profile tools run --rm seeder`, and confirmed that the database was seeded cleanly without external network calls.

---

### Representative Prompt Patterns

Below are representative prompt patterns I use to guide AI towards clean, reviewable proposals:

```text
Review the employee list implementation and identify how to provide current salary
without introducing one HTTP request per employee. Propose the smallest change that
avoids an N+1 query pattern.
```

```text
Review this implementation against the requirement and existing architecture.
Identify incorrect assumptions, performance issues, security concerns, and missing tests.
Do not assume the generated code is correct.
```

---

### How I Validate AI Output

Before accepting any AI-generated proposal into the codebase, I routinely:

- **Read the Diff**: Examine every line of modified code for unexpected side effects or structural bloat.
- **Verify Architectural Alignment**: Ensure new code respects established domain modules (`auth`, `employee`, `salary`, `dashboard`) and layered boundaries (`Controller → Service → Repository`).
- **Check Security & Performance**: Confirm that secrets are not committed, authentication headers/cookies are handled properly, and database queries do not introduce N+1 patterns.
- **Run Automated Tests**: Execute `./gradlew build` for backend tests, `npm test -- --watch=false` for frontend tests, and full-system HTTP acceptance tests (`FullSystemAcceptanceTest.java`).
- **Validate Real Runtime Behavior**: Test containerized builds via `docker compose up --build` and confirm endpoint responses.
- **Leverage CI**: Rely on GitHub Actions (`.github/workflows/ci.yml`) as an independent validation gate.

---

### Principles

- **Understand before accepting**: Never check in code you cannot explain.
- **Treat AI output as a proposal, not authority**: Question assumptions, naming, and structure.
- **Prefer the smallest justified change**: Avoid unnecessary abstractions or framework additions.
- **Validate behavior, not just compilation**: Ensure tests verify real contract requirements.
- **Review security, performance, and maintainability**: Protect token security, optimize queries, and keep modules decoupled.
- **Verify in real runtimes**: Run tests and containers locally rather than assuming code works.
- **Refine prompts when initial output strays**: Provide clear architectural boundaries when AI misses constraints.
- **Keep changes incremental and reviewable**: Build and verify functionality step by step.
