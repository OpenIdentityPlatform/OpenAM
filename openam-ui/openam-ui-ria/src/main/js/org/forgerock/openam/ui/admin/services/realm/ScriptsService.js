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
 * Copyright 2015-2016 ForgeRock AS.
 */

/**
* @module org/forgerock/openam/ui/admin/services/realm/ScriptsService
*/
define([
    "org/forgerock/commons/ui/common/components/Messages",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function (Messages, AbstractDelegate, Configuration, Constants, RealmHelper) {
    var obj = new AbstractDelegate(`${Constants.host}/${Constants.context}/json`);

    function getLocalizedResponse (response) {
        Messages.addMessage({
            type: Messages.TYPE_DANGER,
            response
        });
    }

    obj.validateScript = function (data) {
        return obj.serviceCall({
            url: RealmHelper.decorateURLWithOverrideRealm("/scripts/?_action=validate"),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST",
            data: JSON.stringify(data),
            error: getLocalizedResponse
        });
    };

    return obj;
});
