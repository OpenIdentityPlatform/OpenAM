/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2005 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CallBackHelperBase.java,v 1.3 2008/06/25 05:41:24 qcheng Exp $
 *
 */

/**
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.iplanet.am.sdk.common;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMServiceUtils;
import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceConfig;
import java.util.Map;
import java.util.Set;

/**
 * Helper class to re-use common functionality required for CallBack
 * (AMCallBack) implementation by remote and ldap packages.
 */
public class CallBackHelperBase implements AMConstants {

    protected static Debug debug = MiscUtils.getDebugInstance();

    protected static final String EXTERNAL_ATTRIBUTES_FETCH_ENABLED_ATTR = 
        "iplanet-am-admin-console-external-attribute-fetch-enabled";

    protected SSOToken internalToken = MiscUtils.getInternalToken();

    public CallBackHelperBase() {
    }

    protected Set getOrgConfigAttribute(String orgDN, String attrName) {
        // Obtain the ServiceConfig
        try {
            // Get the org config
            ServiceConfig sc = AMServiceUtils.getOrgConfig(internalToken,
                    orgDN, ADMINISTRATION_SERVICE);
            if (sc != null) {
                Map attributes = sc.getAttributes();
                return (Set) attributes.get(attrName);
            } else {
                return getDefaultGlobalConfig(attrName);
            }
        } catch (Exception ee) {
            return getDefaultGlobalConfig(attrName);
        }
    }

    protected Set getDefaultGlobalConfig(String attrName) {
        // Org Config may not exist. Get default values
        if (debug.messageEnabled()) {
            debug.message("CallBackHelper.getPrePostImpls() "
                    + "Organization config for service ("
                    + ADMINISTRATION_SERVICE + "," + attrName
                    + ") not found. Obtaining default service "
                    + "config values ..");
        }
        try {
            Map defaultValues = AMServiceUtils.getServiceConfig(internalToken,
                    ADMINISTRATION_SERVICE, SchemaType.ORGANIZATION);
            if (defaultValues != null) {
                return (Set) defaultValues.get(attrName);
            }
        } catch (Exception e) {
            if (debug.warningEnabled()) {
                debug.warning("CallBackHelper.getPrePostProcessClasses(): "
                        + "Unable to get Pre/Post Processing information", e);
            }
        }
        return null;
    }

    protected Set getPrePostImpls(String orgDN) {
        return getOrgConfigAttribute(orgDN, PRE_POST_PROCESSING_MODULES_ATTR);
    }

    public boolean isExistsPrePostPlugins(String orgDN) {
        Set plugins = getPrePostImpls(orgDN);
        return ((plugins != null) && (!plugins.isEmpty()));
    }

    public boolean isExternalGetAttributesEnabled(String orgDN) {
        Set values = getOrgConfigAttribute(orgDN,
                EXTERNAL_ATTRIBUTES_FETCH_ENABLED_ATTR);

        boolean enabled = false;
        if (values != null && !values.isEmpty()) {
            String val = (String) values.iterator().next();
            enabled = (val.equalsIgnoreCase("true"));
        }

        debug.message("CallBackHelper.isExternalGetAttributeEnabled() = "
                + enabled);

        return enabled;
    }

}
