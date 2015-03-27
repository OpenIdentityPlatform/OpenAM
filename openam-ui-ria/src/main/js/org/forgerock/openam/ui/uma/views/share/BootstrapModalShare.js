/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS.
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

/*global define, $, _ */

define("org/forgerock/openam/ui/uma/views/share/BootstrapModalShare", [
        'org/forgerock/commons/ui/common/main/AbstractView',
        'org/forgerock/openam/ui/uma/views/share/CommonShare',
        "org/forgerock/commons/ui/common/util/UIUtils",
        "org/forgerock/commons/ui/common/main/Configuration"
], function(AbstractView, CommonShare, uiUtils, conf) {

    var BootstrapModalShare = AbstractView.extend({
        template: "templates/uma/bootstrap/BootstrapModalShareTemplate.html",
        element: "#dialogs",

        data: { },

        mode: "append",

        actions: [],

        show: function(callback) {

            this.data.dialogTitle = 'Share the resource';

            this.parentRender(_.bind(function() {      
                var self = this,
                    commonShareView = new CommonShare();

                this.$el.addClass('show');
                this.$el.on('click','#done , #btn-close, #shareButton' , _.bind(this.close, this));

                this.loadContent(callback);
                commonShareView.baseTemplate = 'templates/common/DefaultBaseTemplate.html';
                commonShareView.element = '#dialogs .modal-body';
                commonShareView.render([this.data.currentResourceSetId, true], callback);

                this.$el.find("#modal")
                    .on("hidden.bs.modal", function (e) {
                        self.close(e);
                    })
                    .modal("show");

            }, this));
        },
        loadContent: function(callback) { 
            uiUtils.fillTemplateWithData(this.data.theme.path + this.contentTemplate,
                _.extend({}, conf.globalData, this.data),callback);
        },

        close: function(e) {
            this.$el.find("#modal").modal("hide");
            this.$el.removeClass('show').hide().empty();        
        },

        render: function() {
            this.show();
        }

    });

    return new BootstrapModalShare();
});
