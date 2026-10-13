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

import { expect } from "@playwright/test";

export const OPENAM_BASE = process.env.OPENAM_BASE_URL ?? "http://openam.example.org:8080/openam";
export const ADMIN_USER = process.env.OPENAM_ADMIN_USER ?? "amadmin";
export const ADMIN_PASS = process.env.OPENAM_ADMIN_PASS ?? "ampassword";

export const USERNAME = process.env.OPENAM_USERNAME ?? "demo";
export const PASSWORD = process.env.OPENAM_PASSWORD ?? "changeit";

export async function getAdminToken(request) {
    return getAuthToken(request, ADMIN_USER, ADMIN_PASS)
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

// XUI / LESS-based OpenAM login form selectors
export const XUI_LOGIN = {
  usernameInput: "#idToken1",
  passwordInput: "#idToken2",
  // The submit button id varies between XUI builds (loginButton / loginButton_0 / none),
  // so match by submit type as the working SAML spec does.
  loginButton: "#loginButton, input[type=\"submit\"], button[type=\"submit\"]",
};

/** Fill in and submit the XUI login form once the page shows it. */
export async function submitXuiLogin(page, username, password) {
  await expect(page.locator(XUI_LOGIN.usernameInput)).toBeVisible({ timeout: 30_000 });
  await page.fill(XUI_LOGIN.usernameInput, username);
  await page.fill(XUI_LOGIN.passwordInput, password);
  await page.locator(XUI_LOGIN.loginButton).first().click();
}

/**
 * Resolve the id of the active session from the (auto-sent) session cookie, or from the given token.
 * Returns null when there is no valid session.
 */
export async function idFromSession(request, tokenId) {
  const headers = { "Accept-API-Version": "protocol=1.0,resource=2.0" };
  if (tokenId) {
    headers.iPlanetDirectoryPro = tokenId;
  }
  const resp = await request.post(`${OPENAM_BASE}/json/users?_action=idFromSession`, { headers });
  if (!resp.ok()) {
    return null;
  }
  return (await resp.json()).id;
}