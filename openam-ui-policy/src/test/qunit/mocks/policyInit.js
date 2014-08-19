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
    "text!templates/policy/BaseTemplate.html",
    "text!templates/policy/EditApplicationTemplate.html",
    "text!templates/policy/EditEnvironmentTemplate.html",
    "text!templates/policy/EditPolicyTemplate.html",
    "text!templates/policy/EditSubjectTemplate.html",
    "text!templates/policy/ManageApplicationsTemplate.html",
    "text!templates/policy/ManageEnvironmentsTemplate.html",
    "text!templates/policy/ManagePoliciesTemplate.html",
    "text!templates/policy/ManageSubjectsTemplate.html",
    "text!templates/policy/OperatorRulesTemplate.html",
    "text!templates/policy/ResourcesListTemplate.html",
    "text!templates/policy/ReviewApplicationStepTemplate.html",
    "text!templates/policy/ReviewPolicyStepTemplate.html"
], function () {
    /* an unfortunate need to duplicate the file names here, but I haven't
     yet found a way to fool requirejs into doing dynamic dependencies */
    var staticFiles = [
            "templates/policy/ActionsTemplate.html",
            "templates/policy/AddNewResourceTemplate.html",
            "templates/policy/BaseTemplate.html",
            "templates/policy/EditApplicationTemplate.html",
            "templates/policy/EditEnvironmentTemplate.html",
            "templates/policy/EditPolicyTemplate.html",
            "templates/policy/EditSubjectTemplate.html",
            "templates/policy/ManageApplicationsTemplate.html",
            "templates/policy/ManageEnvironmentsTemplate.html",
            "templates/policy/ManagePoliciesTemplate.html",
            "templates/policy/ManageSubjectsTemplate.html",
            "templates/policy/OperatorRulesTemplate.html",
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
            "/openam/json/applications?_queryFilter=true&_pageSize=10&_pagedResultsOffset=0",
            [
                200,
                {
                    "Date": "Fri, 15 Aug 2014 13:32:30 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "5341",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"name\":\"sunIdentityServerLibertyPPService\",\"resources\":[\"*\"],\"actions\":{\"QUERY_interactForConsent\":false,\"QUERY_interactForValue\":false,\"MODIFY_interactForValue\":false,\"QUERY_deny\":false,\"MODIFY_deny\":false,\"MODIFY_interactForConsent\":false,\"MODIFY_allow\":true,\"QUERY_allow\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"User\",\"NOT\",\"AND\",\"Attribute\",\"Role\",\"Group\",\"OR\"],\"attributeNames\":[],\"creationDate\":1403298038012,\"lastModifiedDate\":1403298038012,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"sunIdentityServerLibertyPPService\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"Time\",\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"]},{\"name\":\"iPlanetAMWebAgentService\",\"resources\":[\"qwerrtty\",\"*\"],\"actions\":{\"GET\":false,\"DELETE\":false,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"User\",\"NOT\",\"AND\",\"Attribute\",\"Role\",\"Group\",\"OR\"],\"attributeNames\":[],\"creationDate\":1403298038042,\"lastModifiedDate\":1404317273958,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"iPlanetAMWebAgentService\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"Time\",\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"]},{\"name\":\"sunBank\",\"resources\":[\"*\"],\"actions\":{\"TRANSFER\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"NOT\",\"com.sun.identity.admin.model.BankingViewSubject\"],\"attributeNames\":[],\"creationDate\":1403298038052,\"lastModifiedDate\":1403298038052,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"banking\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"NumericAttribute\",\"Time\",\"NOT\",\"IP\",\"AND\",\"OR\"]},{\"name\":\"paycheck\",\"resources\":[\"http://paycheck.sun.com:8081/*/private\",\"http://paycheck.sun.com:8081/*\",\"http://paycheck.sun.com:8081/*/users/*\"],\"actions\":{\"OPTIONS\":false,\"HEAD\":true,\"PUT\":false},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"User\",\"AND\",\"Role\",\"Group\",\"OR\"],\"attributeNames\":[],\"creationDate\":1403298038032,\"lastModifiedDate\":1406808544428,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"iPlanetAMWebAgentService\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"]},{\"name\":\"im\",\"resources\":[\"http://im.sun.com/register\",\"http://im.sun.com/im.jnlp\"],\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"User\",\"Role\",\"Group\"],\"attributeNames\":[],\"creationDate\":1403298037984,\"lastModifiedDate\":1406714256207,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"iPlanetAMWebAgentService\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"Time\",\"IP\",\"DNSName\"]},{\"name\":\"aqqaaqaq\",\"resources\":[],\"actions\":{\"TRANSFER\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"AnyUser\"],\"attributeNames\":[],\"creationDate\":1404293533315,\"lastModifiedDate\":1404298029098,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"banking\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"AttributeLookup\"]},{\"name\":\"hahaha\",\"resources\":[],\"actions\":{},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[],\"attributeNames\":[],\"creationDate\":1404298013580,\"lastModifiedDate\":1406714271482,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"webservices\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"AttributeLookup\"]},{\"name\":\"yayayagfgfdg\",\"resources\":[\"gfd\",\"rgdf\"],\"actions\":{\"TRANSFER\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"AnyUser\"],\"attributeNames\":[],\"creationDate\":1403298250851,\"lastModifiedDate\":1406714294704,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"banking\",\"realm\":\"/\",\"description\":\"hjdsfds\",\"conditions\":[]},{\"name\":\"sunAMDelegationService\",\"resources\":[\"sms://*\"],\"actions\":{\"MODIFY\":true,\"READ\":true,\"DELEGATE\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"User\",\"Group\"],\"attributeNames\":[],\"creationDate\":1403298038075,\"lastModifiedDate\":1403298038075,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"sunAMDelegationService\",\"realm\":\"/\",\"description\":null,\"conditions\":[]},{\"name\":\"openProvisioning\",\"resources\":[\"/*\"],\"actions\":{\"UPDATE\":true,\"CREATE\":true,\"DELETE\":true,\"READ\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[],\"attributeNames\":[],\"creationDate\":1403298038021,\"lastModifiedDate\":1403298038021,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"openProvisioning\",\"realm\":\"/\",\"description\":null,\"conditions\":[]}],\"resultCount\":10,\"pagedResultsCookie\":null,\"remainingPagedResults\":5}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/applications?_queryFilter=true&_pageSize=10&_pagedResultsOffset=10",
            [
                200,
                {
                    "Date": "Fri, 15 Aug 2014 13:32:36 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "2914",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"name\":\"demodemo\",\"resources\":[\"tftyt\",\"ytrytr\"],\"actions\":{\"UPDATE\":true,\"CREATE\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"AND\",\"Attribute\",\"AnyUser\"],\"attributeNames\":[],\"creationDate\":1403613457461,\"lastModifiedDate\":1404211810777,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"openProvisioning\",\"realm\":\"/\",\"description\":\"fdsf\",\"conditions\":[\"NOT\"]},{\"name\":\"teststst\",\"resources\":[\"qwqwew\",\"dfgfdg\"],\"actions\":{\"TRANSFER\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"NOT\",\"Attribute\",\"NONE\",\"Group\",\"AnyUser\"],\"attributeNames\":[],\"creationDate\":1403599507259,\"lastModifiedDate\":1406707688551,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"banking\",\"realm\":\"/\",\"description\":\"eererre\",\"conditions\":[\"NOT\",\"IP\",\"OR\"]},{\"name\":\"crestPolicyService\",\"resources\":[\"http://*\",\"https://*\"],\"actions\":{\"UPDATE\":true,\"QUERY\":true,\"PATCH\":true,\"CREATE\":true,\"DELETE\":true,\"READ\":true,\"ACTION\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"User\",\"NOT\",\"AND\",\"Attribute\",\"Role\",\"Group\",\"OR\"],\"attributeNames\":[],\"creationDate\":1403298037999,\"lastModifiedDate\":1403298037999,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"crestPolicyService\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"Time\",\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"]},{\"name\":\"sunIdentityServerDiscoveryService\",\"resources\":[\"http://*\",\"https://*\"],\"actions\":{\"LOOKUP\":true,\"UPDATE\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"User\",\"NOT\",\"AND\",\"Attribute\",\"Role\",\"Group\",\"OR\"],\"attributeNames\":[],\"creationDate\":1403298038063,\"lastModifiedDate\":1403298038063,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"sunIdentityServerDiscoveryService\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"Time\",\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"]},{\"name\":\"calendar\",\"resources\":[\"http://calendar.sun.com/my/*\",\"http://calendar.sun.com/*\",\"http://calendar.sun.com/*/calendars?calId=*\",\"http://calendar.sun.com/admin\"],\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"User\",\"NOT\",\"Role\",\"Group\",\"OR\"],\"attributeNames\":[],\"creationDate\":1403298037961,\"lastModifiedDate\":1403298037961,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"iPlanetAMWebAgentService\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"Time\",\"NOT\",\"AND\",\"OR\"]}],\"resultCount\":5,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/applications?_queryFilter=true&_pageSize=20&_pagedResultsOffset=0",
            [
                200,
                {
                    "Date": "Fri, 15 Aug 2014 13:32:39 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "8175",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"name\":\"sunIdentityServerLibertyPPService\",\"resources\":[\"*\"],\"actions\":{\"QUERY_interactForConsent\":false,\"QUERY_interactForValue\":false,\"MODIFY_interactForValue\":false,\"QUERY_deny\":false,\"MODIFY_deny\":false,\"MODIFY_interactForConsent\":false,\"MODIFY_allow\":true,\"QUERY_allow\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"User\",\"NOT\",\"AND\",\"Attribute\",\"Role\",\"Group\",\"OR\"],\"attributeNames\":[],\"creationDate\":1403298038012,\"lastModifiedDate\":1403298038012,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"sunIdentityServerLibertyPPService\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"Time\",\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"]},{\"name\":\"iPlanetAMWebAgentService\",\"resources\":[\"qwerrtty\",\"*\"],\"actions\":{\"GET\":false,\"DELETE\":false,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"User\",\"NOT\",\"AND\",\"Attribute\",\"Role\",\"Group\",\"OR\"],\"attributeNames\":[],\"creationDate\":1403298038042,\"lastModifiedDate\":1404317273958,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"iPlanetAMWebAgentService\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"Time\",\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"]},{\"name\":\"paycheck\",\"resources\":[\"http://paycheck.sun.com:8081/*/private\",\"http://paycheck.sun.com:8081/*\",\"http://paycheck.sun.com:8081/*/users/*\"],\"actions\":{\"OPTIONS\":false,\"HEAD\":true,\"PUT\":false},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"User\",\"AND\",\"Role\",\"Group\",\"OR\"],\"attributeNames\":[],\"creationDate\":1403298038032,\"lastModifiedDate\":1406808544428,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"iPlanetAMWebAgentService\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"]},{\"name\":\"sunBank\",\"resources\":[\"*\"],\"actions\":{\"TRANSFER\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"NOT\",\"com.sun.identity.admin.model.BankingViewSubject\"],\"attributeNames\":[],\"creationDate\":1403298038052,\"lastModifiedDate\":1403298038052,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"banking\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"NumericAttribute\",\"Time\",\"NOT\",\"IP\",\"AND\",\"OR\"]},{\"name\":\"im\",\"resources\":[\"http://im.sun.com/register\",\"http://im.sun.com/im.jnlp\"],\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"User\",\"Role\",\"Group\"],\"attributeNames\":[],\"creationDate\":1403298037984,\"lastModifiedDate\":1406714256207,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"iPlanetAMWebAgentService\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"Time\",\"IP\",\"DNSName\"]},{\"name\":\"yayayagfgfdg\",\"resources\":[\"gfd\",\"rgdf\"],\"actions\":{\"TRANSFER\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"AnyUser\"],\"attributeNames\":[],\"creationDate\":1403298250851,\"lastModifiedDate\":1406714294704,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"banking\",\"realm\":\"/\",\"description\":\"hjdsfds\",\"conditions\":[]},{\"name\":\"aqqaaqaq\",\"resources\":[],\"actions\":{\"TRANSFER\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"AnyUser\"],\"attributeNames\":[],\"creationDate\":1404293533315,\"lastModifiedDate\":1404298029098,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"banking\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"AttributeLookup\"]},{\"name\":\"hahaha\",\"resources\":[],\"actions\":{},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[],\"attributeNames\":[],\"creationDate\":1404298013580,\"lastModifiedDate\":1406714271482,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"webservices\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"AttributeLookup\"]},{\"name\":\"sunAMDelegationService\",\"resources\":[\"sms://*\"],\"actions\":{\"MODIFY\":true,\"READ\":true,\"DELEGATE\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"User\",\"Group\"],\"attributeNames\":[],\"creationDate\":1403298038075,\"lastModifiedDate\":1403298038075,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"sunAMDelegationService\",\"realm\":\"/\",\"description\":null,\"conditions\":[]},{\"name\":\"openProvisioning\",\"resources\":[\"/*\"],\"actions\":{\"UPDATE\":true,\"CREATE\":true,\"DELETE\":true,\"READ\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[],\"attributeNames\":[],\"creationDate\":1403298038021,\"lastModifiedDate\":1403298038021,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"openProvisioning\",\"realm\":\"/\",\"description\":null,\"conditions\":[]},{\"name\":\"demodemo\",\"resources\":[\"tftyt\",\"ytrytr\"],\"actions\":{\"UPDATE\":true,\"CREATE\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"AND\",\"Attribute\",\"AnyUser\"],\"attributeNames\":[],\"creationDate\":1403613457461,\"lastModifiedDate\":1404211810777,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"openProvisioning\",\"realm\":\"/\",\"description\":\"fdsf\",\"conditions\":[\"NOT\"]},{\"name\":\"teststst\",\"resources\":[\"qwqwew\",\"dfgfdg\"],\"actions\":{\"TRANSFER\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"NOT\",\"Attribute\",\"NONE\",\"Group\",\"AnyUser\"],\"attributeNames\":[],\"creationDate\":1403599507259,\"lastModifiedDate\":1406707688551,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"banking\",\"realm\":\"/\",\"description\":\"eererre\",\"conditions\":[\"NOT\",\"IP\",\"OR\"]},{\"name\":\"crestPolicyService\",\"resources\":[\"http://*\",\"https://*\"],\"actions\":{\"UPDATE\":true,\"QUERY\":true,\"PATCH\":true,\"CREATE\":true,\"DELETE\":true,\"READ\":true,\"ACTION\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"User\",\"NOT\",\"AND\",\"Attribute\",\"Role\",\"Group\",\"OR\"],\"attributeNames\":[],\"creationDate\":1403298037999,\"lastModifiedDate\":1403298037999,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"crestPolicyService\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"Time\",\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"]},{\"name\":\"sunIdentityServerDiscoveryService\",\"resources\":[\"http://*\",\"https://*\"],\"actions\":{\"LOOKUP\":true,\"UPDATE\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"User\",\"NOT\",\"AND\",\"Attribute\",\"Role\",\"Group\",\"OR\"],\"attributeNames\":[],\"creationDate\":1403298038063,\"lastModifiedDate\":1403298038063,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"sunIdentityServerDiscoveryService\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"Time\",\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"]},{\"name\":\"calendar\",\"resources\":[\"http://calendar.sun.com/my/*\",\"http://calendar.sun.com/*\",\"http://calendar.sun.com/*/calendars?calId=*\",\"http://calendar.sun.com/admin\"],\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"entitlementCombiner\":\"DenyOverride\",\"subjects\":[\"User\",\"NOT\",\"Role\",\"Group\",\"OR\"],\"attributeNames\":[],\"creationDate\":1403298037961,\"lastModifiedDate\":1403298037961,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"iPlanetAMWebAgentService\",\"realm\":\"/\",\"description\":null,\"conditions\":[\"Time\",\"NOT\",\"AND\",\"OR\"]}],\"resultCount\":15,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/applicationtypes?_queryFilter=true",
            [
                200,
                {
                    "Date": "Mon, 11 Aug 2014 10:03:41 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "3262",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"name\":\"webservices\",\"actions\":{},\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\",\"saveIndex\":\"com.sun.identity.entitlement.util.ResourceNameIndexGenerator\",\"searchIndex\":\"com.sun.identity.entitlement.util.ResourceNameSplitter\",\"applicationClassName\":\"com.sun.identity.entitlement.WebServiceApplication\"},{\"name\":\"openProvisioning\",\"actions\":{\"UPDATE\":true,\"CREATE\":true,\"DELETE\":true,\"READ\":true},\"resourceComparator\":\"com.sun.identity.entitlement.PrefixResourceName\",\"saveIndex\":\"com.sun.identity.entitlement.util.ResourceNameIndexGenerator\",\"searchIndex\":\"com.sun.identity.entitlement.util.ResourceNameSplitter\",\"applicationClassName\":\"com.sun.identity.entitlement.Application\"},{\"name\":\"crestPolicyService\",\"actions\":{\"UPDATE\":true,\"PATCH\":true,\"QUERY\":true,\"CREATE\":true,\"DELETE\":true,\"READ\":true,\"ACTION\":true},\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\",\"saveIndex\":\"org.forgerock.openam.entitlement.indextree.TreeSaveIndex\",\"searchIndex\":\"org.forgerock.openam.entitlement.indextree.TreeSearchIndex\",\"applicationClassName\":\"com.sun.identity.entitlement.Application\"},{\"name\":\"sunIdentityServerLibertyPPService\",\"actions\":{\"QUERY_interactForValue\":false,\"QUERY_interactForConsent\":false,\"MODIFY_interactForValue\":false,\"QUERY_deny\":false,\"MODIFY_deny\":false,\"MODIFY_interactForConsent\":false,\"MODIFY_allow\":true,\"QUERY_allow\":true},\"resourceComparator\":\"com.sun.identity.entitlement.PrefixResourceName\",\"saveIndex\":\"com.sun.identity.entitlement.util.ResourceNameIndexGenerator\",\"searchIndex\":\"com.sun.identity.entitlement.util.ResourceNameSplitter\",\"applicationClassName\":\"com.sun.identity.entitlement.Application\"},{\"name\":\"banking\",\"actions\":{\"TRANSFER\":true},\"resourceComparator\":\"com.sun.identity.entitlement.ExactMatchResourceName\",\"saveIndex\":\"com.sun.identity.entitlement.util.ResourceNameIndexGenerator\",\"searchIndex\":\"com.sun.identity.entitlement.util.ResourceNameSplitter\",\"applicationClassName\":\"com.sun.identity.entitlement.Application\"},{\"name\":\"iPlanetAMWebAgentService\",\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"PUT\":true,\"HEAD\":true},\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\",\"saveIndex\":\"org.forgerock.openam.entitlement.indextree.TreeSaveIndex\",\"searchIndex\":\"org.forgerock.openam.entitlement.indextree.TreeSearchIndex\",\"applicationClassName\":\"com.sun.identity.entitlement.Application\"},{\"name\":\"sunIdentityServerDiscoveryService\",\"actions\":{\"UPDATE\":true,\"LOOKUP\":true},\"resourceComparator\":\"com.sun.identity.entitlement.PrefixResourceName\",\"saveIndex\":\"com.sun.identity.entitlement.util.ResourceNameIndexGenerator\",\"searchIndex\":\"com.sun.identity.entitlement.util.ResourceNameSplitter\",\"applicationClassName\":\"com.sun.identity.entitlement.Application\"},{\"name\":\"sunAMDelegationService\",\"actions\":{\"MODIFY\":true,\"READ\":true,\"DELEGATE\":true},\"resourceComparator\":\"com.sun.identity.entitlement.RegExResourceName\",\"saveIndex\":\"com.sun.identity.entitlement.opensso.DelegationResourceNameIndexGenerator\",\"searchIndex\":\"com.sun.identity.entitlement.opensso.DelegationResourceNameSplitter\",\"applicationClassName\":\"com.sun.identity.entitlement.Application\"}],\"resultCount\":8,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/conditiontypes?_queryID=&_fields=title,logical,config",
            [
                200,
                {
                    "Date": "Mon, 11 Aug 2014 10:03:41 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "1715",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"title\":\"AND\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"conditions\":{\"type\":\"array\",\"items\":{\"type\":\"any\"}}}}},{\"title\":\"AttributeLookup\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"key\":{\"type\":\"string\"},\"value\":{\"type\":\"string\"}}}},{\"title\":\"DNSName\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"domainNameMask\":{\"type\":\"string\"}}}},{\"title\":\"IP\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"startIp\":{\"type\":\"string\"},\"endIp\":{\"type\":\"string\"}}}},{\"title\":\"NOT\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"condition\":{\"type\":\"object\",\"properties\":{}}}}},{\"title\":\"NumericAttribute\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"attributeName\":{\"type\":\"string\"},\"operator\":{\"type\":\"string\",\"enum\":[\"LESS_THAN\",\"LESS_THAN_OR_EQUAL\",\"EQUAL\",\"GREATER_THAN_OR_EQUAL\",\"GREATER_THAN\"]},\"value\":{\"type\":\"number\"}}}},{\"title\":\"OR\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"conditions\":{\"type\":\"array\",\"items\":{\"type\":\"any\"}}}}},{\"title\":\"Policy\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"className\":{\"type\":\"string\"},\"properties\":{\"type\":\"object\"}}}},{\"title\":\"StringAttribute\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"attributeName\":{\"type\":\"string\"},\"value\":{\"type\":\"string\"},\"caseSensitive\":{\"type\":\"boolean\",\"required\":true}}}},{\"title\":\"Time\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"startTime\":{\"type\":\"string\"},\"endTime\":{\"type\":\"string\"},\"startDay\":{\"type\":\"string\"},\"endDay\":{\"type\":\"string\"},\"startDate\":{\"type\":\"string\"},\"endDate\":{\"type\":\"string\"},\"enforcementTimeZone\":{\"type\":\"string\"}}}}],\"resultCount\":10,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/subjecttypes?_queryID=&_fields=title,logical,config",
            [
                200,
                {
                    "Date": "Mon, 11 Aug 2014 10:03:41 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "1209",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"title\":\"AND\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"subjects\":{\"type\":\"array\",\"items\":{\"type\":\"any\"}}}}},{\"title\":\"AnyUser\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{}}},{\"title\":\"Attribute\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"value\":{\"type\":\"string\"},\"id\":{\"type\":\"string\"}}}},{\"title\":\"Group\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}},{\"title\":\"NONE\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{}}},{\"title\":\"NOT\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"subject\":{\"type\":\"object\",\"properties\":{}}}}},{\"title\":\"OR\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"subjects\":{\"type\":\"array\",\"items\":{\"type\":\"any\"}}}}},{\"title\":\"Policy\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"className\":{\"type\":\"string\"},\"values\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}}}},{\"title\":\"Role\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}},{\"title\":\"User\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}}],\"resultCount\":10,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1}"
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
            "/openam/json/policies/qwwqqw",
            [
                200,
                {
                    "Date": "Mon, 11 Aug 2014 10:05:39 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "ETag": "&quot;1407503016553&quot;",
                    "Content-Length": "442",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"name\":\"qwwqqw\",\"active\":true,\"description\":\"rewr\",\"resources\":{\"included\":[\"rerew\"],\"excluded\":[]},\"applicationName\":\"sunIdentityServerLibertyPPService\",\"actionValues\":{\"QUERY_deny\":false,\"MODIFY_deny\":false},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-08-08T13:03:36Z\",\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-07-30T10:09:45Z\"}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/applications/sunIdentityServerLibertyPPService",
            [
                200,
                {
                    "Date": "Mon, 11 Aug 2014 10:05:08 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "ETag": "&quot;1403298038012&quot;",
                    "Content-Length": "739",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"name\":\"sunIdentityServerLibertyPPService\",\"resources\":[\"*\"],\"actions\":{\"QUERY_interactForConsent\":false,\"QUERY_interactForValue\":false,\"MODIFY_interactForValue\":false,\"QUERY_deny\":false,\"MODIFY_deny\":false,\"MODIFY_interactForConsent\":false,\"MODIFY_allow\":true,\"QUERY_allow\":true},\"realm\":\"/\",\"description\":null,\"creationDate\":1403298038012,\"lastModifiedDate\":1403298038012,\"attributeNames\":[],\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"sunIdentityServerLibertyPPService\",\"conditions\":[\"Time\",\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"],\"subjects\":[\"User\",\"NOT\",\"AND\",\"Attribute\",\"Role\",\"Group\",\"OR\"],\"entitlementCombiner\":\"DenyOverride\"}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/policies?_queryFilter=applicationName%20eq%20%22sunIdentityServerLibertyPPService%22&_pageSize=10&_pagedResultsOffset=0",
            [
                200,
                {
                    "Date": "Sat, 16 Aug 2014 18:26:44 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "1566",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"name\":\"qwwqqw\",\"active\":true,\"description\":\"rewr\",\"resources\":{\"included\":[\"rerew\"],\"excluded\":[]},\"applicationName\":\"sunIdentityServerLibertyPPService\",\"actionValues\":{\"QUERY_deny\":false,\"MODIFY_deny\":false},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-08-08T13:03:36Z\",\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-07-30T10:09:45Z\"},{\"name\":\"anotherxamplePolicy\",\"active\":true,\"description\":\"descr\",\"resources\":{\"included\":[\"http://www.example.com:80/*\"],\"excluded\":[]},\"applicationName\":\"sunIdentityServerLibertyPPService\",\"actionValues\":{\"QUERY_interactForValue\":false,\"MODIFY_interactForValue\":false},\"subject\":{\"type\":\"User\",\"subjectName\":\"Bob Bob\"},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-07-30T10:32:58Z\",\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-07-09T11:21:49Z\"},{\"name\":\"anotherxamplePolicy111\",\"active\":true,\"description\":\"4\",\"resources\":{\"included\":[\"http://www.example.com:80/*\"],\"excluded\":[]},\"applicationName\":\"sunIdentityServerLibertyPPService\",\"actionValues\":{},\"subject\":{\"type\":\"User\",\"subjectName\":\"Bob Bob\"},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-07-29T15:21:19Z\",\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-07-29T15:21:19Z\"}],\"resultCount\":3,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/policies?_queryFilter=applicationName%20eq%20%22sunIdentityServerLibertyPPService%22&_pageSize=20&_pagedResultsOffset=0",
            [
                200,
                {
                    "Date": "Sat, 16 Aug 2014 18:26:52 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "1566",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"name\":\"qwwqqw\",\"active\":true,\"description\":\"rewr\",\"resources\":{\"included\":[\"rerew\"],\"excluded\":[]},\"applicationName\":\"sunIdentityServerLibertyPPService\",\"actionValues\":{\"QUERY_deny\":false,\"MODIFY_deny\":false},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-08-08T13:03:36Z\",\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-07-30T10:09:45Z\"},{\"name\":\"anotherxamplePolicy\",\"active\":true,\"description\":\"descr\",\"resources\":{\"included\":[\"http://www.example.com:80/*\"],\"excluded\":[]},\"applicationName\":\"sunIdentityServerLibertyPPService\",\"actionValues\":{\"QUERY_interactForValue\":false,\"MODIFY_interactForValue\":false},\"subject\":{\"type\":\"User\",\"subjectName\":\"Bob Bob\"},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-07-30T10:32:58Z\",\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-07-09T11:21:49Z\"},{\"name\":\"anotherxamplePolicy111\",\"active\":true,\"description\":\"4\",\"resources\":{\"included\":[\"http://www.example.com:80/*\"],\"excluded\":[]},\"applicationName\":\"sunIdentityServerLibertyPPService\",\"actionValues\":{},\"subject\":{\"type\":\"User\",\"subjectName\":\"Bob Bob\"},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-07-29T15:21:19Z\",\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-07-29T15:21:19Z\"}],\"resultCount\":3,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/policies?_queryFilter=applicationName%20eq%20%22sunIdentityServerLibertyPPService%22&_pageSize=30&_pagedResultsOffset=0",
            [
                200,
                {
                    "Date": "Sat, 16 Aug 2014 18:26:55 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "1566",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"name\":\"qwwqqw\",\"active\":true,\"description\":\"rewr\",\"resources\":{\"included\":[\"rerew\"],\"excluded\":[]},\"applicationName\":\"sunIdentityServerLibertyPPService\",\"actionValues\":{\"QUERY_deny\":false,\"MODIFY_deny\":false},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-08-08T13:03:36Z\",\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-07-30T10:09:45Z\"},{\"name\":\"anotherxamplePolicy\",\"active\":true,\"description\":\"descr\",\"resources\":{\"included\":[\"http://www.example.com:80/*\"],\"excluded\":[]},\"applicationName\":\"sunIdentityServerLibertyPPService\",\"actionValues\":{\"QUERY_interactForValue\":false,\"MODIFY_interactForValue\":false},\"subject\":{\"type\":\"User\",\"subjectName\":\"Bob Bob\"},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-07-30T10:32:58Z\",\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-07-09T11:21:49Z\"},{\"name\":\"anotherxamplePolicy111\",\"active\":true,\"description\":\"4\",\"resources\":{\"included\":[\"http://www.example.com:80/*\"],\"excluded\":[]},\"applicationName\":\"sunIdentityServerLibertyPPService\",\"actionValues\":{},\"subject\":{\"type\":\"User\",\"subjectName\":\"Bob Bob\"},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-07-29T15:21:19Z\",\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-07-29T15:21:19Z\"}],\"resultCount\":3,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
            ]
        );
    };
});