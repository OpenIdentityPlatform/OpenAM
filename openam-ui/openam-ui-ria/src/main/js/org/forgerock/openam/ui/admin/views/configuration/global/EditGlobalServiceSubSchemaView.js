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
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/services/global/ServicesService",
    "org/forgerock/openam/ui/admin/views/common/Backlink",
    "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent"
], (AbstractView, Router, ServicesService, Backlink, EditSchemaComponent) => {
    const EditGlobalServiceSubSchemaView = AbstractView.extend({
        template: "templates/admin/views/configuration/EditGlobalConfigurationBaseTemplate.html",
        render ([serviceType, subSchemaType, subSchemaInstanceId]) {
            // global script types can not be deleted
            const showDeleteButton = () => serviceType !== "scripting";

            const editComponent = new EditSchemaComponent({
                data: {
                    serviceType,
                    subSchemaType,
                    subSchemaInstanceId,
                    headerActions: showDeleteButton() ? [
                        { actionPartial: "form/_Button", data: "delete", title: "common.form.delete", icon:"fa-times" }
                    ] : []
                },
                listRoute: Router.configuration.routes.editGlobalService,
                listRouteArgs: [encodeURIComponent(serviceType)],

                template: "templates/admin/views/common/schema/EditServiceSubSchemaTemplate.html",
                subSchemaTemplate: "templates/admin/views/configuration/global/SubSubSchemaListTemplate.html",

                getInstance: () => ServicesService.type.subSchema.instance.get(
                    serviceType, subSchemaType, subSchemaInstanceId),
                updateInstance: (values) => ServicesService.type.subSchema.instance.update(
                    serviceType, subSchemaType, subSchemaInstanceId, values),
                deleteInstance: () => ServicesService.type.subSchema.instance.remove(
                    serviceType, subSchemaType, subSchemaInstanceId),

                getSubSchemaTypes: () => ServicesService.type.subSchema.type.subSchema.type.getAll(
                    serviceType, subSchemaType),
                getSubSchemaCreatableTypes: () => ServicesService.type.subSchema.type.subSchema.type.getCreatables(
                    serviceType, subSchemaType, subSchemaInstanceId),
                getSubSchemaInstances: () => ServicesService.type.subSchema.type.subSchema.instance.getAll(
                    serviceType, subSchemaType, subSchemaInstanceId)
            });

            this.parentRender(() => {
                new Backlink().render(2);
                this.$el.find("[data-global-configuration]").append(editComponent.render().$el);
            });
        }
    });

    return new EditGlobalServiceSubSchemaView();
});
