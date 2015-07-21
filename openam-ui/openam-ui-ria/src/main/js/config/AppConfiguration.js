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
 * Portions copyright 2011-2015 ForgeRock AS.
 */

/*global define*/
define("config/AppConfiguration", [
    "org/forgerock/openam/ui/common/util/Constants"
], function (Constants) {
    var obj = {
            moduleDefinition: [
                {
                    moduleClass: "org/forgerock/commons/ui/common/main/SessionManager",
                    configuration: {
                        loginHelperClass: "org/forgerock/openam/ui/user/login/RESTLoginHelper"
                    }
                },
                {
                    moduleClass: "org/forgerock/commons/ui/common/main/GenericRouteInterfaceMap",
                    configuration: {
                        LoginView : "org/forgerock/openam/ui/user/login/RESTLoginView",
                        UserProfileView : "org/forgerock/commons/ui/user/profile/UserProfileView",
                        LoginDialog : "org/forgerock/openam/ui/user/login/RESTLoginDialog",
                        RegisterView : "org/forgerock/openam/ui/user/profile/RegisterView",
                        ChangeSecurityDataDialog : "org/forgerock/openam/ui/user/profile/ChangeSecurityDataDialog"
                    }
                },
                {
                    moduleClass: "org/forgerock/commons/ui/common/main/Router",
                    configuration: {
                        routes: { },
                        loader: [
                            { "routes": "config/routes/AMRoutesConfig" },
                            { "routes": "config/routes/CommonRoutesConfig" },
                            { "routes": "config/routes/UserRoutesConfig" },
                            { "routes": "config/routes/admin/AdminRoutes" },
                            { "routes": "config/routes/admin/RealmsRoutes" },
                            { "routes": "config/routes/user/UMARoutes" }
                        ]
                    }
                },
                {
                    moduleClass: "org/forgerock/commons/ui/common/SiteConfigurator",
                    configuration: {
                        selfRegistration: false,
                        enterprise: false,
                        remoteConfig: true,
                        delegate: "org/forgerock/openam/ui/common/delegates/SiteConfigurationDelegate"
                    }
                },
                {
                    moduleClass: "org/forgerock/commons/ui/common/main/ProcessConfiguration",
                    configuration: {
                        processConfigurationFiles: [
                            "config/process/AMConfig",
                            "config/process/UserConfig",
                            "config/process/CommonConfig"
                        ]
                    }
                },
                {
                    moduleClass: "org/forgerock/commons/ui/common/main/ServiceInvoker",
                    configuration: {
                        defaultHeaders: {
                        }
                    }
                },
                {
                    moduleClass: "org/forgerock/commons/ui/common/main/ErrorsHandler",
                    configuration: {
                        defaultHandlers: {
                        },
                        loader: [
                                { "defaultHandlers": "config/errorhandlers/CommonErrorHandlers" }
                        ]
                    }
                },
                {
                    moduleClass: "org/forgerock/commons/ui/common/util/UIUtils",
                    configuration: {
                        templateUrls: [
                            "templates/admin/views/realms/policies/common/StripedListItemTemplate.html",
                            "templates/admin/views/realms/policies/applications/ApplicationsToolbarTemplate.html",
                            "templates/admin/views/realms/policies/policies/EditPolicyTemplate.html",
                            "templates/admin/views/realms/policies/policies/conditions/EditSubjectTemplate.html",
                            "templates/admin/views/realms/policies/policies/conditions/EditEnvironmentTemplate.html",
                            "templates/admin/views/realms/policies/policies/conditions/OperatorRulesTemplate.html",
                            "templates/admin/views/realms/policies/policies/conditions/ConditionAttrEnum.html",
                            "templates/admin/views/realms/policies/policies/conditions/ConditionAttrString.html",
                            "templates/admin/views/realms/policies/policies/conditions/ConditionAttrBoolean.html",
                            "templates/admin/views/realms/policies/policies/conditions/ConditionAttrArray.html",
                            "templates/admin/views/realms/policies/policies/conditions/ConditionAttrObject.html",
                            "templates/admin/views/realms/policies/policies/conditions/ConditionAttrTime.html",
                            "templates/admin/views/realms/policies/policies/conditions/ConditionAttrDay.html",
                            "templates/admin/views/realms/policies/policies/conditions/ConditionAttrDate.html",
                            "templates/admin/views/realms/policies/policies/conditions/ConditionAttrTimeZone.html",
                            "templates/admin/views/realms/policies/policies/conditions/ListItem.html",
                            "templates/admin/views/realms/policies/policies/conditions/LegacyListItem.html",
                            "templates/admin/views/realms/policies/policies/StripedListActionItemTemplate.html",
                            "templates/admin/views/realms/policies/resourceTypes/ResourceTypesToolbarTemplate.html",
                            "templates/admin/views/realms/scripts/ScriptsToolbarTemplate.html",
                            "templates/admin/views/realms/scripts/ScriptValidationTemplate.html",
                            "templates/admin/views/realms/scripts/ChangeContextTemplate.html",
                            "templates/uma/backgrid/cell/RevokeCell.html",
                            "templates/uma/backgrid/cell/SelectizeCell.html",
                            "templates/user/ConfirmPasswordDialogTemplate.html"
                        ],
                        partialUrls: [
                            "partials/form/_JSONSchemaFooter.html",
                            "partials/headers/_Title.html",
                            "partials/headers/_TitleWithSubAndIcon.html"
                        ]
                    }
                },
                {
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
                },
                {
                    moduleClass: "org/forgerock/commons/ui/common/main/ValidatorsManager",
                    configuration: {
                        policyDelegate: "org/forgerock/openam/ui/common/delegates/PolicyDelegate",
                        validators: { },
                        loader: [
                             {"validators": "config/validators/UserValidators"},
                             {"validators": "config/validators/CommonValidators"}
                         ]
                    }
                },
                {
                    moduleClass: "org/forgerock/commons/ui/common/components/Navigation",
                    configuration: {
                        userBar: [
                            {
                                "id": "profileLink",
                                "href": "#profile/",
                                "i18nKey": "common.user.profile"
                            }, {
                                "id": "changePasswordLink",
                                "event" : Constants.EVENT_SHOW_CHANGE_SECURITY_DIALOG,
                                "i18nKey": "common.user.changePassword"
                            }, {
                                "id": "logoutLink",
                                "href": "#logout/",
                                "i18nKey": "common.form.logout"
                            }
                        ],
                        links: {
                            "admin": {
                                "role": "ui-admin",
                                "urls": {
                                    "realms": {
                                        "url": "#realms",
                                        "name": "config.AppConfiguration.Navigation.links.realms.title",
                                        "icon": "fa fa-cloud",
                                        "dropdown" : true,
                                        "urls": [{
                                            "url": "#realms",
                                            "name": "config.AppConfiguration.Navigation.links.realms.showAll",
                                            "icon": "fa fa-th"
                                        }, {
                                            "event": Constants.EVENT_ADD_NEW_REALM_DIALOG,
                                            "name": "config.AppConfiguration.Navigation.links.realms.newRealm",
                                            "icon": "fa fa-plus"
                                        }, {
                                            divider: true
                                        }]
                                    },
                                    "federation": {
                                        "url": "#federation",
                                        "name": "config.AppConfiguration.Navigation.links.federation",
                                        "icon": "fa fa-building-o"
                                    },
                                    "configuration": {
                                        "url": "#configuration",
                                        "name": "config.AppConfiguration.Navigation.links.configuration",
                                        "icon": "fa fa-cog"
                                    },
                                    "sessions": {
                                        "url": "#sessions",
                                        "name": "config.AppConfiguration.Navigation.links.sessions",
                                        "icon": "fa fa-users"
                                    }
                                }
                            },
                            "user" : {
                                "urls": {
                                    "dashboard": {
                                        "url": "#dashboard/",
                                        "name": "config.AppConfiguration.Navigation.links.dashboard",
                                        "icon": "fa fa-dashboard",
                                        "inactive": false
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
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            ],
            loggerLevel: "debug"
        };
    return obj;
});
