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

/*global $, define, _ */

define("org/forgerock/openam/ui/dashboard/TrustedDevicesDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/main/Configuration"
], function(constants, AbstractDelegate, conf) {

    var obj = new AbstractDelegate(constants.host + '/' + constants.context + '/json/users/');

   obj.getTrustedDevices = function() {
       return obj.serviceCall({
            url: conf.loggedUser.uid + '/devices/trusted/?_queryId=*',
            headers: {"Cache-Control": "no-cache"}
        });
    };

   obj.deleteTrustedDevice = function(id) {
       return obj.serviceCall({
           url: conf.loggedUser.uid + '/devices/trusted/' + id,
           type: "DELETE"
       });
    };


    return obj;
});



