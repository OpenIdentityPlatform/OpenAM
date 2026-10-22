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
 * GHSA-hvfc-4v4r-8p9w - no session is issued for a credential published in the installer source.
 *
 * The configurator creates the WS-Security agents with a password equal to their own name, and the
 * agent authenticator with "changeit". They are now created inactive, so authentication as any of
 * them must not produce a session. The `anonymous` user of the Top Level Realm shipped with the
 * literal hash of the password "anonymous"; it now holds no password, and SMSAuthModule refuses
 * password authentication for it outright.
 *
 * What this proves: none of these credentials yields a token on a freshly installed server. What it
 * does not distinguish is why - an inactive identity and an identity that never existed both fail
 * the same way, which is the point. The administrator login at the top is the control: it shows the
 * instance authenticates at all, so a blanket failure cannot pass this spec unnoticed.
 *
 * That a deactivated agent is what stops the credential from working is shown separately, in
 * inactive-agent.spec.mjs, on an agent that spec creates and deletes itself. This one stays
 * read-only, so it does not activate a shipped identity even momentarily.
 */

import { test, expect } from "@playwright/test";
import { OPENAM_BASE, ADMIN_USER, ADMIN_PASS } from "../common/openam-commons.mjs";

const APPLICATION_MODULE = "?authIndexType=module&authIndexValue=Application";

/**
 * The status a refused credential comes back with. AMAuthErrorCodeResponseStatusMapping maps
 * AUTH_USER_INACTIVE, AUTH_LOGIN_FAILED and AUTH_USER_NOT_FOUND to 401, but AUTH_MODULE_DENIED to 400
 * and AUTH_MODULE_NOT_FOUND to 404, so asserting on it is what separates "the credential was refused"
 * from "the request never got as far as the credential". It does not separate the two ways the
 * credential itself can be refused: MODULE_BASED_AUTH_NOT_ALLOWED is 401 as well, so a regression in
 * the Application module exemption of AMLoginContext would still pass here.
 */
const REFUSED = 401;

/** The identities AMSetupServlet creates, with the password it creates them with. */
const INSTALLATION_DEFAULTS = [
    { name: "wsc", password: "wsc" },
    { name: "wsp", password: "wsp" },
    { name: "SecurityTokenService", password: "SecurityTokenService" },
    { name: "agentAuth", password: "changeit" },
];

async function authenticate(request, username, password, query = "") {
    const response = await request.post(`${OPENAM_BASE}/json/authenticate${query}`, {
        headers: {
            "Content-Type": "application/json",
            "Accept-API-Version": "resource=2.0, protocol=1.0",
            "X-OpenAM-Username": username,
            "X-OpenAM-Password": password,
        },
        failOnStatusCode: false,
    });

    let body = {};
    try {
        body = await response.json();
    } catch {
        // A failure can come back as an error page rather than JSON; either way there is no token.
    }
    return { status: response.status(), tokenId: body.tokenId };
}

test.describe("default credentials do not authenticate", () => {

    test("the administrator can authenticate, so the instance is up", async ({ request }) => {
        const { status, tokenId } = await authenticate(request, ADMIN_USER, ADMIN_PASS);

        expect(status).toBe(200);
        expect(tokenId).toBeTruthy();
    });

    for (const { name, password } of INSTALLATION_DEFAULTS) {
        test(`${name} is refused its installation password`, async ({ request }) => {
            const { status, tokenId } = await authenticate(request, name, password, APPLICATION_MODULE);

            expect(tokenId, `${name} was issued a session for its installation password (HTTP ${status})`)
                    .toBeFalsy();
            expect(status, `${name} was refused, but not by authentication`).toBe(REFUSED);
        });
    }

    test("anonymous is refused password authentication", async ({ request }) => {
        const { status, tokenId } = await authenticate(request, "anonymous", "anonymous");

        expect(tokenId, `anonymous was issued a session for the password it used to ship with (HTTP ${status})`)
                .toBeFalsy();
        expect(status, "anonymous was refused, but not by authentication").toBe(REFUSED);
    });
});
