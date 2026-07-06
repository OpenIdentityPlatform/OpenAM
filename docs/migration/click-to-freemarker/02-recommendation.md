# Recommendation: replace Apache Click with Jakarta Servlets + FreeMarker

## Decision

Migrate the OpenAM configurator/upgrade wizard from Apache Click to **plain Jakarta Servlets +
FreeMarker** server-rendered templates. This lets us delete the ~36k-LOC vendored Click fork and
the abandoned upstream `org.apache.click` jars while keeping the existing server-rendered +
AJAX-validation architecture and all backend coupling intact.

## Constraints that shaped the choice

- **Runs pre-configuration.** The wizard serves a bare, unconfigured WAR — before the
  CREST/CHF/Guice OpenAM runtime is up. The replacement must not depend on that runtime.
- **JDK 11 / Jakarta EE 9 (Servlet 5.x).** The codebase is 100% `jakarta.servlet` already; the
  replacement must be Jakarta-namespace clean.
- **Small, self-contained surface.** 13 page classes + 12 templates; thin backend coupling
  (`AMSetupServlet`, `UpgradeServices`, OpenDJ). No admin-console/auth/federation impact.
- **Goal is debt removal**, not a product rewrite — the primary win is deleting the fork.

## Why FreeMarker + Servlets

- **No new dependency category.** `org.freemarker:freemarker` 2.3.31 is already a managed dependency
  used by `openam-oauth2` (`.ftl` consent/error/form-post pages); `jakarta.servlet-api` is already
  `provided` in `openam-core`.
- **Runs fine pre-config** — plain servlets + a servlet-agnostic template engine.
- **Nearly mechanical template port.** Velocity `$page.getLocalizedString("k")` / `$context` /
  `$path` / `$startingTab` → FreeMarker `${...}`. The embedded YUI JS and the
  `{"valid":..,"body":..}` AJAX contract carry over unchanged.
- **Reuses the valuable code.** `AjaxPage`'s validation/i18n/LDAP/JSON helpers are already
  framework-agnostic; only the Click seams (`extends Page`, `ActionLink`, `getContext()`) change.
- **Deletes the most code for the least new surface** — the whole point.

## Alternatives considered (and why not)

- **React SPA + JAX-RS (Jersey 3.1.10, already on classpath).** Aligns with the XUI React/Vite
  modernization direction, but adds a front-end build pipeline and a bigger rewrite for a one-time
  bootstrap UI. Overkill here. *Revisit only if the configurator is folded into the XUI React app.*
- **ForgeRock CREST / CHF (the house REST standard).** Resource/CRUD-oriented — a poor fit for a
  stateful multi-step wizard — and the CREST/Guice runtime isn't reliably available at setup time.
- **JSP + JSTL** (the other dominant legacy templating, ~445 JSPs). Zero new deps, but doubles down
  on legacy tech the project is otherwise moving away from.
- **Spring Boot / Spring MVC.** Only present in the isolated JDK-17 `openam-mcp-server` module;
  heavy new deps on a JDK-11 core bootstrap path. Rejected.

## Delivery strategy

Incremental, de-risked: **pilot one wizard step end-to-end on a parallel route** (nothing existing
breaks), verify, lock the repeatable pattern, then roll out the remaining pages and remove Click.
See `03-migration-plan.md`.
