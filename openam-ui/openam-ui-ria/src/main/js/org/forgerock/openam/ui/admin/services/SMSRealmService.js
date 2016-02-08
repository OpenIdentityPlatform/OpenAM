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

define("org/forgerock/openam/ui/admin/services/SMSRealmService", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/SMSServiceUtils",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function ($, _, AbstractDelegate, Constants, SMSServiceUtils, RealmHelper) {
    /**
     * @exports org/forgerock/openam/ui/admin/services/SMSRealmService
     */
    var obj = new AbstractDelegate(Constants.host + "/" + Constants.context + "/json"),
        scopedByRealm = function (realm, path) {
            var encodedRealm = "";

            if (realm !== "/") {
                encodedRealm = RealmHelper.encodeRealm(realm);
            }

            return encodedRealm + "/realm-config/" + path;
        },
        getServiceSchema = function (realm, type) {
            // TODO temporary defaulting to an email service until server side is ready
            type = "email";

            return obj.serviceCall({
                url: scopedByRealm(realm, "services/" + type + "?_action=schema"),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            }).then(function (data) {
                return SMSServiceUtils.sanitizeSchema(data);
            });
        },
        getServiceSubSchema = function (realm, serviceType, subSchemaType) {
            return obj.serviceCall({
                url: scopedByRealm(realm, "services/" + serviceType + "/" + subSchemaType + "?_action=schema"),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            }).then(function (data) {
                return SMSServiceUtils.sanitizeSchema(data);
            });
        };

    obj.authentication = {
        get: function (realm) {
            var url = scopedByRealm(realm, "authentication");

            return $.when(
                obj.serviceCall({
                    url: url + "?_action=schema",
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST"
                }),
                obj.serviceCall({
                    url: url,
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                })
            ).then(function (schemaData, valuesData) {
                return {
                    schema: SMSServiceUtils.sanitizeSchema(schemaData[0]),
                    values: valuesData[0]
                };
            });
        },
        update: function (realm, data) {
            return obj.serviceCall({
                url: scopedByRealm(realm, "authentication"),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "PUT",
                data: JSON.stringify(data)
            });
        },
        chains: {
            all: function (realm) {
                var url = scopedByRealm(realm, "authentication");

                return $.when(
                    obj.serviceCall({
                        url: url + "/chains?_queryFilter=true",
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    }),
                    obj.serviceCall({
                        url: url,
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    })
                ).then(function (chainsData, authenticationData) {
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
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST",
                    data: JSON.stringify(data)
                });
            },
            get: function (realm, name) {
                var moduleName,
                    url = scopedByRealm(realm, "authentication");

                return $.when(
                    obj.serviceCall({
                        url: url,
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    }),
                    obj.serviceCall({
                        url: url + "/chains/" + name,
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    }),
                    obj.serviceCall({
                        url: url + "/modules?_queryFilter=true",
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    })
                ).then(function (authenticationData, chainData, modulesData) {

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
            remove: function (realm, name) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/chains/" + name),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "DELETE"
                });
            },
            update: function (realm, name, data) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/chains/" + name),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "PUT",
                    data: JSON.stringify(data)
                });
            }
        },
        modules: {
            all: function (realm) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/modules?_queryFilter=true"),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                }).done(SMSServiceUtils.sortResultBy("_id"));
            },
            create: function (realm, data, type) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/modules/" + type + "?_action=create"),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST",
                    data: JSON.stringify(data)
                });
            },
            get: function (realm, name, type) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/modules/" + type + "/" + name),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                }).then(function (data) {
                    return data;
                });
            },
            exists: function (realm, name) {
                var promise = $.Deferred(),
                    request = obj.serviceCall({
                        url: scopedByRealm(realm, 'authentication/modules?_queryFilter=_id eq "' + name +
                            '"&_fields=_id'),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    });

                request.done(function (data) {
                    promise.resolve(data.result.length > 0);
                });
                return promise;
            },
            remove: function (realm, name, type) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/modules/" + type + "/" + name),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "DELETE"
                });
            },
            update: function (realm, name, type, data) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/modules/" + type + "/" + name),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "PUT",
                    data: JSON.stringify(data)
                });
            },
            types: {
                all: function (realm) {
                    return obj.serviceCall({
                        url: scopedByRealm(realm, "authentication/modules/types?_queryFilter=true"),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                    }).done(SMSServiceUtils.sortResultBy("name"));
                },
                get: function (realm, type) {
                    // TODO: change this to a proper server-side call when OPENAM-7242 is implemented
                    return obj.authentication.modules.types.all(realm).then(function (data) {
                        return _.findWhere(data.result, { "_id": type });
                    });
                }
            },
            schema: function (realm, type) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "authentication/modules/" + type + "?_action=schema"),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST"
                }).then(function (data) {
                    return SMSServiceUtils.sanitizeSchema(data);
                });
            }
        }
    };

    // TODO AME-9702 and AME-9641: update when server-side is ready (do not forget to pass in the realm into the calls)
    obj.services = {
        instance: {
            getAll: function (realm) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "services?_queryFilter=true"),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                });
            },

            get: function (realm, type) {
                function getInstance () {
                    // TODO temporary defaulting to an email service until server side is ready
                    return $.Deferred().resolve({
                        "emailAddressAttribute": "mail", "password": null, "hostname": "smtp.gmail.com",
                        "sslState": "SSL", "port": 465, "subject": null, "from": "no-reply@openam.org", "message": null,
                        "emailImplClassName": "org.forgerock.openam.services.email.MailServerImpl",
                        "username": "forgerocksmtp"
                    });
                }

                function getName (type) {
                    return obj.serviceCall({
                        url: scopedByRealm(realm, "services?_action=getAllTypes"),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                        type: "POST"
                    }).then(function (all) {
                        return _.findWhere(all.result, { "_id": type }).name;
                    });
                }

                return $.when(getServiceSchema(realm, type), getInstance(), getName(type))
                    .then(function (schema, values, name) {
                        return {
                            schema: schema,
                            values: values,
                            name: name
                        };
                    });
            },

            getInitialState: function (realm, type) {
                function getTemplate (type) {
                    // TODO temporary defaulting to an email service until server side is ready
                    type = "email";

                    return obj.serviceCall({
                        url: scopedByRealm(realm, "services/" + type + "?_action=template"),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                        type: "POST"
                    });
                }

                return $.when(getServiceSchema(realm, type), getTemplate(type)).then(function (schema, values) {
                    return {
                        schema: schema,
                        values: values[0]
                    };
                });
            },

            remove: function (realmPath, ids) {
                var mock = [
                        { "_id":"audit", "name":"Audit Logging" },
                        { "_id":"dashboard", "name":"Dashboard" },
                        { "_id":"email", "name":"Email Service" }
                    ],
                    promises = _.map(ids, function (id) {
                        mock = _.reject(mock, { "_id": id });
                        return $.Deferred().resolve();
                    });

                return Promise.all(promises);
            },

            update: function () {
                return $.Deferred().resolve();
            },

            create: function () {
                return $.Deferred().resolve();
            }
        },

        type: {
            getCreatables: function (realm) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "services?_action=getCreatableTypes"),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST"
                });
            },

            subSchema: {
                instance: {
                    get: function (realm, serviceType, subSchemaType, subSchemaInstance) {
                        function getInstance () {
                            return obj.serviceCall({
                                url: scopedByRealm(realm, "services/" + serviceType + "/" + subSchemaType + "/" +
                                    subSchemaInstance),
                                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                            });
                        }

                        return $.when(getServiceSubSchema(realm, serviceType, subSchemaType), getInstance())
                            .then(function (subSchema, values) {
                                return {
                                    subSchema: subSchema,
                                    values: values
                                };
                            });
                    },

                    getInitialState: function (realm, serviceType, subSchemaType) {
                        function getTemplate (serviceType, subSchemaType) {
                            return obj.serviceCall({
                                url: scopedByRealm(realm, "services/" + serviceType + "/" + subSchemaType +
                                    "?_action=template"),
                                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                                type: "POST"
                            });
                        }

                        return $.when(
                            getServiceSubSchema(realm, serviceType, subSchemaType),
                            getTemplate(serviceType, subSchemaType)
                        ).then(function (subSchema, values) {
                            return {
                                subSchema: subSchema,
                                values: values[0]
                            };
                        });
                    },

                    remove: function () {
                        return $.Deferred().resolve();
                    },

                    update: function () {
                        return $.Deferred().resolve();
                    },

                    create: function () {
                        return $.Deferred().resolve();
                    }
                },

                type: {
                    getCreatables: function () {
                        return $.Deferred().resolve();
                    }
                }
            }
        }
    };

    obj.dashboard = {
        commonTasks: {
            all: function (realm) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "commontasks?_queryFilter=true"),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                });
            }
        }
    };

    return obj;
});
