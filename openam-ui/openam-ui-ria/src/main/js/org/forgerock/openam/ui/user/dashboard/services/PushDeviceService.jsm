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

import AbstractDelegate from "org/forgerock/commons/ui/common/main/AbstractDelegate";
import Configuration from "org/forgerock/commons/ui/common/main/Configuration";
import Constants from "org/forgerock/commons/ui/common/util/Constants";
import RealmHelper from "org/forgerock/openam/ui/common/util/RealmHelper";

const delegate = new AbstractDelegate(`${Constants.host}/${Constants.context}/json/`);
const getPath = function () {
    return `__subrealm__/users/${Configuration.loggedUser.get("uid")}/devices/push/`;
};

export function getAll () {
    return delegate.serviceCall({
        url: RealmHelper.decorateURIWithSubRealm(`${getPath()}?_queryFilter=true`),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        suppressEvents: true
    }).then((value) => value.result);
}

export function remove (uuid) {
    return delegate.serviceCall({
        url: RealmHelper.decorateURIWithSubRealm(getPath() + uuid),
        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
        suppressEvents: true,
        method: "DELETE"
    });
}
