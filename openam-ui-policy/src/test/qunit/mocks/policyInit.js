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
    "text!templates/policy/EditPolicyTemplate.html",
    "text!templates/policy/ListApplicationsTemplate.html",
    "text!templates/policy/ListPoliciesTemplate.html",
    "text!templates/policy/ManageApplicationsTemplate.html",
    "text!templates/policy/ManagePoliciesTemplate.html",
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
            "templates/policy/EditPolicyTemplate.html",
            "templates/policy/ListApplicationsTemplate.html",
            "templates/policy/ListPoliciesTemplate.html",
            "templates/policy/ManageApplicationsTemplate.html",
            "templates/policy/ManagePoliciesTemplate.html",
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
            "/openam/json/applications?_queryFilter=true",
            [
                200,
                {
                    "Date": "Fri, 20 Jun 2014 13:17:04 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Transfer-Encoding": "chunked",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"name\":\"iiii\",\"resources\":[\"ertrtre\",\"234234\"],\"actions\":{\"UPDATE\":false,\"QUERY\":false,\"PATCH\":false,\"CREATE\":false,\"DELETE\":true,\"READ\":true,\"ACTION\":true},\"description\":\"Description556\",\"applicationType\":\"crestPolicyService\",\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\",\"attributeNames\":[],\"creationDate\":1403109830182,\"lastModifiedDate\":1403217252906,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[],\"realm\":\"/\",\"subjects\":[],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"uuuu\",\"resources\":[],\"actions\":{},\"description\":null,\"applicationType\":\"webservices\",\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\",\"attributeNames\":[],\"creationDate\":1403109720035,\"lastModifiedDate\":1403109720035,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[],\"realm\":\"/\",\"subjects\":[],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"ddd\",\"resources\":[],\"actions\":{},\"description\":null,\"applicationType\":\"webservices\",\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\",\"attributeNames\":[],\"creationDate\":1403109763331,\"lastModifiedDate\":1403109763331,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[],\"realm\":\"/\",\"subjects\":[],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"sunBank1\",\"resources\":[\"*\"],\"actions\":{\"TRANSFER\":true},\"description\":null,\"applicationType\":\"banking\",\"resourceComparator\":\"com.sun.identity.entitlement.ExactMatchResourceName\",\"attributeNames\":[],\"creationDate\":1401882302290,\"lastModifiedDate\":1403105014856,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[\"not\",\"dateRange\",\"timezone\",\"lowerTransferLimit\",\"timeRange\",\"or\",\"upperTransferLimit\",\"anyTransferLimit\",\"ipRange\",\"daysOfWeek\",\"and\"],\"realm\":\"/\",\"subjects\":[\"com.sun.identity.admin.model.NotViewSubject\",\"com.sun.identity.admin.model.BankingViewSubject\",\"com.sun.identity.admin.model.VirtualViewSubject\"],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"sunIdentityServerLibertyPPService\",\"resources\":[\"*\"],\"actions\":{\"QUERY_interactForConsent\":false,\"QUERY_interactForValue\":false,\"MODIFY_interactForValue\":false,\"QUERY_deny\":false,\"MODIFY_deny\":false,\"MODIFY_interactForConsent\":false,\"MODIFY_allow\":true,\"QUERY_allow\":true},\"description\":null,\"applicationType\":\"sunIdentityServerLibertyPPService\",\"resourceComparator\":\"com.sun.identity.entitlement.PrefixResourceName\",\"attributeNames\":[],\"creationDate\":1401882302314,\"lastModifiedDate\":1401882302314,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[\"not\",\"dateRange\",\"timezone\",\"timeRange\",\"or\",\"dnsName\",\"ipRange\",\"daysOfWeek\",\"and\"],\"realm\":\"/\",\"subjects\":[\"com.sun.identity.admin.model.IdRepoUserViewSubject\",\"com.sun.identity.admin.model.NotViewSubject\",\"com.sun.identity.admin.model.IdRepoRoleViewSubject\",\"com.sun.identity.admin.model.AndViewSubject\",\"com.sun.identity.admin.model.VirtualViewSubject\",\"com.sun.identity.admin.model.OrViewSubject\",\"com.sun.identity.admin.model.IdRepoGroupViewSubject\",\"com.sun.identity.admin.model.AttributeViewSubject\"],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"www\",\"resources\":[],\"actions\":{},\"description\":null,\"applicationType\":\"webservices\",\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\",\"attributeNames\":[],\"creationDate\":1403109659885,\"lastModifiedDate\":1403109659885,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[],\"realm\":\"/\",\"subjects\":[],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"iPlanetAMWebAgentService\",\"resources\":[\"*\"],\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"description\":null,\"applicationType\":\"iPlanetAMWebAgentService\",\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\",\"attributeNames\":[],\"creationDate\":1401882302229,\"lastModifiedDate\":1401882302229,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[\"not\",\"dateRange\",\"timezone\",\"timeRange\",\"or\",\"dnsName\",\"ipRange\",\"daysOfWeek\",\"and\"],\"realm\":\"/\",\"subjects\":[\"com.sun.identity.admin.model.IdRepoUserViewSubject\",\"com.sun.identity.admin.model.NotViewSubject\",\"com.sun.identity.admin.model.IdRepoRoleViewSubject\",\"com.sun.identity.admin.model.AndViewSubject\",\"com.sun.identity.admin.model.VirtualViewSubject\",\"com.sun.identity.admin.model.OrViewSubject\",\"com.sun.identity.admin.model.IdRepoGroupViewSubject\",\"com.sun.identity.admin.model.AttributeViewSubject\"],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"sunBank\",\"resources\":[\"*\"],\"actions\":{\"fdsfds\":false,\"eewrewrew\":false,\"fdsfsdfsd\":false,\"TRANSFER\":true},\"description\":null,\"applicationType\":\"banking\",\"resourceComparator\":\"com.sun.identity.entitlement.ExactMatchResourceName\",\"attributeNames\":[],\"creationDate\":1401882302290,\"lastModifiedDate\":1403102608811,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[\"not\",\"dateRange\",\"lowerTransferLimit\",\"timezone\",\"timeRange\",\"or\",\"upperTransferLimit\",\"anyTransferLimit\",\"ipRange\",\"daysOfWeek\",\"and\"],\"realm\":\"/\",\"subjects\":[\"com.sun.identity.admin.model.NotViewSubject\",\"com.sun.identity.admin.model.BankingViewSubject\",\"com.sun.identity.admin.model.VirtualViewSubject\"],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"paycheck\",\"resources\":[\"http://paycheck.sun.com:8081/*/private\",\"http://paycheck.sun.com:8081/*\",\"http://paycheck.sun.com:8081/*/users/*\"],\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"description\":null,\"applicationType\":\"iPlanetAMWebAgentService\",\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\",\"attributeNames\":[],\"creationDate\":1401882302302,\"lastModifiedDate\":1401882302302,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[\"not\",\"or\",\"dnsName\",\"ipRange\",\"and\"],\"realm\":\"/\",\"subjects\":[\"com.sun.identity.admin.model.IdRepoUserViewSubject\",\"com.sun.identity.admin.model.IdRepoRoleViewSubject\",\"com.sun.identity.admin.model.AndViewSubject\",\"com.sun.identity.admin.model.VirtualViewSubject\",\"com.sun.identity.admin.model.OrViewSubject\",\"com.sun.identity.admin.model.IdRepoGroupViewSubject\"],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"sunAMDelegationService\",\"resources\":[\"sms://*\"],\"actions\":{\"MODIFY\":true,\"READ\":true,\"DELEGATE\":true},\"description\":null,\"applicationType\":\"sunAMDelegationService\",\"resourceComparator\":\"com.sun.identity.entitlement.RegExResourceName\",\"attributeNames\":[],\"creationDate\":1401882302269,\"lastModifiedDate\":1401882302269,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[],\"realm\":\"/\",\"subjects\":[\"com.sun.identity.admin.model.IdRepoUserViewSubject\",\"com.sun.identity.admin.model.IdRepoGroupViewSubject\"],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"im\",\"resources\":[\"http://im.sun.com/register\",\"http://im.sun.com/im.jnlp\"],\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"description\":null,\"applicationType\":\"iPlanetAMWebAgentService\",\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\",\"attributeNames\":[],\"creationDate\":1401882302280,\"lastModifiedDate\":1401882302280,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[\"dnsName\",\"ipRange\",\"daysOfWeek\"],\"realm\":\"/\",\"subjects\":[\"com.sun.identity.admin.model.IdRepoUserViewSubject\",\"com.sun.identity.admin.model.IdRepoRoleViewSubject\",\"com.sun.identity.admin.model.VirtualViewSubject\",\"com.sun.identity.admin.model.IdRepoGroupViewSubject\"],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"sunBankJJJ\",\"resources\":[\"*\",\"dsf\"],\"actions\":{\"TRANSFER\":true},\"description\":null,\"applicationType\":\"banking\",\"resourceComparator\":\"com.sun.identity.entitlement.ExactMatchResourceName\",\"attributeNames\":[],\"creationDate\":1401882302290,\"lastModifiedDate\":1403105562697,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[\"not\",\"dateRange\",\"timezone\",\"lowerTransferLimit\",\"timeRange\",\"or\",\"upperTransferLimit\",\"anyTransferLimit\",\"ipRange\",\"daysOfWeek\",\"and\"],\"realm\":\"/\",\"subjects\":[\"com.sun.identity.admin.model.NotViewSubject\",\"com.sun.identity.admin.model.BankingViewSubject\",\"com.sun.identity.admin.model.VirtualViewSubject\"],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"openProvisioning\",\"resources\":[\"/*\"],\"actions\":{\"UPDATE\":true,\"CREATE\":true,\"DELETE\":true,\"READ\":true},\"description\":null,\"applicationType\":\"openProvisioning\",\"resourceComparator\":\"com.sun.identity.entitlement.PrefixResourceName\",\"attributeNames\":[],\"creationDate\":1401882302349,\"lastModifiedDate\":1403105520635,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[],\"realm\":\"/\",\"subjects\":[],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"demomomomo\",\"resources\":[],\"actions\":{\"TRANSFER\":false},\"description\":\"123\",\"applicationType\":\"banking\",\"resourceComparator\":\"com.sun.identity.entitlement.ExactMatchResourceName\",\"attributeNames\":[],\"creationDate\":1403187480723,\"lastModifiedDate\":1403187516591,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[],\"realm\":\"/\",\"subjects\":[],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"crestPolicyService\",\"resources\":[\"http://*\",\"https://*\"],\"actions\":{\"UPDATE\":true,\"QUERY\":true,\"PATCH\":true,\"CREATE\":true,\"DELETE\":true,\"READ\":true,\"ACTION\":true},\"description\":null,\"applicationType\":\"crestPolicyService\",\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\",\"attributeNames\":[],\"creationDate\":1401882302251,\"lastModifiedDate\":1401882302251,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[\"not\",\"dateRange\",\"timezone\",\"timeRange\",\"or\",\"dnsName\",\"ipRange\",\"daysOfWeek\",\"and\"],\"realm\":\"/\",\"subjects\":[\"com.sun.identity.admin.model.IdRepoUserViewSubject\",\"com.sun.identity.admin.model.NotViewSubject\",\"com.sun.identity.admin.model.IdRepoRoleViewSubject\",\"com.sun.identity.admin.model.AndViewSubject\",\"com.sun.identity.admin.model.VirtualViewSubject\",\"com.sun.identity.admin.model.OrViewSubject\",\"com.sun.identity.admin.model.IdRepoGroupViewSubject\",\"com.sun.identity.admin.model.AttributeViewSubject\"],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"TEST_ACTIONS\",\"resources\":[\"123\"],\"actions\":{\"UPDATE\":false,\"QUERY\":true,\"PATCH\":false},\"description\":\"tetete\",\"applicationType\":\"crestPolicyService\",\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\",\"attributeNames\":[],\"creationDate\":1403169609283,\"lastModifiedDate\":1403169609283,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[],\"realm\":\"/\",\"subjects\":[],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"yyy\",\"resources\":[],\"actions\":{},\"description\":null,\"applicationType\":\"webservices\",\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\",\"attributeNames\":[],\"creationDate\":1403109622658,\"lastModifiedDate\":1403109622658,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[],\"realm\":\"/\",\"subjects\":[],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"sunIdentityServerDiscoveryService\",\"resources\":[\"http://*\",\"https://*\"],\"actions\":{\"LOOKUP\":true,\"UPDATE\":true},\"description\":null,\"applicationType\":\"sunIdentityServerDiscoveryService\",\"resourceComparator\":\"com.sun.identity.entitlement.PrefixResourceName\",\"attributeNames\":[],\"creationDate\":1401882302327,\"lastModifiedDate\":1401882302327,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[\"not\",\"dateRange\",\"timezone\",\"timeRange\",\"or\",\"dnsName\",\"ipRange\",\"daysOfWeek\",\"and\"],\"realm\":\"/\",\"subjects\":[\"com.sun.identity.admin.model.IdRepoUserViewSubject\",\"com.sun.identity.admin.model.NotViewSubject\",\"com.sun.identity.admin.model.IdRepoRoleViewSubject\",\"com.sun.identity.admin.model.AndViewSubject\",\"com.sun.identity.admin.model.VirtualViewSubject\",\"com.sun.identity.admin.model.OrViewSubject\",\"com.sun.identity.admin.model.IdRepoGroupViewSubject\",\"com.sun.identity.admin.model.AttributeViewSubject\"],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"crest\",\"resources\":[\"http://www.example.com:8080/*\",\"http://www.example.com:8080/*?*\"],\"actions\":{\"UPDATE\":true,\"QUERY\":true,\"PATCH\":true,\"CREATE\":true,\"DELETE\":true,\"READ\":true,\"ACTION\":true},\"description\":\"An example application for Common REST\",\"applicationType\":\"crestPolicyService\",\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\",\"attributeNames\":[],\"creationDate\":1398761708295,\"lastModifiedDate\":1403109091002,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[\"not\",\"dateRange\",\"timezone\",\"timeRange\",\"or\",\"dnsName\",\"ipRange\",\"daysOfWeek\",\"and\"],\"realm\":\"/\",\"subjects\":[\"com.sun.identity.admin.model.IdRepoUserViewSubject\",\"com.sun.identity.admin.model.NotViewSubject\",\"com.sun.identity.admin.model.IdRepoRoleViewSubject\",\"com.sun.identity.admin.model.AndViewSubject\",\"com.sun.identity.admin.model.VirtualViewSubject\",\"com.sun.identity.admin.model.OrViewSubject\",\"com.sun.identity.admin.model.IdRepoGroupViewSubject\",\"com.sun.identity.admin.model.AttributeViewSubject\"],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"qq\",\"resources\":[],\"actions\":{},\"description\":null,\"applicationType\":\"webservices\",\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\",\"attributeNames\":[],\"creationDate\":1403109125439,\"lastModifiedDate\":1403109125439,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[],\"realm\":\"/\",\"subjects\":[],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"fafafa\",\"resources\":[],\"actions\":{\"DELETE\":false,\"READ\":false},\"description\":null,\"applicationType\":\"crestPolicyService\",\"resourceComparator\":\"com.sun.identity.entitlement.RegExResourceName\",\"attributeNames\":[],\"creationDate\":1403223076843,\"lastModifiedDate\":1403223097495,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[],\"realm\":\"/\",\"subjects\":[],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null},{\"name\":\"calendar\",\"resources\":[\"http://calendar.sun.com/my/*\",\"http://calendar.sun.com/*\",\"http://calendar.sun.com/*/calendars?calId=*\",\"http://calendar.sun.com/admin\"],\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"description\":null,\"applicationType\":\"iPlanetAMWebAgentService\",\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\",\"attributeNames\":[],\"creationDate\":1401882302336,\"lastModifiedDate\":1401882302336,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[\"not\",\"dateRange\",\"timezone\",\"timeRange\",\"or\",\"daysOfWeek\",\"and\"],\"realm\":\"/\",\"subjects\":[\"com.sun.identity.admin.model.IdRepoUserViewSubject\",\"com.sun.identity.admin.model.NotViewSubject\",\"com.sun.identity.admin.model.IdRepoRoleViewSubject\",\"com.sun.identity.admin.model.VirtualViewSubject\",\"com.sun.identity.admin.model.OrViewSubject\",\"com.sun.identity.admin.model.IdRepoGroupViewSubject\"],\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"saveIndex\":null,\"searchIndex\":null}],\"resultCount\":22,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/applicationtypes?_queryFilter=true",
            [
                200,
                {
                    "Date": "Wed, 25 Jun 2014 09:34:18 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "3262",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"name\":\"webservices\",\"actions\":{},\"saveIndex\":\"com.sun.identity.entitlement.util.ResourceNameIndexGenerator\",\"searchIndex\":\"com.sun.identity.entitlement.util.ResourceNameSplitter\",\"applicationClassName\":\"com.sun.identity.entitlement.WebServiceApplication\",\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\"},{\"name\":\"openProvisioning\",\"actions\":{\"UPDATE\":true,\"CREATE\":true,\"DELETE\":true,\"READ\":true},\"saveIndex\":\"com.sun.identity.entitlement.util.ResourceNameIndexGenerator\",\"searchIndex\":\"com.sun.identity.entitlement.util.ResourceNameSplitter\",\"applicationClassName\":\"com.sun.identity.entitlement.Application\",\"resourceComparator\":\"com.sun.identity.entitlement.PrefixResourceName\"},{\"name\":\"sunIdentityServerLibertyPPService\",\"actions\":{\"QUERY_interactForValue\":false,\"QUERY_interactForConsent\":false,\"MODIFY_interactForValue\":false,\"QUERY_deny\":false,\"MODIFY_deny\":false,\"MODIFY_interactForConsent\":false,\"MODIFY_allow\":true,\"QUERY_allow\":true},\"saveIndex\":\"com.sun.identity.entitlement.util.ResourceNameIndexGenerator\",\"searchIndex\":\"com.sun.identity.entitlement.util.ResourceNameSplitter\",\"applicationClassName\":\"com.sun.identity.entitlement.Application\",\"resourceComparator\":\"com.sun.identity.entitlement.PrefixResourceName\"},{\"name\":\"crestPolicyService\",\"actions\":{\"UPDATE\":true,\"PATCH\":true,\"QUERY\":true,\"CREATE\":true,\"DELETE\":true,\"READ\":true,\"ACTION\":true},\"saveIndex\":\"org.forgerock.openam.entitlement.indextree.TreeSaveIndex\",\"searchIndex\":\"org.forgerock.openam.entitlement.indextree.TreeSearchIndex\",\"applicationClassName\":\"com.sun.identity.entitlement.Application\",\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\"},{\"name\":\"iPlanetAMWebAgentService\",\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"PUT\":true,\"HEAD\":true},\"saveIndex\":\"org.forgerock.openam.entitlement.indextree.TreeSaveIndex\",\"searchIndex\":\"org.forgerock.openam.entitlement.indextree.TreeSearchIndex\",\"applicationClassName\":\"com.sun.identity.entitlement.Application\",\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\"},{\"name\":\"banking\",\"actions\":{\"TRANSFER\":true},\"saveIndex\":\"com.sun.identity.entitlement.util.ResourceNameIndexGenerator\",\"searchIndex\":\"com.sun.identity.entitlement.util.ResourceNameSplitter\",\"applicationClassName\":\"com.sun.identity.entitlement.Application\",\"resourceComparator\":\"com.sun.identity.entitlement.ExactMatchResourceName\"},{\"name\":\"sunIdentityServerDiscoveryService\",\"actions\":{\"UPDATE\":true,\"LOOKUP\":true},\"saveIndex\":\"com.sun.identity.entitlement.util.ResourceNameIndexGenerator\",\"searchIndex\":\"com.sun.identity.entitlement.util.ResourceNameSplitter\",\"applicationClassName\":\"com.sun.identity.entitlement.Application\",\"resourceComparator\":\"com.sun.identity.entitlement.PrefixResourceName\"},{\"name\":\"sunAMDelegationService\",\"actions\":{\"MODIFY\":true,\"READ\":true,\"DELEGATE\":true},\"saveIndex\":\"com.sun.identity.entitlement.opensso.DelegationResourceNameIndexGenerator\",\"searchIndex\":\"com.sun.identity.entitlement.opensso.DelegationResourceNameSplitter\",\"applicationClassName\":\"com.sun.identity.entitlement.Application\",\"resourceComparator\":\"com.sun.identity.entitlement.RegExResourceName\"}],\"resultCount\":8,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/conditiontypes?_queryID=&_fields=title,logical",
            [
                200,
                {
                    "Date": "Tue, 01 Jul 2014 12:35:17 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "442",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"title\":\"AND\",\"logical\":true},{\"title\":\"AttributeLookup\",\"logical\":false},{\"title\":\"DNSName\",\"logical\":false},{\"title\":\"IP\",\"logical\":false},{\"title\":\"NOT\",\"logical\":true},{\"title\":\"NumericAttribute\",\"logical\":false},{\"title\":\"OR\",\"logical\":true},{\"title\":\"Policy\",\"logical\":false},{\"title\":\"StringAttribute\",\"logical\":false},{\"title\":\"Time\",\"logical\":false}],\"resultCount\":10,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/subjecttypes?_queryID=&_fields=title,logical",
            [
                200,
                {
                    "Date": "Tue, 01 Jul 2014 12:35:17 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "416",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"title\":\"AND\",\"logical\":true},{\"title\":\"AnyUser\",\"logical\":false},{\"title\":\"Attribute\",\"logical\":false},{\"title\":\"Group\",\"logical\":false},{\"title\":\"NONE\",\"logical\":false},{\"title\":\"NOT\",\"logical\":true},{\"title\":\"OR\",\"logical\":true},{\"title\":\"Policy\",\"logical\":false},{\"title\":\"Role\",\"logical\":false},{\"title\":\"User\",\"logical\":false}],\"resultCount\":10,\"pagedResultsCookie\":null,\"remainingPagedResults\":-1}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/applications/iPlanetAMWebAgentService",
            [
                200,
                {
                    "Date": "Wed, 02 Jul 2014 16:08:02 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "ETag": "&quot;1404317273958&quot;",
                    "Content-Length": "576",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"name\":\"iPlanetAMWebAgentService\",\"resources\":[\"qwerrtty\",\"*\"],\"actions\":{\"GET\":false,\"DELETE\":false,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"realm\":\"/\",\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"attributeNames\":[],\"description\":null,\"creationDate\":1403298038042,\"lastModifiedDate\":1404317273958,\"conditions\":[\"Time\",\"IP\",\"AND\",\"DNSName\"],\"applicationType\":\"iPlanetAMWebAgentService\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"subjects\":[\"User\",\"NOT\",\"AND\",\"Attribute\",\"Role\",\"Group\",\"OR\"],\"entitlementCombiner\":\"DenyOverride\"}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/decisioncombiners/?_queryId=&_fields=title",
            [
                200,
                {
                    "Date": "Wed, 02 Jul 2014 11:35:41 GMT",
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
            "/openam/json/policies/newPolicy",
            [
                200,
                {
                    "Date": "Thu, 03 Jul 2014 10:55:52 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "ETag": "&quot;1403599741705&quot;",
                    "Content-Length": "524",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"name\":\"newPolicy\",\"active\":true,\"description\":\"changed\",\"resources\":{\"included\":[\"http://im.sun.com/register\",\"http://im.sun.com/im.jnlp\"],\"excluded\":[]},\"applicationName\":\"im\",\"actionValues\":{\"POST\":false,\"GET\":true,\"HEAD\":true,\"PUT\":true},\"subject\":{\"type\":\"User\",\"subjectName\":\"Bob Bob\"},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amAdmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-06-24T08:49:01Z\",\"createdBy\":\"id=amAdmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-06-11T13:56:43Z\"}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/applications/im",
            [
                200,
                {
                    "Date": "Thu, 03 Jul 2014 10:55:52 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "ETag": "&quot;1402486647976&quot;",
                    "Content-Length": "918",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"name\":\"im\",\"resources\":[\"http://im.sun.com/register\",\"http://im.sun.com/im.jnlp\"],\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"attributeNames\":[],\"description\":null,\"realm\":\"/\",\"entitlementCombiner\":\"com.sun.identity.entitlement.DenyOverride\",\"searchIndex\":null,\"resourceComparator\":\"com.sun.identity.entitlement.URLResourceName\",\"creationDate\":1402486647976,\"lastModifiedDate\":1402486647976,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"conditions\":[\"dnsName\",\"ipRange\",\"daysOfWeek\"],\"applicationType\":\"iPlanetAMWebAgentService\",\"subjects\":[\"com.sun.identity.admin.model.IdRepoUserViewSubject\",\"com.sun.identity.admin.model.IdRepoRoleViewSubject\",\"com.sun.identity.admin.model.VirtualViewSubject\",\"com.sun.identity.admin.model.IdRepoGroupViewSubject\"],\"saveIndex\":null}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/policies?_queryFilter=applicationName%20eq%20%22im%22",
            [
                200,
                {
                    "Date": "Thu, 03 Jul 2014 10:53:32 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "2353",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"name\":\"demo\",\"active\":true,\"description\":\"demo\",\"resources\":{\"included\":[\"http://im.sun.com/im.jnlp\"],\"excluded\":[]},\"applicationName\":\"im\",\"actionValues\":{\"PUT\":true},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amAdmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-06-26T14:56:04Z\",\"createdBy\":\"id=amAdmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-06-24T12:41:27Z\"},{\"name\":\"newtest\",\"active\":true,\"description\":\"test policy\",\"resources\":{\"included\":[\"http://im.sun.com/im.jnlp\"],\"excluded\":[]},\"applicationName\":\"im\",\"actionValues\":{\"DELETE\":true,\"HEAD\":true},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amAdmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-07-02T13:37:18Z\",\"createdBy\":\"id=amAdmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-06-24T07:19:48Z\"},{\"name\":\"SomenewPolicy\",\"active\":true,\"description\":\"test changed\",\"resources\":{\"included\":[\"http://im.sun.com/register\",\"http://im.sun.com/im.jnlp\"],\"excluded\":[]},\"applicationName\":\"im\",\"actionValues\":{\"POST\":false,\"GET\":true},\"subject\":{\"type\":\"User\",\"subjectName\":\"Bob Bob\"},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amAdmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-06-24T07:31:13Z\",\"createdBy\":\"id=amAdmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-06-11T13:58:17Z\"},{\"name\":\"newPolicy\",\"active\":true,\"description\":\"changed\",\"resources\":{\"included\":[\"http://im.sun.com/register\",\"http://im.sun.com/im.jnlp\"],\"excluded\":[]},\"applicationName\":\"im\",\"actionValues\":{\"POST\":false,\"GET\":true,\"HEAD\":true,\"PUT\":true},\"subject\":{\"type\":\"User\",\"subjectName\":\"Bob Bob\"},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amAdmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-06-24T08:49:01Z\",\"createdBy\":\"id=amAdmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-06-11T13:56:43Z\"},{\"name\":\"bender\",\"active\":true,\"description\":\"bender\",\"resources\":{\"included\":[\"http://im.sun.com/register\"],\"excluded\":[]},\"applicationName\":\"im\",\"actionValues\":{\"POST\":true},\"resourceAttributes\":[],\"lastModifiedBy\":\"id=amAdmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModified\":\"2014-06-30T10:32:52Z\",\"createdBy\":\"id=amAdmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2014-06-24T08:49:52Z\"}],\"resultCount\":5,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
            ]
        );
    };
});