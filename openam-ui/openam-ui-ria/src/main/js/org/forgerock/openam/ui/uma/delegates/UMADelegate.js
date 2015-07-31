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

/*global _, define*/
define("org/forgerock/openam/ui/uma/delegates/UMADelegate", [
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function ($, AbstractDelegate, Configuration, Constants, RealmHelper) {
    var obj = new AbstractDelegate(Constants.host + "/" + Constants.context + "/json/");

    obj.getUmaConfig = function () {
        var promise = $.Deferred(),
            request;

        if (!Configuration.globalData.auth.uma || !Configuration.globalData.auth.uma.resharingMode){
            request = obj.serviceCall({
                url: RealmHelper.decorateURIWithRealm("__subrealm__/serverinfo/uma"),
                headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"}
            });
            request.done(function (data) {

                Configuration.globalData.auth.uma = Configuration.globalData.auth.uma || {};
                Configuration.globalData.auth.uma.resharingMode = data.resharingMode;

                promise.resolve();
            }).error(function () {
                promise.resolve();
            });
        } else {
            promise.resolve();
        }

        return promise;
    };

    obj.unshareAllResources = function() {
        return obj.serviceCall({
            url: RealmHelper.decorateURIWithRealm("__subrealm__/users/" + encodeURIComponent(Configuration.loggedUser.username) + "/oauth2/resource/sets?_action=revokeAll"),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST"
        });
    };

    obj.approveRequest = function(id, permissions) {
        return obj.serviceCall({
            url: RealmHelper.decorateURIWithRealm("__subrealm__/users/" + encodeURIComponent(Configuration.loggedUser.username) + "/uma/pendingrequests/" + id + "?_action=approve"),
            type: "POST",
            data: JSON.stringify({
                scopes: permissions
            })
        });
    };

    obj.denyRequest = function(id) {
        return obj.serviceCall({
            url: RealmHelper.decorateURIWithRealm("__subrealm__/users/" + encodeURIComponent(Configuration.loggedUser.username) + "/uma/pendingrequests/" + id + "?_action=deny"),
            type: "POST"
        });
    };

    obj.labels = {
        mock: function() {
            return [
                { type: "star", name: "Starred", _id: "ID_1" },
                { type: "system", name: "Facebook", _id: "ID_2" },
                { type: "system", name: "Flickr", _id: "ID_5" },
                { type: "user", name: "Mammals", _id: "ID_7" },
                { type: "user", name: "Mammals/Apes/Humans", _id: "ID_8" },
                { type: "user", name: "Mammals/Apes/Monkeys", _id: "ID_22" },
                { type: "user", name: "Insects/Flys/Bees", _id: "ID_23" },
                { type: "user", name: "Mammals/Dogs", _id: "ID_24" },
                { type: "user", name: "Mammals/Whales", _id: "ID_26" },
                { type: "user", name: "Mammals/Apes/Humans/Coders", _id: "ID_99" },
                { type: "user", name: "Mammals/Dogs/Puppies", _id: "ID_28" }
            ];
        },
        all: function () {
            // Configuration.globalData.auth.uma = Configuration.globalData.auth.uma || {};
            // Configuration.globalData.auth.uma.labels = returnObj;
            var self = this;

            return obj.serviceCall({
                url: RealmHelper.decorateURIWithRealm("__subrealm__/users/" + encodeURIComponent(Configuration.loggedUser.username) + "/oauth2/resources/labels?_queryFilter=true")
            }).done(function(data) {
                if(!_.any(data.result, { name: "starred" })) {
                    self.create("starred");
                }
            });
            // }).then(function() {
            //     var mockData = _.sortBy(obj.labels.mock(), "name");
            //     return {
            //         "user": _.filter(mockData, {"type": "user"}),
            //         "starred": _.filter(mockData, {"type": "star"}),
            //         "system": _.filter(mockData, {"type": "system"})
            //     };
            // });
        },
        create: function(name) {
            return obj.serviceCall({
                url: RealmHelper.decorateURIWithRealm("__subrealm__/users/" + encodeURIComponent(Configuration.loggedUser.username) + "/oauth2/resources/labels?_action=create"),
                type: "POST",
                data: JSON.stringify({
                    name: name,
                    type: "SYSTEM"
                })
            });
        },
        getStarred: function(){
            var promise = $.Deferred(),
                umaConfig = Configuration.globalData.auth.uma;

            if (umaConfig && umaConfig.labels && umaConfig.labels.starred) {
                promise.resolve(umaConfig.labels.starred);
            } else {
                promise = obj.labels.all();
                promise.done(function () {
                    promise.resolve(Configuration.globalData.auth.uma.labels.starred);
                }).error(function () {
                    promise.resolve();
                });
            }
            return promise;
        },
        get: function(id, type){
            var promise = $.Deferred(),
                umaConfig = Configuration.globalData.auth.uma,
                label;

            if (umaConfig && umaConfig.labels) {
                label = _.find(umaConfig.labels[type], {_id: id});
                promise.resolve(label);
            } else {
                promise = obj.labels.all();
                promise.done(function () {
                    label = _.find(Configuration.globalData.auth.uma.labels[type], {_id: id});
                    promise.resolve(label);
                }).error(function () {
                    promise.fail();
                });
            }
            return promise;
        },
        getById: function(id) {
            var promise = $.Deferred();

            promise.resolve(_.findWhere(obj.labels.mock(), { "_id": id }));

            return promise;
        }
    };

    return obj;
});
