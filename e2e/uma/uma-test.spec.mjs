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
 * Protocol cover for the three /uma endpoints. /uma is served by Restlet (UMAServiceEndpointApplication under
 * the ForgeRockRest servlet). This spec pins its black-box contract before the area moves to CHF.
 *
 * Two error shapes coexist here on purpose, and each test pins its own:
 *   - the ACCESS TOKEN PROTECTION FILTER answers CREST-shaped: {code, reason, message}
 *   - the ENDPOINTS answer UMA-shaped:                          {error, error_description}
 * A flip may change a pinned value only as a listed divergence.
 */

import { test, expect } from "@playwright/test";
import { OPENAM_BASE, getAdminToken, PASSWORD, USERNAME } from "../common/openam-commons.mjs";
import {
  REALM, authorizationApiToken, deleteResourceSet, ensureProviderConfig, ensureResourceServerClient,
  ensureUmaProvider, protectionApiToken, registerResourceSet as postResourceSet, uniqueName,
  warmUpResourceSetStore,
} from "../common/oauth2-fixtures.mjs";

/** This spec's own client -- see the note in oauth2-fixtures.mjs about per-file ownership. */
const RS_CLIENT_ID = "test_client_rs_uma";

/** Protection API token: the resource server's bearer. */
let pat;
/** Authorization API token: the requesting party's bearer. */
let aat;

/** Resource sets registered by this run, torn down in afterAll. */
const registered = [];

/** Content-Type of every JSON answer under /uma on master. */
const JSON_TYPE = "application/json";

/** Pins the status, the Content-Type and the whole JSON body of an answer. */
async function expectJson(response, status, body) {
  const text = await response.text();
  expect(response.status(), text).toBe(status);
  expect(response.headers()["content-type"]).toBe(JSON_TYPE);
  expect(JSON.parse(text)).toEqual(body);
}

/** The CREST body the protection filter answers with; the message is InvalidTokenException's. */
const UNAUTHORIZED = {
  code: 401, reason: "Unauthorized",
  message: "The access token provided is expired, revoked, malformed, or invalid for other reasons.",
};

/** The grant types the provider advertises, sorted; both PAT and AAT fields return the same set. */
const PROVIDER_GRANT_TYPES = ["authorization_code", "client_credentials", "implicit", "password"];

/** The answer to a valid ticket from a requesting party that has not yet supplied claims. */
const NEED_INFO = {
  error: "need_info",
  error_description: "Additional information is required to determine whether the client is authorized to access "
    + "the requested resource.",
  requesting_party_claims: [{
    name: "id_token",
    issuer: [`${OPENAM_BASE}/oauth2`],
    claim_type: "urn:ietf:params:oauth:token-type:jwt",
    claim_token_format: ["http://openid.net/specs/openid-connect-core-1_0.html#HybridIDToken"],
  }],
};

/** Registers a resource set under a name no earlier run can have taken, and returns its id. */
async function registerResourceSet(request, label) {
  const { response } = await postResourceSet(request, pat, uniqueName(label));
  expect(response.status(), `resource_set registration failed: ${await response.text()}`).toBe(201);
  const id = (await response.json())._id;
  registered.push(id);
  return id;
}

test.beforeAll(async ({ request }) => {
  const adminToken = await getAdminToken(request);
  expect(adminToken, "amadmin login failed").toBeTruthy();
  await ensureProviderConfig(adminToken, request);
  await ensureUmaProvider(adminToken, request);
  await ensureResourceServerClient(adminToken, request, RS_CLIENT_ID);
  pat = await protectionApiToken(request, RS_CLIENT_ID, USERNAME, PASSWORD);
  aat = await authorizationApiToken(request, RS_CLIENT_ID, USERNAME, PASSWORD);
  expect(pat).toBeTruthy();
  expect(aat).toBeTruthy();
  await warmUpResourceSetStore(request, pat);
});

test.afterAll(async ({ request }) => {
  for (const id of registered) {
    await deleteResourceSet(request, pat, id);
  }
});

test.describe("/uma/.well-known/uma-configuration", () => {

  test("is public and advertises the UMA endpoints", async ({ request }) => {
    const response = await request.get(`${OPENAM_BASE}/uma/.well-known/uma-configuration`);
    const config = await response.json();
    console.log(`[uma] uma-configuration -> ${response.status()} keys=${Object.keys(config).length}`);

    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toBe(JSON_TYPE);
    const sorted = Object.fromEntries(Object.entries(config)
      .map(([key, value]) => [key, Array.isArray(value) ? [...value].sort() : value]));
    expect(sorted).toEqual({
      version: "1.0",
      issuer: `${OPENAM_BASE}/oauth2`,
      pat_profiles_supported: ["bearer"],
      aat_profiles_supported: ["bearer"],
      rpt_profiles_supported: ["bearer"],
      pat_grant_types_supported: PROVIDER_GRANT_TYPES,
      aat_grant_types_supported: PROVIDER_GRANT_TYPES,
      token_endpoint: `${OPENAM_BASE}/oauth2/access_token`,
      authorization_endpoint: `${OPENAM_BASE}/oauth2/authorize`,
      introspection_endpoint: `${OPENAM_BASE}/oauth2/introspect`,
      resource_set_registration_endpoint: `${OPENAM_BASE}/oauth2/resource_set`,
      permission_registration_endpoint: `${OPENAM_BASE}/uma/permission_request`,
      rpt_endpoint: `${OPENAM_BASE}/uma/authz_request`,
      dynamic_client_endpoint: `${OPENAM_BASE}/oauth2/connect/register`,
    });
  });

  test("answers 404 to the realm-prefixed path spelling", async ({ request }) => {
    const response = await request.get(`${OPENAM_BASE}/uma/realms/${REALM}/.well-known/uma-configuration`);
    // RestletRealmRouter takes `realms` as a sub-realm name, so this spelling does not resolve. The target is
    // 200 with the same configuration as the bare path; until then the 404 is pinned by status only.
    expect(response.status()).toBe(404);
  });
});

test.describe("/uma/permission_request", () => {

  test("issues a permission ticket for a registered resource set", async ({ request }) => {
    const resourceSetId = await registerResourceSet(request, "uma e2e album");

    const response = await request.post(`${OPENAM_BASE}/uma/permission_request`, {
      headers: { Authorization: `Bearer ${pat}`, "Content-Type": "application/json" },
      data: { resource_set_id: resourceSetId, scopes: ["read"] },
    });
    const body = await response.json();
    console.log(`[uma] permission_request -> ${response.status()} ${JSON.stringify(body)}`);

    expect(response.status()).toBe(201);
    expect(response.headers()["content-type"]).toBe(JSON_TYPE);
    expect(body.ticket).toBeTruthy();
    expect(Object.keys(body)).toEqual(["ticket"]);
  });

  test("rejects an unknown resource set id in the UMA error shape", async ({ request }) => {
    const response = await request.post(`${OPENAM_BASE}/uma/permission_request`, {
      headers: { Authorization: `Bearer ${pat}`, "Content-Type": "application/json" },
      data: { resource_set_id: "no-such-resource-set", scopes: ["read"] },
    });
    // Endpoint error => UMA shape: the protection filter must not turn it into {code, reason, message}.
    await expectJson(response, 400, {
      error: "invalid_resource_set_id",
      error_description: "Could not find Resource Set, no-such-resource-set",
    });
  });

  test("rejects a request with no resource_set_id", async ({ request }) => {
    const response = await request.post(`${OPENAM_BASE}/uma/permission_request`, {
      headers: { Authorization: `Bearer ${pat}`, "Content-Type": "application/json" },
      data: {},
    });
    await expectJson(response, 400, {
      error: "invalid_resource_set_id",
      error_description: "Invalid Permission Request. Missing required attribute, 'resource_set_id'.",
    });
  });

  test("rejects an unauthenticated permission request in the CREST shape", async ({ request }) => {
    const response = await request.post(`${OPENAM_BASE}/uma/permission_request`, {
      headers: { "Content-Type": "application/json" },
      data: { resource_set_id: "irrelevant", scopes: ["read"] },
    });
    // The protection filter's answer on master: CREST-shaped, with no challenge header. This is contract by
    // decision, with no target named.
    await expectJson(response, 401, UNAUTHORIZED);
    expect(response.headers()["www-authenticate"]).toBeUndefined();
  });

  test("answers 405 with Allow: POST to GET", async ({ request }) => {
    const response = await request.get(`${OPENAM_BASE}/uma/permission_request`, {
      headers: { Authorization: `Bearer ${pat}` },
    });
    console.log(`[uma] permission_request GET -> ${response.status()}`);
    // Route reached, verb unmapped: the framework's own 405. Its body is a framework page, so only the status
    // and the Allow header are pinned.
    expect(response.status()).toBe(405);
    expect(response.headers()["allow"]).toBe("POST");
  });
});

test.describe("/uma/authz_request", () => {

  test("a valid ticket enters claim gathering", async ({ request }) => {
    const resourceSetId = await registerResourceSet(request, "uma e2e rpt album");
    const ticketResponse = await request.post(`${OPENAM_BASE}/uma/permission_request`, {
      headers: { Authorization: `Bearer ${pat}`, "Content-Type": "application/json" },
      data: { resource_set_id: resourceSetId, scopes: ["read"] },
    });
    expect(ticketResponse.status()).toBe(201);
    const ticket = (await ticketResponse.json()).ticket;

    const response = await request.post(`${OPENAM_BASE}/uma/authz_request`, {
      headers: { Authorization: `Bearer ${aat}`, "Content-Type": "application/json" },
      data: { ticket },
    });
    // No policy grants the requesting party anything yet, so UMA asks for claims rather than issuing an RPT.
    await expectJson(response, 403, NEED_INFO);
  });

  test("rejects an unknown ticket in the UMA error shape", async ({ request }) => {
    const response = await request.post(`${OPENAM_BASE}/uma/authz_request`, {
      headers: { Authorization: `Bearer ${aat}`, "Content-Type": "application/json" },
      data: { ticket: "no-such-ticket" },
    });
    await expectJson(response, 400, {
      error: "invalid_ticket",
      error_description: "Unable to retrieve Permission Ticket",
    });
  });

  test("rejects a token without the uma_authorization scope", async ({ request }) => {
    const response = await request.post(`${OPENAM_BASE}/uma/authz_request`, {
      headers: { Authorization: `Bearer ${pat}`, "Content-Type": "application/json" },
      data: { ticket: "irrelevant" },
    });
    // Scope enforcement lives in the protection filter, so this is CREST-shaped like the 401 above.
    await expectJson(response, 403, {
      code: 403, reason: "Forbidden", message: "The resource requested requires scope: uma_authorization",
    });
  });

  test("rejects an unauthenticated authorization request in the CREST shape", async ({ request }) => {
    const response = await request.post(`${OPENAM_BASE}/uma/authz_request`, {
      headers: { "Content-Type": "application/json" },
      data: { ticket: "irrelevant" },
    });
    await expectJson(response, 401, UNAUTHORIZED);
    expect(response.headers()["www-authenticate"]).toBeUndefined();
  });
});
