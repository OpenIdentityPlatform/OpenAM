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

define("org/forgerock/openam/ui/uma/util/URLHelper", [
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function (Configuration, Constants, RealmHelper) {
    return {
        substitute: function (url) {
            return function () {
                var replacedUrl = url.replace("__api__", Constants.host + "/" + Constants.context + "/json")
                    .replace("__host__", Constants.host)
                    .replace("__context__", Constants.context)
                    .replace("__username__", Configuration.loggedUser.get("username"));

                return RealmHelper.decorateURIWithSubRealm(replacedUrl);
            };
        }
    };
});
