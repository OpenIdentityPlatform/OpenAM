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

/*global define, $, _, form2js */

/**
 * @author mbilski
 */
define("org/forgerock/openam/ui/user/profile/ChangeSecurityDataDialog", [
    "org/forgerock/commons/ui/common/components/Dialog",
    "org/forgerock/commons/ui/common/main/ValidatorsManager",
    "org/forgerock/commons/ui/common/main/Configuration",
    "UserDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/user/delegates/SecurityQuestionDelegate"
], function(Dialog, validatorsManager, conf, userDelegate, uiUtils, eventManager, constants, securityQuestionDelegate) {
    var ChangeSecurityDataDialog = Dialog.extend({    
        contentTemplate: "templates/openam/ChangeSecurityDataDialogTemplate.html",
        
        data: {         
            width: 800,
            height: 400
        },
        
        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "customValidate": "customValidate",
            "click .dialogCloseCross img": "close",
            "click input[name='close']": "close",
            "click .dialogContainer": "stop"
        },

        formSubmit: function(event) {
            var data = {};
            
            event.preventDefault();
            
            if(validatorsManager.formValidated(this.$el.find("#passwordChange"))) {            
                data.username = form2js("content", '.', false).username;
                $.extend(data, form2js("passwordChange", '.', false));
                data.userpassword = data.password;
                this.delegate.updateUser(data.username, conf.globalData.auth.realm, data, _.bind(function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "securityDataChanged");
                    this.close();
                }, this), function(e) {
                    if(JSON.parse(e.responseText).message === "Invalid Password"){
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidOldPassword");
                    }
                });

            }
            
        },
        customValidate: function () {

            if(validatorsManager.formValidated(this.$el.find("#passwordChange")) || validatorsManager.formValidated(this.$el.find("#securityDataChange"))) {
                this.$el.find("input[type=submit]").removeClass('inactive').addClass('active');
            }
            else {
                this.$el.find("input[type=submit]").addClass('inactive').removeClass('active');
            }
                
            
        },
        render: function() {
            this.actions = {};
            this.addAction($.t("common.form.update"), "submit");
            
            this.delegate = userDelegate;
            
                this.data.height = 260;
                
            $("#dialogs").hide();
            this.show(_.bind(function() {
                validatorsManager.bindValidators(this.$el);
                $("#dialogs").show();
                this.reloadData();
                
            }, this));
        },
        
        reloadData: function() {
            this.$el.find("input[name=_id]").val(conf.loggedUser.name);
        }
    }); 
    
    return new ChangeSecurityDataDialog();
});