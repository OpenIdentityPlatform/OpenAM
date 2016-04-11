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

define("org/forgerock/openam/ui/admin/services/ServersService", [
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/SMSServiceUtils",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues"
], (_, AbstractDelegate, Constants, SMSServiceUtils, JSONSchema, JSONValues) => {
    const obj = new AbstractDelegate(`${Constants.host}/${Constants.context}/json/global-config/servers`);
    const DEFAULT_SERVER = "server-default";

    // TODO: remove when AME-10196 is fixed
    const mockData = (data) => _.extend(data, { type: "object" });

    const getSchema = (server, section) => obj.serviceCall({
        url: `/${server}/properties/${section}?_action=schema`,
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST"
    }).then((response) => new JSONSchema(mockData(response)));

    const getValues = (server, section) => obj.serviceCall({
        url: `/${server}/properties/${section}`,
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    }).then((response) => new JSONValues(response));

    obj.servers = {
        get: (server, section) => Promise.all([
            getSchema(server, section),
            getValues(server, section)
        ]).then((response) => ({
            schema: response[0],
            values: response[1]
        })),
        getDefaults: (section) => obj.servers.get(DEFAULT_SERVER, section),
        getAll: () => obj.serviceCall({
            url: `?_queryFilter=true`,
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then((response) => _.reject(response.result, { "_id" : "server-default" })),
        remove: (id) => obj.serviceCall({
            url: `/${id}`,
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "DELETE"
        }),
        update:  (id, data) => obj.serviceCall({
            url: `${id}`,
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "PUT",
            data: JSON.stringify(data)
        })
    };

    return obj;
});
