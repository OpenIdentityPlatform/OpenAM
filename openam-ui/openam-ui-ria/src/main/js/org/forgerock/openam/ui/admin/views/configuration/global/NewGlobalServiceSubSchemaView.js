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
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractView",
    "org/forgerock/commons/ui/common/main/Router",
    "org/forgerock/openam/ui/admin/services/global/ServicesService",
    "org/forgerock/openam/ui/admin/views/common/Backlink",
    "org/forgerock/openam/ui/admin/views/common/schema/NewSchemaComponent"
], (_, AbstractView, Router, ServicesService, Backlink, NewSchemaComponent) => {
    const NewGlobalServiceSubSchemaView = AbstractView.extend({
        template: "templates/admin/views/configuration/EditGlobalConfigurationBaseTemplate.html",
        render ([serviceInstance, subSchemaType]) {
            const newSchemaComponent = new NewSchemaComponent({
                data: { serviceInstance, subSchemaType },

                listRoute: Router.configuration.routes.editGlobalService,
                listRouteArgs: [encodeURIComponent(serviceInstance)],

                editRoute: Router.configuration.routes.globalServiceSubSchemaEdit,
                editRouteArgs: (newInstanceId) => _.map([serviceInstance, subSchemaType, newInstanceId],
                    encodeURIComponent),

                template: "templates/admin/views/common/schema/NewServiceSubSchemaTemplate.html",

                getInitialState: () => ServicesService.type.subSchema.instance.getInitialState(
                        serviceInstance, subSchemaType),
                createInstance: (values) => ServicesService.type.subSchema.instance.create(
                        serviceInstance, subSchemaType, values)
            });

            this.parentRender(() => {
                new Backlink().render();
                this.$el.find("[data-global-configuration]").append(newSchemaComponent.render().$el);
            });
        }
    });

    return new NewGlobalServiceSubSchemaView();
});
