# Account lockout after a counter write failure

Issue: OpenIdentityPlatform/OpenAM#1107. The issue has no comments or linked PRs.

A failed counter write must retain the current failed attempt and switch that
identity to server-local memory locking. Keep a positive configured duration;
use 30 minutes when the configured duration is zero. Apply this policy to special
identities too; do not introduce name-based exemptions.

## Approach

Use shared, per-identity fallback state keyed by the existing normalized identity
identifier. Keep the configured persistence flag unchanged so callers continue
resolving aliases to that identifier. Reads, increments, lock checks and resets
must use the fallback state across newly constructed authentication helpers.
Serialize updates of each fallback counter and preserve existing persisted counts.

An instance-only flag cannot survive new authentication requests. A realm-wide
switch unnecessarily changes healthy identities when only one entry rejects writes.
The user accepted the recommended per-identity approach and 30-minute default.

Log the transition once per identity at error level, including the original error
and that counters are per-server and lost on restart. Remain in memory until the
server restarts; automatic restoration to persistence is outside this change.
Writable stores and explicitly configured memory mode retain their behavior.
A read failure alone does not activate fallback; a subsequent failed write does.
