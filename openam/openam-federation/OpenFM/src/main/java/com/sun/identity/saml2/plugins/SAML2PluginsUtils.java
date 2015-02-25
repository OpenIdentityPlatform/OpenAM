/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: SAML2PluginsUtils.java,v 1.1 2008/07/08 23:03:34 hengming Exp $
 */

package com.sun.identity.saml2.plugins;

import java.util.Map;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;

/**
 * The <code>SAML2PluginsUtils</code> contains utility methods for SAML 2.0
 * plugins classes.
 */
public class SAML2PluginsUtils {

    /**
     * Checks if dynamical profile creation or ignore profile is enabled.
     * @param realm realm to check the dynamical profile creation attributes.
     * @return true if dynamical profile creation or ignore profile is enabled,
     * false otherwise.
     */
    public static boolean isDynamicalOrIgnoredProfile(String realm) {
        try {
            OrganizationConfigManager orgConfigMgr = AuthD.getAuth().
                getOrgConfigManager(realm);
            ServiceConfig svcConfig = orgConfigMgr.getServiceConfig(
                ISAuthConstants.AUTH_SERVICE_NAME);
            Map attrs = svcConfig.getAttributes();
            String tmp = CollectionHelper.getMapAttr(
                attrs, ISAuthConstants.DYNAMIC_PROFILE);
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "SAML2PluginsUtils.isDynamicalOrIgnoredProfile: attr=" +
                    tmp);
            }
            return ((tmp != null) && (tmp.equalsIgnoreCase("createAlias") ||
                tmp.equalsIgnoreCase("true") ||
                tmp.equalsIgnoreCase("ignore")));
        } catch (Exception e) {
            SAML2Utils.debug.error("SAML2PluginsUtils." +
                "isDynamicalOrIgnoredProfile: unable to get attribute", e);
            return false;
        }
    }

}
