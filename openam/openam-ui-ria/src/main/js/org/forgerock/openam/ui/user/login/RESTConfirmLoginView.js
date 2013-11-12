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

/*global define, $, form2js, _, js2form, Handlebars, window */

define("org/forgerock/openam/ui/user/login/RESTConfirmLoginView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/user/login/AuthNDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/openam/ui/user/login/RESTLoginHelper"
], function(AbstractView, authNDelegate, conf, restLoginHelper) {
    
    var ConfirmLoginView = AbstractView.extend({
        template: "templates/openam/RESTConfirmLoginTemplate.html",
        
        data: {},
        events: {
            "click button#continueLogin": "continueLogin",
            "click button#logout": "logout"
        },
        render: function(args, callback) {
            this.parentRender(function() {
               $('#menu').hide();
               $('#user-nav').hide();
            });
        },
        continueLogin: function(){
            $('#menu').show();
            $('#user-nav').show();
            location.href = "#login" + conf.globalData.auth.realm;
            return false;
        },
        logout: function(){
            authNDelegate.logout().then(function(){
                location.href = "#login" + conf.globalData.auth.passedInRealm + restLoginHelper.filterUrlParams(conf.globalData.auth.urlParams);
            });
            return false;
        }
    }); 
    
    return new ConfirmLoginView();
});


