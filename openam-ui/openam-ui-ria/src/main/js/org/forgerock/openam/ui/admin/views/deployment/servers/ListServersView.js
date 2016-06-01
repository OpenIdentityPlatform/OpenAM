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
 * Copyright 2016 ForgeRock AS.
 */

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/admin/utils/form/showConfirmationBeforeDeleting",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/admin/services/global/ServersService",
    "org/forgerock/openam/ui/common/components/TemplateBasedView",
    "org/forgerock/openam/ui/admin/views/common/ToggleCardListView"
], ($, _, AbstractView, showConfirmationBeforeDeleting, Messages, ServersService, TemplateBasedView,
    ToggleCardListView) => {

    const ListServersView = AbstractView.extend({
        template: "templates/admin/views/deployment/servers/ListServersTemplate.html",
        events: {
            "click [data-delete-item]" : "onDelete"
        },
        partials: [
            "partials/util/_ButtonLink.html",
            "templates/admin/views/deployment/servers/_ServerCard.html"
        ],
        onDelete (event) {
            event.preventDefault();
            const id = $(event.currentTarget).data().deleteItem;
            showConfirmationBeforeDeleting({
                message: $.t("console.common.confirmDeleteText", { type: $.t("console.servers.common.confirmType") })
            },
            () => {
                ServersService.servers.remove(id).then(() => {
                    this.render();
                }, (response) => {
                    Messages.addMessage({ response, type: Messages.TYPE_DANGER });
                });
            });
        },
        renderToggleView (data) {
            const tableData = {
                "headers": [$.t("console.servers.list.table.0"), $.t("console.servers.list.table.1")],
                "items" : data
            };

            this.toggleView = new ToggleCardListView({
                el: "#toggleCardList",
                activeView: this.toggleView ? this.toggleView.getActiveView() : ToggleCardListView.DEFAULT_VIEW,
                button: {
                    href: "#deployment/servers/new",
                    icon: "fa-plus",
                    title: $.t("console.servers.list.new"),
                    btnClass: "btn-primary"
                }
            });

            this.toggleView.render((toggleView) => {
                new TemplateBasedView({
                    data: tableData,
                    el: toggleView.getElementA(),
                    template: "templates/admin/views/deployment/servers/ServersCardsTemplate.html",
                    callback: () => {
                        this.$el.find('[data-toggle="popover"]').popover();
                    }
                }).render();
                new TemplateBasedView({
                    data: tableData,
                    el: toggleView.getElementB(),
                    template: "templates/admin/views/deployment/servers/ServersTableTemplate.html"
                }).render();
            });
        },

        showCallToAction () {
            this.$el.find(".call-to-action-block").removeClass("hidden");
        },

        render (args, callback) {
            ServersService.servers.getAll().then((data) => {
                this.parentRender(() => {
                    if (_.isEmpty(data)) {
                        this.showCallToAction();
                    } else {
                        this.renderToggleView(data);
                    }

                    if (callback) {
                        callback();
                    }
                });
            }, (response) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response
                });
            });
        }
    });

    return new ListServersView();
});
