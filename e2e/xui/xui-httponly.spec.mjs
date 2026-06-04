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
 * OpenAM XUI - HttpOnly session cookie test
 *
 * Goal: prove that the XUI works correctly REGARDLESS of whether the session
 * cookie (e.g. iPlanetDirectoryPro) is issued with the HttpOnly flag.
 *
 * The test is "mode-agnostic": it asks the server which mode it is running in
 * (GET /json/serverinfo/*, field "cookieHttpOnly") and then asserts that the
 * real browser cookie and the XUI behaviour match that mode. The very same spec
 * therefore validates BOTH modes:
 *   - run it against a server started without the flag        -> HttpOnly = false
 *   - run it against a server started with
 *     -Dcom.sun.identity.cookie.httponly=true                 -> HttpOnly = true
 *
 * Optionally set EXPECT_COOKIE_HTTPONLY=true|false to additionally assert that
 * the server is in the expected mode (useful for the CI matrix).
 */

import { test, expect } from "@playwright/test";
import { OPENAM_BASE, USERNAME, PASSWORD, ADMIN_USER, ADMIN_PASS } from "../common/openam-commons.mjs";

// XUI / LESS-based OpenAM login form selectors
const SEL = {
    usernameInput: "#idToken1",
    passwordInput: "#idToken2",
    // The submit button id varies between XUI builds (loginButton / loginButton_0 / none),
    // so match by submit type as the working SAML spec does.
    loginButton: "#loginButton, input[type=\"submit\"], button[type=\"submit\"]",
};

// Optional hard expectation for the CI matrix ("true" | "false" | undefined)
const EXPECT_HTTPONLY = process.env.EXPECT_COOKIE_HTTPONLY;

async function getServerInfo(request) {
    const resp = await request.get(`${OPENAM_BASE}/json/serverinfo/*`, {
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
    });
    expect(resp.ok(), "GET /json/serverinfo/* should succeed").toBeTruthy();
    return resp.json();
}

/** Log in through the XUI login form and wait until the user leaves the #login route. */
async function loginViaXui(page, user, pass) {
    await page.goto(`${OPENAM_BASE}/XUI/#login/`);
    await expect(page.locator(SEL.usernameInput)).toBeVisible({ timeout: 20_000 });
    await page.fill(SEL.usernameInput, user);
    await page.fill(SEL.passwordInput, pass);
    await page.locator(SEL.loginButton).first().click();
    await page.waitForURL((url) => !url.hash.startsWith("#login"), { timeout: 30_000 });
}

/** Resolve the username of the active session from the (auto-sent) session cookie. */
async function idFromSession(request) {
    const resp = await request.post(`${OPENAM_BASE}/json/users?_action=idFromSession`, {
        headers: { "Accept-API-Version": "protocol=1.0,resource=2.0" },
    });
    if (!resp.ok()) {
        return null;
    }
    return (await resp.json()).id;
}

test.describe("OpenAM XUI - HttpOnly session cookie", () => {
    test("XUI login/session/logout work and cookie flag matches server mode", async ({ page, context }) => {
        // ── 1. Discover the mode the server is actually running in ──────────────
        const info = await getServerInfo(page.request);
        const cookieName = info.cookieName ?? "iPlanetDirectoryPro";
        const httpOnly = info.cookieHttpOnly === true;
        console.log(`Server reports cookieName=${cookieName}, cookieHttpOnly=${httpOnly}`);

        if (EXPECT_HTTPONLY !== undefined) {
            expect(httpOnly, `server should run with cookieHttpOnly=${EXPECT_HTTPONLY}`)
                .toBe(EXPECT_HTTPONLY === "true");
        }

        // ── 2. Log in through the XUI login form ────────────────────────────────
        await page.goto(`${OPENAM_BASE}/XUI/#login/`);
        await expect(page.locator(SEL.usernameInput)).toBeVisible({ timeout: 20_000 });
        await page.fill(SEL.usernameInput, USERNAME);
        await page.fill(SEL.passwordInput, PASSWORD);
        await page.locator(SEL.loginButton).first().click();

        // ── 3. XUI must consider the user logged in (leaves the #login route) ────
        await page.waitForURL((url) => !url.hash.startsWith("#login"), { timeout: 30_000 });

        // ── 4. The session cookie must carry the expected HttpOnly attribute ────
        const cookies = await context.cookies();
        const session = cookies.find((c) => c.name === cookieName);
        expect(session, `session cookie "${cookieName}" must be present`).toBeTruthy();
        expect(session.httpOnly, `cookie HttpOnly attribute must match server mode`).toBe(httpOnly);

        // ── 5. JS visibility of the cookie must match the mode ──────────────────
        // With HttpOnly=true the token must NOT be readable from document.cookie;
        // with HttpOnly=false it must be readable. XUI must keep working either way.
        const visibleInJs = await page.evaluate((name) => document.cookie.includes(`${name}=`), cookieName);
        expect(visibleInJs, "document.cookie visibility must be the inverse of HttpOnly").toBe(!httpOnly);

        // ── 6. Logged-in detection must work WITHOUT reading the cookie in JS ────
        // idFromSession resolves the session from the auto-sent (HttpOnly) cookie.
        const idResp = await page.request.post(
            `${OPENAM_BASE}/json/users?_action=idFromSession`,
            { headers: { "Accept-API-Version": "protocol=1.0,resource=2.0" } }
        );
        expect(idResp.ok(), "idFromSession should resolve the active session").toBeTruthy();
        const idJson = await idResp.json();
        expect(String(idJson.id).toLowerCase()).toBe(USERNAME.toLowerCase());

        // ── 7. Logout through the XUI must end on the logged-out/login route ────
        await page.goto(`${OPENAM_BASE}/XUI/#logout/`);
        await page.waitForURL((url) => /^#(loggedOut|login)/.test(url.hash), { timeout: 30_000 });

        // ── 8. The session must be invalidated server-side after logout ─────────
        // Checking the browser cookie is not reliable: in HttpOnly mode JavaScript
        // cannot clear it and the REST logout may not emit a Set-Cookie, so a stale
        // (but dead) cookie can linger. The meaningful guarantee is that the server
        // no longer resolves the session, which holds in both modes.
        const afterLogoutId = await page.request.post(
            `${OPENAM_BASE}/json/users?_action=idFromSession`,
            { headers: { "Accept-API-Version": "protocol=1.0,resource=2.0" } }
        );
        const sessionStillValid = afterLogoutId.ok() &&
            String((await afterLogoutId.json()).id).toLowerCase() === USERNAME.toLowerCase();
        expect(sessionStillValid, "session must be invalid after logout").toBe(false);
    });

    test("admin stays logged in to the console after a browser page reload", async ({ page }) => {
        // Reloading re-bootstraps the XUI from scratch: any in-memory token is lost, so the
        // session must be re-detected purely from the (auto-sent) session cookie. This is the
        // critical path that the HttpOnly support has to keep working.

        // ── 1. Log in to the admin console ──────────────────────────────────────
        await loginViaXui(page, ADMIN_USER, ADMIN_PASS);
        expect(String(await idFromSession(page.request)).toLowerCase()).toBe(ADMIN_USER.toLowerCase());

        // Land on the admin console (realms view) so the reload happens on a real console route.
        await page.goto(`${OPENAM_BASE}/XUI/#realms/%2F`);
        await page.waitForURL((url) => !url.hash.startsWith("#login"), { timeout: 30_000 });

        // ── 2. Reload the page in the browser ───────────────────────────────────
        await page.reload({ waitUntil: "networkidle" });

        // ── 3. The user must still be logged in (not bounced back to #login) ─────
        await page.waitForLoadState("networkidle");
        expect(page.url(), "reload must not redirect to the login page").not.toContain("#login");
        await expect(page.locator(SEL.usernameInput), "login form must not be shown after reload")
            .toHaveCount(0);

        // ── 4. The session is still resolvable after the reload ─────────────────
        expect(String(await idFromSession(page.request)).toLowerCase()).toBe(ADMIN_USER.toLowerCase());
    });
});







