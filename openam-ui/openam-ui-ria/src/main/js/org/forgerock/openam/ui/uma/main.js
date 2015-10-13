/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 2015 ForgeRock AS.
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


define([
    "./models/UMAPolicy",
    "./models/UMAPolicyPermission",
    "./models/UMAPolicyPermissionScope",
    "./models/UMAResourceSetWithPolicy",
    "./models/User",

    "./views/request/ListRequest",
    "./views/request/EditRequest",
    "./views/backgrid/cells/PermissionsCell",
    "./views/history/ListHistory",

    "./views/resource/BasePage",
    "./views/resource/LabelTreeNavigationView",
    "./views/resource/MyLabelsPage",
    "./views/resource/MyResourcesPage",
    "./views/resource/ResourcePage",
    "./views/resource/SharedWithMePage",
    "./views/resource/StarredPage",

    "./views/share/BaseShare",
    "./views/share/CommonShare",
    "./views/share/ShareCounter"
]);
