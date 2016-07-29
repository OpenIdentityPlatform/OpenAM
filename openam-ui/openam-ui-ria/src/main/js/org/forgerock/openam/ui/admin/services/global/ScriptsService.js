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

/**
 * @module org/forgerock/openam/ui/admin/services/global/ScriptsService
 */
define([
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/services/fetchUrl"
], (AbstractDelegate, Constants, fetchUrl) => {
    const obj = new AbstractDelegate(`${Constants.host}/${Constants.context}/json`);

    obj.scripts = {
        /**
         * Gets all script's contexts.
         * @returns {Promise.<Object>} Service promise
         */
        getAllContexts () {
            return obj.serviceCall({
                url: fetchUrl.legacy("/global-config/services/scripting/contexts?_queryFilter=true", { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            });
        },

        /**
         * Gets a default global script's context.
         * @returns {Promise.<Object>} Service promise
         */
        getDefaultGlobalContext () {
            return obj.serviceCall({
                url: fetchUrl.legacy("/global-config/services/scripting", { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
            });
        },

        /**
         * Gets a script's schema.
         * @returns {Promise.<Object>} Service promise
         */
        getSchema () {
            return obj.serviceCall({
                url: fetchUrl.legacy("/global-config/services/scripting?_action=schema", { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            });
        },

        /**
         * Gets a script context's schema.
         * @returns {Promise.<Object>} Service promise
         */
        getContextSchema () {
            return obj.serviceCall({
                url: fetchUrl.legacy("/global-config/services/scripting/contexts?_action=schema", { realm: false }),
                headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
                type: "POST"
            });
        }
    };

    return obj;
});
