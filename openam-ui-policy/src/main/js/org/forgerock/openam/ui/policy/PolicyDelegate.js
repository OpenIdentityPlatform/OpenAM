/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 ForgeRock AS. All Rights Reserved
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
 * @author Eugenia Sergueeva
 */

/*global define*/

define("org/forgerock/openam/ui/policy/PolicyDelegate", [
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/commons/ui/common/main/AbstractDelegate"
], function (constants, AbstractDelegate) {

    var obj = new AbstractDelegate(constants.host + "/openam/json");

    obj.getAllApplications = function (successCallback, errorCallback) {
        return obj.serviceCall({
            url: "/applications?_queryFilter=true",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            success: function (data) {
                if (successCallback) {
                    successCallback(data);
                }
            },
            error: errorCallback
        });
    };

    obj.getApplicationTypes = function (successCallback, errorCallback) {
        return obj.serviceCall({
            url: "/applicationtypes?_queryFilter=true",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            success: function (data) {
                if (successCallback) {
                    successCallback(data);
                }
            },
            error: errorCallback
        });
    };

    obj.getApplicationByName = function (name, successCallback, errorCallback) {
        return obj.serviceCall({
            url: "/applications/" + name,
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            success: function (data) {
                if (successCallback) {
                    successCallback(data);
                }
            },
            error: errorCallback
        });
    };

    obj.updateApplication = function (appName, data, successCallback, errorCallback) {
        return obj.serviceCall({
            url: "/applications/" + appName,
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            type: "PUT",
            data: JSON.stringify(data),
            success: function (data) {
                if (successCallback) {
                    successCallback(data);
                }
            },
            error: errorCallback
        });
    };

    obj.createApplication = function (data, successCallback, errorCallback) {
        return obj.serviceCall({
            url: "/applications/?_action=create",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            type: "POST",
            data: JSON.stringify(data),
            success: function (data) {
                if (successCallback) {
                    successCallback(data);
                }
            },
            error: errorCallback
        });
    };

    obj.getDecisionCombiners = function (successCallback, errorCallback) {
        return obj.serviceCall({
            url: "/decisioncombiners/?_queryId=&_fields=title",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            success: function (data) {
                if (successCallback) {
                    successCallback(data);
                }
            },
            error: errorCallback
        });
    };

    obj.getEnvironmentConditions = function (successCallback, errorCallback) {
        return obj.serviceCall({
            url: "/conditiontypes?_queryID=&_fields=title,logical,config",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            success: function (data) {
                if (successCallback) {
                    successCallback(data);
                }
            },
            error: errorCallback
        });
    };

    obj.getSubjectConditions = function (successCallback, errorCallback) {
        return obj.serviceCall({
            url: "/subjecttypes?_queryID=&_fields=title,logical,config",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            success: function (data) {
                if (successCallback) {
                    successCallback(data);
                }
            },
            error: errorCallback
        });
    };

    obj.getApplicationPolicies = function (appName, successCallback, errorCallback) {
        var filter = encodeURIComponent('applicationName eq "' + appName + '"');

        return obj.serviceCall({
            url: "/policies?_queryFilter=" + filter,
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            success: function (data) {
                if (successCallback) {
                    successCallback(data);
                }
            },
            error: errorCallback
        });
    };

    obj.getPolicyByName = function (name, successCallback, errorCallback) {
        return obj.serviceCall({
            url: "/policies/" + name,
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            success: function (data) {
                if (successCallback) {
                    successCallback(data);
                }
            },
            error: errorCallback
        });
    };

    obj.updatePolicy = function (policyName, data, successCallback, errorCallback) {
        return obj.serviceCall({
            url: "/policies/" + policyName,
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            type: "PUT",
            data: JSON.stringify(data),
            success: function (data) {
                if (successCallback) {
                    successCallback(data);
                }
            },
            error: errorCallback
        });
    };

    obj.createPolicy = function (data, successCallback, errorCallback) {
        return obj.serviceCall({
            url: "/policies/" + data.name,
            headers: { "If-None-Match": "*", "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "PUT",
            data: JSON.stringify(data),
            success: function (data) {
                if (successCallback) {
                    successCallback(data);
                }
            },
            error: errorCallback
        });
    };

    return obj;
});
