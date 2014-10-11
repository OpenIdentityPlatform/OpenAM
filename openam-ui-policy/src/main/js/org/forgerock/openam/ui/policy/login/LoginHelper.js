/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All Rights Reserved
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

/*global define, window */

define("org/forgerock/openam/ui/policy/login/LoginHelper", [
    "org/forgerock/openam/ui/policy/login/SessionDelegate",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/openam/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractConfigurationAware",
    "org/forgerock/commons/ui/common/main/ServiceInvoker",
    "org/forgerock/commons/ui/common/main/Configuration"
], function (sessionDelegate, eventManager, constants, AbstractConfigurationAware, serviceInvoker, conf) {
    var obj = new AbstractConfigurationAware(),
        reauthenticate = function () {
            window.location.href = constants.host + "/"+ constants.context + "?goto=" + encodeURIComponent(window.location.href);
        };

    obj.login = function (params, successCallback, errorCallback) {

    };

    obj.logout = function (successCallback) {
        // Do not invoke the successCallback, because we don't want the default forgerock-ui behavior
        sessionDelegate.logout().always(reauthenticate);
    };

    obj.getLoggedUser = function (successCallback, errorCallback) {
        return sessionDelegate.getProfile(function (user) {
            if (conf.globalData.auth.realm === undefined) {
                conf.globalData.auth.realm = user.userid.realm;
            }
            if (successCallback) {
                successCallback(user);
            }
        }, 
        reauthenticate, 
        {"serverError": {status: "503"}, "unauthorized": {status: "401"}});
    };

    return obj;
});