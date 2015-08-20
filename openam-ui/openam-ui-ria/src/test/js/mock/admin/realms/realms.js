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

/*global define*/
define("mock/admin/realms/realms", [
], function () {
    return function (server) {
        server.respondWith(
            "GET",
            /\/json\/global-config\/realms\/$/,
            [
                200,
                { },
                JSON.stringify(
                    {
                        "parentPath": null,
                        "active": true,
                        "name": "/",
                        "aliases": ["openam.local.esergueeva.com", "openam"]
                    }
                )
            ]
        );

        server.respondWith(
            "GET",
            /\/json\/global-config\/realms\?_queryFilter=true/,
            [
                200,
                { },
                JSON.stringify(
                    {
                        "result": [
                            {
                                "parentPath": null,
                                "active": true,
                                "name": "/",
                                "aliases": ["openam.local.esergueeva.com", "openam"]
                            }
                        ],
                        "resultCount": 1,
                        "pagedResultsCookie": null,
                        "remainingPagedResults": -1
                    }
                )
            ]
        );

        server.respondWith(
            "GET",
            /\/json\/realm-config\/commontasks\?_queryFilter=true/,
            [
                200,
                { },
                JSON.stringify(
                    {"result": [
                        {"name": "Create SAMLv2 Providers", "description": "Use these work flows to create hosted or remote identity and service providers for SAMLv2 Federation.", "tasks": [
                            {"name": "Create Hosted Identity Provider", "description": "This allows you to configure this instance of OpenAM server as an Identity Provider (IDP). You need three things: Name, Circle of Trust (COT) and optionally Signing Certificate. Metadata represents the configuration necessary to execute federation protocols (eg SAMLv2) as well as the mechanism to communicate this configuration to other entities (eg SPs) in a COT. A COT is a group of IDPs and SPs that trust each other and in effect represents the confines within which all federation communications are performed.", "link": "task/CreateHostedIDP"},
                            {"name": "Create Hosted Service Provider", "description": "This allows you to configure this instance of OpenAM server as an Service Provider (SP). You need three things: Name, Circle of Trust (COT). Metadata represents the configuration necessary to execute federation protocols (eg SAMLv2) as well as the mechanism to communicate this configuration to other entities (eg IDPs) in a COT. A COT is a group of IDPs and SPs that trust each other and in effect represents the confines within which all federation communications are performed.", "link": "task/CreateHostedSP"},
                            {"name": "Register Remote Identity Provider", "description": "This allows you to register a remote Identity Provider (IDP). You need two things: Circle of Trust (COT). Metadata represents the configuration necessary to execute federation protocols (eg SAMLv2) as well as the mechanism to communicate this configuration to other entities (eg SPs) in a COT. A COT is a group of IDPs and SPs that trust each other and in effect represents the confines within which all federation communications are performed.", "link": "task/CreateRemoteIDP"},
                            {"name": "Register Remote Service Provider", "description": "This allows you to register a remote Service Provider (SP). You need two things: Circle of Trust (COT). Metadata represents the configuration necessary to execute federation protocols (eg SAMLv2) as well as the mechanism to communicate this configuration to other entities (eg SPs) in a COT. A COT is a group of IDPs and SPs that trust each other and in effect represents the confines within which all federation communications are performed.", "link": "task/CreateRemoteSP"}
                        ], "_id": "saml2"},
                        {"name": "Configure OAuth2/OpenID Connect", "description": "This task configures OAuth2/OpenID Connect per realm. Each realm can act as an authorization server.", "tasks": [
                            {"name": "Configure OAuth2/OpenID Connect", "description": "You configure the OAuth 2.0/OpenID Connect authorization service for a particular realm. This process also protects the authorization endpoint using a standard policy.", "link": "task/ConfigureOAuth2"}
                        ], "_id": "oauth2"},
                        {"name": "Create Fedlet", "description": "Create a Fedlet to enable federation between an identity provider hosted on this instance of OpenAM and a remote service provider that does not have a federation solution. Before beginning, a hosted identity provider must be configured.", "tasks": [
                            {"name": "Create Fedlet", "description": "Fedlet is ideal for an identity provider that needs to enable a service provider that does not have any kind of federation solution in place. A Fedlet is a very small zip file that you can provide a service provider so they can instantaneously federate with you. The service provider simply adds the Fedlet to their application, deploys their application and they are federation enabled.", "link": "task/CreateFedlet"}
                        ], "_id": "fedlet"},
                        {"name": "Configure Google Apps", "description": "Integrate OpenAM with Google Apps web applications to create a single sign-on environment. Before beginning, a hosted identity provider and Circle of Trust must be configured.", "tasks": [
                            {"name": "Configure Google Apps", "description": "For instructions on integrating Google Apps with OpenAM, see https://wikis.forgerock.org/confluence/display/openam/Integrate+With+Google+Apps", "link": "task/ConfigureGoogleApps"}
                        ], "_id": "googleapps"},
                        {"name": "Configure Salesforce CRM", "description": "Integrate OpenAM with Salesforce CRM to create a single sign-on environment. Before beginning, a SAMLv2 hosted identity provider and Circle of Trust must be configured.", "tasks": [
                            {"name": "Configure Salesforce CRM", "description": "See the OpenAM Wiki for more information.", "link": "task/ConfigureSalesForceApps"}
                        ], "_id": "salesforce"},
                        {"name": "Configure Social Authentication", "description": "Add social authentication options per realm. This task configures authentication through third parties such as Facebook, Google and Microsoft.", "tasks": [
                            {"name": "Configure Facebook Authentication", "description": "This task guides you through the process of adding Facebook as an authentication option for a particular realm. Once configured, the OpenAM login page for the chosen realm will include a link to authenticate using a Facebook account.", "link": "task/ConfigureSocialAuthN?type=facebook"},
                            {"name": "Configure Google Authentication", "description": "This task guides you through the process of adding Google as an authentication option for a particular realm. Once configured, the OpenAM login page for the chosen realm will include a link to authenticate using a Google account.", "link": "task/ConfigureSocialAuthN?type=google"},
                            {"name": "Configure Microsoft Authentication", "description": "This task guides you through the process of adding Microsoft as an authentication option for a particular realm. Once configured, the OpenAM login page for the chosen realm will include a link to authenticate using a Microsoft account.", "link": "task/ConfigureSocialAuthN?type=microsoft"},
                            {"name": "Configure Other Authentication", "description": "This task guides you through the process of adding another third party as an authentication option for a particular realm. Once configured, the OpenAM login page for the chosen realm will include a link to authenticate using an account held with this third party.", "link": "task/ConfigureSocialAuthN?type=other"}
                        ], "_id": "socialauthentication"},
                        {"name": "Test Federation Connectivity", "description": "Use this automated test to determine if federation connections are being made successfully, or to identify where issues might be located.", "tasks": [
                            {"name": "Test Federation Connectivity", "description": "Whether you have just set up your Federated accounts or are interested in troubleshooting an issue with your existing accounts, this test will show you if the connections are being made successfully or identify where the troubles are located.", "link": "task/ValidateSAML2Setup"}
                        ], "_id": "testfederation"},
                        {"name": "Get Product Documentation", "description": "Launch the OpenAM product documentation page.", "tasks": [
                            {"name": "Get Product Documentation", "description": "This link launches the OpenAM product documentation page. For additional information, visit the OpenAM community site at http://openam.forgerock.org/ and the OpenAM Wiki at https://wikis.forgerock.org/confluence/display/OPENAM/Home", "link": "http://docs.forgerock.org/en/index.html?product=openam"}
                        ], "_id": "documenation"}
                    ], "resultCount": 8, "pagedResultsCookie": null, "remainingPagedResults": -1}
                )
            ]
        );

        server.respondWith(
            "POST",
            /\/json\/global-config\/realms\/\?_action=schema/,
            [
                200,
                { },
                JSON.stringify(
                    {
                        "type": "object",
                        "properties": {
                            "name": {
                                "type": "string",
                                "title": "Name",
                                "required": true
                            },
                            "parentPath": {
                                "type": "string",
                                "title": "Parent",
                                "required": true
                            },
                            "aliases": {
                                "type": "array",
                                "items": {
                                    "type": "string"
                                },
                                "title": "Realm/DNS Aliases",
                                "description": "List of associated DNS domains for this realm.<br><br>When a request is " +
                                    "received by the authentication user interface, OpenAM searches this attribute in all " +
                                    "realms to find the matching realm into which the user should be authenticated. OpenAM " +
                                    "must only find one matching realm so therefore do not put duplicate entries into " +
                                    "multiple realms. Additionally if OpenAM does not find a matching realm, the user is " +
                                    "presented with an error screen.",
                                "required": true
                            },
                            "active": {
                                "type": "boolean",
                                "title": "Realm Status",
                                "description": "Enable or Disable this realm.<br><br>If the realm is disabled, all OpenAM " +
                                    "services will be unavailable for all users in this realm.",
                                "required": true
                            }
                        }
                    }
                )
            ]
        );
    };
});
