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

/*global require, define, $, _*/

define("org/forgerock/openam/ui/common/util/ThemeManager", [
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(constants,conf) {

    var obj = {},
        themeConfigPromise;

    obj.loadThemeCSS = function(theme){

        $('head').find('link[type="image/x-icon"]').remove();
        $('head').find('link[type="text/css"]').remove();

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

         $("<link/>", {
             rel: "stylesheet",
             type: "text/css",
             href: theme.stylesheet
         }).appendTo("head");
    };

    obj.loadThemeConfig = function(){
        if (themeConfigPromise === undefined) {
            themeConfigPromise = $.getJSON(require.toUrl(constants.THEME_CONFIG_PATH));
        }
        return themeConfigPromise;
    };

    obj.getTheme = function(){
        var theme = {},
            newLessVars = {},
            realmDefined = typeof conf.globalData.auth.subRealm !== 'undefined',
            themeName, defaultTheme;

        //find out if the theme has changed
        if (conf.globalData.theme && obj.mapRealmToTheme() === conf.globalData.theme.name) {
            //no change so use the existing theme
            return $.Deferred().resolve(conf.globalData.theme);
        } else {
            return obj.loadThemeConfig().then(function(themeConfig){
                obj.data = themeConfig;
                conf.globalData.themeConfig = obj.updateSrcProperties(themeConfig);
                themeName = obj.mapRealmToTheme();

                theme = _.reject(obj.data.themes,function(t){return t.name !== themeName;})[0];

                if(theme.name !== 'default' && theme.path === ''){
                    defaultTheme = _.reject(obj.data.themes,function(t){return t.name !== 'default';})[0];
                    theme = $.extend(true,{}, defaultTheme, theme);
                }
                obj.loadThemeCSS(theme);
                conf.globalData.theme = theme;
                return theme;
            });
        }
    };

    obj.mapRealmToTheme = function(){
        var testString,
            theme = "default";
        if(conf.globalData.auth.subRealm && conf.globalData.auth.subRealm.substring(1).length !== 0){
            testString = conf.globalData.auth.subRealm.substring(1);
        }
        else{
            testString = document.domain;
        }

        _.each(obj.data.themes,function(t){
            _.each(t.realms,function(r){
                if(t.regex){
                    var patt = new RegExp(r);
                    if(patt.test(testString)){
                        theme = t.name;
                        return false;
                    }
                }
                else{
                    if(r === testString){
                        theme = t.name;
                        return false;
                    }
                }

            });
        });

        return theme;
    };

    obj.updateSrcProperties = function(config) {
        var i;
        if (config.themes && _.isArray(config.themes)) {
            for (i = 0; i < config.themes.length; i++) {
                _.each(config.themes[i].settings, obj.updateSrc);
            }
        } else {
            _.each(config.settings, obj.updateSrc);
        }
    };

    obj.updateSrc = function(value) {
        if (_.has(value, 'src')) {
            value.src = require.toUrl(value.src);
        }
    };


    return obj;
});
