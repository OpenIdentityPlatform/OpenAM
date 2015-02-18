/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 ForgeRock AS. All Rights Reserved
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

/**
 * @author Julian Kigwana
 */

/*global define, _*/

define("org/forgerock/openam/ui/uma/delegates/UmaDelegate", [
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate"
], function (conf, constants, AbstractDelegate) {

    var obj = new AbstractDelegate(constants.host + "/" + constants.context + "/json");

    obj.ERROR_HANDLERS = {
        "Bad Request":              { status: "400" },
        "Not found":                { status: "404" },
        "Gone":                     { status: "410" },
        "Conflict":                 { status: "409" },
        "Internal Server Error":    { status: "500" },
        "Service Unavailable":      { status: "503" }
    };

    obj.serviceCall = function (args) {
        var realm = conf.globalData.auth.realm;
        if (realm !== "/" && // prevents urls like /openam/json//applicationtypes
            _.find(["/policies", "/users", "/groups"], function (w) { // the only endpoints that are currently realm "aware"
                return args.url.indexOf(w) === 0;
            })) {
            args.url = realm + args.url;
        }
        return AbstractDelegate.prototype.serviceCall.call(this, args);
    };

    obj.getPoliciesByQuery = function (query) {
        return obj.serviceCall({
            url: "/policies?_queryFilter=" + query,
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"}
        });
    };

    obj.getResourceSetFromId = function (uid) {
        return obj.serviceCall({
            url: "/users/" + conf.loggedUser.username + "/oauth2/resourcesets/" + uid,
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"}
        });
    };

    obj.getPoliciesById = function (uid) {
        return obj.serviceCall({
            url: "/users/" + conf.loggedUser.username + "/uma/policies/" + uid,
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"}
        });
    };

    obj.createPolicy = function(username, policyId, permissions) {
      return obj.serviceCall({
          url: "/users/" + username + "/uma/policies?_action=create",
          type: "POST",
          data: JSON.stringify({
              policyId: policyId,
              permissions: permissions
          }),
          errorsHandlers: obj.ERROR_HANDLERS
      });
    };

    obj.getUser = function(username) {
        return obj.serviceCall({
            url: "/users/" + username,
            headers: { "Cache-Control": "no-cache", "Accept-API-Version": "protocol=1.0,resource=2.0" }
        });
    };

    obj.searchUsers = function(query) {
        return obj.serviceCall({
            url: "/users?_queryId=" + query + "*",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"}
        });
    };

    return obj;
});
