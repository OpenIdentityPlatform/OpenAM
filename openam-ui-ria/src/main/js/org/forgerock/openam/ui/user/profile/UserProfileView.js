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

/*global define, $, form2js, _, js2form, document */

/**
 * @author mbilski
 */
define("org/forgerock/openam/ui/user/profile/UserProfileView", [
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "UserDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractView, validatorsManager, uiUtils, userDelegate, eventManager, constants, conf) {
    var UserProfileView = AbstractView.extend({
        template: "templates/openam/UserProfileTemplate.html",
        baseTemplate: "templates/openam/DefaultBaseTemplate.html",
        delegate: userDelegate,
        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate"
        },
        
        formSubmit: function(event) {
            var data = {};
            
            event.preventDefault();
            event.stopPropagation();
            
            if(validatorsManager.formValidated(this.$el)) {
                data = form2js(this.$el.attr("id"), '.', false);
                data._id = data.name;
                this.delegate.updateEntity(data, _.bind(function() {
                    conf.loggedUser = data;
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "profileUpdateSuccessful");
                }, this));
            } else {
                console.log('invalid form');
            }
        },
        
        render: function(args, callback) {
            this.parentRender(function() {
                var self = this;

                validatorsManager.bindValidators(this.$el);
                    
                this.reloadData();

                if(callback) {
                    callback();
                }
                
                
                
            });            
        },
        
        reloadData: function() {
            js2form(document.getElementById(this.$el.attr("id")), conf.loggedUser);
            this.$el.find("input[type=submit]").val($.t("common.form.update"));
            validatorsManager.validateAllFields(this.$el);
        }
    }); 
    
    return new UserProfileView();
});


