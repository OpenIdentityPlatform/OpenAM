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


import { expect, test } from "@playwright/test";
import { isInactiveIntrospection, withIntrospectionClient } from "../common/oauth2-fixtures.mjs";

test.use({ trace: "off", screenshot: "off" });

async function expectActiveToken(fixture, token, type, operation) {
  const body = await fixture.introspect(token, type, "POST", operation);
  expect(body?.active === true, operation + " reports an active token").toBe(true);
  expect(body?.client_id === fixture.id, operation + " identifies the issuing client").toBe(true);
  expect(body?.token_type === type, operation + " identifies the token type").toBe(true);
  return body.sub;
}

test("revokes a refresh token and its linked access tokens", async ({ ignoreHTTPSErrors }) => {
  await withIntrospectionClient({
    ignoreHTTPSErrors,
    prefix: "s7b",
    grantType: "password",
    additionalGrantTypes: ["refresh_token"],
    allowInactiveBeforeCleanup: true,
  }, async (fixture) => {
    const { accessToken, refreshToken, issueTokens, postRevocation, introspect } = fixture;
    const exchanged = await issueTokens("refresh_token", refreshToken);
    expect(typeof exchanged.refreshToken === "string" && exchanged.refreshToken.trim().length > 0,
      "Refresh token exchange issues a new refresh token").toBe(true);
    expect(exchanged.refreshToken !== refreshToken, "Refresh token exchange rotates the original refresh token").toBe(true);
    const originalRefreshState = await introspect(refreshToken, "refresh_token", "POST", "Original refresh token after exchange");
    expect(isInactiveIntrospection(originalRefreshState), "Original refresh token is inactive after exchange").toBe(true);
    const control = await issueTokens("password");

    const targets = [
      { token: accessToken, type: "access_token", label: "Original grant access token" },
      { token: exchanged.accessToken, type: "access_token", label: "Exchanged access token" },
      { token: exchanged.refreshToken, type: "refresh_token", label: "Rotated refresh token" },
    ];
    const controls = [
      { token: control.accessToken, type: "access_token", label: "Control access token" },
      { token: control.refreshToken, type: "refresh_token", label: "Control refresh token" },
    ];
    const issued = [refreshToken, ...[...targets, ...controls].map(({ token }) => token)];
    expect(new Set(issued).size === issued.length, "All issued token identifiers are distinct").toBe(true);

    const subject = await expectActiveToken(fixture, accessToken, "access_token", "Original grant access token before POST revocation");
    expect(typeof subject === "string" && subject.trim().length > 0, "Original grant access token has a subject").toBe(true);
    for (const { token, type, label } of [...targets.slice(1), ...controls]) {
      const operation = label + " before POST revocation";
      expect(await expectActiveToken(fixture, token, type, operation) === subject,
        operation + " identifies the same subject").toBe(true);
    }

    const response = await postRevocation(exchanged.refreshToken);
    expect(response.status(), "POST revocation returns HTTP 200").toBe(200);
    const headers = response.headers();
    expect(headers["content-type"]?.split(";")[0].trim().toLowerCase() === "application/json",
      "POST revocation returns application/json").toBe(true);
    const body = await response.json().catch(() => undefined);
    expect(body !== null && typeof body === "object" && !Array.isArray(body) && Object.keys(body).length === 0,
      "POST revocation returns an empty JSON object").toBe(true);
    // The Restlet success response omits both cache headers.
    expect(headers["cache-control"] === undefined, "POST revocation omits Cache-Control").toBe(true);
    expect(headers.pragma === undefined, "POST revocation omits Pragma").toBe(true);

    for (const { token, type, label } of targets) {
      const result = await introspect(token, type, "POST", label + " after POST revocation");
      expect(isInactiveIntrospection(result), label + " is inactive after POST revocation").toBe(true);
    }
    for (const { token, type, label } of controls) {
      const operation = label + " after POST revocation";
      expect(await expectActiveToken(fixture, token, type, operation) === subject,
        operation + " identifies the same subject").toBe(true);
    }
  });
});

test("rejects GET revocation without invalidating the token", async ({ ignoreHTTPSErrors }) => {
  await withIntrospectionClient({ ignoreHTTPSErrors, prefix: "s7b", grantType: "client_credentials" }, async (fixture) => {
    const { accessToken, getRevocation } = fixture;
    await expectActiveToken(fixture, accessToken, "access_token", "Access token before GET revocation");

    const response = await getRevocation(accessToken);
    expect(response.status(), "Direct GET revocation returns HTTP 405").toBe(405);

    await expectActiveToken(fixture, accessToken, "access_token", "Access token after GET revocation");
  });
});
