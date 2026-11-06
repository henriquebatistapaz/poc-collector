# Constitution — Multi-Source Collector Mini Project Principles

Immutable principles every SDD, plan or implementation must honor. If a spec conflicts with this document, this document wins — the spec must be revised.

## 1. Language

Code identifiers, comments, APIs, specs, and documentation use English.

## 2. Architecture

1. Keep each source adapter isolated behind a small connector contract and an explicit capability status.
2. Only use access methods documented or explicitly permitted by the source; unsupported sources stay disabled.
3. Normalize and deduplicate items centrally after source-specific parsing.
4. Keep the first approved implementation slice small; add persistence/API/brokers only after a separate decision.

## 3. Quality

- All new code has tests. A build with passing tests is a prerequisite for completing any task.
- No silent exception swallowing.
- Changes must not reduce existing coverage.
- Tests must use local fixtures by default and must not depend on live source availability.

## 4. Security

- No secrets in code or versioned configuration — use environment variables/local profiles.
- SQL always parameterized; external input validated at the boundary.
- New dependencies require justification in the SDD and a check for known CVEs.

## 5. Process (Spec-Driven)

- Non-trivial feature → SDD in `docs/sdd/` before code, following TEMPLATE.sdd.md.
- SDD approved by a human before implementation.
- The SDD's acceptance criteria are the final validation contract.
- Relevant architecture changes recorded in the SDD's decisions section (or a referenced ADR).

## 6. Simplicity

- Prefer the existing pattern in the codebase over introducing a new one.
- Do not add abstractions "for the future" without a present use case.

## 7. Bounded automation

- Every automated loop (fix→test, review→fix→re-review) has an attempt budget — default **3 attempts**. Reaching the limit means STOP and escalate to a human with a summary of what was tried and why it keeps failing.
- Escalating is success, not failure — the forbidden outcomes are looping silently and "resolving" the loop by weakening the check (deleting a test, suppressing a warning, softening a finding).
- Each attempt must be a DIFFERENT hypothesis — retrying the same fix twice counts as one attempt and one wasted slot.
