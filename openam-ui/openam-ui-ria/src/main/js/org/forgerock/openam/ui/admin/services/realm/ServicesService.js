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

define([
    "jquery",
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/models/JSONSchema",
    "org/forgerock/openam/ui/common/models/JSONValues",
    "org/forgerock/openam/ui/common/services/fetchUrl",
    "org/forgerock/openam/ui/common/util/array/arrayify",
    "org/forgerock/openam/ui/common/util/Promise"
], ($, _, AbstractDelegate, Constants, JSONSchema, JSONValues, fetchUrl, arrayify, Promise) => {
    /**
     * @exports org/forgerock/openam/ui/admin/services/realm/ServicesService
     */
    const obj = new AbstractDelegate(`${Constants.host}/${Constants.context}/json`);

    const getServiceSchema = function (realm, type) {
        return obj.serviceCall({
            url: fetchUrl.default(`/realm-config/services/${type}?_action=schema`, { realm }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST"
        }).then((response) => new JSONSchema(response));
    };
    const getServiceSubSchema = function (realm, serviceType, subSchemaType) {
        return obj.serviceCall({
            url: fetchUrl.default(`/realm-config/services/${serviceType}/${subSchemaType}?_action=schema`, { realm }),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST"
        }).then((response) => new JSONSchema(response));
    };

    obj.instance = {
        getAll (realm) {
            return obj.serviceCall({
                url: fetchUrl.default("/realm-config/services?_queryFilter=true", { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            }).then((response) => _.sortBy(response.result, "name"));
        },
        get (realm, type) {
            function getInstance () {
                return obj.serviceCall({
                    url: fetchUrl.default(`/realm-config/services/${type}`, { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                });
            }

            return Promise.all([getServiceSchema(realm, type), getInstance()]).then((response) => ({
                name: response[1][0]._type.name,
                schema: response[0],
                values: new JSONValues(response[1][0])
            }));
        },
        getInitialState (realm, type) {
            function getTemplate () {
                return obj.serviceCall({
                    url: fetchUrl.default(`/realm-config/services/${type}?_action=template`, { realm }),
                    headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                    type: "POST"
                }).then((response) => new JSONValues(response));
            }

            return Promise.all([getServiceSchema(realm, type), getTemplate()]).then((response) => ({
                schema: response[0],
                values: response[1]
            }));
        },
        remove (realm, types) {
            const promises = _.map(arrayify(types), (type) => obj.serviceCall({
                url: fetchUrl.default(`/realm-config/services/${type}`, { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "DELETE"
            }));

            return Promise.all(promises);
        },
        update (realm, type, data) {
            return obj.serviceCall({
                url: fetchUrl.default(`/realm-config/services/${type}`, { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "PUT",
                data: new JSONValues(data).toJSON()
            }).then((response) => new JSONValues(response));
        },
        create (realm, type, data) {
            return obj.serviceCall({
                url: fetchUrl.default(`/realm-config/services/${type}?_action=create`, { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST",
                data: new JSONValues(data).toJSON()
            });
        }
    };

    obj.type = {
        getCreatables (realm) {
            return obj.serviceCall({
                url: fetchUrl.default("/realm-config/services?_action=getCreatableTypes&forUI=true", { realm }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            }).then((response) => _.sortBy(response.result, "name"));
        },
        subSchema: {
            type: {
                getAll (realm, serviceType) {
                    return obj.serviceCall({
                        url: fetchUrl.default(`/realm-config/services/${serviceType}?_action=getAllTypes`, { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                        type: "POST"
                    }).then((response) => response.result);
                },
                getCreatables (realm, serviceType) {
                    return obj.serviceCall({
                        url: fetchUrl.default(
                            `/realm-config/services/${serviceType}?_action=getCreatableTypes`, { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                        type: "POST"
                    }).then((response) => _.sortBy(response.result, "name"));
                }
            },
            instance: {
                getAll (realm, serviceType) {
                    return obj.serviceCall({
                        url: fetchUrl.default(
                            `/realm-config/services/${serviceType}?_action=nextdescendents`, { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                        type: "POST"
                    }).then((response) => _.sortBy(response.result, "_id"));
                },
                get (realm, serviceType, subSchemaType, subSchemaInstance) {
                    function getInstance () {
                        return obj.serviceCall({
                            url: fetchUrl.default(
                                `/realm-config/services/${serviceType}/${subSchemaType}/${subSchemaInstance}`, { realm }
                            ),
                            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
                        }).then((response) => new JSONValues(response));
                    }

                    return Promise.all([getServiceSubSchema(realm, serviceType, subSchemaType), getInstance()])
                        .then((response) => ({
                            schema: response[0],
                            values: response[1]
                        }));
                },

                getInitialState (realm, serviceType, subSchemaType) {
                    function getTemplate (serviceType, subSchemaType) {
                        return obj.serviceCall({
                            url: fetchUrl.default(
                                `/realm-config/services/${serviceType}/${subSchemaType}?_action=template`, { realm }),
                            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                            type: "POST"
                        }).then((response) => new JSONValues(response));
                    }

                    return Promise.all([
                        getServiceSubSchema(realm, serviceType, subSchemaType),
                        getTemplate(serviceType, subSchemaType)
                    ]).then((response) => ({
                        schema: response[0],
                        values: response[1]
                    }));
                },

                remove (realm, serviceType, subSchemaType, subSchemaInstance) {
                    return obj.serviceCall({
                        url: fetchUrl.default(
                            `/realm-config/services/${serviceType}/${subSchemaType}/${subSchemaInstance}`, { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                        type: "DELETE"
                    });
                },

                update (realm, serviceType, subSchemaType, subSchemaInstance, data) {
                    return obj.serviceCall({
                        url: fetchUrl.default(
                            `/realm-config/services/${serviceType}/${subSchemaType}/${subSchemaInstance}`, { realm }),
                        headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                        type: "PUT",
                        data: JSON.stringify(data)
                    });
                },

                create (realm, serviceType, subSchemaType, data) {
                    return obj.serviceCall({
                        url: fetchUrl.default(
                            `/realm-config/services/${serviceType}/${subSchemaType}?_action=create`, { realm }),
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
