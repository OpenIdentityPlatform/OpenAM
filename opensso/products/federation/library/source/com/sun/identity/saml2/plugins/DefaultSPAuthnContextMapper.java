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
 * $Id: DefaultSPAuthnContextMapper.java,v 1.9 2008/11/10 22:57:02 veiming Exp $
 *
 */


package com.sun.identity.saml2.plugins;

import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.saml2.protocol.RequestedAuthnContext;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.profile.SPSSOFederate;
import com.sun.identity.saml2.profile.SPCache;
import com.sun.identity.shared.encode.URLEncDec;
import com.sun.identity.shared.xml.XMLUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Set;
import java.util.StringTokenizer;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

/**
 * The <code>DefaultSPAuthnContextMapper.java</code> class determines
 * the authentication context and the authentication requirements for
 * authentication by the authenticaion authority.
 *
 * This implementation only uses Authentication Class Reference.
 * The Authentication Class Reference can be passed as a query parameter
 * or set in the SP Entity Configuration.
 */

public class DefaultSPAuthnContextMapper implements SPAuthnContextMapper {

    static String DEFAULT = "default";
    static String DEFAULT_CLASS_REF = "defaultClassRef";

    /**
     * Returns the <code>RequestedAuthnContext</code> object.
     *
     * The RequestedAuthContext is created based on the query parameters
     * AuthnContextClassRef and AuthComparison  in the request
     * and authnContext attribute ,
     * spAuthncontextClassrefMapping, and  authComparison
     * attribute, spAuthncontextComparisonType ,  
     * set in the Service Provider Extended Configuration.
     * If the AuthnContext Class Reference cannot be determined then
     * the default value
     * urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTranstport
     * will be used. AuthnComparsion defaults to "exact" if no value
     * is specified.
     *
     * @param realm  Realm or Organization of the Service Provider.
     * @param hostEntityID Entity ID of the Service Provider.
     * @param paramsMap Map containing key/value pairs of parameters.
     *        The key/value pairs are those accepted during SP SSO
     *        initiation.
     * @throws SAML2Exception if an error occurs.
     */
    public RequestedAuthnContext getRequestedAuthnContext(String realm,
        String hostEntityID, Map paramsMap)
        throws SAML2Exception {

        // Read the AuthnContext Class Reference passed as query string
        // to SP 
        List authContextClassRef =
            (List) paramsMap.get(SAML2Constants.AUTH_CONTEXT_CLASS_REF);
        List authLevelList = 
            ((List)paramsMap.get(SAML2Constants.AUTH_LEVEL));
    
        Integer authLevel=null;
        if (authLevelList != null && !authLevelList.isEmpty()) {
            try { 
                authLevel =
                    new Integer((String) authLevelList.iterator().next());
            } catch (NumberFormatException nfe) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message("not a valid integer",nfe);   
                }
            } catch (Exception e) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message("error getting " 
                        + "integer object",e);
                }
            }
        }

        if (authLevel == null) {
            authLevel = getAuthLevelFromAdvice(paramsMap);
        }

        if (SAML2Utils.debug.messageEnabled()) {   
            SAML2Utils.debug.message("authLevel in Query:"+ authLevel);
            SAML2Utils.debug.message("authContextClassRef in Query:"+
                                      authContextClassRef);
        }

        // Retreived the cached AuthClass Ref / Auth Level Map
        Map authRefMap = getAuthRefMap(realm, hostEntityID);

        List authCtxList = new ArrayList();

        // create a List of AuthnContext Class Reference
        if (authContextClassRef != null && !authContextClassRef.isEmpty()) {
            Iterator i = authContextClassRef.iterator();
            while (i.hasNext()) {
                String authClassRef = prefixIfRequired((String) i.next());
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message("DefaultSPAuthnContextMapper: "
                        + "authClassRef=" + authClassRef);
                }
                authCtxList.add(authClassRef);
            }
        }   
        if (authLevel != null) {
            Set authCtxSet = authRefMap.keySet();
            Iterator i = authCtxSet.iterator();
            while (i.hasNext()) {
                String className = (String)i.next();
                if (DEFAULT.equals(className) || 
                    DEFAULT_CLASS_REF.equals(className)) 
                {
                    continue;
                }

                Integer aLevel = (Integer)authRefMap.get(className);
                if (aLevel != null &&
                    aLevel.intValue() >= authLevel.intValue()) {
                    authCtxList.add(className);
                }
            }
        }

        if ((authCtxList == null || authCtxList.isEmpty()) 
            && (authRefMap != null 
            && !authRefMap.isEmpty())) {   
            String defaultClassRef = (String) authRefMap.get(DEFAULT_CLASS_REF);
            if (defaultClassRef != null) {
                authCtxList.add(defaultClassRef);
            } else {
                Set authCtxSet = authRefMap.keySet();
            
                Iterator i = authCtxSet.iterator();
                while (i.hasNext()) {
                    String val = (String) i.next();
                    if (val != null && !val.equals(DEFAULT)) {
                        authCtxList.add(val);
                    }
                }
            }
        }

        // if list empty set the default
        if (authCtxList.isEmpty()) {
            authCtxList.add(
                SAML2Constants.CLASSREF_PASSWORD_PROTECTED_TRANSPORT);
        }
        
        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("SPCache.authContextHash is: "
                 + SPCache.authContextHash);
            SAML2Utils.debug.message("authCtxList is: "+ authCtxList);
        }
                
        // Retrieve Auth Comparison from Query parameter
        String authCtxComparison = SPSSOFederate.getParameter(paramsMap,
            SAML2Constants.SP_AUTHCONTEXT_COMPARISON);

        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("AuthComparison in Query:"+
                                      authCtxComparison);
        }
        if ((authCtxComparison == null) || 
            !isValidAuthComparison(authCtxComparison)) {
            authCtxComparison = SAML2Utils.getAttributeValueFromSSOConfig(
                realm, hostEntityID, SAML2Constants.SP_ROLE,
                SAML2Constants.SP_AUTHCONTEXT_COMPARISON_TYPE);

            if ((authCtxComparison != null) &&
                (!isValidAuthComparison(authCtxComparison))) {
                authCtxComparison = null;
            }
        } 

        RequestedAuthnContext reqCtx = 
            ProtocolFactory.getInstance().createRequestedAuthnContext();
        reqCtx.setAuthnContextClassRef(authCtxList);
        reqCtx.setComparison(authCtxComparison);

        return reqCtx;
    }

    /**
     * Returns the auth level from advice.
     * The advice is passed in through paramsMap as follows:
     * Key:                  Value:
     * sunamcompositeadvice URLEncoded XML blob that specifies auth level
     *                      advice. Here is an example of the xml blob:
     *                      <Advice>
     *                      <AttributeValuePair>
     *                      <Attribute name="AuthLevelConditionAdvice"/>
     *                      <Value>/:1</Value>
     *                      </AttributeValuePair>
     *                      </Advice>
     *
     *                      In this advice, the requested auth level is 1.
     *                      Note: The ":" before auth level 1 is a must.
     */
    private Integer getAuthLevelFromAdvice(Map paramsMap) {
        Integer level = null;
        List advices = (List) paramsMap.get(SAML2Constants.AUTH_LEVEL_ADVICE);
        if (advices != null && !advices.isEmpty()) {
            String adviceXML = URLEncDec.decode(
                (String) advices.iterator().next());
            if (SAML2Utils.debug.messageEnabled()) {
                SAML2Utils.debug.message(
                    "DefaultSPAuthnContextMapper:adviceXML=" + adviceXML);
            }
            Set authLevelvalues = null;
            // parse xml
            Document document = XMLUtils.toDOMDocument(
                adviceXML, SAML2Utils.debug);
            if (document != null) {
                Node adviceNode = XMLUtils.getRootNode(document, "Advices");
                if (adviceNode != null) {
                    Map advicePair = XMLUtils.parseAttributeValuePairTags(
                        adviceNode);
                    authLevelvalues = (Set) advicePair.get(
                        "AuthLevelConditionAdvice");
                }
            }
            if ((authLevelvalues != null) && (!authLevelvalues.isEmpty())) {
                // get the lowest auth level from the given set
                Iterator iter = authLevelvalues.iterator();
                while (iter.hasNext()) {
                    String authLevelvalue = (String) iter.next();
                    if (authLevelvalue != null && authLevelvalue.length() != 0){
                        int index = authLevelvalue.indexOf(":");
                        String authLevelStr = null;
                        if (index != -1) {
                            authLevelStr = 
                                authLevelvalue.substring(index +1).trim();
                        } else {
                            authLevelStr = authLevelvalue;
                        }
                        try {
                            Integer authLevel = new Integer(authLevelStr);
                            if (level == null || level.compareTo(authLevel) > 0)
                            {
                                level = authLevel;
                            }
                        } catch (Exception nex) {
                            continue;
                        }
                    }
                }
                
            }
        }
        return level;   
    }

    /**
     * Returns the auth level for the AuthContext
     *
     * @param reqCtx  the RequestedAuthContext object.
     * @param authnContext  the AuthnContext object.
     * @param realm the realm or organization to 
     *    retreive the authncontext.
     * @param hostEntityID the Service Provider Identity String.
     * @param idpEntityID the Identity Provider Identity String.
     * @return authlevel an integer value.
     * @throws SAML2Exception if there is an error.
     */
    public int getAuthLevel(RequestedAuthnContext reqCtx,
                            AuthnContext authnContext,
                            String realm,
                            String hostEntityID, String idpEntityID) 
                            throws SAML2Exception {

        Map authRefMap = 
                (Map) SPCache.authContextHash.get(hostEntityID+"|"+realm);
        if (authRefMap == null || authRefMap.isEmpty()) {
            authRefMap = getAuthRefMap(realm,hostEntityID);
        }

        int authLevel = 0;

        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("DefaultSPAuthnContextMapper:hostEntityID:"
                                        + hostEntityID);
            SAML2Utils.debug.message("DefaultSPAuthnContextMapper:realm:"
                                        + realm);
            SAML2Utils.debug.message("DefaultSPAuthnContextMapper:MAP:"
                                        + authRefMap);
            SAML2Utils.debug.message("DefaultSPAuthnContextMapper:HASH:"
                                        + SPCache.authContextHash);
        }
        String authnClassRef = null;
        if (authnContext != null) {
            authnClassRef = authnContext.getAuthnContextClassRef();
        }

        if ((reqCtx != null) && (authnClassRef != null) &&
            (!isAuthnContextMatching(reqCtx.getAuthnContextClassRef(),
            authnClassRef, reqCtx.getComparison(), realm, hostEntityID))) {

            throw new SAML2Exception(SAML2Utils.bundle.getString(
                "invalidAuthnContextClassRef"));
        }

        Integer authLevelInt = null;
        if ((authnClassRef != null) && (authnClassRef.length() > 0)) {
            if ((authRefMap != null) && (!authRefMap.isEmpty())) {
                authLevelInt = (Integer)authRefMap.get(authnClassRef);
            }
        } else {
            if ((authRefMap != null) && (!authRefMap.isEmpty())) {
                authLevelInt = (Integer)authRefMap.get(DEFAULT);
            }
        }

        if (authLevelInt != null) {
            authLevel = authLevelInt.intValue();
        }

        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("DefaultSPAuthnContextMapper:authnClRef:"
                                        + authnClassRef);
            SAML2Utils.debug.message("DefaultSPAuthnContextMapper:authLevel :"
                                        + authLevel);
        }
        return authLevel;
    }

    /** 
     * Returns true if the specified AuthnContextClassRef matches a list of
     * requested AuthnContextClassRef.
     *
     * @param requestedACClassRefs a list of requested AuthnContextClassRef's
     * @param acClassRef AuthnContextClassRef
     * @param comparison the type of comparison
     * @param realm  Realm or Organization of the Service Provider.
     * @param hostEntityID Entity ID of the Service Provider.
     * 
     * @return true if the specified AuthnContextClassRef matches a list of
     *     requested AuthnContextClassRef
     */
    public boolean isAuthnContextMatching(List requestedACClassRefs,
        String acClassRef, String comparison, String realm,
        String hostEntityID) {

        Map authRefMap = getAuthRefMap(realm, hostEntityID);

        return SAML2Utils.isAuthnContextMatching(requestedACClassRefs,
            acClassRef, comparison, authRefMap);
    }

    /* parses the AuthContext attribute to get the Class Reference and
     * authlevel 
     */
    private static Map getAuthnCtxFromSPConfig(String realm,
        String hostEntityID) {

        List authContextClassRefConfig = 
            SAML2Utils.getAllAttributeValueFromSSOConfig(realm, hostEntityID,
            SAML2Constants.SP_ROLE, 
            SAML2Constants.SP_AUTH_CONTEXT_CLASS_REF_ATTR);

        if (SAML2Utils.debug.messageEnabled()) {
            SAML2Utils.debug.message("DefaultSPAuthnContextMapper: List:"
                        +authContextClassRefConfig);
        }
        HashMap authRefMap =  new LinkedHashMap();

        if (authContextClassRefConfig != null && 
            authContextClassRefConfig.size() != 0) {

            Iterator i = authContextClassRefConfig.iterator();
            while (i.hasNext()) { 
                boolean isDefault = false;
                String authRefVal = (String)i.next();
                if (authRefVal.endsWith("|" + DEFAULT)) {
                    authRefVal = authRefVal.substring(0,
                        authRefVal.length() - DEFAULT.length());
                    isDefault = true;
                }
                StringTokenizer st = new StringTokenizer(authRefVal,"|");
                String authClass = null;
                try {
                    authClass = (String) st.nextToken();
                } catch (Exception e ) {
                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message("AuthnContextClassRef "
                                                 + "not found");
                    }
                }

                if (st.hasMoreTokens()) {
                    Integer authLevel = null;
                    try {
                        authLevel = new Integer(st.nextToken());
                    } catch (NumberFormatException nfe) {
                        if (SAML2Utils.debug.messageEnabled()) {
                            SAML2Utils.debug.message(
                                "DefaultSPAuthnContextMapper." +
                                "getAuthnCtxFromSPConfig:", nfe);
                        }
                    }

                    if (SAML2Utils.debug.messageEnabled()) {
                        SAML2Utils.debug.message(
                            "DefaultSPAuthnContextMapper." +
                            "getAuthnCtxFromSPConfig: AuthLevel is " +
                            authLevel);
                    }
                    if (authLevel != null) {
                        if (isDefault && (!authRefMap.containsKey(DEFAULT))) {
                            authRefMap.put(DEFAULT, authLevel);
                        }
                        if (authClass != null) {
                            authRefMap.put(prefixIfRequired(authClass),
                                authLevel);
                        }
                    }
                }
                if (isDefault && (authClass != null) &&
                    (!authRefMap.containsKey(DEFAULT_CLASS_REF)))
                {
                    authRefMap.put(
                        DEFAULT_CLASS_REF, prefixIfRequired(authClass));
                }
            }
        }

        return Collections.unmodifiableMap(authRefMap);
    }


    /* checks for validity of authcomparision */
    private static boolean isValidAuthComparison(String authComparison) {

        return authComparison.equals("exact") 
                                || authComparison.equals("maximum") 
                                || authComparison.equals("minimum") 
                                || authComparison.equals("better") ;
    }

    /* returns a Map with key as the hostEntityID|realm and value the
     * the SP Extended configuration attributes.
     */
    private static Map getAuthRefMap(String realm,String hostEntityID) {
        String key = hostEntityID + "|" + realm;
        Map authRefMap = (Map)SPCache.authContextHash.get(key);

        if (authRefMap == null) {
            try {
                authRefMap = (Map)getAuthnCtxFromSPConfig(realm, hostEntityID);
                if ((authRefMap != null) && (!authRefMap.isEmpty())) {
                    SPCache.authContextHash.put(key, authRefMap);
                }
            } catch (Exception e ) {
                if (SAML2Utils.debug.messageEnabled()) {
                    SAML2Utils.debug.message("DefaultSPAuthnContextMapper." +
                        "getAuthRefMap:", e);
                }
            }
        }
        return authRefMap;
    }

    /**
     * Adds prefix to the authn class reference only when there is 
     * no ":" present.
     */ 
    private static String prefixIfRequired(String authClassRef) {
        if ((authClassRef != null) && (authClassRef.indexOf(':') == -1)) {
            return SAML2Constants.AUTH_CTX_PREFIX + authClassRef;
        } else {
            return authClassRef;
        }
    }
}
