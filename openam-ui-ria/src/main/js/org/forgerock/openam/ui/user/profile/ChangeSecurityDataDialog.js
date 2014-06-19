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
    "org/forgerock/commons/ui/common/util/Constants"
], function(Dialog, validatorsManager, conf, userDelegate, uiUtils, eventManager, constants) {
    var ChangeSecurityDataDialog = Dialog.extend({    
        contentTemplate: "templates/openam/ChangeSecurityDataDialogTemplate.html",
        
        data: { },
        
        events: {
            "click input[type=submit]": "formSubmit",
            "onValidate": "onValidate",
            "customValidate": "customValidate",
            "click .dialogCloseCross img": "close",
            "click input[name='close']": "close",
            "click .dialogContainer": "stop"
        },

        errorsHandlers: {
            "Bad Request":              { status: "400" }
        },

        formSubmit: function(event) {
            var data = {}, _this = this;
            
            event.preventDefault();
            
            if (validatorsManager.formValidated(this.$el.find("#passwordChange"))) {            
                data.username = form2js("content", '.', false).uid;
                data.currentpassword = this.$el.find("#currentPassword").val(); 
                data.userpassword =  this.$el.find("#password").val();
                this.delegate.changePassword(conf.loggedUser, data, _.bind(function() {
                    eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "securityDataChanged");
                    _this.close();
                }, this), function(e) {
                    if(JSON.parse(e.responseText).message === "Invalid Password"){
                        eventManager.sendEvent(constants.EVENT_DISPLAY_MESSAGE_REQUEST, "invalidOldPassword");
                        _this.close();
                    }
                },_this.errorsHandlers);

            }
            
        },
        customValidate: function () {
            if (validatorsManager.formValidated(this.$el.find("#passwordChange")) || validatorsManager.formValidated(this.$el.find("#securityDataChange"))) {
                this.$el.find("input[type=submit]").prop('disabled', false);
            }
            else {
                this.$el.find("input[type=submit]").prop('disabled', true);
            } 
        },
        render: function() {
            this.actions = [];
            this.addAction($.t("common.form.update"), "submit");
            this.delegate = userDelegate;

            $("#dialogs").hide();
            this.show(_.bind(function() {
                validatorsManager.bindValidators(this.$el);
                $("#dialogs").show();
                this.reloadData();
                
            }, this));      
        },
        
        reloadData: function() {
            this.$el.find("input[name=_id]").val(conf.loggedUser.name);
            this.$el.find("input[type=submit]").prop('disabled', true);
        }
    }); 
    
    return new ChangeSecurityDataDialog();
});