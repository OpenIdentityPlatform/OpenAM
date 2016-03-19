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

define("org/forgerock/openam/ui/admin/views/deployment/ListSitesView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/openam/ui/admin/services/SitesService",
    "org/forgerock/openam/ui/common/components/TemplateBasedView",
    "org/forgerock/openam/ui/admin/views/common/ToggleCardListView",
    "org/forgerock/openam/ui/admin/views/deployment/deleteInstance"
], ($, _, AbstractView, FormHelper, Messages, SitesService, TemplateBasedView, ToggleCardListView, deleteInstance) => {

    const ListSitesView = AbstractView.extend({
        template: "templates/admin/views/deployment/ListSitesTemplate.html",
        events: {
            "click [data-delete-item]" : "onDelete"
        },
        partials: [
            "partials/util/_ButtonLink.html",
            "templates/admin/views/deployment/_SiteCard.html"
        ],

        onDelete (event) {
            event.preventDefault();
            const id = $(event.currentTarget).data().deleteItem;
            SitesService.sites.get(id).then((data) => {
                FormHelper.showConfirmationBeforeDeleting({
                    message: $.t("console.common.confirmDeleteText", { type: $.t("console.sites.common.confirmType") })
                }, _.partial(deleteInstance, data.values.raw._id, data.values.raw.etag, () => {
                    this.render();
                }));
            });
        },

        renderToggleView (data) {
            const tableData = {
                "headers": [
                    $.t("console.sites.list.table.0"), $.t("console.sites.list.table.1"),
                    $.t("console.sites.list.table.2"), $.t("console.sites.list.table.3")
                ],
                "items" : data
            };

            this.toggleView = new ToggleCardListView({
                el: "#toggleCardList",
                activeView: this.toggleView ? this.toggleView.getActiveView() : 0,
                button: {
                    href: "TODO: Add the link here",
                    icon: "fa-plus",
                    title: $.t("console.sites.list.new"),
                    btnclass: "btn-primary"
                }
            });

            this.toggleView.render((toggleView) => {
                new TemplateBasedView({
                    data: tableData,
                    el: toggleView.getElementA(),
                    template: "templates/admin/views/deployment/SitesCardsTemplate.html"
                }).render();
                new TemplateBasedView({
                    data: tableData,
                    el: toggleView.getElementB(),
                    template: "templates/admin/views/deployment/SitesTableTemplate.html"
                }).render();
            });
        },

        showCallToAction () {
            this.$el.find(".call-to-action-block").removeClass("hidden");
        },

        render (args, callback) {

            SitesService.sites.getAll().then((data) => {

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

    return new ListSitesView();
});
