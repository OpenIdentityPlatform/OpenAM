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
 * Protocol cover for the OIDC half of /oauth2: discovery, the JWK set, id_token issuance and idtokeninfo.
 * On master Restlet serves all of them (OAuth2RouterProvider under the ForgeRockRest servlet). This spec pins
 * their black-box contract before /oauth2 moves to CHF. A flip may change a pinned value only as a divergence
 * listed in its PR.
 */

import { createHash } from "node:crypto";
import { test, expect, request as apiRequest } from "@playwright/test";
import { OPENAM_BASE, getAdminToken, getAuthToken, PASSWORD, USERNAME } from "../common/openam-commons.mjs";
import {
  OIDC_CLIENT_SECRET, REALM, authorizationCodeTokens, basicAuth, ensureOidcClient, ensureProviderConfig,
  sessionContext,
} from "../common/oauth2-fixtures.mjs";

/** This spec's own client -- spec files run in parallel and rewriting a shared client kills its tokens. */
const OIDC_CLIENT_ID = "test_client_oidc";

const NONCE = "n-0S6_WzA2Mj";

/** Content-Type of the successful JSON answers: discovery, the JWK set and idtokeninfo. */
const JSON_TYPE = "application/json;charset=UTF-8";

/** Content-Type of the error answers: idtokeninfo's 400s, the unknown realm's 404. Unlike the 200s, no charset. */
const ERROR_TYPE = "application/json";

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
