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

/**
 * @module org/forgerock/openam/ui/admin/services/global/AuthenticationService
 */
define([
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/SMSServiceUtils",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/common/util/Promise"
], (_, AbstractDelegate, Constants, SMSServiceUtils, JSONSchema, JSONValues, Promise) => {
    const obj = new AbstractDelegate(`${Constants.host}/${Constants.context}/json/global-config/authentication`);

    function getModuleUrl (id) {
        return id === "core" ? "" : `/modules/${id}`;
    }

    obj.authentication = {
        getAll () {
            return obj.serviceCall({
                url: "/modules?_action=getAllTypes",
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            }).then((data) => _.sortBy(data.result, "name"));
        },

        schema () {
            return SMSServiceUtils.schemaWithDefaults(obj, "");
        },

        get: (id) => {
            const getSchema = () => obj.serviceCall({
                url: `${getModuleUrl(id)}?_action=schema`,
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            }).then((response) => {
                delete response.properties.defaults; // TODO: remove when OPENAM-8822 is fixed
                return new JSONSchema(response);
            });

            const getValues = () => obj.serviceCall({
                url: getModuleUrl(id),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            });

            return Promise.all([getSchema(), getValues()]).then((response) => ({
                schema: response[0],
                values: new JSONValues(response[1][0]),
                name: response[1][0]._type.name
            }));
        },

        update (id, data) {
            return obj.serviceCall({
                url: getModuleUrl(id),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "PUT",
                data: JSON.stringify(data)
            });
        }
    };
    return obj;
});
