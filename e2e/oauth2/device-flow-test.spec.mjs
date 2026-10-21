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
 * OAuth2 device flow - the verification endpoint under implied consent.
 *
 * The realm allows clients to skip consent and the client asks for it, so /oauth2/device/user takes the
 * branch that authorizes a device code without rendering a consent page. That branch used to bind the device
 * code to the caller's session with no CSRF check at all (GHSA-545c-rm35-2jj2): a logged-in victim following
 * one cross-site link handed the attacker's client access and ID tokens issued for the victim.
 *
 * This spec pins every half of the fix:
 *   - no navigation to the endpoint, however it is triggered, authorizes anything on its own;
 *   - a navigation carrying a user code does not offer that code back to the user either, so a link cannot
 *     turn the page into a one-click approval;
 *   - the code that gets authorized is the one submitted in the form, never one a link smuggled through the
 *     query string, which the form's own action would otherwise carry back;
 *   - the form is served only from an address carrying no user code, so a browser never submits two of them,
 *     and a request that does carry two is refused;
 *   - the user code form the endpoint renders still completes the flow, because it carries a CSRF token, and
 *     a non-browser client can complete it by submitting its own session id as that token;
 *   - the flow still completes for a visitor who is not logged in yet, which is the normal way in: their
 *     submission is sent to login and comes back here, and neither leg may smuggle a user code along.
 */

import { test, expect } from "@playwright/test";
import { OPENAM_BASE, getAdminToken, getAuthToken, USERNAME, PASSWORD } from "../common/openam-commons.mjs";

const REALM = "root";
// A public client (token endpoint auth method "none") with implied consent - the combination that selects
// the skip-consent branch, and the one an attacker needs no secret to drive.
const CLIENT_ID = "test_device_app";
const SCOPE = "profile";
const DEVICE_GRANT = "urn:ietf:params:oauth:grant-type:device_code";

/**
 * Turns implied consent on for the realm's OAuth2 provider, leaving the rest of its configuration alone.
 * Without this the endpoint takes the consent branch and the flow under test never runs.
 *
 * The sibling oauth2 spec provisions the same service, and Playwright runs spec files in parallel workers,
 * so the service can appear or change underneath this hook. Both specs want implied consent enabled, so a
 * lost update can only cost a retry, never leave the wrong value behind - which is also why nothing here
 * restores the previous setting: doing so would break whichever spec is still running.
 */
async function ensureClientsCanSkipConsent(adminToken, request) {
    const path = `${OPENAM_BASE}/json/realms/${REALM}/realm-config/services/oauth-oidc`;
    const headers = {
        "iPlanetDirectoryPro": adminToken,
        "Content-Type": "application/json",
        "Accept-API-Version": "protocol=1.0,resource=1.0",
    };

    let lastFailure = "no attempt was made";

    for (let attempt = 0; attempt < 3; attempt++) {
        const current = await request.get(path, { headers });

        if (current.status() === 404) {
            const created = await request.post(`${path}?_action=create`, {
                headers,
                data: {
                    advancedOAuth2Config: {
                        clientsCanSkipConsent: true,
                        supportedScopes: [SCOPE],
                        defaultScopes: [SCOPE],
                    },
                },
            });
            if (created.ok()) {
                return;
            }
            lastFailure = `create: ${created.status()} ${await created.text()}`;
            continue; // another worker created it first - read it back and merge instead
        }

        expect(current.ok(), `failed to read the OAuth2 provider config: ${current.status()}`).toBeTruthy();

        const config = await current.json();
        const advanced = config.advancedOAuth2Config ?? {};
        const supported = advanced.supportedScopes ?? [];
        const byDefault = advanced.defaultScopes ?? [];
        if (advanced.clientsCanSkipConsent === true && supported.includes(SCOPE) && byDefault.includes(SCOPE)) {
            return;
        }

        // The same three settings the 404 branch above creates, so both paths leave the realm in one state.
        config.advancedOAuth2Config = {
            ...advanced,
            clientsCanSkipConsent: true,
            supportedScopes: [...new Set([...supported, SCOPE])],
            defaultScopes: [...new Set([...byDefault, SCOPE])],
        };
        const updated = await request.put(path, { headers, data: config });
        if (updated.ok()) {
            return;
        }
        lastFailure = `update: ${updated.status()} ${await updated.text()}`;
    }

    throw new Error(`could not enable implied consent on the realm's OAuth2 provider: ${lastFailure}`);
}

async function ensureDeviceClientExists(adminToken, request) {
    const path = `${OPENAM_BASE}/json/realms/${REALM}/realm-config/agents/OAuth2Client/${CLIENT_ID}`;
    const headers = {
        "iPlanetDirectoryPro": adminToken,
        "Content-Type": "application/json",
        "Accept-API-Version": "protocol=2.0,resource=1.0",
    };

    const existing = await request.get(path, { headers });
    if (existing.ok()) {
        return;
    }

    const created = await request.put(path, {
        headers,
        data: {
            "com.forgerock.openam.oauth2provider.clientType": "Public",
            "com.forgerock.openam.oauth2provider.scopes": [`[0]=${SCOPE}`],
            "com.forgerock.openam.oauth2provider.defaultScopes": [`[0]=${SCOPE}`],
            "com.forgerock.openam.oauth2provider.grantTypes": [`[0]=${DEVICE_GRANT}`],
            "com.forgerock.openam.oauth2provider.responseTypes": ["[0]=device_code"],
            "com.forgerock.openam.oauth2provider.tokenEndPointAuthMethod": "none",
            "isConsentImplied": true,
            "sunIdentityServerDeviceStatus": "Active",
        },
    });
    expect(created.ok(), `failed to create the device client: ${created.status()}`).toBeTruthy();
}

/** Starts a device authorization, as the attacker's device would. */
async function startDeviceAuthorization(request) {
    const response = await request.post(`${OPENAM_BASE}/oauth2/realms/${REALM}/device/code`, {
        form: {
            client_id: CLIENT_ID,
            response_type: "device_code",
            scope: SCOPE,
        },
        headers: { "Accept": "application/json" },
    });
    expect(response.ok(), `device authorization failed: ${response.status()}`).toBeTruthy();

    const body = await response.json();
    expect(body).toHaveProperty("user_code");
    expect(body).toHaveProperty("device_code");
    return body;
}

/** One poll of the token endpoint, exactly as the waiting device would do it. */
function pollForTokens(request, deviceCode) {
    return request.post(`${OPENAM_BASE}/oauth2/realms/${REALM}/access_token`, {
        form: {
            grant_type: DEVICE_GRANT,
            client_id: CLIENT_ID,
            device_code: deviceCode,
        },
        headers: { "Accept": "application/json" },
    });
}

/**
 * Asserts the device code has not been bound to anyone. A poll that arrives inside the provider's poll
 * interval answers "slow_down" instead of "authorization_pending"; both mean the same thing here - no tokens.
 */
async function expectStillPending(request, deviceCode, because) {
    const response = await pollForTokens(request, deviceCode);
    expect(response.status(), because).toBe(400);
    const body = await response.json();
    expect(["authorization_pending", "slow_down"], because).toContain(body.error);
}

test.beforeAll(async ({ request }) => {
    const adminToken = await getAdminToken(request);
    if (!adminToken) {
        test.skip("Skipping: could not authenticate as the OpenAM administrator");
    }
    await ensureClientsCanSkipConsent(adminToken, request);
    await ensureDeviceClientExists(adminToken, request);
});

test.describe("OAuth2 device verification endpoint with implied consent", () => {

    // The XUI login form, whose submit button id varies between builds - matched by type, as the xui spec does.
    const LOGIN = {
        username: "#idToken1",
        password: "#idToken2",
        submit: "#loginButton, input[type=\"submit\"], button[type=\"submit\"]",
    };

    /** Types a user code into the rendered form, submits it, and returns the response to the POST. */
    async function submitUserCodeForm(page, userCode) {
        await page.locator("#deviceUserCode").fill(userCode);
        const [response] = await Promise.all([
            page.waitForResponse((r) => r.request().method() === "POST" && r.url().includes("/device/user")),
            page.locator("button[type=\"submit\"]").click(),
        ]);
        return response;
    }

    /** Logs the browser in as the victim, exactly as it would be when a link from another site is followed. */
    async function loginInTheBrowser(page, request) {
        const sessionToken = await getAuthToken(request, USERNAME, PASSWORD);
        expect(sessionToken).toBeTruthy();
        await page.context().addCookies([{
            name: "iPlanetDirectoryPro",
            value: sessionToken,
            domain: new URL(OPENAM_BASE).hostname,
            path: "/",
        }]);
        return sessionToken;
    }

    test("A navigation carrying a user code neither authorizes it nor keeps it in the address",
        async ({ page, request }) => {
            const { user_code, device_code } = await startDeviceAuthorization(request);
            await loginInTheBrowser(page, request);

            // The whole attack from the advisory: one top-level GET, which SameSite=Lax still lets the session
            // cookie ride along on.
            await page.goto(
                `${OPENAM_BASE}/oauth2/realms/${REALM}/device/user?user_code=${encodeURIComponent(user_code)}`);

            // The victim gets an empty form. Filling the code in would leave the link one click from success,
            // on a page that names neither the client nor the scopes it is about to grant.
            const userCodeInput = page.locator("#deviceUserCode");
            await expect(userCodeInput).toBeVisible();
            await expect(userCodeInput).toHaveValue("");

            // The form has no action, so it posts back to the address it was served from. The code is
            // redirected out of that address rather than left in it, or the submission would carry two.
            expect(new URL(page.url()).searchParams.get("user_code"),
                "the address the form is served from must carry no user code").toBeNull();

            await expectStillPending(request, device_code,
                "a mere navigation to the verification endpoint must not authorize the device code");
        });

    test("A link carrying one code cannot decide what the form submits", async ({ page, request }) => {
        const attacker = await startDeviceAuthorization(request);
        const victim = await startDeviceAuthorization(request);
        await loginInTheBrowser(page, request);

        // The query string used to win over the body, which made the code the victim typed irrelevant.
        await page.goto(
            `${OPENAM_BASE}/oauth2/realms/${REALM}/device/user?user_code=${encodeURIComponent(attacker.user_code)}`);
        await submitUserCodeForm(page, victim.user_code);
        await expect(page.locator("#deviceUserCode")).toHaveCount(0);

        // What the user typed is what was authorized, and the link got nothing.
        const tokens = await pollForTokens(request, victim.device_code);
        expect(tokens.ok(), `expected tokens for the code the user typed: ${tokens.status()}`).toBeTruthy();
        await expectStillPending(request, attacker.device_code,
            "the code from the link must not be authorized by a form the user filled in with another code");
    });

    /**
     * The redirect keeps a browser from ever submitting two codes at once. A client that builds the request
     * itself is not bound by it, so the endpoint refuses such a request as well.
     */
    test("A request carrying two different user codes is refused", async ({ request }) => {
        const attacker = await startDeviceAuthorization(request);
        const victim = await startDeviceAuthorization(request);
        const sessionToken = await getAuthToken(request, USERNAME, PASSWORD);

        const response = await request.post(
            `${OPENAM_BASE}/oauth2/realms/${REALM}/device/user`
                + `?user_code=${encodeURIComponent(attacker.user_code)}`, {
                form: { user_code: victim.user_code, csrf: sessionToken },
                headers: { "iPlanetDirectoryPro": sessionToken },
                maxRedirects: 0,
            });
        expect(response.status()).toBe(400);

        await expectStillPending(request, attacker.device_code,
            "a request carrying two different user codes must authorize neither of them");
        await expectStillPending(request, victim.device_code,
            "a request carrying two different user codes must authorize neither of them");
    });

    test("Submitting the rendered form completes the flow", async ({ page, request }) => {
        const { user_code, device_code } = await startDeviceAuthorization(request);
        await loginInTheBrowser(page, request);

        // The verification URI OpenAM hands out carries no code; the user types theirs in.
        await page.goto(`${OPENAM_BASE}/oauth2/realms/${REALM}/device/user`);
        await expect(page.locator("#deviceUserCode")).toBeVisible();

        // The form carries the token that makes its submission distinguishable from a forged one.
        const csrfField = page.locator("input[name=\"csrf\"]");
        await expect(csrfField).toBeAttached();
        expect(await csrfField.inputValue()).not.toBe("");

        await submitUserCodeForm(page, user_code);
        await expect(page.locator("#deviceUserCode")).toHaveCount(0);

        const response = await pollForTokens(request, device_code);
        expect(response.ok(), `expected tokens after the form was submitted: ${response.status()}`).toBeTruthy();
        const tokens = await response.json();
        expect(tokens).toHaveProperty("access_token");

        const userInfo = await request.get(`${OPENAM_BASE}/oauth2/realms/${REALM}/userinfo`, {
            headers: { "Authorization": `Bearer ${tokens.access_token}`, "Accept": "application/json" },
        });
        expect(userInfo.ok()).toBeTruthy();
        expect((await userInfo.json()).sub).toBe(USERNAME);
    });

    test("A direct post without a csrf value is rejected", async ({ request }) => {
        const { user_code, device_code } = await startDeviceAuthorization(request);
        const sessionToken = await getAuthToken(request, USERNAME, PASSWORD);

        const response = await request.post(`${OPENAM_BASE}/oauth2/realms/${REALM}/device/user`, {
            form: { user_code },
            headers: { "iPlanetDirectoryPro": sessionToken },
            maxRedirects: 0,
        });

        expect(response.status()).toBe(400);
        await expectStillPending(request, device_code,
            "a post without a csrf value must not authorize the device code");
    });

    test("A non-browser client completes the flow with its session id as the csrf value", async ({ request }) => {
        const { user_code, device_code } = await startDeviceAuthorization(request);
        const sessionToken = await getAuthToken(request, USERNAME, PASSWORD);

        const response = await request.post(`${OPENAM_BASE}/oauth2/realms/${REALM}/device/user`, {
            form: { user_code, csrf: sessionToken },
            headers: { "iPlanetDirectoryPro": sessionToken },
            maxRedirects: 0,
        });
        expect(response.ok(), `direct submission failed: ${response.status()}`).toBeTruthy();

        const tokens = await pollForTokens(request, device_code);
        expect(tokens.ok(), `expected tokens after the direct submission: ${tokens.status()}`).toBeTruthy();
        expect(await tokens.json()).toHaveProperty("access_token");
    });

    /**
     * The normal way in is a visitor who is not logged in yet: they type their code, the submission is sent
     * to login, and the browser comes back here. The goto used to carry the user code so that the return
     * could authorize it as a GET - the shape this advisory turns on. The GET no longer authorizes anything,
     * so a code left in that goto would only sit in the query string of the form served after login and
     * disagree with whatever the user types next, which gets the submission refused with no way back.
     */
    test("The login a submission without a session triggers does not carry the user code", async ({ page, request }) => {
        const { user_code, device_code } = await startDeviceAuthorization(request);

        // No session cookie is seeded: this browser has never authenticated.
        await page.goto(`${OPENAM_BASE}/oauth2/realms/${REALM}/device/user`);
        const submission = await submitUserCodeForm(page, user_code);

        expect([301, 302, 303, 307],
            `an unauthenticated submission should be redirected to login, got ${submission.status()}`)
            .toContain(submission.status());
        const location = submission.headers()["location"];
        expect(location, "the redirect to login should name where to return to").toContain("goto");
        expect(decodeURIComponent(location), "the login redirect must not carry the user code")
            .not.toContain("user_code");

        await expectStillPending(request, device_code,
            "being sent to login must not authorize the device code");
    });

    /**
     * The whole of that journey, through the login form and back. It is the one path no other test here
     * covers, because every other one starts from a browser that already holds a session.
     */
    test("A visitor who logs in on the way through completes the flow", async ({ page, request }) => {
        const { user_code, device_code } = await startDeviceAuthorization(request);

        await page.goto(`${OPENAM_BASE}/oauth2/realms/${REALM}/device/user`);
        await submitUserCodeForm(page, user_code);

        // Log in where the submission was sent, then follow the goto back to the verification endpoint.
        await expect(page.locator(LOGIN.username)).toBeVisible({ timeout: 20_000 });
        await page.fill(LOGIN.username, USERNAME);
        await page.fill(LOGIN.password, PASSWORD);
        await page.locator(LOGIN.submit).first().click();
        await page.waitForURL((url) => url.pathname.includes("/device/user"), { timeout: 30_000 });

        // The code is asked for again rather than authorized on arrival, and the field is empty: the return
        // is a GET, and a GET is the one request a foreign origin can make this browser send.
        const userCodeInput = page.locator("#deviceUserCode");
        await expect(userCodeInput).toBeVisible();
        await expect(userCodeInput).toHaveValue("");
        await expectStillPending(request, device_code,
            "coming back from login must not authorize the device code on its own");

        // Typing it in now completes the flow - no leftover code in the URL to disagree with.
        await submitUserCodeForm(page, user_code);
        await expect(page.locator("#deviceUserCode")).toHaveCount(0);

        const tokens = await pollForTokens(request, device_code);
        expect(tokens.ok(), `expected tokens after logging in and submitting: ${tokens.status()}`).toBeTruthy();
        expect(await tokens.json()).toHaveProperty("access_token");
    });

    /**
     * The dev guide's curl example repeats the user code in the query string and in the body. Refusing a
     * request that carries two different codes must not refuse one that carries the same code twice.
     */
    test("The documented direct post, with the user code in both the query string and the body, still works",
        async ({ request }) => {
            const { user_code, device_code } = await startDeviceAuthorization(request);
            const sessionToken = await getAuthToken(request, USERNAME, PASSWORD);

            const response = await request.post(
                `${OPENAM_BASE}/oauth2/realms/${REALM}/device/user?user_code=${encodeURIComponent(user_code)}`, {
                    form: { user_code, csrf: sessionToken },
                    headers: { "iPlanetDirectoryPro": sessionToken },
                    maxRedirects: 0,
                });
            expect(response.ok(), `documented direct submission failed: ${response.status()}`).toBeTruthy();

            const tokens = await pollForTokens(request, device_code);
            expect(tokens.ok(), `expected tokens after the documented submission: ${tokens.status()}`).toBeTruthy();
            expect(await tokens.json()).toHaveProperty("access_token");
        });
});
