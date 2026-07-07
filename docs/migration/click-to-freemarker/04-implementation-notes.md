# Implementation notes: increments 0-6

Findings from actually building the pilot (0, 1), the Steps 2/4/5/6 batch (2), Step3 +
LDAPStoreWizardPage (3), Step7 (4), the wizard shell (5), and Options + DefaultSummary (6), worth
knowing before increment 7+. Background in `02-recommendation.md` / `03-migration-plan.md`.

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

### `Step2Test.validateConfigDirRejectsNoWritePermission` needed a Windows guard

CI on a Windows agent failed this test: `File.setWritable(false)` on a directory isn't enforced
there (Windows doesn't honor the read-only attribute for writes *into* a directory, only for the
file itself), so `Step2.hasWritePermission()`'s `File.canWrite()` check kept returning `true` and
`validateConfigDir()` took the happy path instead of the rejection branch. Same underlying
category as this doc's other "not reliably reproducible across environments" gaps, just discovered
post-merge via CI rather than while writing the test. Fixed by checking whether `setWritable(false)`
actually took effect (`canWrite()` still `true` afterwards) and skipping via `SkipException` with an
explanatory message when it didn't — also covers a privileged/root POSIX test runner, which would
hit the same symptom for the same reason (permission checks bypassed).

## Increment 3: Step3 + LDAPStoreWizardPage

Step3 is the heaviest page so far (many public-field/session pre-fills, the `LDAPStore` object,
`clearStore`, the bespoke `validateHostName` JSON shape). It's also the first page with a
*migrated intermediate base class*: `LDAPStoreWizardPage` sat between `Step3` and the old
`ProtectedPage`, so it had to be ported too, following the same recipe as `SetupPage` itself
(`onSecurityCheck()` moved onto `LDAPStoreWizardPage`, not duplicated onto `Step3` — Step3 never
overrode it in the old code either, it just inherited the check).

### `setConfigType()`/`setReplication()`: old Click `ActionLink`s that returned `true` with no render-skip

Unlike every handler ported in increments 1-2 (which all either call `writeToResponse(...)` — which
now bakes in `skipRender()` — or explicitly call `setPath(null)`/`skipRender()`), Step3's
`setConfigType()` and `setReplication()` write **nothing** to the response and return `true` with
no `setPath(null)` at all. In the old Click framework an `ActionLink` handler returning `true` (and
never calling `setPath(null)`) tells Click to *continue* normal request processing — i.e. run
`onGet()` and render the full page template into the same AJAX response.
`ConfiguratorServlet.invokeAction()` has no equivalent: it invokes the `@ConfiguratorAction` method
and returns unconditionally, **ignoring the method's boolean return value entirely**. So in the new
code these two handlers now produce an empty response instead of a full-page render.

Checked whether this is an observable behavior change: both callers
(`enableRemote`/`disableRemote`/`enableExisting`/`disableExisting` in `step3.ftl`) invoke them via
`AjaxUtils.call(url)` with **no callback**, i.e. fire-and-forget — nothing on the client ever reads
the response body either way. And `Step3.onInit()` has no session side effects (only session reads
+ `addModel` calls), so re-running it (as the old full-page render would have done) wouldn't have
produced any additional session state. Net effect: the missing render is bytes nobody reads, not a
behavior difference. Ported as plain `skipRender()` + `@ConfiguratorAction`, with a comment at each
call site pointing back here — **if a future page has this same "return true, no write" shape but
its `onInit()` *does* have session side effects, that page cannot be ported this simply** and
`ConfiguratorServlet` would need to grow support for continuing to render on a truthy return.

### `getAvailablePort(int)` added to `SetupPage`

Called out in earlier increments as "add only when the page that needs it migrates" (mirrors the
`getBaseDir`/`getCookieDomain`/`getHostName`/`getAttribute` additions in increment 2). Step3 is the
first migrated page that needs it (`configStorePort`/`configStoreAdminPort`/`configStoreJmxPort`/
`localRepPort`/`existingPort` defaults, plus the `validateSMHost` port fallback). Copied 1:1 from
`AjaxPage.getAvailablePort(int)`: `Integer.toString(AMSetupUtils.getFirstUnusedPort(getHostName(),
portNumber, 1000))`.

### Two more pre-existing Velocity-silently-blank template variables, this time in step3.htm

Same `$var`-vs-`addModel` diff recommended by the plan and already used in increment 2's Step4
port. `step3.htm` references `$existingStoreHost` and `$existingStorePort` in the "existing LDAP"
section's initial `value="..."` attributes, but **neither key is ever `addModel`-ed by
`Step3.onInit()` or `LDAPStoreWizardPage.onInit()`**. They're populated only client-side, by JS
reading the `validateHostName` JSON response (`resp.existingStoreHost`/`resp.existingStorePort`)
and setting the `<input>` `.value` directly — the server-rendered initial value was always silently
blank under Velocity. Ported as `${existingStoreHost!""}` / `${existingStorePort!""}`. (By contrast,
`configStorePassword` *is* `addModel`-ed but never referenced by the template at all — the
template only reads `$store.password` for that field. Harmless, unused-model-entry case, left
as-is, not a gap that needs a `!""` default.)

### The `LDAPStore`/`store`/`clearStore`/`LDAP_STORE_SESSION_KEY` machinery looks mostly vestigial

`LDAPStoreWizardPage.save(LDAPStore)` is never called anywhere in the repo (confirmed by grep before
porting) — the `store` field is populated once per request (`getConfig()`/`ensureConfig()`) and
handed to the template for the one `$store.password` prefill, but nothing ever writes a populated
`LDAPStore` back into the `customConfigStore` session key, so that prefill is always blank in
practice. `Step3.LDAP_STORE_SESSION_KEY = "wizardCustomConfigStore"` is also dead — the session key
actually used is `LDAPStoreWizardPage`'s own default, `"customConfigStore"` (a different string);
the constant is never read anywhere. All of this is pre-existing (predates this migration) and
ported byte-for-byte, including the naming mismatch — not a bug introduced here, and not this
increment's job to clean up. Worth knowing if a future increment is tempted to "simplify" this
class: the apparent dead weight is original behavior, not migration residue.

### Increment 3 test coverage gaps (same category as prior increments)

- `validateHostName()`/`validateSMHost()`: require a live remote-server HTTP round trip and a live
  LDAP connection respectively — same disproportionate-effort call as Step4Test's
  `validateUMHost`/`validateUMDomainName` skip.
- `validateLocalPort()`/`validateLocalAdminPort()`/`validateLocalJmxPort()`: the "port not in use"
  happy path calls `AMSetupUtils.isPortInUse()`, which binds a real `ServerSocket` — environment-
  dependent. Only the parameter-validation branches and the external-data-store bypass (which
  short-circuits before ever calling `isPortInUse()`) are unit-tested; the local/embedded
  port-in-use branch is manual-smoke-only.
- `validateConfigStoreHost()`'s `UnknownHostException` branch isn't unit-tested — DNS behavior for a
  deliberately-invalid hostname isn't reliably reproducible across environments/sandboxes. The
  resolvable-host branch is covered using `localhost` (resolves without real network).

`ConfiguratorServletTest`'s "unregistered path forwards to Click" example was moved from
`step3.htm` (now migrated) to `step7.htm` (still Click, next up in increment 4) — update this again
whenever step7 lands.

## Increment 4: Step7 (summary)

The simplest page so far: `Step7.onInit()` only reads session attributes and `addModel`s a
display-only summary, and — confirmed by grepping `step7.htm` for `actionLink` before porting —
**the template never posts back to the page at all**. No `ActionLink`s, no
`@ConfiguratorAction` methods, nothing to dispatch. `ConfiguratorServlet`'s registry entry alone
(`Step7.class`) is the entire wiring; the page class itself only overrides `onSecurityCheck()`
(copied 1:1 from the other steps' `ProtectedPage` port) and `onInit()`.

All three `SetupPage` helpers Step7 needs (`getAttribute`, `getHostName` transitively via
`getAvailablePort`, `getAvailablePort` itself) were already added in increments 2-3 — no new
`SetupPage` surface required for this increment.

### `$embedded` in step7.htm was already dead before this port — a model-key/template-var mismatch, not a `!""` gap

Different from every other "silently blank" finding in increments 2-3 (which were template vars
never `addModel`-ed at all): here **both sides exist, under different names**. `Step7.java` only
ever calls `add("isEmbedded", "1")`; `step7.htm`/`step7.ftl` only ever reads `$embedded`/
`${embedded}`. Confirmed by grepping the whole `com.sun.identity.config` tree (and Click's own
`Page`/`AjaxPage`) for a public field or model key literally named `embedded` — there isn't one
reachable by Step7. Net effect, **unchanged by this port**: the two embedded-only summary rows
(local admin port, local JMX port) never render on the summary page, regardless of whether the
config store is actually embedded. Ported as `<#if embedded??>` (an existence check on a key that
is never populated, so always false) rather than inventing a real boolean — matches the "byte-
identical, bugs included" rule already applied to Step4's ODSEE checkbox bug and Step3's dead
`LDAPStore` machinery. `Step7Test.onInitSummarizesEmbeddedConfigStoreWithDefaultUserStore` asserts
`model.doesNotContainKey("embedded")` specifically so a future cleanup doesn't "fix" this by
accident without it being a deliberate, reviewed decision.

### `getAvailablePort()` runs unconditionally on every `onInit()`, even when the session already has a value

`getAttribute("configStorePort", getAvailablePort(50389))` (and the `configStoreAdminPort`/
`configStoreJmxPort` siblings) evaluates the `getAvailablePort(...)` argument **eagerly**, in
plain Java, before `getAttribute` ever checks whether the session attribute exists — this was true
in the original Click `Step7.java` too, copied verbatim here. So every single render of the
summary page binds and releases up to three real local sockets via
`AMSetupUtils.getFirstUnusedPort`/`isPortInUse`, whether or not the result is actually used. Not a
regression (identical to pre-migration behavior) and not this increment's job to optimize, but
worth knowing: unlike Step3Test (which carefully picked external-data-store scenarios specifically
to avoid ever calling `isPortInUse`), `Step7Test` cannot avoid this — every test stubs
`request.getServerName()` to `"localhost"` purely to keep the resulting socket bind deterministic.

### `EXT_DATA_STORE` NPE if Step7 is reached before Step4 ever ran for the session

`Step7.onInit()`'s user-store branch does `tmp = ctx.getSessionAttribute(EXT_DATA_STORE); if
(tmp.equals("true"))` with no null guard — identical to the original Click page. `EXT_DATA_STORE`
is written only by `Step4.onInit()`. Checked whether this is reachable through the real UI: the
still-Click wizard shell (`wizard.htm`, increment 5) lazy-loads each `stepN.htm` fragment on
demand via `AjaxUtils.load` only when the user navigates to that tab (`showTab`/`nextTab`), so
normal next-button navigation always runs Step4's `onInit()` at least once before Step7 can be
reached. Not provably unreachable (a deep link or unusual navigation could in principle hit tab 7
without tab 4), but not a behavior change either way — pre-existing in the Click version.
Locked in with a dedicated test (`Step7Test.onInitThrowsIfExtDataStoreNeverSet`) asserting the
`NullPointerException`, specifically so this doesn't get silently "fixed" as a side effect of an
unrelated future change to this method.

### Real FreeMarker render of `step7.ftl` — checked out-of-band, not as a permanent test

Same module-boundary gap as `step1.ftl` (the `.ftl` lives in `openam-server-only`, a WAR module
with no test tree; `ConfiguratorServlet` lives in `openam-core`). Rather than leave this fully
unverified before merging, the template was rendered directly with a throwaway FreeMarker 2.3.31
harness (real `Configuration` + real `step7.ftl` from disk, stub `page.getLocalizedString`)
against three scenarios — minimal/embedded, fully-populated external config+user store+load
balancer, and the default-user-store branch — and confirmed no `InvalidReferenceException` in any
of them. This wasn't kept as a checked-in test (would need a cross-module test source set to load
the real production template, which is out of scope here, same call already made for `step1.ftl`);
re-run manually if `step7.ftl`'s var references change.

`ConfiguratorServletTest`'s "unregistered path forwards to Click" example was moved from
`step7.htm` (now migrated) to `wizard.htm` (still Click, next up in increment 5) — update this
again whenever the wizard shell lands.

## Increment 5: the wizard shell (`Wizard.java` + `wizard.ftl`)

The first page constructed from a class whose old Click version relied on **eager instance-field
initializers** (`private String hostName = getHostName();`, and three `getAvailablePort`-derived
`public String defaultPort/defaultAdminPort/defaultJmxPort` fields built from it) that themselves
call `getContext()`. This surfaced a real gap in the wrapper architecture, not just a per-page
porting detail.

### `getContext()` is not safe in a field initializer or constructor under `ConfiguratorServlet`

Old Click's `Page.getContext()` reads `Context.getThreadLocalContext()`, a `ThreadLocal` that
`ClickServlet` binds **before** it constructs the page instance (`newPageInstance(...)` runs after
the thread-local is already set) — so a Click page's field initializers, and even its constructor
body, can safely call `getContext()`. `ConfiguratorServlet.service()` does the opposite:
it calls `pageClass.getDeclaredConstructor().newInstance()` **first**, then
`page.setContext(new ConfiguratorContext(...))` afterwards. Any migrated page whose fields or
constructor call `getContext()`/`getHostName()`/etc. eagerly will NPE, because `context` is still
null at that point.

None of Steps 1-7 hit this (none has a field initializer that touches the context). Fix applied
here: moved the equivalent computation into `Wizard.onInit()`, which `ConfiguratorServlet` always
calls (right after `onSecurityCheck()` passes) before either render or `?actionLink=` dispatch —
the same "runs unconditionally on every request" guarantee already established in increment 1's
`onInit()` note. This preserves the old behavior's actual observable effect: old Click reconstructed
a fresh (non-stateful — `Wizard` never calls `setStateful(true)`) page per request, so
`hostName`/`defaultPort`/`defaultAdminPort`/`defaultJmxPort` were recomputed, and fresh sockets
bound/released via `AMSetupUtils.getFirstUnusedPort`, on **every single request to `wizard.htm`**
regardless of whether it was a plain GET or an `?actionLink=` call — not just on first load. Moving
the computation to `onInit()` reproduces exactly that, just later in the call stack than field-init
time. **If a future page needs eager state that depends on the request/session, put it in
`onInit()`, never in a field initializer or the constructor.**

### `SetupPage.configLocale` had to become `protected`, not stay `private`

`Wizard.createConfig()`'s last step (`request.addParameter("locale", configLocale.toString())`)
reads the old `AjaxPage.configLocale` field directly — it was `protected` there, deliberately, for
subclass access. `SetupPage`'s port of the same field was `private`. `DefaultSummary.java`
(increment 6, not yet migrated) has the identical `configLocale.toString()` line, so this isn't
a Wizard-only fix: `SetupPage.configLocale` is now `protected`, matching `AjaxPage`'s original
visibility exactly. Worth checking for other `protected`-in-`AjaxPage`-but-`private`-in-`SetupPage`
fields before increment 6 lands, since Options/DefaultSummary are the last consumers of the old
base class's subclass-visible state.

### `testNewInstanceUrl`/`pushConfig`: `ActionLink`s with no backing method at all — not the same as Step1/Step6's kept-but-unreferenced ones

Old `Wizard.java` declared `testUrlLink`/`pushConfigLink` `ActionLink`s bound by name to
`testNewInstanceUrl()`/`pushConfig()` — but **neither method exists anywhere in `Wizard.java`**,
and `git log --follow -p` on the file shows they never did in this repo's history. This is different
from Step1's `checkAdminPassword`/`checkAgentPassword` or Step6's `checkAgentPassword` (increments 1
and 2): those methods are real and correct, just not called by any template, so they were kept and
`@ConfiguratorAction`-annotated for URL parity. Here there is no method to annotate — Click's
`ActionLink.bindRequestValue()`/`dispatchActionEvent()` would reflectively look up `pushConfig`/
`testNewInstanceUrl` on `Wizard` via `ClickUtils.invokeListener`, fail with
`NoSuchMethodException`, and rethrow as a `RuntimeException` ("Exception occurred invoking public
method"). Confirmed this is also unreachable through the actual UI: `wizard.htm` builds a
`pushConfigDialog` (`YAHOO.widget.SimpleDialog`) in `wizardInit()` but **never calls `.show()` on
it anywhere** — grepped the whole `webapp/` tree — so a user can never trigger
`pushNewInstanceConfig()`, the one JS function that posts `?actionLink=pushConfig`.
`testNewInstanceUrl` has no caller in the JS at all, commented or otherwise. Net effect: both
`ActionLink` fields were dropped with no replacement (nothing to port), leaving `createConfig` as
the sole `@ConfiguratorAction`. Direct-URL invocation of `?actionLink=pushConfig` now 404s instead
of 500ing — a different failure mode, but both are equally unreachable through the product, so this
is not a behavior change a real user or the IT suite can observe.

### Two more pre-existing dead fields, left in place

`cookieDomain` (a `private String` field, assigned `null` and never read again — shadowed by an
unrelated local variable of the same name inside `createConfig()`) and `dataStore` (a `private
String` field initialized to `SetupConstants.SMS_EMBED_DATASTORE` and never referenced again
anywhere in the class) are both fully dead, pre-existing this migration. Kept byte-for-byte, same
"port the bugs/dead code, don't clean up" rule already applied to Step3's `LDAPStore` machinery and
Step4/Step7's findings.

### `startingTab`'s "user cookie" comment was already stale before this port

`wizard.htm`'s JS comment (`// determined by Click Wizard.java control based on user cookie`)
doesn't match the actual code: `Wizard.java` has no cookie-reading logic anywhere, `startingTab` is
a plain `public int startingTab = 1;` field never reassigned except by client-side JS
(`startNewConfig()` sets the JS-local `startingTab = 2`, which never round-trips back to the
server). Only the now-actively-wrong framework reference ("Click Wizard.java") was edited out of
the ported comment in `wizard.ftl`; the "user cookie" claim was left as-is since it predates this
migration and isn't this increment's job to fix — noted here so it isn't mistaken for a port error.

### `wizard.htm` has zero Velocity control-flow directives

Grepped for `#if`/`#foreach`/`#set`/`#else`/`#end` — none. Only four distinct `$`-prefixed forms
appear (`$context`, `$path` is absent as a bare token — it's always paired with `$context` as
`$context$path` — `$startingTab`, `$page.getLocalizedString(...)`), all straight token-map
substitutions. The simplest template port of the six done so far.

### Test coverage: `createConfig()` itself is not unit-tested, but a real Selenium IT test already covers it end-to-end

`createConfig()` is the wizard's "execute" operation, but its only substantive logic is copying
session state into request parameters ahead of one direct call to the static
`AMSetupServlet.processRequest(request, response)`, which performs real configuration writes
(embedded/external OpenDJ, on-disk config) — out of proportion for an `openam-core` unit test, same
category of call already made for `AMSetupServlet.isConfigured()` in increment 1. Unlike earlier
"manual-smoke-only" gaps, this one already has automated e2e coverage:
`openam-server/.../test/integration/IT_SetupWithOpenDJ.java` is a Cargo-container Selenium test that
walks all seven wizard tabs via `nextTabButton` and clicks `writeConfigButton`, which is exactly
`Wizard.createConfig()`'s only trigger path. Worth running that IT suite (not attempted this session
— it needs a full container deploy, out of scope for the `openam-core`-only build used here) as the
real verification for this method, rather than adding static-mocking (e.g. PowerMock, already a test
dependency per increment 1's note) to fake it in a unit test — consistent with not introducing that
technique anywhere else in this migration so far.

### `wizard.ftl` real-render check

Same module-boundary gap as `step1.ftl`/`step7.ftl` (`.ftl` lives in `openam-server-only`, no test
tree; `ConfiguratorServlet` lives in `openam-core`). Rendered directly with a throwaway FreeMarker
2.3.31 harness (real `Configuration` + real `wizard.ftl` from disk, stub `page.getLocalizedString`)
with `context`/`path`/`startingTab` stubs; confirmed no `InvalidReferenceException` and spot-checked
that `${context}${path}?actionLink=createConfig` etc. resolve correctly. Not kept as a checked-in
test, same call as the prior two pages; re-run manually if `wizard.ftl`'s var references change.

`ConfiguratorServletTest`'s "unregistered path forwards to Click" example was moved from
`wizard.htm` (now migrated) to `options.htm` (still Click, next up in increment 6) — update this
again whenever `Options.java`/`options.ftl` lands.

## Increment 6: Options + DefaultSummary

The first increment to migrate a **full top-level HTML page**, not a fragment. Every page in
increments 1-5 (steps 1-7, the wizard shell) is loaded as an AJAX fragment into some container —
`options.htm` is that container. It's requested directly by the browser and, uniquely among all
12 old templates, is the *only* page whose Click page class overrides `getTemplate()` (via
`TemplatedPage`) to point at a **second, wrapping** template (`assets/templates/main.html`), which
Velocity's `#parse($path)` then used to splice `options.htm`'s own content into the middle of a
shared HTML/`<head>`/copyright-footer shell. `defaultSummary.htm` (the other page in this
increment) has no such wrapping — like every fragment page before it, its class extends
`ProtectedPage`, not `TemplatedPage`, so it never went through `main.html` at all.

### `ConfiguratorServlet` has no decorator/layout support, so `main.html` had to be inlined into `options.ftl`

Click's two-level template resolution (page template, wrapped by a layout template that
`#parse`s it back in) has no equivalent in `ConfiguratorServlet.render()`, which loads and
processes exactly one named template per URL. Building generic decorator support for this one
remaining full-page use case would be speculative infrastructure for a single caller. Instead,
`options.ftl` is `main.html` and the old `options.htm` **merged into one file**: the full
`<!DOCTYPE>`/`<head>`/table-layout shell from `main.html`, with `options.htm`'s content (its own
`<link>`/`<script>`/`<div id="options-container">`, translated with the usual token map) inserted
literally at the point where `#parse($path)` used to sit. Note that `options.htm`'s own
`<link>`/`<script>` tags end up inside the HTML `<body>`, not `<head>`, exactly as they did before
(Velocity's `#parse` is a literal text splice, and browsers tolerate this) — not "fixed" as part of
this port. If a future page ever needs the same `main.html` wrapping, extract this into a real
FreeMarker `<#include>`/macro at that point; doing it for a single page here would be premature.

### `main.html`'s `$imports` is always empty for `Options` — dropped, not defaulted

`$imports` is Click's own `PageImports` mechanism (auto-collects `JsImport`/`CssImport` from a
page's `getHeadElements()` and its registered `Control`s). Confirmed via the fork's `Page`/
`PageImports` source that `getHeadElements()` defaults to an empty list unless a page overrides
it, and neither `Options` nor any of its old ancestors (`TemplatedPage`/`AjaxPage`) does, nor do
raw `ActionLink` fields contribute head elements. So `$imports` always rendered empty for this
page; the line is dropped entirely rather than reproduced as `${imports!""}` on an empty line,
matching the plan's own instruction to drop unused shared-model keys.

### `Options`'s three `ActionLink`s are dead — same shape as `Wizard`'s `testNewInstanceUrl`/`pushConfig`

`createConfigLink`/`testUrlLink`/`pushConfigLink` were bound by name to `upgrade()`/`coexist()`/
`olderUpgrade()` — none of which exist anywhere in `Options.java`, confirmed by grep and by `git
log --follow -p`. Exactly the increment-5 `Wizard` situation: Click would have reflectively
thrown `NoSuchMethodException` on any request that actually tried to invoke one, and the template
never posts to any of them — the matching `id="upgradeLink"`/`id="DemoConfiguration"`/
`id="CreateNewConf"` HTML elements are plain anchors driven by static JS `addEventListener` calls,
unrelated to Click's `ActionLink` control mechanism despite the name coincidence. Dropped with no
replacement, same as `Wizard`'s pair.

### `TemplatedPage`'s title/`currentYear`/status-message machinery was entirely dead for `Options` — dropped along with the class

`Options` was `TemplatedPage`'s only subclass (confirmed by grep), so this increment retires
`TemplatedPage.java` per the plan. Before deleting it, checked whether its `onInit()` machinery
(`addModel("title", ...)`, `currentYear`, `statusMessages`/`addStatusMessageCode`/
`getStatusMessageCodes()`) does anything observable: grepped every `$`-token in `options.htm` and
every caller of `addStatusMessageCode`/`getStatusMessageCodes` repo-wide. Neither `$title` nor
`$currentYear` nor `$statusMessages` is ever referenced by `options.htm`, and nothing outside
`TemplatedPage` itself calls the status-message methods — fully dead, not merely unused-but-safe
like Step3's `configStorePassword`. Unlike that precedent (where the addModel call was left in
place because it was part of a method being ported verbatim), this is the framework class itself
being retired, so the dead mechanism was not carried into `Options` at all: no `title/currentYear/
statusMessages` model keys, no `getTitle()` (which becomes dead the moment nothing adds "title" to
the model, so it was removed rather than left as an orphan). `getTemplate()` was not ported either
— it was Click's own template-resolution hook, made obsolete by `ConfiguratorServlet`'s
URL-to-template mapping.

### A second, unrelated dead field: `Options`'s own private `configLocale`

Separately from the `AjaxPage`/`SetupPage.configLocale` field used by `Wizard`/`DefaultSummary`,
the old `Options.java` declared its own `private java.util.Locale configLocale = null;`, which
**shadows** the inherited one and is never assigned or read anywhere else in the class — inert by
construction, not a bug that changes behavior, just leftover cruft. Not carried into the port.

### Two more pre-existing silently-blank Velocity variables, both in `options.htm`

Same `$var`-vs-`addModel` diff used for every page since Step4. `options.htm` reads `$currentVersion`
inside `#if ($upgrade || $upgradeCompleted)`, but `doInit()` only calls `addModel("currentVersion",
...)` when `upgrade` is `true` — a narrower condition. Since `upgrade` and `upgradeCompleted` are
independent booleans (`upgradeCompleted` is a separate persisted flag from
`AMSetupServlet.isUpgradeCompleted()`), the state `upgrade=false, upgradeCompleted=true` is
reachable (an install that already finished upgrading, so the version check no longer reports
"newer") and would have silently rendered `$currentVersion` blank under Velocity. Ported as
`${currentVersion!""}`. `$odsdir` has no such gap — it's read only inside `#if ($isOpenDS1x)`, and
`addModel("odsdir", ...)` is guarded by the identical condition — but it was still given a
`${odsdir!""}` default defensively, since `AMSetupServlet.getBaseDir()` (its value) can itself
return `null`, which FreeMarker's default object wrapper treats the same as "undefined" and would
otherwise throw. Both defaults were exercised through a throwaway FreeMarker 2.3.31 harness (real
`options.ftl` from disk, stub `page.getLocalizedString`) across three model shapes — new-install,
mid-upgrade, and the `upgrade=false && upgradeCompleted=true` gap specifically — confirming no
`InvalidReferenceException` in any of them; not kept as a checked-in test, same call as every
prior page's real-render check (see below).

### `Options` has no `onSecurityCheck()` override at all — the first migrated page like this

Every other migrated page (`Step1`-`Step7`, `Wizard`) ports `ProtectedPage`'s "block re-entry once
configured" check. `Options` never extended `ProtectedPage` in the first place — only
`TemplatedPage` — so it inherited Click `Page`'s default `onSecurityCheck()` (always `true`,
verified in the fork's source), meaning **the options page must stay reachable even after OpenAM
is configured**: that's exactly how a completed install reaches its upgrade-detection branch
(`isNewInstall()` returning `false`). `SetupPage`'s own default `onSecurityCheck()` is already
`true`, so no override was needed — just a comment on the class recording *why* one isn't there,
so it doesn't look like an oversight next to every other page's explicit override.
`DefaultSummary`, by contrast, did extend `ProtectedPage`, so it got the standard ported override,
identical in shape to `Step7`'s/`Wizard`'s.

### `Options.onInit()` cannot be exercised in an `openam-core` unit test at all — a new, broader category of environment gap

Every previous page's `onInit()` was at least partially unit-tested. `Options.onInit()` is not,
for a reason worth flagging clearly since it's a step beyond the prior per-branch gaps
(`Step2Test`'s `ServicesDefaultValues`, `Step3Test`'s live-network calls): its first real
computation, `EmbeddedOpenDS.isOpenDSVer1Installed()` → `getOpenDSVersion()` →
`AMSetupServlet.getBaseDir()`, unconditionally throws `ConfiguratorException("Servlet Context is
null")` unless `AMSetupServlet`'s private static `servletCtx` has already been set — which only
happens via that servlet's real `init(ServletConfig)`, called by the container because
`openam-server-only/.../WEB-INF/web.xml` declares `AMSetupServlet` with
`<load-on-startup>5</load-on-startup>`. That never runs in a bare `openam-core` test JVM, so *any*
call to `Options.onInit()` throws immediately, before even reaching the debug-parameter check at
the bottom of the method. Confirmed this is pre-existing, not introduced by the port: the old
Click `Options.doInit()` called `EmbeddedOpenDS.isOpenDSVer1Installed()` just as unconditionally.
Faking `AMSetupServlet.servletCtx` well enough to satisfy it (`getAppResource`, `getRealPath`, ...)
would need either reflection into a private static field of a large shared legacy class or calling
its real, heavyweight `init(...)` — disproportionate for this one page, so `OptionsTest` only
covers `isNewInstall()` (which doesn't touch any of this) and documents the rest as untestable
here, same call as every other environment-coupled gap in this migration.

### `DefaultSummary.createDefaultConfig()` not unit-tested — same call as `Wizard.createConfig()`, but with real IT coverage this time

Its only substantive logic is copying session state into request parameters ahead of one direct
call to the static `AMSetupServlet.processRequest(...)`, which performs real configuration writes
— the identical disproportionate-effort call already made for `Wizard.createConfig()` in
increment 5. Unlike that method, there's no dedicated Cargo/Selenium IT test for the default-config
path specifically, but `openam-server`'s existing `IT_Setup` (distinct from increment 5's
`IT_SetupWithOpenDJ`) already clicks `DemoConfiguration` then `createDefaultConfig` through the
real UI, so this isn't left completely unverified even without a unit test.

### `options.ftl`/`defaultSummary.ftl` real-render check

Same module-boundary gap as every prior page (`.ftl` lives in `openam-server-only`, no test tree;
`ConfiguratorServlet` lives in `openam-core`). Rendered both directly with a throwaway FreeMarker
2.3.31 harness (real `Configuration` + real templates from disk, stub `page.getLocalizedString`);
`options.ftl` was checked across the three model shapes described above. Not kept as checked-in
tests, same call as every prior page's real-render check; re-run manually if either template's var
references change.

`ConfiguratorServletTest`'s "unregistered path forwards to Click" example was moved from
`options.htm` (now migrated) to `upgrade.htm` (still Click, next up in increment 7) — update this
again whenever `Upgrade.java`/`upgrade.ftl` lands.

## Increment 7: Upgrade (`openam-upgrade` module)

The last real page. This is also the first (and, per the plan's increment order, only) migrated
page that doesn't live in `openam-core` — and that broke an assumption every prior increment's
registry entry relied on without ever stating it.

### `Upgrade.java` lives downstream of `ConfiguratorServlet`, so it can't be a compile-time registry entry

`openam-upgrade` depends on `openam-core` (for `openam-core`'s huge shared surface generally, not
specifically for anything configurator-related) — not the other way round. `ConfiguratorServlet`
(in `openam-core`) adding `PAGES.put("/config/upgrade/upgrade.htm", Upgrade.class)` the way every
prior increment did for its own page would need a compile-time import of `Upgrade`, which would
make `openam-core` depend on `openam-upgrade` too: a Maven cycle. Nothing in `03-migration-plan.md`
anticipated this because every other page (`Step1`-`Step7`, `Wizard`, `Options`, `DefaultSummary`)
already lived in `openam-core`.

Fix: a small `ConfiguratorPageProvider` SPI (`getPath()` / `getPageClass()`) in
`org.openidentityplatform.openam.config.servlet`, discovered via `ServiceLoader` —
`ConfiguratorServlet`'s static initializer now also runs
`ServiceLoader.load(ConfiguratorPageProvider.class)` and merges whatever it finds into `PAGES`.
`Upgrade` itself doesn't implement the SPI (it's a `SetupPage`, not a registration descriptor); a
tiny sibling class, `com.sun.identity.config.upgrade.UpgradePageProvider`, does, and
`openam-upgrade/src/main/resources/META-INF/services/org.openidentityplatform.openam.config.servlet.ConfiguratorPageProvider`
names it. `openam-upgrade` can freely implement an interface declared in `openam-core` (that's the
dependency direction that already exists), so this needs no new Maven dependency in either
direction — genuine decoupling, not just a workaround.

This isn't a novel idiom for this codebase: `com.sun.identity.setup.SetupListener` (also in
`openam-core`) is discovered the exact same way (`ServiceLoader.load(SetupListener.class)` in
`AMSetupServlet.registerListeners()`), with implementations from several downstream modules
(`openam-scripting`, `openam-radius`, `openam-audit-configuration`, ...) each dropping a
`META-INF/services` file. `ConfiguratorPageProvider` follows that established pattern rather than
inventing a new one (e.g. a startup `ServletContextListener` + a public
`ConfiguratorServlet.registerPage(...)` API, which was considered and rejected: it would need a new
per-increment `web.xml` listener entry, breaking the "pure Java, wired once in increment 1"
invariant the whole registry design is built on).

**Consequence for test coverage, worth knowing before trusting `ConfiguratorServletTest` at face
value:** `openam-core`'s own test classpath never has `openam-upgrade` on it, so
`ServiceLoader.load(ConfiguratorPageProvider.class)` always finds zero providers *there* — meaning
`ConfiguratorServletTest`'s `unregisteredPathForwardsToClickByName` test, which still uses
`/config/upgrade/upgrade.htm`, keeps passing after this increment, but for a different reason than
before (module-boundary artifact of the test run, not because the page is actually unmigrated in
the real, assembled WAR). There is no longer any real un-migrated page left to use as that example
— increment 7 was the last one before final removal. The real end-to-end proof that the
ServiceLoader wiring works lives in `openam-upgrade`'s own test tree instead
(`ConfiguratorServletUpgradeRoutingTest`, package `org.openidentityplatform.openam.config.servlet`),
which — unlike `openam-core` — can see both `ConfiguratorServlet` and `Upgrade`/
`UpgradePageProvider` on the same classpath. It drives a real `ConfiguratorServlet.service()` call
for `?actionLink=doUpgrade` and asserts the failure it gets back (a `ServletException` wrapping a
`NullPointerException` — see below) is the one that proves routing reached the real
`Upgrade.doUpgrade()`, while separately asserting the Click named-dispatcher was never invoked. This
is the first increment with a dedicated routing test living outside `openam-core`, and it's the
template to reuse if a future page ever migrates from a different downstream module.

### `onRender()` has no equivalent in `ConfiguratorServlet` — ported to `onGet()` instead

Unlike every previous page, the old `Upgrade.java` builds its template model in `onRender()`, not
`onInit()` or a Click `onGet()`/`onPost()` override, with an explicit comment explaining why:
`onRender()` only runs when Click is actually about to render the page, never on an `ActionLink`
dispatch (`doUpgrade`/`saveReport` both `return false` with no further Click processing) — so
`generateShortUpgradeReport(...)` (a real report-generation call, not free) is not redone on every
upgrade-progress poll or report download.

`SetupPage.onRender()` exists (copied into the increment-1 scaffolding for Click-lifecycle parity)
but grepping `ConfiguratorServlet.service()` confirms it is **never called** — the servlet's actual
flow is `onSecurityCheck()` -> `onInit()` -> (`actionLink` present: `invokeAction()` and stop) ->
else `onGet()` -> render if not skipped. `onGet()` is the hook that actually reproduces the "only
when about to render, not on actionLink dispatch" guarantee the old `onRender()` provided — so the
model-building logic moved there instead. `Upgrade` is the first page in this migration to actually
override `onGet()` for that reason; every earlier page either had no such optimization to preserve
or built its model unconditionally in `onInit()`. If a future page is ever found overriding
`onRender()` expecting it to run, that expectation is already wrong under `ConfiguratorServlet` —
worth deleting `SetupPage.onRender()` itself in the final increment's cleanup if nothing ever ends
up using it.

### `isLicenseAccepted()`: ported off `Context.getRequestParameter`, not `SetupPage.toString()`

The old code reads `getContext().getRequestParameter(SetupConstants.ACCEPT_LICENSE_PARAM)`, which —
checked directly against the Click fork's `Context.getRequestParameter()` source — is a straight
`request.getParameter(name)` (plus a null-name guard that's irrelevant for a constant). Ported as
`getContext().getRequest().getParameter(...)`, **not** `SetupPage.toString(paramName)` (used
elsewhere in this migration for the same kind of read): `toString()` trims whitespace and maps
`""`/blank to `null` before `Boolean.parseBoolean` would see it, which is extra normalization the
old code never applied here. Using it would have been a silent, if extremely unlikely to matter in
practice, behavior change — flagged here as the reason this one call site looks inconsistent with
the rest of the file's style.

### `doUpgrade()`'s pre-existing "no `error` guard" NPE — kept, locked in with a test

If `Upgrade`'s constructor fails (`error = true`, `upgrade` stays `null`) and `doUpgrade()` is
invoked anyway, `upgrade.upgrade(...)` NPEs — uncaught, since the method only catches
`UpgradeException`. This is original Click behavior, not introduced by this port. Checked
reachability the same way as `Wizard`'s dead `ActionLink`s and `Step7`'s `EXT_DATA_STORE` NPE:
`upgrade.ftl`'s `<#if error??>` branch (ported 1:1 from Velocity's `#if ($error)`) never renders the
`upgradeButton`/`doUpgrade()` JS function when `error` is true, so normal navigation can't trigger
it — but a direct `?actionLink=doUpgrade` request still would. Locked in with
`UpgradeTest.doUpgradeThrowsIfSubsystemFailedToInitialize` (and exercised for real, end-to-end,
through the servlet by `ConfiguratorServletUpgradeRoutingTest` above) so a future refactor doesn't
silently "fix" this without it being a deliberate, reviewed decision — same category and same
rationale as `Step7Test.onInitThrowsIfExtDataStoreNeverSet`.

### `UpgradeTest` environment gap — same category as `OptionsTest`, but this time the gap *is* the tested behavior

`Upgrade`'s constructor calls `AdminTokenAction.getInstance()` / `UpgradeServices.getInstance()`,
both needing a real bootstrapped OpenAM environment (SSOToken infrastructure, SMS-backed config) to
succeed. In a bare `openam-upgrade` unit-test JVM this always fails, and the constructor's broad
`catch (Exception ue)` turns that into `error = true` — exactly like the old Click page did. Unlike
every prior "can't test this branch" gap in this migration, here the untestable-without-a-real-
environment branch **is** the one branch this test JVM can reliably exercise: `onGet()`'s `error`
path and the resulting NPE in `doUpgrade()`. The happy path (`error == false`, a real
`generateShortUpgradeReport`/`upgrade()` call) is manual-smoke/IT-only, same as
`Wizard.createConfig()`/`DefaultSummary.createDefaultConfig()`.

### `upgrade.ftl` real-render check

Same module-boundary gap as every prior page (`.ftl` lives in `openam-server-only`, no test tree;
`ConfiguratorServlet` lives in `openam-core`). Rendered directly with a throwaway FreeMarker 2.3.31
harness (real `Configuration` + real `upgrade.ftl` from disk, stub `page.getLocalizedString`)
against both the `error` and normal (`currentVersion`/`newVersion`/`changelist` populated) branches;
confirmed no `InvalidReferenceException` in either. The `$var`-vs-`addModel` diff found no gaps this
time — `currentVersion`/`newVersion`/`changelist` are always set together in the one branch that
reads them, and `error` is read via `<#if error??>` (an existence check, not a boolean read),
matching the idiom already established for `Step7.ftl`'s `<#if embedded??>` for a key that's only
ever added when true. Not kept as a checked-in test, same call as every prior page's real-render
check; re-run manually if `upgrade.ftl`'s var references change.

This was the last page. What's left is increment 8, the final removal.
