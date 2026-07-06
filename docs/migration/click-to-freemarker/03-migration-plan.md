# Migration plan: Click → Jakarta Servlets + FreeMarker

Living execution doc. Background in `01-click-inventory.md` and `02-recommendation.md`.

## Phase 1 — Pilot: migrate Step 1 end-to-end (parallel route)

Build alongside the live Click wizard (which keeps serving `*.htm`) so nothing breaks.

New code in `openam-core`, package `com.sun.identity.config.servlet`:

- **`ConfiguratorServlet`** (Jakarta `HttpServlet`) — `GET /config/setup/<page>` renders
  `<page>.ftl`; `POST /config/setup/<page>?action=<name>` dispatches to the page handler's named
  method (small action→method map or reflection — mirrors how Click's `ActionLink` bound
  `?actionLink=` to a method). Self-gates on `AMSetupServlet.isConfigured()` (port of
  `ProtectedPage.onSecurityCheck`).
- **`ConfiguratorContext`** — thin wrapper over request/response/session replacing Click `Context`
  (`getRequest/getResponse/getSessionAttribute/setSessionAttribute/removeSessionAttribute/
  getWriter`).
- **`SetupPage`** — new base = `AjaxPage` minus Click. Move `AjaxPage`'s framework-agnostic helpers
  here (or make `AjaxPage extend SetupPage`): the i18n bundle (`amConfigurator`),
  `writeValid/writeInvalid/writeJsonResponse`, `toString/toBoolean/toInt`, `getConnection` (OpenDJ),
  session helpers. Holds a `ConfiguratorContext`.
- **`Step1Page extends SetupPage`** — port of `Step1.checkAdminPassword`/`checkAgentPassword`
  (logic copied 1:1; only the `getContext()`/`ActionLink` seams change).
- A singleton FreeMarker `Configuration` with a `WebappTemplateLoader` rooted at `/config`
  (templates stay in the webapp).

Templates / wiring:
- `openam-server-only/src/main/webapp/config/setup/step1.ftl` — FreeMarker port of `step1.htm`.
  Model exposes `page` (the handler, for `getLocalizedString`), `context`, `path`.
- `openam-server-only/src/main/webapp/WEB-INF/web.xml` — register `ConfiguratorServlet` mapped to
  `/config/setup/*` **only**; leave `click-servlet` + `*.htm` untouched.
- `openam-core/pom.xml` — add managed `org.freemarker:freemarker` (version from root pom
  `freemarker.version` = 2.3.31).

## Phase 2 — Verify the pilot

- **Automated** (match openam-core's TestNG/JUnit + a servlet mock):
  1. Render `step1.ftl` via the FreeMarker `Configuration` with a stub `page` model → assert the
     localized title and `adminPassword`/`adminConfirm` inputs render.
  2. Drive `Step1Page.checkAdminPassword` with mock requests: mismatch → `passwords.do.not.match`;
     `<8` chars → `invalid.password.length`; valid → `OK` + `SessionAttributeNames.
     CONFIG_VAR_ADMIN_PWD` set in session. Assert the `{"valid":..}` JSON body.
- **Manual smoke:** build the `openam-server-only` WAR, deploy on Tomcat 10 / Jetty 11
  (unconfigured), browse `/openam/config/setup/step1`, confirm AJAX validation + localized errors.
- Record the exact working pattern below so rollout is copy-paste.

### Template token map (Velocity `.htm` → FreeMarker `.ftl`)

| Velocity | FreeMarker |
|---|---|
| `$page.getLocalizedString("k")` | `${page.getLocalizedString("k")}` |
| `$context` | `${context}` |
| `$path` | `${path}` |
| `$startingTab` | `${startingTab}` |
| `#if(...) ... #end` | `<#if ...> ... </#if>` |
| `#foreach($x in $xs) ... #end` | `<#list xs as x> ... </#list>` |

(Embedded YUI JS and AJAX endpoints are left as-is; only the `?actionLink=` → `?action=` param name
changes with the dispatch.)

## Phase 3 — Roll out + remove Click (after pilot sign-off)

**Per-page pattern:**
1. Port `<page>.htm` → `<page>.ftl` (token map above).
2. Port the page class: base `AjaxPage`/`ProtectedPage`/`TemplatedPage` → `SetupPage`; replace
   `ActionLink` fields with entries in the servlet's action→method dispatch; retarget `getContext()`
   to `ConfiguratorContext`.
3. Register the route; repoint cross-references (the shell loads `stepN.htm` URLs).

**Inventory to migrate** (`openam-core/.../config/` unless noted):
- `wizard/Wizard.java` + `wizard/wizard.htm` (JS tab shell)
- `wizard/Step1..Step7.java` + `wizard/step1..step7.htm`
- `wizard/LDAPStoreWizardPage.java`
- `Options.java` + `config/options.htm`
- `DefaultSummary.java` + `config/defaultSummary.htm`
- `click/error.htm` + `click/not-found.htm` → plain servlet error pages
- `openam-upgrade/.../config/upgrade/Upgrade.java` + `config/upgrade/upgrade.htm`
- Reimplement/retire `util/TemplatedForm.java` (only class importing real `org.apache.click`) and
  `util/TemplatedPage.java`.

**Final removal checklist:**
- [ ] `web.xml` in **both** `openam-server-only/src/main/webapp/WEB-INF/web.xml` and
  `openam-federation/OpenFM/src/main/resources/xml/noconsole/web.xml` — remove `click-servlet`
  registration + `*.htm` mapping; keep only `ConfiguratorServlet`.
- [ ] Delete `openam-core/src/main/java/org/openidentityplatform/openam/click/` (57 files) and the
  `org.openidentityplatform.openam.velocity` fork.
- [ ] Delete `WEB-INF/click.xml` and `WEB-INF/classes/click-page.properties`.
- [ ] `openam-core/pom.xml` — remove `click-nodeps` + `click-extras`.
- [ ] Root `pom.xml` — remove `<click.version>`; drop the click/velocity fork packages from the
  javadoc/OSGi `excludePackageNames`.
- [ ] Grep-sweep: zero `import ...click...` remaining; no lingering `*.htm` references.

## Verification (end-to-end)

- Pilot: Phase 2 tests pass + manual smoke renders/validates `/config/setup/step1` on an
  unconfigured WAR.
- Rollout: on a fresh unconfigured WAR, run the full wizard Steps 1→7 (incl. custom config/user
  LDAP stores hitting OpenDJ, site config), then **Create Configuration** succeeds via
  `AMSetupServlet.processRequest()` and the console launches. Exercise the Upgrade page against an
  older install. Final gate: full reactor build with Click removed and no `import ...click...`.

## Out of scope

XUI/admin console, auth/federation UIs, classic `openam-console` JSPs — none use Click.
