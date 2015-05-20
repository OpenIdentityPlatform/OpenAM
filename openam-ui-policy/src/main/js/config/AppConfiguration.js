/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2014-2015 ForgeRock AS.
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

/*global define*/

define("config/AppConfiguration", [
    "org/forgerock/commons/ui/common/util/Constants"
], function (Constants) {
    return {
        moduleDefinition: [
            {
                moduleClass: "org/forgerock/commons/ui/common/main/SessionManager",
                configuration: {
                    loginHelperClass: "org/forgerock/openam/ui/policy/login/LoginHelper"
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/main/GenericRouteInterfaceMap",
                configuration: {
                    LoginView: "org/forgerock/openam/ui/policy/login/LoginView",
                    LoginDialog: "org/forgerock/openam/ui/policy/login/LoginDialog"
                }
            },

            {
                moduleClass: "org/forgerock/commons/ui/common/SiteConfigurator",
                configuration: {
                    remoteConfig: true,
                    delegate: "org/forgerock/openam/ui/policy/delegates/SiteConfigurationDelegate"
                }
            },

            {
                moduleClass: "org/forgerock/commons/ui/common/main/ProcessConfiguration",
                configuration: {
                    processConfigurationFiles: [
                        "config/process/CommonConfig",
                        "config/process/PolicyConfig"
                    ]
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/main/Router",
                configuration: {
                    routes: {
                    },
                    loader: [
                        {"routes": "config/routes/CommonRoutesConfig"},
                        {"routes": "config/routes/PolicyRoutesConfig"}
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
                        {"defaultHandlers": "config/errorhandlers/CommonErrorHandlers"}
                    ]
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/components/Navigation",
                configuration: {
                    links: {
                        "admin": {
                            "role": "ui-admin",
                            "urls": {
                                "policies": {
                                    "baseUrl":"/",
                                    "url": "#apps/",
                                    "icon": "fa fa-briefcase",
                                    "name": "config.AppConfiguration.Navigation.links.policyEditor",
                                    "urls": {
                                        "applications": {
                                            "url": "#apps/",
                                            "name": "config.AppConfiguration.Navigation.links.applications"
                                        },
                                        "resourceTypes": {
                                            "url": "#resourceTypes/",
                                            "name": "config.AppConfiguration.Navigation.links.resourceTypes"
                                        }
                                    }
                                },
                                "scripts": {
                                    "event": Constants.EVENT_GO_TO_SCRIPTS_EDITOR,
                                    "name": "config.AppConfiguration.Navigation.links.scriptsEditor",
                                    "icon": "fa fa-code"
                                },
                                "console": {
                                    "event": Constants.EVENT_RETURN_TO_AM_CONSOLE,
                                    "name": "config.AppConfiguration.Navigation.links.console",
                                    "icon": "fa fa-cubes"
                                }
                            }
                        }
                    }
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/util/UIUtils",
                configuration: {
                    templateUrls: [
                        "templates/policy/policies/conditions/EditSubjectTemplate.html",
                        "templates/policy/policies/conditions/EditEnvironmentTemplate.html",
                        "templates/policy/policies/conditions/OperatorRulesTemplate.html",
                        "templates/policy/policies/conditions/ConditionAttrEnum.html",
                        "templates/policy/policies/conditions/ConditionAttrString.html",
                        "templates/policy/policies/conditions/ConditionAttrBoolean.html",
                        "templates/policy/policies/conditions/ConditionAttrArray.html",
                        "templates/policy/policies/conditions/ConditionAttrObject.html",
                        "templates/policy/policies/conditions/ConditionAttrTime.html",
                        "templates/policy/policies/conditions/ConditionAttrDay.html",
                        "templates/policy/policies/conditions/ConditionAttrDate.html",
                        "templates/policy/policies/conditions/ConditionAttrTimeZone.html",
                        "templates/policy/policies/conditions/ListItem.html",
                        "templates/policy/policies/conditions/LegacyListItem.html",
                        "templates/policy/policies/StripedListActionItemTemplate.html",
                        "templates/policy/policies/PoliciesListToolbarTemplate.html",

                        "templates/policy/applications/ApplicationsListActionsCellTemplate.html",
                        "templates/policy/applications/ApplicationsListToolbarTemplate.html",

                        "templates/policy/resourcetypes/ResourceTypesListToolbarTemplate.html",

                        "templates/policy/common/StripedListItemTemplate.html"
                    ]
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/components/Messages",
                configuration: {
                    messages: {
                    },
                    loader: [
                        {"messages": "config/messages/PolicyMessages"},
                        {"messages": "config/messages/CommonMessages"}
                    ]
                }
            },
            {
                moduleClass: "org/forgerock/commons/ui/common/main/ValidatorsManager",
                configuration: {
                    validators: { },
                    loader: [
                        {"validators": "config/validators/CommonValidators"}
                    ]
                }
            }
        ],
        loggerLevel: 'debug'
    };
});
