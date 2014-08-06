/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
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

/*global require, define*/


/**
 * @author mkuleshov
 */

require.config({
    paths: { 

        i18next: "libs/i18next-1.7.3-min",
        backbone: "libs/backbone-0.9.2-min",
        underscore: "libs/underscore-1.4.4-min",
        js2form: "libs/js2form-1.0",
        form2js: "libs/form2js-1.0",
        spin: "libs/spin-1.2.5-min",
        xdate: "libs/xdate-0.7-min",
        doTimeout: "libs/jquery.ba-dotimeout-1.0-min",
        handlebars: "libs/handlebars-1.0.rc.1",
        moment: "libs/moment-1.7.2-min",
        ThemeManager: "org/forgerock/openam/ui/common/util/ThemeManager",
        UserDelegate: "org/forgerock/openam/ui/user/delegates/UserDelegate"
    },

    shim: {
        underscore: {
            exports: "_"
        },
        backbone: {
            deps: ["underscore"],
            exports: "Backbone"
        },
        js2form: {
            exports: "js2form"
        },
        form2js: {
            exports: "form2js"
        },
        spin: {
            exports: "spin"
        },
        xdate: {
            exports: "xdate"
        },
        doTimeout: {
            exports: "doTimeout"
        },
        handlebars: {
            exports: "handlebars"
        },
        i18next: {
            deps: ["handlebars"],
            exports: "i18next"
        },
        moment: {
            exports: "moment"
        }
    }
});

require([   
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/ServiceInvoker",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/util/CookieHelper",  
    "config/main",
    "org/forgerock/commons/ui/common/main",
    "org/forgerock/commons/ui/user/main",
    "org/forgerock/openam/ui/common/main",
    "org/forgerock/openam/ui/user/main"
], function(constants,serviceInvoker,uiUtils,cookieHelper) {

    var 
        conf = {
            defaultHeaders: {}
        },
        urlParams = uiUtils.convertCurrentUrlToJSON().params,
        host = constants.host + "/"+ constants.context + "/json",
        searchParams = window.location.search.substring(1);
        callParams = {
            url: host + urlParams.realm +'/serverinfo/*',
            type: "GET",
            headers: {"Cache-Control": "no-cache"}, 
            success: function() {
                location.href = uiUtils.getCurrentUrlBasePart() + "/"+ constants.context + '/XUI/#continueRegister/&' + searchParams;
            },
            error: function(err) {
                var responseMessage = JSON.parse(err.responseText).message;
                if (responseMessage.indexOf("Invalid realm") > -1) {

                    cookieHelper.cookiesEnabled();
                    var 
                        expire = new Date(),
                        cookieVal =  {
                            realmName : urlParams.realm,
                            valid : false 
                        };
                    
                    expire.setDate(expire.getDate() + 1);
                    cookieHelper.setCookie("invalidRealm",cookieVal,expire);
                    location.href = uiUtils.getCurrentUrlBasePart() + "/"+ constants.context +'/XUI/#login';
                }
            }
        };
        
    if(urlParams.username) {
        location.href = uiUtils.getCurrentUrlBasePart() + "/"+ constants.context +'/XUI/#forgotPasswordChange/&' + searchParams;
    }
    else if(urlParams.realm) {
        serviceInvoker.configuration = conf;
        serviceInvoker.restCall(callParams);
    }
    else if (urlParams.email) {
        location.href = uiUtils.getCurrentUrlBasePart() + "/"+ constants.context + '/XUI/#continueRegister/&' + searchParams;
    }
    else {
        location.href = uiUtils.getCurrentUrlBasePart() + "/"+ constants.context + '/XUI/#login';
    }

});