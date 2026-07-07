# Implementation notes: increments 0, 1 & 2

Findings from actually building the pilot (0, 1) and the Steps 2/4/5/6 batch (2), worth knowing
before increments 3+. Background in `02-recommendation.md` / `03-migration-plan.md`.

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

## Increment 2: Steps 2, 4, 5, 6

All four pages migrated in one batch (Java-only registry entries, no `web.xml` changes needed —
the `*.htm` wiring from increment 1 already covers them). `ConfiguratorServletTest`'s
"unregistered path forwards to Click" example was moved from `step2.htm` (now migrated) to
`step3.htm` (still Click, next up in increment 3) — update this again whenever step3 lands.

### Step6 has the same "template posts to the base handler, not its own ActionLink" quirk as Step1

`step6.htm`'s `validateAgentPasswords()` posts `?actionLink=checkPasswords&type=agent` — the
inherited `SetupPage.checkPasswords()` (same base handler Step1 uses), **not** `Step6`'s own
`checkAgentPassword()`/`validateAgent` ActionLink. Confirmed by reading the template JS, matching
the exact situation `01-click-inventory.md` already flagged for Step1. Handled the same way:
`checkAgentPassword()` is kept, `@ConfiguratorAction`-annotated, purely for URL-preservation
(it was a real, if unreferenced, Click `ActionLink` before) — not deleted, not treated as the
live path. `Step6Test` covers it directly; the actual template-driven behavior is exercised via
`Step1Test`/`ConfiguratorServletTest`'s coverage of the shared `checkPasswords`.

### Two pre-existing Velocity-silently-blank template variables in step4.htm — now need `!""` in FreeMarker

Cross-checked every `$var` in each `.htm` against every `addModel(...)` call in the matching
`.java` (the systematic check the plan's token-map section recommends but doesn't spell out a
method for — `grep -oE '\$[A-Za-z_][A-Za-z0-9_]*'` on the template vs. `grep -oE
'addModel\("[A-Za-z0-9_]+"'` on the page class, diffed by eye). Found two gaps in step4.htm/
Step4.java, both pre-existing Click/Velocity behavior preserved verbatim, not "fixed":

1. **`$selectADUserStoreSSL` is referenced by the template but never `addModel`-ed anywhere in
   `Step4.java`.** Always silently blank in the old Velocity render. Ported as
   `${selectADUserStoreSSL!""}`.
2. **The `LDAPv3ForODSEE` branch of `Step4.onInit()` has a genuine bug**: it sets
   `selectLDAPv3odsee` to `"checked=\"checked\""` and then, two lines later, overwrites the same
   key back to `""` (should have been `selectLDAPv3opends`). Net effect, unchanged by this port:
   picking the ODSEE store type leaves *both* the ODSEE and OpenDS radio buttons unchecked, and
   `selectLDAPv3opends` is left unset for that one request. Old Velocity rendered the unset var as
   silently empty; FreeMarker's strict undefined-variable check would turn it into a 500. Ported
   as `${selectLDAPv3opends!""}` in the template, with a comment at the bug site in `Step4.java`
   pointing here. The Java logic itself was copied 1:1 (bug included) per the byte-identical
   constraint — this is a pre-existing cosmetic bug, not something introduced by the migration,
   and not this increment's job to fix.

Worth doing this same `$var`-vs-`addModel` diff before templating any future page, especially
Step3 (increment 3), which has by far the most public-field/model-var surface area.

### `Step4.setAll()` was already dead code before this port — left un-annotated

No `ActionLink` field bound to it in the old `Step4.java`, and no template ever calls
`?actionLink=setAll`. Unlike Step1's/Step6's unreferenced-by-template-but-still-a-real-ActionLink
methods (kept annotated for URL parity), `setAll()` never had a `ClickActionLink` at all, so it
was never web-invokable in the first place. Kept as a plain, non-`@ConfiguratorAction` method
(matches its old unreachable state exactly) rather than either deleting it or making it newly
invokable.

### New `SetupPage` helpers added for Step2/Step4: `getAttribute`, `getHostName`, `getCookieDomain`, `getBaseDir`

These four were called out in the increment-1 notes as "add only when the page that needs them
migrates." Step2 needed `getBaseDir`/`getCookieDomain` (which itself calls `getHostName`); Step4
needed `getAttribute`/`getHostName`. Added directly to `SetupPage` as `protected` instance
methods, copied 1:1 from `AjaxPage`'s originals but reading through `ConfiguratorContext` instead
of Click's `Context`. Deliberately **not** extracted into `SetupUtils` and **not** back-ported
into `AjaxPage`: unlike `checkPasswords`'s validation semantics (genuine byte-identical-behavior
risk, hence the `SetupUtils` extraction in increment 1), these are one-line, low-risk
computations (`request.getServerName()`, a `System.getProperty("user.home")` fallback) with no
realistic drift risk between the two engines, so duplicating them is cheaper and more surgical
than touching `AjaxPage` (still used by unmigrated pages) for no behavioral benefit. Revisit this
call if a future increment's helper *does* carry real validation-style risk.

`ConfiguratorContext` also gained `getServletContext()` (delegates to
`request.getServletContext()`), needed by Step2's `deployURI` computation — mirrors Click's own
`Context.getServletContext()`.

### Handlers with no `writeToResponse` call still need an explicit `skipRender()`

The increment-1 note about `SetupPage.writeToResponse()` baking in `skipRender()` only covers
handlers that write a body. Three ported handlers never call `writeToResponse` at all —
`Step4.setUMEmbedded()`, `Step4.resetUMEmbedded()`, `Step5.clear()` — matching the old Click code,
where these fire-and-forget AJAX endpoints (`setUMEmbedded`/`resetUMEmbedded`/`clear` in the
templates have no response-handler callback) genuinely write nothing and only called
`setPath(null)`. These three keep an explicit `skipRender()` call (translated 1:1 from
`setPath(null)`), since there's no implicit trigger for them. Everywhere else, a trailing
`setPath(null)` immediately after a `writeToResponse`/`writeValid`/`writeInvalid` call was dropped
entirely (including one at the very *top* of `Step4.validateUMDomainName()`, confirmed redundant
by tracing that every exit path of that method writes a response) — matching the pattern already
set by the increment-1 Step1 port.

### `ServicesDefaultValues.isCookieDomainValid` is untestable from an `openam-core` unit test

`Step2.validateCookieDomain()`'s first line calls `ServicesDefaultValues.isCookieDomainValid(...)`
unconditionally. That class has an eagerly-initialized singleton
(`private static ServicesDefaultValues instance = new ServicesDefaultValues();`) whose constructor
loads `ResourceBundle.getBundle("serviceDefaultValues")` — and that resource
(`serviceDefaultValues.properties`) lives only under
`openam-server-only/src/main/resources/config/`, never on `openam-core`'s classpath (main or
test) in any build configuration, since `openam-core` is upstream of `openam-server-only` in the
reactor, not the reverse. First hit while writing `Step2Test`: any locale, any environment,
calling this method from an `openam-core` test throws `ExceptionInInitializerError` /
`NoClassDefFoundError`. Not fixable without duplicating a production resource file into
`openam-core`'s test resources purely for this, which is out of scope here. `Step2Test` covers
`validateConfigDir()` fully and documents this gap inline instead of testing
`validateCookieDomain()`; the manual smoke step (deploy the unconfigured WAR, exercise Step 2 in
the browser) is the only current coverage for that handler, same category as the still-open
"real FreeMarker render of `step1.ftl` isn't unit-tested" gap above.
