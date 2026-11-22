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

import { randomBytes } from "node:crypto";
import { request as httpRequest } from "node:http";
import { request as httpsRequest } from "node:https";
import { Worker } from "node:worker_threads";
import { test, expect, request as apiRequest } from "@playwright/test";
import { OPENAM_BASE, getAdminToken, getAuthToken, PASSWORD, USERNAME } from "../common/openam-commons.mjs";
import { basicAuth, ensureClient, pkce, sessionContext } from "../common/oauth2-fixtures.mjs";

// Traces can capture bearer tokens in request headers, query strings and form bodies.
test.use({ trace: "off", screenshot: "off" });

const REALM = "root";
const CLIENT_ID = "test_client_app";
const TOKEN_CACHE_CLIENT_ID = "test_client_oauth2_b4b";
const TOKEN_CACHE_CLIENT_SECRET = "b4b-secret-0123456789-0123456789-0123456789";
// A client that does not imply consent, so the resource owner's decision must be posted explicitly. This is
// the situation issue #1080 reports: a non-browser client cannot obtain a token to post with the decision.
const CONSENT_CLIENT_ID = "test_consent_app";
const CONSENT_STATE = "consent-state";
const SCOPE="profile"
const REDIRECT_URI="http://app.invalid/cb"
/**
 * Ensures the OAuth2 service exists in the OpenAM instance.
 * Creates it with default configuration if it doesn't exist.
 *
 * Playwright runs spec files in parallel workers, and the device flow spec provisions the same service. On a
 * fresh instance both hooks see a 404 and both post a create, so losing that race has to be survivable: the
 * loser re-reads instead of failing, which would otherwise take every test in this file down with it.
 */
async function ensureOAuth2ServiceExists(adminToken, request) {
  const path = `${OPENAM_BASE}/json/realms/${REALM}/realm-config/services/oauth-oidc`;
  const headers = {
    "iPlanetDirectoryPro": adminToken,
    "Content-Type": "application/json",
    "Accept-API-Version": "protocol=1.0,resource=1.0",
  };

  let lastFailure = "no attempt was made";

  for (let attempt = 0; attempt < 3; attempt++) {
    const response = await request.get(path, { headers });

    if (response.ok()) {
      console.log("OAuth2 service already exists");
      return;
    }

    if (response.status() !== 404) {
      throw new Error(`Failed to check OAuth2 service: ${response.status()} ${response.statusText()}`);
    }

    // OAuth2 service doesn't exist, create it
    const createResponse = await request.post(`${path}?_action=create`, {
      headers,
      data: {
        advancedOAuth2Config: {
          clientsCanSkipConsent: true,
          supportedScopes: [SCOPE],
          defaultScopes: [SCOPE],
        },
      },
    });

    if (createResponse.ok()) {
      console.log("OAuth2 service created successfully");
      return;
    }
    // Another worker got there first - read it back rather than reporting a failure. Anything else (an
    // expired admin token, a config store error) has to survive to the message below.
    lastFailure = `${createResponse.status()} ${createResponse.statusText()} ${await createResponse.text()}`;
  }

  throw new Error(`Failed to create OAuth2 service: ${lastFailure}`);
}

/**
 * Ensures an OAuth2 client application exists in the OpenAM instance.
 * Creates it with default configuration if it doesn't exist.
 */

async function ensureOAuth2ClientExists(adminToken, request, clientId = CLIENT_ID, isConsentImplied = true) {
  const response = await request.get(
    `${OPENAM_BASE}/json/realms/${REALM}/realm-config/agents/OAuth2Client/${clientId}`,
    {
      method: "GET",
      headers: {
        "iPlanetDirectoryPro": adminToken,
        "Accept-API-Version": "protocol=2.0,resource=1.0",
      },
    }
  );

  if (response.status() === 404) {
    // Client doesn't exist, create it
    const createResponse = await request.put(
      `${OPENAM_BASE}/json/realms/${REALM}/realm-config/agents/OAuth2Client/${clientId}`,
      {
        headers: {
          "iPlanetDirectoryPro": adminToken,
          "Content-Type": "application/json",
          "Accept-API-Version": "protocol=2.0,resource=1.0",
        },
        data: {
          "com.forgerock.openam.oauth2provider.clientType": "Public",
          "com.forgerock.openam.oauth2provider.redirectionURIs": [`[0]=${REDIRECT_URI}`],
          "com.forgerock.openam.oauth2provider.scopes": [`[0]=${SCOPE}`],
          "com.forgerock.openam.oauth2provider.defaultScopes": [`[0]=${SCOPE}`],
          "com.forgerock.openam.oauth2provider.grantTypes": ["[0]=authorization_code"],
          "com.forgerock.openam.oauth2provider.responseTypes": ["[0]=code"],
          "com.forgerock.openam.oauth2provider.tokenEndPointAuthMethod": "none",
          "isConsentImplied": isConsentImplied,
          "sunIdentityServerDeviceStatus": "Active"
        },
      }
    );

    if (!createResponse.ok()) {
      throw new Error(
        `Failed to create OAuth2 client: ${createResponse.statusText}`
      );
    }
    console.log(`OAuth2 client "${clientId}" created successfully`);
  } else if (!response.ok()) {
    throw new Error(
      `Failed to check OAuth2 client: ${response.statusText}`
    );
  } else {
    console.log(`OAuth2 client "${clientId}" already exists`);
  }
}

test.beforeAll(async ({ request }) => {
  const adminToken = await getAdminToken(request)

  expect(Boolean(adminToken), "Admin authentication must return a session token").toBe(true);
  await ensureOAuth2ServiceExists(adminToken, request);
  await ensureOAuth2ClientExists(adminToken, request);
  await ensureOAuth2ClientExists(adminToken, request, CONSENT_CLIENT_ID, false);
});

async function ensureTokenCacheClientExists(request) {
  const adminToken = await getAdminToken(request);
  expect(Boolean(adminToken), "Admin authentication must return a session token").toBe(true);
  await ensureClient(adminToken, request, TOKEN_CACHE_CLIENT_ID, {
    "userpassword": TOKEN_CACHE_CLIENT_SECRET,
    "com.forgerock.openam.oauth2provider.clientType": "Confidential",
    "com.forgerock.openam.oauth2provider.scopes": [`[0]=${SCOPE}`],
    "com.forgerock.openam.oauth2provider.defaultScopes": [`[0]=${SCOPE}`],
    "com.forgerock.openam.oauth2provider.grantTypes": ["[0]=client_credentials"],
    "com.forgerock.openam.oauth2provider.tokenEndPointAuthMethod": "client_secret_basic",
    "sunIdentityServerDeviceStatus": "Active",
  });
}

let accessToken;

// PKCE is mandatory for these clients: they are public and authenticate with "none", so OpenAM rejects an
// authorization request without code_challenge before it reaches consent handling.
function generateVerifier(length = 64) {
    const array = new Uint32Array(length);
    crypto.getRandomValues(array);
    const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';
    return Array.from(array, x => chars[x % chars.length]).join('');
}

async function generateChallenge(verifier) {
    const encoder = new TextEncoder();
    const data = encoder.encode(verifier);
    const hash = await crypto.subtle.digest('SHA-256', data);

    return btoa(String.fromCharCode(...new Uint8Array(hash)))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')
        .replace(/=+$/, '');
}

test.describe("OAuth Service test", () => {
  test("Should receive an auth code and exchange it to access token", async ({ request }) => {

      const demoToken = await getAuthToken(request, USERNAME, PASSWORD);

      const state = "random-state";

      const verifier = generateVerifier();

      const challenge = await generateChallenge(verifier);

      const codeResponse = await request.get(
        `${OPENAM_BASE}/oauth2/authorize`, {
          headers: {
            "iPlanetDirectoryPro": demoToken,
          },
          params: {
            response_type: "code",
            client_id: CLIENT_ID,
            redirect_uri: REDIRECT_URI,
            scope: SCOPE,
            state: state,
            code_challenge: challenge,
            code_challenge_method: "S256"
          },
          maxRedirects: 0
        }
      )

      expect(codeResponse.status()).toBe(302);
      
      const headers = codeResponse.headers();

      const location = headers['location'];

      const locationURL = new URL(location);

      const code = locationURL.searchParams.get("code");

      expect(code).toBeTruthy()
    

      const response = await request.post(`${OPENAM_BASE}/oauth2/access_token`, {
        form: {
          grant_type: 'authorization_code',
          client_id: CLIENT_ID,
          code: code,
          redirect_uri: REDIRECT_URI,
          code_verifier: verifier,
          state: state
        },
        headers: {
          'Accept': 'application/json'
        }
      });

      expect(response.ok()).toBeTruthy();

      const tokens = await response.json();

      expect(tokens).toHaveProperty('access_token');

      accessToken = tokens.access_token

      console.log(`Got access token: ${accessToken}`);
  });

  test("Get user info with access token", async ({ request }) => {
    const response = await request.get(`${OPENAM_BASE}/oauth2/userinfo`, {
        headers: {
          'Authorization': `Bearer ${accessToken}`,
          'Accept': 'application/json'
        }
      });

      expect(response.ok()).toBeTruthy();
      expect(response.status()).toBe(200);

      // 4. Получение и вывод тела ответа
      const userInfo = await response.json();
      expect(userInfo.sub).toBe('demo');
      console.log('User Info Claims:', userInfo);

  });

});

test.describe("OAuth2 userinfo and tokeninfo transport", () => {
  const TRANSPORT_CLIENT_ID = "test_client_oauth2_b4a";
  let transportAccessToken;
  let tokenLifetime;

  test.beforeAll(async ({ request }) => {
    const adminToken = await getAdminToken(request);
    expect(Boolean(adminToken), "Admin authentication must return a session token").toBe(true);
    await ensureOAuth2ClientExists(adminToken, request, TRANSPORT_CLIENT_ID);

    const userToken = await getAuthToken(request, USERNAME, PASSWORD);
    expect(Boolean(userToken), "User authentication must return a session token").toBe(true);
    const context = await sessionContext(apiRequest, userToken);
    try {
      const { verifier, challenge } = await pkce();
      const state = "b4a-transport-state";
      const authorize = await context.get(`${OPENAM_BASE}/oauth2/authorize`, {
        params: {
          response_type: "code",
          client_id: TRANSPORT_CLIENT_ID,
          redirect_uri: REDIRECT_URI,
          scope: SCOPE,
          state,
          code_challenge: challenge,
          code_challenge_method: "S256",
        },
        maxRedirects: 0,
      });
      expect(authorize.status()).toBe(302);
      const location = new URL(authorize.headers()["location"], OPENAM_BASE);
      expect(location.searchParams.has("error")).toBe(false);
      expect(location.searchParams.get("state")).toBe(state);
      const code = location.searchParams.get("code");
      expect(Boolean(code), "Authorization must return a code").toBe(true);

      // This public client authenticates with client_id and PKCE, without Basic authentication.
      const response = await context.post(`${OPENAM_BASE}/oauth2/access_token`, {
        headers: { Accept: "application/json" },
        form: {
          grant_type: "authorization_code",
          client_id: TRANSPORT_CLIENT_ID,
          code,
          redirect_uri: REDIRECT_URI,
          code_verifier: verifier,
        },
      });
      expect(response.status()).toBe(200);
      const tokens = await response.json();
      expect(typeof tokens.access_token).toBe("string");
      expect(Boolean(tokens.access_token), "Token exchange must return an access token").toBe(true);
      expect(tokens.token_type).toBe("Bearer");
      expect(Number.isInteger(tokens.expires_in)).toBe(true);
      expect(tokens.expires_in).toBeGreaterThan(0);
      transportAccessToken = tokens.access_token;
      tokenLifetime = tokens.expires_in;
    } finally {
      await context.dispose();
    }
  });

  test("Get user info with access token in POST form body", async ({ request }) => {
    const response = await request.post(`${OPENAM_BASE}/oauth2/userinfo`, {
      maxRedirects: 0,
      form: { access_token: transportAccessToken },
      headers: { Accept: "application/json" },
    });
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toBe("application/json;charset=UTF-8");
    const userInfo = await response.json();
    const bearerResponse = await request.get(`${OPENAM_BASE}/oauth2/userinfo`, {
      maxRedirects: 0,
      headers: { Authorization: `Bearer ${transportAccessToken}`, Accept: "application/json" },
    });
    expect(bearerResponse.status()).toBe(200);
    expect(userInfo).toEqual(await bearerResponse.json());
    expect(userInfo.sub).toBe(USERNAME);
  });

  test("Reject user info request with token in both header and form body", async ({ request }) => {
    const response = await request.post(`${OPENAM_BASE}/oauth2/userinfo`, {
      maxRedirects: 0,
      form: { access_token: transportAccessToken },
      headers: { Authorization: `Bearer ${transportAccessToken}`, Accept: "application/json" },
    });
    // Current Restlet defect: RFC 6750 section 3.1 requires invalid_request for multiple token transports.
    // Target: https://www.rfc-editor.org/rfc/rfc6750.html#section-3.1
    expect(response.status()).toBe(400);
    expect(response.headers()["content-type"]).toBe("application/json");
    expect(await response.json()).toEqual({
      error: "server_error",
      error_description: "Access Token cannot be provided in both form and header",
    });
  });

  test("Get token info with access token in query parameter", async ({ request }) => {
    const response = await request.get(`${OPENAM_BASE}/oauth2/tokeninfo`, {
      maxRedirects: 0,
      params: { access_token: transportAccessToken },
      headers: { Accept: "application/json" },
    });
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toBe("application/json");
    const tokenInfo = await response.json();
    expect(Object.keys(tokenInfo).sort()).toEqual([
      "access_token", "client_id", "expires_in", "grant_type", "profile", "realm", "scope", "token_type",
    ]);
    expect(tokenInfo.access_token === transportAccessToken, "Token info must identify the issued token").toBe(true);
    expect(tokenInfo.grant_type).toBe("authorization_code");
    expect(tokenInfo.client_id).toBe(TRANSPORT_CLIENT_ID);
    expect(tokenInfo.scope).toEqual([SCOPE]);
    expect(tokenInfo.profile).toBe("");
    expect(tokenInfo.realm).toBe("/");
    expect(tokenInfo.token_type).toBe("Bearer");
    expect(Number.isInteger(tokenInfo.expires_in)).toBe(true);
    expect(tokenInfo.expires_in).toBeGreaterThan(0);
    expect(tokenInfo.expires_in).toBeLessThanOrEqual(tokenLifetime);
  });

  test("Get token info with access token in header", async ({ request }) => {
    const response = await request.get(`${OPENAM_BASE}/oauth2/tokeninfo`, {
      maxRedirects: 0,
      headers: { Authorization: `Bearer ${transportAccessToken}`, Accept: "application/json" },
    });
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toBe("application/json");
    const tokenInfo = await response.json();
    expect(Object.keys(tokenInfo).sort()).toEqual([
      "access_token", "client_id", "expires_in", "grant_type", "profile", "realm", "scope", "token_type",
    ]);
    expect(tokenInfo.access_token === transportAccessToken, "Token info must identify the issued token").toBe(true);
    expect(tokenInfo.grant_type).toBe("authorization_code");
    expect(tokenInfo.client_id).toBe(TRANSPORT_CLIENT_ID);
    expect(tokenInfo.scope).toEqual([SCOPE]);
    expect(tokenInfo.profile).toBe("");
    expect(tokenInfo.realm).toBe("/");
    expect(tokenInfo.token_type).toBe("Bearer");
    expect(Number.isInteger(tokenInfo.expires_in)).toBe(true);
    expect(tokenInfo.expires_in).toBeGreaterThan(0);
    expect(tokenInfo.expires_in).toBeLessThanOrEqual(tokenLifetime);
  });

  test("Reject token info request with token in both header and query parameter", async ({ request }) => {
    const response = await request.get(`${OPENAM_BASE}/oauth2/tokeninfo`, {
      maxRedirects: 0,
      params: { access_token: transportAccessToken },
      headers: { Authorization: `Bearer ${transportAccessToken}`, Accept: "application/json" },
    });
    expect(response.status()).toBe(400);
    expect(response.headers()["content-type"]).toBe("application/json");
    expect(await response.json()).toEqual({
      error: "invalid_request",
      error_description: "Access Token cannot be provided in both query and header",
    });
  });
});

test.describe("OAuth2 access token errors", () => {
  test("unknown grant_type returns unsupported_grant_type", async ({ request }) => {
    const response = await request.post(`${OPENAM_BASE}/oauth2/access_token`, {
      maxRedirects: 0,
      form: { grant_type: "no_such_grant", client_id: CLIENT_ID },
      headers: { Accept: "application/json" },
    });

    expect(response.status()).toBe(400);
    expect((await response.json()).error).toBe("unsupported_grant_type");
    expect(response.headers()["cache-control"]).toBe("no-store");
    expect(response.headers()["pragma"]).toBe("no-cache");
  });

  test("bad client secret returns a Basic challenge", async ({ request }) => {
    await ensureTokenCacheClientExists(request);
    const response = await request.post(`${OPENAM_BASE}/oauth2/access_token`, {
      maxRedirects: 0,
      form: { grant_type: "client_credentials", scope: SCOPE },
      headers: {
        Accept: "application/json",
        Authorization: basicAuth(TOKEN_CACHE_CLIENT_ID, "wrong-secret"),
      },
    });

    expect(response.status()).toBe(401);
    expect((await response.json()).error).toBe("invalid_client");
    expect(response.headers()["www-authenticate"]).toBe('Basic realm="/"');
    expect(response.headers()["cache-control"]).toBe("no-store");
    expect(response.headers()["pragma"]).toBe("no-cache");
  });

  test("GET access_token returns method not allowed", async ({ request }) => {
    const response = await request.get(`${OPENAM_BASE}/oauth2/access_token`, {
      maxRedirects: 0,
      headers: { Accept: "application/json" },
    });

    expect(response.status()).toBe(405);
    expect((await response.json()).error).toBe("method_not_allowed");
    // Restlet omits Allow. The target is Allow: POST per RFC 9110 section 15.5.6.
    expect(response.headers()["allow"]).toBeUndefined();
    expect(response.headers()["cache-control"]).toBe("no-store");
    expect(response.headers()["pragma"]).toBe("no-cache");
  });
});

test.describe("OAuth2 cache headers", () => {
  const authorization = basicAuth(TOKEN_CACHE_CLIENT_ID, TOKEN_CACHE_CLIENT_SECRET);
  let clientAccessToken;

  test.beforeAll(async ({ request }) => {
    await ensureTokenCacheClientExists(request);
    const response = await request.post(`${OPENAM_BASE}/oauth2/access_token`, {
      maxRedirects: 0,
      form: { grant_type: "client_credentials", scope: SCOPE },
      headers: { Accept: "application/json", Authorization: authorization },
    });

    expect(response.status()).toBe(200);
    expect(response.headers()["cache-control"]).toBe("no-store");
    expect(response.headers()["pragma"]).toBe("no-cache");
    const tokens = await response.json();
    expect(typeof tokens.access_token).toBe("string");
    expect(Boolean(tokens.access_token), "Token response must include an access token").toBe(true);
    expect(tokens.token_type).toBe("Bearer");
    expect(Number.isInteger(tokens.expires_in)).toBe(true);
    expect(tokens.expires_in).toBeGreaterThan(0);
    clientAccessToken = tokens.access_token;
  });

  test("tokeninfo success carries its cache headers", async ({ request }) => {
    const response = await request.get(`${OPENAM_BASE}/oauth2/tokeninfo`, {
      maxRedirects: 0,
      params: { access_token: clientAccessToken },
      headers: { Accept: "application/json" },
    });

    expect(response.status()).toBe(200);
    const mediaType = response.headers()["content-type"]?.split(";", 1)[0].trim().toLowerCase();
    expect(mediaType).toBe("application/json");
    expect(response.headers()["cache-control"]).toBe("no-cache, no-store");
    expect(response.headers()["pragma"]).toBeUndefined();
    const tokenInfo = await response.json();
    expect(tokenInfo.client_id).toBe(TOKEN_CACHE_CLIENT_ID);
    expect(tokenInfo.access_token === clientAccessToken, "Token info must identify the issued token").toBe(true);
    expect(tokenInfo.expires_in).toBeGreaterThan(0);
  });

  test("introspect success carries no cache headers", async ({ request }) => {
    const response = await request.post(`${OPENAM_BASE}/oauth2/introspect`, {
      maxRedirects: 0,
      form: { token: clientAccessToken },
      headers: { Accept: "application/json", Authorization: authorization },
    });

    expect(response.status()).toBe(200);
    expect(response.headers()["cache-control"]).toBeUndefined();
    expect(response.headers()["pragma"]).toBeUndefined();
    const tokenInfo = await response.json();
    expect(tokenInfo.active).toBe(true);
    expect(tokenInfo.client_id).toBe(TOKEN_CACHE_CLIENT_ID);
  });

  test("userinfo without a resource owner carries no cache headers", async ({ request }) => {
    const response = await request.get(`${OPENAM_BASE}/oauth2/userinfo`, {
      maxRedirects: 0,
      headers: { Accept: "application/json", Authorization: `Bearer ${clientAccessToken}` },
    });

    // Target: 401 with a Bearer invalid_token challenge for a token that cannot identify an end user.
    expect(response.status()).toBe(400);
    expect((await response.json()).error).toBe("unauthorized_client");
    expect(response.headers()["cache-control"]).toBeUndefined();
    expect(response.headers()["pragma"]).toBeUndefined();
  });
});

/**
 * A non-browser client posts its consent decision directly, without ever rendering the consent page, and
 * submits its own session id as the csrf value. This is the flow documented for headless clients.
 */
test.describe("OAuth2 consent posted directly by a non-browser client", () => {

  /**
   * The authorization request parameters, shared by the GET that renders the consent page and the POST that
   * carries the decision. Both must describe the same request, including the PKCE challenge: the request
   * validators run before the csrf check, so an incomplete POST fails with a redirect long before the csrf
   * value is looked at.
   */
  function authorizeRequest(challenge, extra) {
    return {
      response_type: "code",
      client_id: CONSENT_CLIENT_ID,
      redirect_uri: REDIRECT_URI,
      scope: SCOPE,
      state: CONSENT_STATE,
      code_challenge: challenge,
      code_challenge_method: "S256",
      ...extra,
    };
  }

  /**
   * Authenticates and confirms consent really is required for this client: the GET returns the consent page
   * instead of redirecting with a code. That precondition also proves the client's redirect URI, scope,
   * response type and PKCE requirements are satisfied, so the rejection tests below cannot go green on a 400
   * that has nothing to do with the csrf value.
   */
  async function startConsentFlow(request) {
    const demoToken = await getAuthToken(request, USERNAME, PASSWORD);
    const challenge = await generateChallenge(generateVerifier());

    const consentPage = await request.get(`${OPENAM_BASE}/oauth2/realms/${REALM}/authorize`, {
      headers: {
        "iPlanetDirectoryPro": demoToken,
      },
      params: authorizeRequest(challenge),
      maxRedirects: 0,
    });

    expect(consentPage.status()).toBe(200);
    return { demoToken, challenge };
  }

  test("Should accept the session id as the csrf value", async ({ request }) => {
    const { demoToken, challenge } = await startConsentFlow(request);

    const response = await request.post(`${OPENAM_BASE}/oauth2/realms/${REALM}/authorize`, {
      headers: {
        "iPlanetDirectoryPro": demoToken,
      },
      form: authorizeRequest(challenge, { decision: "allow", csrf: demoToken }),
      maxRedirects: 0,
    });

    expect(response.status()).toBe(302);

    const location = new URL(response.headers()['location']);
    expect(location.searchParams.get("code")).toBeTruthy();
    expect(location.searchParams.get("state")).toBe(CONSENT_STATE);
  });

  test("Should reject the consent decision when csrf is missing", async ({ request }) => {
    const { demoToken, challenge } = await startConsentFlow(request);

    const response = await request.post(`${OPENAM_BASE}/oauth2/realms/${REALM}/authorize`, {
      headers: {
        "iPlanetDirectoryPro": demoToken,
      },
      form: authorizeRequest(challenge, { decision: "allow" }),
      maxRedirects: 0,
    });

    expect(response.status()).toBe(400);
  });

  test("Should reject a csrf value that is not the caller's session", async ({ request }) => {
    const { demoToken, challenge } = await startConsentFlow(request);

    const response = await request.post(`${OPENAM_BASE}/oauth2/realms/${REALM}/authorize`, {
      headers: {
        "iPlanetDirectoryPro": demoToken,
      },
      form: authorizeRequest(challenge, { decision: "allow", csrf: "not-the-session-id" }),
      maxRedirects: 0,
    });

    expect(response.status()).toBe(400);
  });

});

test.describe("OAuth2 routing contract", () => {
  const base = new URL(OPENAM_BASE);
  const oauth2 = OPENAM_BASE + "/oauth2";
  const discovery = "/.well-known/openid-configuration";
  const jwks = "/connect/jwk_uri";
  const realmsUrl = OPENAM_BASE + "/json/global-config/realms";
  const subRealm = "oauth2routing" + randomBytes(6).toString("hex");
  const clientId = "test_client_oauth2_b4c";
  const clientSecret = "b4c-secret-0123456789-0123456789-0123456789";
  const tokenForm = { grant_type: "client_credentials", scope: SCOPE };
  const tokenHeaders = { Authorization: basicAuth(clientId, clientSecret) };
  const emptyBody = { "Content-Length": "0" };

  let credentialed;
  let adminHeaders;
  let subRealmId;
  let token;

  const realmNotFound = (identifier) => ({
    code: 404, reason: "Not Found",
    message: "No mapping organization found for organization identifier: " + identifier,
  });
  const missingProvider = {
    error: "not_found", error_description: "No OpenID Connect provider for realm /" + subRealm,
  };
  const methodNotAllowed = (required, actual) => ({
    error: "method_not_allowed", error_description: "Required Method: " + required + " found: " + actual,
  });

  // The Playwright test runner records request URLs and failed call logs in HTML reports.
  // Use an isolated Playwright context for requests carrying credentials so reporters see only our assertions.
  function privateContext(options = {}) {
    const worker = new Worker(new URL("data:text/javascript," + encodeURIComponent(
      `import { parentPort, workerData } from "node:worker_threads";
      const { request } = await import(workerData.playwright);
      const context = await request.newContext(workerData.options);
      parentPort.on("message", async ({ url, options }) => {
        try {
          const response = await context.fetch(url, options);
          const result = { status: response.status(), headers: response.headers(), text: await response.text() };
          await response.dispose();
          parentPort.postMessage(result);
        } catch {
          parentPort.postMessage(null);
        }
      });`
    )), {
      workerData: { playwright: import.meta.resolve("playwright"), options },
      stdout: true, stderr: true,
    });
    worker.stdout.resume();
    worker.stderr.resume();
    const workerStopped = new Promise((resolve) => {
      worker.on("error", () => resolve(null));
      worker.on("exit", () => resolve(null));
    });

    async function request(url, options) {
      const result = await Promise.race([
        new Promise((resolve) => {
          worker.once("message", resolve);
          worker.postMessage({ url, options: { timeout: 30000, maxRedirects: 0, ...options } });
        }),
        workerStopped,
      ]);
      if (!result) {
        throw new Error("OpenAM request failed");
      }
      return {
        ok: () => result.status >= 200 && result.status < 300,
        status: () => result.status,
        headers: () => result.headers,
        json: async () => {
          try {
            return JSON.parse(result.text);
          } catch {
            throw new Error("OpenAM returned invalid JSON (status " + result.status + ")");
          }
        },
        text: () => Promise.resolve(result.text),
      };
    }

    return {
      fetch: request,
      post: (url, options) => request(url, { ...options, method: "POST" }),
      put: (url, options) => request(url, { ...options, method: "PUT" }),
      delete: (url, options) => request(url, { ...options, method: "DELETE" }),
      dispose: () => worker.terminate(),
    };
  }

  async function login(authenticate, label) {
    const context = privateContext();
    try {
      const session = await authenticate(context);
      expect(Boolean(session), label + " must return a session token").toBe(true);
      return session;
    } finally {
      await context.dispose();
    }
  }

  // Log only the endpoint label and status. Query strings and response bodies can carry credentials.
  function expectStatus(label, actual, expected) {
    console.log("[oauth2 routing] " + label + " -> " + actual);
    expect(actual, label).toBe(expected);
  }

  async function fetch(context, suffix, label, status, options = {}) {
    const response = await context.fetch(oauth2 + suffix, { ...options, maxRedirects: 0 })
      .catch(() => { throw new Error(label + ": request failed"); });
    expectStatus(label, response.status(), status);
    return response;
  }

  // Preserve path bytes, custom Host and requests with no Content-Type.
  function raw(suffix, label, status, { method = "GET", headers = {}, host = base.host } = {}) {
    const https = base.protocol === "https:";
    return new Promise((resolve, reject) => {
      const fail = () => reject(new Error(label + ": request failed"));
      const req = (https ? httpsRequest : httpRequest)({
        hostname: base.hostname, port: base.port || (https ? 443 : 80),
        path: base.pathname.replace(/\/$/, "") + "/oauth2" + suffix,
        method, headers: { ...headers, Host: host }, setHost: false,
        ...(https ? { servername: base.hostname } : {}),
      }, (response) => {
        let text = "";
        response.setEncoding("utf8");
        response.on("data", (chunk) => { text += chunk; });
        response.on("error", fail);
        response.on("end", () => resolve({ status: response.statusCode, headers: response.headers, text }));
      });
      req.on("error", fail);
      req.setTimeout(30000, () => req.destroy());
      req.end();
    }).then((response) => {
      expectStatus(label, response.status, status);
      return response;
    });
  }

  const tokenInfo = (prefix = "") => prefix + "/tokeninfo?access_token=" + encodeURIComponent(token);
  const postToken = (request, suffix, label, status, headers = {}) => fetch(request, "/access_token" + suffix,
    label, status, { method: "POST", form: tokenForm, headers: { ...tokenHeaders, ...headers } });

  test.beforeAll(async () => {
    credentialed = privateContext({ ignoreHTTPSErrors: true });
    const adminToken = await login(getAdminToken, "Admin authentication");
    await ensureClient(adminToken, credentialed, clientId, {
      "userpassword": clientSecret,
      "com.forgerock.openam.oauth2provider.clientType": "Confidential",
      "com.forgerock.openam.oauth2provider.scopes": ["[0]=" + SCOPE],
      "com.forgerock.openam.oauth2provider.defaultScopes": ["[0]=" + SCOPE],
      "com.forgerock.openam.oauth2provider.grantTypes": ["[0]=client_credentials"],
      "com.forgerock.openam.oauth2provider.tokenEndPointAuthMethod": "client_secret_basic",
      "sunIdentityServerDeviceStatus": "Active",
    }).catch(() => { throw new Error("Failed to provision routing client"); });
    const issued = await postToken(credentialed, "", "Issue routing token", 200);
    token = (await issued.json()).access_token;
    expect(Boolean(token), "Client credentials must yield an access token").toBe(true);

    adminHeaders = { iPlanetDirectoryPro: adminToken, "Accept-API-Version": "protocol=1.0,resource=1.0" };
    const created = await credentialed.post(realmsUrl + "?_action=create", {
      headers: adminHeaders, maxRedirects: 0,
      data: { name: subRealm, parentPath: "/", active: true, aliases: [] },
    });
    if (created.ok()) {
      subRealmId = (await created.json())._id;
    }
    expect(created.ok(), "Create routing realm: " + created.status()).toBe(true);
    expect(Boolean(subRealmId), "Realm creation must return an ID").toBe(true);
  });

  test.afterAll(async () => {
    try {
      if (subRealmId) {
        const deleted = await credentialed.delete(realmsUrl + "/" + encodeURIComponent(subRealmId), {
          headers: adminHeaders, maxRedirects: 0,
        });
        expect(deleted.ok(), "Delete routing realm: " + deleted.status()).toBe(true);
      }
    } finally {
      await credentialed?.dispose();
    }
  });

  test("realm query overrides the route realm", async ({ request }) => {
    for (const realm of ["/", "%2F"]) {
      const label = "GET /tokeninfo with realm=" + realm;
      const response = await fetch(credentialed, tokenInfo() + "&realm=" + realm, label, 200);
      expect((await response.json()).realm, label).toBe("/");
    }
    for (const realm of [subRealm, "/" + subRealm]) {
      const label = "GET discovery with realm=" + realm;
      const response = await fetch(request, discovery + "?realm=" + realm, label, 404);
      expect(await response.json(), label).toEqual(missingProvider);
    }
    // The target router resolves the override first and returns 400 invalid_request for this unknown realm.
    await fetch(request, "/nosuchendpoint?realm=bogus", "GET unmatched route with realm=bogus", 404);
  });

  test("unknown realm override fails before the token handler", async () => {
    // The target realm filter returns 400 invalid_request for unknown overrides.
    const get = await fetch(credentialed, "/tokeninfo?realm=bogus&access_token=" + encodeURIComponent(token),
      "GET /tokeninfo with realm=bogus", 404);
    expect(await get.json()).toEqual(realmNotFound("bogus"));

    const post = await postToken(credentialed, "?realm=bogus", "POST /access_token with realm=bogus", 404);
    expect(await post.json()).toEqual(realmNotFound("bogus"));
    expect(post.headers()["cache-control"]).toBeUndefined();
    expect(post.headers()["pragma"]).toBeUndefined();
  });

  test("legacy realm paths resolve and accept a query override", async ({ request }) => {
    const prefix = "/" + subRealm;
    const absent = await fetch(request, prefix + discovery, "GET legacy discovery", 404);
    expect(await absent.json()).toEqual(missingProvider);
    await fetch(credentialed, tokenInfo(prefix), "GET legacy /tokeninfo", 200);
    const root = await fetch(request, prefix + discovery + "?realm=/", "GET legacy discovery with realm=/", 200);
    expect((await root.json()).issuer).toBe(oauth2);
  });

  test("canonical paths resolve root, flat and nested realms", async ({ request }) => {
    await fetch(credentialed, tokenInfo("/realms/root"), "GET canonical root /tokeninfo", 200);
    const bogus = await fetch(credentialed, tokenInfo("/realms/bogus"), "GET canonical unknown realm /tokeninfo", 404);
    expect(await bogus.json()).toEqual(realmNotFound("/bogus"));

    for (const [label, prefix] of [
      ["GET flat realm discovery", "/realms/" + subRealm],
      ["GET nested realm discovery", "/realms/root/realms/" + subRealm],
    ]) {
      const response = await fetch(request, prefix + discovery, label, 404);
      expect(await response.json(), label).toEqual(missingProvider);
    }
  });

  test("HEAD reaches GET resources and is rejected by endpoint method filters", async ({ request }) => {
    for (const [endpoint, suffix, contentType] of [
      ["/tokeninfo", tokenInfo(), "application/json"],
      [discovery, discovery, "application/json;charset=UTF-8"],
      [jwks, jwks, "application/json;charset=UTF-8"],
    ]) {
      const context = endpoint === "/tokeninfo" ? credentialed : request;
      const head = await fetch(context, suffix, "HEAD " + endpoint, 200, { method: "HEAD" });
      await fetch(context, suffix, "GET " + endpoint, 200);
      expect(head.headers()["content-type"], endpoint).toBe(contentType);
      expect(head.headers()["content-length"], endpoint).toBeUndefined();
    }
    const cached = await fetch(credentialed, tokenInfo(), "HEAD /tokeninfo cache", 200, { method: "HEAD" });
    expect(cached.headers()["cache-control"]).toBe("no-cache, no-store");

    const session = await login((context) => getAuthToken(context, USERNAME, PASSWORD), "Demo authentication");
    const { challenge } = await pkce();
    const authorize = "/authorize?" + new URLSearchParams({
      response_type: "code", client_id: CLIENT_ID, redirect_uri: REDIRECT_URI, scope: SCOPE,
      state: "row5", code_challenge: challenge, code_challenge_method: "S256",
    });
    const auth = await sessionContext({ newContext: privateContext }, session);
    try {
      const head = await fetch(auth, authorize, "HEAD /authorize", 405, { method: "HEAD" });
      const get = await fetch(auth, authorize, "GET /authorize", 302);
      const location = get.headers()["location"];
      const code = URL.canParse(location, oauth2) && new URL(location, oauth2).searchParams.get("code");
      expect(Boolean(code), "GET /authorize must redirect with an authorization code").toBe(true);
      expect(head.headers()["cache-control"]).toBe("no-store");
      expect(head.headers()["pragma"]).toBe("no-cache");
    } finally {
      await auth.dispose();
    }

    await fetch(request, "/access_token", "HEAD /access_token", 405, { method: "HEAD" });
    await fetch(request, "/tokeninfo", "HEAD /tokeninfo without token", 401, { method: "HEAD" });
    const get = await fetch(request, "/tokeninfo", "GET /tokeninfo without token", 401);
    expect(get.headers()["content-type"]).toBe("application/json");
  });

  test("unmapped methods distinguish GET resources from method filters", async () => {
    for (const endpoint of ["/tokeninfo", jwks]) {
      for (const method of ["PROPFIND", "PUT"]) {
        const label = method + " " + endpoint;
        const response = await raw(endpoint, label, 405, { method, headers: emptyBody });
        expect(response.headers.allow, label).toBe("GET");
        expect(response.headers["cache-control"], label).toBeUndefined();
      }
    }
    // The target Allow values are POST for /access_token and GET, POST for /authorize.
    for (const [endpoint, required] of [["/access_token", "POST"], ["/authorize", "GET or POST"]]) {
      const label = "PROPFIND " + endpoint;
      const response = await raw(endpoint, label, 405, { method: "PROPFIND" });
      expect(JSON.parse(response.text), label).toEqual(methodNotAllowed(required, "PROPFIND"));
      expect(response.headers.allow, label).toBeUndefined();
      expect(response.headers["cache-control"], label).toBe("no-store");
    }
  });

  test("PATCH validates tokeninfo before rejecting the method", async () => {
    const patch = { method: "PATCH", headers: emptyBody };
    const authenticated = await raw(tokenInfo(), "PATCH /tokeninfo with token", 405, patch);
    expect(authenticated.headers.allow).toBe("GET");
    expect(authenticated.headers["cache-control"]).toBe("no-cache, no-store");

    const unauthenticated = await raw("/tokeninfo", "PATCH /tokeninfo without token", 401, patch);
    expect(JSON.parse(unauthenticated.text).error).toBe("invalid_token");

    const put = await raw(tokenInfo(), "PUT /tokeninfo with token", 405, { method: "PUT", headers: emptyBody });
    expect(put.headers["cache-control"]).toBeUndefined();

    const tokenResponse = await raw("/access_token", "PATCH /access_token", 405, patch);
    expect(JSON.parse(tokenResponse.text)).toEqual(methodNotAllowed("POST", "PATCH"));
  });

  test("API query flags preserve the endpoint responses", async ({ request }) => {
    const plain = await fetch(request, jwks, "GET plain JWKS", 200);
    const keys = await plain.json();
    expect(Array.isArray(keys.keys), "JWKS response must contain a keys array").toBe(true);
    for (const flag of ["_api", "_crestapi"]) {
      const label = "GET JWKS with " + flag;
      const response = await fetch(request, jwks + "?" + flag, label, 200);
      expect(response.headers()["content-type"], label).toBe("application/json;charset=UTF-8");
      expect(await response.json(), label).toEqual(keys);
    }
    await fetch(credentialed, "/tokeninfo?_api&access_token=" + encodeURIComponent(token), "GET /tokeninfo with _api", 200);
  });

  test("OPTIONS is rejected with or without a CORS preflight", async ({ request }) => {
    for (const [label, headers] of [
      ["OPTIONS /authorize without Origin", {}],
      ["OPTIONS /authorize with Origin", { Origin: "http://app.invalid", "Access-Control-Request-Method": "GET" }],
    ]) {
      const response = await fetch(request, "/authorize", label, 405, { method: "OPTIONS", headers });
      expect(await response.json(), label).toEqual(methodNotAllowed("GET or POST", "OPTIONS"));
      expect(Object.keys(response.headers()).filter((header) => header.startsWith("access-control-")), label)
        .toEqual([]);
    }

    const tokeninfo = await fetch(request, "/tokeninfo", "OPTIONS /tokeninfo", 405, { method: "OPTIONS" });
    expect(tokeninfo.headers().allow).toBe("GET");
  });

  test("method override header and query parameter have different scope", async () => {
    const header = await postToken(credentialed, "", "POST /access_token with override header GET", 405,
      { "X-HTTP-Method-Override": "GET" });
    expect(await header.json()).toEqual(methodNotAllowed("POST", "GET"));

    const control = await postToken(credentialed, "", "POST /access_token without override", 200);
    expect(Boolean((await control.json()).access_token), "Control POST must issue a token").toBe(true);

    await raw(tokenInfo(), "POST /tokeninfo with override header GET", 200, {
      method: "POST", headers: { ...emptyBody, "X-HTTP-Method-Override": "GET" },
    });
    await raw(tokenInfo(), "GET /tokeninfo with override header PUT", 405, {
      headers: { "X-HTTP-Method-Override": "PUT" },
    });

    const query = await postToken(credentialed, "?method=GET", "POST /access_token with method=GET", 405);
    expect((await query.json()).error_description).toBe("Required Method: POST found: GET");
    await fetch(credentialed, "/tokeninfo?method=PUT&access_token=" + encodeURIComponent(token),
      "GET /tokeninfo with method=PUT", 200);
  });

  test("unrouted paths return router or realm errors", async ({ request }) => {
    for (const [suffix, realm] of [
      ["/nosuchendpoint", null],
      ["/authorize/extra", "/authorize"],
      ["/connect/nosuch", "/connect"],
      ["/realms/root/nosuch", null],
    ]) {
      const label = "GET " + suffix;
      const response = await fetch(request, suffix, label, 404);
      if (realm) {
        expect(await response.json(), label).toEqual(realmNotFound(realm));
        expect(response.headers()["content-type"], label).toBe("application/json");
      }
    }
  });

  test("bare OAuth2 and realm prefixes have no default endpoint", async () => {
    for (const suffix of ["/", "", "/realms", "/realms/root"]) {
      await raw(suffix, "GET /oauth2" + suffix, 404);
    }
  });

  test("path spelling is significant and resource_set accepts a trailing slash", async () => {
    await raw("/tokeninfo/?access_token=" + encodeURIComponent(token), "GET /tokeninfo/ with token", 404);
    for (const [suffix, realm] of [
      [jwks + "/", "/connect"],
      [discovery + "/", "/.well-known"],
      ["/Tokeninfo", null],
      ["/TOKENINFO", null],
      ["/Connect/jwk_uri", "/Connect"],
      ["/connect/JWK_URI", "/connect"],
      ["//tokeninfo", null],
    ]) {
      const label = "GET " + suffix;
      const response = await raw(suffix, label, 404);
      if (realm) {
        expect(JSON.parse(response.text), label).toEqual(realmNotFound(realm));
      }
    }

    const resourceSet = await raw("/resource_set/", "GET /resource_set/ without token", 401);
    expect(JSON.parse(resourceSet.text).error).toBe("invalid_token");
    // The target response includes WWW-Authenticate: Bearer.
    expect(resourceSet.headers["www-authenticate"]).toBeUndefined();
  });

  test("unknown Host prevents realm resolution on legacy and canonical routes", async () => {
    const unknown = "not-a-real-host.invalid";
    for (const [endpoint, suffix, field, value] of [
      ["/tokeninfo", tokenInfo(), "realm", "/"],
      ["/realms/root/tokeninfo", tokenInfo("/realms/root"), "realm", "/"],
      [discovery, discovery, "issuer", oauth2],
    ]) {
      // The target response for an unknown Host is 400 invalid_request.
      const rejected = await raw(suffix, "GET " + endpoint + " with unknown Host", 500, { host: unknown });
      expect(JSON.parse(rejected.text), endpoint).toEqual({
        code: 500, reason: "Internal Server Error",
        message: "No mapping organization found for organization identifier: " + unknown,
      });

      const accepted = await raw(suffix, "GET " + endpoint + " with known Host", 200);
      expect(JSON.parse(accepted.text)[field], endpoint).toBe(value);
    }
    await raw(jwks, "GET JWKS with known Host", 200);
  });
});
