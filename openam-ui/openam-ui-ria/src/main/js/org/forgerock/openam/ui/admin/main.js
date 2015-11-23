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

define([
    "./delegates/SMSDelegateUtils",
    "./delegates/SMSGlobalDelegate",
    "./delegates/SMSRealmDelegate",

    "./models/Form",
    "./models/FormCollection",

    "./utils/AdministeredRealmsHelper",
    "./utils/FormHelper",
    "./utils/JSONEditorTheme",
    "./utils/RedirectToLegacyConsole",

    "./views/realms/agents/AgentsView",

    "./views/realms/authentication/chains/CriteriaView",
    "./views/realms/authentication/chains/EditChainView",
    "./views/realms/authentication/chains/EditLinkView",
    "./views/realms/authentication/chains/LinkView",
    "./views/realms/authentication/chains/LinkInfoView",
    "./views/realms/authentication/chains/PostProcessView",
    "./views/realms/authentication/ChainsView",
    "./views/realms/authentication/ModulesView",
    "./views/realms/authentication/modules/EditModuleView",
    "./views/realms/authentication/SettingsView",
    "./views/realms/dashboard/DashboardView",
    "./views/realms/dashboard/DashboardTasksView",
    "./views/realms/dataStores/DataStoresView",

    "./views/realms/authorization/common/AbstractListView",

    "./views/realms/authorization/policies/EditPolicyView",

    "./views/realms/authorization/policySets/PolicySetsView",
    "./views/realms/authorization/policySets/EditPolicySetView",

    "./views/realms/authorization/resourceTypes/ResourceTypesView",
    "./views/realms/authorization/resourceTypes/EditResourceTypeView",

    "./views/realms/privileges/PrivilegesView",

    "./views/realms/scripts/EditScriptView",
    "./views/realms/scripts/ScriptsView",

    "./views/realms/sts/STSView",

    "./views/realms/subjects/SubjectsView",

    "./views/realms/RealmsListView",
    "./views/realms/RealmTreeNavigationView",
    "./views/realms/CreateUpdateRealmDialog"

]);
