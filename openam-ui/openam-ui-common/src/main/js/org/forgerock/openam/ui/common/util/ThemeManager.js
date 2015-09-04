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

/*global require, define */

define("org/forgerock/openam/ui/common/util/ThemeManager", [
    "jquery",
    "underscore",
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration"
], function ($, _, Constants, Configuration) {

    /**
     * @exports org/forgerock/openam/ui/common/util/ThemeManager
     */
    var obj = {},
        promise = null,
        applyThemeToPage = function (theme) {
            // We might be switching themes (due to a realm change) and so we need to clean up the previous theme.
            $("link").remove();

            $("<link/>", {
                rel: "icon",
                type: "image/x-icon",
                href: require.toUrl(theme.path + theme.icon)
            }).appendTo("head");

            $("<link/>", {
                rel: "shortcut icon",
                type: "image/x-icon",
                href: require.toUrl(theme.path + theme.icon)
            }).appendTo("head");

            _.each(theme.stylesheets, function(stylesheet) {
                $("<link/>", {
                    rel: "stylesheet",
                    type: "text/css",
                    href: stylesheet
                }).appendTo("head");
            });
        },

        /**
         * Loads the theme configuration json.
         * <p> Returns promise of theme configuration if it has not already been requested.
         * @returns {object} promise containing the theme configuration object
         */
        loadThemeConfig = function () {
            if (promise === null) {
                promise = $.getJSON(require.toUrl(Constants.THEME_CONFIG_PATH));
            }
            return promise;
        },

        /**
         * Find the appropriate theme for the current realm.
         * <p>
         * If a theme is found that matches the current realm then its name will be
         * returned, otherwise the default theme name will be returned.
         * @returns {string} theme The selected theme configuration name.
         */
        getThemeForCurrentRealm = function () {
            var matchedTheme = _.find(obj.data.themes, function (theme) {
                return _.some(theme.realms, function (realm) {
                    if (theme.regex) {
                        var pattern = new RegExp(realm);
                        return pattern.test(Configuration.globalData.realm);
                    } else {
                        return realm === Configuration.globalData.realm;
                    }
                });
            });
            if (matchedTheme) {
                return matchedTheme.name;
            }

            return "default";
        },

        updateSrc = function (value) {
            if (_.has(value, "src")) {
                value.src = require.toUrl(value.src);
            }
        },

        updateSrcProperties = function (config) {
            var i;
            if (config.themes && _.isArray(config.themes)) {
                for (i = 0; i < config.themes.length; i++) {
                    _.each(config.themes[i].settings, updateSrc);
                }
            } else {
                _.each(config.settings, updateSrc);
            }
        },

        isRelativePath = function (path) {
            return path.indexOf("http://") !== 0 &&
                path.indexOf("https://") !== 0 &&
                path.indexOf("/") !== 0;
        };

    /**
     * Determine the theme from the themeConfig and the current realm and setup the theme on the page. This will
     * clear out any previous theme.
     * @param {string} [basePath] A URL to which all relative URLs in the theme will be made relative to.
     * @returns {Promise} a promise that is resolved when the theme has been applied.
     */
    obj.getTheme = function (basePath) {
        var theme = {},
            themeName, defaultTheme;

        // find out if the theme has changed
        if (Configuration.globalData.theme && getThemeForCurrentRealm() === Configuration.globalData.theme.name) {
            //no change so use the existing theme
            return $.Deferred().resolve(Configuration.globalData.theme);
        } else {
            return loadThemeConfig().then(function (themeConfig) {
                obj.data = themeConfig;
                Configuration.globalData.themeConfig = updateSrcProperties(themeConfig);
                themeName = getThemeForCurrentRealm();
                theme = _.find(obj.data.themes, { name: themeName });

                if (theme.name !== "default" && theme.path === "") {
                    defaultTheme = _.find(obj.data.themes, { name: "default" });
                    theme = _.merge({}, defaultTheme, theme, function (objectValue, sourceValue) {
                        // We don't want to merge arrays. If a theme has specified an array, it should be used verbatim.
                        if (_.isArray(sourceValue)) {
                            return sourceValue;
                        }
                        return undefined;
                    });
                }
                if (basePath) {
                    theme.stylesheets = _.map(theme.stylesheets, function (url) {
                        if (isRelativePath(url)) {
                            return basePath + url;
                        } else {
                            return url;
                        }
                    });
                }

                applyThemeToPage(theme);
                Configuration.globalData.theme = theme;
                return theme;
            });
        }
    };

    return obj;
});
