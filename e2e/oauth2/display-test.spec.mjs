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

/*
 * The ?display= surface of the consent pages on /oauth2/authorize and /oauth2/device/user.
 * OAuth2Representation picks the template folder from the
 * parameter: page/ (also for an absent or empty value), popup/ (composed into popup.ftl), touch/ or wap/. This spec
 * pins that black-box contract before /oauth2 moves to CHF. A flip may change a pinned value only as a divergence
 * listed in its PR.
 *
 * Two pinned values are master defects, and the assertions name their targets: touch/authorize.ftl writes the
 * client name under `isplayName`, and an unknown display answers 400 server_error instead of 400 invalid_request.
 */

import { test, expect, request as apiRequest } from "@playwright/test";
import { OPENAM_BASE, getAdminToken, getAuthToken, PASSWORD, USERNAME } from "../common/openam-commons.mjs";
import { REALM, REDIRECT_URI, SCOPE, ensureProviderConfig, pkce, sessionContext } from "../common/oauth2-fixtures.mjs";

/** This spec's own clients: Playwright runs spec files in parallel, and a client another file rewrites mid-run
 *  would break this one. */
const CONSENT_CLIENT_ID = "test_client_display";
const DEVICE_CLIENT_ID = "test_client_display_device";

const OAUTH2 = `${OPENAM_BASE}/oauth2`;
/** The deployment's context path, so the form-target checks survive a deployment mounted elsewhere. */
const CONTEXT_PATH = new URL(OPENAM_BASE).pathname.replace(/\/$/, "");
/** Content-Type of every page below, the WML one included. */
const HTML_TYPE = "text/html;charset=UTF-8";

let adminToken;
let demoToken;
let demoCtx;
/** The name the consent page shows for the resource owner. */
let demoName;
let challenge;

const agentHeaders = () => ({
  iPlanetDirectoryPro: adminToken, "Content-Type": "application/json",
  "Accept-API-Version": "protocol=2.0,resource=1.0",
});

/**
 * Writes a client that requires consent. `isConsentImplied:false` is the whole point: the provider requires consent
 * when `!clientsCanSkipConsent || !isConsentImplied`, so this flag alone sends the request to the consent page, the
 * only page `?display=` selects a template for.
 */
async function writeClient(request, clientId, extra) {
  const written = await request.put(
    `${OPENAM_BASE}/json/realms/${REALM}/realm-config/agents/OAuth2Client/${clientId}`, {
      headers: agentHeaders(),
      data: {
        "com.forgerock.openam.oauth2provider.clientType": "Public",
        "com.forgerock.openam.oauth2provider.redirectionURIs": [`[0]=${REDIRECT_URI}`],
        "com.forgerock.openam.oauth2provider.scopes": [`[0]=${SCOPE}`, "[1]=openid"],
        "com.forgerock.openam.oauth2provider.defaultScopes": [`[0]=${SCOPE}`],
        "com.forgerock.openam.oauth2provider.responseTypes": ["[0]=code", "[1]=token"],
        "com.forgerock.openam.oauth2provider.tokenEndPointAuthMethod": "none",
        isConsentImplied: false,
        sunIdentityServerDeviceStatus: "Active",
        ...extra,
      },
    });
  if (!written.ok()) {
    throw new Error(`Failed to write ${clientId}: ${written.status()} ${written.statusText()} ${await written.text()}`);
  }
}

test.beforeAll(async ({ request }) => {
  adminToken = await getAdminToken(request);
  expect(adminToken, "Admin login must return an SSO token").toEqual(expect.any(String));
  expect(adminToken).toBeTruthy();
  await ensureProviderConfig(adminToken, request);
  await writeClient(request, CONSENT_CLIENT_ID, {
    "com.forgerock.openam.oauth2provider.grantTypes": ["[0]=authorization_code"],
  });
  await writeClient(request, DEVICE_CLIENT_ID, {
    "com.forgerock.openam.oauth2provider.grantTypes": [
      "[0]=authorization_code", "[1]=urn:ietf:params:oauth:grant-type:device_code",
    ],
  });

  // The provider enforces PKCE (codeVerifierEnforced defaults to true), so every response_type=code request below
  // needs an S256 challenge, or it fails validation before a template is chosen.
  challenge = (await pkce()).challenge;

  // The consent page names the resource owner by the provider's display name attribute, cn by default. CI creates
  // demo with cn "Demo Demo"; a local embedded store keeps its own demo entry, so the name is read, not assumed.
  const demo = await request.get(`${OPENAM_BASE}/json/realms/${REALM}/users/${USERNAME}`, {
    headers: { iPlanetDirectoryPro: adminToken, "Accept-API-Version": "resource=3.0, protocol=2.1" },
  });
  if (!demo.ok()) {
    throw new Error(`Failed to read ${USERNAME}: ${demo.status()} ${demo.statusText()} ${await demo.text()}`);
  }
  demoName = (await demo.json()).cn?.[0];
  if (!demoName) {
    throw new Error(`${USERNAME} has no cn`);
  }

  const loginCtx = await apiRequest.newContext({ ignoreHTTPSErrors: true });
  try {
    demoToken = await getAuthToken(loginCtx, USERNAME, PASSWORD);
    expect(demoToken, "Resource-owner login must return an SSO token").toEqual(expect.any(String));
    expect(demoToken).toBeTruthy();
  } finally {
    await loginCtx.dispose();
  }
  // /oauth2/authorize authenticates by the session cookie, never by the iPlanetDirectoryPro header.
  demoCtx = await sessionContext(apiRequest, demoToken);
});

test.afterAll(async () => {
  await demoCtx?.dispose();
});

/** The authorize query, built by hand so the `display` value is the only thing that moves between tests. */
function authorizeQuery(display) {
  const parts = [
    "response_type=code", `client_id=${CONSENT_CLIENT_ID}`,
    `redirect_uri=${encodeURIComponent(REDIRECT_URI)}`, `scope=${SCOPE}`, "state=display",
    `code_challenge=${encodeURIComponent(challenge)}`, "code_challenge_method=S256",
  ];
  if (display !== undefined) parts.push(`display=${display}`);
  return parts.join("&");
}

/**
 * A value the templates interpolate into the `pageData` object literal, scraped out of the rendered page.
 *
 * Anchored on a preceding delimiter: unanchored, `isplayName` would also match inside `displayName`, and the touch
 * tests could no longer tell the two keys apart.
 */
function pageDataValue(html, key) {
  const match = new RegExp(`(?:^|[{,\\s])${key}:\\s*"([^"]*)"`).exec(html);
  return match && match[1];
}

/** Mints a device code for the consent-requiring device client and returns its user code. */
async function deviceCode(request) {
  const response = await request.post(`${OAUTH2}/device/code`, {
    form: {
      client_id: DEVICE_CLIENT_ID, scope: SCOPE, response_type: "code",
      code_challenge: challenge, code_challenge_method: "S256",
    },
  });
  if (!response.ok()) {
    throw new Error(`device/code failed: ${response.status()}`);
  }
  const userCode = (await response.json()).user_code;
  expect(typeof userCode === "string").toBe(true);
  expect(Boolean(userCode)).toBe(true);
  return userCode;
}

/**
 * Submits a user code as the code-entry form does, with the demo session cookie and with the session id as the csrf
 * value (accepted for non-browser clients). display travels in the query; the user code only in the body.
 */
function submitUserCode(userCode, display) {
  return demoCtx.post(`${OAUTH2}/device/user?display=${display}`, {
    form: { user_code: userCode, csrf: demoToken },
    maxRedirects: 0,
  });
}

/** ESAPI HTML-encodes the name before FreeMarker interpolates it; decode once as inert text, never markup. */
async function userNameValue(page, html) {
  return page.evaluate((encoded) => {
    if (encoded === null) return null;
    const text = document.createElement("textarea");
    text.innerHTML = encoded;
    return text.value;
  }, pageDataValue(html, "userName"));
}

test.describe("?display= on /oauth2/authorize", () => {

  test("display=page renders the page/ consent template", async ({ page }) => {
    const response = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery("page")}`, { maxRedirects: 0 });
    const html = await response.text();
    console.log(`[display] page -> ${response.status()} ct=${response.headers()["content-type"]} bytes=${html.length}`);

    expect(response.status(), html).toBe(200);
    // Every display is served as text/html, WML included: the template representation's media type does not vary
    // by folder.
    expect(response.headers()["content-type"]).toBe(HTML_TYPE);
    // AuthorizeEndpointFilter stamps every /authorize answer, the consent page included.
    expect(response.headers()["cache-control"]).toBe("no-store");
    expect(response.headers()["pragma"]).toBe("no-cache");

    expect(html).toMatch(/^<!DOCTYPE html>/);
    // ConsentRequiredResource builds the model from the request's attributes and query plus its own keys. These are
    // the ones this template reads.
    expect(pageDataValue(html, "displayName")).toBe(CONSENT_CLIENT_ID);
    expect(pageDataValue(html, "clientId")).toBe(CONSENT_CLIENT_ID);
    expect(await userNameValue(page, html), "Consent must name the resource owner").toBe(demoName);
    expect(pageDataValue(html, "responseType")).toBe("code");
    expect(pageDataValue(html, "redirectUri")).toBe(REDIRECT_URI);
    expect(pageDataValue(html, "scope")).toBe(SCOPE);
    expect(pageDataValue(html, "state")).toBe("display");
    expect(pageDataValue(html, "csrf")).toBeTruthy();
    // page/authorize.ftl is the only consent template with the saveConsentEnabled guard, which is why the popup and
    // touch tests assert its absence: the cheapest proof that a different file was rendered.
    expect(html).toContain("isSaveConsentEnabled: true");
    // The post-back target is the request's path and its raw query, display included, so a consent POST stays on the
    // same template. ?js_string escapes the leading slash.
    expect(pageDataValue(html, "formTarget"), "Consent target must preserve the raw authorize query")
      .toBe(`\\${CONTEXT_PATH}/oauth2/authorize?${authorizeQuery("page")}`);
  });

  test("an EMPTY display= is the page/ template, not a 400", async () => {
    // `?display=` is a reachable URL and reads back as an empty string. OAuth2Representation treats empty like
    // absent and renders page/; Enum.valueOf("") would be the 400 of the unknown-display test.
    const response = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery("")}`, { maxRedirects: 0 });
    const html = await response.text();
    console.log(`[display] empty -> ${response.status()} bytes=${html.length}`);

    expect(response.status(), html).toBe(200);
    expect(html).toMatch(/^<!DOCTYPE html>/);
    expect(pageDataValue(html, "displayName")).toBe(CONSENT_CLIENT_ID);
    expect(html).toContain("isSaveConsentEnabled: true");
  });

  test("an absent display defaults to the page/ consent template", async () => {
    const response = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery()}`, { maxRedirects: 0 });
    const html = await response.text();
    console.log(`[display] absent -> ${response.status()} bytes=${html.length}`);

    expect(response.status(), html).toBe(200);
    expect(html).toContain("isSaveConsentEnabled: true");
    expect(pageDataValue(html, "displayName"), "Absent display must use the page template").toBe(CONSENT_CLIENT_ID);
  });

  test("display=popup composes the wrapper AROUND the consent page", async ({ page }) => {
    const response = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery("popup")}`, { maxRedirects: 0 });
    const html = await response.text();
    console.log(`[display] popup -> ${response.status()} bytes=${html.length}`);

    expect(response.status(), html).toBe(200);
    expect(response.headers()["content-type"]).toBe(HTML_TYPE);
    // popup.ftl's own doctype is lowercase where every other template's is uppercase: the cheapest discriminator for
    // "the wrapper was rendered, not the bare page".
    expect(html).toMatch(/^<!doctype html>/);
    expect(html).toContain("<title>Authorize popup</title>");
    expect(html).toContain("function poponload()");
    // The composition: OAuth2Representation renders popup/authorize.ftl first and interpolates it, unescaped, into
    // popup.ftl's htmlCode. The model must be inside #print because poponload copies only that container.
    expect(html).toContain('<div id="print" style="visibility: hidden">');
    expect(pageDataValue(html, "displayName")).toBe(CONSENT_CLIENT_ID);
    expect(pageDataValue(html, "clientId")).toBe(CONSENT_CLIENT_ID);
    // Parse inert HTML so the wrapper's onload handler and the embedded scripts never run.
    const popupContent = await page.evaluate((source) => {
      const doc = new DOMParser().parseFromString(source, "text/html");
      return doc.getElementById("print")?.innerHTML ?? "";
    }, html);
    expect(popupContent, "Popup container must contain the consent model").toContain(`displayName: "${CONSENT_CLIENT_ID}"`);
    expect(popupContent, "Popup container must contain the consent model").toContain(`clientId: "${CONSENT_CLIENT_ID}"`);
    // popup/authorize.ftl has no saveConsentEnabled guard, so the embedded page is popup/'s and not page/'s.
    expect(html).not.toContain("isSaveConsentEnabled");
  });

  test("display=touch renders a BLANK client name -- the isplayName typo", async ({ page }) => {
    const response = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery("touch")}`, { maxRedirects: 0 });
    const html = await response.text();
    console.log(`[display] touch -> ${response.status()} bytes=${html.length}`
      + ` isplayName=${JSON.stringify(pageDataValue(html, "isplayName"))}`
      + ` displayName=${JSON.stringify(pageDataValue(html, "displayName"))}`);

    expect(response.status(), html).toBe(200);
    expect(response.headers()["content-type"]).toBe(HTML_TYPE);

    // Master defect: templates/touch/authorize.ftl writes the client name under `isplayName`, so the key the XUI
    // reads, `displayName`, is missing and the consent heading renders empty. Target: `displayName`, as on page/ and
    // popup/. The model is intact: the value is on the page under the wrong key.
    expect(pageDataValue(html, "isplayName")).toBe(CONSENT_CLIENT_ID);
    expect(pageDataValue(html, "displayName"), "Touch displayName must be absent").toBeNull();

    // Everything else on the touch page is intact, which makes the missing name a template typo rather than a
    // broken model.
    expect(pageDataValue(html, "clientId")).toBe(CONSENT_CLIENT_ID);
    expect(await userNameValue(page, html), "Consent must name the resource owner").toBe(demoName);
    expect(pageDataValue(html, "csrf")).toBeTruthy();
    expect(html).not.toContain("isSaveConsentEnabled");
  });

  test("display=wap is WML served as text/html, and DOES render the client name", async () => {
    const response = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery("wap")}`, { maxRedirects: 0 });
    const html = await response.text();
    console.log(`[display] wap -> ${response.status()} ct=${response.headers()["content-type"]} bytes=${html.length}`);

    expect(response.status(), html).toBe(200);
    // A WML document goes out as text/html: the template representation's media type does not vary by folder.
    expect(response.headers()["content-type"]).toBe(HTML_TYPE);
    expect(html).toMatch(/^<\?xml version="1\.0"\?>/);
    expect(html).toContain('<!DOCTYPE wml PUBLIC "-//WAPFORUM//DTD WML 1.2//EN"');
    expect(html).toContain("<wml>");
    // No pageData: wap/ renders server-side instead of handing a model to the XUI, so it proves the model reaches the
    // template, not only the browser.
    expect(html).not.toContain("pageData");
    expect(html).toContain(`<p>${CONSENT_CLIENT_ID}:</p>`);
    // display_scope, the raw scope descriptions only this template reads. ConsentRequiredResource writes it after
    // copying the query into the model, so a client cannot supply its own (CVE-2026-62280).
    expect(html).toContain(`<b>${SCOPE}</b>`);
    // A real HTML form with the CSRF token and the two decision buttons, posted back to the same URL.
    const target = `${CONTEXT_PATH}/oauth2/authorize?${authorizeQuery("wap")}`;
    const escapedTarget = target.replaceAll("&", "&amp;");
    expect(html, "WAP action must preserve the complete authorize query")
      .toContain(`<form action="${escapedTarget}" method="post">`);
    expect(html).toMatch(/<input type="hidden" name="csrf" value="[^"]+"\/>/);
    expect(html).toContain('<input type="submit" name="decision" class="button gray" value="Allow"/>');
  });

  test("an UNKNOWN display is a non-redirecting 400 server_error page", async () => {
    const response = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery("bogus")}`, { maxRedirects: 0 });
    const html = await response.text();
    console.log(`[display] bogus -> ${response.status()} bytes=${html.length}`);

    expect(response.status(), html).toBe(400);
    // Rendered in place, not redirected to the client.
    expect(response.headers()["location"], "Unknown display must not redirect").toBeUndefined();
    expect(response.headers()["content-type"]).toBe(HTML_TYPE);
    expect(html).toContain("<title>OAuth2 Error Page</title>");
    // Master defect. Enum.valueOf's IllegalArgumentException is thrown while the consent page renders, inside
    // AuthorizeResource's ResourceOwnerConsentRequired handler and so past its invalid_request mapping, and
    // ExceptionHandler turns it into ServerException's 400 server_error. Target: 400 invalid_request (RFC 6749
    // §4.1.2.1, an invalid parameter value). The description is Restlet's status text and is not pinned.
    expect(html).toContain('message: "server_error"');
  });

  test("the display name is case-INSENSITIVE, unlike route matching", async () => {
    // OAuth2Representation upper-cases the value before Enum.valueOf; popup wrapper selection separately ignores
    // case. Route matching is case-sensitive (/oauth2/Tokeninfo is a 404): two rules on one URL.
    const upperPage = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery("PAGE")}`, { maxRedirects: 0 });
    const upperPageHtml = await upperPage.text();
    const upperTouch = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery("TOUCH")}`, { maxRedirects: 0 });
    const upperTouchHtml = await upperTouch.text();
    const upperPopup = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery("POPUP")}`, { maxRedirects: 0 });
    const upperPopupHtml = await upperPopup.text();
    console.log(`[display] PAGE -> ${upperPage.status()} TOUCH -> ${upperTouch.status()} POPUP -> ${upperPopup.status()}`);

    expect(upperPage.status(), upperPageHtml).toBe(200);
    expect(upperPageHtml).toContain("isSaveConsentEnabled: true");
    expect(pageDataValue(upperPageHtml, "displayName")).toBe(CONSENT_CLIENT_ID);

    expect(upperTouch.status(), upperTouchHtml).toBe(200);
    expect(pageDataValue(upperTouchHtml, "isplayName")).toBe(CONSENT_CLIENT_ID);
    expect(pageDataValue(upperTouchHtml, "displayName"), "Touch displayName must be absent").toBeNull();

    expect(upperPopup.status(), upperPopupHtml).toBe(200);
    expect(upperPopupHtml).toContain("<title>Authorize popup</title>");
    expect(pageDataValue(upperPopupHtml, "clientId"), "Uppercase popup must include the consent model").toBe(CONSENT_CLIENT_ID);
  });

  test("display is validated AFTER authentication, not before", async () => {
    // ResourceOwnerSessionValidator sends an anonymous caller to login before any template is chosen, so an unknown
    // display gets the login 301 here, not the 400 of the test above. Were the display parsed first, an anonymous
    // caller could probe it by status code.
    const anonCtx = await apiRequest.newContext({ ignoreHTTPSErrors: true });
    try {
      const target = `${OAUTH2}/authorize?${authorizeQuery("bogus")}`;
      const response = await anonCtx.get(target, { maxRedirects: 0 });
      const location = response.headers()["location"];
      console.log(`[display] anonymous bogus -> ${response.status()}`);
      expect(response.status(), await response.text()).toBe(301);
      const login = new URL(location);
      expect(login.origin + login.pathname, "Login URL must match").toBe(`${OPENAM_BASE}/UI/Login`);
      expect([...login.searchParams.keys()], "Login parameter names must match").toEqual(["realm", "goto"]);
      expect(login.searchParams.get("realm"), "Login realm must match").toBe("/");
      // The unknown display survives into goto, so the 400 comes after the user returns.
      expect(login.searchParams.get("goto"), "Login goto must preserve the authorize URL").toBe(target);
    } finally {
      await anonCtx.dispose();
    }
  });

});

test.describe("?display= on /oauth2/device/user", () => {

  test("the device CONSENT page honours every display, exactly as /authorize does", async ({ request, page }) => {
    // DeviceCodeVerificationResource extends ConsentRequiredResource and renders the consent page through the same
    // OAuth2Representation, with the model built from the device code's record. A GET never acts on a user code (it
    // is redirected without it), so the page is reached by the POST the code-entry form makes.
    //
    // display travels in the query, so the post-back target carries it: through pageData.formTarget on the three XUI
    // templates, through the form's own action on wap. The user code is not in the target; the XUI templates carry it
    // as pageData.userCode.
    for (const [display, assertions] of [
      ["page", (html, target, userCode) => {
        expect(html.startsWith("<!DOCTYPE html>")).toBe(true);
        expect(pageDataValue(html, "displayName")).toBe(DEVICE_CLIENT_ID);
        expect(html.includes("isSaveConsentEnabled: true")).toBe(true);
        expect(pageDataValue(html, "formTarget") === `\\${target}`, "Consent target must preserve display").toBe(true);
        expect(pageDataValue(html, "userCode") === userCode, "Consent must preserve the user code").toBe(true);
      }],
      ["popup", (html, target, userCode, consentHtml) => {
        expect(html.startsWith("<!doctype html>")).toBe(true);
        expect(html.includes("<title>Authorize popup</title>")).toBe(true);
        expect(pageDataValue(consentHtml, "displayName")).toBe(DEVICE_CLIENT_ID);
        expect(pageDataValue(consentHtml, "formTarget") === `\\${target}`, "Consent target must preserve display").toBe(true);
        expect(pageDataValue(consentHtml, "userCode") === userCode, "Consent must preserve the user code").toBe(true);
      }],
      // The same master defect as on /authorize, on the second endpoint that renders the touch template. Target:
      // `displayName`.
      ["touch", (html, target, userCode) => {
        expect(pageDataValue(html, "isplayName")).toBe(DEVICE_CLIENT_ID);
        expect(pageDataValue(html, "displayName")).toBeNull();
        expect(pageDataValue(html, "formTarget") === `\\${target}`, "Consent target must preserve display").toBe(true);
        expect(pageDataValue(html, "userCode") === userCode, "Consent must preserve the user code").toBe(true);
      }],
      // wap renders server-side and has no pageData: the target is the form's own action.
      ["wap", (html, target) => {
        expect(html.startsWith('<?xml version="1.0"?>')).toBe(true);
        expect(html.includes(`<p>${DEVICE_CLIENT_ID}:</p>`)).toBe(true);
        expect(pageDataValue(html, "formTarget")).toBeNull();
        expect(html.includes(`<form action="${target}" method="post">`)).toBe(true);
        // Existing defect: neither the target nor a hidden field carries user_code.
        // Submitting WAP consent loses the device code. Target: a hidden user_code body field.
        expect(/<input\b[^>]*\bname=["']user_code["']/.test(html)).toBe(false);
      }],
    ]) {
      const userCode = await deviceCode(request);
      const response = await submitUserCode(userCode, display);
      const html = await response.text();
      console.log(`[display] device consent display=${display} -> ${response.status()}`
        + ` ct=${response.headers()["content-type"]} bytes=${html.length}`);
      expect(response.status(), display).toBe(200);
      expect(response.headers()["content-type"], display).toBe(HTML_TYPE);
      // poponload copies only #print. Parse inert HTML so wrapper handlers and embedded scripts never run.
      const consentHtml = display === "popup" ? await page.evaluate((source) => {
        const doc = new DOMParser().parseFromString(source, "text/html");
        return doc.getElementById("print")?.innerHTML ?? "";
      }, html) : html;
      assertions(html, `${CONTEXT_PATH}/oauth2/device/user?display=${display}`, userCode, consentHtml);
      if (display === "wap") {
        expect(html.includes(`<input type="hidden" name="client_id" value="${DEVICE_CLIENT_ID}"/>`)).toBe(true);
        expect(html.includes('<input type="hidden" name="response_type" value="code"/>')).toBe(true);
        expect(html.includes(`<input type="hidden" name="scope" value="${SCOPE}"/>`)).toBe(true);
        expect(/<input type="hidden" name="csrf" value="[^"]+"\/>/.test(html)).toBe(true);
      } else {
        expect(pageDataValue(consentHtml, "clientId")).toBe(DEVICE_CLIENT_ID);
        expect(pageDataValue(consentHtml, "responseType")).toBe("code");
        expect(pageDataValue(consentHtml, "scope")).toBe(SCOPE);
        expect(Boolean(pageDataValue(consentHtml, "csrf")), "Consent must include CSRF").toBe(true);
      }
    }
  });

  test("an unknown display on the device CONSENT page is a 400, as on /authorize", async ({ request }) => {
    const userCode = await deviceCode(request);
    const response = await submitUserCode(userCode, "bogus");
    const html = await response.text();
    console.log(`[display] device consent bogus -> ${response.status()}`);

    // The same master defect through the same ExceptionHandler, with the same target: 400 invalid_request.
    expect(response.status()).toBe(400);
    expect(response.headers()["content-type"]).toBe(HTML_TYPE);
    expect(response.headers()["location"] === undefined, "Unknown display must not redirect").toBe(true);
    expect(html.includes("<title>OAuth2 Error Page</title>")).toBe(true);
    expect(html.includes('message: "server_error"')).toBe(true);
  });

  test("the device CODE-ENTRY page ignores display entirely -- including a bogus one", async () => {
    // The counter-example that gives the two consent tests their meaning. DeviceCodeVerificationResource renders
    // templates/CodeVerificationForm.ftl by its literal path, with no display folder, so ?display= is never parsed
    // here: an unknown one is a 200, where the consent page one POST later is a 400.
    const rendered = [];
    for (const display of ["page", "popup", "touch", "wap", "bogus"]) {
      const response = await demoCtx.get(`${OAUTH2}/device/user?display=${display}`, { maxRedirects: 0 });
      const html = await response.text();
      console.log(`[display] device form display=${display} -> ${response.status()} bytes=${html.length}`);
      expect(response.status(), display).toBe(200);
      // CodeVerificationForm.ftl is an XUI shell: it carries no form of its own, only the model the main-device
      // bundle renders the code box from. That bundle name tells this page from the consent page.
      expect(html.includes("XUI/main-device"), display).toBe(true);
      expect(html.includes("<title>OAuth2 Authorization Server</title>"), display).toBe(true);
      // Every render mints its own csrf token, the one value that differs between them.
      expect(Boolean(pageDataValue(html, "csrf")), display).toBe(true);
      rendered.push(html.replace(/csrf: "[^"]*"/, 'csrf: ""'));
    }
    // Identical apart from the token: the folder was not consulted.
    for (const html of rendered) {
      expect(html === rendered[0], "Code-entry HTML must ignore display after CSRF masking").toBe(true);
    }
  });

});
