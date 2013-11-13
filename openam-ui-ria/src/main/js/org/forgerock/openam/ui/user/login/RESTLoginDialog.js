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

/*global $, _, define*/

define("org/forgerock/openam/ui/user/login/RESTLoginDialog", [
    "./RESTLoginView",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager", 
    "org/forgerock/commons/ui/common/util/Constants", 
    "org/forgerock/commons/ui/common/components/Dialog",
    "org/forgerock/commons/ui/common/main/SessionManager",
    "org/forgerock/commons/ui/common/main/ViewManager"
], function(loginView, conf, eventManager, constants, Dialog, sessionManager, viewManager) {
    var LoginDialog = Dialog.extend({
        data : {
            width: 512
        },
        actions: {},
        el: '#dialogs',
        parentRender: function (completed) {
            var genericDialog = new Dialog();
            
            this.contentTemplate = this.template;
            this.template = genericDialog.template;
            
            this.setElement($("#dialogs"));
            genericDialog.parentRender.call(this, _.bind(function() {
                this.setElement(this.$el.find(".dialogContainer:last"));
                
                $(".dialog-background").show();
                $(".dialog-background").off('click').on('click', _.bind(this.close, this));
                
                this.loadContent(_.bind(function () {
                    this.resize();
                    completed();
                }, this));
            }, this));
        },
        render: function () {
            this.displayed = true;
            loginView.render.call(this);
        },
        formSubmit: function (e) {
            conf.backgroundLogin = true;
            loginView.formSubmit.call(this,e);
        },
        events: loginView.events,
        selfServiceClick: loginView.selfServiceClick,
        reloadData: loginView.reloadData
    });

    //$.extend(LoginDialog.prototype, _.pick(loginView, 'render', 'formSubmit', 'events'));
    
    return new LoginDialog();
    
});

