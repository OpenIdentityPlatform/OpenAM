/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS.
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

/*global define, window*/

define("config/process/ScriptsConfig", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/EventManager"
], function (Constants, EventManager) {
    return [
        {
            startEvent: Constants.EVENT_RETURN_TO_AM_CONSOLE,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Configuration"
            ],
            processDescription: function (event, conf) {
                var realm = conf.globalData.auth.realm;
                window.location.href = "/" + Constants.context + "/realm/RMRealm?RMRealm.tblDataActionHref=" + encodeURIComponent(realm);
            }
        },
        {
            startEvent: Constants.EVENT_GO_TO_POLICY_EDITOR,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Configuration"
            ],
            processDescription: function (event, conf) {
                var realm = conf.globalData.auth.realm !== '/' ? '?realm=' + conf.globalData.auth.realm : '';
                window.location.href = "/" + Constants.context + "/policyEditor" + realm + "#apps/";
            }
        },
        {
            startEvent: Constants.EVENT_HANDLE_DEFAULT_ROUTE,
            description: "",
            dependencies: [
                "org/forgerock/commons/ui/common/main/Router"
            ],
            processDescription: function (event, router) {
                EventManager.sendEvent(Constants.EVENT_CHANGE_VIEW, {route: router.configuration.routes.listScripts});
            }
        }
    ];
});