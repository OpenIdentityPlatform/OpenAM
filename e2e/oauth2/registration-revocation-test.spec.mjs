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
import { getAdminToken, OPENAM_BASE } from "../common/openam-commons.mjs";
import { basicAuth, ensureClient, ensureOAuth2Provider, unreportedRequest } from "../common/oauth2-fixtures.mjs";

const OAUTH2 = OPENAM_BASE + "/oauth2";
const REGISTRATION = OAUTH2 + "/connect/register";
const REVOCATION = OAUTH2 + "/token/revoke";
const AGENTS = OPENAM_BASE + "/json/realms/root/realm-config/agents/OAuth2Client";
const CLIENT_ATTR = "com.forgerock.openam.oauth2provider.";
const CLIENT_NAME = CLIENT_ATTR + "clientName";
const REDIRECT_URIS = ["http://dynamic.invalid/cb"];
const INVALID_TOKEN = "The access token provided is expired, revoked, malformed, or invalid for other reasons.";

function nonempty(value) {
  return typeof value === "string" && value.trim().length > 0;
}

function bearer(token) {
  return { Authorization: "Bearer " + token };
}

function expectJsonResponse(response, status, operation) {
  expect(response.status(), operation + " HTTP status").toBe(status);
  const mediaType = response.headers()["content-type"]?.split(";")[0].trim().toLowerCase();
  expect(mediaType === "application/json", operation + " content type is JSON").toBe(true);
}

function expectOAuthError(body, error, description, operation) {
  expect(body?.error === error, operation + " error is " + error).toBe(true);
  expect(body?.error_description === description, operation + " reason is " + description).toBe(true);
}

function clientNames(record) {
  const names = record?.[CLIENT_NAME];
  return [names].flat().filter(nonempty).map((name) => name.replace(/^\[\d+\]=/, ""));
}

function isClient(record, id, name) {
  return record?._id === id && clientNames(record).includes(name);
}

// Shared fixtures include response bodies in failures. Keep only the operation and HTTP statuses.
async function setup(request, operation, run) {
  const statuses = [];
  const safeRequest = Object.fromEntries(["get", "post", "put"].map((method) => [
    method, async (url, options) => {
      const response = await request[method](url, options);
      statuses.push(response.status());
      return {
        ...response, text: async () => "",
        json: async () => {
          if (!response.ok()) throw new Error("Setup request failed");
          return response.json();
        },
      };
    },
  ]));
  try {
    return await run(safeRequest);
  } catch {
    const status = statuses.length ? " (HTTP " + statuses.join(", ") + ")" : "";
    throw new Error(operation + " failed" + status);
  }
}

function tokenInfo(request, token) {
  return request.get(OAUTH2 + "/tokeninfo", { headers: { Accept: "application/json" }, params: { access_token: token } });
}

async function expectValidToken(request, token, id, operation) {
  const response = await tokenInfo(request, token);
  expectJsonResponse(response, 200, operation);
  const body = await response.json();
  expect(body?.client_id === id, operation + " identifies the issuing client").toBe(true);
  expect(body?.access_token === token, operation + " returns the submitted access token").toBe(true);
}

async function expectInvalidToken(response, operation) {
  expectJsonResponse(response, 401, operation);
  expectOAuthError(await response.json(), "invalid_token", INVALID_TOKEN, operation);
}

function revoke(request, token, { id, secret }) {
  return request.post(REVOCATION, { form: { token, client_id: id, client_secret: secret } });
}

async function expectRevocation(response, operation) {
  expectJsonResponse(response, 200, operation);
  const body = await response.json();
  const emptyObject = body && typeof body === "object" && !Array.isArray(body) && Object.keys(body).length === 0;
  expect(emptyObject === true, operation + " returns an empty JSON object").toBe(true);
}

async function listClients(request, headers) {
  const response = await request.get(AGENTS, { headers, params: { _queryFilter: "true" } });
  expectJsonResponse(response, 200, "OAuth2 client collection");
  const result = (await response.json())?.result;
  expect(Array.isArray(result) && result.every((client) => nonempty(client?._id)),
    "OAuth2 client collection contains a result array with client IDs").toBe(true);
  return result;
}

function agentUrl(id) {
  return AGENTS + "/" + encodeURIComponent(id);
}

async function readForCleanup(request, url, headers, retryNotFound) {
  const read = () => request.get(url, { headers });
  const response = await read().catch(() => null);
  const status = response?.status();
  return !response || (retryNotFound && status === 404) || status === 408 || status === 429 || status >= 500
    ? read() : response;
}

const test = base.extend({
  fixtures: async ({ ignoreHTTPSErrors }, use) => {
    const prefix = "b5f-" + randomUUID();
    const clients = new Map();
    const names = new Set();
    const confirmed = new Set();
    const tokens = new Map();
    const contexts = [];

    function newContext() {
      const request = unreportedRequest.newContext({ ignoreHTTPSErrors, maxRedirects: 0 });
      contexts.push(request);
      return request;
    }

    let client;
    const clientRequest = () => client ??= newContext();
    let adminRequest;
    let admin;
    async function administrator() {
      const request = adminRequest ??= newContext();
      admin ??= await setup(request, "Authenticate administrator", async (safeRequest) => {
        const token = await getAdminToken(safeRequest);
        if (!nonempty(token)) {
          throw new Error("Administrator authentication returned no session");
        }
        const headers = { iPlanetDirectoryPro: token, "Accept-API-Version": "protocol=2.0,resource=1.0" };
        return { request, headers, token };
      });
      return admin;
    }

    function trackClient(id, name) {
      names.add(name);
      if (nonempty(id) && !clients.has(id)) {
        clients.set(id, name);
      }
    }

    function trackToken(token, credentials) {
      if (nonempty(token) && !tokens.has(token)) {
        tokens.set(token, credentials);
      }
    }

    async function ensureProvider() {
      const { request, token } = await administrator();
      await setup(request, "Ensure root OAuth2 provider", (safeRequest) => ensureOAuth2Provider(token, safeRequest));
    }

    async function provisionClient(id) {
      const name = id + "-name";
      const secret = randomUUID();
      trackClient(id, name);
      const { request, token } = await administrator();
      await setup(request, "Write OAuth2 client", (safeRequest) => ensureClient(token, safeRequest, id, {
        userpassword: secret,
        [CLIENT_NAME]: ["[0]=" + name],
        [CLIENT_ATTR + "clientType"]: "Confidential",
        [CLIENT_ATTR + "grantTypes"]: ["[0]=client_credentials"],
        [CLIENT_ATTR + "tokenEndPointAuthMethod"]: "client_secret_basic",
        [CLIENT_ATTR + "scopes"]: ["[0]=profile"],
        sunIdentityServerDeviceStatus: "Active",
      }));
      confirmed.add(id);
      return { id, name, secret };
    }

    async function issueToken(credentials) {
      const { id, secret } = credentials;
      const response = await clientRequest().post(OAUTH2 + "/access_token", {
        headers: { Authorization: basicAuth(id, secret) },
        form: { grant_type: "client_credentials", scope: "profile" },
      });
      const token = (await response.json())?.access_token;
      trackToken(token, credentials);
      expectJsonResponse(response, 200, "Issue client credentials token");
      expect(nonempty(token), "Issued access token is a nonempty string").toBe(true);
      return token;
    }

    async function register(request, name, options) {
      trackClient(options.data.client_id, name);
      const response = await request.post(REGISTRATION, options);
      const body = await response.json();
      trackClient(body?.client_id, name);
      trackToken(body?.registration_access_token, {
        id: body?.client_id, secret: body?.client_secret, registrationName: name,
      });
      return { response, body };
    }

    async function readClient(id, name) {
      const { request, headers } = await administrator();
      const response = await request.get(agentUrl(id), { headers });
      expectJsonResponse(response, 200, "Read OAuth2 client");
      expect(isClient(await response.json(), id, name), "Direct read identifies the expected OAuth2 client").toBe(true);
      confirmed.add(id);
    }

    try {
      await use({
        prefix, newContext, clientRequest, administrator, ensureProvider, provisionClient, issueToken,
        register, readClient, forgetToken: (token) => tokens.delete(token),
      });
    } finally {
      const failures = [];
      async function attempt(operation) {
        try {
          return await operation();
        } catch (error) {
          failures.push(error.message);
        }
      }

      const ownedClients = new Set();
      const session = names.size ? await attempt(administrator) : undefined;
      if (session) {
        const { request, headers } = session;
        const collection = await attempt(() => listClients(request, headers));
        for (const record of collection ?? []) {
          const name = clientNames(record).find((value) => names.has(value));
          if (name) {
            trackClient(record._id, name);
          }
        }
        for (const [id, name] of clients) {
          const listed = collection?.find((record) => record._id === id);
          const knownOwner = listed ? isClient(listed, id, name) : confirmed.has(id);
          const owned = await attempt(async () => {
            const response = await readForCleanup(request, agentUrl(id), headers, Boolean(listed) || knownOwner);
            if (response.status() === 404) {
              const current = listed ? await attempt(() => listClients(request, headers)) : collection;
              const record = current?.find((entry) => entry._id === id);
              if (current && !record) {
                return false;
              }
              failures.push("Cleanup GET 404 could not be confirmed by the OAuth2 client collection");
              return record ? isClient(record, id, name) : knownOwner;
            }
            if (response.status() === 200 && !isClient(await response.json(), id, name)) {
              failures.push("Cleanup found an OAuth2 client with an unexpected ID or name");
              return false;
            }
            expectJsonResponse(response, 200, "Read OAuth2 client before cleanup");
            return true;
          });
          if (owned ?? knownOwner) {
            ownedClients.add(id);
          }
        }
      }

      // Revoke while the token's issuing client still exists.
      for (const [token, credentials] of tokens) {
        const { id, secret, registrationName } = credentials;
        if (registrationName && (!ownedClients.has(id) || clients.get(id) !== registrationName || !nonempty(secret))) {
          failures.push("Cannot confirm the registration token belongs to a test client");
          continue;
        }
        const needsRevocation = await attempt(async () => {
          const response = await tokenInfo(clientRequest(), token);
          if (response.status() !== 200) {
            await expectInvalidToken(response, "Read token before cleanup");
            return false;
          }
          const body = await response.json();
          if (body?.client_id !== id || body?.access_token !== token) {
            failures.push("Cleanup token lookup returned an unexpected client or access token");
            return false;
          }
          expectJsonResponse(response, 200, "Read token before cleanup");
          return true;
        });
        if (needsRevocation === false) {
          continue;
        }
        await attempt(async () => {
          await expectRevocation(await revoke(clientRequest(), token, credentials), "Revoke token during cleanup");
        });
        await attempt(async () => {
          await expectInvalidToken(await tokenInfo(clientRequest(), token), "Verify token cleanup");
        });
      }

      if (session) {
        const { request, headers } = session;
        for (const id of ownedClients) {
          const url = agentUrl(id);
          await attempt(async () => {
            const response = await request.delete(url, { headers });
            expect(response.ok(), "Delete OAuth2 client HTTP " + response.status()).toBe(true);
          });
          await attempt(async () => {
            expect((await request.get(url, { headers })).status(), "Read OAuth2 client after cleanup").toBe(404);
          });
        }
        await attempt(async () => {
          const remaining = await listClients(request, headers);
          expect(remaining.some((record) => ownedClients.has(record._id) ||
            clientNames(record).some((name) => names.has(name))), "No test OAuth2 clients remain after cleanup").toBe(false);
        });
      }
      for (const request of contexts) {
        await attempt(() => request.dispose());
      }
      if (failures.length) {
        throw new Error("OAuth2 fixture cleanup failed:\n" + failures.join("\n"));
      }
    }
  },
});

test.use({ trace: "off", screenshot: "off" });

test("registers a client and reads it with the registration access token", async ({ fixtures }) => {
  await fixtures.ensureProvider();
  const issuer = await fixtures.provisionClient(fixtures.prefix + "-issuer");
  const accessToken = await fixtures.issueToken(issuer);
  const request = fixtures.clientRequest();
  await expectValidToken(request, accessToken, issuer.id, "Validate registration bearer token");

  const name = fixtures.prefix + "-registered";
  const { response, body } = await fixtures.register(request, name, {
    headers: bearer(accessToken), data: { client_name: name, redirect_uris: REDIRECT_URIS },
  });
  expectJsonResponse(response, 201, "Register OAuth2 client");
  const { client_id: id, client_secret: secret, registration_access_token: token } = body;
  expect(nonempty(id), "Registration returns a client ID").toBe(true);
  expect(nonempty(secret), "Registration returns a client secret").toBe(true);
  expect(nonempty(token), "Registration returns a registration access token").toBe(true);
  expect(body.client_name === name, "Registration returns the supplied client name").toBe(true);
  expect(JSON.stringify(body.redirect_uris) === JSON.stringify(REDIRECT_URIS), "Registration returns the supplied redirect URIs").toBe(true);
  const uri = REGISTRATION + "?client_id=" + encodeURIComponent(id);
  expect(body.registration_client_uri === uri, "Registration returns its management URI").toBe(true);

  const headers = bearer(token);
  async function readRegistration(operation) {
    const stored = await request.get(uri, { headers });
    expectJsonResponse(stored, 200, operation);
    const metadata = await stored.json();
    expect(metadata?.client_id === id && metadata?.client_name === name, operation + " identifies the registered client").toBe(true);
    expect(JSON.stringify(metadata?.redirect_uris) === JSON.stringify(REDIRECT_URIS), operation + " returns the stored redirect URIs").toBe(true);
    expect(Object.hasOwn(metadata, "registration_access_token"), operation + " omits the registration access token").toBe(false);
  }
  await readRegistration("Read registered client");
  // Closed registration can reuse the supplied bearer as its management token.
  const otherToken = await fixtures.issueToken(issuer);
  expect(otherToken !== token, "The other access token differs from the registration access token").toBe(true);
  await expectValidToken(request, otherToken, issuer.id, "Validate the other bearer token");
  await expectInvalidToken(await request.get(uri, { headers: bearer(otherToken) }), "Read with a different bearer token");
  // Restlet implements GET on the management URI. The unsupported DELETE is a framework 405.
  expect((await request.delete(uri, { headers })).status(), "Delete registration HTTP status").toBe(405);
  await readRegistration("Read registered client after rejected DELETE");
});

test("rejects registration without an access token", async ({ fixtures }) => {
  try {
    await fixtures.ensureProvider();
  } catch (error) {
    // The unauthenticated POST must reach the endpoint even when provider preparation fails.
    expect.soft(false, "Prepare root provider: " + error.message).toBe(true);
  }

  const id = fixtures.prefix + "-rejected";
  const name = id + "-name";
  const { response, body } = await fixtures.register(fixtures.newContext(), name, {
    data: { client_id: id, client_name: name, redirect_uris: REDIRECT_URIS },
  });
  expectJsonResponse(response, 400, "Register without an access token");
  expectOAuthError(body, "access_denied", "Access Token not valid", "Register without an access token");

  const control = await fixtures.provisionClient(fixtures.prefix + "-control");
  const { request, headers } = await fixtures.administrator();
  const collection = await listClients(request, headers);
  expect(collection.some((record) => isClient(record, control.id, control.name)),
    "OAuth2 client collection contains the control client").toBe(true);
  expect(collection.some((record) => record._id === id || clientNames(record).includes(name)),
    "Rejected registration stored no client with the submitted ID or name").toBe(false);
  await fixtures.readClient(control.id, control.name);
  expect((await request.get(agentUrl(id), { headers })).status(), "Direct read of the rejected ID").toBe(404);
});

test("revokes an access token so it stops validating", async ({ fixtures }) => {
  await fixtures.ensureProvider();
  const client = await fixtures.provisionClient(fixtures.prefix + "-revoke");
  const token = await fixtures.issueToken(client);
  const control = await fixtures.issueToken(client);
  expect(token !== control, "The target and control access tokens differ").toBe(true);
  const request = fixtures.clientRequest();
  await expectValidToken(request, token, client.id, "Validate target token before revocation");
  await expectValidToken(request, control, client.id, "Validate control token before revocation");

  await expectRevocation(await revoke(request, token, client), "Revoke target access token");
  await expectInvalidToken(await tokenInfo(request, token), "Validate revoked access token");
  fixtures.forgetToken(token);
  await expectValidToken(request, control, client.id, "Validate control token after revocation");
});

test("rejects revocation without the token parameter", async ({ fixtures }) => {
  const response = await fixtures.newContext().post(REVOCATION, {
    form: { client_id: fixtures.prefix + "-missing-token", client_secret: randomUUID() },
  });
  expectJsonResponse(response, 400, "Revoke without the token parameter");
  expectOAuthError(await response.json(), "invalid_request", "Missing parameter: token", "Revoke without the token parameter");
});

test("rejects revocation with an incorrect client secret", async ({ fixtures }) => {
  await fixtures.ensureProvider();
  const client = await fixtures.provisionClient(fixtures.prefix + "-wrong-secret");
  const token = await fixtures.issueToken(client);
  const request = fixtures.clientRequest();
  await expectValidToken(request, token, client.id, "Validate token before rejected revocation");

  const response = await revoke(request, token, { id: client.id, secret: randomUUID() });
  expectJsonResponse(response, 400, "Revoke with an incorrect client secret");
  expectOAuthError(await response.json(), "invalid_client", "Client authentication failed", "Revoke with an incorrect client secret");
  expect(response.headers()["www-authenticate"] === undefined, "Form authentication failure has no WWW-Authenticate header").toBe(true);
  await expectValidToken(request, token, client.id, "Validate token after rejected revocation");
});
