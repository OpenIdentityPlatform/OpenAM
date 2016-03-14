/*
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
 * Copyright 2016 ForgeRock AS.
 */

 define("org/forgerock/openam/ui/admin/views/global/ListAuthenticationView", [
     "jquery",
     "org/forgerock/openam/ui/admin/views/global/createConfigurationListView",
     "org/forgerock/openam/ui/admin/services/SMSGlobalService"
 ], ($, createConfigurationListView, SMSGlobalService) => {

     const ListAuthenticationView = createConfigurationListView(
         $.t("config.AppConfiguration.Navigation.links.configure.authentication"),
         SMSGlobalService.authentication.getAll,
         "templates/admin/views/global/authentication/ListAuthenticationConfigurationTemplate.html"
     );

     return new ListAuthenticationView();
 });
