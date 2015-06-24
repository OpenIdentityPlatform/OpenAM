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
define('org/forgerock/openam/ui/admin/delegates/SMSGlobalDelegate', [
    'jquery',
    'org/forgerock/commons/ui/common/main/AbstractDelegate',
    'org/forgerock/commons/ui/common/util/Constants',
    'org/forgerock/openam/ui/admin/delegates/SMSDelegateUtils'
], function ($, AbstractDelegate, Constants, SMSDelegateUtils) {
    /**
     * @exports org/forgerock/openam/ui/admin/delegates/SMSGlobalDelegate
     */
    var obj = new AbstractDelegate(Constants.host + '/' + Constants.context + '/json/global-config/');

    obj.realms = {
        /**
         * Gets all realms.
         * @returns {Promise.<Object>} Service promise
         */
        all: function () {
            var promise = obj.serviceCall({
                url: 'realms?_queryFilter=true'
            });

            promise.done(function (data) {
                data.result = data.result.sort(function (a, b) {
                    if (a.active === b.active) {
                        // Within the active 'catagories' sort alphabetically
                        if (a.location < b.location) {
                            return -1;
                        } else {
                            return 1;
                        }
                    } else {
                        // Sort active realms before inactive realms
                        if (a.active === true) {
                            return -1;
                        } else {
                            return 1;
                        }
                    }
                });

                promise.done();
            });

            return promise;
        },

        /**
         * Gets a realm.
         * @param  {String} location Unescaped realm location (must have leading slash)
         * @returns {Promise.<Object>} Service promise
         */
        get: function (location) {
            var url = 'realms' + location,
                schemaPromise = obj.serviceCall({
                    url: url + '?_action=schema',
                    type: 'POST'
                }).done(SMSDelegateUtils.sanitize),
                valuesPromise = obj.serviceCall({
                    url: url
                });

            return $.when(schemaPromise, valuesPromise).then(function (schemaData, valuesData) {
                // FIXME: Remove when server provides this correctly
                schemaData[0].type = 'object';

                return {
                    schema: SMSDelegateUtils.sanitizeSchema(schemaData[0]),
                    values: valuesData[0]
                };
            });
        },

        /**
         * Retrieves schema and blank template needed to create a realm.
         */
        schema: function () {
            var url = 'realms',
                schemaPromise = obj.serviceCall({
                    url: url + '?_action=schema',
                    type: 'POST'
                }).done(SMSDelegateUtils.sanitize),
                valuesPromise = obj.serviceCall({
                    url: url
                });

            return $.when(schemaPromise, valuesPromise).then(function (schemaData, valuesData) {
                // FIXME: Remove when server provides this correctly
                schemaData[0].type = 'object';

                return {
                    schema: SMSDelegateUtils.sanitizeSchema(schemaData[0]),
                    values: valuesData[0]
                };
            });
        },

        /**
         * Removes a realm.
         * @param  {String} location Unescaped realm location (must have leading slash)
         * @returns {Promise} Service promise
         */
        remove: function (location) {
            return obj.serviceCall({
                url: 'realms' + location,
                type: 'DELETE'
            });
        },

        /**
         * Saves a realm.
         */
        save: function (data) {
            return obj.serviceCall({
                url: 'realms?_action=create',
                type: 'POST',
                data: JSON.stringify(data)
            });
        }
    };

    return obj;
});
