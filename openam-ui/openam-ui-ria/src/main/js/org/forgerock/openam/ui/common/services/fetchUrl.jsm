/**
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
 * Copyright 2016 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/common/services/fetchUrl
 */
import RealmHelper from "org/forgerock/openam/ui/common/util/RealmHelper";
import store from "store/index";

const hasLeadingSlash = (value) => value[0] === "/";
const throwIfNotAbsoluteRealm = (realm) => {
    if (!hasLeadingSlash(realm)) {
        throw new Error(`[fetchUrl] Realm must be absolute (start with forward slash). "${realm}"`);
    }
};
const throwIfPathHasNoLeadingSlash = (path) => {
    if (!hasLeadingSlash(path)) {
        throw new Error(`[fetchUrl] Path must start with forward slash. "${path}"`);
    }
};
const prefixWithRealmsPath = (realm) => realm.replace(/\//g, "/realms/");
const redesignateIfRootRealm = (realm, to) => { return realm === "/" ? to : realm; };

/**
 * Fetch a URL using the newer method of laying realm information into the URL (e.g. Redux).
 * @param {string} path Path to the resource. Must start with a forward slash.
 * @param {Object} [options] Options to pass to this function.
 * @param {string} [options.realm=store.getState().session.realm] The realm to use when constructing the URL. Must be absolute.
 * @returns {string} URL string to be appended after the <code>.../json</code> path.
 * @throws {Error} If path does not start with a forward slash.
 * @throws {Error} If realm is not absolute (does not start with a forward slash).
 * @example // With session on the root realm
 * fetchUrl.default("/authentication") => "/realms/root/authentication"
 * @example // With session on a sub realm
 * fetchUrl.default("/authentication") => "/realms/root/realms/myRealm/authentication"
 * @example // Forcing a realm
 * fetchUrl.default("/authentication", { realm: "/myRealm" }) => "/realms/root/realms/myRealm/authentication"
 * @example // Forcing no realm
 * fetchUrl.default("/authentication", { realm: false }) => "/authentication"
 */
const fetchUrl = (path, { realm = store.getState().session.realm } = {}) => {
    throwIfPathHasNoLeadingSlash(path);
    if (!realm) { return path; }

    throwIfNotAbsoluteRealm(realm);
    realm = redesignateIfRootRealm(realm, "root"); // TODO "root" is a placeholder. Identifier for a root realm yet undecided.
    realm = prefixWithRealmsPath(realm);

    return realm + path;
};

/**
 * Fetch a URL using the legacy method of laying realm information into the URL (e.g. RealmHelper and global data).
 * @param {string} path Path to the resource. Must start with a forward slash.
 * @param {Object} [options] Options to pass to this function.
 * @param {string} [options.realm=/__subrealm__] The realm to use when constructing the URL. Must be absolute.
 * @returns {string} URL string to be appended after the <code>.../json</code> path.
 * @throws {Error} If path does not start with a forward slash.
 * @throws {Error} If realm is not absolute (does not start with a forward slash).
 * @example // With session on the root realm
 * fetchUrl.legacy("/authentication") => "/authentication"
 * @example // With session on a sub realm
 * fetchUrl.legacy("/authentication") => "/myRealm/authentication"
 * @example // Forcing a realm
 * fetchUrl.legacy("/authentication", { realm: "/myRealm" }) => "myRealm/authentication"
 * @example // Forcing no realm
 * fetchUrl.legacy("/authentication", { realm: false }) => "/authentication"
 */
const fetchUrlLegacy = (path, { realm = "/__subrealm__" } = {}) => {
    throwIfPathHasNoLeadingSlash(path);
    if (!realm) { return path; }

    throwIfNotAbsoluteRealm(realm);
    realm = redesignateIfRootRealm(realm, "");

    return RealmHelper.decorateURIWithRealm(`${realm}${path}`);
};

export { fetchUrl as default };
export { fetchUrlLegacy as legacy };
