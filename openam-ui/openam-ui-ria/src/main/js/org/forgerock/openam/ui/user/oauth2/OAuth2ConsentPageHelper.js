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
 * Copyright 2016 ForgeRock AS.
 */

/**
 * @module org/forgerock/openam/ui/user/oauth2/OAuth2ConsentPageHelper
 */
define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/RealmHelper",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/common/main/ServiceInvoker"
], function ($, _, AbstractDelegate, Constants, RealmHelper, CookieHelper, ServiceInvoker) {

    const oauth2ContextPath = "oauth2";
    const uriContextParts = Constants.context.split("/");
    const uriContext = uriContextParts.slice(0, uriContextParts.indexOf(oauth2ContextPath));
    const obj = new AbstractDelegate(`${Constants.host}/${uriContext}/json/`);
    const getConfiguration = () => {
        return obj.serviceCall({
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.1" },
            url: RealmHelper.decorateURIWithRealm("__subrealm__/serverinfo/*")
        });
    };

    obj.getUserSessionId = () => {
        ServiceInvoker.updateConfigurationCallback({});
        return getConfiguration().then((config) => {
            return CookieHelper.getCookie(config.cookieName);
        });
    };

    return obj;
});
