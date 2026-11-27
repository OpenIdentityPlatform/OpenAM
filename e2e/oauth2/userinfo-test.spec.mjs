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
import { expect, test } from "@playwright/test";
import { OPENAM_BASE, USERNAME } from "../common/openam-commons.mjs";
import { unreportedRequest, withIntrospectionClient } from "../common/oauth2-fixtures.mjs";

test.use({ trace: "off", screenshot: "off" });

const USERINFO = OPENAM_BASE + "/oauth2/userinfo";
const JSON_HEADERS = { Accept: "application/json" };

async function expectInvalidToken(response) {
  expect(response.status(), "UserInfo rejects the token").toBe(401);
  expect(response.headers()["content-type"], "UserInfo error media type").toBe("application/json");
  expect(await response.json(), "UserInfo invalid_token body").toEqual({
    error: "invalid_token",
    error_description: "The access token provided is expired, revoked, malformed, or invalid for other reasons.",
  });
  // Restlet omits the challenge. RFC 6750 section 3 requires a WWW-Authenticate Bearer challenge.
  expect(response.headers()["www-authenticate"], "Restlet omits the Bearer challenge").toBeUndefined();
}

async function userInfoBody(response, operation) {
  expect(response.status(), operation + " status").toBe(200);
  expect(response.headers()["content-type"], operation + " media type").toBe("application/json;charset=UTF-8");
  return response.json();
}

test("ignores query access tokens and preserves supported transports", async ({ ignoreHTTPSErrors }) => {
  await withIntrospectionClient({ ignoreHTTPSErrors, grantType: "password", requireRefreshToken: false },
    async ({ accessToken, request }) => {
      const headers = { ...JSON_HEADERS, Authorization: "Bearer " + accessToken };
      const bearer = await userInfoBody(await request.get(USERINFO, { headers }), "Bearer UserInfo");
      expect(bearer.sub, "UserInfo identifies the resource owner").toBe(USERNAME);

      const form = await userInfoBody(await request.post(USERINFO, {
        headers: JSON_HEADERS, form: { access_token: accessToken },
      }), "Form UserInfo");
      expect(form, "Form UserInfo matches the complete Bearer JSON").toEqual(bearer);

      // Restlet reads header and form tokens only; a valid query token is still missing credentials.
      await expectInvalidToken(await request.get(USERINFO, {
        headers: JSON_HEADERS, params: { access_token: accessToken },
      }));

      const combined = await userInfoBody(await request.get(USERINFO, {
        headers, params: { access_token: randomUUID() },
      }), "Bearer with query UserInfo");
      expect(combined, "Query token leaves the complete Bearer JSON unchanged").toEqual(bearer);
    });
});

test("rejects a JSON POST to userinfo", async ({ ignoreHTTPSErrors }) => {
  const request = unreportedRequest.newContext({ ignoreHTTPSErrors, maxRedirects: 0 });
  try {
    const response = await request.post(USERINFO, {
      headers: { ...JSON_HEADERS, "Content-Type": "application/json" },
      data: { access_token: randomUUID() },
    });
    // Restlet selects the form POST handler by media type; only pin the framework status.
    expect(response.status(), "Restlet rejects JSON POST").toBe(415);
  } finally {
    await request.dispose();
  }
});

test("rejects userinfo without a bearer token", async ({ ignoreHTTPSErrors }) => {
  const request = unreportedRequest.newContext({ ignoreHTTPSErrors, maxRedirects: 0 });
  try {
    await expectInvalidToken(await request.get(USERINFO, { headers: JSON_HEADERS }));
  } finally {
    await request.dispose();
  }
});

test("rejects userinfo with an invalid bearer token", async ({ ignoreHTTPSErrors }) => {
  const request = unreportedRequest.newContext({ ignoreHTTPSErrors, maxRedirects: 0 });
  try {
    await expectInvalidToken(await request.get(USERINFO, {
      headers: { ...JSON_HEADERS, Authorization: "Bearer " + randomUUID() },
    }));
  } finally {
    await request.dispose();
  }
});
