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
 * $Id: SMServiceListener.java,v 1.6 2009/10/01 00:18:33 veiming Exp $
 *
 */

/*
 * Portions Copyrighted 2011-2013 ForgeRock AS
 */
package com.sun.identity.delegation;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.sm.SMSException;
import com.sun.identity.sm.ServiceListener;
import com.sun.identity.common.GeneralTaskRunnable;
import com.sun.identity.common.SystemTimer;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.ServiceConfigManager;
import com.sun.identity.sm.ServiceManager;
import com.sun.identity.authentication.util.ISAuthConstants;
import java.security.AccessController;

/**
 * Service Configuration listener class to clean the delegation polcies
 * when realms are removed.
 */
public class SMServiceListener implements ServiceListener {
    
    private static SMServiceListener serviceListener;
    private String listenerId;
    private Debug debug = DelegationManager.debug;
    
    // private constructor
    private SMServiceListener() {
        // do nothing
    }
    
    public static SMServiceListener getInstance() {
        if (serviceListener == null) {
            serviceListener = new SMServiceListener();
        }
        return (serviceListener);
    }
    
    public void registerForNotifications() {
        if (listenerId != null) {
            // Listener already registered
            return;
        }
        SSOToken token = (SSOToken) AccessController.doPrivileged(
            AdminTokenAction.getInstance());
        try {
            // Try Delegation Service, present only in OpenSSO
            // Since Delegation Service is being added only in OpenSSO
            // check for its presense in root realm. If not present,
            // it is an upgrade from AM 7.1 and use AuthService
            ServiceConfigManager scm = null;
            try {
                scm = new ServiceConfigManager(
                    DelegationManager.DELEGATION_SERVICE, token);
                if (scm.getOrganizationConfig("/", null) == null) {
                    // Delegation servier does not exisit for realm
                    // Default to auth service
                    scm = null;
                }
            } catch (SMSException smse) {
                // Ignore exception and continue with Auth Service
            }
            if (scm == null) {
                // Delegation Service not found, use Auth service
                scm = new ServiceConfigManager(
                    ISAuthConstants.AUTH_SERVICE_NAME, token);
            }
            listenerId = scm.addListener(this);
        } catch (SMSException ex) {
            debug.error("Unable to register SMS notification for Delegation",
                ex);
        } catch (SSOException ex) {
            debug.error("Unable to register SMS notification for Delegation",
                ex);
        }
    }

    public void schemaChanged(String serviceName, String version) {
        // do nothing
    }

    public void globalConfigChanged(String serviceName, String version,
        String groupName, String serviceComponent, int type) {
        // do nothing
    }

    public void organizationConfigChanged(String serviceName, String version,
        String orgName, String groupName, String serviceComponent, int type) {
        // If event type is delete,
        // remove realm privileges for the organization
        if ((serviceComponent == null) || (serviceComponent.length() == 0)) {
            if (type == ServiceListener.REMOVED) {
                // Schedule the task to delete delegation policies
                DeleteDelegationPolicyTask task =
                    new DeleteDelegationPolicyTask(orgName);
                if (debug.messageEnabled()) {
                    debug.message("SMServiceListener.occ scheduling " +
                        "policies to be removed for org: " + orgName +
                        " GN: " + groupName + " SC: " + serviceComponent);
                }
                SystemTimer.getTimer().schedule(task, 0);
            } else if (type == ServiceListener.ADDED) {
                // Create the delegation policies
                // OPENAM-3226
                // delegation policies are now created in OrganizationConfigManager
                // to avoid datastore replication conflicts
            }
        }
    }
    
    private class DeleteDelegationPolicyTask extends GeneralTaskRunnable {
        
        private String realm;
        
        private DeleteDelegationPolicyTask(String realm) {
            this.realm = realm;
        }

        public void run() {
            // Check if the realm exists, if not delete the delegation rules
            SSOToken token = (SSOToken) AccessController.doPrivileged(
                AdminTokenAction.getInstance());
            try {
                // Throws SMSException is realm does not exist
                new OrganizationConfigManager(token, realm);
            } catch (SMSException e) {
                try {
                    // Realm not present, remove the delegation policies
                    if (debug.messageEnabled()) {
                        debug.message("Deleting delegation privilegs for " +
                            "realm" + realm);
                    }
                    DelegationUtils.deleteRealmPrivileges(token, realm);
                } catch (SSOException ex) {
                    if (debug.messageEnabled()) {
                        debug.message("Error deleting delegation privilegs " +
                            "for realm" + realm, ex);
                    }
                } catch (DelegationException ex) {
                    if (debug.messageEnabled()) {
                        debug.message("Error deleting delegation privilegs " +
                            "for realm" + realm, ex);
                    }
                }
            }
        }

        public boolean addElement(Object key) {
            return false;
        }

        public boolean removeElement(Object key) {
            return false;
        }

        public boolean isEmpty() {
            return true;
        }

        public long getRunPeriod() {
            return -1;
        }
    }
}
