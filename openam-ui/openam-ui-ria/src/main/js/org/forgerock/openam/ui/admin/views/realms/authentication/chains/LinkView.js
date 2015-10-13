/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions copyright [year] [name of copyright owner]".
 *
 * Copyright 2015 ForgeRock AS.
 */


define("org/forgerock/openam/ui/admin/views/realms/authentication/chains/LinkView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/admin/views/realms/authentication/chains/EditLinkView"
], function ($, _, AbstractView, BootstrapDialog, UIUtils, EditLinkView) {

    var LinkView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/chains/LinkTemplate.html",
        popoverTemplate: "templates/admin/views/realms/authentication/chains/PopoverTemplate.html",
        mode: "replace",
        events: {
            "dblclick .panel": "editItem",
            "click #editBtn": "editItem",
            "click #deleteBtn": "deleteItem",
            "change #selectCriteria": "selectCriteria",
            "mouseenter .btn-auth-module-info": "showPopover",
            "focusin  .btn-auth-module-info":   "showPopover",
            "mouseleave .btn-auth-module-info": "hidePopover",
            "focusout.btn-auth-module-info":    "hidePopover"
        },

        deleteItem: function () {
            this.parent.data.form.chainData.authChainConfiguration.splice(this.$el.index(), 1);
            this.parent.validateChain();
            this.remove();
        },

        editItem: function (e) {
            EditLinkView.show(this);
        },

        showPopover: function (e) {
            var self = this,
                popover = this.$el.find(".btn-auth-module-info").data("bs.popover"),
                selected = this.$el.find("#selectCriteria option:selected"),
                index = selected.index(),
                data = { criteria : selected.val().toLowerCase() };

            if (this.$el.index() >= this.parent.data.form.chainData.authChainConfiguration.length - 1) {
                data.failText = $.t("console.authentication.editChains.lastCriteria");
                data.passText = $.t("console.authentication.editChains.lastCriteria");
                data.last = true;
            } else {
                data.failText = $.t("console.authentication.editChains.criteria." + index + ".fail");
                data.passText = $.t("console.authentication.editChains.criteria." + index + ".pass");
            }

            UIUtils.fillTemplateWithData(self.popoverTemplate, data, function (data) {
                popover.options.content = data;
                popover.options.title = selected.text();
                self.$el.find(".btn-auth-module-info").popover("show");
            });

        },

        hidePopover: function (e) {
            this.$el.find(".btn-auth-module-info").popover("hide");
        },

        selectCriteria: function () {
            this.data.linkConfig.criteria = this.$el.find("#selectCriteria option:selected").val();
            this.parent.validateChain();
        },

        render: function () {
            var self = this;

            this.data.optionsLength = _.keys(this.data.linkConfig.options).length;
            this.data.typeDescription = this.getModuleDesciption();
            this.parentRender(function () {
                self.$el.find(".btn-auth-module-info").popover({
                    trigger : "manual",
                    container : "body",
                    html : "true",
                    placement : "right",
                    template: '<div class="popover am-link-popover" role="tooltip"><div class="arrow"></div>' +
                        '<h3 class="popover-title"></h3><div class="popover-content"></div></div>'
                });
            });

            this.parent.validateChain();
        },

        getModuleDesciption: function () {
            // The server allows for deletion of modules that are in use within a chain.
            // The chain itself will still have a reference to the deleetd module.
            // Below we are checking if the module is present. If it isn't the typeDescription is left blank;
            var name = _.find(this.data.allModules, { _id : this.data.linkConfig.module });

            if (this.data.linkConfig.module && name) {
                return name.typeDescription;
            }

            return;
        }

    });

    return LinkView;

});
