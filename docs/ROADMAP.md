# Anychain Rust Roadmap

This roadmap starts from the restored Rust line on `main` and keeps the Kotlin migration available as a reference in the `archive/kotlin` branch and `kotlin-reference` tag.

## Goal

Rebuild Anychain as a Rust codebase that is easy to reason about in production: clear module boundaries, durable persistence, explicit concurrency, good test coverage, and enough observability to debug real workloads.

## Priority Order

### 1. Stabilize the core architecture

- Split domain logic from infrastructure concerns.
- Keep `Block`, `Transaction`, and `Blockchain` in a pure core layer.
- Introduce small traits for hashing, clocks, and persistence so behavior can be tested in isolation.
- Remove any direct I/O from the core path unless it is explicitly a storage adapter.

### 2. Harden persistence

- Wrap the database behind a repository or store abstraction.
- Make tip updates atomic and easy to recover after interruption.
- Add corruption and missing-data handling instead of assuming a perfect database.
- Keep serialization format decisions explicit and versionable.

### 3. Define clean CLI and API boundaries

- Keep command parsing and HTTP handlers thin.
- Put orchestration in a shared application service rather than inside transport code.
- Reuse the same core service from CLI and API.
- Make request validation and error translation consistent across both entry points.

### 4. Prepare concurrency and async behavior

- Choose a clear shared-state strategy for the chain.
- Make mining, validation, and reads safe under concurrent load.
- Add cancellation and graceful shutdown paths early.
- Prefer explicit ownership over hidden locking behavior.

### 5. Build the test pyramid

- Add focused unit tests for domain behavior.
- Add persistence round-trip tests.
- Add CLI and API integration tests for the main flows.
- Add regression tests for chain validation, mining, and recovery paths.

### 6. Add observability hooks

- Use structured logging with useful request and block identifiers.
- Add timing around mining, persistence, and validation.
- Expose lightweight health and readiness checks.
- Leave room for metrics without coupling the core to a monitoring stack.

### 7. Improve documentation last

- Document the module layout and data flow.
- Explain how persistence works and how recovery behaves.
- Keep a short operator-friendly runbook for local execution and debugging.

## Near-Term Implementation Shape

- `anychain-core`: domain types, validation, mining, and repository traits.
- `anychain-storage`: persistence adapters and serialization helpers.
- `anychain-cli`: user-facing commands only.
- `anychain-api`: transport layer plus request/response mapping.
- `anychain-app`: shared orchestration if the split starts to feel too crowded in the binaries.

## Definition of Done for the Next Major Pass

- Core logic can be exercised without starting the CLI or API.
- Storage can be swapped or mocked without touching domain code.
- API and CLI share the same behavior for the same operation.
- Tests cover the main success paths and the most likely failure paths.
- Logs make it possible to trace a request or mining action end to end.
