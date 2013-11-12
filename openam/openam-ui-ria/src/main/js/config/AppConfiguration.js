/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 ForgeRock AS. All rights reserved.
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

/**
 * @author yaromin
 */
define("config/AppConfiguration", [
    "org/forgerock/commons/ui/common/util/Constants", 
    "org/forgerock/commons/ui/common/main/EventManager"
], function(constants, eventManager) {
    var obj = {
            moduleDefinition: [
               {
                   moduleClass: "org/forgerock/commons/ui/common/main/SessionManager",
                   configuration: {
                       loginHelperClass: "org/forgerock/openam/ui/user/login/RESTLoginHelper"
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/user/SiteConfigurator",
                   configuration: {
                       selfRegistration: false,
                       enterprise: false
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/common/main/ProcessConfiguration",
                   configuration: {
                       processConfigurationFiles: [
                           "config/process/UserConfig",
                           "config/process/CommonConfig"
                       ]
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/common/main/Router",
                   configuration: {
                       routes: {
                       },
                       loader: [
                                {"routes":"config/routes/CommonRoutesConfig"}, 
                                {"routes":"config/routes/AMRoutesConfig"}, 
                                {"routes":"config/routes/UserRoutesConfig"}
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
                                {"defaultHandlers":"config/errorhandlers/CommonErrorHandlers"}
                       ]
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/common/components/Navigation",
                   configuration: {
                       links: {                          
                           "user" : {
                               "urls": {
                                   "dashboard": {
                                       "url": "#dashboard/",
                                       "name": "config.AppConfiguration.Navigation.links.dashboard"
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
                       ]
                   } 
               },               
               {
                   moduleClass: "org/forgerock/commons/ui/common/components/Messages",
                   configuration: {
                       messages: {
                       },
                       loader: [
                                {"messages":"config/messages/UserMessages"}
                       ]
                   } 
               },
               {
                   moduleClass: "org/forgerock/commons/ui/common/main/ValidatorsManager",
                   configuration: {
                       validators: {
                       },
                       loader: [
                                {"validators":"config/validators/UserValidators"},
                                {"validators":"config/validators/CommonValidators"}
                       ]
                   } 
               },
               {
                    moduleClass: "org/forgerock/commons/ui/common/components/Navigation",
                    configuration: {
                        links: {
                            "user" : {
                                "urls": {
                                    "dashboard": {
                                        "url": "#dashboard/",
                                        "name": "config.AppConfiguration.Navigation.links.dashboard"
                                    }/*,
                                    "oauth2": {
                                        "url": "#oauth2/tokens",
                                        "name": "config.AppConfiguration.Navigation.links.oauthtokens"
                                    }*/
                                }
                            }
                        }
                    }
               }
               ],
               loggerLevel: 'debug'
    };
    return obj;
});
