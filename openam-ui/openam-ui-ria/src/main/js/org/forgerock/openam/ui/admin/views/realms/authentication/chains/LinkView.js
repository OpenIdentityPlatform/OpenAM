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
    "handlebars",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($, _, AbstractView, BootstrapDialog, Handlebars, UIUtils) {

    var LinkView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/chains/LinkTemplate.html",
        popoverTemplate: "templates/admin/views/realms/authentication/chains/PopoverTemplate.html",
        mode: "replace",
        partials : ["templates/admin/views/realms/authentication/chains/_CriteriaFooter.html"],
        events: {
            "click #editBtn": "editItem",
            "click #deleteBtn": "deleteItem",
            "change #selectCriteria": "selectCriteria",
            "mouseenter .auth-criteria-info": "showPopover",
            "focusin    .auth-criteria-info": "showPopover",
            "mouseleave .auth-criteria-info": "hidePopover",
            "focusout   .auth-criteria-info": "hidePopover"
        },

        deleteItem: function () {
            this.parent.data.form.chainData.authChainConfiguration.splice(this.$el.index(), 1);
            this.parent.validateChain();
            this.remove();
        },

        editItem: function () {
            this.parent.editItem(this);
        },

        showPopover: function () {
            var self = this,
                popover = this.$el.find(".auth-criteria-info").data("bs.popover"),
                selected = this.$el.find("#selectCriteria option:selected"),
                index = selected.index(),
                data = {
                    criteria : selected.val().toLowerCase(),
                    passText : $.t("console.authentication.editChains.criteria." + index + ".passText"),
                    failText : $.t("console.authentication.editChains.criteria." + index + ".failText")
                };

            UIUtils.fillTemplateWithData(self.popoverTemplate, data, function (data) {
                popover.options.content = data;
                popover.options.title = selected.text();
                self.$el.find(".auth-criteria-info").popover("show");
            });

        },

        hidePopover: function () {
            this.$el.find(".auth-criteria-info").popover("hide");
        },

        renderArrows: function () {
            var html = Handlebars.compile(
                    "{{> templates/admin/views/realms/authentication/chains/_CriteriaFooter type='" +
                    this.data.linkConfig.criteria + "'}}"
                );

            this.$el.find(".criteria-view").html(html);
        },

        selectCriteria: function () {
            this.data.linkConfig.criteria = this.$el.find("#selectCriteria option:selected").val();
            this.parent.validateChain();
            this.renderArrows();
        },

        render: function () {
            var self = this;

            this.data.optionsLength = _.keys(this.data.linkConfig.options).length;
            this.data.typeDescription = this.getModuleDesciption();
            this.parent.validateChain();

            this.parentRender(function () {
                self.renderArrows();
                self.$el.find(".auth-criteria-info").popover({
                    trigger : "manual",
                    container : "body",
                    html : "true",
                    placement : "right",
                    template: '<div class="popover am-link-popover" role="tooltip"><div class="arrow"></div>' +
                        '<h3 class="popover-title"></h3><div class="popover-content"></div></div>'
                });
            });
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
