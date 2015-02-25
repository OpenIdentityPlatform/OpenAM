/** 
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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
], function(constants,conf) {
    
    var obj = {},
        themeCSSPromise,
        themeConfigPromise;

    obj.loadThemeCSS = function(theme){

        if (themeCSSPromise === undefined) {
            $('head').find('link[href*=less]').remove();
            $('head').find('link[href*=favicon]').remove();
            
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
            
            themeCSSPromise = $.ajax({
                url: constants.LESS_VERSION,
                dataType: "script",
                cache:true,
                error: function (request, status, error) {
                    console.log(request.responseText);
                }
              });
        }

        return themeCSSPromise;
    };
    
    obj.loadThemeConfig = function(){
        if (themeConfigPromise === undefined) {
            themeConfigPromise = $.getJSON(constants.THEME_CONFIG_PATH);
        }
        return themeConfigPromise;
    };
    
    obj.getTheme = function(){
        var theme = {},
            newLessVars = {},
            themeName, prom, defaultTheme ;
        
        //find out if the theme has changed
        if(conf.globalData.theme && obj.mapRealmToTheme() === conf.globalData.theme.name){
            //no change so use the existing theme
            prom = $.Deferred();
            prom.resolve(conf.globalData.theme);
            return prom;
        } else {
            return obj.loadThemeConfig().then(function(themeConfig){
                obj.data = themeConfig;
                conf.globalData.themeConfig = themeConfig;
                themeName = obj.mapRealmToTheme();

                theme = _.reject(obj.data.themes,function(t){return t.name !== themeName;})[0];
                
                if (theme.name !== 'default' && theme.path === '') {
                    defaultTheme = _.reject(obj.data.themes,function(t){return t.name !== 'default';})[0];
                    theme = $.extend(true,{}, defaultTheme, theme);
                }
                
                return obj.loadThemeCSS(theme).then(function(){
                    _.each(theme.settings.lessVars, function (value, key) {
                        newLessVars['@' + key] = value;
                    });
                    less.modifyVars(newLessVars);
                    
                    conf.globalData.theme = theme;
                    
                    return theme;
                });

            });
        }
    };
    
    obj.mapRealmToTheme = function(){
        var testString, 
            theme = "default";
        if(conf.globalData.auth.realm && conf.globalData.auth.realm.substring(1).length !== 0){
            testString = conf.globalData.auth.realm.substring(1);
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
    
    
    return obj;
});