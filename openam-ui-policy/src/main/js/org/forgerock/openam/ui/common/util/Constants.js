/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2014 ForgeRock AS. All rights reserved.
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

/*global define, location */

define("org/forgerock/openam/ui/common/util/Constants", [
    "org/forgerock/commons/ui/common/util/Constants"
], function (commonConstants) {
    var context = location.pathname.substring(1,location.pathname.indexOf('policyEditor')-1);
    commonConstants.context = context;
    commonConstants.THEME_CONFIG_PATH = 'themeConfig.json';
    commonConstants.CONSOLE_PATH = '/' + commonConstants.context + '/console';
    commonConstants.CONSOLE_USERS = ['amadmin']; 
    commonConstants.OPENAM_HEADER_PARAM_CUR_PASSWORD = "currentpassword";
    commonConstants.OPENAM_STORAGE_KEY_PREFIX = "FR-OpenAM-";
    commonConstants.EVENT_RETURN_TO_AM_CONSOLE = "EVENT_RETURN_TO_AM_CONSOLE";

    return commonConstants;
});
