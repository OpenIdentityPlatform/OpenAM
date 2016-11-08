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
 * @module org/forgerock/openam/ui/admin/services/global/SessionsService
 */

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import fetchUrl from "org/forgerock/openam/ui/common/services/fetchUrl";

const obj = new AbstractDelegate(`${Constants.host}/${Constants.context}/json`);

export function getByUserIdAndRealm (id, realm) {
    const queryFilter = encodeURIComponent(`username eq "${id}" and realm eq "${realm}"`);

    return obj.serviceCall({
        url: fetchUrl(`/sessions?_queryFilter=${queryFilter}`, { realm: false }),
        headers: { "Accept-API-Version": "protocol=1.0,resource=2.0" }
    }).then((response) => response.result);
}

export function invalidateByHandles (handles) {
    return obj.serviceCall({
        url: fetchUrl("/sessions?_action=logoutByHandle", { realm: false }),
        type: "POST",
        headers: { "Accept-API-Version": "protocol=1.0,resource=2.0" },
        data: JSON.stringify({ sessionHandles: handles })
    }).then((response) => response.result);
}
