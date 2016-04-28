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
    "org/forgerock/openam/ui/admin/services/realm/ServicesService",
    "org/forgerock/openam/ui/admin/views/common/schema/EditSchemaComponent"
], (AbstractView, Router, ServicesService, EditSchemaComponent) => AbstractView.extend({
    render ([realmPath, type]) {
        const editComponent = new EditSchemaComponent({
            data: {
                realmPath,
                type,
                headerActions:
                    [{ actionPartial: "form/_Button", data: "delete", title: "common.form.delete", icon: "fa-times" }]
            },
            listRoute: Router.configuration.routes.realmsServices,
            listRouteArgs: [encodeURIComponent(realmPath)],

            template: "templates/admin/views/realms/services/EditServiceTemplate.html",
            subSchemaTemplate: "templates/admin/views/realms/services/SubSchemaListTemplate.html",

            getInstance: () => ServicesService.instance.get(realmPath, type),
            updateInstance: (values) => ServicesService.instance.update(realmPath, type, values),
            deleteInstance: () => ServicesService.instance.remove(realmPath, type),

            getSubSchemaTypes: () => ServicesService.type.subSchema.type.getAll(realmPath, type),
            getSubSchemaCreatableTypes: () => ServicesService.type.subSchema.type.getCreatables(realmPath, type),
            getSubSchemaInstances: () => ServicesService.type.subSchema.instance.getAll(realmPath, type),
            deleteSubSchemaInstance: (subSchemaType, subSchemaInstance) =>
                ServicesService.type.subSchema.instance.remove(realmPath, type, subSchemaType, subSchemaInstance)
        });

        this.parentRender(() => { this.$el.append(editComponent.render().$el); });
    }
}));
