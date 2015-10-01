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
 * Copyright 2015 ForgeRock AS.
 */

/*global define*/

define("org/forgerock/openam/ui/dashboard/delegates/DeviceManagementDelegate", [
    "jquery",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function ($, Configuration, Constants, AbstractDelegate, RealmHelper) {
    var obj = new AbstractDelegate(Constants.host + "/" + Constants.context + "/json/");

    obj.deleteDevice = function (uuid) {
        return obj.serviceCall({
            url: RealmHelper.decorateURIWithSubRealm("users/" +
                                                     Configuration.loggedUser.get("uid") +
                                                     "/devices/2fa/oath/" +
                                                     uuid),
            method: "DELETE"
        });
    };

    obj.setDeviceSkippable = function (statusDevice) {
        var skipOption = {};
        skipOption.value = statusDevice;
        return obj.serviceCall({
            url: RealmHelper.decorateURIWithRealm("users/" +
                                                  Configuration.loggedUser.get("uid") +
                                                  "/devices/2fa/oath/?_action=skip"),
            data: JSON.stringify(skipOption),
            method: "POST"
        });
    };

    obj.getDevices = function () {
        var path = "users/" + Configuration.loggedUser.get("uid") + "/devices/2fa/oath/";
        return $.when(
            obj.serviceCall({
                url: RealmHelper.decorateURIWithSubRealm(path + "?_queryFilter=true")
            }),
            obj.serviceCall({
                url: RealmHelper.decorateURIWithRealm(path + "?_action=check"),
                method: "POST"
            })
        ).then(function (devicesData, statusData) {
            devicesData[0].result.skipped = statusData[0].result;
            return devicesData[0];
        });
    };

    return obj;
});
