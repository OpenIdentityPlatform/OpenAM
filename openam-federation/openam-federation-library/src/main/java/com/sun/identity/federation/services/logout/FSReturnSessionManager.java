/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: FSReturnSessionManager.java,v 1.4 2009/01/28 05:35:07 ww203982 Exp $
 *
 * Portions Copyright 2015 ForgeRock AS.
 */

package com.sun.identity.federation.services.logout;

import java.util.HashMap;
import java.util.Map;

import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import org.forgerock.openam.ldap.LDAPUtils;
import org.forgerock.opendj.ldap.DN;

/**
 * Contains session information for logout.
 */
public final class FSReturnSessionManager{
    
    private static Map instanceMap = new HashMap();
    private Map userAndProviderMap = new HashMap();
    
    private FSReturnSessionManager() {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSReturnSessionManager(): Called." +
                " A new instance of FSReturnSessionManager created");
        }
    }
    
    /**
     * Returns the provider info where logout was initiated for
     * a certain userDN. This is so that control of flow can be returned back to
     * that provider to display status page of that provider.
     * @param userDN user whose provider information is to be retrieved
     * @return HashMap  containing <code>providerId</code>, 
     *  <code>relaystate</code>, <code>sessionIndex</code>, etc.
     */
    public HashMap getUserProviderInfo(String userDN){
        FSUtils.debug.message("FSReturnSessionManager::getUserProviderInfo");
        userDN = DN.valueOf(userDN).toString().toLowerCase();
        return (HashMap)userAndProviderMap.get(userDN);
    }
    
    /**
     * Set logout status so that control of flow can be returned back to
     * that provider to display status page of that provider.
     * @param logoutStatus logout status to be saved
     * @param userDN user whose provider information is to be retrieved
     */
    public void setLogoutStatus(String logoutStatus,String userDN){
        FSUtils.debug.message("FSReturnSessionManager::setLogoutStatus");
        userDN = LDAPUtils.formatToRFC(userDN);
        HashMap userMap =  (HashMap)userAndProviderMap.get(userDN);
        if (userMap != null) {
            userMap.remove(IFSConstants.LOGOUT_STATUS);
            userMap.put(IFSConstants.LOGOUT_STATUS, logoutStatus);
            removeUserProviderInfo(userDN);
            synchronized (userAndProviderMap) {
                userAndProviderMap.put(userDN, userMap);
            }
        }
    }
    
    /**
     * Sets the provider info where logout was initiated
     * for a user. Other values that are needed when returning control back
     * like <code>relayState</code> is also stored.
     * @param userDN user whose provider information is to be retrieved
     * @param providerId providerId where logout was initiated for this user
     * @param isIDP the role of the source provider
     * @param relayState url must be sent back in return
     * @param responseTo <code>InResponseTo</code> value
     */
    public void setUserProviderInfo(
        String userDN,
        String providerId,
        String isIDP,
        String relayState,
        String responseTo) 
    {
        FSUtils.debug.message(
            "Entered FSReturnSessionManager::setUserProviderInfo");
        userDN = DN.valueOf(userDN).toString().toLowerCase();
        HashMap valMap = new HashMap();
        valMap.put(IFSConstants.PROVIDER, providerId);
        valMap.put(IFSConstants.ROLE, isIDP);
        valMap.put(IFSConstants.LOGOUT_RELAY_STATE, relayState);
        valMap.put(IFSConstants.RESPONSE_TO, responseTo);
        removeUserProviderInfo(userDN);
        synchronized (userAndProviderMap) {
            userAndProviderMap.put(userDN, valMap);
        }
    }
    
    /**
     * Removes provider information for user. This function is called prior to
     * returning after logout.
     * @param userDN user whose logout is being performed
     */
    public void removeUserProviderInfo(String userDN){
        FSUtils.debug.message(
            "Entered FSReturnSessionManager::removeUserProviderInfo");
        userDN = DN.valueOf(userDN).toString().toLowerCase();
        synchronized (userAndProviderMap) {
            userAndProviderMap.remove(userDN);
        }
    }
    
    
    /**
     * Gets the singleton instance of <code>FSReturnSessionManager</code>.
     * There is a single instance for each hosted provider.
     * @return metaAlias the hosted provider whose instance needs to be
     *  returned
     * @return the singleton <code>FSReturnSessionManager</code> instance
     */
    public static FSReturnSessionManager getInstance(String metaAlias){
        FSUtils.debug.message("Entered FSReturnSessionManager::getInstance");
        FSReturnSessionManager instance = null;
        synchronized (FSReturnSessionManager.class) {
            instance =
                (FSReturnSessionManager)instanceMap.get(metaAlias);
            if (instance == null) {
                if (FSUtils.debug.messageEnabled() ) {
                    FSUtils.debug.message("Constructing a new instance"
                        + " of FSReturnSessionManager");
                }
                instance = new FSReturnSessionManager();
                instanceMap.put(metaAlias, instance);
            }
            return (instance);
        }
    }
}
