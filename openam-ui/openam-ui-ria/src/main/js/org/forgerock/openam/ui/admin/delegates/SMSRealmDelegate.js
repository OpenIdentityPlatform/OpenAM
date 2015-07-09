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

 /*global define*/
define("org/forgerock/openam/ui/admin/delegates/SMSRealmDelegate", [
    "jquery",
    "underscore",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/delegates/SMSDelegateUtils"
], function ($, _, AbstractDelegate, Constants, SMSDelegateUtils) {
    /**
     * @exports org/forgerock/openam/ui/admin/delegates/SMSRealmDelegate
     */
    var obj = new AbstractDelegate(Constants.host + "/" + Constants.context + "/json"),
        scopedByRealm = function (realm, path) {
            if (realm === "/") { realm = ""; }

            return realm + "/realm-config/" + path;
        };

    obj.authentication = {
        get: function (realm) {
            var url = scopedByRealm(realm, "authentication");

            return $.when(
                obj.serviceCall({ url: url + "?_action=schema", type: "POST" }),
                obj.serviceCall({ url: url })
            ).then(function(schemaData, valuesData) {
                return {
                    schema: SMSDelegateUtils.sanitizeSchema(schemaData[0]),
                    values: valuesData[0]
                };
            });
        },
        update: function (realm, data) {
            return obj.serviceCall({
                url: scopedByRealm(realm, "authentication"),
                type: "PUT",
                data: JSON.stringify(data)
            });
        },
        chains: {
            all: function (realm) {
                var url = scopedByRealm(realm, "authentication");

                return $.when(
                    obj.serviceCall({ url: url + "/chains?_queryFilter=true" }),
                    obj.serviceCall({ url: url })
                ).then(function(chainsData, authenticationData) {
                    _.each(chainsData[0].result, function (chainData) {
                        if (chainData._id === authenticationData[0].adminAuthModule) {
                            chainData.defaultConfig = chainData.defaultConfig || {};
                            chainData.defaultConfig.adminAuthModule = true;
                        }

                        if (chainData._id === authenticationData[0].orgConfig) {
                            chainData.defaultConfig = chainData.defaultConfig || {};
                            chainData.defaultConfig.orgConfig = true;
                        }
                    });

                    return {
                        values: chainsData[0]
                    };
                });
            },
            create: function (realm, data) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/chains?_action=create"),
                    type: "POST",
                    data: JSON.stringify(data)
                });
            },
            get: function (realm, name) {
                return $.when(
                    obj.serviceCall({ url: scopedByRealm(realm, "authentication/chains/" + name) }),
                    obj.serviceCall({ url: scopedByRealm(realm, "authentication/modules?_queryFilter=true") })
                ).then(function (chainData, modulesData) {

                    _.each(chainData[0].authChainConfiguration, function (chainLink, index) {
                        chainData[0].authChainConfiguration[index].type = _.findWhere(modulesData[0].result, { _id: chainLink.module }).type;
                    });

                    return {
                        chainData: chainData[0],
                        modulesData: _.sortBy(modulesData[0].result, "_id")
                    };
                });
            },
            remove: function (realm, name) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/chains/" + name),
                    type: "DELETE"
                });
            },
            update: function (realm, name, data) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/chains/" + name),
                    type: "PUT",
                    data: JSON.stringify(data)
                });
            }
        },
        modules: {
            all: function (realm) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/modules?_queryFilter=true")
                }).done(SMSDelegateUtils.sortResultBy("_id"));
            },
            create: function (realm, data, type) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/modules/" + type + "?_action=create"),
                    type: "POST",
                    data: JSON.stringify(data)
                });
            },
            get: function (realm, name, type) {
                return obj.serviceCall({ url: scopedByRealm(realm, "authentication/modules/" + type + "/" + name) });
            },
            exists: function (realm, name) {
                var promise = $.Deferred(),
                    request = obj.serviceCall({
                        url: scopedByRealm(realm, 'authentication/modules?_queryFilter=_id eq "' + name + '"&_fields=_id')
                    });

                request.done(function (data) {
                    promise.resolve(data.result.length > 0);
                });
                return promise;
            },
            remove: function (realm, name, type) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/modules/" + type + "/" + name),
                    type: "DELETE"
                });
            },
            update: function (realm, name, type, data) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/modules/" + type + "/" + name),
                    type: "PUT",
                    data: JSON.stringify(data)
                });
            },
            types: {
                all: function (realm) {
                    return obj.serviceCall({
                        url: scopedByRealm(realm, "authentication/modules/types?_queryFilter=true")
                    }).done(SMSDelegateUtils.sortResultBy("name"));
                }
            }
        }
    };

    obj.dashboard = {
        commonTasks: {
            all: function (realm) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "commontasks?_queryFilter=true")
                });
            }
        }
    };

    return obj;
});
