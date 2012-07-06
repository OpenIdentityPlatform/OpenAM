/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SessionSiteNames.java,v 1.2 2008/06/25 05:42:28 qcheng Exp $
 *
 */

package com.sun.identity.common.configuration;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.setup.SetupConstants;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import java.security.AccessController;
import java.util.Collections;
import java.util.Set;

/**
 * This class provides methods for getting site names.
 */
public class SessionSiteNames implements ISubConfigNames {
    private static final String SESSION_SVC_NAME = "iPlanetAMSessionService";
    private static final String SUBSCHEMA_SITE = "Site";
    private Debug debug = Debug.getInstance(SetupConstants.DEBUG_NAME);
   
    /**
     * Returns the possible values for site names.
     *
     * @return possible values for site names.
     */
    public Set getNames() {
        Set names = Collections.EMPTY_SET;
        try {
            SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
            Set possibleNames = SiteConfiguration.getSites(adminToken);
            
            if ((possibleNames != null) && !possibleNames.isEmpty()) {
                Set existingNames = getExistingNames(adminToken);
                if (existingNames != null) {
                    possibleNames.removeAll(existingNames);
                }
                names = possibleNames;
            }
        } catch (SMSException ex) {
            debug.error("SessionSiteNames.getNames", ex);
        } catch (SSOException ex) {
            debug.error("SessionSiteNames.getNames", ex);
        }
        return names;
    }
    
    private Set getExistingNames(SSOToken adminToken) {
        try {
            ServiceConfigManager scm = new ServiceConfigManager(
                SESSION_SVC_NAME, adminToken);
            ServiceConfig serviceConfig = scm.getGlobalConfig(null);
            return serviceConfig.getSubConfigNames("*", SUBSCHEMA_SITE);
        } catch (SMSException ex) {
            debug.error("SessionSiteNames.getExistingNames", ex);
        } catch (SSOException ex) {
            debug.error("SessionSiteNames.getExistingNames", ex);
        }
        return null;
    }
}
