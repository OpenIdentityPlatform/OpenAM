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

    const obj = new AbstractDelegate(
        `${Constants.host}/${Constants.context}/json/global-config/servers/server-default/properties/`);

    // TODO: remove when AME-10196 is fixed
    const mockData = (data) => _.extend(data, { type: "object" });

    const getSchema = (id) => obj.serviceCall({
        url: `${id}?_action=schema`,
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        type: "POST",
        success: (data) => mockData(data)
    });

    const getValues = (id) => obj.serviceCall({
        url: `${id}`,
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        success: (data) => data
    });

    obj.servers = {
        defaults: {
            get: (sectionId) =>
                Promise.all([getSchema(sectionId), getValues(sectionId)]).then((response) => ({
                    schema: new JSONSchema(response[0]),
                    values: new JSONValues(response[1])
                }))
        }
    };

    return obj;
});
