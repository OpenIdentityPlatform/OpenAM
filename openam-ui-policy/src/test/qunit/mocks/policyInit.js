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
    "text!templates/policy/common/HelpLinkTemplate.html",
    "text!templates/policy/login/LoginDialog.html",

    "text!templates/policy/applications/EditApplicationTemplate.html",
    "text!templates/policy/applications/ManageAppsGridActionsTemplate.html",
    "text!templates/policy/applications/ManageAppsGridCellActionsTemplate.html",
    "text!templates/policy/applications/ManageAppsGridTemplate.html",
    "text!templates/policy/applications/ManageAppsTemplate.html",
    "text!templates/policy/applications/ReviewApplicationStepTemplate.html",

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

    "text!templates/policy/policies/ActionsTemplate.html",
    "text!templates/policy/policies/EditPolicyTemplate.html",
    "text!templates/policy/policies/ManagePoliciesGridActionsTemplate.html",
    "text!templates/policy/policies/ManagePoliciesGridTemplate.html",
    "text!templates/policy/policies/ManagePoliciesTemplate.html",
    "text!templates/policy/policies/ReviewPolicyStepTemplate.html",
    "text!templates/policy/policies/ManagePoliciesHeaderTemplate.html",

    "text!templates/policy/referrals/EditReferralTemplate.html",
    "text!templates/policy/referrals/ManageRefsGridActionsTemplate.html",
    "text!templates/policy/referrals/ManageRefsGridTemplate.html",
    "text!templates/policy/referrals/ReviewReferralStepTemplate.html",
    "text!templates/policy/referrals/SelectRealmsTemplate.html",

    "text!templates/policy/resources/AddNewResourceTemplate.html",
    "text!templates/policy/resources/ResourcesListTemplate.html",

    "text!configuration.json"
], function () {
    /* an unfortunate need to duplicate the file names here, but I haven't
     yet found a way to fool requirejs into doing dynamic dependencies */
    var staticFiles = [
            "templates/policy/BaseTemplate.html",
            "templates/policy/common/HelpLinkTemplate.html",
            "templates/policy/login/LoginDialog.html",

            "templates/policy/applications/EditApplicationTemplate.html",
            "templates/policy/applications/ManageAppsGridActionsTemplate.html",
            "templates/policy/applications/ManageAppsGridCellActionsTemplate.html",
            "templates/policy/applications/ManageAppsGridTemplate.html",
            "templates/policy/applications/ManageAppsTemplate.html",
            "templates/policy/applications/ReviewApplicationStepTemplate.html",

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

            "templates/policy/policies/ActionsTemplate.html",
            "templates/policy/policies/EditPolicyTemplate.html",
            "templates/policy/policies/ManagePoliciesGridActionsTemplate.html",
            "templates/policy/policies/ManagePoliciesGridTemplate.html",
            "templates/policy/policies/ManagePoliciesTemplate.html",
            "templates/policy/policies/ReviewPolicyStepTemplate.html",
            "templates/policy/policies/ManagePoliciesHeaderTemplate.html",

            "templates/policy/referrals/EditReferralTemplate.html",
            "templates/policy/referrals/ManageRefsGridActionsTemplate.html",
            "templates/policy/referrals/ManageRefsGridTemplate.html",
            "templates/policy/referrals/ReviewReferralStepTemplate.html",
            "templates/policy/referrals/SelectRealmsTemplate.html",

            "templates/policy/resources/AddNewResourceTemplate.html",
            "templates/policy/resources/ResourcesListTemplate.html",

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
            /\/json\/serverinfo\/\*/,
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
            /\/json\/applications\?filters=/,
            [
                200,
                {
                    "Date": "Tue, 17 Feb 2015 14:38:47 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "1034",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"name\":\"iPlanetAMWebAgentService\",\"resources\":[\"*://*:*/*?*\",\"*://*:*/*\"],\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"attributeNames\":[],\"conditions\":[\"AuthenticateToService\",\"AuthScheme\",\"IPv6\",\"SimpleTime\",\"OAuth2Scope\",\"IPv4\",\"AuthenticateToRealm\",\"OR\",\"AMIdentityMembership\",\"LDAPFilter\",\"SessionProperty\",\"AuthLevel\",\"LEAuthLevel\",\"Session\",\"NOT\",\"AND\",\"ResourceEnvIP\"],\"resourceComparator\":null,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"iPlanetAMWebAgentService\",\"creationDate\":1423749236772,\"lastModifiedDate\":1423749236772,\"realm\":\"/\",\"description\":\"The built-in Application used by OpenAM Policy Agents.\",\"subjects\":[\"JwtClaim\",\"AuthenticatedUsers\",\"Identity\",\"NOT\",\"AND\",\"NONE\",\"OR\"],\"saveIndex\":null,\"searchIndex\":null,\"entitlementCombiner\":\"DenyOverride\",\"editable\":true}],\"resultCount\":1,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
            ]
        );

        server.respondWith(
            "GET",
            /\/json\/applications\/iPlanetAMWebAgentService/,
            [
                200,
                {
                    "Date": "Tue, 17 Feb 2015 14:38:49 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "ETag": "&quot;1423749236772&quot;",
                    "Content-Length": "953",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"name\":\"iPlanetAMWebAgentService\",\"resources\":[\"*://*:*/*?*\",\"*://*:*/*\"],\"actions\":{\"POST\":true,\"PATCH\":true,\"GET\":true,\"DELETE\":true,\"OPTIONS\":true,\"HEAD\":true,\"PUT\":true},\"attributeNames\":[],\"conditions\":[\"AuthenticateToService\",\"AuthScheme\",\"IPv6\",\"SimpleTime\",\"OAuth2Scope\",\"IPv4\",\"AuthenticateToRealm\",\"OR\",\"AMIdentityMembership\",\"LDAPFilter\",\"SessionProperty\",\"AuthLevel\",\"LEAuthLevel\",\"Session\",\"NOT\",\"AND\",\"ResourceEnvIP\"],\"resourceComparator\":null,\"createdBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedBy\":\"id=dsameuser,ou=user,dc=openam,dc=forgerock,dc=org\",\"applicationType\":\"iPlanetAMWebAgentService\",\"creationDate\":1423749236772,\"lastModifiedDate\":1423749236772,\"realm\":\"/\",\"description\":\"The built-in Application used by OpenAM Policy Agents.\",\"subjects\":[\"JwtClaim\",\"AuthenticatedUsers\",\"Identity\",\"NOT\",\"AND\",\"NONE\",\"OR\"],\"saveIndex\":null,\"searchIndex\":null,\"entitlementCombiner\":\"DenyOverride\",\"editable\":true}"
            ]
        );

        server.respondWith(
            "GET",
            /\/json\/applicationtypes\/iPlanetAMWebAgentService/,
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
            /\/json\/subjecttypes\?_queryId=&_fields=title,logical,config/,
            [
                200,
                {
                    "Date": "Tue, 17 Feb 2015 14:38:53 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "1072",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"title\":\"AND\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"subjects\":{\"type\":\"array\",\"items\":{\"type\":\"any\"}}}}},{\"title\":\"AuthenticatedUsers\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{}}},{\"title\":\"Identity\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"subjectValues\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}}}},{\"title\":\"JwtClaim\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"claimName\":{\"type\":\"string\"},\"claimValue\":{\"type\":\"string\"}}}},{\"title\":\"NONE\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{}}},{\"title\":\"NOT\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"subject\":{\"type\":\"object\",\"properties\":{}}}}},{\"title\":\"OR\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"subjects\":{\"type\":\"array\",\"items\":{\"type\":\"any\"}}}}},{\"title\":\"Policy\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"name\":{\"type\":\"string\"},\"className\":{\"type\":\"string\"},\"values\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}}}}],\"resultCount\":8,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
            ]
        );

        server.respondWith(
            "GET",
            /\/json\/conditiontypes\?_queryId=&_fields=title,logical,config/,
            [
                200,
                {
                    "Date": "Tue, 17 Feb 2015 14:38:53 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "2850",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"title\":\"AMIdentityMembership\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"amIdentityName\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}}}},{\"title\":\"AND\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"conditions\":{\"type\":\"array\",\"items\":{\"type\":\"any\"}}}}},{\"title\":\"AuthLevel\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"authLevel\":{\"type\":\"integer\"}}}},{\"title\":\"AuthScheme\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"authScheme\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}},\"applicationIdleTimeout\":{\"type\":\"integer\"},\"applicationName\":{\"type\":\"string\"}}}},{\"title\":\"AuthenticateToRealm\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"authenticateToRealm\":{\"type\":\"string\"}}}},{\"title\":\"AuthenticateToService\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"authenticateToService\":{\"type\":\"string\"}}}},{\"title\":\"IPv4\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"startIp\":{\"type\":\"string\"},\"endIp\":{\"type\":\"string\"},\"dnsName\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}}}},{\"title\":\"IPv6\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"startIp\":{\"type\":\"string\"},\"endIp\":{\"type\":\"string\"},\"dnsName\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}}}},{\"title\":\"LDAPFilter\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"ldapFilter\":{\"type\":\"string\"}}}},{\"title\":\"LEAuthLevel\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"authLevel\":{\"type\":\"integer\"}}}},{\"title\":\"NOT\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"condition\":{\"type\":\"object\",\"properties\":{}}}}},{\"title\":\"OAuth2Scope\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"requiredScopes\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}}}},{\"title\":\"OR\",\"logical\":true,\"config\":{\"type\":\"object\",\"properties\":{\"conditions\":{\"type\":\"array\",\"items\":{\"type\":\"any\"}}}}},{\"title\":\"Policy\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"className\":{\"type\":\"string\"},\"properties\":{\"type\":\"object\"}}}},{\"title\":\"ResourceEnvIP\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"resourceEnvIPConditionValue\":{\"type\":\"array\",\"items\":{\"type\":\"string\"}}}}},{\"title\":\"Session\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"maxSessionTime\":{\"type\":\"number\"},\"terminateSession\":{\"type\":\"boolean\",\"required\":true}}}},{\"title\":\"SessionProperty\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"ignoreValueCase\":{\"type\":\"boolean\",\"required\":true},\"properties\":{\"type\":\"object\"}}}},{\"title\":\"SimpleTime\",\"logical\":false,\"config\":{\"type\":\"object\",\"properties\":{\"startTime\":{\"type\":\"string\"},\"endTime\":{\"type\":\"string\"},\"startDay\":{\"type\":\"string\"},\"endDay\":{\"type\":\"string\"},\"startDate\":{\"type\":\"string\"},\"endDate\":{\"type\":\"string\"},\"enforcementTimeZone\":{\"type\":\"string\"}}}}],\"resultCount\":18,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
            ]
        );

        server.respondWith(
            "GET",
            /\/json\/decisioncombiners\/\?_queryId=&_fields=title/,
            [
                200,
                {
                    "Date": "Thu, 19 Feb 2015 08:30:40 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "105",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"title\":\"DenyOverride\"}],\"resultCount\":1,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
            ]
        );

        server.respondWith(
            "GET",
            /\/json\/subjectattributes\?_queryFilter=true/,
            [
                200,
                {
                    "Date": "Tue, 17 Feb 2015 14:38:53 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "2722",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[\"sunIdentityServerPPInformalName\",\"sunIdentityServerPPFacadeGreetSound\",\"uid\",\"manager\",\"sunIdentityServerPPCommonNameMN\",\"sunIdentityServerPPLegalIdentityGender\",\"preferredLocale\",\"iplanet-am-session-get-valid-sessions\",\"sunIdentityServerPPFacadegreetmesound\",\"iplanet-am-user-password-reset-question-answer\",\"telephoneNumber\",\"employeeNumber\",\"iplanet-am-user-admin-start-dn\",\"iplanet-am-user-success-url\",\"sunIdentityServerPPDemographicsDisplayLanguage\",\"iplanet-am-user-federation-info\",\"objectClass\",\"sunIdentityServerPPDemographicsLanguage\",\"authorityRevocationList\",\"sunIdentityServerPPLegalIdentityDOB\",\"sunIdentityServerPPSignKey\",\"sunIdentityServerPPEmploymentIdentityOrg\",\"createTimestamp\",\"iplanet-am-session-max-caching-time\",\"sn\",\"iplanet-am-session-quota-limit\",\"sunIdentityServerPPEncryPTKey\",\"iplanet-am-session-max-session-time\",\"sunIdentityServerPPCommonNamePT\",\"sun-fm-saml2-nameid-info\",\"sunIdentityServerDiscoEntries\",\"iplanet-am-user-login-status\",\"sunIdentityServerPPCommonNameCN\",\"distinguishedName\",\"iplanet-am-session-max-idle-time\",\"cn\",\"sunIdentityServerPPLegalIdentityVATIdType\",\"iplanet-am-user-password-reset-options\",\"preferredlanguage\",\"iplanet-am-user-federation-info-key\",\"sunIdentityServerPPDemographicsAge\",\"inetUserHttpURL\",\"iplanet-am-user-alias-list\",\"sunIdentityServerPPFacadeNamePronounced\",\"sunIdentityServerPPEmploymentIdentityJobTitle\",\"sunIdentityMSISDNNumber\",\"sunIdentityServerPPMsgContact\",\"givenName\",\"sunIdentityServerPPLegalIdentityMaritalStatus\",\"devicePrintProfiles\",\"memberOf\",\"sun-fm-saml2-nameid-infokey\",\"iplanet-am-session-service-status\",\"sunIdentityServerPPLegalIdentityVATIdValue\",\"userPassword\",\"sunIdentityServerPPFacadeMugShot\",\"sunIdentityServerPPEmploymentIdentityAltO\",\"iplanet-am-user-auth-config\",\"assignedDashboard\",\"iplanet-am-user-failure-url\",\"sunAMAuthInvalidAttemptsData\",\"sunIdentityServerPPLegalIdentityLegalName\",\"adminRole\",\"sunIdentityServerPPCommonNameAltCN\",\"dn\",\"iplanet-am-session-add-session-listener-on-all-sessions\",\"userCertificate\",\"mail\",\"sunIdentityServerPPLegalIdentityAltIdValue\",\"iplanet-am-user-password-reset-force-reset\",\"caCertificate\",\"sunIdentityServerPPEmergencyContact\",\"sunIdentityServerPPDemographicsBirthDay\",\"sunIdentityServerPPFacadeWebSite\",\"preferredtimezone\",\"sunIdentityServerPPLegalIdentityAltIdType\",\"sunIdentityServerPPCommonNameSN\",\"sunIdentityServerPPAddressCard\",\"postalAddress\",\"iplanet-am-session-destroy-sessions\",\"modifyTimestamp\",\"inetUserStatus\",\"iplanet-am-auth-configuration\",\"iplanet-am-user-auth-modules\",\"iplanet-am-user-account-life\",\"sunIdentityServerPPDemographicsTimeZone\",\"sunIdentityServerPPCommonNameFN\"],\"resultCount\":87,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
            ]
        );

        server.respondWith(
            "GET",
            /\/json\/policies\/test_pol/,
            [
                200,
                {
                    "Date": "Tue, 17 Feb 2015 14:38:53 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "ETag": "&quot;1424172768985&quot;",
                    "Content-Length": "831",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"name\":\"test_pol\",\"active\":true,\"description\":\"\",\"resources\":[\"*://*:*/*?*\"],\"applicationName\":\"iPlanetAMWebAgentService\",\"actionValues\":{\"PATCH\":true,\"OPTIONS\":true},\"subject\":{\"type\":\"NOT\",\"subject\":{\"type\":\"NONE\"}},\"condition\":{\"type\":\"IPv4\",\"startIp\":\"1.1.1.1\",\"endIp\":\"2.2.2.2\",\"ipRange\":[],\"dnsName\":[]},\"resourceAttributes\":[{\"type\":\"User\",\"propertyName\":\"caCertificate\",\"propertyValues\":[]},{\"type\":\"Static\",\"propertyName\":\"rrr\",\"propertyValues\":[\"ii\"]},{\"type\":\"User\",\"propertyName\":\"authorityRevocationList\",\"propertyValues\":[]},{\"type\":\"User\",\"propertyName\":\"assignedDashboard\",\"propertyValues\":[]}],\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedDate\":\"2015-02-17T11:32:48.985Z\",\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2015-02-17T11:32:48.985Z\"}"
            ]
        );

        server.respondWith(
            "GET",
            /\/json\/policies\?filters=/,
            [
                200,
                {
                    "Date": "Tue, 17 Feb 2015 11:32:58 GMT",
                    "Cache-Control": "no-cache",
                    "Server": "Apache-Coyote/1.1",
                    "Content-Length": "912",
                    "Content-API-Version": "protocol=1.0,resource=1.0",
                    "Content-Type": "application/json;charset=UTF-8"
                },
                "{\"result\":[{\"name\":\"test_pol\",\"active\":true,\"description\":\"\",\"resources\":[\"*://*:*/*?*\"],\"applicationName\":\"iPlanetAMWebAgentService\",\"actionValues\":{\"PATCH\":true,\"OPTIONS\":true},\"subject\":{\"type\":\"NOT\",\"subject\":{\"type\":\"NONE\"}},\"condition\":{\"type\":\"IPv4\",\"startIp\":\"1.1.1.1\",\"endIp\":\"2.2.2.2\",\"ipRange\":[],\"dnsName\":[]},\"resourceAttributes\":[{\"type\":\"User\",\"propertyName\":\"caCertificate\",\"propertyValues\":[]},{\"type\":\"Static\",\"propertyName\":\"rrr\",\"propertyValues\":[\"ii\"]},{\"type\":\"User\",\"propertyName\":\"authorityRevocationList\",\"propertyValues\":[]},{\"type\":\"User\",\"propertyName\":\"assignedDashboard\",\"propertyValues\":[]}],\"lastModifiedBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"lastModifiedDate\":\"2015-02-17T11:32:48.985Z\",\"createdBy\":\"id=amadmin,ou=user,dc=openam,dc=forgerock,dc=org\",\"creationDate\":\"2015-02-17T11:32:48.985Z\"}],\"resultCount\":1,\"pagedResultsCookie\":null,\"remainingPagedResults\":0}"
            ]
        );
    };
});
