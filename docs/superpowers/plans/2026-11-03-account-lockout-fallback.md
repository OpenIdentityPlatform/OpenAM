# Account Lockout Fallback Implementation Plan

> **For agentic workers:** Use superpowers:executing-plans to implement this plan. Do not commit, push or publish without separate user instructions.

**Goal:** Preserve account lockout when persisting a failed-attempt counter fails.
**Architecture:** Shared per-identity fallback counters, stable identity resolution, and effective memory-lock checks across authentication requests.
**Tech Stack:** Java 11, Maven, TestNG, Mockito/PowerMock.
**Spec:** docs/superpowers/specs/2026-11-03-account-lockout-fallback-design.md

## Global Constraints

- Fallback duration is 30 minutes for configured zero; otherwise retain configured duration.
- No new special-user exemptions, new dependencies, or external writes.
- Source copyright notices must cover 2026 for 3A Systems, LLC.
- Leave all changes uncommitted for user review.

## Review Focus

- Concurrent failures and stale state must not lose increments during transition.
- A failure at the threshold must lock immediately, preserving persisted history.
- Alias/DN and separate-realm lookups must not bypass or share the fallback.
- Expiry and successful-login resets must retain fallback without directory writes.
- Healthy identities and explicit memory mode must retain their behavior.

### Task 1: Counter fallback and authentication enforcement

**Files:**
- Modify: openam-core/src/main/java/com/sun/identity/common/ISAccountLockout.java
- Modify: openam-core/src/main/java/com/sun/identity/authentication/service/AMAccountLockout.java
- Create: openam-core/src/test/java/com/sun/identity/common/ISAccountLockoutTest.java
- Create: openam-core/src/test/java/com/sun/identity/authentication/service/AMAccountLockoutTest.java

**Interfaces:** Retain the configured persistence flag. Add `isMemoryLocking(String userDN)` for effective per-identity mode; existing methods retain their signatures.

- [x] Write regression tests: repeated failed stores across new instances lock at five, retain an existing count of four, expire at the configured deadline, default to 30 minutes for zero, reset after success, isolate identities, and serialize concurrent failures.
- [x] Run tests against current code and confirm behavioral assertion failures.
- [x] Implement shared fallback state in ISAccountLockout with one transition error and no further persistence attempts, including resets. Use the existing injected clock consistently on touched lock decisions.
- [x] Test AMAccountLockout with fresh instances and alias/DN lookups for zero-duration fallback; fix effective-mode checks without changing identity keys.
- [x] Run `JAVA_HOME=/usr/local/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home mvn -pl openam-core -am test -Dtest=ISAccountLockoutTest,AMAccountLockoutTest -Dsurefire.failIfNoSpecifiedTests=false` and inspect results. Expected: all targeted tests pass.
- [x] Review the whole diff independently; resolve important findings with regression tests.
- [x] Run appropriate neighboring authentication tests and `git diff --check`. Show diff and proposed commit message; stop before git actions.

## Verification

- Initial core regressions: 8 failures out of 11 on unchanged production code.
- Initial caller regressions: 3 failures out of 4 on unchanged production code.
- Independent review covered cached snapshots, publication, single-attempt thresholds,
  zero multipliers, physical locks, and atomic expiry/reset; all findings addressed.
- Removing atomic expiry/reset makes `delayedExpiryResetCannotEraseANewLockoutCycle`
  fail; the atomic implementation was restored before the final build.
- A stored duration from an earlier configuration must not override the 30-minute
  fallback when the current configured duration is zero; regression verified RED/GREEN.
- Final reactor build selected all 236 standard openam-core test classes:
  2287 tests passed, zero failures/errors/skips, including all 29 new lockout cases.
- `git diff --check` passed. No commits, pushes, or GitHub publications performed.
