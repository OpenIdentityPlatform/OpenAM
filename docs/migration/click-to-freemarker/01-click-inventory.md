# Apache Click usage inventory (OpenAM)

> Snapshot of where and how Apache Click is used, so this doesn't need to be
> re-discovered. Captured against `master` @ 16.1.2-SNAPSHOT.

## TL;DR

Apache Click powers **only** OpenAM's first-run **configurator / upgrade wizard** at
`/openam/config/*` on an *unconfigured* WAR. It does **not** touch the admin console (XUI/React),
auth, or federation runtime. To keep Click alive on Jakarta EE 9 the project carries a **vendored
fork of Apache Click 2.3.0** (57 files, ~36k LOC) and *still* depends on the abandoned upstream
javax-only jars.

Three surfaces must be unwound: (a) the fork, (b) the residual real-Click Maven deps, (c) the
consumer page classes + `.htm` templates.

## Modules involved

| Module dir | artifactId | Click footprint |
|---|---|---|
| `openam-core` | `openam` | The 57-class fork + 13/16 consumer page classes; declares the Click deps |
| `openam-upgrade` | `openam-upgrade` | 1 page class (`Upgrade.java`); no direct dep (inherits from openam-core) |
| `openam-server-only` | `openam-server-only` | Web assets: `click.xml`, `web.xml` ClickServlet + `*.htm` mapping, all 12 `.htm` templates |
| `openam-federation/OpenFM` | `OpenFM` | 2nd `web.xml` (`noconsole`) registering ClickServlet + `*.htm` mapping |

## Maven dependencies

- Root `pom.xml`: `<click.version>2.3.0</click.version>` (~line 90); fork packages listed in the
  javadoc/OSGi `excludePackageNames` (`org.apache.click`, `org.openidentityplatform.openam.click`,
  `org.openidentityplatform.openam.velocity`).
- `openam-core/pom.xml` (~lines 260–269): `org.apache.click:click-nodeps:2.3.0` and
  `org.apache.click:click-extras:2.3.0`.
- No other module references Click. `net.sf.click` is not used anywhere.

## The vendored fork (bulk of the code)

`openam-core/src/main/java/org/openidentityplatform/openam/click/` — **57 Java files**, a
repackaged Apache Click 2.3.0 made to run on `jakarta.servlet`. Sub-packages:
- root: `ClickServlet`, `Page`, `Context`, `Control`, `ControlRegistry`, `ActionEventDispatcher`,
  `ActionResult`, `PageInterceptor`, `ActionListener`
- `control/`: `Form`, `Field`, `TextField`, `TextArea`, `HiddenField`, `Button`, `ActionLink`,
  `AbstractLink`, `Table`, `Column`, `TablePaginator`, `Radio`, `RadioGroup`, `Label`, `FileField`,
  `Container`, `AbstractContainer`, `AbstractControl`, `Decorator`
- `service/`: `TemplateService`, `VelocityTemplateService`, `ConfigService`, `XmlConfigService`,
  `LogService`, `ConsoleLogService`, `ResourceService`, `ClickResourceService`, `FileUploadService`,
  `MessagesMapService`, `DefaultMessagesMapService`
- `util/`: `ClickUtils`, `ContainerUtils`, `PageImports`, `SessionMap`, `ErrorPage`, `ErrorReport`
- `element/`: `ResourceElement`, `JsImport`, `JsScript`, `CssImport`, `CssStyle`

Plus a small `org.openidentityplatform.openam.velocity` fork (Velocity is the fork's template
engine). The fork still `import`s upstream `org.apache.click.*` in ~30 places (util/service/
element), which is why the upstream 2.3.0 jars remain on the classpath.

## Consumer page classes (what actually uses Click)

Base/framework classes — `openam-core/src/main/java/com/sun/identity/config/util/`:
- `AjaxPage.java` — `extends org.openidentityplatform.openam.click.Page`; base for all config
  pages. **Most of it is framework-agnostic** (i18n `amConfigurator`, OpenDJ LDAP validation via
  `getConnection`, JSON AJAX helpers `writeValid`/`writeInvalid`/`writeJsonResponse`,
  `toString/toBoolean/toInt`, password checks, session get/set/reset). Click seams: `extends Page`,
  `ActionLink` fields, `getContext()`.
- `ProtectedPage.java` — `extends AjaxPage`; `onSecurityCheck()` blocks once
  `AMSetupServlet.isConfigured()` is true (the only access control — pre-config bootstrap UI).
- `TemplatedPage.java` — abstract templated-page base.
- `TemplatedForm.java` — **the only class importing REAL Apache Click**
  (`org.apache.click.control.Field` / `Form`); `extends` Click `Form`. **Dead code** — referenced
  nowhere but itself, so it can be deleted immediately (removes the last real-`org.apache.click`
  import from consumer code; the upstream jars then linger only for the fork).

Wizard pages — `openam-core/src/main/java/com/sun/identity/config/wizard/` (extend `ProtectedPage`):
- `Wizard.java` — the "execute" controller. `createConfig()` aggregates all session data from every
  step and calls `AMSetupServlet.processRequest()`. Also `testNewInstanceUrl`, `pushConfig`.
- `Step1.java` (admin+agent passwords), `Step2.java` (config dir + cookie domain),
  `Step3.java` (`extends LDAPStoreWizardPage`; config data store, embedded/external OpenDJ),
  `Step4.java` (external user store), `Step5.java` (site/LB), `Step6.java` (policy-agent password),
  `Step7.java` (summary).
- `LDAPStoreWizardPage.java` — shared base for LDAP-store steps (`clearStore`).

Other config pages — `openam-core/src/main/java/com/sun/identity/config/`:
- `DefaultSummary.java` (`extends ProtectedPage`) — one-click default-config path.
- `Options.java` (`extends TemplatedPage`) — upgrade/coexistence options + version detection.
- Support (non-page): `SessionAttributeNames.java` (session-key constants), `SetupWriter.java`,
  `pojos/LDAPStore.java`.

Upgrade page — `openam-upgrade/src/main/java/com/sun/identity/config/upgrade/Upgrade.java`
(`extends AjaxPage`) — runs `UpgradeServices`; `saveReport` streams the upgrade report.

**Totals:** 13 concrete page classes + 3 base classes; ~3,400 LOC in openam-core config +
~117 LOC upgrade.

## Templates (Velocity `.htm`)

`openam-server-only/src/main/webapp/` — **12 templates** (~2,829 lines):
- `click/error.htm`, `click/not-found.htm`
- `config/options.htm`, `config/defaultSummary.htm`
- `config/upgrade/upgrade.htm`
- `config/wizard/wizard.htm` (the JS tab shell) + `config/wizard/step1..step7.htm`

Template idioms to port: `$page.getLocalizedString("k")`, `$context` (context path),
`$path` (page path), `$startingTab`, plus per-page vars injected via Click's **public-field
exposure** (`$configStoreHost`, `$type`, `$store`, `$selectEmbedded`, …). Templates embed **YUI** JS
and drive AJAX field validation against `$context$path?actionLink=<name>` (the step posts back to its
own URL).

**Response contract (verified):** most handlers return **plain text** (`"true"`/`"ok"`/localized
error string), read by the templates as `response.responseText == "true"`. The
`{"valid":.., "body":..}` JSON template (`AjaxPage.java:80`) is essentially **unused** by these
pages. The one structured response is Step 3's `validateHostName`, which returns a **bespoke** JSON
shape (`{code, existingPort, embedded, replication, replicationPort, existingStoreHost,
existingStorePort, message}`) parsed via `eval(...)`. Each handler's exact output bytes must be
preserved. Note: `step1.htm` calls the **base-class `checkPasswords`** handler (plain text), *not*
`Step1`'s own `checkAdminPassword`/`checkAgentPassword` `ActionLink`s, which no template references.

## Configuration & servlet wiring

- `openam-server-only/src/main/webapp/WEB-INF/click.xml` — `<click-app>` `production` mode, pages
  package `com.sun.identity`, with an `<excludes>` list that excludes essentially everything
  except `/config/*` (also excludes `/config/auth/default/*` and `/config/federation/*`).
- `openam-server-only/src/main/webapp/WEB-INF/web.xml` — servlet `click-servlet` →
  `org.openidentityplatform.openam.click.ClickServlet` (the FORK), mapped to `*.htm`.
- `openam-federation/OpenFM/src/main/resources/xml/noconsole/web.xml` — same ClickServlet + `*.htm`
  mapping.
- `openam-server-only/src/main/webapp/WEB-INF/classes/click-page.properties` — Click's *own*
  control i18n (drop, don't migrate; app i18n is the `amConfigurator` bundle).

## Architecture (how the wizard flows)

1. `wizard.htm` renders a JS tab shell (`tab1..tab7`, `wizardStep1..7` divs) and, via YUI
   `AjaxUtils.load`, lazy-loads each `config/wizard/stepN.htm` as a **separate Click page request**
   into its div.
2. Each step's embedded JS POSTs to `$context$path?actionLink=<name>` → dispatched by ClickServlet
   to that page's `ActionLink`-bound method (e.g. `Step1.checkAdminPassword`), which validates and
   stores the value into the HTTP **session** (`SessionAttributeNames.*`), returning JSON/text.
3. The final **Create Configuration** calls `?actionLink=createConfig` → `Wizard.createConfig()`,
   which reads every session attribute, builds a wrapped request, and calls
   `AMSetupServlet.processRequest()`.

## Backend coupling (the only real integration points)

- Config: `com.sun.identity.setup.AMSetupServlet` (`processRequest`, `isConfigured`,
  `getPresetConfigDir`, `getErrorMessage`), `AMSetupUtils`, `SetupConstants`.
- Upgrade: `org.forgerock.openam.upgrade.UpgradeServices` / `UpgradeUtils` / `VersionUtils` with an
  admin `SSOToken` from `AdminTokenAction`.
- LDAP validation: OpenDJ SDK (`org.forgerock.opendj.ldap`) directly in `AjaxPage.getConnection()`.
- Session: Click `Context` session attributes keyed by `SessionAttributeNames`.
- i18n: `ResourceBundle` `amConfigurator`, locale from `?locale=` or `Accept-Language`.
