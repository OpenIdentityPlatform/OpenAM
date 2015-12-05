/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
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

define("org/forgerock/openam/ui/dashboard/delegates/MyApplicationsDelegate", [
    "underscore",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function (_, constants, AbstractDelegate, RealmHelper) {
    var obj = new AbstractDelegate(constants.host + "/" + constants.context + "/json/");

    function sortApps (apps) {
        return _.map(_.sortBy(_.keys(apps), function (key) { return key; }), function (key) {
            var app = {
                id: key
            };
            _.each(apps[key], function (v, k) { app[k] = v[0]; });
            return app;
        });
    }

    obj.getMyApplications = function () {
        return obj.serviceCall({
            url: RealmHelper.decorateURIWithSubRealm("__subrealm__/dashboard/assigned"),
            headers: { "Cache-Control": "no-cache", "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then(sortApps);
    };

    obj.getAvailableApplications = function () {
        return obj.serviceCall({
            url: RealmHelper.decorateURIWithSubRealm("__subrealm__/dashboard/available"),
            headers: { "Cache-Control": "no-cache", "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then(sortApps);
    };

    return obj;
});
