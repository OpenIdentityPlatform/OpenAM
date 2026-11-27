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
import { isInactiveIntrospection, withIntrospectionClient } from "../common/oauth2-fixtures.mjs";

test.use({ trace: "off", screenshot: "off" });

function expectActive(body, clientId, type, operation) {
  expect(body?.active === true, operation + " reports an active token").toBe(true);
  expect(body?.client_id === clientId, operation + " identifies the issuing client").toBe(true);
  expect(body?.token_type === type, operation + " identifies the token type").toBe(true);
}

test("introspects an access token by GET and form POST", async ({ ignoreHTTPSErrors }) => {
  await withIntrospectionClient({ ignoreHTTPSErrors, grantType: "client_credentials" }, async (fixture) => {
    const { id, accessToken, introspect } = fixture;
    const query = await introspect(accessToken, "access_token", "GET", "GET access token introspection");
    expectActive(query, id, "access_token", "GET access token introspection");
    const form = await introspect(accessToken, "access_token", "POST", "Form access token introspection");
    expectActive(form, id, "access_token", "Form access token introspection");
  });
});

test("introspects a refresh token with and without a hint", async ({ ignoreHTTPSErrors }) => {
  await withIntrospectionClient({ ignoreHTTPSErrors, grantType: "password" }, async ({ id, refreshToken, introspect }) => {
    const hinted = await introspect(refreshToken, "refresh_token", "POST", "Refresh token introspection with a hint");
    expectActive(hinted, id, "refresh_token", "Refresh token introspection with a hint");
    const unhinted = await introspect(refreshToken, undefined, "POST", "Refresh token introspection without a hint");
    expectActive(unhinted, id, "refresh_token", "Refresh token introspection without a hint");
  });
});

test("returns inactive for a mismatched token type hint", async ({ ignoreHTTPSErrors }) => {
  await withIntrospectionClient({ ignoreHTTPSErrors, grantType: "client_credentials" }, async (fixture) => {
    const { id, accessToken, introspect } = fixture;
    const control = await introspect(accessToken, "access_token", "POST", "Introspection with the matching hint");
    expectActive(control, id, "access_token", "Introspection with the matching hint");

    // Restlet only searches the hinted type. RFC 7662 section 2.1 requires falling back to other types,
    // which would make this access token active.
    const mismatched = await introspect(accessToken, "refresh_token", "POST", "Introspection with a mismatched hint");
    expect(isInactiveIntrospection(mismatched), "Mismatched hint returns only active false").toBe(true);
  });
});

test("returns only active false for an unknown token", async ({ ignoreHTTPSErrors }) => {
  await withIntrospectionClient({ ignoreHTTPSErrors }, async ({ introspect }) => {
    const body = await introspect(randomUUID(), undefined, "POST", "Unknown token introspection");
    expect(isInactiveIntrospection(body), "Unknown token returns only active false").toBe(true);
  });
});
