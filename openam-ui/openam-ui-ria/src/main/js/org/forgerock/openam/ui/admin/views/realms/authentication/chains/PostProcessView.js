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


define("org/forgerock/openam/ui/admin/views/realms/authentication/chains/PostProcessView", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/components/BootstrapDialog",
    "handlebars"
], function ($, _, AbstractView, BootstrapDialog, Handlebars) {

    var PostProcessView = AbstractView.extend({
        template: "templates/admin/views/realms/authentication/chains/PostProcessTemplate.html",
        events: {
            "click .delete-btn": "remove",
            "click #addBtn": "add",
            "change #newProcessClass": "change",
            "keyup  #newProcessClass": "change"
        },
        element: "#postProcessView",
        partials: [
            "partials/alerts/_Alert.html"
        ],

        add: function () {
            var newProcessClass = this.$el.find("#newProcessClass").val().trim(),
                invalidName = _.find(this.data.chainData.loginPostProcessClass, function (className) {
                    return className === newProcessClass;
                });
            if (invalidName) {
                this.$el.find("#alertContainer").html(
                    Handlebars.compile("{{> alerts/_Alert type='warning' " +
                        "text='console.authentication.editChains.processingClass.duplicateClass'}}")
                );
            } else {
                this.data.chainData.loginPostProcessClass.push(newProcessClass);
                this.render(this.data.chainData);
            }
        },

        remove: function (e) {
            var index = $(e.currentTarget).closest("tr").index();
            this.data.chainData.loginPostProcessClass[index] = "";
            this.render(this.data.chainData);
        },

        change: function (e) {
            this.$el.find("#addBtn").prop("disabled", (e.currentTarget.value.length === 0));
        },

        addClassNameDialog: function () {
            var self = this,
                promise = $.Deferred(),
                newProcessClass = this.$el.find("#newProcessClass").val().trim();
            if (newProcessClass === "") {
                self.$el.find("#newProcessClass").val("");
                promise.resolve();
            } else {
                BootstrapDialog.show({
                    title: $.t("console.authentication.editChains.processingClass.addClassNameDialog.title"),
                    message: $.t("console.authentication.editChains.processingClass.addClassNameDialog.message",
                        { newClassName: newProcessClass }),
                    closable: false,
                    buttons: [{
                        label: $.t("common.form.cancel"),
                        action: function (dialog) {
                            self.$el.find("#newProcessClass").val("");
                            dialog.close();
                            promise.resolve();
                        }
                    }, {
                        id: "btnOk",
                        label: $.t("common.form.ok"),
                        cssClass: "btn-primary",
                        action: function (dialog) {
                            self.add();
                            dialog.close();
                            promise.resolve();
                        }
                    }]
                });
            }
            return promise;
        },

        render: function (chainData) {
            this.data.chainData = chainData;
            this.parentRender();
        }
    });

    return new PostProcessView();

});
