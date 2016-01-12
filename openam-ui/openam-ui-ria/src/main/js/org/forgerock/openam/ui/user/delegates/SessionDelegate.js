/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014-2015 ForgeRock AS.
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

define("org/forgerock/openam/ui/user/delegates/SessionDelegate", [
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants"
], function (_, AbstractDelegate, Constants) {
    var obj = new AbstractDelegate(Constants.host + "/" + Constants.context + "/json/sessions");

    function performServiceCall (options) {
        return obj.serviceCall(_.merge({
            type: "POST",
            data: {},
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.1" },
            errorsHandlers: { "Bad Request": { status: 400 } }
        }, options));
    }

    obj.getMaxIdle = function (token) {
        return performServiceCall({
            url: "?_action=getMaxIdle&tokenId=" + token,
            suppressSpinner: true
        });
    };

    obj.getTimeLeft = function (token) {
        return performServiceCall({
            url: "?_action=getTimeLeft&tokenId=" + token,
            suppressSpinner: true
        });
    };

    obj.isSessionValid = function (token) {
        return performServiceCall({
            url: "/" + token + "?_action=validate"
        });
    };

    obj.logout = function (token) {
        return performServiceCall({
            url: "/" + token + "?_action=logout"
        });
    };

    return obj;
});
