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
 * Protocol cover for the OIDC half of /oauth2: discovery, the JWK set, id_token issuance, idtokeninfo and the
 * session endpoints checkSession and endSession. On master Restlet serves all of them (OAuth2RouterProvider under
 * the ForgeRockRest servlet), except the bare /oauth2/connect/checkSession, which web.xml maps to a JSP. This spec
 * pins their black-box contract before /oauth2 moves to CHF. A flip may change a pinned value only as a divergence
 * listed in its PR.
 */

import { createHash, createHmac } from "node:crypto";
import { test, expect, request as apiRequest } from "@playwright/test";
import { OPENAM_BASE, getAdminToken, getAuthToken, PASSWORD, USERNAME } from "../common/openam-commons.mjs";
import {
  CLIENT_SESSION_URI, OIDC_CLIENT_SECRET, REALM, authorizationCodeTokens, basicAuth, ensureNoSessionUriOidcClient,
  ensureOidcClient, ensureProviderConfig, sessionContext,
} from "../common/oauth2-fixtures.mjs";

/** This spec's own client -- spec files run in parallel and rewriting a shared client kills its tokens. */
const OIDC_CLIENT_ID = "test_client_oidc";

/** Same realm, no clientSessionURI: a client as the admin API creates it by default. */
const NO_SESSION_URI_CLIENT_ID = "test_client_oidc_nosessionuri";

const NONCE = "n-0S6_WzA2Mj";

/** Content-Type of the successful JSON answers: discovery, the JWK set and idtokeninfo. */
const JSON_TYPE = "application/json;charset=UTF-8";

/**
 * Content-Type of the error answers: the 400s of idtokeninfo, checkSession and endSession, and the unknown realm's
 * 404. Unlike the 200s, no charset.
 */
const ERROR_TYPE = "application/json";

/** Content-Type of both checkSession pages: the JSP on the bare path and the Restlet page under realms/. */
const HTML_TYPE = "text/html;charset=UTF-8";

/** Every member the discovery document has on master, sorted. */
const DISCOVERY_KEYS = [
  "acr_values_supported", "authorization_endpoint", "check_session_iframe", "claims_parameter_supported",
  "claims_supported", "device_authorization_endpoint", "end_session_endpoint",
  "id_token_encryption_alg_values_supported", "id_token_encryption_enc_values_supported",
  "id_token_signing_alg_values_supported", "introspection_endpoint", "issuer", "jwks_uri",
  "registration_endpoint", "response_types_supported", "revocation_endpoint", "scopes_supported",
  "subject_types_supported", "token_endpoint", "token_endpoint_auth_methods_supported", "userinfo_endpoint",
  "version",
];

/** Pins the status, the Content-Type and the whole JSON body of an error answer. */
async function expectJson(response, status, body) {
  const text = await response.text();
  expect(response.status(), text).toBe(status);
  expect(response.headers()["content-type"]).toBe(ERROR_TYPE);
  expect(JSON.parse(text)).toEqual(body);
}

/** The base64url left half of the SHA-256 digest: at_hash and c_hash for an HS256 id_token (OIDC Core 3.3.2.11). */
function leftHalfHash(value) {
  return createHash("sha256").update(value, "ascii").digest().subarray(0, 16).toString("base64url");
}

let adminToken;

// File level: discovery advertises the provider's scopes, so every test needs the provider configured.
test.beforeAll(async ({ request }) => {
  adminToken = await getAdminToken(request);
  expect(adminToken, "amadmin login failed").toBeTruthy();
  await ensureProviderConfig(adminToken, request);
});

/**
 * Logs demo in and runs the authorization_code flow for this spec's client. Returns a request context whose cookie
 * jar carries that session and the id_token issued under it. The caller disposes `ctx`.
 */
async function oidcSession(nonce) {
  const loginCtx = await apiRequest.newContext();
  let ssoToken;
  try {
    ssoToken = await getAuthToken(loginCtx, USERNAME, PASSWORD);
  } finally {
    await loginCtx.dispose();
  }
  expect(ssoToken, "demo login failed").toBeTruthy();
  const ctx = await sessionContext(apiRequest, ssoToken);
  try {
    const { tokens } = await authorizationCodeTokens(ctx, {
      clientId: OIDC_CLIENT_ID, clientSecret: OIDC_CLIENT_SECRET, scope: "openid", nonce,
    });
    return { ctx, idToken: tokens.id_token };
  } catch (e) {
    await ctx.dispose();
    throw e;
  }
}

test.describe("OIDC discovery", () => {

  test("/oauth2/.well-known/openid-configuration advertises this deployment's endpoints", async ({ request }) => {
    const response = await request.get(`${OPENAM_BASE}/oauth2/.well-known/openid-configuration`);
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toBe(JSON_TYPE);

    const config = await response.json();
    console.log(`[oidc] discovery issuer=${config.issuer} keys=${Object.keys(config).length}`);
    // Public discovery: no session, no client, no token.
    expect(Object.keys(config).sort()).toEqual(DISCOVERY_KEYS);
    expect(config.issuer).toBe(`${OPENAM_BASE}/oauth2`);
    expect(config.authorization_endpoint).toBe(`${OPENAM_BASE}/oauth2/authorize`);
    expect(config.token_endpoint).toBe(`${OPENAM_BASE}/oauth2/access_token`);
    expect(config.userinfo_endpoint).toBe(`${OPENAM_BASE}/oauth2/userinfo`);
    expect(config.jwks_uri).toBe(`${OPENAM_BASE}/oauth2/connect/jwk_uri`);
    expect(config.registration_endpoint).toBe(`${OPENAM_BASE}/oauth2/connect/register`);
    expect(config.end_session_endpoint).toBe(`${OPENAM_BASE}/oauth2/connect/endSession`);
    expect(config.check_session_iframe).toBe(`${OPENAM_BASE}/oauth2/connect/checkSession`);
    expect(config.introspection_endpoint).toBe(`${OPENAM_BASE}/oauth2/introspect`);
    expect(config.revocation_endpoint).toBe(`${OPENAM_BASE}/oauth2/token/revoke`);
    expect(config.device_authorization_endpoint).toBe(`${OPENAM_BASE}/oauth2/device/code`);
    expect(config.response_types_supported).toEqual(expect.arrayContaining(["code", "token"]));
    expect(config.subject_types_supported).toEqual(expect.arrayContaining(["public"]));
    expect(config.scopes_supported).toEqual(expect.arrayContaining(["openid", "profile"]));
    expect(config.id_token_signing_alg_values_supported).toEqual(expect.arrayContaining(["HS256", "RS256"]));
  });

  test("the realm-prefixed discovery path answers the same document", async ({ request }) => {
    // On master RestletRealmRouter resolves the realm-prefixed spelling. The CHF flip re-implements realm
    // routing, so both spellings have to keep answering the same document. Alone this cannot tell routing from
    // a router that drops the realm segment; the unknown-realm test below can.
    const bare = await request.get(`${OPENAM_BASE}/oauth2/.well-known/openid-configuration`);
    const modern = await request.get(`${OPENAM_BASE}/oauth2/realms/${REALM}/.well-known/openid-configuration`);
    for (const response of [bare, modern]) {
      expect(response.status()).toBe(200);
      expect(response.headers()["content-type"]).toBe(JSON_TYPE);
    }
    expect(await modern.json()).toEqual(await bare.json());
  });

  test("the discovery path of an unknown realm answers 404", async ({ request }) => {
    // A router that drops the realm segment would answer the root document here.
    const response = await request.get(
      `${OPENAM_BASE}/oauth2/realms/${REALM}/realms/no-such-realm/.well-known/openid-configuration`);
    await expectJson(response, 404, {
      code: 404, reason: "Not Found",
      message: "No mapping organization found for organization identifier: /no-such-realm",
    });
  });

  test("/oauth2/connect/jwk_uri publishes a usable JWK set", async ({ request }) => {
    const response = await request.get(`${OPENAM_BASE}/oauth2/connect/jwk_uri`);
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toBe(JSON_TYPE);

    const jwks = await response.json();
    console.log(`[oidc] jwk_uri keys=${jwks.keys?.length}`);
    expect(Object.keys(jwks)).toEqual(["keys"]);
    expect(Array.isArray(jwks.keys)).toBe(true);
    expect(jwks.keys.length).toBeGreaterThan(0);
    for (const key of jwks.keys) {
      // Enough for a relying party to select and build a key: an omission here breaks every RP.
      expect(key.kty).toBeTruthy();
      expect(key.kid).toBeTruthy();
      expect(key.use).toBeTruthy();
    }
    const rsa = jwks.keys.find((key) => key.kty === "RSA");
    expect(rsa, "the JWK set must publish the RSA signing key").toBeTruthy();
    expect(rsa.n).toBeTruthy();
    expect(rsa.e).toBeTruthy();
    // A JWK set must never carry private material.
    expect(rsa.d).toBeUndefined();
  });
});

test.describe("OIDC id_token issuance and idtokeninfo", () => {

  /** Cookie jar holding the demo session -- /oauth2/authorize authenticates by cookie, not by header. */
  let demoCtx;
  /** The authorization code the token response came from. */
  let code;
  let tokens;

  // Only this describe runs the authorize + token exchange, so a broken /oauth2/authorize or
  // /oauth2/access_token does not turn the discovery and jwk_uri tests red.
  test.beforeAll(async ({ request }) => {
    await ensureOidcClient(adminToken, request, OIDC_CLIENT_ID);

    let demoToken;
    const loginCtx = await apiRequest.newContext();
    try {
      demoToken = await getAuthToken(loginCtx, USERNAME, PASSWORD);
    } finally {
      await loginCtx.dispose();
    }
    expect(demoToken, "demo login failed").toBeTruthy();
    demoCtx = await sessionContext(apiRequest, demoToken);

    // One id_token for the whole describe: the issuance test and idtokeninfo both need a real one.
    ({ code, tokens } = await authorizationCodeTokens(demoCtx, {
      clientId: OIDC_CLIENT_ID, clientSecret: OIDC_CLIENT_SECRET, scope: "openid profile", nonce: NONCE,
    }));
  });

  test.afterAll(async () => {
    await demoCtx?.dispose();
  });

  test("the authorization_code flow issues an id_token bound to the request", async () => {
    console.log(`[oidc] token keys=${Object.keys(tokens).join(",")}`);
    expect(Object.keys(tokens).sort()).toEqual([
      "access_token", "expires_in", "id_token", "nonce", "refresh_token", "scope", "token_type",
    ]);
    expect(tokens.access_token).toBeTruthy();
    expect(tokens.refresh_token).toBeTruthy();
    expect(tokens.token_type).toBe("Bearer");
    expect(tokens.scope.split(" ").sort()).toEqual(["openid", "profile"]);
    expect(tokens.id_token).toBeTruthy();

    const [header, payload] = tokens.id_token.split(".");
    expect(tokens.id_token.split(".")).toHaveLength(3);
    const claims = JSON.parse(Buffer.from(payload, "base64url").toString("utf8"));
    expect(JSON.parse(Buffer.from(header, "base64url").toString("utf8")).alg).toBe("HS256");
    expect(claims.iss).toBe(`${OPENAM_BASE}/oauth2`);
    expect(claims.aud).toBe(OIDC_CLIENT_ID);
    expect(claims.sub).toBe(USERNAME);
    expect(claims.nonce).toBe(NONCE);
    expect(claims.realm).toBe("/");
    // at_hash/c_hash bind the id_token to the access token and the code it came from.
    expect(claims.at_hash).toBe(leftHalfHash(tokens.access_token));
    expect(claims.c_hash).toBe(leftHalfHash(code));
    expect(claims.exp).toBeGreaterThan(claims.iat);
  });

  test("/oauth2/idtokeninfo returns the claim set for a valid id_token", async ({ request }) => {
    const response = await request.post(`${OPENAM_BASE}/oauth2/idtokeninfo`, {
      headers: { Authorization: basicAuth(OIDC_CLIENT_ID, OIDC_CLIENT_SECRET) },
      form: { id_token: tokens.id_token },
    });
    const text = await response.text();
    expect(response.status(), text).toBe(200);
    expect(response.headers()["content-type"]).toBe(JSON_TYPE);
    const claims = JSON.parse(text);
    console.log(`[oidc] idtokeninfo -> ${response.status()} sub=${claims.sub}`);

    expect(claims.sub).toBe(USERNAME);
    expect(claims.aud).toBe(OIDC_CLIENT_ID);
    expect(claims.iss).toBe(`${OPENAM_BASE}/oauth2`);
    expect(claims.nonce).toBe(NONCE);
    expect(claims.tokenName).toBe("id_token");
  });

  test("/oauth2/idtokeninfo rejects a missing id_token", async ({ request }) => {
    const response = await request.post(`${OPENAM_BASE}/oauth2/idtokeninfo`, {
      headers: { Authorization: basicAuth(OIDC_CLIENT_ID, OIDC_CLIENT_SECRET) },
      form: {},
    });
    await expectJson(response, 400, { error: "bad_request", error_description: "no id_token in request" });
  });

  test("/oauth2/idtokeninfo rejects a malformed id_token", async ({ request }) => {
    const response = await request.post(`${OPENAM_BASE}/oauth2/idtokeninfo`, {
      headers: { Authorization: basicAuth(OIDC_CLIENT_ID, OIDC_CLIENT_SECRET) },
      form: { id_token: "not.a-jwt" },
    });
    // The parse failure surfaces as a 400, never as a 500.
    await expectJson(response, 400, {
      error: "bad_request", error_description: "invalid id_token: not right number of dots, 2",
    });
  });

  // idtokeninfo authenticates the client of an HMAC-signed id_token (idTokenInfoClientAuthenticationEnabled
  // defaults to true). Master answers a failed client authentication with the same 400 and description as an
  // id_token whose audience is no registered client.
  test("/oauth2/idtokeninfo rejects a wrong client secret", async ({ request }) => {
    const response = await request.post(`${OPENAM_BASE}/oauth2/idtokeninfo`, {
      headers: { Authorization: basicAuth(OIDC_CLIENT_ID, "wrong-secret") },
      form: { id_token: tokens.id_token },
    });
    await expectJson(response, 400, {
      error: "invalid_client", error_description: "no registered client matches audience of id_token",
    });
  });

  test("/oauth2/idtokeninfo rejects a caller without client credentials", async ({ request }) => {
    const response = await request.post(`${OPENAM_BASE}/oauth2/idtokeninfo`, {
      form: { id_token: tokens.id_token },
    });
    await expectJson(response, 400, {
      error: "invalid_client", error_description: "no registered client matches audience of id_token",
    });
  });

  test("/oauth2/idtokeninfo rejects an id_token with a forged signature", async ({ request }) => {
    // Flip the FIRST signature character: the low bits of the last one are padding, so changing it can leave
    // the decoded signature unchanged.
    const [header, payload, signature] = tokens.id_token.split(".");
    const forged = `${header}.${payload}.${signature[0] === "A" ? "B" : "A"}${signature.slice(1)}`;
    const response = await request.post(`${OPENAM_BASE}/oauth2/idtokeninfo`, {
      headers: { Authorization: basicAuth(OIDC_CLIENT_ID, OIDC_CLIENT_SECRET) },
      form: { id_token: forged },
    });
    await expectJson(response, 400, { error: "bad_request", error_description: "invalid id_token" });
  });
});

test.describe("OIDC checkSession", () => {

  const CHECK_SESSION = `${OPENAM_BASE}/oauth2/realms/${REALM}/connect/checkSession`;

  const b64 = (o) => Buffer.from(JSON.stringify(o)).toString("base64url");

  /** A JWT whose signature verifies against no key: enough for a token checkSession must not accept. */
  const jwt = (claims) => `${b64({ alg: "HS256", typ: "JWT" })}.${b64(claims)}.c2ln`;

  /** An HS256 JWT signed with the given secret, so it passes checkSession's signature check. */
  const signedJwt = (claims, secret) => {
    const signingInput = `${b64({ alg: "HS256", typ: "JWT" })}.${b64(claims)}`;
    return `${signingInput}.${createHmac("sha256", secret).update(signingInput).digest("base64url")}`;
  };

  /** checkSession reads the id_token from the Referer's query string, the way the RP's iframe passes it. */
  const withIdToken = (idToken) => ({ headers: { Referer: `http://rp.invalid/page?id_token=${idToken}` } });

  let session;

  test.beforeAll(async ({ request }) => {
    await ensureOidcClient(adminToken, request, OIDC_CLIENT_ID);
    await ensureNoSessionUriOidcClient(adminToken, request, NO_SESSION_URI_CLIENT_ID);
    session = await oidcSession("check-session");
  });

  test.afterAll(async () => {
    await session?.ctx.dispose();
  });

  test("/oauth2/connect/checkSession serves the session-management iframe", async ({ request }) => {
    const response = await request.get(`${OPENAM_BASE}/oauth2/connect/checkSession`);
    const html = await response.text();
    console.log(`[oidc] checkSession -> ${response.status()} ct=${response.headers()["content-type"]} bytes=${html.length}`);
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toBe(HTML_TYPE);
    // The RP-facing contract of the iframe: it listens for postMessage and answers with a session state.
    expect(html).toContain("addEventListener");
    expect(html).toContain("receiveMessage");
    expect(html).toContain("session_state");
  });

  test("the bare path is the JSP and the realm-prefixed path is the Restlet page", async ({ request }) => {
    const jsp = await request.get(`${OPENAM_BASE}/oauth2/connect/checkSession`);
    const page = await request.get(CHECK_SESSION);
    const jspHtml = await jsp.text();
    const pageHtml = await page.text();
    console.log(`[oidc] checkSession jsp=${jsp.status()} page=${page.status()}`);

    for (const [name, response] of [["jsp", jsp], ["page", page]]) {
      expect(response.status(), name).toBe(200);
      expect(response.headers()["content-type"], name).toBe(HTML_TYPE);
      // The RP frames this page: SecurityHeaders excludes /connect/checkSession, so neither answer carries
      // X-Frame-Options. Neither carries a cache header.
      expect(response.headers()["x-frame-options"], name).toBeUndefined();
      expect(response.headers()["cache-control"], name).toBeUndefined();
      expect(response.headers()["pragma"], name).toBeUndefined();
    }

    // The JSP loads sha256.js by a relative path, the Restlet page by the absolute base URL.
    expect(jspHtml).toContain('<script src="../../js/sha256.js">');
    expect(pageHtml).toContain(`<script src="${OPENAM_BASE}/js/sha256.js">`);

    // Defect, pinned as master answers it: the JSP quotes the flag, so `!validSession` is false for the string
    // "false" and the page reads the session cookie even when the session is not valid. Target, as the Restlet
    // page already emits it: `var validSession = false;`.
    expect(jspHtml).toContain('var validSession = "false";');
    expect(pageHtml).toContain("var validSession = false;");
  });

  test("checkSession answers GET and POST identically", async ({ request }) => {
    const get = await request.get(CHECK_SESSION);
    const post = await request.post(CHECK_SESSION, { form: {} });
    console.log(`[oidc] checkSession GET=${get.status()} POST=${post.status()}`);
    expect(get.status()).toBe(200);
    expect(post.status()).toBe(200);
    expect(post.headers()["content-type"]).toBe(HTML_TYPE);
    expect(await post.text()).toBe(await get.text());
  });

  test("an id_token in the Referer supplies the client's origin", async ({ request }) => {
    const response = await request.get(CHECK_SESSION, withIdToken(session.idToken));
    const html = await response.text();
    console.log(`[oidc] checkSession valid id_token -> ${response.status()}`);
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toBe(HTML_TYPE);
    // The client's registered clientSessionURI becomes the page's postMessage origin check.
    expect(html).toContain(`var clientURI = "${CLIENT_SESSION_URI}";`);
  });

  test("an unusable id_token in the Referer leaves the origin empty", async ({ request }) => {
    // Every case renders the page with an empty origin and an invalid session, so the page accepts no message.
    const cases = {
      "no aud claim": jwt({ iss: "x", sub: USERNAME }),
      "unknown aud": jwt({ aud: "no_such_client", realm: "/" }),
      "malformed jwt": "not.a-jwt",
    };
    for (const [name, idToken] of Object.entries(cases)) {
      const response = await request.get(CHECK_SESSION, withIdToken(idToken));
      const html = await response.text();
      console.log(`[oidc] checkSession ${name} -> ${response.status()}`);
      expect(response.status(), name).toBe(200);
      expect(response.headers()["content-type"], name).toBe(HTML_TYPE);
      expect(html, name).toContain('var clientURI = "";');
      expect(html, name).toContain("var validSession = false;");
    }
  });

  test("a client without a clientSessionURI leaves the origin empty", async ({ request }) => {
    // Signed locally with the client's secret, the id_token passes the signature check and reaches the client's
    // clientSessionURI, which the admin API leaves empty by default. The answer is the same page as for an
    // unusable id_token.
    const idToken = signedJwt({ aud: NO_SESSION_URI_CLIENT_ID, realm: "/", sub: USERNAME }, OIDC_CLIENT_SECRET);
    const response = await request.get(CHECK_SESSION, withIdToken(idToken));
    const html = await response.text();
    console.log(`[oidc] checkSession client without clientSessionURI -> ${response.status()}`);
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toBe(HTML_TYPE);
    expect(html).toContain('var clientURI = "";');
    expect(html).toContain("var validSession = false;");
  });

  test("checkSession answers a display other than page with a 400", async ({ request }) => {
    // Defect, pinned as master answers it: check_session_iframe has no display parameter (OpenID Connect Session
    // Management 1.0), yet the Restlet page resolves one like an authorization page and fails. Target: 200 with
    // the same page as without the parameter. error_description is not pinned: it is Restlet's status text.
    for (const display of ["popup", "touch", "wap", "bogus"]) {
      const response = await request.get(`${CHECK_SESSION}?display=${display}`);
      const text = await response.text();
      console.log(`[oidc] checkSession display=${display} -> ${response.status()} ${text}`);
      expect(response.status(), display).toBe(400);
      expect(response.headers()["content-type"], display).toBe(ERROR_TYPE);
      const body = JSON.parse(text);
      expect(Object.keys(body).sort(), display).toEqual(["error", "error_description"]);
      expect(body.error, display).toBe("server_error");
    }

    // page is the only display that renders, and it is the default.
    const page = await request.get(`${CHECK_SESSION}?display=page`);
    expect(page.status()).toBe(200);
    expect(page.headers()["content-type"]).toBe(HTML_TYPE);
    expect(await page.text()).toBe(await (await request.get(CHECK_SESSION)).text());
  });
});
