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
 * The ?display= surface of the consent page on /oauth2/authorize.
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
    throw new Error(`Failed to write ${clientId}: ${written.status()}`);
  }
}

test.beforeAll(async ({ request }) => {
  adminToken = await getAdminToken(request);
  expect(typeof adminToken === "string", "Admin login must return an SSO token").toBe(true);
  expect(Boolean(adminToken)).toBe(true);
  await ensureProviderConfig(adminToken, request);
  await writeClient(request, CONSENT_CLIENT_ID, {
    "com.forgerock.openam.oauth2provider.grantTypes": ["[0]=authorization_code"],
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
    throw new Error(`Failed to read ${USERNAME}: ${demo.status()}`);
  }
  demoName = (await demo.json()).cn?.[0];
  if (!demoName) {
    throw new Error(`${USERNAME} has no cn`);
  }

  const loginCtx = await apiRequest.newContext();
  try {
    demoToken = await getAuthToken(loginCtx, USERNAME, PASSWORD);
    expect(typeof demoToken === "string", "Resource-owner login must return an SSO token").toBe(true);
    expect(Boolean(demoToken)).toBe(true);
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

test.describe("?display= on /oauth2/authorize", () => {

  test("display=page renders the page/ consent template", async () => {
    const response = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery("page")}`, { maxRedirects: 0 });
    const html = await response.text();
    console.log(`[display] page -> ${response.status()} ct=${response.headers()["content-type"]} bytes=${html.length}`);

    expect(response.status()).toBe(200);
    // Every display is served as text/html, WML included: the template representation's media type does not vary
    // by folder.
    expect(response.headers()["content-type"]).toBe(HTML_TYPE);
    // AuthorizeEndpointFilter stamps every /authorize answer, the consent page included.
    expect(response.headers()["cache-control"]).toBe("no-store");
    expect(response.headers()["pragma"]).toBe("no-cache");

    expect(html.startsWith("<!DOCTYPE html>")).toBe(true);
    // ConsentRequiredResource builds the model from the request's attributes and query plus its own keys. These are
    // the ones this template reads.
    expect(pageDataValue(html, "displayName")).toBe(CONSENT_CLIENT_ID);
    expect(pageDataValue(html, "clientId")).toBe(CONSENT_CLIENT_ID);
    expect(pageDataValue(html, "userName")).toBe(demoName);
    expect(pageDataValue(html, "responseType")).toBe("code");
    expect(pageDataValue(html, "redirectUri")).toBe(REDIRECT_URI);
    expect(pageDataValue(html, "scope")).toBe(SCOPE);
    expect(pageDataValue(html, "state")).toBe("display");
    expect(Boolean(pageDataValue(html, "csrf"))).toBe(true);
    // page/authorize.ftl is the only consent template with the saveConsentEnabled guard, which is why the popup and
    // touch tests assert its absence: the cheapest proof that a different file was rendered.
    expect(html.includes("isSaveConsentEnabled: true")).toBe(true);
    // The post-back target is the request's path and its raw query, display included, so a consent POST stays on the
    // same template. ?js_string escapes the leading slash.
    expect(pageDataValue(html, "formTarget")).toBe(`\\${CONTEXT_PATH}/oauth2/authorize?${authorizeQuery("page")}`);
  });

  test("an EMPTY display= is the page/ template, not a 400", async () => {
    // `?display=` is a reachable URL and reads back as an empty string. OAuth2Representation treats empty like
    // absent and renders page/; Enum.valueOf("") would be the 400 of the unknown-display test.
    const response = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery("")}`, { maxRedirects: 0 });
    const html = await response.text();
    console.log(`[display] empty -> ${response.status()} bytes=${html.length}`);

    expect(response.status()).toBe(200);
    expect(html.startsWith("<!DOCTYPE html>")).toBe(true);
    expect(pageDataValue(html, "displayName")).toBe(CONSENT_CLIENT_ID);
    expect(html.includes("isSaveConsentEnabled: true")).toBe(true);
  });

  test("display=popup composes the wrapper AROUND the consent page", async () => {
    const response = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery("popup")}`, { maxRedirects: 0 });
    const html = await response.text();
    console.log(`[display] popup -> ${response.status()} bytes=${html.length}`);

    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toBe(HTML_TYPE);
    // popup.ftl's own doctype is lowercase where every other template's is uppercase: the cheapest discriminator for
    // "the wrapper was rendered, not the bare page".
    expect(html.startsWith("<!doctype html>")).toBe(true);
    expect(html.includes("<title>Authorize popup</title>")).toBe(true);
    expect(html.includes("function poponload()")).toBe(true);
    // The composition: OAuth2Representation renders popup/authorize.ftl first and interpolates it, unescaped, into
    // popup.ftl's htmlCode, so the inner page's pageData is readable here. A wrapper rendered without its page would
    // leave this div empty, and the two assertions below would catch it.
    expect(html.includes('<div id="print" style="visibility: hidden">')).toBe(true);
    expect(pageDataValue(html, "displayName")).toBe(CONSENT_CLIENT_ID);
    expect(pageDataValue(html, "clientId")).toBe(CONSENT_CLIENT_ID);
    // popup/authorize.ftl has no saveConsentEnabled guard, so the embedded page is popup/'s and not page/'s.
    expect(html.includes("isSaveConsentEnabled")).toBe(false);
  });

  test("display=touch renders a BLANK client name -- the isplayName typo", async () => {
    const response = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery("touch")}`, { maxRedirects: 0 });
    const html = await response.text();
    console.log(`[display] touch -> ${response.status()} bytes=${html.length}`
      + ` isplayName=${JSON.stringify(pageDataValue(html, "isplayName"))}`
      + ` displayName=${JSON.stringify(pageDataValue(html, "displayName"))}`);

    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toBe(HTML_TYPE);

    // Master defect: templates/touch/authorize.ftl writes the client name under `isplayName`, so the key the XUI
    // reads, `displayName`, is missing and the consent heading renders empty. Target: `displayName`, as on page/ and
    // popup/. The model is intact: the value is on the page under the wrong key.
    expect(pageDataValue(html, "isplayName")).toBe(CONSENT_CLIENT_ID);
    expect(pageDataValue(html, "displayName")).toBeNull();

    // Everything else on the touch page is intact, which makes the missing name a template typo rather than a
    // broken model.
    expect(pageDataValue(html, "clientId")).toBe(CONSENT_CLIENT_ID);
    expect(pageDataValue(html, "userName")).toBe(demoName);
    expect(Boolean(pageDataValue(html, "csrf"))).toBe(true);
    expect(html.includes("isSaveConsentEnabled")).toBe(false);
  });

  test("display=wap is WML served as text/html, and DOES render the client name", async () => {
    const response = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery("wap")}`, { maxRedirects: 0 });
    const html = await response.text();
    console.log(`[display] wap -> ${response.status()} ct=${response.headers()["content-type"]} bytes=${html.length}`);

    expect(response.status()).toBe(200);
    // A WML document goes out as text/html: the template representation's media type does not vary by folder.
    expect(response.headers()["content-type"]).toBe(HTML_TYPE);
    expect(html.startsWith('<?xml version="1.0"?>')).toBe(true);
    expect(html.includes('<!DOCTYPE wml PUBLIC "-//WAPFORUM//DTD WML 1.2//EN"')).toBe(true);
    expect(html.includes("<wml>")).toBe(true);
    // No pageData: wap/ renders server-side instead of handing a model to the XUI, so it proves the model reaches the
    // template, not only the browser.
    expect(html.includes("pageData")).toBe(false);
    expect(html.includes(`<p>${CONSENT_CLIENT_ID}:</p>`)).toBe(true);
    // display_scope, the raw scope descriptions only this template reads. ConsentRequiredResource writes it after
    // copying the query into the model, so a client cannot supply its own (CVE-2026-62280).
    expect(html.includes(`<b>${SCOPE}</b>`)).toBe(true);
    // A real HTML form with the CSRF token and the two decision buttons, posted back to the same URL.
    expect(html.includes(`<form action="${CONTEXT_PATH}/oauth2/authorize?`)).toBe(true);
    expect(/<input type="hidden" name="csrf" value="[^"]+"\/>/.test(html)).toBe(true);
    expect(html.includes('<input type="submit" name="decision" class="button gray" value="Allow"/>')).toBe(true);
  });

  test("an UNKNOWN display is a non-redirecting 400 server_error page", async () => {
    const response = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery("bogus")}`, { maxRedirects: 0 });
    const html = await response.text();
    console.log(`[display] bogus -> ${response.status()} bytes=${html.length}`);

    expect(response.status()).toBe(400);
    // Rendered in place, not redirected to the client.
    expect(response.headers()["location"] === undefined, "Unknown display must not redirect").toBe(true);
    expect(response.headers()["content-type"]).toBe(HTML_TYPE);
    expect(html.includes("<title>OAuth2 Error Page</title>")).toBe(true);
    // Master defect. Enum.valueOf's IllegalArgumentException is thrown while the consent page renders, inside
    // AuthorizeResource's ResourceOwnerConsentRequired handler and so past its invalid_request mapping, and
    // ExceptionHandler turns it into ServerException's 400 server_error. Target: 400 invalid_request (RFC 6749
    // §4.1.2.1, an invalid parameter value). The description is Restlet's status text and is not pinned.
    expect(html.includes('message: "server_error"')).toBe(true);
  });

  test("the display name is case-INSENSITIVE, unlike route matching", async () => {
    // OAuth2Representation upper-cases the value before Enum.valueOf, so PAGE/page and TOUCH/touch pick the same
    // folder. Route matching is case-sensitive (/oauth2/Tokeninfo is a 404): two rules on one URL.
    const upperPage = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery("PAGE")}`, { maxRedirects: 0 });
    const upperPageHtml = await upperPage.text();
    const upperTouch = await demoCtx.get(`${OAUTH2}/authorize?${authorizeQuery("TOUCH")}`, { maxRedirects: 0 });
    const upperTouchHtml = await upperTouch.text();
    console.log(`[display] PAGE -> ${upperPage.status()} TOUCH -> ${upperTouch.status()}`);

    expect(upperPage.status()).toBe(200);
    expect(upperPageHtml.includes("isSaveConsentEnabled: true")).toBe(true);
    expect(pageDataValue(upperPageHtml, "displayName")).toBe(CONSENT_CLIENT_ID);

    expect(upperTouch.status()).toBe(200);
    expect(pageDataValue(upperTouchHtml, "isplayName")).toBe(CONSENT_CLIENT_ID);
    expect(pageDataValue(upperTouchHtml, "displayName")).toBeNull();
  });

  test("display is validated AFTER authentication, not before", async () => {
    // ResourceOwnerSessionValidator sends an anonymous caller to login before any template is chosen, so an unknown
    // display gets the login 301 here, not the 400 of the test above. Were the display parsed first, an anonymous
    // caller could probe it by status code.
    const anonCtx = await apiRequest.newContext();
    try {
      const target = `${OAUTH2}/authorize?${authorizeQuery("bogus")}`;
      const response = await anonCtx.get(target, { maxRedirects: 0 });
      const location = response.headers()["location"];
      console.log(`[display] anonymous bogus -> ${response.status()}`);
      expect(response.status()).toBe(301);
      const login = new URL(location);
      expect(login.origin + login.pathname).toBe(`${OPENAM_BASE}/UI/Login`);
      expect([...login.searchParams.keys()]).toEqual(["realm", "goto"]);
      expect(login.searchParams.get("realm")).toBe("/");
      // The unknown display survives into goto, so the 400 comes after the user returns.
      expect(login.searchParams.get("goto") === target, "Login goto must preserve the authorize URL").toBe(true);
    } finally {
      await anonCtx.dispose();
    }
  });

});
