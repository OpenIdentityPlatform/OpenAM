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

/*global define, _*/

define("org/forgerock/openam/ui/policy/PolicyDelegate", [
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
            _.find(["/applications", "/policies", "/referrals", "/users"], function (w) { // the only four options which are currently realm "aware"
                return args.url.indexOf(w) === 0;
            })) {
            args.url = realm + args.url;
        }
        return AbstractDelegate.prototype.serviceCall.call(this, args);
    };

    obj.getApplicationType = function (type) {
        return obj.serviceCall({
            url: "/applicationtypes/" + type,
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"}
        });
    };

    obj.getApplicationByName = function (name) {
        return obj.serviceCall({
            url: "/applications/" + name,
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"}
        });
    };

    obj.updateApplication = function (name, data) {
        return obj.serviceCall({
            url: "/applications/" + name,
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            type: "PUT",
            data: JSON.stringify(data),
            errorsHandlers: obj.ERROR_HANDLERS
        });
    };

    obj.createApplication = function (data) {
        return obj.serviceCall({
            url: "/applications/?_action=create",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            type: "POST",
            data: JSON.stringify(data),
            errorsHandlers: obj.ERROR_HANDLERS
        });
    };

    obj.deleteApplication = function (name) {
        return obj.serviceCall({
            url: "/applications/" + name,
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            type: "DELETE",
            errorsHandlers: obj.ERROR_HANDLERS
        });
    };

    obj.getDecisionCombiners = function () {
        return obj.serviceCall({
            url: "/decisioncombiners/?_queryId=&_fields=title",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"}
        });
    };

    obj.getEnvironmentConditions = function () {
        return obj.serviceCall({
            url: "/conditiontypes?_queryId=&_fields=title,logical,config",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"}
        });
    };

    obj.getSubjectConditions = function () {
        return obj.serviceCall({
            url: "/subjecttypes?_queryId=&_fields=title,logical,config",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"}
        });
    };

    obj.getPolicyByName = function (name) {
        return obj.serviceCall({
            url: "/policies/" + encodeURIComponent(name),
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"}
        });
    };

    obj.updatePolicy = function (name, data) {
        return obj.serviceCall({
            url: "/policies/" + encodeURIComponent(name),
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            type: "PUT",
            data: JSON.stringify(data),
            errorsHandlers: obj.ERROR_HANDLERS
        });
    };

    obj.createPolicy = function (data) {
        return obj.serviceCall({
            url: "/policies/" + encodeURIComponent(data.name),
            headers: { "If-None-Match": "*", "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "PUT",
            data: JSON.stringify(data),
            errorsHandlers: obj.ERROR_HANDLERS
        });
    };

    obj.deletePolicy = function (name) {
        return obj.serviceCall({
            url: "/policies/" + encodeURIComponent(name),
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            type: "DELETE"
        });
    };

    obj.getReferralByName = function (name) {
        return obj.serviceCall({
            url: "/referrals/" + name,
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"}
        });
    };

    obj.updateReferral = function (name, data) {
        return obj.serviceCall({
            url: "/referrals/" + name,
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            type: "PUT",
            data: JSON.stringify(data),
            errorsHandlers: obj.ERROR_HANDLERS
        });
    };

    obj.createReferral = function (data) {
        return obj.serviceCall({
            url: "/referrals/?_action=create",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            type: "POST",
            data: JSON.stringify(data),
            errorsHandlers: obj.ERROR_HANDLERS
        });
    };

    obj.deleteReferral = function (name) {
        return obj.serviceCall({
            url: "/referrals/" + name,
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"},
            type: "DELETE"
        });
    };

    obj.getAllUserAttributes = function () {
        return obj.serviceCall({
            url: "/subjectattributes?_queryFilter=true",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"}
        });
    };

    obj.queryIdentities = function (name, query) {
        return obj.serviceCall({
            url: "/" + name + "?_queryId=" + query + "*",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"}
        });
    };

    obj.getUniversalId = function (name, type) {
        return obj.serviceCall({
            url: "/" + type + "/" + name + "?_fields=universalid",
            headers: {"Cache-Control": "no-cache", "Accept-API-Version": "protocol=1.0,resource=2.0"}
        });
    };

    obj.getAllRealms = function () {
        return obj.serviceCall({
            url: "/realms?_queryFilter=true",
            headers: {"Accept-API-Version": "protocol=1.0,resource=1.0"}
        });
    };

    obj.importPolicies = function (data) {
        var subrealm = conf.globalData.auth.realm !== "/" ? conf.globalData.auth.realm : "";
        return obj.serviceCall({
            serviceUrl: constants.host + "/" + constants.context,
            url: "/xacml" + subrealm +"/policies",
            type: "POST",
            data: data,
            errorsHandlers: obj.ERROR_HANDLERS
        });
    };

    return obj;
});
