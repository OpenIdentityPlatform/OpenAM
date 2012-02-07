/**
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
 * $Id: FSRegistrationManager.java,v 1.3 2008/06/25 05:47:03 qcheng Exp $
 *
 */


package com.sun.identity.federation.services.registration;

import com.sun.identity.federation.common.FSUtils;
import java.util.HashMap;
import java.util.Map;

/**
 * Manages registration state per provider.
 */
public final class FSRegistrationManager{
    
    private static Map instanceMap = new HashMap();
    private Map registrationRequestMap = new HashMap();
    
    private FSRegistrationManager() {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSRegistrationManager(): Called." +
            " A new instance of FSRegistrationManager created");
        }
    }
    
    /**
     * Returns registration state for the provider.
     * @param registrationId registration id
     * @return HashMap containing registration state info.
     */
    protected HashMap getRegistrationMap(String registrationId){
        FSUtils.debug.message("FSRegistrationManager::getRegistrationMap");
        if (registrationRequestMap.containsKey(registrationId)) {
            return (HashMap)registrationRequestMap.get(registrationId);
        } else {
            return null;
        }                
    }
    
    /**
     * Sets registration state info.
     * @param registrationId registration id
     * @param valMap registration state info
     */
    protected void setRegistrationMapInfo(
        String registrationId,
        HashMap valMap)
    {
        FSUtils.debug.message(
            "Entered FSRegistrationManager::setRegistrationMapInfo");
        removeRegistrationMapInfo(registrationId);
        registrationRequestMap.put(registrationId, valMap);                
    }
    
    /**
     * Removes registartion state info.
     * @param registrationId registration id
     */
    protected void removeRegistrationMapInfo(String registrationId){
        FSUtils.debug.message(
            "Entered FSRegistrationManager::removeRegistrationMapInfo");
        if (registrationRequestMap.containsKey(registrationId)) {
            registrationRequestMap.remove(registrationId);
        }
    }
    
    
    /**
     * Gets the singleton instance of <code>FSRegistrationManager</code>.
     * There is a single instance for each hostedProvider.
     * @param metaAlias hosted provider meta alias whose instance 
     *  needs to be returned
     * @return the singleton <code>FSRegistrationManager</code> instance
     */
    protected static FSRegistrationManager getInstance(String metaAlias){
        FSUtils.debug.message("Entered FSRegistrationManager::getInstance");
        FSRegistrationManager instance = null;
        synchronized (FSRegistrationManager.class) {
            instance = (FSRegistrationManager)instanceMap.get(metaAlias);
            if (instance == null) {
                FSUtils.debug.message(
                    "Constructing a new instance of FSRegistrationManager");
                instance = new FSRegistrationManager();
                instanceMap.put(metaAlias, instance);
            }
            return (instance);
        }
    }
}
