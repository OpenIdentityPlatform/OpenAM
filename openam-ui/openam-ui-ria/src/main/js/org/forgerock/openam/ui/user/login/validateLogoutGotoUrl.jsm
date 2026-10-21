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
 * Decides where the <code>goto</code> of a <code>#logout/</code> route may send the browser.
 *
 * The goto comes from the address bar, so anyone can put any URL there. Only the server knows the realm's Valid
 * goto URL Resources, and it only answers for a live session, so this has to be asked before the session ends.
 * Without a session there is nothing to ask, and the goto is held to this server: a relative URL or one of the
 * page's own origin.
 * @module org/forgerock/openam/ui/user/login/validateLogoutGotoUrl
 */

import $ from "jquery";
import AuthNService from "org/forgerock/openam/ui/user/services/AuthNService";
import { get as getSessionToken } from "org/forgerock/openam/ui/user/login/tokens/SessionToken";
import { isSessionValid } from "org/forgerock/openam/ui/user/services/SessionService";
import { isHttpUrl, isSameOriginUrl } from "org/forgerock/openam/ui/common/util/uri/redirectUrl";

function ownOrigin (url) {
    return isSameOriginUrl(url) ? url : undefined;
}

/**
 * @param {String} [goto] The goto of the route, URL encoded the way it stands in the address bar.
 * @returns {Promise} Resolved with the decoded URL to send the browser to, or with nothing when it may not be
 * followed. Never rejected.
 */
export default function validateLogoutGotoUrl (goto) {
    if (!goto) {
        return $.Deferred().resolve().promise();
    }

    let url;
    try {
        url = decodeURIComponent(goto);
    } catch (error) {
        return $.Deferred().resolve().promise();
    }

    const sessionToken = getSessionToken();
    if (!sessionToken) {
        return $.Deferred().resolve(ownOrigin(url)).promise();
    }

    const askTheServer = () => AuthNService.validateGotoUrl(goto).then((data) => {
        // The server answers with the goto when it accepts it, and with another URL when it does not.
        if (data && data.successURL === url && isHttpUrl(url)) {
            return url;
        }
        return undefined;
    }, () => ownOrigin(url));

    return isSessionValid(sessionToken).then((isValid) => {
        if (isValid) {
            return askTheServer();
        }
        return ownOrigin(url);
    }, () => ownOrigin(url));
}
