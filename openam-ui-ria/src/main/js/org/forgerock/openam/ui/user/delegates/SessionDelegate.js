/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014 ForgeRock AS.
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
 * @author Halina Shabuldayeva
 */

/*global document, $, define, _, window */

define("org/forgerock/openam/ui/user/delegates/SessionDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate"
], function(constants, AbstractDelegate) {

    var obj = new AbstractDelegate(constants.host + "/"+ constants.context + "/json/sessions");

    obj.isSessionValid = function (tokenCookie){
        return obj.serviceCall({
            type: "POST",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.1"},
            data: {},
            url: "/" + tokenCookie + "?_action=validate",
            errorsHandlers: {"Bad Request": {status: 400}}
        });
    };

    obj.logout = function (tokenCookie) {
        return obj.serviceCall({
            type: "POST",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.1"},
            data: "{}",
            url: "/" + tokenCookie + "?_action=logout",
            errorsHandlers: {"Bad Request": {status: 400}}
        });
    };

    return obj;
});
