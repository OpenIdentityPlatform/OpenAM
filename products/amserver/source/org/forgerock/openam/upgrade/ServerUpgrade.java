/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 ForgeRock AS. All Rights Reserved
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
 *
 */

package org.forgerock.openam.upgrade;

import com.sun.identity.shared.Constants;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;

/**
 *
 * @author steve
 */
public class ServerUpgrade {
    private static Map<String, UpgradeHelper> serviceHelpers;
    private static final String SERVER_UPGRADE = "serverupgrade";
    private static final String ATTR_UPGRADE_HELPER = "upgrade.helper";
    private static final String ATTR_DEFAULT_UPGRADE = "defaults.to.upgrade";
    private static final String SERVICES_TO_DELETE = "services.to.delete";
    private static ResourceBundle res = null;
    private static UpgradeException initializationException = null;
    
    static {
        loadResourceBundle();
        populateUpgradeHelpers();
    }
    
    protected static void loadResourceBundle() {
        res = ResourceBundle.getBundle(SERVER_UPGRADE);
    }
    
    public static Set<String> getAttrsToUpgrade() 
    throws UpgradeException {
        assertInitialized();
        Set<String> values = new HashSet<String>();
            
        if (!res.containsKey(ATTR_DEFAULT_UPGRADE)) {
            throw new UpgradeException("Unable to find " + ATTR_DEFAULT_UPGRADE + " in " + SERVER_UPGRADE);
        }
        
        String attrValues = res.getString(ATTR_DEFAULT_UPGRADE);
        
        if (attrValues != null) {
            StringTokenizer st = new StringTokenizer(attrValues, ",");
            
            while (st.hasMoreTokens()) {
                values.add(st.nextToken());
            }
        }
        
        return values;
    }
    
    public static Set<String> getServicesToDelete()
    throws UpgradeException {
        assertInitialized();
        Set<String> values = new HashSet<String>();
        
        if (!res.containsKey(SERVICES_TO_DELETE)) {
            throw new UpgradeException("Unable to find " + SERVICES_TO_DELETE + " in " + SERVER_UPGRADE);
        }
        
        String attrValues = res.getString(SERVICES_TO_DELETE);
        
        if (attrValues != null) {
            StringTokenizer st = new StringTokenizer(attrValues, ",");
            
            while (st.hasMoreTokens()) {
                values.add(st.nextToken());
            }
        }
        
        return values;
    }
    
    /**
     * Returns the upgrade helper for a given service name or null if none is
     * configured.
     * 
     * @param serviceName The service name for which a service upgrade helper should be returned
     * @return The service upgrade helper if available
     * @throws UpgradeException If the resource bundle is missing
     */
    public static UpgradeHelper getServiceHelper(String serviceName) 
    throws UpgradeException {
        assertInitialized();
        
        return serviceHelpers.get(serviceName);
    }
    
    protected static void populateUpgradeHelpers() {
        Map<String, UpgradeHelper> values = new HashMap<String, UpgradeHelper>();            
        String attrValues = res.getString(ATTR_UPGRADE_HELPER);
        
        if (attrValues != null) {
            StringTokenizer st = new StringTokenizer(attrValues, Constants.COMMA);
            
            while (st.hasMoreTokens()) {
                String serviceHelper = st.nextToken();
                
                if (serviceHelper != null) {
                    if (serviceHelper.indexOf(Constants.EQUALS) == -1) {
                        // bad formatting
                        continue;
                    }
                    
                    String serviceName = serviceHelper.substring(0, serviceHelper.indexOf(Constants.EQUALS));
                    String helperClass = serviceHelper.substring(serviceHelper.indexOf(Constants.EQUALS) + 1);
                    UpgradeHelper helper = null;
                    
                    try {
                        helper = (UpgradeHelper) Class.forName(helperClass).newInstance();
                        values.put(serviceName, helper);
                    } catch (Exception ex) {
                        UpgradeUtils.debug.error("Unable to load helper class: " + helperClass, ex);
                        initializationException = 
                                new UpgradeException("Unable to load helper class: " + helperClass + ex.getMessage());
                    }
                }
            }
        }
        
        if (UpgradeUtils.debug.messageEnabled()) {
            UpgradeUtils.debug.message("Helper classes: " + values);
        }
        
        serviceHelpers = values;
    }
    
    protected static void assertInitialized() 
    throws UpgradeException {
        if (initializationException != null) {
            throw initializationException;
        }
    }
}

