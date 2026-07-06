# Implementation notes: increments 0 & 1

Findings from actually building the pilot, worth knowing before increments 2+. Background in
`02-recommendation.md` / `03-migration-plan.md`.

## FreeMarker's `WebappTemplateLoader` cannot be used (javax vs jakarta)

`03-migration-plan.md` names `freemarker.cache.WebappTemplateLoader` as the loader for
`WEB-INF/templates/config/**`. It doesn't work: **even in the newest available FreeMarker jar
(2.3.34)** that class's constructor is hard-wired to `javax.servlet.ServletContext`, never
Jakarta. FreeMarker has not been updated for Jakarta EE 9. This is the *exact* problem the
vendored Click fork already hit with Velocity Tools' `WebappResourceLoader` — that's why
`org.openidentityplatform.openam.velocity` exists as a from-scratch Jakarta port of it.

Fix used here: a small hand-written `org.openidentityplatform.openam.config.servlet.ServletContextTemplateLoader`
(implements `freemarker.cache.TemplateLoader` directly against `jakarta.servlet.ServletContext
.getResource`/`getResourceAsStream`). ~30 lines, no reflection tricks needed since the
`TemplateLoader` interface itself has no servlet-API types in its signature — only the loader's
*constructor* needs a `ServletContext`, and jakarta's has everything required. Templates still
live at `WEB-INF/templates/config/**`, non-web-accessible, exactly as planned; only the loader
implementation differs from the plan text.

## `onInit()` must always run before an `?actionLink=` dispatch

`03-migration-plan.md`'s per-request steps read as if `onInit()`/`onGet()` only run in the
non-action branch (step 3), with step 2 (actionLink dispatch) implied separate. Re-checking
`ClickServlet.processPageEvents`: `onInit()` always runs first; the `actionLink` convention isn't
Click's newer `pageAction` request-scoped dispatch, it's the *classic* `ActionLink` control, whose
bound method fires during the control-processing phase that follows `onInit()` unconditionally.

This matters because `AjaxPage.onInit()` → `initializeResourceBundle()` sets the response content
type (`text/html; charset=UTF-8`), and the **happy path** of `checkPasswords()` /
`checkAdminPassword()` / `checkAgentPassword()` never calls `getLocalizedString(...)` (so never
lazily triggers that init as a side effect) — it only writes `"true"` / `"OK"` directly. Skipping
`onInit()` on the actionLink branch would silently drop the content-type header on the success
path only, which is exactly the kind of divergence that's easy to miss in testing and only shows
up on the happy path in a real browser.

`ConfiguratorServlet` therefore always calls `page.onInit()` right after `onSecurityCheck()`
passes, for *every* request to a registered page, action or not.

## `SetupPage.writeToResponse()` calls `skipRender()` itself

In the old code every single call site of `AjaxPage.writeToResponse(...)`, across
`AjaxPage`/`Step1`–`Step5`/`Wizard`/`DefaultSummary`/`Upgrade`, is immediately followed by
`setPath(null)` (or the method just ends). No exception found. `SetupPage.writeToResponse()` bakes
that in — callers no longer need to remember the follow-up `skipRender()` call. This was checked
carefully because it's the kind of place a byte-identical-behavior constraint could quietly break;
if a future increment finds a page whose `writeToResponse` call *is* meant to be followed by
template rendering, that page needs a different method, not a change to this one.

## `SetupUtils` — what actually moved out of `AjaxPage`

Only what `checkPasswords`/`checkAdminPassword`/`checkAgentPassword` needed: `getConnection`,
`getMessage(ResultCode)`, `parseBoolean`/`parseInt` (renamed from `toBoolean`/`toInt` since they're
now pure — no request access), the `{"valid":..,"body":..}` JSON builder, and `MIN_PASSWORD_SIZE`.
`AjaxPage` now delegates to these instead of duplicating them — one source of truth while both
engines run side by side, per the plan.

Left alone: `AjaxPage.OLD_RESPONSE_TEMPLATE` (`{"isValid":..,"errorMessage":..}`) — it was already
unused before this change (confirmed via repo-wide grep), so it wasn't moved or touched, just left
where it was. Also left alone: `getHostName`/`getBaseDir`/`getAvailablePort`/`getCookieDomain` —
used by `Wizard`/other unmigrated pages, not by Step1, and not in the plan's explicit list of what
`SetupPage` carries. Add these to `SetupUtils`/`SetupPage` only when the page that needs them
migrates (mirrors the "no speculative code" call in the plan — don't guess their pure-vs-page-bound
split ahead of the increment that actually exercises it, e.g. Step3's host/port validation).

## Step1's unreferenced `ActionLink`s were kept, annotated

`01-click-inventory.md` flags `checkAdminPassword`/`checkAgentPassword` as unreferenced by any
template (the live handler is the inherited `checkPasswords`). Decision (confirmed with the repo
owner): keep them, `@ConfiguratorAction`-annotated, so the URLs stay invocable exactly as before —
matches the migration's URL-preservation constraint even for endpoints nothing currently calls.

## Click's public-field template-model exposure, reproduced generically

`ConfiguratorServlet.render()` copies every non-static public field of the page instance into the
FreeMarker model by reflection, matching Click's own auto-exposure (not just `addModel(...)`
entries). One side effect worth knowing: `SetupPage.responseString` (public, default `"true"`) is
therefore in every migrated page's model as `${responseString}`, same as it silently was in the old
Click/Velocity model. No template currently references it either way — noted here so it isn't
mistaken for a leak introduced by the port when Step3 (which relies heavily on public-field
pre-fills) migrates.

## Template path convention

Registry key is the servlet path (e.g. `/config/wizard/step1.htm`); the `.ftl` is looked up at
`WEB-INF/templates/config/<same path with the "/config/" prefix stripped, ".htm" -> ".ftl">`, i.e.
`WEB-INF/templates/config/wizard/step1.ftl`. Every page registered in `ConfiguratorServlet.PAGES`
must have its `.ftl` at that derived path — there's no per-entry override, so keep new pages
under `/config/...htm` (all of them are, per the inventory).

## `noconsole/web.xml` is not a standalone XML document

`openam-federation/OpenFM/src/main/resources/xml/noconsole/web.xml` (and its `console`/`wss`
siblings) has no `<?xml?>` declaration and no `<web-app>` root — it's a bare fragment (just
`<servlet>`/`<servlet-mapping>` elements) consumed by the legacy
`com.sun.identity.tools.deployablewar.WarCreator` tool, not parsed standalone. `xmllint --noout`
will (correctly) reject it; that's not a regression, the file was never independently well-formed.
Don't "fix" it by adding a root element without checking how `WarCreator` (or whatever assembles
the final `web.xml`) actually consumes it.

## Test coverage gap: real FreeMarker render of `step1.ftl` isn't unit-tested

`step1.ftl` lives in `openam-server-only` (a WAR module with no test tree), while
`ConfiguratorServlet`/`SetupPage` live in `openam-core` — so an `openam-core` unit test can't load
the real production template across that module boundary. What's covered instead:

- `ServletContextTemplateLoaderTest` (openam-core): proves the loader + FreeMarker `Configuration`
  + model-building mechanism works, against a local test-fixture `.ftl`, not the real one.
- `ConfiguratorServletTest` (openam-core): proves routing (registered-page actionLink dispatch,
  unknown-action 404, unregistered-path named-forward to `click-servlet`) against the real
  registry — but neither path in these tests reaches the `render()`/FreeMarker branch.

Actually rendering `step1.ftl` end-to-end is only exercised by the plan's manual smoke step
(deploy the unconfigured WAR, load the wizard, watch Step 1 render) — not run this session per
the chosen verification depth. Worth doing once, and worth re-checking after any change to the
token-map port (`$page.getLocalizedString` / `$context$path` conversions) since a FreeMarker
strict-undefined-variable error would only surface there, not in these unit tests.

## `onSecurityCheck()`'s "already configured" branch isn't unit-tested

`Step1.onSecurityCheck()` calls the static `AMSetupServlet.isConfigured()`, backed by a plain
static field (`isConfiguredFlag`, defaults `false`). Tests exercise the natural default
(unconfigured → proceed); forcing the "already configured → block" branch would need PowerMock
(already a test dependency in `openam-core`, used elsewhere in the module) or a testability seam
on `AMSetupServlet`. Skipped for this pilot as disproportionate for one `if`; revisit if increment
2+ wants it, or if `AMSetupServlet` grows a non-static way to ask this question.

## Naming decisions for future increments to match

- New servlet's `<servlet-name>` in both `web.xml` descriptors: `configurator-servlet`.
- `ConfiguratorServlet.PAGES` is a `private static final Map<String, Class<? extends SetupPage>>`
  populated in a static initializer — migrating a page is one `PAGES.put(...)` line, nothing else
  in the servlet changes.
