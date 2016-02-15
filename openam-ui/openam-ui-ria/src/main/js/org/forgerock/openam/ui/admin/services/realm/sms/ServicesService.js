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
 * Copyright 2016 ForgeRock AS.
 */

define("org/forgerock/openam/ui/admin/services/realm/sms/ServicesService", [
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/services/SMSServiceUtils",
    "org/forgerock/openam/ui/common/util/Promise",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function ($, _, AbstractDelegate, Constants, SMSServiceUtils, Promise, RealmHelper) {
    /**
     * @exports org/forgerock/openam/ui/admin/services/realm/sms/ServicesService
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

    obj.instance = {
        getAll: function (realm) {
            return obj.serviceCall({
                url: scopedByRealm(realm, "services?_queryFilter=true"),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            });
        },
        get: function (realm, type) {
            function getInstance () {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "services/" + type),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                }).then(function (response) {
                    return response;
                });
            }

            function getName () {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "services?_action=getAllTypes"),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST"
                }).then(function (all) {
                    return _.findWhere(all.result, { "_id": type }).name;
                });
            }

            function getSubSchemaTypes () {
                // return obj.serviceCall({
                //     url: scopedByRealm(realm, "services/" + type + "?_action=getAllTypes"),
                //     headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                //     type: "POST"
                // });

                return $.Deferred().resolve([
                    { "_id":"CSV", "description":"CSV" },
                    { "_id":"JDBC", "description":"CSV" },
                    { "_id":"Syslog", "description":"JDBC" }
                ]);
            }

            function getSubSchemaInstances () {
                // return obj.serviceCall({
                //     url: scopedByRealm(realm, "services/" + type + "?_queryFilter=true"),
                //     headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                // });

                return $.Deferred().resolve([
                    { "_id":"csv1", "type":"CSV", "typeDescription":"CSV" },
                    { "_id":"csv2", "type":"CSV", "typeDescription":"CSV" },
                    { "_id":"jdbc", "type":"JDBC", "typeDescription":"JDBC" }
                ]);
            }

            function getSubSchemaCreatables () {
                // return obj.serviceCall({
                //     url: scopedByRealm(realm, "services/" + type + "?_action=getCreatableTypes"),
                //     headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                //     type: "POST"
                // });

                return $.Deferred().resolve([
                    { "_id":"CSV", "description":"CSV" },
                    { "_id":"JDBC", "description":"JDBC" },
                    { "_id":"Syslog", "description":"Syslog" }
                ]);
            }

            function getSubSchema () {
                return getSubSchemaTypes(realm, type).then(function (types) {
                    if (types.length > 0) {
                        return Promise.all([getSubSchemaInstances(), getSubSchemaCreatables()])
                            .then(function (result) {
                                return {
                                    instances: result[0],
                                    creatables: result[1]
                                };
                            });
                    } else {
                        return null;
                    }
                });
            }

            return Promise.all([getServiceSchema(realm, type), getInstance(), getName(), getSubSchema()])
                .then(function (data) {
                    var schema = data[0],
                        subSchema = data[3];

                    return {
                        schema: schema,
                        values: data[1],
                        name: data[2],
                        subschema: subSchema
                    };
                });
        },
        getInitialState: function (realm, type) {
            function getTemplate (type) {
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
        remove: function (realm, types) {
            if (!_.isArray(types)) {
                types = [types];
            }

            var promises = _.map(types, function (type) {
                return obj.serviceCall({
                    url: scopedByRealm(realm, "services/" + type),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "DELETE"
                });
            });

            return Promise.all(promises);
        },
        update: function (realm, type, data) {
            return obj.serviceCall({
                url: scopedByRealm(realm, "services/" + type),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "PUT",
                data: JSON.stringify(data)
            });
        },
        create: function (realm, type, data) {
            return obj.serviceCall({
                url: scopedByRealm(realm, "services/" + type + "?_action=create"),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST",
                data: JSON.stringify(data)
            });
        }
    };

    obj.type = {
        getCreatables: function (realm) {
            function sortByName (response) {
                return _.sortBy(response.result, "name");
            }

            var promise = obj.serviceCall({
                url: scopedByRealm(realm, "services?_action=getCreatableTypes"),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            });

            return promise.then(sortByName);
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

                remove: function (realm, serviceType, subSchemaType, subSchemaInstance) {
                    return obj.serviceCall({
                        url: scopedByRealm(realm, "services/" + serviceType + "/" + subSchemaType +
                            "/" + subSchemaInstance),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                        type: "DELETE"
                    });
                },

                update: function (realm, serviceType, subSchemaType, subSchemaInstance, data) {
                    return obj.serviceCall({
                        url: scopedByRealm(realm, "services/" + serviceType + "/" + subSchemaType +
                            "/" + subSchemaInstance),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                        type: "PUT",
                        data: JSON.stringify(data)
                    });
                },

                create: function (realm, serviceType, subSchemaType, data) {
                    return obj.serviceCall({
                        url: scopedByRealm(realm, "services/" + serviceType + "/" + subSchemaType + "?_action=create"),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                        type: "POST",
                        data: JSON.stringify(data)
                    });
                }
            }
        }
    };

    return obj;
});
