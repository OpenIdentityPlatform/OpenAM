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

import { test, expect, request as apiRequest } from "@playwright/test";
import { OPENAM_BASE, getAdminToken, getAuthToken, PASSWORD, USERNAME } from "../common/openam-commons.mjs";
import {
  basicAuth, ensureClient, ensureProviderConfig, pageDataValue, pkce, readUserCommonName, sessionContext,
  unreportedRequest, userNameValue,
} from "../common/oauth2-fixtures.mjs";

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

test.beforeAll(async ({ ignoreHTTPSErrors }) => {
  const request = unreportedRequest.newContext({ ignoreHTTPSErrors });
  try {
    const adminToken = await getAdminToken(request);
    expect(Boolean(adminToken), "Admin authentication must return a session token").toBe(true);
    await ensureOAuth2ServiceExists(adminToken, request);
    await ensureOAuth2ClientExists(adminToken, request);
    await ensureOAuth2ClientExists(adminToken, request, CONSENT_CLIENT_ID, false);
  } finally {
    await request.dispose();
  }
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

test.describe("OAuth2 authorize contract", () => {
  const OAUTH2 = `${OPENAM_BASE}/oauth2`;
  const AUTHORIZE = `${OAUTH2}/authorize`;
  const authorizePath = new URL(AUTHORIZE).pathname;
  const implicitClientId = "test_client_oauth2_b4d_implicit";
  const unknownScope = "no_such_scope";
  const noRedirects = { maxRedirects: 0 };

  let adminToken;
  let challenge;
  let demoToken;

  // Each session starts with its own cookie jar. The consent GET and POST share a jar through both requests.
  const contractTest = test.extend({
    authorizeAdminRequest: async ({ ignoreHTTPSErrors }, use) => {
      const request = unreportedRequest.newContext({ ignoreHTTPSErrors });
      try {
        await use(request);
      } finally {
        await request.dispose();
      }
    },

    authorizeSession: async ({ ignoreHTTPSErrors }, use) => {
      if (!demoToken) {
        const loginContext = unreportedRequest.newContext({ ignoreHTTPSErrors });
        try {
          demoToken = await getAuthToken(loginContext, USERNAME, PASSWORD);
          expect(Boolean(demoToken), "Resource-owner authentication must return a session token").toBe(true);
        } finally {
          await loginContext.dispose();
        }
      }

      const contexts = [];
      try {
        await use(async (extraCookies) => {
          const context = await sessionContext(unreportedRequest, demoToken, extraCookies);
          contexts.push(context);
          return context;
        });
      } finally {
        await Promise.all(contexts.map((context) => context.dispose()));
      }
    },
  });

  contractTest.beforeAll(async ({ authorizeAdminRequest }) => {
    adminToken = await getAdminToken(authorizeAdminRequest);
    expect(Boolean(adminToken), "Admin authentication must return a session token").toBe(true);
    await ensureProviderConfig(adminToken, authorizeAdminRequest);
    challenge = (await pkce()).challenge;
  });

  // Keep the raw query stable because both the login redirect and consent form carry it.
  function authorizeUrl({ clientId = CLIENT_ID, responseType = "code", scope = SCOPE,
    redirectUri = REDIRECT_URI, state, loginHint } = {}) {
    const query = new URLSearchParams({ response_type: responseType, client_id: clientId, redirect_uri: redirectUri, scope });
    if (state !== undefined) query.set("state", state);
    if (loginHint !== undefined) query.set("login_hint", loginHint);
    if (responseType === "code") {
      query.set("code_challenge", challenge);
      query.set("code_challenge_method", "S256");
    }
    return `${AUTHORIZE}?${query}`;
  }

  function expectNoCache(response) {
    expect(response.headers()["cache-control"]).toBe("no-store");
    expect(response.headers()["pragma"]).toBe("no-cache");
  }

  function responseCookies(response, name) {
    return response.headersArray()
      .filter((header) => header.name.toLowerCase() === "set-cookie" && header.value.startsWith(`${name}=`))
      .map((header) => {
        const [value, ...attributes] = header.value.slice(name.length + 1).split(";");
        return {
          value,
          attributes: new Map(attributes.map((attribute) => {
            const [key, attributeValue] = attribute.trim().split("=", 2);
            return [key.toLowerCase(), attributeValue];
          })),
        };
      });
  }

  function expectErrorPage(response) {
    expect(response.status()).toBe(400);
    expect(response.headers()["content-type"]).toBe("text/html;charset=UTF-8");
    expect("location" in response.headers(), "Error page must have no Location header").toBe(false);
  }

  function expectCodeCallback(response, state) {
    expect(response.status()).toBe(302);
    const location = response.headers()["location"];
    expect(Boolean(location?.startsWith(`${REDIRECT_URI}?`)), "Redirect must use the registered callback").toBe(true);
    expect(location.includes("#"), "Code callback must have no fragment").toBe(false);
    const params = new URL(location).searchParams;
    expect(Boolean(params.get("code")), "Callback must carry an authorization code").toBe(true);
    expect(params.get("state")).toBe(state);
    return params;
  }

  function expectInvalidScope(params, state) {
    expect(params.get("error")).toBe("invalid_scope");
    expect(params.get("error_description")).toBe(`Unknown/invalid scope(s): [${unknownScope}]`);
    expect(params.get("state")).toBe(state);
  }

  contractTest("redirects an unauthenticated GET to login with the original request as goto", async ({ request }) => {
    const url = authorizeUrl({ state: "row1state" });
    const response = await request.get(url, noRedirects);
    expect(response.status()).toBe(301);
    const location = response.headers()["location"];
    expect(Boolean(location?.startsWith(`${OPENAM_BASE}/UI/Login?realm=%2F&goto=`)), "Login redirect and encoded realm")
      .toBe(true);
    const loginUrl = new URL(location);
    expect(loginUrl.searchParams.get("realm")).toBe("/");
    expect(loginUrl.searchParams.get("goto")).toBe(url);
    expect(location).toContain("redirect_uri%3Dhttp%253A%252F%252Fapp.invalid%252Fcb");
    expectNoCache(response);
  });

  contractTest("renders consent data and a matching CSRF cookie", async ({ authorizeAdminRequest, authorizeSession, page }) => {
    const demoName = await readUserCommonName(adminToken, authorizeAdminRequest, USERNAME);
    const context = await authorizeSession();
    const url = authorizeUrl({ clientId: CONSENT_CLIENT_ID, state: "row2state" });
    const response = await context.get(url, noRedirects);
    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toBe("text/html;charset=UTF-8");
    expectNoCache(response);
    const html = await response.text();

    expect(pageDataValue(html, "clientId")).toBe(CONSENT_CLIENT_ID);
    expect(pageDataValue(html, "displayName")).toBe(CONSENT_CLIENT_ID);
    expect(await userNameValue(page, html)).toBe(demoName);
    expect(pageDataValue(html, "responseType")).toBe("code");
    expect(pageDataValue(html, "redirectUri")).toBe(REDIRECT_URI);
    expect(pageDataValue(html, "scope")).toBe(SCOPE);
    expect(pageDataValue(html, "state")).toBe("row2state");
    expect(pageDataValue(html, "locale")).toBe("*");
    expect(html.includes("isSaveConsentEnabled: true"), "Consent page allows saving the decision").toBe(true);
    expect(html.includes('displayScopes: [ { "name": "profile"'), "Consent page displays the profile scope").toBe(true);
    // FreeMarker escapes the leading slash in the JavaScript form target.
    expect(pageDataValue(html, "formTarget")).toBe(`\\${authorizePath}${new URL(url).search}`);

    const cookies = responseCookies(response, "oauth2_csrf");
    expect(cookies.length, "Response must set one oauth2_csrf cookie").toBe(1);
    const [cookie] = cookies;
    const csrf = pageDataValue(html, "csrf");
    expect(Boolean(csrf), "Consent page must carry a CSRF token").toBe(true);
    expect(cookie.value === csrf, "CSRF cookie must match the page token").toBe(true);
    expect(cookie.attributes.get("path")).toBe("/");
    expect(cookie.attributes.has("httponly")).toBe(true);
    expect(cookie.attributes.get("samesite")).toBe("Lax");
  });

  contractTest("renders an invalid_client error without redirecting an unknown client", async ({ authorizeSession }) => {
    const context = await authorizeSession();
    const response = await context.get(authorizeUrl({ clientId: "no_such_client", state: "row3state" }), noRedirects);
    expectErrorPage(response);
    const html = await response.text();
    expect(pageDataValue(html, "message")).toBe("invalid_client");
    expect(pageDataValue(html, "description")).toBe("Client authentication failed");
    expectNoCache(response);
  });

  contractTest("renders a redirect_uri_mismatch error without redirecting to an unregistered callback",
    async ({ authorizeSession }) => {
      const context = await authorizeSession();
      const url = authorizeUrl({ redirectUri: "http://unregistered.invalid/cb", state: "row4state" });
      const response = await context.get(url, noRedirects);
      expectErrorPage(response);
      const html = await response.text();
      expect(pageDataValue(html, "message")).toBe("redirect_uri_mismatch");
      expect(pageDataValue(html, "description")).toBe("The redirection URI provided does not match a pre-registered value.");
    });

  contractTest("redirects invalid_scope for the code flow in the callback query", async ({ authorizeSession }) => {
    const context = await authorizeSession();
    const response = await context.get(authorizeUrl({ scope: unknownScope, state: "row5state" }), noRedirects);
    expect(response.status()).toBe(302);
    const location = response.headers()["location"];
    expect(Boolean(location?.startsWith(`${REDIRECT_URI}?`)), "Error must use the registered callback query").toBe(true);
    expect(location.includes("#"), "Error callback must have no fragment").toBe(false);
    expectInvalidScope(new URL(location).searchParams, "row5state");
    expectNoCache(response);
  });

  contractTest("redirects invalid_scope for the implicit flow in the callback fragment",
    async ({ authorizeAdminRequest, authorizeSession }) => {
      await ensureClient(adminToken, authorizeAdminRequest, implicitClientId, {
        "com.forgerock.openam.oauth2provider.clientType": "Public",
        "com.forgerock.openam.oauth2provider.redirectionURIs": [`[0]=${REDIRECT_URI}`],
        "com.forgerock.openam.oauth2provider.scopes": [`[0]=${SCOPE}`],
        "com.forgerock.openam.oauth2provider.grantTypes": ["[0]=implicit"],
        "com.forgerock.openam.oauth2provider.responseTypes": ["[0]=token"],
        "com.forgerock.openam.oauth2provider.tokenEndPointAuthMethod": "none",
        sunIdentityServerDeviceStatus: "Active",
      });
      const context = await authorizeSession();
      const url = authorizeUrl({
        clientId: implicitClientId, responseType: "token", scope: unknownScope, state: "row6state",
      });
      const response = await context.get(url, noRedirects);
      expect(response.status()).toBe(302);
      const location = response.headers()["location"];
      expect(Boolean(location?.startsWith(`${REDIRECT_URI}#`)), "Error must use the registered callback fragment").toBe(true);
      expectInvalidScope(new URLSearchParams(new URL(location).hash.slice(1)), "row6state");
    });

  contractTest("rejects PUT with the authorize filter's method error", async ({ request }) => {
    const response = await request.put(authorizeUrl(), noRedirects);
    expect(response.status()).toBe(405);
    expect(response.headers()["content-type"]).toBe("application/json");
    const body = await response.json();
    expect(body.error).toBe("method_not_allowed");
    expect(body.error_description).toBe("Required Method: GET or POST found: PUT");
    expectNoCache(response);
  });

  contractTest("rejects a nonempty JSON POST with the authorize filter's content type error", async ({ request }) => {
    const response = await request.post(AUTHORIZE, {
      ...noRedirects,
      headers: { "Content-Type": "application/json" },
      data: { response_type: "code", client_id: CLIENT_ID, redirect_uri: REDIRECT_URI, scope: SCOPE },
    });
    expect(response.status()).toBe(400);
    expect(response.headers()["content-type"]).toBe("application/json");
    const body = await response.json();
    expect(body.error).toBe("invalid_request");
    expect(body.error_description).toBe("Invalid Content Type");
    expectNoCache(response);
  });

  contractTest("accepts the consent form and redirects with a code and issuer", async ({ authorizeSession }) => {
    const context = await authorizeSession();
    const url = authorizeUrl({ clientId: CONSENT_CLIENT_ID, state: "row9state" });
    const page = await context.get(url, noRedirects);
    expect(page.status()).toBe(200);
    const html = await page.text();
    const csrf = pageDataValue(html, "csrf");
    expect(Boolean(csrf), "Consent page must carry a CSRF token").toBe(true);
    const formTarget = pageDataValue(html, "formTarget")?.replace(/\\\//g, "/");
    expect(formTarget).toBe(`${authorizePath}${new URL(url).search}`);

    const response = await context.post(new URL(formTarget, OPENAM_BASE).href, {
      ...noRedirects,
      form: { decision: "allow", save_consent: "off", csrf },
    });
    const params = expectCodeCallback(response, "row9state");
    expect(params.get("scope")).toBe(SCOPE);
    expect(params.get("client_id")).toBe(CONSENT_CLIENT_ID);
    expect(params.get("iss")).toBe(OAUTH2);
    expectNoCache(response);
    expect(responseCookies(response, "oidcLoginHint").length, "No login_hint was supplied").toBe(0);
  });

  contractTest("sets login hint on consent and deletes only an existing hint after success",
    async ({ authorizeSession }) => {
      const consent = await authorizeSession();
      const loginHint = "authorize-contract-hint";
      const page = await consent.get(authorizeUrl({ clientId: CONSENT_CLIENT_ID, state: "row9b-a", loginHint }), noRedirects);
      expect(page.status()).toBe(200);
      const hints = responseCookies(page, "oidcLoginHint");
      expect(hints.length, "Consent response must set one login hint cookie").toBe(1);
      expect(hints[0].value === loginHint, "Login hint cookie must match the request hint").toBe(true);
      expect(hints[0].attributes.get("path")).toBe("/");
      expect(hints[0].attributes.has("httponly")).toBe(true);
      expect(hints[0].attributes.has("max-age"), "Login hint must be a session cookie without Max-Age").toBe(false);
      expect(hints[0].attributes.has("expires"), "Login hint must be a session cookie without Expires").toBe(false);

      const fresh = await authorizeSession();
      const freshResponse = await fresh.get(authorizeUrl({ state: "row9b-b", loginHint }), noRedirects);
      expectCodeCallback(freshResponse, "row9b-b");
      expect(responseCookies(freshResponse, "oidcLoginHint").length, "No existing login hint to delete").toBe(0);

      const primed = await authorizeSession([{ name: "oidcLoginHint", value: "previous-hint" }]);
      const primedResponse = await primed.get(authorizeUrl({ state: "row9b-c", loginHint }), noRedirects);
      expectCodeCallback(primedResponse, "row9b-c");
      const deletions = responseCookies(primedResponse, "oidcLoginHint");
      expect(deletions.length, "Success must emit one login hint deletion").toBe(1);
      const [deletion] = deletions;
      expect(deletion.value === "", "Deletion must carry an empty login hint").toBe(true);
      expect(Date.parse(deletion.attributes.get("expires")) < Date.now(), "Deletion must expire in the past").toBe(true);
      expect(deletion.attributes.has("max-age"), "Deletion must use the Expires attribute").toBe(false);
      // Target: expire the Path=/ cookie with Path=/ and HttpOnly. Restlet omits both attributes on deletion.
      expect(deletion.attributes.has("path")).toBe(false);
      expect(deletion.attributes.has("httponly")).toBe(false);
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
