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

define("org/forgerock/openam/ui/uma/delegates/UMADelegate", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function ($, _, AbstractDelegate, Configuration, Constants, RealmHelper) {
    var obj = new AbstractDelegate(Constants.host + "/" + Constants.context + "/json/");

    obj.getUmaConfig = function () {
        return obj.serviceCall({
            url: RealmHelper.decorateURIWithRealm("__subrealm__/serverinfo/uma"),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then(function (data) {
            Configuration.globalData.auth.uma = Configuration.globalData.auth.uma || {};
            Configuration.globalData.auth.uma.enabled = data.enabled;
            Configuration.globalData.auth.uma.resharingMode = data.resharingMode;
            return data;
        });
    };

    obj.unshareAllResources = function () {
        return obj.serviceCall({
            url: RealmHelper.decorateURIWithRealm("__subrealm__/users/" +
                encodeURIComponent(Configuration.loggedUser.get("username")) +
                "/oauth2/resources/sets?_action=revokeAll"),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST"
        });
    };

    obj.approveRequest = function (id, permissions) {
        return obj.serviceCall({
            url: RealmHelper.decorateURIWithRealm("__subrealm__/users/" +
                encodeURIComponent(Configuration.loggedUser.get("username")) + "/uma/pendingrequests/" + id +
                "?_action=approve"),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST",
            data: JSON.stringify({
                scopes: permissions
            })
        });
    };

    obj.denyRequest = function (id) {
        return obj.serviceCall({
            url: RealmHelper.decorateURIWithRealm("__subrealm__/users/" +
                encodeURIComponent(Configuration.loggedUser.get("username")) + "/uma/pendingrequests/" + id +
                "?_action=deny"),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST"
        });
    };

    // FIXME: Mutiple calls to #all end-point throughout this section. Optimize
    obj.labels = {
        all: function () {
            return obj.serviceCall({
                url: RealmHelper.decorateURIWithRealm("__subrealm__/users/" +
                    encodeURIComponent(Configuration.loggedUser.get("username")) +
                    "/oauth2/resources/labels?_queryFilter=true"),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            });
        },
        create: function (name, type) {
            return obj.serviceCall({
                url: RealmHelper.decorateURIWithRealm("__subrealm__/users/" +
                    encodeURIComponent(Configuration.loggedUser.get("username")) +
                    "/oauth2/resources/labels?_action=create"),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST",
                data: JSON.stringify({
                    name: name,
                    type: type
                })
            });
        },
        get: function (id) {
            return obj.serviceCall({
                url: RealmHelper.decorateURIWithRealm("__subrealm__/users/" +
                                                      encodeURIComponent(Configuration.loggedUser.get("username")) +
                                                      "/oauth2/resources/labels?_queryFilter=true"),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            }).then(function (data) {
                return _.find(data.result, { _id: id });
            });
        },
        getByName: function (name) {
            return obj.serviceCall({
                url: RealmHelper.decorateURIWithRealm("__subrealm__/users/" +
                    encodeURIComponent(Configuration.loggedUser.get("username")) +
                    "/oauth2/resources/labels?_queryFilter=true"),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            }).then(function (data) {
                data = !_.any(data.result, function (label) { return label.name.toLowerCase() === name; });
            });
        },
        remove: function (id) {
            return obj.serviceCall({
                url: RealmHelper.decorateURIWithRealm("__subrealm__/users/" +
                    encodeURIComponent(Configuration.loggedUser.get("username")) +
                    "/oauth2/resources/labels/" +
                    encodeURIComponent(id)),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "DELETE"
            });
        }
    };

    return obj;
});
