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
 * Copyright 2015 ForgeRock AS.
 */

/*global define*/
define([
    "./delegates/SMSDelegate",
    "./models/Form",
    "./models/FormCollection",
    "./utils/FormHelper",
    "./utils/JsonEditorTheme",

    "./views/commonTasks/CommonTasksView",

    "./views/configuration/ConfigurationView",

    "./views/federation/FederationView",

    "./views/realms/agents/AgentsView",
    "./views/realms/authentication/AdvancedView",
    "./views/realms/authentication/ChainsView",
    "./views/realms/authentication/ModulesView",
    "./views/realms/authentication/SettingsView",
    "./views/realms/dataStores/DataStoresView",
    "./views/realms/general/GeneralView",
    "./views/realms/policies/PoliciesView",
    "./views/realms/privileges/PrivilegesView",
    "./views/realms/scripts/ScriptsView",
    "./views/realms/services/ServicesView",
    "./views/realms/sts/STSView",
    "./views/realms/subjects/SubjectsView",

    "./views/realms/RealmsListView",
    "./views/realms/RealmView",

    "./views/sessions/SessionsView"
]);
