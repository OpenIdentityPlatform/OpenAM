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

/*global define, $, form2js, _, js2form, window */

define("org/forgerock/openam/ui/user/login/RESTLoginView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/user/login/AuthNDelegate",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/util/CookieHelper",
    "org/forgerock/commons/ui/user/delegates/SiteIdentificationDelegate",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractView, authNDelegate, validatorsManager, eventManager, constants, cookieHelper, siteIdentificationDelegate, conf) {
    var LoginView = AbstractView.extend({
        template: "templates/openam/RESTLoginTemplate.html",
        baseTemplate: "templates/user/LoginBaseTemplate.html",
        
        events: {
            "click input[type=submit]": "formSubmit"
        },
        formSubmit: function (e) {
            e.preventDefault();
            
            eventManager.sendEvent(constants.EVENT_LOGIN_REQUEST, form2js(this.$el[0]));
        },
        render: function(args, callback) {
            var loginProcess;
            
            if (window.location.hash === "#logout/") {
                eventManager.sendEvent(constants.EVENT_LOGOUT);
            }
            
            loginProcess = authNDelegate.getRequirements();
            
            loginProcess.done(_.bind(function (reqs) {
                var cleaned = _.clone(reqs);
                cleaned.callbacks = [];
                _.each(reqs.callbacks, function(element) {
                    cleaned.callbacks.push({
                        input: {
                            i18n_key: "openam.authentication.input." + element.input[0].name,
                            name: element.input[0].name,
                            value: element.input[0].value
                        },
                        output: element.output[0],
                        type: element.type
                    });
                });
                this.reqs = reqs;
                this.data = cleaned;
                
                this.parentRender(function() {
                    validatorsManager.bindValidators(this.$el);
                    this.$el.find(":input:first").focus();
                    
                    if(callback) {
                        callback();
                    }
                });
            }, this));

        }
    }); 
    
    return new LoginView();
});


