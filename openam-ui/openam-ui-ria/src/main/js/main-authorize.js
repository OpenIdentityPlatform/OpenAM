/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All rights reserved.
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

require.config({

    map: {
        "*" : {
            "ThemeManager" : "org/forgerock/openam/ui/common/util/ThemeManager",
            "Router": "org/forgerock/openam/ui/common/SingleRouteRouter",
            // TODO: Remove this when there are no longer any references to the "underscore" dependency
            "underscore"   : "lodash"
        }
    },

    paths: {
        "lodash":       "libs/lodash-3.10.1-min",
        "handlebars":   "libs/handlebars-3.0.3-min",
        "i18next":      "libs/i18next-1.7.3-min",
        "jquery":       "libs/jquery-2.1.1-min",
        "text":         "libs/text"
    },

    shim: {
        "handlebars": {
            exports: "handlebars"
        },
        "i18next": {
            deps: ["jquery", "handlebars"],
            exports: "i18next"
        },
        "underscore": {
            exports: "_"
        }
    }
});

require([
    "jquery",
    "underscore",
    "handlebars",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/common/util/Constants",
    "text!templates/user/AuthorizeTemplate.html",
    "text!templates/common/LoginBaseTemplate.html",
    "text!templates/common/FooterTemplate.html",
    "text!templates/common/LoginHeaderTemplate.html",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "ThemeManager",
    "Router"
], function ($, _, HandleBars, Configuration, Constants, AuthorizeTemplate,
            LoginBaseTemplate, FooterTemplate, LoginHeaderTemplate, i18nManager, ThemeManager, Router) {

    // Helpers for the code that hasn't been properly migrated to require these as explicit dependencies:
    window.$ = $;
    window._ = _;

    var formTemplate,
        baseTemplate,
        footerTemplate,
        loginHeaderTemplate,
        data = window.pageData || {},
        KEY_CODE_ENTER = 13,
        KEY_CODE_SPACE = 32;

    i18nManager.init({
        paramLang: {
            locale: data.locale || Constants.DEFAULT_LANGUAGE
        },
        defaultLang: Constants.DEFAULT_LANGUAGE,
        nameSpace: "authorize"
    });

    if (data.oauth2Data) {
        _.each(data.oauth2Data.displayScopes, function (obj) {
            if (_.isEmpty(obj.values)) {
                delete obj.values;
            }
            return obj;
        });

        _.each(data.oauth2Data.displayClaims, function (obj) {
            if (_.isEmpty(obj.values)) {
                delete obj.values;
            }
            return obj;
        });

        if (_.isEmpty(data.oauth2Data.displayScopes) && _.isEmpty(data.oauth2Data.displayClaims)) {
            data.noScopes = true;
        }

    } else {
        data.noScopes = true;
    }

    Configuration.globalData = { realm : data.realm };

    Router.currentRoute = {
        navGroup: "user"
    };

    ThemeManager.getTheme().always(function (theme) {
        data.theme = theme;
        baseTemplate = HandleBars.compile(LoginBaseTemplate);
        formTemplate = HandleBars.compile(AuthorizeTemplate);
        footerTemplate = HandleBars.compile(FooterTemplate);
        loginHeaderTemplate = HandleBars.compile(LoginHeaderTemplate);

        $("#wrapper").html(baseTemplate(data));
        $("#footer").html(footerTemplate(data));
        $("#loginBaseLogo").html(loginHeaderTemplate(data));
        $("#content").html(formTemplate(data)).find(".panel-heading").bind("click keyup", function (e) {
            // keyup is required so that the collasped panel can be opened with the keyboard alone,
            // and without relying on a mouse click event.
            if (e.type === "keyup" && e.keyCode !== KEY_CODE_ENTER && e.keyCode !== KEY_CODE_SPACE) {
                return;
            }
            $(this).toggleClass("expanded").next(".panel-collapse").slideToggle();
        });

    });

});
