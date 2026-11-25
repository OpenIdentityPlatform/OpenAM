/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2026 3A Systems, LLC.
 */

import { randomUUID } from "node:crypto";
import { test as base, expect } from "@playwright/test";
import { OPENAM_BASE, getAdminToken, getAuthToken, PASSWORD, USERNAME } from "../common/openam-commons.mjs";
import {
  ensureClient, ensureOAuth2Provider, ensureProviderConfig, pkce, readUserCommonName, REDIRECT_URI,
  sessionContext, unreportedRequest, userNameValue,
} from "../common/oauth2-fixtures.mjs";

const CLIENT_ID = "test_client_oauth2_b5e_device";
const DEVICE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";
const DEVICE_CODE = OPENAM_BASE + "/oauth2/device/code";
const DEVICE_USER = OPENAM_BASE + "/oauth2/device/user";
const TOKEN_ENDPOINT = OPENAM_BASE + "/oauth2/access_token";
const TOKEN_REVOCATION = OPENAM_BASE + "/oauth2/token/revoke";
const DIRECT = { maxRedirects: 0 };
const TOKEN_FIELDS = ["refresh_token", "access_token", "id_token"];
const clientKey = "com.forgerock.openam.oauth2provider.";

function requestFactory(ignoreHTTPSErrors) {
  return {
    newContext: (options) => unreportedRequest.newContext({ ...options, ignoreHTTPSErrors, ...DIRECT }),
  };
}

async function requestFixture({ ignoreHTTPSErrors }, use) {
  const request = requestFactory(ignoreHTTPSErrors).newContext();
  try {
    await use(request);
  } finally {
    await request.dispose();
  }
}

const test = base.extend({
  anonymous: requestFixture,
  publicClient: requestFixture,
  admin: async ({ ignoreHTTPSErrors }, use) => {
    const request = requestFactory(ignoreHTTPSErrors).newContext();
    try {
      const token = await getAdminToken(request);
      requireNonempty(token, "Administrator session");
      await use({ request, token });
    } finally {
      await request.dispose();
    }
  },
  deviceClient: async ({ admin, publicClient }, use) => {
    await ensureOAuth2Provider(admin.token, admin.request);
    await ensureClient(admin.token, admin.request, CLIENT_ID, {
      [clientKey + "clientType"]: "Public",
      // Restlet classifies a form containing only client_id as client_secret_post when openid is allowed.
      // A public client registered with none should accept that form without a secret.
      [clientKey + "tokenEndPointAuthMethod"]: "client_secret_post",
      [clientKey + "responseTypes"]: ["[0]=code"],
      [clientKey + "grantTypes"]: ["[0]=authorization_code", "[1]=" + DEVICE_GRANT],
      [clientKey + "scopes"]: ["[0]=openid", "[1]=profile"],
      [clientKey + "defaultScopes"]: ["[0]=openid"],
      [clientKey + "redirectionURIs"]: ["[0]=" + REDIRECT_URI],
      isConsentImplied: false,
      sunIdentityServerDeviceStatus: "Active",
    });
    await use(publicClient);
  },
  owner: async ({ ignoreHTTPSErrors }, use) => {
    const factory = requestFactory(ignoreHTTPSErrors);
    let session;
    try {
      // The unknown-code test completes its anonymous step before authenticating.
      await use(async () => {
        const login = factory.newContext();
        try {
          const token = await getAuthToken(login, USERNAME, PASSWORD);
          requireNonempty(token, "Resource owner session");
          session = await sessionContext(factory, token);
          return session;
        } finally {
          await login.dispose();
        }
      });
    } finally {
      await session?.dispose();
    }
  },
});

test.use({ trace: "off", screenshot: "off" });

function isNonempty(value) {
  return typeof value === "string" && value.trim().length > 0;
}

function requireNonempty(value, label) {
  expect(isNonempty(value), label + " must be a nonempty string").toBe(true);
}

function expectContentType(response, mediaType, label) {
  const type = response.headers()["content-type"]?.split(";")[0].trim().toLowerCase();
  expect(type === mediaType, label + " content type").toBe(true);
}

function expectStatus(response, status, label) {
  expect(response.status(), label + " HTTP status").toBe(status);
}

async function htmlPage(response, status, label) {
  expectStatus(response, status, label);
  expectContentType(response, "text/html", label);
  const directives = response.headers()["cache-control"]?.split(",").map((part) => part.trim().toLowerCase());
  expect(directives?.includes("no-store"), label + " Cache-Control includes no-store").toBe(true);
  return response.text();
}

// Read the model from the inline script as text, without running it.
function pageData(html) {
  const literal = /^\s*pageData\s*=\s*(\{[\s\S]*?^\s*\})(?:;|(?=\s*<\/script>))/m.exec(html)?.[1];
  requireNonempty(literal, "pageData");
  // The templates use bare property names and FreeMarker's JavaScript string escapes.
  const json = literal.replace(/"(?:\\.|[^"\\])*"|(\w+)(\s*:)/g, (token, key, colon) => {
    if (key) return JSON.stringify(key) + colon;
    return token.replace(/\\(\\|['>]|x[\da-f]{2})/gi, (escape, value) => {
      if (value === "\\") return escape;
      return value.startsWith("x") ? "\\u00" + value.slice(1) : value;
    });
  });
  try {
    return JSON.parse(json);
  } catch {
    throw new Error("Invalid pageData object");
  }
}

function expectMain(html, module) {
  const main = /<script\b[^>]*\bdata-main="([^"]+)"/.exec(html)?.[1];
  expect(main?.endsWith("/XUI/" + module), "XUI entry point is " + module).toBe(true);
}

async function entryCsrf(response) {
  const html = await htmlPage(response, 200, "Public code entry form");
  expectMain(html, "main-device");
  const model = pageData(html);
  expect(Object.hasOwn(model, "errorCode"), "Code entry form has no error").toBe(false);
  expect(Object.hasOwn(model, "done"), "Code entry form is incomplete").toBe(false);
  const csrf = model.csrf;
  requireNonempty(csrf, "Code entry CSRF");
  return csrf;
}

async function issueCode(request, parameters) {
  const response = await request.post(DEVICE_CODE, { ...DIRECT, form: { client_id: CLIENT_ID, ...parameters } });
  expectStatus(response, 200, "Device code issuance");
  expectContentType(response, "application/json", "Device code issuance");
  const code = await response.json();
  requireNonempty(code.user_code, "User code");
  requireNonempty(code.device_code, "Device code");
  return code;
}

async function codeForEntry(request, parameters = {}) {
  const { verifier, challenge } = await pkce();
  const code = await issueCode(request, {
    scope: "profile", response_type: "code", code_challenge: challenge, code_challenge_method: "S256", ...parameters,
  });
  return { ...code, verifier };
}

async function submitCode(request, userCode, url = DEVICE_USER, headers) {
  const csrf = await entryCsrf(await request.get(url, DIRECT));
  return request.post(url, { ...DIRECT, form: { user_code: userCode, csrf }, headers });
}

async function consentPage(request, code, headers) {
  const response = await submitCode(request, code.user_code, DEVICE_USER, headers);
  const html = await htmlPage(response, 200, "Device consent page");
  expectMain(html, "main-authorize");
  const model = pageData(html);
  const data = model.oauth2Data;
  expect(Boolean(data), "pageData contains oauth2Data").toBe(true);
  expect(data.clientId === CLIENT_ID, "Consent client ID").toBe(true);
  expect(data.userCode === code.user_code, "Consent user code matches issuance").toBe(true);
  requireNonempty(data.csrf, "Consent CSRF");
  return { model, data, csrf: data.csrf };
}

function submitDecision(request, code, fields) {
  return request.post(DEVICE_USER, {
    ...DIRECT, form: { user_code: code.user_code, save_consent: "off", ...fields },
  });
}

function parseLocation(response, label) {
  const location = response.headers()["location"];
  requireNonempty(location, label + " Location");
  try {
    return new URL(location, DEVICE_USER);
  } catch {
    throw new Error(label + " returned an invalid Location");
  }
}

async function expectPoll(request, code, status, error) {
  const label = error ?? "Approved device code";
  const response = await request.post(TOKEN_ENDPOINT, {
    ...DIRECT,
    form: {
      grant_type: DEVICE_GRANT, client_id: CLIENT_ID, device_code: code.device_code, code_verifier: code.verifier,
    },
  });
  const body = await response.json();
  const tokens = [body.refresh_token, body.access_token].filter(isNonempty);
  try {
    expectStatus(response, status, label);
    expectContentType(response, "application/json", label);
    if (error) {
      expect(body.error === error, label + " OAuth2 error").toBe(true);
      if (error === "authorization_pending") {
        expect(body.error_description === "The user has not yet completed authorization",
          label + " OAuth2 error description").toBe(true);
      }
      for (const field of TOKEN_FIELDS) {
        expect(Object.hasOwn(body, field), label + " omits " + field).toBe(false);
      }
    } else {
      requireNonempty(body.access_token, label + " access token");
    }
  } finally {
    const failures = [];
    for (const token of tokens) {
      try {
        const revoked = await request.post(TOKEN_REVOCATION, { ...DIRECT, form: { client_id: CLIENT_ID, token } });
        if (!revoked.ok()) failures.push(revoked.status());
      } catch {
        failures.push("request failed");
      }
    }
    expect.soft(failures, label + " token revocation failures").toEqual([]);
  }
}

test.describe("OAuth2 device endpoints", () => {
  test("issues a device code and verification URI", async ({ deviceClient }) => {
    const code = await issueCode(deviceClient, { scope: "openid profile", response_type: "device_code" });
    expect(code.verification_uri === DEVICE_USER, "Device verification URI").toBe(true);
    expect(typeof code.interval === "number" && code.interval > 0, "Positive polling interval").toBe(true);
    expect(typeof code.expires_in === "number" && code.expires_in > 0, "Positive device code lifetime").toBe(true);
  });

  test("rejects missing device code parameters", async ({ anonymous }) => {
    const response = await anonymous.post(DEVICE_CODE, { ...DIRECT, form: { client_id: "b5e-" + randomUUID() } });
    expectStatus(response, 400, "Missing device code parameters");
    expectContentType(response, "application/json", "Missing device code parameters");
    const body = await response.json();
    expect(body.error === "bad_request", "Missing parameters OAuth2 error").toBe(true);
    expect(body.error_description === "client_id, scope and response_type are required parameters",
      "Missing parameters OAuth2 error description").toBe(true);
  });

  test("renders the public code entry form", async ({ anonymous }) => {
    await entryCsrf(await anonymous.get(DEVICE_USER, DIRECT));
  });

  test("redirects anonymous device submissions to login", async ({ anonymous, deviceClient }) => {
    const code = await codeForEntry(deviceClient);
    const initialUrl = new URL(DEVICE_USER);
    initialUrl.searchParams.set("user_code", code.user_code);
    initialUrl.searchParams.append("b5e_marker", "one");
    initialUrl.searchParams.append("b5e_marker", "two");

    // Restlet removes the code from the address before rendering the form.
    const initial = await anonymous.get(initialUrl.href, DIRECT);
    expectStatus(initial, 303, "Device user GET with user_code");
    const formUrl = parseLocation(initial, "Device code removal");
    expect(formUrl.origin === initialUrl.origin, "Code entry redirect origin").toBe(true);
    expect(formUrl.pathname === initialUrl.pathname, "Code entry redirect path").toBe(true);
    expect(formUrl.searchParams.has("user_code"), "Code entry redirect omits user_code").toBe(false);
    expect(JSON.stringify(formUrl.searchParams.getAll("b5e_marker")) === '["one","two"]',
      "Code entry redirect preserves both markers").toBe(true);

    const submission = await submitCode(anonymous, code.user_code, formUrl.href);
    expectStatus(submission, 301, "Anonymous device user POST");
    const login = parseLocation(submission, "Anonymous device login");
    const expectedLogin = new URL(OPENAM_BASE + "/UI/Login");
    expect(login.origin === expectedLogin.origin, "Device login origin").toBe(true);
    expect(login.pathname === expectedLogin.pathname, "Device login path").toBe(true);
    expect(login.searchParams.get("goto") === formUrl.href, "Login returns to the code entry URL").toBe(true);
    await expectPoll(deviceClient, code, 400, "authorization_pending");
  });

  test("renders consent for an authenticated submission", async ({ deviceClient, owner }) => {
    const code = await codeForEntry(deviceClient);
    await consentPage(await owner(), code);
  });

  test("renders not_found for an unknown user code", async ({ anonymous, owner }) => {
    async function expectUnknown(request) {
      // Device codes use Base58. This value cannot collide with a live code.
      const response = await submitCode(request, "not-a-valid-device-code");
      const html = await htmlPage(response, 200, "Unknown user code");
      expectMain(html, "main-device");
      expect(pageData(html).errorCode === "not_found", "Unknown user code marker").toBe(true);
      expect(response.headers()["location"] === undefined, "Unknown user code has no Location").toBe(true);
    }
    await test.step("anonymous", () => expectUnknown(anonymous));
    await test.step("authenticated", async () => expectUnknown(await owner()));
  });

  test("renders the device consent model", async ({ admin, deviceClient, owner, page }) => {
    await ensureProviderConfig(admin.token, admin.request);
    const code = await codeForEntry(deviceClient, {
      scope: "openid profile", state: "device-state", nonce: "device-nonce", ui_locales: "fr-CA",
    });
    const { model, data } = await consentPage(await owner(), code, { "Accept-Language": "en-US" });

    expect(model.locale === "fr-CA", "Stored ui_locales takes precedence").toBe(true);
    expect(model.realm === "/", "Consent model uses the root realm").toBe(true);
    for (const [field, value] of Object.entries({
      state: "device-state", nonce: "device-nonce", displayName: CLIENT_ID, responseType: "code",
      formTarget: new URL(DEVICE_USER).pathname,
    })) {
      expect(data[field] === value, "Device consent " + field).toBe(true);
    }
    const scopes = ["openid", "profile"];
    const scope = typeof data.scope === "string" ? data.scope.trim().split(/\s+/).sort() : [];
    expect(JSON.stringify(scope) === JSON.stringify(scopes), "Device consent scope").toBe(true);
    expect(Array.isArray(data.displayScopes), "Device consent has displayScopes").toBe(true);
    expect(data.displayScopes.map(({ name }) => name).sort(), "Displayed scopes").toEqual(scopes);
    expect(data.isSaveConsentEnabled === true, "Consent can be saved").toBe(true);

    const commonName = await readUserCommonName(admin.token, admin.request, USERNAME);
    requireNonempty(data.userName, "Device consent owner name");
    expect(await userNameValue(page, "userName: " + JSON.stringify(data.userName)), "Device consent owner name")
      .toBe(commonName);
  });

  test("renders the thanks page for allow and deny", async ({ deviceClient, owner }) => {
    const session = await owner();
    for (const decision of ["allow", "deny"]) {
      await test.step(decision, async () => {
        const code = await codeForEntry(deviceClient);
        const { csrf } = await consentPage(session, code);
        const response = await submitDecision(session, code, { decision, csrf });
        const html = await htmlPage(response, 200, "Device consent " + decision);
        const model = pageData(html);
        expect(model.done === true, "Device consent is complete").toBe(true);
        // CodeThanks.ftl currently appends /XUI to the realm. The realm field should identify the root realm as /.
        expect(model.realm === "//XUI", "Thanks page realm").toBe(true);

        if (decision === "allow") {
          await expectPoll(deviceClient, code, 200);
        } else {
          await expectPoll(deviceClient, code, 403, "authorization_declined");
        }
      });
    }
  });

  test("rejects missing and wrong consent CSRF", async ({ deviceClient, owner }) => {
    const session = await owner();
    for (const [name, fields] of [["missing", {}], ["wrong", { csrf: "not-the-token" }]]) {
      await test.step(name, async () => {
        const code = await codeForEntry(deviceClient);
        await consentPage(session, code);
        const response = await submitDecision(session, code, { decision: "allow", ...fields });
        const html = await htmlPage(response, 400, "Device consent with " + name + " CSRF");
        expect(html.includes("<title>OAuth2 Error Page</title>"), "OAuth2 error page title").toBe(true);
        expect(pageData(html).error?.message === "bad_request", "CSRF OAuth2 error").toBe(true);
        expect(response.headers()["location"] === undefined, "CSRF error has no Location").toBe(true);
        await expectPoll(deviceClient, code, 400, "authorization_pending");
      });
    }
  });

  test("rejects PUT on the device verification endpoint", async ({ anonymous }) => {
    expectStatus(await anonymous.put(DEVICE_USER, DIRECT), 405, "Device user PUT");
  });
});
