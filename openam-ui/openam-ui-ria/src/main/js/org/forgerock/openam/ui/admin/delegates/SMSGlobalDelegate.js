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
define("org/forgerock/openam/ui/admin/delegates/SMSGlobalDelegate", [
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/delegates/SMSDelegateUtils"
], function ($, AbstractDelegate, Constants, SMSDelegateUtils) {
    /**
     * @exports org/forgerock/openam/ui/admin/delegates/SMSGlobalDelegate
     */
    var obj = new AbstractDelegate(Constants.host + "/" + Constants.context + "/json/global-config/"),
        schemaWithValues = function(url) {
            return $.when(
                obj.serviceCall({ url: url + "?_action=schema", type: "POST" }),
                obj.serviceCall({ url: url })
            ).then(function(schemaData, valuesData) {
                return {
                    schema: SMSDelegateUtils.sanitizeSchema(schemaData[0]),
                    values: valuesData[0]
                };
            });
        };

    obj.realms = {
        /**
         * Gets all realms.
         * @returns {Promise.<Object>} Service promise
         */
        all: function () {
            return obj.serviceCall({
                url: "realms?_queryFilter=true"
            }).done(function (data) {
                data.result = data.result.sort(function (a, b) {
                    if (a.active === b.active) {
                        // Within the active 'catagories' sort alphabetically
                        return a.path < b.path ? -1 : 1;
                    } else {
                        // Sort active realms before inactive realms
                        return a.active === true ? -1 : 1;
                    }
                });
            });
        },

        /**
         * Creates a realm.
         * @param  {Object} data Complete representation of realm
         * @returns {Promise} Service promise
         */
        create: function (data) {
            return obj.serviceCall({
                url: "realms?_action=create",
                type: "POST",
                data: JSON.stringify(data)
            });
        },

        /**
         * Gets a realm's schema together with it's values.
         * @param  {String} path Unescaped realm path (must have leading slash). e.g. "/myrealm"
         * @returns {Promise.<Object>} Service promise
         */
        get: function (path) {
            return schemaWithValues("realms" + path);
        },

        /**
         * Gets a blank realm's schema together with it's values.
         * @returns {Promise.<Object>} Service promise
         */
        schema: function () {
            return schemaWithValues("realms");
        },

        /**
         * Removes a realm.
         * @param  {String} path Unescaped realm path (must have leading slash). e.g. "/myrealm"
         * @returns {Promise} Service promise
         */
        remove: function (path) {
            return obj.serviceCall({ url: "realms" + path, type: "DELETE" });
        },

        /**
         * Updates a realm.
         * @param  {Object} data Complete representation of realm
         * @returns {Promise} Service promise
         */
        update: function (path, data) {
            return obj.serviceCall({
                url: "realms" + path,
                type: "PUT",
                data: JSON.stringify(data)
            });
        }
    };

    return obj;
});
