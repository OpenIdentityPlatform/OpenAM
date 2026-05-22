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