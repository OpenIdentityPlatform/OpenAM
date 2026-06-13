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
 *
 * This spec covers three scenarios:
 *   1. login / session detection / logout, with the cookie HttpOnly flag matching the server mode;
 *   2. the admin staying logged in to the console after a full browser page reload;
 *   3. an agent-driven session upgrade (step-up) being recognised as an upgrade — not a brand-new
 *      login — after a fresh page load in HttpOnly mode.
 *
 * Step-up background / the bug it guards against:
 *   A step-up is triggered by a fresh page load (a redirect from the agent). After such a reload
 *   the XUI in-memory token is empty and, because the session cookie is HttpOnly, JavaScript cannot
 *   read the tokenId. As a result the XUI cannot send the "sessionUpgradeSSOTokenId" query param.
 *   Server-side that param used to be the ONLY source for the session to upgrade
 *   (LoginAuthenticator resolves it via getExistingValidSSOToken(new SessionID(getSSOTokenId())) and
 *   never reads the cookie). Without a fallback the request falls through to a brand-new login: the
 *   existing session is orphaned, its properties/sessionHandle are lost and composite-advice step-up
 *   can loop. The fix: when "sessionUpgradeSSOTokenId" is absent the REST authenticate flow falls
 *   back to the session carried by the (auto-sent) HttpOnly cookie as the upgrade target.
 *
 *   Token never leaves the body in HttpOnly mode (by default): a successful /json/authenticate
 *   response does NOT echo the tokenId when the cookie is HttpOnly (the token is delivered only via
 *   Set-Cookie). This prevents an XSS on the origin from reading a replayable SSO token via a single
 *   fetch. The XUI in HttpOnly mode does not consume body.tokenId — it relies on the auto-sent
 *   cookie / idFromSession.
 *
 * Response-body contract / configuration:
 *   The presence of body.tokenId in a successful /json/authenticate response depends on two server
 *   properties:
 *
 *     com.sun.identity.cookie.httponly                          (cookie HttpOnly flag)
 *     org.openidentityplatform.openam.httponly.allowTokenInBody (default: false)
 *
 *   Behaviour matrix (success response body):
 *     | httponly | allowTokenInBody | body.tokenId |
 *     |----------|------------------|--------------|
 *     | false    | (ignored)        | yes (legacy) |
 *     | true     | false (default)  | no           |
 *     | true     | true             | yes (opt-in) |
 *
 *   In all cases the session cookie is still set via Set-Cookie. This spec is mode-agnostic and, in
 *   the default HttpOnly deployment (allowTokenInBody=false), asserts that body.tokenId is absent.
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

    test("step-up after a fresh page load is recognised as a session upgrade, not a new login",
        async ({ page }) => {
            // ── 1. Discover the mode the server is actually running in ──────────────
            const info = await getServerInfo(page.request);
            const httpOnly = info.cookieHttpOnly === true;
            console.log(`Server reports cookieHttpOnly=${httpOnly}`);

            // The cookie fallback is specific to HttpOnly mode; in token-readable mode the XUI sends
            // the upgrade token itself and there is nothing to fall back to.
            test.skip(!httpOnly, "Session-cookie upgrade fallback only applies in HttpOnly mode");

            // ── 2. Log in -> establishes the HttpOnly session cookie in the browser ──
            await loginViaXui(page, USERNAME, PASSWORD);
            const idBefore = await idFromSession(page.request);
            expect(String(idBefore).toLowerCase()).toBe(USERNAME.toLowerCase());

            // ── 3. Simulate the step-up request issued right after the redirect ──────
            // A fresh page load means the XUI in-memory token is empty and the HttpOnly cookie
            // cannot be read, so NO sessionUpgradeSSOTokenId is sent. The HttpOnly session cookie
            // is, however, auto-sent with this request.
            const resp = await page.request.post(`${OPENAM_BASE}/json/authenticate`, {
                headers: {
                    "Content-Type": "application/json",
                    "Accept-API-Version": "protocol=1.0,resource=2.1",
                },
                data: "{}",
            });
            expect(resp.ok(), "authenticate against the existing session should succeed").toBeTruthy();
            const body = await resp.json();

            // ── 4. The existing session is recognised (no brand-new login) ──────────
            // With the cookie fallback the server resolves the session from the auto-sent HttpOnly
            // cookie and completes against it (successUrl/realm) instead of starting a brand-new login
            // (which would answer with an authId + callbacks, i.e. a fresh login form).
            //
            // Note: in HttpOnly mode the server deliberately does NOT echo the tokenId in the body
            // (it is delivered only via Set-Cookie), so recognition is asserted via the absence of a
            // fresh login and a successful completion, and confirmed by idFromSession below — NOT by
            // reading a token from the response body.
            expect(body.authId, "must NOT start a brand-new login flow (no fresh authId)").toBeFalsy();
            expect(body.callbacks, "must NOT present a fresh login form (no callbacks)").toBeFalsy();
            expect(body.tokenId, "tokenId must NOT be echoed in the body in HttpOnly mode").toBeFalsy();
            expect(body.successUrl ?? body.realm, "completion must reference the existing session")
                .toBeTruthy();

            // ── 5. The session is still the same user's session (not orphaned/replaced) ──
            const idAfter = await idFromSession(page.request);
            expect(String(idAfter).toLowerCase()).toBe(USERNAME.toLowerCase());
        });
});









