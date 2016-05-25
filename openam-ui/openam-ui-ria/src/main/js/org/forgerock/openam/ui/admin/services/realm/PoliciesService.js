/*
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
 * Portions copyright 2014-2016 ForgeRock AS.
 */

/**
* @module org/forgerock/openam/ui/admin/services/realm/PoliciesService
*/
define([
    "lodash",
    "org/forgerock/commons/ui/common/main/AbstractDelegate",
    "org/forgerock/commons/ui/common/util/Constants",
    "org/forgerock/openam/ui/admin/utils/AdministeredRealmsHelper",
    "org/forgerock/openam/ui/common/util/RealmHelper"
], function (_, AbstractDelegate, Constants, AdministeredRealmsHelper, RealmHelper) {
    var obj = new AbstractDelegate(`${Constants.host}/${Constants.context}/json`),
        getCurrentAdministeredRealm = function () {
            var realm = AdministeredRealmsHelper.getCurrentRealm();
            return realm === "/" ? "" : RealmHelper.encodeRealm(realm);
        };

    obj.getApplicationType = function (type) {
        return obj.serviceCall({
            url: RealmHelper.decorateURLWithOverrideRealm(`/applicationtypes/${type}`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.getDecisionCombiners = function () {
        return obj.serviceCall({
            url: RealmHelper.decorateURLWithOverrideRealm("/decisioncombiners/?_queryId=&_fields=title"),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.getEnvironmentConditions = function () {
        return obj.serviceCall({
            url: RealmHelper.decorateURLWithOverrideRealm("/conditiontypes?_queryId=&_fields=title,logical,config"),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.getSubjectConditions = function () {
        return obj.serviceCall({
            url: RealmHelper.decorateURLWithOverrideRealm("/subjecttypes?_queryId=&_fields=title,logical,config"),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.getAllUserAttributes = function () {
        return obj.serviceCall({
            url: RealmHelper.decorateURLWithOverrideRealm(`${
                getCurrentAdministeredRealm()
                }/subjectattributes?_queryFilter=true`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.queryIdentities = function (name, query) {
        return obj.serviceCall({
            url: RealmHelper.decorateURLWithOverrideRealm(`${
                getCurrentAdministeredRealm()
                }/${name}?_queryId=${query}*`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.getUniversalId = function (name, type) {
        return obj.serviceCall({
            url: RealmHelper.decorateURLWithOverrideRealm(`${
                getCurrentAdministeredRealm()
                }/${type}/${name}?_fields=universalid`),
            headers: { "Cache-Control": "no-cache", "Accept-API-Version": "protocol=1.0,resource=2.0" }
        });
    };

    obj.getDataByType = function (type) {
        return obj.serviceCall({
            url: RealmHelper.decorateURLWithOverrideRealm(`${getCurrentAdministeredRealm()}/${type}?_queryFilter=true`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.getScriptById = function (id) {
        return obj.serviceCall({
            url: RealmHelper.decorateURLWithOverrideRealm(`${getCurrentAdministeredRealm()}/scripts/${id}`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.getAllRealms = function () {
        return obj.serviceCall({
            url: RealmHelper.decorateURLWithOverrideRealm("/realms?_queryFilter=true"),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    obj.importPolicies = function (data) {
        return obj.serviceCall({
            serviceUrl: `${Constants.host}/${Constants.context}`,
            url: RealmHelper.decorateURLWithOverrideRealm(`/xacml${getCurrentAdministeredRealm()}/policies`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" },
            type: "POST",
            data
        });
    };

    obj.listResourceTypes = function () {
        return obj.serviceCall({
            url: RealmHelper.decorateURLWithOverrideRealm(`${
                getCurrentAdministeredRealm()
                }/resourcetypes?_queryFilter=name+eq+${encodeURIComponent('"^(?!Delegation Service$).*"')}`),
            headers: { "Accept-API-Version": "protocol=1.0,resource=1.0" }
        });
    };

    return obj;
});
