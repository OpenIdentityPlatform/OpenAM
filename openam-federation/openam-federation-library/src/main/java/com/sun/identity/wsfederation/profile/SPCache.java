/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SPCache.java,v 1.5 2009/12/14 23:42:48 mallas Exp $
 *
 * Portions Copyright 2015-2016 ForgeRock AS.
 */

package com.sun.identity.wsfederation.profile;

import static org.forgerock.openam.utils.Time.*;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;
import java.util.Date;
import com.sun.identity.shared.configuration.SystemPropertiesManager;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.common.PeriodicCleanUpMap;
import com.sun.identity.common.SystemTimerPool;

/**
 * This class provides the memory store for
 * WS-Federation request and response information on Service Provider side.
 *
 */
public class SPCache {

    public static int interval = SAML2Constants.CACHE_CLEANUP_INTERVAL_DEFAULT;
    public static PeriodicCleanUpMap assertionByIDCache = null;
   
    static {
        String intervalStr = SystemPropertiesManager.get(
            SAML2Constants.CACHE_CLEANUP_INTERVAL);
        try {
            if (intervalStr != null && intervalStr.length() != 0) {
                interval = Integer.parseInt(intervalStr);
                if (interval < 0) {
                    interval =
                        SAML2Constants.CACHE_CLEANUP_INTERVAL_DEFAULT;
                }
            }
        } catch (NumberFormatException e) {
            if (WSFederationUtils.debug.messageEnabled()) {
                WSFederationUtils.debug.message("SPCache.constructor: "
                    + "invalid cleanup interval. Using default.");
            }
        }
        assertionByIDCache = new PeriodicCleanUpMap(interval * 1000,
                                 interval * 1000);
        SystemTimerPool.getTimerPool().schedule(assertionByIDCache,
                new Date(currentTimeMillis() + interval * 1000));
    }

    private SPCache() {
    }

    /**
     * Hashtable saves the request info.
     * Key   :   requestID String
     * Value : AuthnRequestInfo object
     */
    public static Hashtable requestHash = new Hashtable(); 

    /**
     * Hashtable saves the MNI request info.
     * Key   :   requestID String
     * Value : ManageNameIDRequestInfo object
     */
    protected static Hashtable mniRequestHash = new Hashtable(); 

    /**
     * Hashtable to save the relayState URL.
     * Key  : a String the relayStateID 
     * Value: a String the RelayState Value 
     */
    protected static Hashtable relayStateHash= new Hashtable(); 

    /**
     * Hashtable stores information required for LogoutRequest consumption.
     * key : String NameIDInfoKey (NameIDInfoKey.toValueString())
     * value : List of SPFedSession's
     *       (SPFedSession - idp sessionIndex (String)
     *                     - sp token id (String)                     
     * one key --- multiple SPFedSession's
     */
    protected static Hashtable fedSessionListsByNameIDInfoKey = new Hashtable();

    /**
     * SP: used to correlate LogoutRequest ID and inResponseTo in LogoutResponse
     * element : request ID (String)
     */
    public static Set logoutRequestIDs =
         Collections.synchronizedSet(new HashSet());

    /**
     * Hashtable saves response info for local auth.
     * Key: requestID String
     * Value: ResponseInfo object
     */
    protected static Hashtable responseHash = new Hashtable();

    /**
     * Hashtable saves AuthnContext Mapper object.
     * Key: hostEntityID+realmName
     * Value: SPAuthnContextMapper
     */
    public static Hashtable authCtxObjHash = new Hashtable();

    /**
     * Hashtable saves AuthnContext class name and the authLevel. 
     * Key: hostEntityID+realmName
     * Value: Map containing AuthContext Class Name as Key and value
     *              is authLevel.
     */
    public static Hashtable authContextHash = new Hashtable();

    /**
     * Hashtable saves the Request Parameters before redirecting
     * to IDP Discovery Service to retreive the preferred IDP.
     * Key: requestID a String
     * Value : Request Parameters Map , a Map
     */
    public static Hashtable reqParamHash = new Hashtable();


    /**
     * Cache saves the sp account mapper.
     * Key : sp account mapper class name
     * Value : sp account mapper object
     */
    public static Hashtable spAccountMapperCache = new Hashtable();


    /**
     * Clears the auth context object hash table.
     *
     * @param realmName Organization or Realm
     */
    public static void clear(String realmName) {
        boolean isDefault = isDefaultOrg(realmName);
        if ((authCtxObjHash != null) && (!authCtxObjHash.isEmpty())) {
            Enumeration keys = authCtxObjHash.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                if (key.indexOf("|"+realmName) != -1) {
                        authCtxObjHash.remove(key);
                }
                if (isDefault && key.endsWith("|/")) {
                    authCtxObjHash.remove(key);
                }
            }
        }
        if ((authContextHash != null) && (!authContextHash.isEmpty())) {
            Enumeration keys = authContextHash.keys();
            while (keys.hasMoreElements()) {
                String key = (String) keys.nextElement();
                if (key.indexOf("|"+realmName) != -1) {
                        authContextHash.remove(key);
                }
                if (isDefault && key.endsWith("|/")) {
                    authCtxObjHash.remove(key);
                }
            }
        }

    }


    /**
     * Clears the auth context object hash table.
     */
    public static void clear() {
        if ((authCtxObjHash != null) &&
                        (!authCtxObjHash.isEmpty())) {
            authCtxObjHash.clear();
        }
        if ((authContextHash != null) && 
                        (!authContextHash.isEmpty())) {
            authContextHash.clear();
        }
   }


    /**
     * Returns <code>true</code> if the realm is root.
     *
     * @param orgName the organization name
     * @return <code>true</code> if realm is root.
     */
    public static boolean isDefaultOrg(String orgName) {
        return (orgName !=null) || orgName.equals("/");
    }

}
