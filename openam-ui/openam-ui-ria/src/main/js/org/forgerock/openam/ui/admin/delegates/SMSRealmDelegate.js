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
                    // FIXME: This is a temporay client side fix until AME-7202 is completed.
                    chainData[0].authChainConfiguration = SMSDelegateUtils.authChainConfigurationToJson(chainData[0].authChainConfiguration);

                    _.each(chainData[0].authChainConfiguration, function (chainLink, index) {
                        chainData[0].authChainConfiguration[index].type = _.findWhere(modulesData[0].result, { _id: chainLink.module }).type;
                    });

                    return {
                        chainData: chainData[0],
                        modulesData: modulesData[0].result
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
                var cleaned = SMSDelegateUtils.authChainConfigurationToXml(data);
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/chains/" + name),
                    type: "PUT",
                    data: JSON.stringify(cleaned)
                });
            }
        },
        modules: {
            all: function (realm) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/modules?_queryFilter=true")
                });
            },
            create: function (realm, data) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/modules/" + data.type + "?_action=create"),
                    type: "POST",
                    data: JSON.stringify(data)
                });
            },
            get: function (realm, name, type) {
                var url = scopedByRealm(realm, "authentication/modules/" + type + "/" + name);

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
            has: function (realm, name) {
                var promise = $.Deferred(),
                    request = this.get(realm, name);

                request.done(function () {
                    promise.resolve(false);
                }).fail(function () {
                    promise.resolve(true);
                });
                return promise;
            },
            remove: function (realm, name, type) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/modules/" + type + "/" + name),
                    type: "DELETE"
                });
            },
            update: function (realm, data) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/modules/" + data.type + "/" + data.name),
                    type: "PUT",
                    data: JSON.stringify(data)
                });
            },
            types: {
                all: function (realm) {
                    return obj.serviceCall({
                        url: scopedByRealm(realm, "authentication/modules/types?_queryFilter=true")
                    }).done(function(data) {
                        data.result = _.sortBy(data.result, "name");
                    });
                }
            }
        }
    };

    return obj;
});
