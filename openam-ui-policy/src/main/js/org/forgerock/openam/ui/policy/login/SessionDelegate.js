/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All rights reserved.
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

/*global $, define, _, console */

define("org/forgerock/openam/ui/policy/login/SessionDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager"
], function(constants, AbstractDelegate, configuration, eventManager) {

    var obj = new AbstractDelegate(constants.host + "/"+ constants.context + "/json/users");

    /**
     * Checks if logged in and returns users id
     */
    obj.getProfile = function(successCallback, errorCallback, errorsHandlers) {
        return obj.serviceCall({
            url: "?_action=idFromSession",
            data: "{}",
            type: "POST",
            success: function (data) {
                configuration.globalData.auth.successURL = data.successURL;
                configuration.globalData.auth.fullLoginURL = data.fullLoginURL;
                successCallback({userid: {id : data.id, realm: data.realm}, username: data.dn, roles: ["ui-admin","ui-user"] });
            },
            error: errorCallback,
            errorsHandlers: errorsHandlers
        });
    };

    return obj;
});
