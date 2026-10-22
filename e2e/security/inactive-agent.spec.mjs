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
 * GHSA-hvfc-4v4r-8p9w - a deactivated agent does not authenticate.
 *
 * The fix for the advisory creates the WS-Security agents and the agent authenticator inactive rather than
 * changing their passwords, so everything rests on a deactivated agent being unable to authenticate even with the
 * right password. default-credentials.spec.mjs shows that the shipped agents get no session, but it cannot show
 * why, because an identity that never existed fails the same way.
 *
 * This spec closes that gap on an agent it creates itself: the same credentials are refused after nothing but the
 * status changing. It deliberately does not care which layer refuses them - today it is LoginState, reading the
 * status of the identity while it builds the session - only that deactivating an agent is enough.
 *
 * The agent is created for this spec alone and deleted afterwards, so no shipped identity is activated even
 * momentarily and the read-only spec running beside it is unaffected.
 */

import { test, expect, request as apiRequest } from "@playwright/test";
import { OPENAM_BASE, getAdminToken } from "../common/openam-commons.mjs";

/** Created and deleted by this spec. The name is unlikely to collide with anything a deployment has. */
const AGENT = "ghsa-hvfc-status-probe";
/** Random, used nowhere else, and the agent is deleted at the end of the spec. */
const AGENT_PASSWORD = "Rk8zQv2ThYs4Wm7Q";

const APPLICATION_MODULE = "?authIndexType=module&authIndexValue=Application";
/** The identity endpoints of /json/agents. */
const IDENTITY_VERSION = "protocol=1.0,resource=2.1";
/** AUTH_USER_INACTIVE, like the other ways authentication can refuse a credential, comes back as 401. */
const REFUSED = 401;
const STATUS = "sunIdentityServerDeviceStatus";

let api;
let adminToken;

async function authenticate(username, password) {
    const response = await api.post(`${OPENAM_BASE}/json/authenticate${APPLICATION_MODULE}`, {
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

function identityHeaders() {
    return {
        "iPlanetDirectoryPro": adminToken,
        "Content-Type": "application/json",
        "Accept-API-Version": IDENTITY_VERSION,
    };
}

async function createAgent() {
    return api.post(`${OPENAM_BASE}/json/agents/?_action=create`, {
        headers: identityHeaders(),
        data: {
            username: AGENT,
            userpassword: [AGENT_PASSWORD],
            // The plainest agent there is: the installer creates the agent authenticator this way, with a
            // password and nothing else that has to be valid.
            AgentType: ["SharedAgent"],
            [STATUS]: ["Active"],
        },
        failOnStatusCode: false,
    });
}

async function setAgentStatus(status) {
    return api.put(`${OPENAM_BASE}/json/agents/${AGENT}`, {
        headers: identityHeaders(),
        data: { [STATUS]: [status] },
        failOnStatusCode: false,
    });
}

async function readAgentStatus() {
    const response = await api.get(`${OPENAM_BASE}/json/agents/${AGENT}`, {
        headers: identityHeaders(),
        failOnStatusCode: false,
    });
    const body = await response.json();
    // The attribute comes back under the name the agent profile stores it as, whose case has changed between
    // releases, so it is looked up without case.
    const name = Object.keys(body).find((key) => key.toLowerCase() === STATUS.toLowerCase());
    return name === undefined ? [] : [].concat(body[name]);
}

async function deleteAgent() {
    return api.delete(`${OPENAM_BASE}/json/agents/${AGENT}`, {
        headers: identityHeaders(),
        failOnStatusCode: false,
    });
}

test.describe.configure({ mode: "serial" });

test.describe("a deactivated agent does not authenticate", () => {

    test.beforeAll(async () => {
        api = await apiRequest.newContext({ ignoreHTTPSErrors: true });
        adminToken = await getAdminToken(api);
        expect(adminToken, "could not authenticate as the administrator").toBeTruthy();

        // A run interrupted between the two tests below leaves the agent behind, deactivated.
        await deleteAgent();

        const created = await createAgent();
        expect(created.ok(), `the probe agent could not be created (HTTP ${created.status()})`).toBeTruthy();
    });

    test.afterAll(async () => {
        if (api !== undefined) {
            await deleteAgent();
            await api.dispose();
        }
    });

    test("authenticates while it is active", async () => {
        expect(await readAgentStatus()).toContain("Active");

        const { status, tokenId } = await authenticate(AGENT, AGENT_PASSWORD);

        expect(status, "an active agent was refused its own password").toBe(200);
        expect(tokenId, "an active agent was issued no session").toBeTruthy();
    });

    test("is refused the same password once it is deactivated", async () => {
        const updated = await setAgentStatus("Inactive");
        expect(updated.ok(), `the probe agent could not be deactivated (HTTP ${updated.status()})`).toBeTruthy();
        // Without this the test would also pass where the write went nowhere and the agent stayed active but
        // authentication broke for an unrelated reason.
        expect(await readAgentStatus(), "the probe agent is not actually inactive").toContain("Inactive");

        const { status, tokenId } = await authenticate(AGENT, AGENT_PASSWORD);

        expect(tokenId, `a deactivated agent was issued a session (HTTP ${status})`).toBeFalsy();
        expect(status, "the deactivated agent was refused, but not by authentication").toBe(REFUSED);
    });
});
