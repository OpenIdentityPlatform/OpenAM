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
    "backbone",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/realm/ServicesService",
    "org/forgerock/openam/ui/admin/utils/FormHelper",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/openam/ui/common/util/Promise"
], ($, _, Backbone, Messages, EventManager, Router, Constants, ServicesService, FormHelper, UIUtils, Promise) => {
    function deleteSubSchema (realmPath, type, subSchemaType, subSchemaInstance) {
        return ServicesService.type.subSchema.instance.remove(realmPath, type, subSchemaType, subSchemaInstance);
    }

    const SubschemaListView = Backbone.View.extend({
        template: "templates/admin/views/realms/services/SubSchemaListTemplate.html",
        events: {
            "click [data-subschema-delete]" : "onDelete"
        },
        initialize (options) {
            this.options = options;
        },
        render () {
            Promise.all([
                ServicesService.type.subSchema.instance.getAll(this.options.realmPath, this.options.type),
                ServicesService.type.subSchema.type.getCreatables(this.options.realmPath, this.options.type)
            ]).then((response) => {
                const data = _.merge({}, this.options, {
                    instances: response[0][0],
                    creatables:response[1][0]
                });

                UIUtils.fillTemplateWithData(this.template, data, (html) => {
                    this.$el.html(html);
                });
            });

            return this;
        },
        onDelete (event) {
            event.preventDefault();

            const target = $(event.currentTarget);
            const subSchemaInstance = target.closest("tr").data("subschemaId");
            const subSchemaType = target.closest("tr").data("subschemaType");

            FormHelper.showConfirmationBeforeDeleting({
                message: $.t("console.services.subSchema.confirmDeleteSelected")
            }, () => {
                deleteSubSchema(this.options.realmPath,
                                this.options.type,
                                subSchemaType,
                                subSchemaInstance)
                .then(() => {
                    EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
                    this.render();
                }, (response) => Messages.addMessage({ response, type: Messages.TYPE_DANGER }));
            });
        }
    });

    return SubschemaListView;
});
