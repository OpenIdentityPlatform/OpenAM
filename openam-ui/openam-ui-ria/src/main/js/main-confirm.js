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
 * Portions copyright 2011-2015 ForgeRock AS.
 */

/*global require, define, window*/
require.config({
    map: {
        "*": {
            "ThemeManager": "org/forgerock/openam/ui/common/util/ThemeManager",
            "UserDelegate": "org/forgerock/openam/ui/user/delegates/UserDelegate"
        }
    },
    paths: {
        "backbone": "libs/backbone-1.1.2-min",
        "bootstrap": "libs/bootstrap-3.3.5-custom",
        "bootstrap-dialog": "libs/bootstrap-dialog-1.34.4-min",
        "doTimeout": "libs/jquery.ba-dotimeout-1.0-min",
        "form2js": "libs/form2js-2.0",
        "handlebars": "libs/handlebars-3.0.3-min",
        "i18next": "libs/i18next-1.7.3-min",
        "jquery": "libs/jquery-2.1.1-min",
        "js2form": "libs/js2form-2.0",
        "moment": "libs/moment-2.8.1-min",
        "spin": "libs/spin-2.0.1-min",
        "underscore": "libs/lodash-2.4.1-min",
        "xdate": "libs/xdate-0.8-min"
    },
    shim: {
        "backbone": {
            deps: ["underscore"],
            exports: "Backbone"
        },
        "bootstrap": {
            deps: ["jquery"]
        },
        "bootstrap-dialog": {
            deps: ["jquery", "underscore", "backbone", "bootstrap"]
        },
        "doTimeout": {
            deps: ["jquery"],
            exports: "doTimeout"
        },
        "form2js": {
            exports: "form2js"
        },
        "i18next": {
            deps: ["jquery", "handlebars"],
            exports: "i18next"
        },
        "js2form": {
            exports: "js2form"
        },
        "moment": {
            exports: "moment"
        },
        "spin": {
            exports: "spin"
        },
        "underscore": {
            exports: "_"
        },
        "xdate": {
            exports: "xdate"
        }
    }
});

require([
    "underscore",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/ServiceInvoker",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/openam/ui/common/util/RealmHelper",
    "org/forgerock/commons/ui/common/main",
    "org/forgerock/openam/ui/main",
    "config/main"
], function (_, Constants, ServiceInvoker, UIUtils, CookieHelper, RealmHelper) {
    var conf = {
            defaultHeaders: {}
        },
        callParams,
        responseMessage,
        urlParams = UIUtils.convertCurrentUrlToJSON().params,
        basePath = _.initial(Constants.context.split("/")).join("/"),
        host = Constants.host + "/" + basePath + "/json/",
        searchParams = window.location.search.substring(1);

    callParams = {
        url: host + RealmHelper.decorateURIWithRealm("__subrealm__/serverinfo/*"),
        type: "GET",
        headers: { "Cache-Control": "no-cache" },
        success: function () {
            window.location.href = UIUtils.getCurrentUrlBasePart() + "/" + basePath
                + "/XUI/#continueRegister/&" + searchParams;
        },
        error: function (err) {
            responseMessage = JSON.parse(err.responseText).message;
            if (responseMessage.indexOf("Invalid realm") > -1) {
                CookieHelper.cookiesEnabled();
                var expire = new Date(),
                    cookieVal = {
                        realmName: urlParams.realm,
                        valid: false
                    };

                expire.setDate(expire.getDate() + 1);
                CookieHelper.setCookie("invalidRealm", cookieVal, expire);
                window.location.href = UIUtils.getCurrentUrlBasePart() + "/" + basePath + "/XUI/#login";
            }
        }
    };

    if (urlParams.username) {
        window.location.href = UIUtils.getCurrentUrlBasePart() + "/" + basePath
            + "/XUI/#forgotPasswordChange/&" + searchParams;
    } else if (urlParams.realm) {
        ServiceInvoker.configuration = conf;
        ServiceInvoker.restCall(callParams);
    } else if (urlParams.email) {
        window.location.href = UIUtils.getCurrentUrlBasePart() + "/" + basePath
            + "/XUI/#continueRegister/&" + searchParams;
    } else {
        window.location.href = UIUtils.getCurrentUrlBasePart() + "/" + basePath
            + "/XUI/#login";
    }
});
