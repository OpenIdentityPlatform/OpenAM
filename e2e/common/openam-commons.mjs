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

export const OPENAM_BASE = process.env.OPENAM_BASE_URL ?? "http://openam.example.org:8080/openam";
export const ADMIN_USER = process.env.OPENAM_ADMIN_USER ?? "amadmin";
export const ADMIN_PASS = process.env.OPENAM_ADMIN_PASS ?? "ampassword";

export const USERNAME = process.env.OPENAM_USERNAME ?? "demo";
export const PASSWORD = process.env.OPENAM_PASSWORD ?? "changeit";

export async function getAdminToken(request) {
    return getAuthToken(request, ADMIN_USER, ADMIN_PASS)
}

/**
 * Ensures the OAuth2 provider service exists in the realm, creating it with the given scopes if not.
 * Safe to call from several spec files at once: a create that loses the race counts as success.
 */
export async function ensureOAuth2ServiceExists(adminToken, request, realm, scopes) {
  const url = `${OPENAM_BASE}/json/realms/${realm}/realm-config/services/oauth-oidc`;
  const headers = {
    "iPlanetDirectoryPro": adminToken,
    "Accept-API-Version": "protocol=1.0,resource=1.0",
  };

  const response = await request.get(url, { headers });
  if (response.ok()) {
    console.log("OAuth2 service already exists");
    return;
  }
  if (response.status() !== 404) {
    throw new Error(`Failed to check OAuth2 service: ${response.statusText()}`);
  }

  const createResponse = await request.post(`${url}?_action=create`, {
    headers: { ...headers, "Content-Type": "application/json" },
    data: {
      advancedOAuth2Config: {
        clientsCanSkipConsent: true,
        supportedScopes: scopes,
        defaultScopes: scopes,
      },
    },
  });

  if (createResponse.ok()) {
    console.log("OAuth2 service created successfully");
    return;
  }
  if (!(await request.get(url, { headers })).ok()) {
    throw new Error(`Failed to create OAuth2 service: ${createResponse.statusText()}`);
  }
}

export async function getAuthToken(request, username, password) {
  const resp = await request.post(`${OPENAM_BASE}/json/authenticate`, {
    headers: { 
      "Content-Type": "application/json",
      "X-OpenAM-Username": username,
      "X-OpenAM-Password": password,
      "Content-Type": "application/json",
      "Accept-API-Version": "resource=2.0, protocol=1.0",
    }
  });
  const json = await resp.json();
  return json.tokenId;
}