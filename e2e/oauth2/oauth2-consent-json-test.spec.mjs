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

/**
 * Covers the JSON consent representation, which lets a client that cannot render the consent page
 * obtain the anti-CSRF token it must echo back with the consent decision.
 *
 * Consent is only requested when the client does not imply it, so these tests use their own client
 * with isConsentImplied=false. The implied-consent client is here to prove the JSON branch cannot
 * hijack the ordinary redirect flow.
 */

import { test, expect } from "@playwright/test";
import { OPENAM_BASE, ensureOAuth2ServiceExists, getAdminToken, getAuthToken, PASSWORD, USERNAME }
  from "../common/openam-commons.mjs";

const REALM = "root";
const CONSENT_CLIENT_ID = "test_consent_client";
const IMPLIED_CLIENT_ID = "test_implied_consent_client";
const SCOPE = "profile";
const REDIRECT_URI = "http://app.invalid/cb";

async function ensureClientExists(adminToken, request, clientId, consentImplied) {
  const response = await request.get(
    `${OPENAM_BASE}/json/realms/${REALM}/realm-config/agents/OAuth2Client/${clientId}`,
    {
      headers: {
        "iPlanetDirectoryPro": adminToken,
        "Accept-API-Version": "protocol=2.0,resource=1.0",
      },
    }
  );

  if (response.status() !== 404) {
    if (!response.ok()) {
      throw new Error(`Failed to check OAuth2 client "${clientId}": ${response.statusText()}`);
    }
    console.log(`OAuth2 client "${clientId}" already exists`);
    return;
  }

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
        "isConsentImplied": consentImplied,
        "sunIdentityServerDeviceStatus": "Active",
      },
    }
  );

  if (!createResponse.ok()) {
    throw new Error(`Failed to create OAuth2 client "${clientId}": ${createResponse.statusText()}`);
  }
  console.log(`OAuth2 client "${clientId}" created successfully`);
}

function generateVerifier(length = 64) {
  const array = new Uint32Array(length);
  crypto.getRandomValues(array);
  const chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";
  return Array.from(array, (x) => chars[x % chars.length]).join("");
}

async function generateChallenge(verifier) {
  const hash = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(verifier));
  return btoa(String.fromCharCode(...new Uint8Array(hash)))
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

/** The authorization request parameters, shared by every test below. */
async function authorizeParams(clientId, state) {
  const verifier = generateVerifier();
  return {
    verifier,
    params: {
      response_type: "code",
      client_id: clientId,
      redirect_uri: REDIRECT_URI,
      scope: SCOPE,
      state,
      code_challenge: await generateChallenge(verifier),
      code_challenge_method: "S256",
    },
  };
}

test.beforeAll(async ({ request }) => {
  const adminToken = await getAdminToken(request);
  if (!adminToken) {
    test.skip("Skipping: could not obtain an admin token");
  }
  // The implied-consent client only skips consent when the provider allows it, so the service has to be
  // in place before either client is created - this file cannot rely on running after its sibling.
  await ensureOAuth2ServiceExists(adminToken, request, REALM, [SCOPE]);
  await ensureClientExists(adminToken, request, CONSENT_CLIENT_ID, false);
  await ensureClientExists(adminToken, request, IMPLIED_CLIENT_ID, true);
});

test.describe("OAuth2 JSON consent representation", () => {

  test("Should return the consent model as JSON and accept the decision posted back", async ({ request }) => {
    const demoToken = await getAuthToken(request, USERNAME, PASSWORD);
    const { verifier, params } = await authorizeParams(CONSENT_CLIENT_ID, "json-consent-state");

    // 1. Ask for the consent details as JSON instead of the HTML consent page.
    const consentResponse = await request.get(`${OPENAM_BASE}/oauth2/authorize`, {
      headers: { "iPlanetDirectoryPro": demoToken, "Accept": "application/json" },
      params,
      maxRedirects: 0,
    });

    expect(consentResponse.status()).toBe(200);
    expect(consentResponse.headers()["content-type"]).toContain("application/json");

    const consent = await consentResponse.json();
    console.log("Consent model:", consent);
    expect(typeof consent.csrf).toBe("string");
    expect(consent.csrf.length).toBeGreaterThan(0);
    expect(consent.target).toContain("authorize");
    expect(consent.client_id).toBe(CONSENT_CLIENT_ID);
    expect(Array.isArray(consent.scopes)).toBe(true);

    // 2. Post the decision back, exactly as the consent form does. The target already carries the
    // authorization request parameters, so only the decision and the token are added.
    const decisionResponse = await request.post(new URL(consent.target, OPENAM_BASE).toString(), {
      headers: { "iPlanetDirectoryPro": demoToken },
      form: { decision: "allow", csrf: consent.csrf },
      maxRedirects: 0,
    });

    expect(decisionResponse.status()).toBe(302);
    const code = new URL(decisionResponse.headers()["location"]).searchParams.get("code");
    expect(code).toBeTruthy();

    // 3. The code must be redeemable, proving the whole flow completed.
    const tokenResponse = await request.post(`${OPENAM_BASE}/oauth2/access_token`, {
      headers: { "Accept": "application/json" },
      form: {
        grant_type: "authorization_code",
        client_id: CONSENT_CLIENT_ID,
        code,
        redirect_uri: REDIRECT_URI,
        code_verifier: verifier,
      },
    });

    expect(tokenResponse.ok()).toBeTruthy();
    expect(await tokenResponse.json()).toHaveProperty("access_token");
  });

  test("Should reject a consent decision posted without the token", async ({ request }) => {
    const demoToken = await getAuthToken(request, USERNAME, PASSWORD);
    const { params } = await authorizeParams(CONSENT_CLIENT_ID, "missing-csrf-state");

    const consentResponse = await request.get(`${OPENAM_BASE}/oauth2/authorize`, {
      headers: { "iPlanetDirectoryPro": demoToken, "Accept": "application/json" },
      params,
      maxRedirects: 0,
    });
    expect(consentResponse.status()).toBe(200);
    const consent = await consentResponse.json();

    const decisionResponse = await request.post(new URL(consent.target, OPENAM_BASE).toString(), {
      headers: { "iPlanetDirectoryPro": demoToken },
      form: { decision: "allow" },
      maxRedirects: 0,
    });

    expect(decisionResponse.status()).toBe(400);
  });

  test("Should not hand the token to another origin", async ({ request }) => {
    const demoToken = await getAuthToken(request, USERNAME, PASSWORD);
    const { params } = await authorizeParams(CONSENT_CLIENT_ID, "foreign-origin-state");

    // A page on another origin must not be able to read the token and forge a consent decision,
    // even where the deployment allows credentialed CORS on /oauth2.
    const response = await request.get(`${OPENAM_BASE}/oauth2/authorize`, {
      headers: {
        "iPlanetDirectoryPro": demoToken,
        "Accept": "application/json",
        "Origin": "http://evil.invalid",
      },
      params,
      maxRedirects: 0,
    });

    expect(response.status()).toBe(403);
    expect(await response.text()).not.toContain("csrf");
  });

  test("Should still serve the HTML consent page when JSON is not asked for", async ({ request }) => {
    const demoToken = await getAuthToken(request, USERNAME, PASSWORD);
    const { params } = await authorizeParams(CONSENT_CLIENT_ID, "html-consent-state");

    // curl's default Accept is */*, which must not be treated as a request for JSON.
    const response = await request.get(`${OPENAM_BASE}/oauth2/authorize`, {
      headers: { "iPlanetDirectoryPro": demoToken, "Accept": "*/*" },
      params,
      maxRedirects: 0,
    });

    expect(response.status()).toBe(200);
    expect(response.headers()["content-type"]).toContain("text/html");
  });

  test("Should not change the flow for clients that imply consent", async ({ request }) => {
    const demoToken = await getAuthToken(request, USERNAME, PASSWORD);
    const { params } = await authorizeParams(IMPLIED_CLIENT_ID, "implied-consent-state");

    // Consent is never requested here, so asking for JSON must make no difference at all.
    const response = await request.get(`${OPENAM_BASE}/oauth2/authorize`, {
      headers: { "iPlanetDirectoryPro": demoToken, "Accept": "application/json" },
      params,
      maxRedirects: 0,
    });

    expect(response.status()).toBe(302);
    expect(new URL(response.headers()["location"]).searchParams.get("code")).toBeTruthy();
  });
});
