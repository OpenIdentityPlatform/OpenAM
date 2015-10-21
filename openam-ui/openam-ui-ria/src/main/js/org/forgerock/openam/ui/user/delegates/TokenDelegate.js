/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2011-2015 ForgeRock AS.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

define("org/forgerock/openam/ui/user/delegates/TokenDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate"
], function (constants, AbstractDelegate) {
    var obj = new AbstractDelegate(constants.host + "/" + constants.context + "/frrest/oauth2/token");

    /**
     * Gets all the OAuth 2 tokens for a user
     * @param successCallback
     * @param errorCallback
     */
    obj.getAllTokens = function (successCallback, errorCallback) {

        obj.serviceCall({ url: "/?_queryid=*", success: function (data) {
            if (successCallback) {
                successCallback(data.result);
            }
        }, error: errorCallback });

    };

    /**
     *  Deletes a token given a tokenID
     * @param successCallback success handler
     * @param errorCallback error handler
     * @param id TokenID
     */
    obj.deleteToken = function (successCallback, errorCallback, id) {
        obj.serviceCall({ type: "DELETE", url: "/" + id, success: function () {
            if (successCallback) {
                successCallback(id);
            }
        }, error: errorCallback(id) });
    };

    /**
     * Gets a token given a tokenID
     * @param successCallback success handler
     * @param errorCallback error handler
     * @param id TokenID
     */
    obj.getTokenByID = function (successCallback, errorCallback, id) {
        obj.serviceCall({ url: "/" + id, success: function (data) {
            if (successCallback) {
                successCallback(data);
            }
        }, error: errorCallback });
    };

    /**
     * See AbstractDelegate.patchEntityDifferences
     */
    obj.patchTokenDifferences = function (oldTokenData, newTokenData, successCallback, errorCallback,
                                          noChangesCallback) {
        obj.patchEntityDifferences({
            id: oldTokenData._id,
            rev: oldTokenData._rev
        }, oldTokenData, newTokenData, successCallback, errorCallback, noChangesCallback);
    };

    /**
     * See AbstractDelegate.patchEntity
     */
    obj.patchSelectedTokenAttributes = function (id, rev, patchDefinitionObject, successCallback, errorCallback,
                                                 noChangesCallback) {
        obj.patchEntity({
            id: id,
            rev: rev
        }, patchDefinitionObject, successCallback, errorCallback, noChangesCallback);
    };

    return obj;
});
