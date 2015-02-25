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
 * Copyright 2014 ForgeRock AS.
 */

package com.sun.identity.authentication.service;

import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.config.AMAuthenticationManager;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.ChoiceValues;
import com.sun.identity.sm.SMSEntry;

import java.security.AccessController;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * The class determines all configured Module Instances for realm
 * This list is computed per realm.
 */
public class AllConfiguredModuleInstances extends ChoiceValues {

    /**
     * Creates <code>ConfiguredModuleInstances</code> object.
     * Default constructor that will be used by the SMS
     * to create an instance of this class
     */
    public AllConfiguredModuleInstances() {
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

        Map<String, String> answer = new HashMap<String, String>();
        try {
            SSOToken adminToken = AccessController.doPrivileged(AdminTokenAction.getInstance());
            AMAuthenticationManager amAM = new AMAuthenticationManager(adminToken, orgDN);
            Set<String> instanceNames = amAM.getAllowedModuleNames();
            for (String config : instanceNames) {
                answer.put(config, config);
            }
        } catch (Exception e) {
            // do nothing as instanceNames will be empty.
        }
        return answer;
    }

}
