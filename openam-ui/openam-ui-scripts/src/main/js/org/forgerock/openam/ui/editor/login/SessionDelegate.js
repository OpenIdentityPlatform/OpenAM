/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

/*global $, define, _ */

define("org/forgerock/openam/ui/editor/login/SessionDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/CookieHelper"
], function (Constants, AbstractDelegate, Configuration, CookieHelper) {

    var obj = new AbstractDelegate(Constants.host + "/" + Constants.context + "/json/users");

    obj.logout = function () {
        return obj.serviceCall({
            type: "POST",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            data: "{}",
            url: "",
            serviceUrl: Constants.host + "/" + Constants.context + "/json/sessions?_action=logout",
            errorsHandlers: {"Bad Request": {status: 400}, "Unauthorized": {status: 401}}
        }).always(function () {
            Configuration.loggedUser = null;
            _.each(Configuration.globalData.auth.cookieDomains, function (cookieDomain) {
                CookieHelper.deleteCookie(Configuration.globalData.auth.cookieName, "/", cookieDomain);
            });
        });
    };

    /**
     * Checks if logged in and returns users id
     */
    obj.getProfile = function (successCallback, errorCallback, errorsHandlers) {
        return obj.serviceCall({
            url: "?_action=idFromSession",
            headers: {"Accept-API-Version": "protocol=1.0,resource=2.0"},
            data: "{}",
            type: "POST",
            success: function (data) {
                Configuration.globalData.auth.successURL = data.successURL;
                Configuration.globalData.auth.fullLoginURL = data.fullLoginURL;
                successCallback({realm: data.realm, cn: data.id, roles: ["ui-admin", "ui-user"] });
            },
            error: errorCallback,
            errorsHandlers: errorsHandlers
        });
    };

    return obj;
});