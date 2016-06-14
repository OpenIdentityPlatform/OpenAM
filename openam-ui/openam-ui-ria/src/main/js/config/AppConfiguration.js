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
 * Portions copyright 2011-2016 ForgeRock AS.
 */

define([
    "org/forgerock/openam/ui/common/util/Constants"
], function (Constants) {
    var obj = {
        moduleDefinition: [{
            moduleClass: "org/forgerock/commons/ui/common/main/SessionManager",
            configuration: {
                loginHelperClass: "org/forgerock/openam/ui/user/login/RESTLoginHelper"
            }
        }, {
            moduleClass: "org/forgerock/commons/ui/common/main/Router",
            configuration: {
                routes: {},
                loader: [
                    { "routes": "config/routes/AMRoutesConfig" },
                    { "routes": "config/routes/CommonRoutesConfig" },
                    { "routes": "config/routes/UserRoutesConfig" },
                    { "routes": "config/routes/admin/RealmsRoutes" },
                    { "routes": "config/routes/admin/GlobalRoutes" },
                    { "routes": "config/routes/user/UMARoutes" }
                ]
            }
        }, {
            moduleClass: "org/forgerock/commons/ui/common/SiteConfigurator",
            configuration: {
                selfRegistration: false,
                enterprise: false,
                remoteConfig: true,
                delegate: "org/forgerock/openam/ui/common/services/SiteConfigurationService"
            }
        }, {
            moduleClass: "org/forgerock/commons/ui/common/main/ProcessConfiguration",
            configuration: {
                processConfigurationFiles: [
                    "config/process/AMConfig",
                    "config/process/CommonConfig"
                ]
            }
        }, {
            moduleClass: "org/forgerock/commons/ui/common/main/ServiceInvoker",
            configuration: {
                defaultHeaders: {
                }
            }
        }, {
            moduleClass: "org/forgerock/commons/ui/common/main/ErrorsHandler",
            configuration: {
                defaultHandlers: {
                },
                loader: [
                        { "defaultHandlers": "config/errorhandlers/CommonErrorHandlers" }
                ]
            }
        }, {
            moduleClass: "org/forgerock/commons/ui/common/util/UIUtils",
            configuration: {
                templateUrls: [
                    "templates/common/components/table/ReadOnlyRow.html",
                    "templates/common/components/table/NewRow.html",
                    "templates/common/components/table/EditRow.html"
                ],
                partialUrls: [
                    "partials/form/_JSONSchemaFooter.html",
                    "partials/form/_AutoCompleteOffFix.html",
                    "partials/form/_Button.html",
                    "partials/form/_Select.html",
                    "partials/headers/_Title.html",
                    "partials/headers/_TitleWithSubAndIcon.html",
                    "partials/login/_Choice.html",
                    "partials/login/_Confirmation.html",
                    "partials/login/_Default.html",
                    "partials/login/_HiddenValue.html",
                    "partials/login/_Password.html",
                    "partials/login/_Redirect.html",
                    "partials/login/_RememberLogin.html",
                    "partials/login/_ScriptTextOutput.html",
                    "partials/login/_SelfService.html",
                    "partials/login/_SocialAuthn.html",
                    "partials/login/_TextInput.html",
                    "partials/login/_TextOutput.html",
                    "partials/login/_PollingWait.html"
                ]
            }
        }, {
            moduleClass: "org/forgerock/commons/ui/common/components/Messages",
            configuration: {
                messages: {
                },
                loader: [
                    { "messages": "config/messages/CommonMessages" },
                    { "messages": "config/messages/UserMessages" },
                    { "messages": "config/AppMessages" }
                ]
            }
        }, {
            moduleClass: "org/forgerock/commons/ui/common/main/ValidatorsManager",
            configuration: {
                validators: { },
                loader: [
                     { "validators": "config/validators/CommonValidators" },
                     { "validators": "config/validators/AMValidators" }
                ]
            }
        }, {
            moduleClass: "org/forgerock/commons/ui/common/components/Navigation",
            configuration: {
                username: {
                    "label" : "config.AppConfiguration.Navigation.username.label"
                },
                userBar: [{
                    "href": "#profile/details",
                    "i18nKey": "common.user.selfService",
                    "navGroup": "admin",
                    "visibleToRoles": ["ui-self-service-user"]
                }, {
                    "href": "#profile/details",
                    "i18nKey": "common.user.profile",
                    "navGroup": "user",
                    "visibleToRoles": ["ui-self-service-user"]
                }, {
                    "href": "#realms",
                    "i18nKey": "common.user.administration",
                    "navGroup": "user",
                    "visibleToRoles": ["ui-realm-admin"]
                }, {
                    "href": "#logout/",
                    "i18nKey": "common.form.logout"
                }],
                links: {
                    "admin": {
                        "urls": {
                            "realms": {
                                "url": "#realms",
                                "name": "config.AppConfiguration.Navigation.links.realms.title",
                                "icon": "fa fa-cloud hidden-md",
                                "dropdown" : true,
                                "urls": [{
                                    "url": "#realms",
                                    "name": "config.AppConfiguration.Navigation.links.realms.showAll",
                                    "icon": "fa fa-th"
                                }, {
                                    "url": "#realms/new",
                                    "name": "config.AppConfiguration.Navigation.links.realms.newRealm",
                                    "icon": "fa fa-plus"
                                }, {
                                    divider: true
                                }],
                                "visibleToRoles": ["ui-realm-admin"]
                            },
                            "configure": {
                                "url": "#configure",
                                "name": "config.AppConfiguration.Navigation.links.configure.title",
                                "icon": "fa fa-wrench hidden-md",
                                "dropdown" : true,
                                "urls": [{
                                    "url": "#configure/authentication",
                                    "name": "config.AppConfiguration.Navigation.links.configure.authentication",
                                    "icon": "fa fa-user"
                                }, {
                                    "event": Constants.EVENT_REDIRECT_TO_JATO_CONFIGURATION,
                                    //"url": "#configure/global-services",
                                    "name": "config.AppConfiguration.Navigation.links.configure.global-services",
                                    "icon": "fa fa-globe"
                                }, {
                                    "url": "#configure/server-defaults/general",
                                    "name": "config.AppConfiguration.Navigation.links.configure.server-defaults",
                                    "icon": "fa fa-server"
                                }],
                                "visibleToRoles": ["ui-global-admin"]
                            },
                            "deployment": {
                                "url": "#deployment",
                                "name": "config.AppConfiguration.Navigation.links.deployment.title",
                                "icon": "fa fa-sitemap hidden-md",
                                "dropdown" : true,
                                "urls": [{
                                    "url": "#deployment/servers",
                                    "name": "config.AppConfiguration.Navigation.links.deployment.servers",
                                    "icon": "fa fa-server"
                                }, {
                                    "url": "#deployment/sites",
                                    "name": "config.AppConfiguration.Navigation.links.deployment.sites",
                                    "icon": "fa fa-bookmark-o"
                                }],
                                "visibleToRoles": ["ui-global-admin"]
                            },
                            "federation": {
                                "event": Constants.EVENT_REDIRECT_TO_JATO_FEDERATION,
                                "name": "config.AppConfiguration.Navigation.links.federation",
                                "icon": "fa fa-building-o hidden-md",
                                "visibleToRoles": ["ui-global-admin"]
                            },
                            "sessions": {
                                "event": Constants.EVENT_REDIRECT_TO_JATO_SESSIONS,
                                "name": "config.AppConfiguration.Navigation.links.sessions",
                                "icon": "fa fa-users hidden-md",
                                "visibleToRoles": ["ui-global-admin"]
                            }
                        }
                    },
                    "user" : {
                        "urls": {
                            "dashboard": {
                                "url": "#dashboard/",
                                "name": "config.AppConfiguration.Navigation.links.dashboard",
                                "icon": "fa fa-dashboard",
                                "visibleToRoles": ["ui-self-service-user"]
                            },
                            "uma": {
                                "icon": "fa fa-user",
                                "name": "config.AppConfiguration.Navigation.links.uma",
                                "dropdown" : true,
                                "urls": {
                                    "listResource": {
                                        "url": "#uma/resources/",
                                        "name": "config.AppConfiguration.Navigation.links.umaLinks.resources"
                                    },
                                    "listHistory": {
                                        "url": "#uma/history/",
                                        "name": "config.AppConfiguration.Navigation.links.umaLinks.history"
                                    },
                                    "listRequests": {
                                        "url": "#uma/requests/",
                                        "name": "config.AppConfiguration.Navigation.links.umaLinks.requests"
                                    }
                                },
                                "visibleToRoles": ["ui-uma-user"]
                            }
                        }
                    }
                }
            }
        }],
        loggerLevel: "debug"
    };
    return obj;
});
