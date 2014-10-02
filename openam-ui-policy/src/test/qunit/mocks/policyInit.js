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

/*global require, define*/
define([
    "text!templates/policy/ActionsTemplate.html",
    "text!templates/policy/AddNewResourceTemplate.html",
    "text!templates/policy/ApplicationTableCellActionsTemplate.html",
    "text!templates/policy/ApplicationTableGlobalActionsTemplate.html",
    "text!templates/policy/BaseTemplate.html",
    "text!templates/policy/ConditionAttrBoolean.html",
    "text!templates/policy/ConditionAttrEnum.html",
    "text!templates/policy/ConditionAttrString.html",
    "text!templates/policy/ConditionAttrTimeDate.html",
    "text!templates/policy/EditApplicationTemplate.html",
    "text!templates/policy/EditEnvironmentTemplate.html",
    "text!templates/policy/EditPolicyTemplate.html",
    "text!templates/policy/EditSubjectTemplate.html",
    "text!templates/policy/ManageApplicationsTemplate.html",
    "text!templates/policy/ManageRulesTemplate.html",
    "text!templates/policy/ManagePoliciesTemplate.html",
    "text!templates/policy/OperatorRulesTemplate.html",
    "text!templates/policy/PoliciesTableGlobalActionsTemplate.html",
    "text!templates/policy/ResourcesListTemplate.html",
    "text!templates/policy/ReviewApplicationStepTemplate.html",
    "text!templates/policy/ReviewPolicyStepTemplate.html"
], function () {
    /* an unfortunate need to duplicate the file names here, but I haven't
     yet found a way to fool requirejs into doing dynamic dependencies */
    var staticFiles = [
            "templates/policy/ActionsTemplate.html",
            "templates/policy/AddNewResourceTemplate.html",
            "templates/policy/ApplicationTableCellActionsTemplate.html",
            "templates/policy/ApplicationTableGlobalActionsTemplate.html",
            "templates/policy/BaseTemplate.html",
            "templates/policy/ConditionAttrBoolean.html",
            "templates/policy/ConditionAttrEnum.html",
            "templates/policy/ConditionAttrString.html",
            "templates/policy/ConditionAttrTimeDate.html",
            "templates/policy/EditApplicationTemplate.html",
            "templates/policy/EditEnvironmentTemplate.html",
            "templates/policy/EditPolicyTemplate.html",
            "templates/policy/EditSubjectTemplate.html",
            "templates/policy/ManageApplicationsTemplate.html",
            "templates/policy/ManageRulesTemplate.html",
            "templates/policy/ManagePoliciesTemplate.html",
            "templates/policy/OperatorRulesTemplate.html",
            "templates/policy/PoliciesTableGlobalActionsTemplate.html",
            "templates/policy/ResourcesListTemplate.html",
            "templates/policy/ReviewApplicationStepTemplate.html",
            "templates/policy/ReviewPolicyStepTemplate.html"
        ],
        deps = arguments;

    return function (server) {

        _.each(staticFiles, function (file, i) {
            server.respondWith(
                "GET",
                new RegExp(file.replace(/([\/\.\-])/g, "\\$1") + "$"),
                [
                    200,
                    { },
                    deps[i]
                ]
            );
        });

        server.respondWith(
            "GET",
            "/openam/json/applications?_queryFilter=true&_pageSize=10&_sortKeys=name&_pagedResultsOffset=0",
            [
                200,
                {
                    "Date": "Thu, 18 Sep 2014 19:01:09 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "5475",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"name\":\"aq qw qw\",\"resources\":[\"123\"],\"actions\":{},\"applicationType\":\"webservices\",\"subjects\":[],\"creationDate\":1409583320290,\"lastModifiedDate\":1411065478449,\"entitlementCombiner\":\"DenyOverride\",\"attributeNames\":[],\"realm\":\"/\",\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"description\":\"fdfdsfds\",\"conditions\":[]},{\"name\":\"aqqa aqaq1\",\"resources\":[\"cxzczxcxz\"],\"actions\":{\"TRANSFER\":true},\"applicationType\":\"banking\",\"subjects\":[\"AnyUser\"],\"creationDate\":1404293533315,\"lastModifiedDate\":1409582745402,\"entitlementCombiner\":\"DenyOverride\",\"attributeNames\":[],\"realm\":\"/\",\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"description\":null,\"conditions\":[\"AttributeLookup\"]},{\"name\":\"calendar\",\"resources\":[\"http://calendar.sun.com/my/*\",\"http://calendar.sun.com/*\",\"http://calendar.sun.com/*/calendars?calId=*\",\"http://calendar.sun.com/admin\"],\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"applicationType\":\"iPlanetAMWebAgentService\",\"subjects\":[\"User\",\"NOT\",\"Role\",\"OR\",\"Group\"],\"creationDate\":-1,\"lastModifiedDate\":-1,\"entitlementCombiner\":\"DenyOverride\",\"attributeNames\":[],\"realm\":\"/\",\"createdBy\":null,\"lastModifiedBy\":null,\"description\":null,\"conditions\":[\"Time\",\"NOT\",\"AND\",\"OR\"]},{\"name\":\"crestPolicyService\",\"resources\":[\"http://*\",\"https://*\"],\"actions\":{\"UPDATE\":true,\"QUERY\":true,\"PATCH\":true,\"CREATE\":true,\"DELETE\":true,\"READ\":true,\"ACTION\":true},\"applicationType\":\"crestPolicyService\",\"subjects\":[\"User\",\"NOT\",\"AND\",\"Attribute\",\"Role\",\"Group\",\"OR\"],\"creationDate\":1403298037999,\"lastModifiedDate\":1403298037999,\"entitlementCombiner\":\"DenyOverride\",\"attributeNames\":[],\"realm\":\"/\",\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"description\":null,\"conditions\":[\"Time\",\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"]},{\"name\":\"iPlanetAMWebAgentService\",\"resources\":[\"qwerrtty\",\"*\"],\"actions\":{\"GET\":false,\"DELETE\":false,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"applicationType\":\"iPlanetAMWebAgentService\",\"subjects\":[\"User\",\"NOT\",\"AND\",\"Attribute\",\"Role\",\"Group\",\"OR\"],\"creationDate\":1403298038042,\"lastModifiedDate\":1404317273958,\"entitlementCombiner\":\"DenyOverride\",\"attributeNames\":[],\"realm\":\"/\",\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"description\":null,\"conditions\":[\"Time\",\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"]},{\"name\":\"im\",\"resources\":[\"http://im.sun.com/register\",\"http://im.sun.com/im.jnlp\"],\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"applicationType\":\"iPlanetAMWebAgentService\",\"subjects\":[\"User\",\"Role\",\"Group\"],\"creationDate\":1403298037984,\"lastModifiedDate\":1406714256207,\"entitlementCombiner\":\"DenyOverride\",\"attributeNames\":[],\"realm\":\"/\",\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"description\":null,\"conditions\":[\"Time\",\"IP\",\"DNSName\"]},{\"name\":\"paycheck\",\"resources\":[\"http://paycheck.sun.com:8081/*/private\",\"http://paycheck.sun.com:8081/*\",\"http://paycheck.sun.com:8081/*/users/*\"],\"actions\":{\"OPTIONS\":false,\"HEAD\":true,\"PUT\":false},\"applicationType\":\"iPlanetAMWebAgentService\",\"subjects\":[\"User\",\"AND\",\"Role\",\"Group\",\"OR\"],\"creationDate\":1403298038032,\"lastModifiedDate\":1406808544428,\"entitlementCombiner\":\"DenyOverride\",\"attributeNames\":[],\"realm\":\"/\",\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"description\":null,\"conditions\":[\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"]},{\"name\":\"sunAMDelegationService\",\"resources\":[\"sms://*\"],\"actions\":{\"MODIFY\":true,\"READ\":true,\"DELEGATE\":true},\"applicationType\":\"sunAMDelegationService\",\"subjects\":[\"User\",\"Group\"],\"creationDate\":1403298038075,\"lastModifiedDate\":1403298038075,\"entitlementCombiner\":\"DenyOverride\",\"attributeNames\":[],\"realm\":\"/\",\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"description\":null,\"conditions\":[]},{\"name\":\"sunBank\",\"resources\":[\"*\"],\"actions\":{\"TRANSFER\":true},\"applicationType\":\"banking\",\"subjects\":[\"NOT\",\"com.sun.identity.admin.model.BankingViewSubject\"],\"creationDate\":1403298038052,\"lastModifiedDate\":1403298038052,\"entitlementCombiner\":\"DenyOverride\",\"attributeNames\":[],\"realm\":\"/\",\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"description\":null,\"conditions\":[\"NumericAttribute\",\"Time\",\"NOT\",\"IP\",\"AND\",\"OR\"]},{\"name\":\"sunIdentityServerDiscoveryService\",\"resources\":[\"http://*\",\"https://*\"],\"actions\":{\"LOOKUP\":true,\"UPDATE\":true},\"applicationType\":\"sunIdentityServerDiscoveryService\",\"subjects\":[\"User\",\"NOT\",\"AND\",\"Attribute\",\"Role\",\"Group\",\"OR\"],\"creationDate\":1403298038063,\"lastModifiedDate\":1403298038063,\"entitlementCombiner\":\"DenyOverride\",\"attributeNames\":[],\"realm\":\"/\",\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"description\":null,\"conditions\":[\"Time\",\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"]}],\"resultCount\":10,\"pagedResultsCookie\":null,\"remainingPagedResults\":3}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/applicationtypes/iPlanetAMWebAgentService",
            [
                200,
                {
                    "Date": "Thu, 18 Sep 2014 18:39:20 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "ETag": "&quot;1411065560848&quot;",
                    "Content-Length": "415",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"name\":\"iPlanetAMWebAgentService\",\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"PUT\":true,\"HEAD\":true},\"saveIndex\":\"org.forgerock.openam.entitlement.indextree.TreeSaveIndex\",\"searchIndex\":\"org.forgerock.openam.entitlement.indextree.TreeSearchIndex\",\"applicationClassName\":\"com.sun.identity.entitlement.Application\",\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\"}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/subjecttypes?_queryID=&_fields=title,logical,config",
            [
                200,
                {
                    "Date": "Thu, 18 Sep 2014 19:01:28 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "1208",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"title\":\"AND\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"subjects\":{\"type\":\"array\",\"items\":{\"type\":\"any\"}}}}},{\"title\":\"AnyUser\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{}}},{\"title\":\"Attribute\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"value\":{\"type\":\"string\"},\"id\":{\"type\":\"string\"}}}},{\"title\":\"Group\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}},{\"title\":\"NONE\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{}}},{\"title\":\"NOT\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"subject\":{\"type\":\"object\",\"properties\":{}}}}},{\"title\":\"OR\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"subjects\":{\"type\":\"array\",\"items\":{\"type\":\"any\"}}}}},{\"title\":\"Policy\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"className\":{\"type\":\"string\"},\"values\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}}}},{\"title\":\"Role\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}},{\"title\":\"User\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}}],\"resultCount\":10,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/conditiontypes?_queryID=&_fields=title,logical,config",
            [
                200,
                {
                    "Date": "Thu, 18 Sep 2014 19:01:28 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "1714",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"title\":\"AND\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"conditions\":{\"type\":\"array\",\"items\":{\"type\":\"any\"}}}}},{\"title\":\"AttributeLookup\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"key\":{\"type\":\"string\"},\"value\":{\"type\":\"string\"}}}},{\"title\":\"DNSName\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"domainNameMask\":{\"type\":\"string\"}}}},{\"title\":\"IP\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"startIp\":{\"type\":\"string\"},\"endIp\":{\"type\":\"string\"}}}},{\"title\":\"NOT\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"condition\":{\"type\":\"object\",\"properties\":{}}}}},{\"title\":\"NumericAttribute\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"attributeName\":{\"type\":\"string\"},\"operator\":{\"type\":\"string\",\"enum\":[\"LESS_THAN\",\"LESS_THAN_OR_EQUAL\",\"EQUAL\",\"GREATER_THAN_OR_EQUAL\",\"GREATER_THAN\"]},\"value\":{\"type\":\"number\"}}}},{\"title\":\"OR\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"conditions\":{\"type\":\"array\",\"items\":{\"type\":\"any\"}}}}},{\"title\":\"Policy\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"className\":{\"type\":\"string\"},\"properties\":{\"type\":\"object\"}}}},{\"title\":\"StringAttribute\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"attributeName\":{\"type\":\"string\"},\"value\":{\"type\":\"string\"},\"caseSensitive\":{\"type\":\"boolean\",\"required\":true}}}},{\"title\":\"Time\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"startTime\":{\"type\":\"string\"},\"endTime\":{\"type\":\"string\"},\"startDay\":{\"type\":\"string\"},\"endDay\":{\"type\":\"string\"},\"startDate\":{\"type\":\"string\"},\"endDate\":{\"type\":\"string\"},\"enforcementTimeZone\":{\"type\":\"string\"}}}}],\"resultCount\":10,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/decisioncombiners/?_queryId=&_fields=title",
            [
                200,
                {
                    "Date": "Mon, 11 Aug 2014 10:03:41 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "106",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"title\":\"DenyOverride\"}],\"resultCount\":1,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/applications/iPlanetAMWebAgentService",
            [
                200,
                {
                    "Date": "Mon, 11 Aug 2014 10:03:41 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "ETag": "&quot;1404317273958&quot;",
                    "Content-Length": "587",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"name\":\"iPlanetAMWebAgentService\",\"resources\":[\"qwerrtty\",\"*\"],\"actions\":{\"GET\":false,\"DELETE\":false,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"realm\":\"/\",\"description\":null,\"creationDate\":1403298038042,\"lastModifiedDate\":1404317273958,\"attributeNames\":[],\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"iPlanetAMWebAgentService\",\"conditions\":[\"Time\",\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"],\"subjects\":[\"User\",\"NOT\",\"AND\",\"Attribute\",\"Role\",\"Group\",\"OR\"],\"entitlementCombiner\":\"DenyOverride\"}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/policies/anotherxamplePolicy",
            [
                200,
                {
                    "Date": "Thu, 18 Sep 2014 19:01:28 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "ETag": "&quot;1406716378997&quot;",
                    "Content-Length": "525",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"name\":\"anotherxamplePolicy\",\"active\":true,\"description\":\"descr\",\"resources\":[\"http://www.example.com:80/*\"],\"applicationName\":\"sunIdentityServerLibertyPPService\",\"actionValues\":{\"QUERY_interactForValue\":false,\"MODIFY_interactForValue\":false},\"subject\":{\"type\":\"User\",\"subjectName\":\"Bob Bob\"},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-07-30T10:32:58Z\",\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-07-09T11:21:49Z\"}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/applications/sunIdentityServerLibertyPPService",
            [
                200,
                {
                    "Date": "Thu, 18 Sep 2014 19:01:28 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "ETag": "&quot;1403298038012&quot;",
                    "Content-Length": "739",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"name\":\"sunIdentityServerLibertyPPService\",\"resources\":[\"*\", \"-*-\", \"http://www.hello.com/-*-/world/*\"],\"actions\":{\"QUERY_interactForConsent\":false,\"QUERY_interactForValue\":false,\"MODIFY_interactForValue\":false,\"QUERY_deny\":false,\"MODIFY_deny\":false,\"MODIFY_interactForConsent\":false,\"MODIFY_allow\":true,\"QUERY_allow\":true},\"applicationType\":\"sunIdentityServerLibertyPPService\",\"subjects\":[\"User\",\"NOT\",\"AND\",\"Attribute\",\"Role\",\"Group\",\"OR\"],\"creationDate\":1403298038012,\"lastModifiedDate\":1403298038012,\"entitlementCombiner\":\"DenyOverride\",\"attributeNames\":[],\"realm\":\"/\",\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"description\":null,\"conditions\":[\"Time\",\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"]}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/policies?_queryFilter=applicationName%20eq%20%22sunIdentityServerLibertyPPService%22&_pageSize=10&_sortKeys=name&_pagedResultsOffset=0",
            [
                200,
                {
                    "Date": "Thu, 18 Sep 2014 19:01:24 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "606",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"name\":\"anotherxamplePolicy\",\"active\":true,\"description\":\"descr\",\"resources\":[\"http://www.example.com:80/*\"],\"applicationName\":\"sunIdentityServerLibertyPPService\",\"actionValues\":{\"QUERY_interactForValue\":false,\"MODIFY_interactForValue\":false},\"subject\":{\"type\":\"User\",\"subjectName\":\"Bob Bob\"},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-07-30T10:32:58Z\",\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-07-09T11:21:49Z\"}],\"resultCount\":1,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
            ]
        );
    };
});