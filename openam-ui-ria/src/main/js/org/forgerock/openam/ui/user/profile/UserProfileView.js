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

/*global window, define, $, form2js, _, js2form, document, console */

/**
 * @author mbilski
 */
define("org/forgerock/openam/ui/user/profile/UserProfileView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/components/Navigation",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractView, validatorsManager, uiUtils, userDelegate, router, navigation, eventManager, constants, conf) {
    var UserProfileView = AbstractView.extend({
        template: "templates/openam/UserProfileTemplate.html",
        delegate: userDelegate,
        events: {
            "click input[name=saveButton]": "formSubmit",
            "click input[name=resetButton]": "reloadData",
            "onValidate": "onValidate"
        },
        
        formSubmit: function(event) {
            var data = {};
            
            event.preventDefault();
            event.stopPropagation();
            
            if(validatorsManager.formValidated(this.$el)) {
                data = form2js(this.$el.attr("id"), '.', false);
                this.delegate.updateUser(data.username, conf.globalData.auth.realm, data, _.bind(function() {
                    $.extend(conf.loggedUser, data);
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateSuccessful");
                }, this));
            } else {
                console.log('invalid form');
            }
        },
        
        render: function(args, callback) {
            
            this.parentRender(function() {
                validatorsManager.bindValidators(this.$el);
                    
                this.reloadData();

                if(callback) {
                    callback();
                }
                
                if (window.location.hash !== "#" + router.getLink(router.configuration.routes.profile)) {
                    eventManager.sendEvent(constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.profile });
                }
                
            });
        },
        
        reloadData: function() {
            js2form(document.getElementById(this.$el.find("#UserProfileForm").attr("id")), conf.loggedUser);
            this.$el.find("input[name=saveButton]").val($.t("common.form.update"));
            this.$el.find("input[name=resetButton]").val($.t("common.form.reset"));
            validatorsManager.validateAllFields(this.$el);
        }
    }); 
    
    return new UserProfileView();
});


