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
 * $Id: IDPCache.java,v 1.18 2009/05/14 17:23:45 exu Exp $
 *
 * Portions Copyrighted 2010-2014 ForgeRock AS.
 */
package com.sun.identity.saml2.profile;

import com.sun.identity.common.PeriodicCleanUpMap;
import java.util.Collections;
import java.util.Hashtable;
import java.util.HashSet;
import java.util.Set;

/**
 * This class caches authn request objects and relay states
 * based on the request id of the authn requests 
 * It also caches idp session by session index. 
 * TODO: Add cleanup thread to update IDP Cache if the cached
 * objects stay in the cache longer than a certain Cache Duration.
 */

public class IDPCache {
    
    private IDPCache() {
    }

    /**
     * Cache saves the authn request objects.
     * Key : request ID String
     * Value : AuthnRequest object
     * TODO : handle the case when assertion effective time is different
     *        from cleanup interval
     */
    public static PeriodicCleanUpMap authnRequestCache = new PeriodicCleanUpMap(
        SPCache.interval * 1000, SPCache.interval * 1000); 

    /**
     * Cache saves the authn context objects before IDP redirects user to 
     * authentication.
     * Key : request ID String
     * Value : AuthnContext object
     */
    public static PeriodicCleanUpMap idpAuthnContextCache = 
        new PeriodicCleanUpMap(
            SPCache.interval * 1000, SPCache.interval * 1000); 

    /**
     * Cache saves the assertion objects.
     * Key : user ID String
     * Value : list of assertion objects
     */
    public static Hashtable assertionCache = new Hashtable();

    /**
     * Cache saves the assertion objects.
     * Key : assertion ID String
     * Value : assertion object
     */
    public static PeriodicCleanUpMap assertionByIDCache =
        new PeriodicCleanUpMap(SPCache.interval * 1000,
        SPCache.interval * 1000); 

    /**
     * Cache saves the relay state strings.
     * Key : request ID String
     * Value : relay state string
     */
    public static PeriodicCleanUpMap relayStateCache = new PeriodicCleanUpMap(
        SPCache.interval * 1000, SPCache.interval * 1000); 

    /**
     * Cache saves the idp sessions.
     * key : sessionIndex (String)
     * value :IDPSession
     * IDP: used in SingleSignOnService and SingleLogoutService
     *      to invalidate a specific session
     */
    public static Hashtable<String, IDPSession> idpSessionsByIndices = new Hashtable<String, IDPSession>();

    /**
     * Cache saves Responses to be used by ArtifactResolutionService.
     * key --- artifact string (after encoding and all that)
     * value --- Response
     * IDP: used in SingleSignOnService and ArtifactResolutionService
     * TODO : handle the case when artifact expiration time is different
     *        from cleanup interval
     */
    public static PeriodicCleanUpMap responsesByArtifacts = 
       new PeriodicCleanUpMap(SPCache.interval * 1000, SPCache.interval * 1000);

    /**
     * Hashtable saves the MNI request info.
     * Key   :   requestID String
     * Value : ManageNameIDRequestInfo object
     */
    public static PeriodicCleanUpMap mniRequestHash = new PeriodicCleanUpMap(
        SPCache.interval * 1000, SPCache.interval * 1000);

    /**
     * Cache saves the idp attribute mapper.
     * Key : idp attribute mapper class name
     * Value : idp attribute mapper object
     */
    public static Hashtable idpAttributeMapperCache = new Hashtable(); 

    /**
     * Cache saves the idp account mapper.
     * Key : idp account mapper class name
     * Value : idp account mapper object
     */
    public static Hashtable idpAccountMapperCache = new Hashtable();

    /**
     * Cache saves the idp authn context mapper.
     * Key : idp authn context mapper class name
     * Value : idp authn context mapper object
     */
    public static Hashtable idpAuthnContextMapperCache = new Hashtable(); 

    /**
     * Cache saves the idp ecp session mapper.
     * Key : idp ecp session mapper class name
     * Value : idp ecp session mapper object
     */
    public static Hashtable idpECPSessionMapperCache = new Hashtable(); 

     /**
     * Cache saves the IDP Proxy Finder.
     * Key : IDP Proxy Finder class name
     * Value : IDP Proxy Finder mapper object
     */
    public static Hashtable idpProxyFinderCache = new Hashtable();

    /**
     * Cache saves the IDP Adapter.
     * Key : IDP Adapter class name
     * Value : IDP Adapter mapper object
     */
    public static Hashtable idpAdapterCache = new Hashtable();

    /**
     * Cache saves information needed after coming back from COT cookie setting.
     * key --- cachedResID (String)
     * value --- Response Information List (ArrayList of size 9)
     * IDP: used in SingleSignOnService and ArtifactResolutionService
     */
    public static PeriodicCleanUpMap responseCache = new PeriodicCleanUpMap(
        SPCache.interval * 1000, SPCache.interval * 1000);
 
    /**
     * Cache saves informate needed to determine the Authentication
     * Context of the incoming request from Service Provider. 
     * key   : sessionIndex (String)
     * value : the AuthnContext object
     */
    public static Hashtable authnContextCache = new Hashtable();

    /**
     * Cache saves information to determine if the request was
     * a session upgrade case. 
     * key   : requestID (String)
     * value : session upgrade (Boolean)
     */
    public static Set isSessionUpgradeCache =  
        Collections.synchronizedSet(new HashSet());

    /**
     * Cache saves the IDP Session object before an session upgrade.
     * key    : requestID (String)
     * value  : IDPSession object.
     */
    public static Hashtable oldIDPSessionCache = new Hashtable();
    
    /**
      * Cache saves the original AuthnRequest coming from SP to IDP proxy
      * key   : requestID (String) 
      * value : AuthnRequest 
      */ 
    public static PeriodicCleanUpMap proxySPAuthnReqCache =
         new PeriodicCleanUpMap(
         SPCache.interval * 1000, SPCache.interval * 1000); 

    /** 
      * Cache saves the SAML2SessionPartner  
      * key   : sessionId (String) 
      * value : SAML2SessionPartner
      */
    public static Hashtable idpSessionsBySessionID = new Hashtable(); 
    
    /** 
      * Cache saves user ID for transient NameID 
      * key   : NameID value (String) 
      * value : user ID 
      */
    public static Hashtable userIDByTransientNameIDValue = new Hashtable(); 

    /** 
      * Cache saves the original LogoutRequest coming from SP to IDP proxy
      * key   : requestID (String) 
      * value : LogoutRequest
      */
    public static PeriodicCleanUpMap proxySPLogoutReqCache =
        new PeriodicCleanUpMap(
        SPCache.interval * 1000, SPCache.interval * 1000); 
    
    /** 
      * Cache saves the SOAPMessage created by proxy IDP to the original SP
      * key   : requestID (String) 
      * value : SOAPMessage
      */
    public static PeriodicCleanUpMap SOAPMessageByLogoutRequestID =
         new PeriodicCleanUpMap(
         SPCache.interval * 1000, SPCache.interval * 1000); 
    
    /**
      * Cache saves the SAML2 Session Partner's providerID 
      * key   : sessionId (String)
      * value : SAML2 SessionPartner's provider id 
      */
    public static Hashtable spSessionPartnerBySessionID = new Hashtable();
    
     /** 
      * Cache saves the original LogoutResponse generated by IDP proxy 
      * to the IDP
      * key   : requestID (String) 
      * value : Map keeping LogoutResponse, sending location, 
      *         spEntityID and idpEntityID. 
      */
     public static PeriodicCleanUpMap logoutResponseCache =
          new PeriodicCleanUpMap(
          SPCache.interval * 1000, SPCache.interval * 1000); 
    /**
     * Hashtable saves AuthnContextClassRef to auth schems mapping
     * key  : hostEntityID + "|" + realmName
     * value: Map containing AuthnContext class ref as Key and 
     *            Set of auth schemes as value.
     */
    public static Hashtable classRefSchemesHash = new Hashtable();

    /**
     * Hashtable saves AuthnContextClassRef to AuthLevel mapping
     * key  : hostEntityID + "|" + realmName
     * value: Map containing AuthnContext class ref as Key and 
     *            authLevel as value.
     */
    public static Hashtable classRefLevelHash = new Hashtable();

    /**
     * Hashtable saves AuthLevel to AuthnContextClassRef mapping
     * key  : hostEntityID + "|" + realmName
     * value: String default AuthnContext Class Ref.
     */
    public static Hashtable defaultClassRefHash = new Hashtable();

    /**
     * Hashtable saves NameID format to user profile attribute mapping
     * key  : hostEntityID + "|" + realm
     * value: Map containing NameNameID format as Key and user profile
     *     attribute name as Value.
     */
    public static Hashtable formatAttributeHash = new Hashtable();

    /**
     * Clears the authn context mapping hash tables.
     * @param realmName Organization or Realm
     */
    public static void clear(String realmName) {
        if (classRefSchemesHash != null && !classRefSchemesHash.isEmpty()) {
            classRefSchemesHash.clear();
        }
        if (classRefLevelHash != null && !classRefLevelHash.isEmpty()) {
            classRefLevelHash.clear();
        }
        if (defaultClassRefHash != null && !defaultClassRefHash.isEmpty()) {
            defaultClassRefHash.clear();
        }
        formatAttributeHash.clear();
    }
}
