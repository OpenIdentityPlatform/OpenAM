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
 * $Id: ConfiguredModuleInstances.java,v 1.4 2008/06/25 05:42:04 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.authentication.service;

import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.shared.Constants;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.sm.ChoiceValues;
import com.sun.identity.sm.SMSEntry;

/**
 * The class determines the configured LDAP/DataStore Module Instances for realm
 * This list is computed per realm.
 */
public class ConfiguredModuleInstances extends ChoiceValues {
    
    /**
     * Creates <code>ConfiguredModuleInstances</code> object.
     * Default constructor that will be used by the SMS
     * to create an instance of this class
     */
    public ConfiguredModuleInstances() {
        // do nothing
    }
    
    /**
     * Returns the choice values and their corresponding I18N keys for top 
     * organization.
     * @return the map of choice values.
     */
    public Map getChoiceValues() {
        return getChoiceValues(Collections.EMPTY_MAP);
    }
    
    /**
     * Returns the map of choice values for given environment params.
     * @param envParams to get the map of choice values
     * @return the map of choice values for given environment params.
     */
    public Map getChoiceValues(Map envParams) {
        
        String orgDN = null;
        if (envParams != null) {
            orgDN = (String)envParams.get(Constants.ORGANIZATION_NAME);
        }
        if (orgDN == null || orgDN.length() == 0) {
            orgDN = SMSEntry.getRootSuffix();
        }

        Map answer = new HashMap();
        try {
            getInstanceNames(orgDN,"LDAP", answer);
            getInstanceNames(orgDN,"DataStore", answer);
            getInstanceNames(orgDN,"AD", answer);
            getInstanceNames(orgDN,"Anonymous", answer);
            getInstanceNames(orgDN,"JDBC", answer);
        } catch (Exception e) {
            // do nothing as instanceNames will be empty.
        }
        //return the choice values map
        return (answer);
    }
    
    private void getInstanceNames(String orgDN, String moduleType, Map answer) {
        Set instanceNames = null;
        try {
            SSOToken adminToken = (SSOToken)AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            AMAuthenticationManager amAM = new
                AMAuthenticationManager(adminToken, orgDN);
            instanceNames = amAM.getModuleInstanceNames(moduleType);
            if (instanceNames != null && !instanceNames.isEmpty()) {
                for (Iterator it = instanceNames.iterator(); it.hasNext(); ) {
                    String config = (String) it.next();
                    answer.put(config, config);
                }
            }
        } catch (Exception exp) {
            // Do nothing
        }
    }
}
