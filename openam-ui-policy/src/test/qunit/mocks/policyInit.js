/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 ForgeRock AS. All Rights Reserved
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
    "text!templates/policy/BaseTemplate.html",

    "text!templates/policy/applications/EditApplicationTemplate.html",
    "text!templates/policy/applications/ManageAppsGridActionsTemplate.html",
    "text!templates/policy/applications/ManageAppsGridCellActionsTemplate.html",
    "text!templates/policy/applications/ManageAppsGridTemplate.html",
    "text!templates/policy/applications/ManageAppsTemplate.html",
    "text!templates/policy/applications/ReviewApplicationStepTemplate.html",

    "text!templates/policy/policies/ActionsTemplate.html",
    "text!templates/policy/common/HelpLinkTemplate.html",

    "text!templates/policy/login/LoginDialog.html",

    "text!templates/policy/policies/attributes/ResponseAttrsStatic.html",
    "text!templates/policy/policies/attributes/ResponseAttrsUser.html",

    "text!templates/policy/policies/conditions/ConditionAttrArray.html",
    "text!templates/policy/policies/conditions/ConditionAttrBoolean.html",
    "text!templates/policy/policies/conditions/ConditionAttrDate.html",
    "text!templates/policy/policies/conditions/ConditionAttrDay.html",
    "text!templates/policy/policies/conditions/ConditionAttrEnum.html",
    "text!templates/policy/policies/conditions/ConditionAttrObject.html",
    "text!templates/policy/policies/conditions/ConditionAttrString.html",
    "text!templates/policy/policies/conditions/ConditionAttrTime.html",
    "text!templates/policy/policies/conditions/ConditionAttrTimeZone.html",
    "text!templates/policy/policies/conditions/EditEnvironmentTemplate.html",
    "text!templates/policy/policies/conditions/EditSubjectTemplate.html",
    "text!templates/policy/policies/conditions/LegacyListItem.html",
    "text!templates/policy/policies/conditions/ListItem.html",
    "text!templates/policy/policies/conditions/ManageRulesTemplate.html",
    "text!templates/policy/policies/conditions/OperatorRulesTemplate.html",

    "text!templates/policy/policies/EditPolicyTemplate.html",
    "text!templates/policy/policies/ManagePoliciesGridActionsTemplate.html",
    "text!templates/policy/policies/ManagePoliciesGridTemplate.html",
    "text!templates/policy/policies/ManagePoliciesTemplate.html",
    "text!templates/policy/policies/ReviewPolicyStepTemplate.html",

    "text!templates/policy/referrals/EditReferralTemplate.html",
    "text!templates/policy/referrals/ManageRefsGridActionsTemplate.html",
    "text!templates/policy/referrals/ManageRefsGridTemplate.html",
    "text!templates/policy/referrals/ReviewReferralStepTemplate.html",
    "text!templates/policy/referrals/SelectRealmsTemplate.html",

    "text!configuration.json"
], function () {
    /* an unfortunate need to duplicate the file names here, but I haven't
     yet found a way to fool requirejs into doing dynamic dependencies */
    var staticFiles = [
            "templates/policy/BaseTemplate.html",

            "templates/policy/applications/EditApplicationTemplate.html",
            "templates/policy/applications/ManageAppsGridActionsTemplate.html",
            "templates/policy/applications/ManageAppsGridCellActionsTemplate.html",
            "templates/policy/applications/ManageAppsGridTemplate.html",
            "templates/policy/applications/ManageAppsTemplate.html",
            "templates/policy/applications/ReviewApplicationStepTemplate.html",

            "templates/policy/policies/ActionsTemplate.html",
            "templates/policy/common/HelpLinkTemplate.html",

            "templates/policy/login/LoginDialog.html",

            "templates/policy/policies/attributes/ResponseAttrsStatic.html",
            "templates/policy/policies/attributes/ResponseAttrsUser.html",

            "templates/policy/policies/conditions/ConditionAttrArray.html",
            "templates/policy/policies/conditions/ConditionAttrBoolean.html",
            "templates/policy/policies/conditions/ConditionAttrDate.html",
            "templates/policy/policies/conditions/ConditionAttrDay.html",
            "templates/policy/policies/conditions/ConditionAttrEnum.html",
            "templates/policy/policies/conditions/ConditionAttrObject.html",
            "templates/policy/policies/conditions/ConditionAttrString.html",
            "templates/policy/policies/conditions/ConditionAttrTime.html",
            "templates/policy/policies/conditions/ConditionAttrTimeZone.html",
            "templates/policy/policies/conditions/EditEnvironmentTemplate.html",
            "templates/policy/policies/conditions/EditSubjectTemplate.html",
            "templates/policy/policies/conditions/LegacyListItem.html",
            "templates/policy/policies/conditions/ListItem.html",
            "templates/policy/policies/conditions/ManageRulesTemplate.html",
            "templates/policy/policies/conditions/OperatorRulesTemplate.html",

            "templates/policy/policies/EditPolicyTemplate.html",
            "templates/policy/policies/ManagePoliciesGridActionsTemplate.html",
            "templates/policy/policies/ManagePoliciesGridTemplate.html",
            "templates/policy/policies/ManagePoliciesTemplate.html",
            "templates/policy/policies/ReviewPolicyStepTemplate.html",

            "templates/policy/referrals/EditReferralTemplate.html",
            "templates/policy/referrals/ManageRefsGridActionsTemplate.html",
            "templates/policy/referrals/ManageRefsGridTemplate.html",
            "templates/policy/referrals/ReviewReferralStepTemplate.html",
            "templates/policy/referrals/SelectRealmsTemplate.html",

            "configuration.json"
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
            "/openam/json/serverinfo/*",
            [
                200,
                {
                    "Date": "Mon, 10 Nov 2014 10:13:34 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "ETag": "&quot;-1510027421&quot;",
                    "Content-Length": "350",
                    "Content-API-Version": "protocol=1.0,resource=1.1",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"domains\":[\".esergueeva.com\"],\"protectedUserAttributes\":[],\"cookieName\":\"iPlanetDirectoryPro\",\"forgotPassword\":\"false\",\"selfRegistration\":\"false\",\"lang\":\"en\",\"successfulUserRegistrationDestination\":\"default\",\"socialImplementations\":[],\"referralsEnabled\":\"false\",\"zeroPageLogin\":{\"enabled\":false,\"refererWhitelist\":[\"\"],\"allowedWithoutReferer\":true}}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/applications?_queryFilter=name+eq+%22%5E(%3F!calendar%24).*%22+AND+name+eq+%22%5E(%3F!crestPolicyService%24).*%22+AND+name+eq+%22%5E(%3F!im%24).*%22+AND+name+eq+%22%5E(%3F!openProvisioning%24).*%22+AND+name+eq+%22%5E(%3F!paycheck%24).*%22+AND+name+eq+%22%5E(%3F!sunAMDelegationService%24).*%22+AND+name+eq+%22%5E(%3F!sunBank%24).*%22+AND+name+eq+%22%5E(%3F!sunIdentityServerDiscoveryService%24).*%22+AND+name+eq+%22%5E(%3F!sunIdentityServerLibertyPPService%24).*%22&_pageSize=10&_sortKeys=name&_pagedResultsOffset=0",
            [
                200,
                {
                    "Date": "Mon, 10 Nov 2014 10:13:35 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "3305",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"name\":\"12344\",\"resources\":[\"*\"],\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"realm\":\"/\",\"subjects\":[\"JwtClaim\",\"AuthenticatedUsers\",\"Identity\",\"NOT\",\"Policy\",\"AND\",\"NONE\",\"OR\"],\"entitlementCombiner\":\"DenyOverride\",\"attributeNames\":[],\"description\":null,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":1414658465582,\"lastModifiedDate\":1414658465582,\"applicationType\":\"iPlanetAMWebAgentService\",\"conditions\":[\"AuthenticateToService\",\"AuthScheme\",\"IP\",\"SimpleTime\",\"OAuth2Scope\",\"AuthenticateToRealm\",\"OR\",\"AMIdentityMembership\",\"LDAPFilter\",\"SessionProperty\",\"AuthLevel\",\"Policy\",\"LEAuthLevel\",\"Session\",\"NOT\",\"AND\",\"ResourceEnvIP\"]},{\"name\":\"iPlanetAMWebAgentService\",\"resources\":[\"*://*:*/*?*\",\"*://*:*/*\"],\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"realm\":\"/\",\"subjects\":[\"JwtClaim\",\"AuthenticatedUsers\",\"Identity\",\"NOT\",\"AND\",\"OR\"],\"entitlementCombiner\":\"DenyOverride\",\"attributeNames\":[],\"description\":\"The built-in Application used by OpenAM Policy Agents.\",\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":1412268029931,\"lastModifiedDate\":1412268029931,\"applicationType\":\"iPlanetAMWebAgentService\",\"conditions\":[\"AuthenticateToService\",\"AuthLevelLE\",\"AuthScheme\",\"IP\",\"SimpleTime\",\"OAuth2Scope\",\"AuthenticateToRealm\",\"OR\",\"AMIdentityMembership\",\"LDAPFilter\",\"SessionProperty\",\"AuthLevel\",\"Session\",\"NOT\",\"AND\",\"ResourceEnvIP\"]},{\"name\":\"rere\",\"resources\":[\"*\"],\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"realm\":\"/\",\"subjects\":[\"JwtClaim\",\"AuthenticatedUsers\",\"Identity\",\"NOT\",\"Policy\",\"AND\",\"NONE\",\"OR\"],\"entitlementCombiner\":\"DenyOverride\",\"attributeNames\":[],\"description\":null,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":1414581736586,\"lastModifiedDate\":1414581736586,\"applicationType\":\"iPlanetAMWebAgentService\",\"conditions\":[\"AuthenticateToService\",\"AuthScheme\",\"IP\",\"SimpleTime\",\"OAuth2Scope\",\"AuthenticateToRealm\",\"OR\",\"AMIdentityMembership\",\"LDAPFilter\",\"SessionProperty\",\"AuthLevel\",\"Policy\",\"LEAuthLevel\",\"Session\",\"NOT\",\"AND\",\"ResourceEnvIP\"]},{\"name\":\"testapp\",\"resources\":[\"*\"],\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"realm\":\"/\",\"subjects\":[\"JwtClaim\",\"AuthenticatedUsers\",\"Identity\",\"NOT\",\"Policy\",\"AND\",\"NONE\",\"OR\"],\"entitlementCombiner\":\"DenyOverride\",\"attributeNames\":[],\"description\":null,\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":1414421154057,\"lastModifiedDate\":1414421154057,\"applicationType\":\"iPlanetAMWebAgentService\",\"conditions\":[\"AuthenticateToService\",\"AuthScheme\",\"IP\",\"SimpleTime\",\"OAuth2Scope\",\"AuthenticateToRealm\",\"OR\",\"AMIdentityMembership\",\"LDAPFilter\",\"SessionProperty\",\"AuthLevel\",\"Policy\",\"LEAuthLevel\",\"Session\",\"NOT\",\"AND\",\"ResourceEnvIP\"]}],\"resultCount\":4,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
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
            "/openam/json/subjectattributes?_queryID",
            [
                200,
                {
                    "Date": "Tue, 21 Oct 2014 11:48:11 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "2686",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[\"sunIdentityServerPPInformalName\",\"sunIdentityServerPPFacadeGreetSound\",\"uid\",\"manager\",\"sunIdentityServerPPCommonNameMN\",\"sunIdentityServerPPLegalIdentityGender\",\"preferredLocale\",\"iplanet-am-session-get-valid-sessions\",\"sunIdentityServerPPFacadegreetmesound\",\"iplanet-am-user-password-reset-question-answer\",\"telephoneNumber\",\"employeeNumber\",\"iplanet-am-user-admin-start-dn\",\"iplanet-am-user-success-url\",\"sunIdentityServerPPDemographicsDisplayLanguage\",\"iplanet-am-user-federation-info\",\"objectClass\",\"sunIdentityServerPPDemographicsLanguage\",\"authorityRevocationList\",\"sunIdentityServerPPLegalIdentityDOB\",\"sunIdentityServerPPSignKey\",\"sunIdentityServerPPEmploymentIdentityOrg\",\"iplanet-am-session-max-caching-time\",\"sn\",\"iplanet-am-session-quota-limit\",\"sunIdentityServerPPEncryPTKey\",\"iplanet-am-session-max-session-time\",\"sunIdentityServerPPCommonNamePT\",\"sun-fm-saml2-nameid-info\",\"sunIdentityServerDiscoEntries\",\"iplanet-am-user-login-status\",\"sunIdentityServerPPCommonNameCN\",\"distinguishedName\",\"iplanet-am-session-max-idle-time\",\"cn\",\"sunIdentityServerPPLegalIdentityVATIdType\",\"iplanet-am-user-password-reset-options\",\"preferredlanguage\",\"iplanet-am-user-federation-info-key\",\"sunIdentityServerPPDemographicsAge\",\"inetUserHttpURL\",\"iplanet-am-user-alias-list\",\"sunIdentityServerPPFacadeNamePronounced\",\"sunIdentityServerPPEmploymentIdentityJobTitle\",\"sunIdentityMSISDNNumber\",\"sunIdentityServerPPMsgContact\",\"givenName\",\"sunIdentityServerPPLegalIdentityMaritalStatus\",\"devicePrintProfiles\",\"memberOf\",\"sun-fm-saml2-nameid-infokey\",\"iplanet-am-session-service-status\",\"sunIdentityServerPPLegalIdentityVATIdValue\",\"userPassword\",\"sunIdentityServerPPFacadeMugShot\",\"sunIdentityServerPPEmploymentIdentityAltO\",\"iplanet-am-user-auth-config\",\"assignedDashboard\",\"iplanet-am-user-failure-url\",\"sunAMAuthInvalidAttemptsData\",\"sunIdentityServerPPLegalIdentityLegalName\",\"adminRole\",\"sunIdentityServerPPCommonNameAltCN\",\"dn\",\"iplanet-am-session-add-session-listener-on-all-sessions\",\"userCertificate\",\"mail\",\"sunIdentityServerPPLegalIdentityAltIdValue\",\"iplanet-am-user-password-reset-force-reset\",\"caCertificate\",\"sunIdentityServerPPEmergencyContact\",\"sunIdentityServerPPDemographicsBirthDay\",\"sunIdentityServerPPFacadeWebSite\",\"preferredtimezone\",\"sunIdentityServerPPLegalIdentityAltIdType\",\"sunIdentityServerPPCommonNameSN\",\"sunIdentityServerPPAddressCard\",\"postalAddress\",\"iplanet-am-session-destroy-sessions\",\"inetUserStatus\",\"iplanet-am-auth-configuration\",\"iplanet-am-user-auth-modules\",\"iplanet-am-user-account-life\",\"sunIdentityServerPPDemographicsTimeZone\",\"sunIdentityServerPPCommonNameFN\"],\"resultCount\":85,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
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
                    "Date": "Tue, 21 Oct 2014 11:48:11 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "ETag": "&quot;1412268029825&quot;",
                    "Content-Length": "739",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"name\":\"sunIdentityServerLibertyPPService\",\"resources\":[\"*\"],\"actions\":{\"QUERY_interactForConsent\":false,\"QUERY_interactForValue\":false,\"MODIFY_interactForValue\":false,\"QUERY_deny\":false,\"MODIFY_deny\":false,\"MODIFY_interactForConsent\":false,\"MODIFY_allow\":true,\"QUERY_allow\":true},\"attributeNames\":[],\"realm\":\"/\",\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"description\":null,\"subjects\":[\"User\",\"NOT\",\"AND\",\"Attribute\",\"Role\",\"Group\",\"OR\"],\"entitlementCombiner\":\"DenyOverride\",\"creationDate\":1412268029825,\"lastModifiedDate\":1412268029825,\"applicationType\":\"sunIdentityServerLibertyPPService\",\"conditions\":[\"Time\",\"NOT\",\"IP\",\"AND\",\"DNSName\",\"OR\"]}"
            ]
        );

        server.respondWith(
            "GET",
            "/openam/json/policies?_queryFilter=applicationName+eq+%22sunIdentityServerLibertyPPService%22&_pageSize=10&_sortKeys=name&_pagedResultsOffset=0",
            [
                200,
                {
                    "Date": "Tue, 21 Oct 2014 11:48:09 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "81",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[],\"resultCount\":0,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
            ]
        );
    };
});
