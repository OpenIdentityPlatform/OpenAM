/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All rights reserved.
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

/*global require, define, $, _, less*/

/**
 * @author huck.elliott
 */
define("ThemeManager", [
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration"
], function (constants, conf) {

    var obj = {},
        themePromise;

    obj.loadThemeCSS = function (theme) {
        var head = $('head');
        head.find('link[href*=less]').remove();
        head.find('link[href*=favicon]').remove();

        $("<link/>", {
            rel: "stylesheet/less",
            type: "text/css",
            href: theme.path + "css/styles.less"
        }).appendTo("head");

        $("<link/>", {
            rel: "icon",
            type: "image/x-icon",
            href: theme.path + theme.icon
        }).appendTo("head");

        $("<link/>", {
            rel: "shortcut icon",
            type: "image/x-icon",
            href: theme.path + theme.icon
        }).appendTo("head");

        return $.ajax({
            url: constants.LESS_VERSION,
            dataType: "script",
            cache: true,
            error: function (request, status, error) {
                console.log(request.responseText);
            }
        });
    };

    obj.loadThemeConfig = function () {
        var prom = $.Deferred();
        //check to see if the config file has been loaded already
        //if so use what is already there if not load it
        if (conf.globalData.themeConfig) {
            prom.resolve(conf.globalData.themeConfig);
            return prom;
        } else {
            return $.getJSON(constants.THEME_CONFIG_PATH);
        }
    };

    obj.getTheme = function () {
        if (themePromise === undefined) {
            themePromise = obj.loadThemeConfig().then(function (themeConfig) {
                var newLessVars = {};

                conf.globalData.theme = themeConfig;
                //the following line is needed to align commons code with idm
                themeConfig.path = "";
                return obj.loadThemeCSS(themeConfig).then(function () {
                    _.each(themeConfig.settings.lessVars, function (value, key) {
                        newLessVars['@' + key] = value;
                    });
                    less.modifyVars(newLessVars);

                    return themeConfig;
                });
            });
        }
        return themePromise;
    };

    return obj;
});