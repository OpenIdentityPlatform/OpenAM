/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/**
 * @author JKigwana
 */

/*global window, define, $, document, Handlebars, _ */

define("org/forgerock/openam/ui/policy/PolicyEditorUtils", [
    "handlebars",
    "i18next"
], function (handlebars, i18next) {
    var obj = {};


    Handlebars.registerHelper('policyEditorRealmUrlHelper', function(string) {

        var result = string.slice(1),
            baseSplit = window.location.href.split('?realm='),
            realms = [],
            realmStr = '',
            title = '';
        if(result.length > 0){
            if (baseSplit.length === 2){
                realms = result.split('/');
                result = '';
                // if there are realms
                _.each(realms, function(realm, index, list){

                    realmStr = list.slice(0, index+1);
                    realmStr = realmStr.join('/');

                    // dont add link on last/current realm
                    if (index < list.length-1) {
                        title = $.t("policy.common.viewAllApplications") +' > '+ realm;
                        result += '<span class="realm icon-arrow-right2"></span>'+
                                        '<a title="'+ title +'" href="'+ baseSplit[0] +'?realm=/'+ realmStr +'">'+ realm + '</a>';
                    } else {
                        result += '<span class="realm icon-arrow-right2"></span>' + realm;
                    }

                });
            } else {
                result = '<span class="realm icon-arrow-right2"></span>' + result.replace(/\//g, '<span class="realm icon-arrow-right2"></span>');
            }
        }

        if (baseSplit.length === 2) {
            title = $.t("policy.common.viewAllApplications") +' ('+ $.t("policy.common.topLeveRealm") + ')';
            result ='<span class="realm toplevel">/</span> <a title="'+ title +'" href="'+ baseSplit[0] +'">('+  $.t("policy.common.topLeveRealm") +')</a>' + result;
        } else {
            result = '<span class="realm toplevel">/</span> (Top Level Realm)' + result;
        }

        return new Handlebars.SafeString(result);
    });

    return obj;
});
