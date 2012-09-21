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
 * $Id: DCTreeServicesHelper.java,v 1.3 2008/06/25 05:41:24 qcheng Exp $
 *
 */

package com.iplanet.am.sdk.common;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.am.sdk.AMException;
import com.iplanet.am.sdk.AMSDKBundle;
import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceSchema;
import com.sun.identity.sm.ServiceSchemaManager;
import java.util.Map;
import java.util.Set;

public class DCTreeServicesHelper implements AMConstants {

    protected static final String DCTREE_START_DN = SystemProperties
            .get("com.iplanet.am.domaincomponent");

    protected static final String IPLANET_DOMAIN_NAME_ATTR = 
        "sunPreferredDomain";

    protected static final String DOMAIN_BASE_DN = 
        "inetDomainBaseDN";

    protected static final String INET_CANONICAL_DOMAIN = 
        "inetcanonicaldomainname";

    private static Debug debug = MiscUtils.debug;

    protected ServiceSchema adminServiceGlobalSchema;

    protected boolean isInitialized = false;

    /**
     * Public default constructor
     * 
     */
    public DCTreeServicesHelper() {
    }

    public void initialize() throws AMException, SSOException {
        try {
            ServiceSchemaManager scm = new ServiceSchemaManager(
                    ADMINISTRATION_SERVICE, MiscUtils.getInternalToken());
            adminServiceGlobalSchema = scm.getGlobalSchema();
            isInitialized = true;
        } catch (SMSException ex) {
            debug.error(AMSDKBundle.getString("354"), ex);
            throw new AMException(AMSDKBundle.getString("354"), "354");
        }
    }

    protected boolean isInitalized() {
        return isInitialized;
    }

    protected ServiceSchema getAdminServiceGlobalSchema() {
        return adminServiceGlobalSchema;
    }

    /**
     * Method to determine if DC Tree support is required or not.
     * 
     * @return true if DC Tree support required, false otherwise
     */
    public boolean isRequired() throws AMException, SSOException {

        if (!isInitalized()) {
            initialize();
        }

        Map attrMap = getAdminServiceGlobalSchema().getAttributeDefaults();
        Set values = (Set) attrMap.get(DCT_ENABLED_ATTR);
        boolean required = false;

        if (values == null || values.isEmpty()) {
            required = false;
        } else {
            String val = (String) values.iterator().next();
            required = (val.equalsIgnoreCase("true"));
        }

        return required;
    }
}
