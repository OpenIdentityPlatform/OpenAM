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
 * Copyright 2015 ForgeRock AS.
 */

/*global require, define, window*/
require.config({
    map: {
        "*" : {
            "ThemeManager" : "org/forgerock/openam/ui/common/util/ThemeManager"
        }
    },
    paths: {
        "underscore":   "libs/lodash-2.4.1-min",
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
    "handlebars",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/common/util/Constants",
    "text!templates/user/DeviceTemplate.html",
    "text!templates/user/DeviceDoneTemplate.html",
    "text!templates/common/LoginBaseTemplate.html",
    "text!templates/common/FooterTemplate.html",
    "org/forgerock/commons/ui/common/main/i18nManager",
    "ThemeManager",
    "org/forgerock/commons/ui/common/util/URIUtils"
], function ($, HandleBars, Configuration, Constants, DeviceTemplate, DeviceDoneTemplate,
            LoginBaseTemplate, FooterTemplate, i18nManager, ThemeManager, URIUtils) {
    var data = window.pageData,
        template = data.done ? DeviceDoneTemplate : DeviceTemplate;

    i18nManager.init({
        paramLang: {
            locale: data.locale
        },
        defaultLang: Constants.DEFAULT_LANGUAGE,
        nameSpace: "device"
    });

    Configuration.globalData = { realm : data.realm };

    ThemeManager.getTheme().always(function (theme) {
        data.theme = theme;

        $("#wrapper").html(HandleBars.compile(LoginBaseTemplate)(data));
        $("#footer").html(HandleBars.compile(FooterTemplate)(data));
        $("#content").html(HandleBars.compile(template)(data));
    });
});
