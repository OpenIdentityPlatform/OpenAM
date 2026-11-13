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
 * OpenAM XUI - self-registration with auto-login
 *
 * A user who registers through the XUI with "auto-login" as the registration destination must end up
 * logged in, and stay logged in across a page reload, in both session cookie modes:
 *   - HttpOnly off: the registration response carries the tokenId and the XUI writes the cookie;
 *   - HttpOnly on (the default): JavaScript cannot write the cookie, so the server sets it on the
 *     registration response and keeps the tokenId out of the body.
 *
 * The spec enables self-registration in the top level realm (no captcha, no email verification,
 * no security questions) and deletes the user it registers.
 */

import { test, expect } from "@playwright/test";
import { OPENAM_BASE, ADMIN_USER, ADMIN_PASS } from "../common/openam-commons.mjs";

const SERVICE_URL = `${OPENAM_BASE}/json/realms/root/realm-config/services/selfService`;
const SERVICE_API = "protocol=1.0,resource=1.0";
const REGISTRATION = {
    userRegistrationEnabled: true,
    userRegisteredDestination: "auto-login",
    userRegistrationCaptchaEnabled: false,
    userRegistrationEmailVerificationEnabled: false,
    userRegistrationKbaEnabled: false,
};

async function getServerInfo(request) {
    const resp = await request.get(`${OPENAM_BASE}/json/serverinfo/*`, {
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
    });
    expect(resp.ok(), "GET /json/serverinfo/* should succeed").toBeTruthy();
    return resp.json();
}

/**
 * Logs in the administrator and returns the SSO token: from the body when the server echoes it,
 * otherwise from the last non-empty session cookie (HttpOnly mode).
 */
async function getAdminSession(request, cookieName) {
    const resp = await request.post(`${OPENAM_BASE}/json/authenticate`, {
        headers: {
            "Content-Type": "application/json",
            "X-OpenAM-Username": ADMIN_USER,
            "X-OpenAM-Password": ADMIN_PASS,
            "Accept-API-Version": "resource=2.0, protocol=1.0",
        },
    });
    expect(resp.ok(), "administrator authentication should succeed").toBeTruthy();
    const body = await resp.json();
    if (body.tokenId) {
        return body.tokenId;
    }
    const token = resp.headersArray()
        .filter((h) => h.name.toLowerCase() === "set-cookie")
        .map((h) => h.value.split(";", 1)[0])
        .filter((pair) => pair.startsWith(`${cookieName}=`))
        .map((pair) => pair.substring(cookieName.length + 1))
        .filter((value) => value.length > 0)
        .pop();
    expect(token, "administrator session must be in the body or the session cookie").toBeTruthy();
    return token;
}

/**
 * Enables self-registration with auto-login, creating the realm's self-service config if needed. The
 * config comes in sections (generalConfig, userRegistration, ...); the service needs its key aliases,
 * which a fresh config leaves empty, so the default keystore's self-service keys are filled in.
 */
async function enableAutoLoginRegistration(request, adminHeaders) {
    const current = await request.get(SERVICE_URL, { headers: adminHeaders });
    const exists = current.status() !== 404;
    const source = exists ? current : await request.post(`${SERVICE_URL}?_action=template`, { headers: adminHeaders });
    expect(source.ok(), `reading the self-service config: ${await source.text()}`).toBeTruthy();
    const config = await source.json();
    delete config._id;
    delete config._rev;
    delete config._type;
    config.generalConfig = {
        ...config.generalConfig,
        encryptionKeyPairAlias: config.generalConfig?.encryptionKeyPairAlias || "selfserviceenctest",
        signingSecretKeyAlias: config.generalConfig?.signingSecretKeyAlias || "selfservicesigntest",
    };
    config.userRegistration = { ...config.userRegistration, ...REGISTRATION };
    const saved = exists
        ? await request.put(SERVICE_URL, {
            headers: { ...adminHeaders, "Content-Type": "application/json" }, data: config })
        : await request.post(`${SERVICE_URL}?_action=create`, {
            headers: { ...adminHeaders, "Content-Type": "application/json" }, data: config });
    expect(saved.ok(), `saving the self-service config: ${await saved.text()}`).toBeTruthy();
}

/** Resolves the username of the session the browser's (auto-sent) cookie carries. */
async function idFromSession(request) {
    const resp = await request.post(`${OPENAM_BASE}/json/users?_action=idFromSession`, {
        headers: { "Accept-API-Version": "protocol=1.0,resource=2.0" },
    });
    return resp.ok() ? (await resp.json()).id : null;
}

test.describe("OpenAM XUI - self-registration", () => {
    test("a user registered with auto-login is logged in and stays logged in after a reload",
        async ({ page, context, request }) => {
            const info = await getServerInfo(request);
            const cookieName = info.cookieName ?? "iPlanetDirectoryPro";
            const httpOnly = info.cookieHttpOnly === true;
            console.log(`Server reports cookieName=${cookieName}, cookieHttpOnly=${httpOnly}`);

            const adminHeaders = {
                [cookieName]: await getAdminSession(request, cookieName),
                "Accept-API-Version": SERVICE_API,
            };
            await enableAutoLoginRegistration(request, adminHeaders);

            const username = `selfreg${Date.now()}`;
            const password = "Selfreg-Passw0rd";
            try {
                // ── 1. Register through the XUI ─────────────────────────────────────
                await page.goto(`${OPENAM_BASE}/XUI/#register/`);
                await expect(page.locator("#input-username")).toBeVisible({ timeout: 30_000 });
                await page.fill("#input-username", username);
                await page.fill("#input-givenName", "Self");
                await page.fill("#input-sn", "Registered");
                // The password validators run asynchronously on keyup, which fill() does not fire, and a
                // stale result for a shorter prefix can land last; a final keyup revalidates the whole value
                for (const field of ["#input-password", "#input-confirmPassword"]) {
                    await page.locator(field).pressSequentially(password, { delay: 20 });
                    await page.waitForTimeout(500);
                    await page.locator(field).press("End");
                }
                const submit = page.locator("input[type=\"submit\"]");
                await expect(submit, "the form must validate before it can be submitted").toBeEnabled();

                const registered = page.waitForResponse((resp) =>
                    resp.url().includes("selfservice/userRegistration")
                    && resp.url().includes("_action=submitRequirements")
                    && resp.status() === 200, { timeout: 30_000 });
                await submit.click();
                const registration = await (await registered).json();

                // ── 2. The session travels as the server intends in this mode ───────
                expect(registration.type, "registration must end in the auto-login stage").toBe("autoLoginStage");
                expect(registration.tag).toBe("end");
                if (httpOnly) {
                    expect(registration.additions?.tokenId,
                        "the registration response must not expose the session in HttpOnly mode").toBeFalsy();
                } else {
                    expect(registration.additions?.tokenId, "the XUI needs the tokenId to write the cookie")
                        .toBeTruthy();
                }

                // ── 3. The browser holds the session cookie with the server's HttpOnly flag ──
                await page.waitForURL((url) => !url.hash.startsWith("#register"), { timeout: 30_000 });
                await expect.poll(async () => (await context.cookies()).find((c) => c.name === cookieName),
                    { message: `session cookie "${cookieName}" must be set`, timeout: 15_000 }).toBeTruthy();
                const session = (await context.cookies()).find((c) => c.name === cookieName);
                expect(session.httpOnly, "cookie HttpOnly attribute must match the server mode").toBe(httpOnly);

                // ── 4. The new user is logged in, before and after a reload ─────────
                expect(String(await idFromSession(page.request)).toLowerCase()).toBe(username.toLowerCase());
                await page.reload({ waitUntil: "networkidle" });
                expect(page.url(), "reload must not redirect to the login page").not.toContain("#login");
                expect(String(await idFromSession(page.request)).toLowerCase()).toBe(username.toLowerCase());
            } finally {
                try {
                    const deleted = await request.delete(`${OPENAM_BASE}/json/realms/root/users/${username}`, {
                        headers: { ...adminHeaders, "Accept-API-Version": "protocol=1.0,resource=2.0" },
                    });
                    console.log(`Deleting ${username}: HTTP ${deleted.status()}`);
                } catch (e) {
                    console.log(`Could not delete ${username}: ${e.message}`);
                }
            }
        });
});
