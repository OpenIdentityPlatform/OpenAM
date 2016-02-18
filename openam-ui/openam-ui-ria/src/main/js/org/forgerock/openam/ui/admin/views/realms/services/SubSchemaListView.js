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

define("org/forgerock/openam/ui/admin/views/realms/services/SubSchemaListView", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/realm/sms/ServicesService",
    "org/forgerock/openam/ui/admin/utils/FormHelper"
], ($, _, Messages, AbstractView, EventManager, Router, Constants, ServicesService, FormHelper) => {

    function renderView () {
        return Promise.all([
            ServicesService.type.subSchema.instance.getAll(this.data.realmPath, this.data.type),
            ServicesService.type.subSchema.type.getCreatables(this.data.realmPath, this.data.type)
        ]).then((data) => {
            this.data.instances = data[0];
            this.data.creatables = data[1];

            this.parentRender();
        });
    }

    function deleteSubSchema (subSchemaType, subSchemaInstance) {
        ServicesService.type.subSchema.instance.remove(
            this.data.realmPath,
            this.data.type,
            subSchemaType,
            subSchemaInstance
        ).then(() => {
            EventManager.sendEvent(Constants.EVENT_DISPLAY_MESSAGE_REQUEST, "changesSaved");
            renderView.call(this);
        }, (response) => {
            Messages.addMessage({
                response: response,
                type: Messages.TYPE_DANGER
            });
        });
    }

    const SubschemaListView = AbstractView.extend({
        template: "templates/admin/views/realms/services/SubSchemaListTemplate.html",
        events: {
            "click [data-subschema-delete]" : "onDelete"
        },

        render: function (data) {
            _.extend(this.data, data);
            renderView.call(this);
        },

        onDelete: function (e) {
            e.preventDefault();

            const target = $(e.currentTarget),
                subSchemaInstance = target.closest("tr").data("subschemaId"),
                subSchemaType = target.closest("tr").data("subschemaType");

            FormHelper.showConfirmationBeforeDeleting({
                message: $.t("console.services.list.confirmDeleteSelected")
            }, _.bind(deleteSubSchema, this, subSchemaType, subSchemaInstance));
        }
    });

    return new SubschemaListView();
});
