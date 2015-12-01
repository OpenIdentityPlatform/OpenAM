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
 * Copyright 2015 ForgeRock AS.
 */

define("org/forgerock/openam/ui/common/delegates/ServerDelegate", [
    "jquery",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants"
], function ($, AbstractDelegate, Constants) {
    /**
     * @exports org/forgerock/openam/ui/common/delegates/ServerDelegate
     */
    var obj = new AbstractDelegate(Constants.host + "/" + Constants.context + "/json/serverinfo");

    obj.version = function () {
        return obj.serviceCall({
            url: "/version",
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        }).then(function (data) {
            return data.version + " " + $.t("common.form.build") + " " + data.revision + "(" + data.date + ")";
        });
    };

    return obj;
});
