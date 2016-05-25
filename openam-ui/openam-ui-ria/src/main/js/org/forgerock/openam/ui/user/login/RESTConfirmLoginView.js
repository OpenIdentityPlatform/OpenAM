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
 * Copyright 2011-2016 ForgeRock AS.
 */


define([
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/user/login/RESTLoginHelper",
    "org/forgerock/openam/ui/user/services/SessionService",
    "org/forgerock/commons/ui/common/util/CookieHelper"
], function ($, AbstractView, Configuration, restLoginHelper, SessionService, cookieHelper) {

    var ConfirmLoginView = AbstractView.extend({
        template: "templates/openam/RESTConfirmLoginTemplate.html",
        baseTemplate: "templates/common/LoginBaseTemplate.html",

        data: {},
        events: {
            "click button#continueLogin": "continueLogin",
            "click button#logout": "logout"
        },
        render () {
            this.parentRender(function () {
                $("#menu").hide();
                $("#user-nav").hide();
            });
        },
        continueLogin () {
            var href = "#login/";

            $("#menu").show();
            $("#user-nav").show();

            if (Configuration.globalData.auth.subRealm) {
                href += Configuration.globalData.auth.subRealm;
            }
            location.href = href;
            return false;
        },
        logout () {
            var tokenCookie = cookieHelper.getCookie(Configuration.globalData.auth.cookieName);
            SessionService.logout(tokenCookie).then(function () {
                restLoginHelper.removeSessionCookie();
                var realm = (Configuration.globalData.auth.passedInRealm != null)
                                ? Configuration.globalData.auth.passedInRealm
                                : Configuration.globalData.auth.subRealm;
                location.href = `#login/${
                    realm
                    }${restLoginHelper.filterUrlParams(Configuration.globalData.auth.urlParams)}`;
            });
            return false;
        }
    });

    return new ConfirmLoginView();
});
