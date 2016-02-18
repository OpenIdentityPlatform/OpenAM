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
 * Copyright 2016 ForgeRock AS.
 */

define("org/forgerock/openam/ui/admin/views/realms/services/ServicesView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/openam/ui/admin/services/realm/sms/ServicesService",
    "org/forgerock/openam/ui/admin/utils/FormHelper"
], ($, _, Messages, AbstractView, ServicesService, FormHelper) => {
    function getServiceIdFromElement (element) {
        return $(element).closest("tr").data("serviceId");
    }
    function loadServicesFromServer (realmPath) {
        return ServicesService.instance.getAll(realmPath);
    }
    function deleteServices (ids) {
        FormHelper.showConfirmationBeforeDeleting({
            message: $.t("console.services.list.confirmDeleteSelected", { count: ids.length })
        }, () => {
            ServicesService.instance.remove(this.data.realmPath, ids).then(() => {
                this.rerender();
            }, (reason) => {
                Messages.addMessage({
                    type: Messages.TYPE_DANGER,
                    response: reason
                });
                this.rerender();
            });
        });
    }

    const ServicesView = AbstractView.extend({
        template: "templates/admin/views/realms/services/ServicesTemplate.html",
        partials: [
            "partials/util/_HelpLink.html"
        ],
        events: {
            "change [data-select-service]": "serviceSelected",
            "click [data-delete-service]": "onDeleteSingle",
            "click [data-delete-services]": "onDeleteMultiple"
        },
        serviceSelected: function (event) {
            const anyServicesSelected = this.$el.find("input[type=checkbox]").is(":checked"),
                row = $(event.currentTarget).closest("tr");

            row.toggleClass("selected");
            this.$el.find("[data-delete-services]").prop("disabled", !anyServicesSelected);
        },
        onDeleteSingle: function (event) {
            event.preventDefault();

            const id = getServiceIdFromElement(event.currentTarget);

            _.bind(deleteServices, this)([id]);
        },
        onDeleteMultiple: function (event) {
            event.preventDefault();

            const ids = _(this.$el.find("input[type=checkbox]:checked")).toArray().map(getServiceIdFromElement).value();

            _.bind(deleteServices, this)(ids);
        },
        render: function (args, callback) {
            this.data.args = args;
            this.data.realmPath = args[0];

            loadServicesFromServer(this.data.realmPath).then((services) => {
                this.data.services = services;

                this.parentRender(() => {
                    if (callback) {
                        callback();
                    }
                });
            });
        },
        rerender: function () {
            this.render(this.data.args);
        }
    });

    return ServicesView;
});
