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
  * @module org/forgerock/openam/ui/admin/services/global/ServersService
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
    const obj = new AbstractDelegate(`${Constants.host}/${Constants.context}/json/global-config/servers`);
    const DEFAULT_SERVER = "server-default";
    const ADVANCED_SECTION = "advanced";
    const isDefaultServer = (serverId) => serverId === "server-defaults";
    const normalizeServerId = (serverId) => {
        return isDefaultServer(serverId) ? DEFAULT_SERVER : serverId;
    };

    const objectToArray = (valuesObject) => _.map(valuesObject, (value, key) => ({ key, value }));
    const arrayToObject = (valuesArray) => _.reduce(valuesArray, (result, item) => {
        result[item.key] = item.value;
        return result;
    }, {});

    const getSchema = (server, section) => obj.serviceCall({
        url: `/${server}/properties/${section}?_action=schema`,
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST"
    }).then((response) => new JSONSchema(response));

    const getValues = (server, section) => obj.serviceCall({
        url: `/${server}/properties/${section}`,
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    }).then((response) => {
        if (section === ADVANCED_SECTION) {
            response = _.sortBy(objectToArray(response), (value) => value.key);
        }
        return new JSONValues(response);
    });

    const updateServer = (section, data, id = DEFAULT_SERVER) => {
        let modifiedData = data;
        if (section === ADVANCED_SECTION) {
            modifiedData = arrayToObject(data[ADVANCED_SECTION]);
        }
        return obj.serviceCall({
            url: `/${id}/properties/${section}`,
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "PUT",
            data: JSON.stringify(modifiedData)
        });
    };

    obj.servers = {
        clone: (id, clonedUrl) => obj.serviceCall({
            url: `/${id}?_action=clone`,
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST",
            data: JSON.stringify({ clonedUrl })
        }),
        get: (server, section) => {
            return Promise.all([
                getSchema(server, section),
                getValues(server, section)
            ]).then((response) => ({
                schema: response[0],
                values: response[1]
            }));
        },
        getWithDefaults: (server, section) => {
            const normalizedServerId = normalizeServerId(server);
            const promises = [obj.servers.get(normalizedServerId, section)];

            if (!isDefaultServer(server) && section !== "directoryConfiguration") {
                promises.push(getValues(DEFAULT_SERVER, section));
            }
            return Promise.all(promises).then(([instance, defaultValues = {}]) => {
                return {
                    schema: instance.schema,
                    values: instance.values,
                    defaultValues
                };
            });
        },
        getUrl: (id) => obj.serviceCall({
            url: `/${id}`,
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then((response) => response.url, () => undefined),
        getAll: () => obj.serviceCall({
            url: "?_queryFilter=true",
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then((response) => _.reject(response.result, { "_id" : DEFAULT_SERVER })),
        remove: (id) => obj.serviceCall({
            url: `/${id}`,
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "DELETE"
        }),
        create:  (data) => obj.serviceCall({
            url: "?_action=create",
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST",
            data: JSON.stringify(data)
        }),
        update: (section, data, id) => updateServer(section, data, normalizeServerId(id)),
        ADVANCED_SECTION,
        DEFAULT_SERVER
    };

    return obj;
});
