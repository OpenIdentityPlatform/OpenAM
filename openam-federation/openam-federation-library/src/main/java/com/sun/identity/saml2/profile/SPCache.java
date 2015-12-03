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
 * $Id: SPCache.java,v 1.17 2009/06/09 20:28:32 exu Exp $
 *
 * Portions Copyrighted 2015 ForgeRock AS.
 */


package com.sun.identity.saml2.profile;

import java.util.Hashtable;

import com.sun.identity.common.PeriodicCleanUpMap;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.shared.configuration.SystemPropertiesManager;


/**
 * This class provides the memory store for SAML request and response information on Service Provider side.
 */

public class SPCache {

    public static int interval = SAML2Constants.CACHE_CLEANUP_INTERVAL_DEFAULT;
    public static boolean isFedlet = false; 
    
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
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message("SPCache.constructor: "
                    + "invalid cleanup interval. Using default.");
            }
        }
        // use the configuration implementation class to determine
        // if this is Fedlet, this could be done using a dedicate property
        // in the future 
        String configClass = SystemPropertiesManager.get(
            "com.sun.identity.plugin.configuration.class");
        if ((configClass != null) && (configClass.trim().equals(
        "com.sun.identity.plugin.configuration.impl.FedletConfigurationImpl"))){
            //this is a Fedlet
            isFedlet = true;
        } 
    }
    
    private SPCache() {
    }

    /**
     * Map saves the authentication request.
     * Key   :   A unique key String value
     * Value : AuthnRequest object
     */
    final public static PeriodicCleanUpMap authnRequestHash = new PeriodicCleanUpMap(
            interval * 1000, interval * 1000);

    /**
     * Map saves data on whether the account was federated.
     * Key   :   A unique key String value
     * Value : String representing boolean val
     */
    final public static PeriodicCleanUpMap fedAccountHash = new PeriodicCleanUpMap(
            interval * 1000, interval * 1000);

    /**
     * Map saves the request info.
     * Key   :   requestID String
     * Value : AuthnRequestInfo object
     */
    final public static PeriodicCleanUpMap requestHash = new PeriodicCleanUpMap(
        interval * 1000, interval * 1000); 

    /**
     * Map saves the MNI request info.
     * Key   :   requestID String
     * Value : ManageNameIDRequestInfo object
     */
    final protected static PeriodicCleanUpMap mniRequestHash = new PeriodicCleanUpMap(
        interval * 1000, interval * 1000);

    /**
     * Map to save the relayState URL.
     * Key  : a String the relayStateID 
     * Value: a String the RelayState Value 
     */
    final public static PeriodicCleanUpMap relayStateHash= new PeriodicCleanUpMap(
        interval * 1000, interval * 1000);

    /**
     * Hashtable stores information required for LogoutRequest consumption.
     * key : String NameIDInfoKey (NameIDInfoKey.toValueString())
     * value : List of SPFedSession's
     *       (SPFedSession - idp sessionIndex (String)
     *                     - sp token id (String)                     
     * one key --- multiple SPFedSession's
     */
    final public static Hashtable fedSessionListsByNameIDInfoKey = new Hashtable();

    /**
     * SP: used to map LogoutRequest ID and inResponseTo in LogoutResponse
     * element to the original LogoutRequest object
     * key : request ID (String)
     * value : original logout request object  (LogotRequest)
     */
    final public static PeriodicCleanUpMap logoutRequestIDHash =
        new PeriodicCleanUpMap(interval * 1000, interval * 1000);

    /**
     * Map saves response info for local auth.
     * Key: requestID String
     * Value: ResponseInfo object
     */
    final protected static PeriodicCleanUpMap responseHash = new PeriodicCleanUpMap(
        interval * 1000, interval * 1000);

    /**
     * Hashtable saves AuthnContext Mapper object.
     * Key: hostEntityID+realmName
     * Value: SPAuthnContextMapper
     */
    final public static Hashtable authCtxObjHash = new Hashtable();

    /**
     * Hashtable saves AuthnContext class name and the authLevel. 
     * Key: hostEntityID+realmName
     * Value: Map containing AuthContext Class Name as Key and value
     *              is authLevel.
     */
    final public static Hashtable authContextHash = new Hashtable();

    /**
     * Hashtable saves the Request Parameters before redirecting
     * to IDP Discovery Service to retreive the preferred IDP.
     * Key: requestID a String
     * Value : Request Parameters Map , a Map
     */
    final public static PeriodicCleanUpMap reqParamHash = new PeriodicCleanUpMap(
        SPCache.interval * 1000, SPCache.interval * 1000);


    /**
     * Cache saves the sp account mapper.
     * Key : sp account mapper class name
     * Value : sp account mapper object
     */
    final public static Hashtable spAccountMapperCache = new Hashtable();
    
    /**
     * Cache saves the sp adapter class instance.
     * Key : realm + spEntityID + adapterClassName
     * Value : sp adapter class instance 
     * (<code>SAML2ServiceProviderAdapter</code>)
     */
    final public static Hashtable spAdapterClassCache = new Hashtable();

    /**
     * Cache saves the fedlet adapter class instance.
     * Key : realm + spEntityID + adapterClassName
     * Value : fedlet adapter class instance 
     * (<code>FedletAdapter</code>)
     */
    public static Hashtable fedletAdapterClassCache = new Hashtable();

    /**
     * Cache saves the ecp request IDP list finder.
     * Key : ecp request IDP list finder class name
     * Value : ecp request IDP list finder object
     */
    final public static Hashtable ecpRequestIDPListFinderCache = new Hashtable();

    /**
     * Cache saves the assertion id.
     * Key : assertion ID String
     * Value : Constant  
     */
    final public static PeriodicCleanUpMap assertionByIDCache =
        new PeriodicCleanUpMap(interval * 1000,
        interval * 1000);
    
    /**
     * Clears the auth context object hash table.
     *
     * @param realmName Organization or Realm
     */
    public static void clear(String realmName) {
        if ((authCtxObjHash != null) &&
                        (!authCtxObjHash.isEmpty())) {
            authCtxObjHash.clear();
        }
        if ((authContextHash != null) && 
                        (!authContextHash.isEmpty())) {
            authContextHash.clear();
        }
   }
}
