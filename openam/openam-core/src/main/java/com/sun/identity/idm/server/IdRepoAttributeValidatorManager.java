/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
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
 * $Id: IdRepoAttributeValidatorManager.java,v 1.2 2010/01/26 00:04:38 hengming Exp $
 */
package com.sun.identity.idm.server;

import java.security.AccessController;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.IdConstants;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceConfig;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceListener;


/**
 * The <code>IdRepoAttributeValidatorManager</code> class manages
 * <code>IdRepoAttributeValidator</code> for realm.
 */
public class IdRepoAttributeValidatorManager implements ServiceListener {
    static final String ATTR_IDREPO_ATTRIBUTE_VALIDATOR =
        "sunIdRepoAttributeValidator";
    static IdRepoAttributeValidatorManager instance = null;
    static Debug debug = Debug.getInstance("amIdm");
    static boolean initializedListeners;
    static ServiceConfigManager idRepoServiceConfigManager;
    static Map<String, IdRepoAttributeValidator> validatorCache = new HashMap();

    private IdRepoAttributeValidatorManager() {
        // Initialize listeners
        if (debug.messageEnabled()) {
            debug.message("IdRepoAttributeValidatorManager: " +
            "constructor called");
        }
        initializeListeners();
    }

    /**
     * Returns an instance of <code>IdRepoAttributeValidatorManager</code>
     * @return an instance of <code>IdRepoAttributeValidatorManager</code>
     */
    public static IdRepoAttributeValidatorManager getInstance() {
        if (instance == null) {
            synchronized (debug) {
                if (instance == null) {
                    instance = new IdRepoAttributeValidatorManager();
                }
            }
        }
        return instance;
    }

    /**
     * Returns an instance of <code>IdRepoAttributeValidator</code> for 
     * specified realm.
     * @param realm the realm
     * @return an instance of <code>IdRepoAttributeValidator</code>
     * @throws IdRepoException if there are repository related error conditions.
     */
    public IdRepoAttributeValidator getIdRepoAttributeValidator(String realm)
        throws IdRepoException {

        IdRepoAttributeValidator validator = validatorCache.get(realm);
        if (validator != null) {
            return validator;
        }

        Map<String, Set<String>> configParams = new HashMap();
        synchronized (validatorCache) {
            try {
                ServiceConfig orgConfig =
                    idRepoServiceConfigManager.getOrganizationConfig(realm,
                    null);
                Map<String, Set<String>> attrMap =
                    orgConfig.getAttributesForRead();

                Set<String> attrValues = 
                    attrMap.get(ATTR_IDREPO_ATTRIBUTE_VALIDATOR);
                String className = null;
                for(String attrValue: attrValues) {
                    int index = attrValue.indexOf("=");
                    if (index != -1) {
                        String name = attrValue.substring(0, index).trim();
                        String value = attrValue.substring(index + 1).trim();
                        if (name.equals("class")) {
                            className = value;
                        } else {
                            Set<String> values = configParams.get(name);
                            if (values == null) {
                                values = new HashSet();
                                configParams.put(name, values);
                            }
                            values.add(value);
                        }
                    }
                }
                Class validatorClass = Class.forName(className);
                validator = (IdRepoAttributeValidator)
                    validatorClass.newInstance();
            } catch (Exception ex) {
                if (debug.warningEnabled()) {
                    debug.warning("IdRepoAttributeValidatorManager." +
                        "initializeListeners:", ex);
                }
            }

            if (validator == null) {
                validator = new IdRepoAttributeValidatorImpl();
            }
            validator.initialize(configParams);
        }

        return validator;
    }

    private void initializeListeners() {
        // Add listeners to Service Schema and Config Managers
        if (debug.messageEnabled()) {
            debug.message("IdRepoAttributeValidatorManager." +
                "initializeListeners: setting up ServiceListener");
        }
        SSOToken adminToken = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());

        try {
            // Initialize schema objects
            idRepoServiceConfigManager = new ServiceConfigManager(
                adminToken, IdConstants.REPO_SERVICE, "1.0");
            idRepoServiceConfigManager.addListener(this);

        } catch (SMSException smse) {
            debug.error("IdRepoAttributeValidatorManager.initializeListeners:",
                smse);
        } catch (SSOException ssoe) {
            debug.error("IdRepoAttributeValidatorManager.initializeListeners:",
                ssoe);
        }
    }

    /**
     * Notification for global config changes to IdRepoService
     */
    public void globalConfigChanged(String serviceName, String version,
        String groupName, String serviceComponent, int type) {
    }

    /**
     * Notification for organization config changes to IdRepoService
     */
    public void organizationConfigChanged(String serviceName, String version,
        String orgName, String groupName, String serviceComponent, int type) {
    }

    /**
     * Notification for schema changes to IdRepoService
     */
    public void schemaChanged(String serviceName, String version) {
        if (debug.messageEnabled()) {
            debug.message("IdRepoAttributeValidatorManager.schemaChanged: " +
                "Service name = " + serviceName);
        }
        synchronized(validatorCache) {
            validatorCache.clear();
        }
    }
}
