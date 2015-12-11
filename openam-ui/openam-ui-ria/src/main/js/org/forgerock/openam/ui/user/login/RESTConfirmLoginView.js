/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS.
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


define("org/forgerock/openam/ui/user/login/RESTConfirmLoginView", [
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/user/delegates/AuthNDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/user/login/RESTLoginHelper",
    "org/forgerock/openam/ui/user/delegates/SessionDelegate",
    "org/forgerock/commons/ui/common/util/CookieHelper"
], function ($, AbstractView, authNDelegate, conf, restLoginHelper, sessionDelegate, cookieHelper) {

    var ConfirmLoginView = AbstractView.extend({
        template: "templates/openam/RESTConfirmLoginTemplate.html",
        baseTemplate: "templates/common/LoginBaseTemplate.html",

        data: {},
        events: {
            "click button#continueLogin": "continueLogin",
            "click button#logout": "logout"
        },
        render: function () {
            this.parentRender(function () {
                $("#menu").hide();
                $("#user-nav").hide();
            });
        },
        continueLogin: function () {
            var href = "#login/";

            $("#menu").show();
            $("#user-nav").show();

            if (conf.globalData.auth.subRealm) {
                href += conf.globalData.auth.subRealm;
            }
            location.href = href;
            return false;
        },
        logout: function () {
            var tokenCookie = cookieHelper.getCookie(conf.globalData.auth.cookieName);
            sessionDelegate.logout(tokenCookie).then(function () {
                restLoginHelper.removeSessionCookie();
                var realm = (conf.globalData.auth.passedInRealm != null) ? conf.globalData.auth.passedInRealm
                                                                         : conf.globalData.auth.subRealm;
                location.href = "#login/" + realm + restLoginHelper.filterUrlParams(conf.globalData.auth.urlParams);
            });
            return false;
        }
    });

    return new ConfirmLoginView();
});
