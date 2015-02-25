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

/*global define, _ */

define("org/forgerock/openam/ui/policy/SiteConfigurationDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/Configuration",
    "org/forgerock/commons/ui/common/main/EventManager",
    "org/forgerock/commons/ui/common/util/UIUtils",
    "org/forgerock/commons/ui/common/main/AbstractDelegate"

], function(constants, conf, eventManager, uiUtils, AbstractDelegate) {
    var obj = new AbstractDelegate('');
    
    obj.getConfiguration = function(successCallback, errorCallback) {
        console.info("Getting configuration");

        var urlParams = uiUtils.convertCurrentUrlToJSON().params;
        if (urlParams) {
            conf.globalData.auth.realm = urlParams.realm;
        } else {
            conf.globalData.auth.realm = undefined;
        }

        obj.serviceCall({url: "configuration.json", 
            success: function (data) {
                if (successCallback) {
                    successCallback(data.configuration);
                }
            }, 
            error: function (data) {
                if (errorCallback) {
                    errorCallback({lang: "en"});
                }
            }, 
            headers: {}
        });

        obj.serviceCall({
            serviceUrl: constants.host + "/" + constants.context + "/json",
            url: "/serverinfo/*",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"}
        }).done(function (info) {
            _.extend(conf.globalData, {serverInfo: info});
        });
    };

    return obj;
});


