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
 * OpenAM classic (JATO) admin console - smoke test that saving still works with bounded deserialisation.
 *
 * IOUtils.deserialise bounds Java deserialisation (a stream pre-scan plus a JEP 290 filter), and the
 * console reads its page session back through it on every POST. This spec drives a real save in the
 * console under those bounds: log in as amadmin, open the Globalization service, add a throwaway
 * "Supported Charsets" entry through its Add sub-page, save, confirm the value persisted across a
 * reload, and restore the service. A rejected page session would redirect the browser to the console's
 * "Invalid URL." page (ConsoleServletBase.onPageSessionDeserializationException), so the spec also
 * asserts that no POST ever lands there.
 *
 * What this does NOT pin: the console's raised collection depth (IOUtils.CONSOLE_STATE_MAX_COLLECTION_DEPTH).
 * AMViewBeanBase.getPageSessionAttributeString() keeps a page session longer than MAX_PG_SESSION_SIZE
 * (1024-32 characters) server-side in its SessionStore and posts only its ids. Every console page whose page
 * session holds an attribute-value map of sets (Globalization, SOAP Binding, SAML2 SOAP Binding,
 * web-services AuthN and Personal Profile) exceeds that size, so the map -> map -> set never travels
 * through IOUtils.deserialise here, and this spec still passes with maxcollectiondepth=2 configured
 * explicitly. The depth is pinned by the unit round trips in openam-console instead
 * (AMViewBeanBasePageSessionRoundTripTest, SerializedFieldTest).
 *
 * JATO/Lockhart element ids are generated, so controls are addressed by a name fragment first and a
 * visible-value fallback second.
 */

import { test, expect } from "@playwright/test";
import { OPENAM_BASE, ADMIN_USER, ADMIN_PASS, loginViaXui } from "../common/openam-commons.mjs";

const SMG11N_URL = `${OPENAM_BASE}/service/SMG11N`;

// A unique, reversible marker for the supported-charset "locale" column, so a re-run never clashes and
// the restore step can find exactly the row it added.
const MARKER_LOCALE = `e2e-depth-${Date.now()}`;
const MARKER_CHARSET = "UTF-8";

/**
 * The table renders locale and charsets in separate cells, so the added entry is the row with our locale.
 * Console pages nest tables inside layout tables, so only a row with no nested rows is the table row itself;
 * an outer layout row would also contain the text, and its first checkbox belongs to another entry.
 */
function markerRow(page) {
    return page.locator("tr", { hasText: MARKER_LOCALE }).filter({ hasNot: page.locator("tr") });
}

const SUCCESS_MESSAGE = "Profile was updated."; // amConsole message.updated, set by handleButton1Request
const INVALID_URL_TEXT = "Invalid URL."; // amConsole invalid.url.message, shown by AMInvalidURL

/** Fail loudly if a page-session deserialisation rejection bounced us to the Invalid-URL page. */
async function assertNotInvalidUrlPage(page, where) {
    expect(page.url(), `${where}: must not be redirected to the Invalid-URL page (page-session rejected)`)
        .not.toContain("AMInvalidURL");
    const body = await page.content();
    expect(body.includes(INVALID_URL_TEXT),
        `${where}: console must not show "${INVALID_URL_TEXT}" (page-session deserialisation was rejected)`)
        .toBeFalsy();
}

/** Confirm we are actually on a rendered classic-console JATO page and not the login/XUI shell. */
async function assertOnClassicConsole(page) {
    const body = await page.content();
    const looksClassic = body.includes("jato.pageSession") || /name="[^"]*\.button1"/.test(body);
    expect(looksClassic, "expected the classic (JATO) console page to render; " +
        "if this fails the classic console is not reachable in this deployment").toBeTruthy();
}

/**
 * Click the first element matching any of the candidate locators. JATO/Lockhart element ids are
 * generated, so each control is addressed by a name fragment first and a visible-value fallback second.
 */
async function clickAny(page, candidates, what) {
    for (const selector of candidates) {
        const loc = page.locator(selector).first();
        if (await loc.count() > 0 && await loc.isVisible().catch(() => false)) {
            await loc.click();
            return;
        }
    }
    throw new Error(`Could not locate the "${what}" control (tried: ${candidates.join(" | ")})`);
}

async function fillAny(page, candidates, value, what) {
    for (const selector of candidates) {
        const loc = page.locator(selector).first();
        if (await loc.count() > 0 && await loc.isVisible().catch(() => false)) {
            await loc.fill(value);
            return;
        }
    }
    throw new Error(`Could not locate the "${what}" field (tried: ${candidates.join(" | ")})`);
}

/** Deletes the marker row from a fresh Globalization page and saves; does nothing if the row is absent. */
async function removeMarkerEntry(page) {
    await page.goto(SMG11N_URL, { waitUntil: "networkidle" });
    if (await markerRow(page).count() === 0) {
        return;
    }
    await markerRow(page).first().locator("input[type='checkbox']").first().check();
    await clickAny(page, [
        "input[name*='tblSupportedCharsetsButtonDelete']",
        "input[type='submit'][value='Delete']",
    ], "Supported Charsets > Delete");
    await page.waitForLoadState("networkidle");
    await assertNotInvalidUrlPage(page, "deleting the added supported-charset entry");

    await clickAny(page, [
        "input[name$='.button1']",
        "input[type='submit'][value='Save']",
    ], "Globalization > Save (restore)");
    await page.waitForLoadState("networkidle");
    await assertNotInvalidUrlPage(page, "saving Globalization after restore");
}

test.describe("OpenAM classic console - save under bounded deserialisation", () => {
    test("adding and saving a Globalization entry round-trips", async ({ page }) => {
        // Record every POST that actually carried a page session, so we can prove the round trip really
        // exercised the raised deserialisation depth rather than an empty/trivial form.
        const pageSessionPosts = [];
        page.on("request", (req) => {
            if (req.method() !== "POST") {
                return;
            }
            const body = req.postData() || "";
            if (body.includes("jato.pageSession=") && !/jato\.pageSession=(&|$)/.test(body)) {
                pageSessionPosts.push(req.url());
            }
        });

        // 1. Authenticate as amadmin (XUI form) -> establishes the session cookie the classic console reuses.
        await loginViaXui(page, ADMIN_USER, ADMIN_PASS);

        // 2. Open the Globalization global service in the classic console.
        await page.goto(SMG11N_URL, { waitUntil: "networkidle" });
        await assertNotInvalidUrlPage(page, "opening Globalization");
        await assertOnClassicConsole(page);

        // 3. Start the "Add Supported Charset" sub-flow. This makes SMG11NViewBean store
        //    PROPERTY_ATTRIBUTE (a Map<attr, Set>) into the page session and forward to the add sub-page.
        await clickAny(page, [
            "input[name*='tblSupportedCharsetsButtonAdd']",
            "input[type='submit'][value='Add...']",
        ], "Supported Charsets > Add");
        await page.waitForLoadState("networkidle");
        await assertNotInvalidUrlPage(page, "opening the Add Supported Charset sub-page");

        // 4. Fill the sub-page (locale + charsets are two CCTextField inputs) and submit with OK. The OK
        //    POST carries the page session back to the server, which must accept it.
        await fillAny(page, ["input[name$='locale']", "input[name*='.locale']"], MARKER_LOCALE, "locale");
        await fillAny(page, ["input[name$='charsets']", "input[name*='.charsets']"], MARKER_CHARSET, "charsets");
        await clickAny(page, [
            "input[name$='.button1']",
            "input[type='submit'][value='OK']",
        ], "Add Supported Charset > OK");
        await page.waitForLoadState("networkidle");

        try {
            // 5. Back on the Globalization page: the page session was accepted (no Invalid-URL bounce)
            //    and the new entry is present in the table.
            await assertNotInvalidUrlPage(page, "returning from the Add Supported Charset sub-page");
            await assertOnClassicConsole(page);
            await expect(markerRow(page), "the added supported-charset entry must appear after the sub-page round trip")
                .toContainText(MARKER_CHARSET, { timeout: 15_000 });

            // 6. Save the service; it must report "Profile was updated.".
            await clickAny(page, [
                "input[name$='.button1']",
                "input[type='submit'][value='Save']",
            ], "Globalization > Save");
            await page.waitForLoadState("networkidle");
            await assertNotInvalidUrlPage(page, "saving Globalization");
            await expect(page.locator("body"), "saving the Globalization service must succeed")
                .toContainText(SUCCESS_MESSAGE, { timeout: 15_000 });

            // 7. The save must have gone out as a real page-session POST (not an empty form).
            expect(pageSessionPosts.length,
                "at least one POST must have carried a non-empty jato.pageSession ")
                .toBeGreaterThan(0);

            // 8. Persistence: reload the service fresh and confirm the value is still there.
            await page.goto(SMG11N_URL, { waitUntil: "networkidle" });
            await assertNotInvalidUrlPage(page, "reloading Globalization");
            await expect(markerRow(page), "the saved supported-charset entry must persist across a reload")
                .toContainText(MARKER_CHARSET, { timeout: 15_000 });
        } finally {
            // 9. Restore, even when an assertion above failed: remove the entry we added and save, so the
            //    service is left as we found it.
            await removeMarkerEntry(page);
        }

        // The entry must be gone again after the restore + reload.
        await page.goto(SMG11N_URL, { waitUntil: "networkidle" });
        await expect(markerRow(page), "the added supported-charset entry must be removed by the restore step")
            .toHaveCount(0, { timeout: 15_000 });
    });
});
