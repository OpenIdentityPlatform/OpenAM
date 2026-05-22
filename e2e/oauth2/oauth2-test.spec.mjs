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
const SCOPE="profile"
const REDIRECT_URI="http://app.invalid/cb"
/**
 * Ensures the OAuth2 service exists in the OpenAM instance.
 * Creates it with default configuration if it doesn't exist.
 */
async function ensureOAuth2ServiceExists(adminToken, request) {
  const response = await request.get(`${OPENAM_BASE}/json/realms/${REALM}/realm-config/services/oauth-oidc`,
    {
      headers: {
        "iPlanetDirectoryPro": adminToken,
        "Accept-API-Version": "protocol=1.0,resource=1.0",
      },
    }
  );

  if (response.status() === 404) {
    // OAuth2 service doesn't exist, create it
    const createResponse = await request.post(`${OPENAM_BASE}/json/realms/${REALM}/realm-config/services/oauth-oidc?_action=create`,
      {
        headers: {
          "iPlanetDirectoryPro": adminToken,
          "Content-Type": "application/json",
          "Accept-API-Version": "protocol=1.0,resource=1.0",
        },
        data: {
          advancedOAuth2Config: {
            clientsCanSkipConsent: true,
            supportedScopes: [SCOPE],
            defaultScopes: [SCOPE],
          },
        },
      }
    );

    if (!createResponse.ok()) {
      throw new Error(
        `Failed to create OAuth2 service: ${createResponse.statusText()}`
      );
    }
    console.log("OAuth2 service created successfully");
  } else if (!response.ok()) {
    throw new Error(
      `Failed to check OAuth2 service: ${createResponse.statusText()}`
    );
  } else {
    console.log("OAuth2 service already exists");
  }
}

/**
 * Ensures an OAuth2 client application exists in the OpenAM instance.
 * Creates it with default configuration if it doesn't exist.
 */

// curl -sS -X PUT \
//     -H "iPlanetDirectoryPro: ${ADMIN_TOKEN}" \
//     -H "Content-Type: application/json" -H "Accept-API-Version: protocol=2.0,resource=1.0" \
//     -d "{
//         \"com.forgerock.openam.oauth2provider.clientType\": \"Public\",
//         \"com.forgerock.openam.oauth2provider.redirectionURIs\": [\"[0]=${REDIRECT_URI}\"],
//         \"com.forgerock.openam.oauth2provider.scopes\": [\"[0]=${SCOPE}\"],
//         \"com.forgerock.openam.oauth2provider.defaultScopes\": [\"[0]=${SCOPE}\"],
//         \"com.forgerock.openam.oauth2provider.grantTypes\": [\"[0]=authorization_code\"],
//         \"com.forgerock.openam.oauth2provider.responseTypes\": [\"[0]=code\"],
//         \"com.forgerock.openam.oauth2provider.tokenEndPointAuthMethod\": \"none\",
//         \"isConsentImplied\": true,
//         \"sunIdentityServerDeviceStatus\": \"Active\"
//     }" \
//     "${BASE}/json/realms/${REALM}/realm-config/agents/OAuth2Client/${CLIENT_ID}" \
//     -o "${TMP}/client.json" -w "  client provisioned HTTP %{http_code}\n"

async function ensureOAuth2ClientExists(adminToken, request) {
  const response = await request.get(
    `${OPENAM_BASE}/json/realms/${REALM}/realm-config/agents/OAuth2Client/${CLIENT_ID}`,
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
      `${OPENAM_BASE}/json/realms/${REALM}/realm-config/agents/OAuth2Client/${CLIENT_ID}`,
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
          "isConsentImplied": true,
          "sunIdentityServerDeviceStatus": "Active"
        },
      }
    );

    if (!createResponse.ok()) {
      throw new Error(
        `Failed to create OAuth2 client: ${createResponse.statusText}`
      );
    }
    console.log(`OAuth2 client "${CLIENT_ID}" created successfully`);
  } else if (!response.ok()) {
    throw new Error(
      `Failed to check OAuth2 client: ${response.statusText}`
    );
  } else {
    console.log(`OAuth2 client "${CLIENT_ID}" already exists`);
  }
}

test.beforeAll(async ({ request }) => {
  const adminToken = await getAdminToken(request)

  if (!adminToken) {
    test.skip("Skipping: ADMIN_TOKEN not set");

  }
  await ensureOAuth2ServiceExists(adminToken, request);
  await ensureOAuth2ClientExists(adminToken, request);
});

let accessToken;

test.describe("OAuth Service test", () => {
  test("Should receive an auth code and exchange it to access token", async ({ request }) => {

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
