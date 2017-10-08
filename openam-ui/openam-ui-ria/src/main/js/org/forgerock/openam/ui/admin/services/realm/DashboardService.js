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
 * @module org/forgerock/openam/ui/admin/services/realm/DashboardService
 */
define([
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/services/fetchUrl"
], (AbstractDelegate, Constants, fetchUrl) => {
    const obj = new AbstractDelegate(`${Constants.host}/${Constants.context}/json`);

    obj.dashboard = {
        commonTasks: {
            all: (realm) => obj.serviceCall({
                url: fetchUrl.default("/realm-config/commontasks?_queryFilter=true", { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            })
        }
    };

    return obj;
});
