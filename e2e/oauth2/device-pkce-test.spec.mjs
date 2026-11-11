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
 * Copyright 2026 Aravind Yedmala.
 */

import { test, expect } from "@playwright/test";
import { OPENAM_BASE, getAdminToken, getAuthToken, USERNAME, PASSWORD } from "../common/openam-commons.mjs";
import { REALM, SCOPE, ensureDevicePkceProvider, ensureDevicePkceClient } from "../common/oauth2-fixtures.mjs";

// HTTP coverage for GHSA-h93p-35jm-rj83, part of #1223. Unlike the Java resource tests, these
// assertions survive removal of Restlet and exercise challenge persistence through the token store.
const CLIENT_ID = "test_device_pkce_app";
const DEVICE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";
const ENDPOINT = `${OPENAM_BASE}/oauth2/realms/${REALM}`;
// RFC 7636 Appendix B's S256 test vector; plain uses the same valid 43-character verifier.
const VERIFIER = "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk";
const WRONG_VERIFIER = "a".repeat(43);
const METHODS = [
  { name: "S256", challenge: "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM", method: "S256" },
  { name: "plain", challenge: VERIFIER, method: "plain" },
  { name: "omitted (defaults to plain)", challenge: VERIFIER },
];

async function issueCode(request, { challenge, method } = {}) {
  const form = { client_id: CLIENT_ID, response_type: "device_code", scope: SCOPE };
  if (challenge !== undefined) form.code_challenge = challenge;
  if (method !== undefined) form.code_challenge_method = method;
  return request.post(`${ENDPOINT}/device/code`, { form, headers: { Accept: "application/json" } });
}

async function newCode(request, parameters) {
  const response = await issueCode(request, parameters);
  expect(response.status(), `device code issuance: ${await response.text()}`).toBe(200);
  const body = await response.json();
  expect(body.device_code).toEqual(expect.any(String));
  expect(body.device_code.length).toBeGreaterThan(0);
  expect(body.user_code).toEqual(expect.any(String));
  expect(body.user_code.length).toBeGreaterThan(0);
  return body;
}

async function approveCode(request, userCode, sessionToken) {
  // This is the documented non-browser submission, including the session-bound CSRF value.
  const response = await request.post(`${ENDPOINT}/device/user`, {
    form: { user_code: userCode, csrf: sessionToken },
    headers: { iPlanetDirectoryPro: sessionToken },
    maxRedirects: 0,
  });
  expect(response.status(), `device approval: ${await response.text()}`).toBe(200);
}

function redeem(request, deviceCode, verifier) {
  const form = { grant_type: DEVICE_GRANT, client_id: CLIENT_ID, device_code: deviceCode };
  if (verifier !== undefined) form.code_verifier = verifier;
  return request.post(`${ENDPOINT}/access_token`, { form, headers: { Accept: "application/json" } });
}

async function expectOAuthError(response, error) {
  expect(response.status(), `expected ${error}: ${await response.text()}`).toBe(400);
  const body = await response.json();
  expect(body.error).toBe(error);
  expect(body).not.toHaveProperty("access_token");
  expect(body).not.toHaveProperty("refresh_token");
  expect(body).not.toHaveProperty("id_token");
}

async function expectTokensForUser(request, deviceCode, verifier) {
  const response = await redeem(request, deviceCode, verifier);
  expect(response.status(), `token redemption: ${await response.text()}`).toBe(200);
  const body = await response.json();
  expect(body.access_token).toEqual(expect.any(String));
  expect(body.access_token.length).toBeGreaterThan(0);
  const userInfo = await request.get(`${ENDPOINT}/userinfo`, {
    headers: { Authorization: `Bearer ${body.access_token}`, Accept: "application/json" },
  });
  expect(userInfo.status(), `userinfo: ${await userInfo.text()}`).toBe(200);
  expect((await userInfo.json()).sub).toBe(USERNAME);
}

test.describe("Device authorization PKCE over HTTP", () => {
  let sessionToken;

  test.beforeAll(async ({ request }) => {
    const adminToken = await getAdminToken(request);
    expect(adminToken, "administrator authentication is required; security coverage must not silently skip").toBeTruthy();
    await ensureDevicePkceProvider(adminToken, request);
    await ensureDevicePkceClient(adminToken, request, CLIENT_ID);
    sessionToken = await getAuthToken(request, USERNAME, PASSWORD);
    expect(sessionToken, "resource-owner authentication is required").toBeTruthy();
  });

  for (const method of ["bogus", "s256", "PLAIN", ""]) {
    test(`issuance rejects challenge method ${JSON.stringify(method)}`, async ({ request }) => {
      await expectOAuthError(await issueCode(request, { ...METHODS[0], method }), "invalid_request");
    });
  }

  for (const parameters of METHODS) {
    test(`${parameters.name}: correct verifier completes the device flow`, async ({ request }) => {
      const code = await newCode(request, parameters);
      await approveCode(request, code.user_code, sessionToken);
      await expectTokensForUser(request, code.device_code, VERIFIER);
    });

    for (const attempt of [
      { name: "missing", verifier: undefined, error: "invalid_request" },
      { name: "wrong", verifier: WRONG_VERIFIER, error: "invalid_grant" },
    ]) {
      test(`${parameters.name}: ${attempt.name} verifier cannot redeem or consume an approved code`,
        async ({ request }) => {
          const code = await newCode(request, parameters);
          await approveCode(request, code.user_code, sessionToken);
          await expectOAuthError(await redeem(request, code.device_code, attempt.verifier), attempt.error);
          // A rejection alone could pass if the flow were broken for everyone. Redeeming the SAME code
          // with the right verifier proves both that approval worked and that the attack did not consume it.
          await expectTokensForUser(request, code.device_code, VERIFIER);
        });
    }
  }

  for (const attempt of [
    { name: "missing", verifier: undefined, error: "invalid_request" },
    { name: "wrong", verifier: WRONG_VERIFIER, error: "invalid_grant" },
  ]) {
    test(`S256: ${attempt.name} verifier is rejected before user approval`, async ({ request }) => {
      const code = await newCode(request, METHODS[0]);
      await expectOAuthError(await redeem(request, code.device_code, attempt.verifier), attempt.error);
      await approveCode(request, code.user_code, sessionToken);
      await expectTokensForUser(request, code.device_code, VERIFIER);
    });
  }

  test("a device flow without a challenge still works without a verifier", async ({ request }) => {
    const code = await newCode(request);
    await approveCode(request, code.user_code, sessionToken);
    await expectTokensForUser(request, code.device_code);
  });
});
