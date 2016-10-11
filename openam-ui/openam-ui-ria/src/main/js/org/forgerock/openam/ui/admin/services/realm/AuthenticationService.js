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
 * @module org/forgerock/openam/ui/admin/services/realm/AuthenticationService
 */
define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/SMSServiceUtils",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "org/forgerock/openam/ui/common/util/Promise"
], ($, _, AbstractDelegate, Constants, SMSServiceUtils, fetchUrl, Promise) => {
    const obj = new AbstractDelegate(`${Constants.host}/${Constants.context}/json`);

    obj.authentication = {
        get (realm) {
            return SMSServiceUtils.schemaWithValues(obj, fetchUrl.default("/realm-config/authentication", { realm }));
        },
        update (realm, data) {
            return obj.serviceCall({
                url: fetchUrl.default("/realm-config/authentication", { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "PUT",
                data: JSON.stringify(data)
            });
        },
        chains: {
            all (realm) {
                return Promise.all([
                    obj.serviceCall({
                        url: fetchUrl.default("/realm-config/authentication/chains?_queryFilter=true", { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    }),
                    obj.serviceCall({
                        url: fetchUrl.default("/realm-config/authentication", { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    })
                ]).then((response) => {
                    const chainsData = response[0];
                    const authenticationData = response[1];

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
            create (realm, data) {
                return obj.serviceCall({
                    url: fetchUrl.default("/realm-config/authentication/chains?_action=create", { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST",
                    data: JSON.stringify(data)
                });
            },
            get (realm, name) {
                var moduleName;

                return Promise.all([
                    obj.serviceCall({
                        url: fetchUrl.default("/realm-config/authentication", { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    }),
                    obj.serviceCall({
                        url: fetchUrl.default(`/realm-config/authentication/chains/${name}`, { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    }),
                    obj.serviceCall({
                        url: fetchUrl.default("/realm-config/authentication/modules?_queryFilter=true", { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    })
                ]).then((response) => {
                    const authenticationData = response[0];
                    const chainData = response[1];
                    const modulesData = response[2];

                    if (chainData[0]._id === authenticationData[0].adminAuthModule) {
                        chainData[0].adminAuthModule = true;
                    }

                    if (chainData[0]._id === authenticationData[0].orgConfig) {
                        chainData[0].orgConfig = true;
                    }

                    _.each(chainData[0].authChainConfiguration, function (chainLink) {
                        moduleName = _.find(modulesData[0].result, { _id: chainLink.module });
                        // The server allows for deletion of modules that are in use within a chain. The chain itself
                        // will still have a reference to the deleted module.
                        // Below we are checking if the module is present. If it isn't the type is left undefined
                        if (moduleName) {
                            chainLink.type = moduleName.type;
                        }
                    });

                    return {
                        chainData: chainData[0],
                        modulesData: _.sortBy(modulesData[0].result, "_id")
                    };
                });
            },
            remove (realm, name) {
                return obj.serviceCall({
                    url: fetchUrl.default(`/realm-config/authentication/chains/${name}`, { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "DELETE"
                });
            },
            update (realm, name, data) {
                return obj.serviceCall({
                    url: fetchUrl.default(`/realm-config/authentication/chains/${name}`, { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "PUT",
                    data: JSON.stringify(data)
                });
            }
        },
        modules: {
            all (realm) {
                return obj.serviceCall({
                    url: fetchUrl.default("/realm-config/authentication/modules?_queryFilter=true", { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                }).done(SMSServiceUtils.sortResultBy("_id"));
            },
            create (realm, data, type) {
                return obj.serviceCall({
                    url: fetchUrl.default(`/realm-config/authentication/modules/${type}?_action=create`, { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST",
                    data: JSON.stringify(data)
                });
            },
            get (realm, name, type, options) {
                return obj.serviceCall(_.merge({
                    url: fetchUrl.default(`/realm-config/authentication/modules/${type}/${name}`, { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                }, options)).then((data) => data);
            },
            exists (realm, name) {
                var promise = $.Deferred(),
                    request = obj.serviceCall({
                        url: fetchUrl.default(
                            `/realm-config/authentication/modules?_queryFilter=_id eq "${name}"&_fields=_id`, { realm }
                        ),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    });

                request.done(function (data) {
                    promise.resolve(data.result.length > 0);
                });
                return promise;
            },
            remove (realm, name, type) {
                return obj.serviceCall({
                    url: fetchUrl.default(`/realm-config/authentication/modules/${type}/${name}`, { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "DELETE"
                });
            },
            update (realm, name, type, data) {
                return obj.serviceCall({
                    url: fetchUrl.default(`/realm-config/authentication/modules/${type}/${name}`, { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "PUT",
                    data: JSON.stringify(data)
                });
            },
            types: {
                all (realm) {
                    return obj.serviceCall({
                        url: fetchUrl.default("/realm-config/authentication/modules?_action=getAllTypes", { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                        type: "POST"
                    }).done(SMSServiceUtils.sortResultBy("name"));
                },
                get (realm, type) {
                    // TODO: change this to a proper server-side call when OPENAM-7242 is implemented
                    return obj.authentication.modules.types.all(realm).then(function (data) {
                        return _.findWhere(data.result, { "_id": type });
                    });
                }
            },
            schema (realm, type) {
                return obj.serviceCall({
                    url: fetchUrl.default(`/realm-config/authentication/modules/${type}?_action=schema`, { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST"
                }).then(function (data) {
                    return SMSServiceUtils.sanitizeSchema(data);
                });
            }
        }
    };

    return obj;
});
