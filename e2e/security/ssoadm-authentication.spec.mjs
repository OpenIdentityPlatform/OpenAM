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
 * GHSA-hvfc-4v4r-8p9w - ssoadm still authenticates with module based authentication disabled.
 *
 * Disabling module based authentication is part of the fix, and ssoadm used to authenticate by naming
 * the DataStore module, which such a realm refuses. It now authenticates against the authentication
 * Administrator Authentication Configuration of the top level realm - the chain that already serves a
 * browser login to /openam/console, falling back to the Organization Authentication Configuration
 * where the realm names no administrator chain - which names no module at all either way.
 *
 * A subcommand exiting zero proves nothing on its own: Authenticator.ldapLogin() catches a refused
 * remote login and falls back to the internal LoginContext, which authenticates administrators the
 * configuration store knows - amadmin among them. What the fallback cannot hide is the record
 * AuthContext writes when the remote login is refused, so this spec runs a subcommand and requires
 * that the client's authentication debug holds no such record afterwards. Verified against the
 * previous ssoadm, which leaves two of them behind for the same command.
 *
 * `list-sessions` is the one subcommand with no fallback (SessionCommand calls sessionBasedLogin
 * directly), but it goes on to register a session listener, which a remote client is refused here for
 * reasons that have nothing to do with the login, so it is not what is asserted on.
 *
 * The spec drives the real ssoadm in the container the SAML suite already sets up and skips itself
 * where that container is not running.
 */

import { test, expect } from "@playwright/test";
import { execSync } from "child_process";

/** The container the CI job installs the administration tools into. */
const CONTAINER = process.env.OPENAM_SSOADM_CONTAINER ?? "openam-idp";
const TOOLS_DIR = "/usr/openam/ssoadmintools";
const SSOADM_DIR = `${TOOLS_DIR}/openam/bin`;
/** Where AuthContext records a refused remote login on the client side. */
const DEBUG_DIR = `${TOOLS_DIR}/debug`;
const AUTH_DEBUG = `${DEBUG_DIR}/Authentication`;
/** Where the administration tools read their own debug level from, written by ./setup. */
const TOOLS_CONFIG = `${TOOLS_DIR}/resources/bootstrapConfig.properties`;
const DEBUG_CONFIG = "com.iplanet.services.debug.level";
const REFUSED_LOGIN = "Failed to login";
const ADMIN = process.env.OPENAM_ADMIN_USER ?? "amadmin";

function inContainer(command, workingDirectory = "") {
    const directory = workingDirectory === "" ? "" : `-w '${workingDirectory}' `;
    return execSync(`docker exec ${directory}${CONTAINER} bash -c ${JSON.stringify(command)}`, {
        encoding: "utf-8",
        timeout: 120_000,
    });
}

const containerIsRunning = (() => {
    try {
        execSync(`docker exec ${CONTAINER} test -x ${SSOADM_DIR}/ssoadm`, { stdio: "ignore" });
        return true;
    } catch {
        return false;
    }
})();

test.describe("ssoadm authenticates against the administrator chain of the top level realm", () => {

    test.skip(!containerIsRunning, `${CONTAINER} is not running with the administration tools installed`);

    /*
     * What this test asserts is the absence of a record, which is worth nothing unless the tools
     * are writing debug where it looks. Three things establish that, because none of them can be
     * taken on trust: the debug directory exists, the tools are not configured to log nothing, and
     * something in that directory was actually written. The commands carry no "|| true", so a moved
     * directory or an unreadable file fails the test instead of yielding an empty string.
     *
     * One gap is left deliberately rather than papered over: `truncate` creates the Authentication
     * file where it is absent, so a *renamed* file would still leave this reading an empty one. It
     * cannot be closed by requiring the file up front, because a container whose tools have not yet
     * authenticated has not written it, and the check would then fail for the wrong reason.
     *
     * The one thing it cannot establish from here is that a refused login would have been written,
     * because provoking one means breaking the realm for the specs beside this one. Measured by
     * hand instead, on this same image: blanking the Administrator Authentication Configuration of
     * the top level realm makes ssoadm fall back to the organization chain and leaves exactly one
     * "Failed to login" in this file, where a run against the configured realm leaves it empty. So
     * the marker does appear when it should - and note that it appears for the fallback too, which
     * is why this assertion belongs with a realm that names an administrator chain, as a default
     * installation does.
     */
    test("a subcommand authenticates remotely, without falling back", () => {
        inContainer(`test -d ${DEBUG_DIR}`);
        // grep exits non-zero where the file or the setting has moved, which fails the test rather
        // than leaving the assertion below asserting nothing.
        const debugLevel = inContainer(`grep '^${DEBUG_CONFIG}' ${TOOLS_CONFIG}`);
        expect(debugLevel, "the tools log nothing, so the assertion below could not fail")
                .not.toMatch(/=\s*off/i);

        // Empty first, so that what the file holds afterwards belongs to this run alone. Creates it
        // where the tools have not written one yet, which is what a clean container looks like.
        inContainer(`truncate -s 0 ${AUTH_DEBUG}`);

        const output = inContainer(`./ssoadm list-realms --adminid ${ADMIN} --password-file pwd.txt --realm /`,
                SSOADM_DIR);
        // Not merely non-empty: a subcommand that authenticated and reached the server lists the realm.
        expect(output, "ssoadm returned no realm, so it never got as far as the server").toContain("/");

        // The tools write into this directory, so the one file read below is not the only thing
        // standing between a moved debug path and a vacuous pass.
        const written = inContainer(`find ${DEBUG_DIR} -type f -size +0c`);
        expect(written, `nothing under ${DEBUG_DIR} was written, so the tools log somewhere else`)
                .toBeTruthy();

        const authenticationDebug = inContainer(`cat ${AUTH_DEBUG}`);
        expect(authenticationDebug,
                "ssoadm fell back to the internal login, so the remote login was refused")
                .not.toContain(REFUSED_LOGIN);
    });
});
