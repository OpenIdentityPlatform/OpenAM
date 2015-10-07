/**
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

/*global define*/
define("org/forgerock/openam/ui/admin/views/realms/authentication/ChainsView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/delegates/SMSRealmDelegate",
    "org/forgerock/commons/ui/common/util/UIUtils"
], function ($, _, AbstractView, BootstrapDialog, Router, SMSRealmDelegate, UIUtils) {
    var ChainsView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/ChainsTemplate.html",
        events: {
            "change input[data-chain-name]" : "chainSelected",
            "click  button.delete-chain-btn": "deleteChain",
            "click  #deleteChains"          : "deleteChains",
            "click  #selectAll"             : "selectAll",
            "click  #addChain"              : "addChain"
        },
        addChain: function(e) {
            e.preventDefault();
            var self = this,
                chainName,
                invalidName = false,
                href = $(e.currentTarget).attr("href");

            UIUtils.fillTemplateWithData("templates/admin/views/realms/authentication/chains/AddChainTemplate.html", self.data, function(html) {

                BootstrapDialog.show({
                    title: $.t("console.authentication.chains.createNewChain"),
                    message: $(html),
                    buttons: [{
                        label: $.t("common.form.create"),
                        cssClass: "btn-primary",
                        action: function(dialog) {
                            chainName = dialog.getModalBody().find("#newName").val().trim();

                            // TODO : More client side validation here
                            invalidName = _.find(self.data.sortedChains, function(chain){
                                return chain._id === chainName;
                            });

                            if (invalidName){
                                // FIXME:  This needs to come from a template or partial. This is a temporay fix.
                                var alert = "<div class='alert alert-warning' role='alert'>"+
                                                "<div class='media'>"+
                                                    "<button type='button' class='close' data-dismiss='alert'><span aria-hidden='true'>×</span><span class='sr-only'>"+$.t("common.form.close")+"</span></button>"+
                                                    "<div class='media-left' href=#'>"+
                                                        "<i class='fa fa-exclamation-circle'></i>"+
                                                    "</div>"+
                                                    "<div class='media-body'>"+ $.t("console.authentication.chains.duplicateChain") +"</div>"+
                                                "</div>"+
                                            "</div>";

                                dialog.getModalBody().find("#alertContainer").html(alert);
                            } else {
                                SMSRealmDelegate.authentication.chains.create(self.data.realmPath, { _id: chainName }).done(function() {
                                    dialog.close();
                                    Router.routeTo(Router.configuration.routes.realmsAuthenticationChainEdit, {
                                        args: [encodeURIComponent(self.data.realmPath), encodeURIComponent(chainName)],
                                        trigger: true
                                    });
                                }).fail(function() {
                                    // TODO: Add failure condition
                                });
                            }
                        }
                    }, {
                        label: $.t("common.form.cancel"),
                        action: function(dialog) {
                            dialog.close();
                        }
                    }]
                });
            });
        },
        chainSelected: function(event) {
            var hasChainsSelected = this.$el.find("input[type=checkbox][data-chain-name]").is(":checked"),
                row = $(event.currentTarget).closest("tr"),
                checked = $(event.currentTarget).is(":checked");

            this.$el.find("#deleteChains").prop("disabled", !hasChainsSelected);

            if (checked) {
                row.addClass("selected");
            } else {
                row.removeClass("selected");
                this.$el.find("#selectAll").prop("checked", false);
            }
        },
        selectAll: function(event) {
            var checked = $(event.currentTarget).is(":checked");
            this.$el.find(".sorted-chains input[type=checkbox][data-chain-name]:not(:disabled)").prop("checked", checked);
            if (checked) {
                this.$el.find(".sorted-chains:not(.default-config-row)").addClass("selected");
            } else {
                this.$el.find(".sorted-chains").removeClass("selected");
            }
            this.$el.find("#deleteChains").prop("disabled", !checked);
        },
        deleteChain: function(e) {
            var self = this,
                chainName = $(e.currentTarget).data().chainName;

            SMSRealmDelegate.authentication.chains.remove(this.data.realmPath, chainName).done(function() {
                self.render([self.data.realmPath]);
            });
        },
        deleteChains: function() {
            var self = this,
                chainNames = self.$el.find(".sorted-chains input[type=checkbox][data-chain-name]:checked").toArray().map(function(element) {
                    return $(element).data().chainName;
                }),
                promises = chainNames.map(function(name) {
                    return SMSRealmDelegate.authentication.chains.remove(self.data.realmPath, name);
                });

            $.when(promises).done(function() {
                self.render([self.data.realmPath]);
            });
        },
        render: function (args, callback) {
            var self = this,
                sortedChains = [];

            this.data.realmPath = args[0];

            SMSRealmDelegate.authentication.chains.all(this.data.realmPath).done(function(data) {
                _.each(data.values.result, function(obj) {
                    // Add default chains to top of list.
                    if (obj.active) {
                        sortedChains.unshift(obj);
                    } else {
                        sortedChains.push(obj);
                    }
                });
                self.data.sortedChains = sortedChains;
                self.parentRender(function() {
                    if (callback) {
                        callback();
                    }
                });
            }).fail(function() {
                // TODO: Add failure condition
            });
        }
    });

    return ChainsView;
});
