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
define('org/forgerock/openam/ui/admin/delegates/SMSRealmDelegate', [
    'jquery',
    'underscore',
    'org/forgerock/commons/ui/common/main/AbstractDelegate',
    'org/forgerock/commons/ui/common/util/Constants',
    'org/forgerock/openam/ui/admin/delegates/SMSDelegateUtils'
], function ($, _, AbstractDelegate, Constants, SMSDelegateUtils) {
    /**
     * @exports org/forgerock/openam/ui/admin/delegates/SMSRealmDelegate
     */
    var obj = new AbstractDelegate(Constants.host + '/' + Constants.context + '/json/realm-config/');

    obj.authentication = {
        get: function () {
            var url = 'authentication',
                schemaPromise = obj.serviceCall({
                    url: url + '?_action=schema',
                    type: 'POST'
                }).done(SMSDelegateUtils.sanitize),
                valuesPromise = obj.serviceCall({
                    url: url
                });

            return $.when(schemaPromise, valuesPromise).then(function (schemaData, valuesData) {
                return {
                    schema: SMSDelegateUtils.sanitizeSchema(schemaData[0]),
                    values: valuesData[0]
                };
            });
        },
        save: function (data) {
            return obj.serviceCall({
                url: 'authentication',
                type: 'PUT',
                data: JSON.stringify(data)
            });
        },

        chains: {
            get: function () {
                var promise = obj.serviceCall({
                        url: 'authentication/chains?_queryFilter=true'
                    });

                return $.when(promise).then(function (valuesData) {
                    return {
                        values: valuesData
                    };
                });
            },

            getChain: function (name) {
                var promise = obj.serviceCall({
                        url: 'authentication/chains/' + name
                    });

                return $.when(promise).then(function (valuesData) {
                    // FIXME: This is a temporay client side fix until AME-7202 is completed.
                    valuesData.authChainConfiguration = SMSDelegateUtils.authChainConfigurationToJson(valuesData.authChainConfiguration);
                    return {
                        values: valuesData
                    };
                });
            },

            getChainWithType: function (name) {
                var chainData = obj.serviceCall({
                        url: 'authentication/chains/' + name
                    }),
                    modulesPromise = obj.serviceCall({
                        url: 'authentication/modules?_queryFilter=true'
                    });

                return $.when(chainData, modulesPromise).then(function (chainData, modulesData) {

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

            getWithDefaults: function () {
                var url = 'authentication',
                    chainsPromise = obj.serviceCall({
                        url: url + '/chains?_queryFilter=true'
                    }),
                    valuesPromise = obj.serviceCall({
                        url: url
                    });

                return $.when(chainsPromise, valuesPromise).then(function (chainsData, valuesData) {
                    _.each(chainsData[0].result, function (obj) {
                        if (obj._id === valuesData[0].adminAuthModule) {
                            obj.defaultConfig = obj.defaultConfig || {};
                            obj.defaultConfig.adminAuthModule = true;
                        }

                        if (obj._id === valuesData[0].orgConfig ) {
                            obj.defaultConfig = obj.defaultConfig || {};
                            obj.defaultConfig.orgConfig = true;
                        }
                    });

                    return {
                        values: chainsData[0]
                    };
                });
            },
            remove: function (name) {
                return obj.serviceCall({
                    url: 'authentication/chains/' + name,
                    type: 'DELETE'
                });
            },
            save: function (name, data) {
                var cleaned = SMSDelegateUtils.authChainConfigurationToXml(data);
                return obj.serviceCall({
                    url: 'authentication/chains/' + name,
                    type: 'PUT',
                    data: JSON.stringify(cleaned)
                });
            },

            create: function (data) {
                return obj.serviceCall({
                    url: 'authentication/chains?_action=create',
                    type: 'POST',
                    data: JSON.stringify(data)
                });
            }
        },

        modules: {
            getModules: function () {
                var promise = obj.serviceCall({
                        url: 'authentication/modules?_queryFilter=true'
                    });

                return $.when(promise).then(function (valuesData) {
                    return {
                        values: valuesData
                    };
                });
            },
            getModuleTypes: function () {
                var promise = obj.serviceCall({
                        url: 'authentication/modules/types?_queryFilter=true'
                    });

                return $.when(promise).then(function (data) {
                    return _.sortBy(data.result, 'name');
                });
            },
            getModule: function (name) {
                return obj.serviceCall({
                    url: 'authentication/modules/' + name,
                    errorsHandlers: {
                        'Not Found': { status: '404' }
                    }
                });
            },
            removeModule: function (name) {
                return obj.serviceCall({
                    url: 'authentication/modules/' + name,
                    type: 'DELETE'
                });
            },
            hasModuleName: function (name) {
                var promise = $.Deferred(),
                    request = this.getModule(name);

                request.done(function () {
                    promise.resolve(false);
                }).fail(function () {
                    promise.resolve(true);
                });
                return promise;
            },
            saveModule: function (data) {
                return obj.serviceCall({
                    url: 'authentication/modules/',
                    type: 'PUT',
                    data: JSON.stringify(data)
                });
            }
        }
    };

    return obj;
});
