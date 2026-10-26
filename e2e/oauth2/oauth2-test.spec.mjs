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

import { test, expect } from "@playwright/test";
import { OPENAM_BASE, getAdminToken, getAuthToken, PASSWORD, USERNAME } from "../common/openam-commons.mjs";

const REALM = "root";
const CLIENT_ID = "test_client_app";
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

  if (!adminToken) {
    test.skip("Skipping: ADMIN_TOKEN not set");

  }
  await ensureOAuth2ServiceExists(adminToken, request);
  await ensureOAuth2ClientExists(adminToken, request);
  await ensureOAuth2ClientExists(adminToken, request, CONSENT_CLIENT_ID, false);
});

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

/**
 * WebFinger (/.well-known/webfinger) has a servlet of its own in web.xml, outside /oauth2. Its resource reads the
 * deployment URL from the servlet request, and that request can only be reached when the servlet is the in-repo
 * Restlet ServerServlet: on any other one, every request fails with a 500.
 */
test.describe("OpenID Connect issuer discovery through WebFinger", () => {

  test("Should answer with the OpenID Connect issuer", async ({ request }) => {
    const resource = `acct:${USERNAME}@${new URL(OPENAM_BASE).hostname}`;
    const rel = "http://openid.net/specs/connect/1.0/issuer";

    const response = await request.get(`${OPENAM_BASE}/.well-known/webfinger`, {
      params: { resource, rel },
    });

    expect(response.status()).toBe(200);

    const discovery = await response.json();
    expect(discovery.subject).toBe(resource);
    expect(discovery.links).toContainEqual({ rel, href: `${OPENAM_BASE}/oauth2` });
  });

});
