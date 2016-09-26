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
 * @module org/forgerock/openam/ui/admin/services/realm/AgentsService
 */

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";
import Promise from "org/forgerock/openam/ui/common/util/Promise";
import JSONSchema from "org/forgerock/openam/ui/common/models/JSONSchema";
import JSONValues from "org/forgerock/openam/ui/common/models/JSONValues";

const obj = new AbstractDelegate(`${Constants.host}/${Constants.context}/json`);

export function getCreatableTypes (realm) {
    return obj.serviceCall({
        url: fetchUrl("/realm-config/agents?_action=getCreatableTypes", { realm }),
        type: "POST",
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
    }).then((data) => data.result);
}

export function getInitialState (realm, type) {
    function getTemplate () {
        return obj.serviceCall({
            url: fetchUrl(`/realm-config/agents/${type}?_action=template`, { realm }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST"
        }).then((response) => new JSONValues(response));
    }

    function getSchema () {
        return obj.serviceCall({
            url: fetchUrl(`/realm-config/agents/${type}?_action=schema`, { realm }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST"
        }).then((response) => {
            return new JSONSchema(response);
        });
    }

    return Promise.all([getSchema(type), getTemplate()]).then((response) => ({
        schema: response[0],
        values: response[1]
    }));
}
