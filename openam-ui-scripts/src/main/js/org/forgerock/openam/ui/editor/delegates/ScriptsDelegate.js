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
 * Copyright 2015 ForgeRock AS.
 */

/*global _, define*/
define('org/forgerock/openam/ui/editor/delegates/ScriptsDelegate', [
    'org/forgerock/commons/ui/common/main/AbstractDelegate',
    'org/forgerock/commons/ui/common/main/Configuration',
    'org/forgerock/commons/ui/common/util/Constants',
    'org/forgerock/openam/ui/common/util/RealmHelper'
], function (AbstractDelegate, Configuration, Constants, RealmHelper) {
    var obj = new AbstractDelegate(Constants.host + '/' + Constants.context + '/json');

    obj.ERROR_HANDLERS = {
        "Bad Request": { status: "400" },
        "Not found": { status: "404" },
        "Gone": { status: "410" },
        "Conflict": { status: "409" },
        "Internal Server Error": { status: "500" },
        "Service Unavailable": { status: "503" }
    };

    obj.validateScript = function (data) {
        return obj.serviceCall({
            url: RealmHelper.decorateURLWithOverrideRealm('/scripts/?_action=validate'),
            type: 'POST',
            data: JSON.stringify(data),
            errorsHandlers: obj.ERROR_HANDLERS
        });
    };

    return obj;
});
