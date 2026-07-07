# Migration plan: Click → Jakarta Servlets + FreeMarker

Living execution doc. Background in `01-click-inventory.md` and `02-recommendation.md`.

## Context

The configurator/upgrade wizard is the last consumer of the vendored Apache Click 2.3.0 fork
(57 files, ~36k LOC) and the abandoned upstream `org.apache.click` jars. We want that debt gone.
Two hard constraints shape *how*:

1. **Preserve every servlet path.** All existing URLs stay byte-for-byte identical —
   `/config/options.htm`, `/config/wizard/wizard.htm`, `/config/wizard/step1..7.htm`,
   `/config/defaultSummary.htm`, `/config/upgrade/upgrade.htm`. These are baked into `AMSetupFilter`
   (`SETUP_URI = "/config/options.htm"`, `UPGRADE_URI = "/config/upgrade/upgrade.htm"`), into the
   wizard shell's JS (`"$context/config/wizard/step" + n + ".htm"`), and into every step's
   self-posting AJAX (`$context$path?actionLink=<name>`). Changing any URL breaks the wizard.
2. **No one-shot cutover.** Pages move off Click **one at a time**. After each increment the reactor
   builds green and the wizard works end-to-end with Click and FreeMarker running side by side.
   Click, the fork, and the deps are deleted only in the final increment, once nothing uses them.

This supersedes the earlier draft of this plan, which introduced a *new parallel* `/config/setup/*`
route and a bulk rollout. Both are abandoned: the new route would have changed URLs (violating #1),
and bulk rollout violates #2.

## How both constraints are satisfied at once

**A single wrapper servlet owns `*.htm` and routes each request to FreeMarker or Click.**
`ConfiguratorServlet` takes over the `*.htm` mapping Click has today and, per request, consults a
**migrated-page registry** (the `path → PageClass` map it already needs): if the path is migrated it
renders FreeMarker; otherwise it **delegates to the untouched Click fork**. Migrating a page is then a
**Java-only** change (add one registry entry) — no per-page `web.xml` edits.

Delegation to Click uses a **named `RequestDispatcher`**, which is the one trick that makes this work:

- Keep `click-servlet` as a `<servlet>` declaration but **remove its `*.htm` `<servlet-mapping>`** —
  it becomes reachable only by name, not by URL.
- Un-migrated path → `getServletContext().getNamedDispatcher("click-servlet").forward(req, res)`.
- A **named** forward does **not** rewrite the request path, so Click still sees
  `getServletPath() == /config/wizard/step2.htm` and resolves the page exactly as today.
  `ClickServlet.service()` sets up its own thread-locals on that forward; the `/*` filters
  (`amSetupFilter`, audit) already ran once on the original request, so there is no double-filtering
  and no init problem (Click inits lazily on the first forward). The wrapper decides routing before
  writing anything, so the response is never already-committed.

This replaces the per-page exact-`<servlet-mapping>` scheme from the earlier draft: one **one-time**
`web.xml` change in each of the two descriptors (re-point `*.htm` to the wrapper, unmap
`click-servlet`), after which every increment is pure Java. The two Click-excluded trees
(`/config/auth/default/*`, `/config/federation/*`) are `.jsp`/`.xml`, never `.htm`, so `*.htm` never
sees them; the wrapper's registry must likewise only ever claim the known configurator URLs.

Because URLs, the `actionLink` query-param name, and the exact **response byte formats** are all
preserved, **the wizard shell and all embedded step JS need zero changes** as pages migrate. A step
served by FreeMarker is indistinguishable to the shell from one served by Click.

## Corrections to earlier assumptions (verified against source)

- **Step 1's live handler is the base-class `checkPasswords`, not `checkAdminPassword`.** `step1.htm`
  POSTs `?actionLink=checkPasswords` to `AjaxPage.checkPasswords`, which writes **plain text**
  `"true"` or a localized error string via `writeToResponse(...)`. The `ActionLink`s named
  `checkAdminPassword`/`checkAgentPassword` on `Step1.java` are **not referenced by any template**.
- **The `{"valid":.., "body":..}` JSON contract is essentially unused** by these templates. Most
  handlers return plain text (`"true"`/`"ok"`/localized error). The one structured response is
  `validateHostName` (Step 3), which returns a **bespoke** JSON shape
  (`{code, existingPort, embedded, replication, replicationPort, existingStoreHost,
  existingStorePort, message}`) read via `eval(...)`. Each handler's exact output bytes must be
  preserved.
- **`TemplatedForm.java` is dead code** — the only class importing *real* `org.apache.click`, and it
  is referenced nowhere but itself. It can be deleted immediately (increment 0) as a freebie.
  `TemplatedPage.java` is used only by `Options.java`.
- Click auto-exposes a page's **public fields** into the template model (not just `addModel(...)`).
  Step 3 relies on this heavily (`$configStoreHost`, `$type`, `$store`, `$selectEmbedded`, …). The
  replacement servlet must reproduce public-field exposure (see below) so page classes need minimal
  edits.

## Shared infrastructure (built once, in the pilot increment)

New code in `openam-core`, package `org.openidentityplatform.openam.config.servlet`:

- **`ConfiguratorServlet`** (`jakarta.servlet.HttpServlet`) — the wrapper/dispatcher, **mapped to
  `*.htm`**. For each request:
  0. Look up the URL in the **migrated-page registry** (a small explicit `path → class` map — we
     hard-wire the known pages rather than re-implement Click's CamelCase automapping). **Not
     migrated → `getNamedDispatcher("click-servlet").forward(req, res)`** and return. Migrated →
     continue.
  1. Instantiate the page class, attach a `ConfiguratorContext`, call `onSecurityCheck()`; if it
     returns false (already configured), render nothing — the port of `ProtectedPage.onSecurityCheck`.
  2. If the request carries `?actionLink=<name>` (on GET or POST — Click checks both), **dispatch to
     the same-named handler method** and stop (the handler writes the response directly). This is the
     port of Click's `ActionLink` → method binding.
  3. Otherwise run `onInit()`/`onGet()`, build the FreeMarker model, render the page's `.ftl`.
  Dispatch is restricted to methods marked with a new **`@ConfiguratorAction`** annotation (action
  name defaults to the method name), so only intended handlers are web-invocable — a safe, explicit
  replacement for the public `ActionLink` fields. Adding a page later = one registry entry (Java
  only); the Click-fallback branch is deleted in the final increment.
- **`ConfiguratorContext`** — thin wrapper over `HttpServletRequest`/`HttpServletResponse`/session,
  the drop-in for Click `Context.getThreadLocalContext()`: `getRequest()`, `getResponse()`,
  `getSession()`, `get/set/removeSessionAttribute(...)`, `getWriter()`. Passed into the page (a field)
  instead of Click's thread-local lookup.
- **`SetupPage`** — new non-Click base class = the framework-agnostic half of `AjaxPage`, adapted to
  hold a `ConfiguratorContext` instead of calling `getContext()`. Carries: the `amConfigurator` i18n
  (`getLocalizedString`, bundle init from the request locale), the response writers
  (`writeValid/writeInvalid/writeJsonResponse/writeToResponse`), param coercion
  (`toString/toBoolean/toInt`), OpenDJ `getConnection` + LDAP error mapping, session helpers, the
  base AJAX handlers (`checkPasswords`, `validateInput`, `resetSessionAttributes`), and a
  `skipRender` flag replacing the `setPath(null)` idiom. Overridable lifecycle hooks:
  `onSecurityCheck()`/`onInit()`/`onGet()`/`onRender()`.
  - To keep the two engines behaviorally identical during coexistence, extract the **pure** helpers
    (no request/response: `getConnection`, `getMessage(ResultCode)`, `toBoolean`, `toInt`, the JSON
    string builders, password-length/host/port checks) into a shared static `SetupUtils` used by
    **both** `SetupPage` and the still-live Click `AjaxPage`. One source of truth for validation
    semantics that must stay byte-identical; the duplication shrinks to nothing as pages migrate.
- **FreeMarker `Configuration`** — a lazily-initialized singleton (add `org.freemarker:freemarker` to
  `openam-core/pom.xml`; version is managed in root pom, `freemarker.version = 2.3.31`, already used
  by `openam-oauth2`). Templates load from a non-web-accessible root
  `WEB-INF/templates/config/**` via a `WebappTemplateLoader` (so `.ftl` sources are never served
  raw). Object wrapper exposes public methods so `${page.getLocalizedString("k")}` works.

**FreeMarker model per render** (reproduces what Click injected; templates are ported to reference
the same names): `page` (the handler — for `getLocalizedString`), `context` (= `request
.getContextPath()`), `path` (= the request's own `.htm` servlet path, so `$context$path` still
posts-back-to-self), plus the handler's **public fields** copied in by reflection (Click parity),
plus any explicit `addModel(...)` entries (e.g. `startingTab` for the shell, `store`/`type` for
Step 3). Drop Click's `request`/`response`/`session`/`format`/`messages` keys unless a template
actually references them (the wizard templates use only `page`/`context`/`path` + page vars).

## The per-page increment recipe (repeat for each page)

Each increment is a self-contained, mergeable unit. Order within an increment:

1. **Port the template**: create `WEB-INF/templates/config/<...>/<page>.ftl` from the Velocity
   `<page>.htm`, using the token map below. Keep the URL, the inline `<script>` structure, the shared
   global JS symbols (`field`, `ie7fix`, `nextTab`, `store*Valid`, …), the `actionLink` param name,
   and each handler's exact response bytes.
2. **Port the page class**: change its base from `AjaxPage`/`ProtectedPage`/`TemplatedPage` to
   `SetupPage`; delete the Click `ActionLink` fields and annotate the handler methods with
   `@ConfiguratorAction`; replace `getContext()` with the injected `ConfiguratorContext`; replace
   `setPath(null)` with `skipRender()`. Handler bodies (validation, session writes, LDAP) copy 1:1.
3. **Register the page**: add one `path → PageClass` entry to `ConfiguratorServlet`'s migrated-page
   registry. **No `web.xml` edit** — the wrapper already owns `*.htm` (wired once in increment 1);
   registered paths render FreeMarker, everything else still forwards to Click.
4. **Delete the old Velocity `<page>.htm`** once the `.ftl` is verified live (no file is ever claimed
   by both engines).
5. **Verify** (see per-increment verification) and merge.

### Template token map (Velocity `.htm` → FreeMarker `.ftl`)

| Velocity | FreeMarker |
|---|---|
| `$context`, `$path`, `$startingTab`, `$type`, `$store` | `${context}`, `${path}`, `${startingTab}`, `${type}`, `${store}` |
| `$page.getLocalizedString("k")` | `${page.getLocalizedString('k')}` |
| `#if($x) … #else … #end` | `<#if x> … <#else> … </#if>` |
| `#foreach($x in $xs) … #end` | `<#list xs as x> … </#list>` |
| `value="#if($store.password)$store.password#{end}"` | `value="<#if store.password??>${store.password}</#if>"` |
| bare `$maybeMissing` (Velocity renders empty) | `${maybeMissing!""}` (FreeMarker is strict — use `!` default) |

Watch-outs: FreeMarker treats undefined vars as errors (Velocity silently renders nothing) — use
`!`/`??` for optional fields. Literal `${...}` inside embedded JS would be interpreted by FreeMarker;
none exists in these YUI-era templates, but escape with `<#noparse>`/`${r"..."}` if any appears.

## Increment order

- **Increment 0 — freebie cleanup (no behavior change):** delete dead `TemplatedForm.java`.
- **Increment 1 — pilot: Step 1 (+ the one-time wiring).** Build all shared infra above and migrate
  `Step1`/`step1.ftl`. **One-time `web.xml` change in both descriptors**
  (`openam-server-only/src/main/webapp/WEB-INF/web.xml` and
  `openam-federation/OpenFM/src/main/resources/xml/noconsole/web.xml`): re-point the `*.htm`
  `<servlet-mapping>` from `click-servlet` to `ConfiguratorServlet`, and leave `click-servlet`
  declared but **unmapped** (named-dispatch target). **Spike first:** prove a named forward renders an
  un-migrated Click page correctly (servletPath preserved, Click thread-locals set up) — the one real
  risk in the wrapper approach. Then register only `/config/wizard/step1.htm` as migrated; every other
  `.htm` still forwards to Click. Step 1 is the smallest page (its only live AJAX endpoint is the base
  `checkPasswords`, plain-text `"true"`/error). The still-Click shell (`wizard.htm`) loads `step1.htm`
  as a fragment via AJAX — proving the mixed-engine model through the real UI. **This increment locks
  the repeatable pattern; record anything non-obvious below.**
- **Increment 2 — Steps 2, 4, 5, 6.** Straightforward field/validation steps.
- **Increment 3 — Step 3 (+ Step 4 if not already) and `LDAPStoreWizardPage`.** The heaviest: many
  public-field-driven pre-fills, the `$store` object, `clearStore`, and the bespoke `validateHostName`
  JSON shape. Do this after the pattern is proven.
- **Increment 4 — Step 7** (summary).
- **Increment 5 — the wizard shell** (`Wizard.java` + `wizard.ftl`). Carries `createConfig` /
  `pushConfig` and `startingTab`; posts to `AMSetupServlet.processRequest()`. Fragments already
  migrated, so this just swaps the coordinating page.
- **Increment 6 — `Options.java` + `options.ftl`** (heavy `#if` control flow; retires
  `TemplatedPage.java`) **and `DefaultSummary.java` + `defaultSummary.ftl`.**
- **Increment 7 — `Upgrade.java` + `upgrade.ftl`** (`openam-upgrade` module).
- **Increment 8 — final removal** (below). At this point no source references Click.

Within each increment, one page = one commit where practical, so review stays page-sized.

## Per-increment verification

- **Automated** (openam-core TestNG/JUnit + a servlet mock):
  - Render the page's `.ftl` through the FreeMarker `Configuration` with a stub `page` model → assert
    localized title + expected inputs render.
  - Drive each `@ConfiguratorAction` handler with mock request/response → assert the **exact response
    bytes** (e.g. Step 1: mismatch → localized error text; `<8` chars → length error; valid → `"true"`
    + `SessionAttributeNames.CONFIG_VAR_ADMIN_PWD` set in session) and session side effects.
- **Manual smoke:** build the `openam-server-only` WAR, deploy unconfigured on Tomcat 10 / Jetty 11,
  walk the wizard through the browser. After each increment the migrated page must render and validate
  identically, and the **not-yet-migrated pages must still work on Click**.
- **Regression guard:** confirm a registered path renders FreeMarker while a sibling **un-registered**
  `.htm` still forwards to Click and renders identically to before (the named-dispatch fallback).

## Final removal (increment 8) — done

- [x] Delete `ConfiguratorServlet`'s Click-fallback branch (the named-dispatch `else`), so unknown
  `.htm` now 404s instead of forwarding. The `*.htm` → `ConfiguratorServlet` mapping **stays** (it is
  now the sole `.htm` handler). Remove the leftover **unmapped** `click-servlet` `<servlet>`
  declaration from **both** `openam-server-only/.../WEB-INF/web.xml` and
  `openam-federation/OpenFM/.../noconsole/web.xml`. (The migrated-page registry is the durable
  allow-list of configurator URLs — no `/config/*` blanket mapping, which would swallow the `.jsp`
  under `/config/auth/default` and `/config/federation`.)
- [x] Delete `openam-core/src/main/java/org/openidentityplatform/openam/click/` (57 files) and the
  `org.openidentityplatform.openam.velocity` fork.
- [x] `TemplatedPage.java` deleted in increment 6 (its only subclass, `Options`, migrated off it).
- [x] Delete `com/sun/identity/config/util/AjaxPage.java`, `ProtectedPage.java`
  (all logic now in `SetupPage`/`SetupUtils`); **`SetupUtils` was kept as a separate class**, not
  folded back inline — see `04-implementation-notes.md` for why.
- [x] Delete `WEB-INF/click.xml` and `WEB-INF/classes/click-page.properties`.
- [x] Delete the two Click error templates `click/error.htm`, `click/not-found.htm` (plus the empty
  `click/index.html` directory-listing blocker, removed along with the now-gone directory). **No**
  `<error-page>` replacement was added — see `04-implementation-notes.md`, neither `web.xml` ever had
  a servlet-spec `<error-page>` entry pointing at them, so there is nothing to preserve.
- [x] `openam-core/pom.xml` — remove `click-nodeps` + `click-extras` (needed only by the fork).
- [x] Root `pom.xml` — remove `<click.version>`; drop the `org.apache.click` /
  `org.openidentityplatform.openam.click` / `org.openidentityplatform.openam.velocity` packages from
  the javadoc/OSGi `excludePackageNames`.
- [x] Grep-sweep: zero `import ...click...` remain; no lingering Velocity `.htm` under
  `webapp/config/**`; all wizard URLs resolve to `ConfiguratorServlet`.

## Verification (end-to-end, after final removal)

On a fresh **unconfigured** WAR: run the full wizard Steps 1→7 (including custom config/user LDAP
stores hitting embedded + external OpenDJ, and site config), then **Create Configuration** succeeds
via `AMSetupServlet.processRequest()` and the console launches. Exercise the one-click default path
(`DefaultSummary`) and the **Upgrade** page against an older install. Confirm the "already configured"
gate (`onSecurityCheck` → `AMSetupServlet.isConfigured()`) blocks re-entry. Final gate: full reactor
build with Click removed and no `import ...click...` anywhere.

## Out of scope

XUI/admin console, auth/federation UIs, classic `openam-console` JSPs, and the Click-excluded
`/config/auth/default/*` + `/config/federation/*` static/JSP trees — none use Click.
