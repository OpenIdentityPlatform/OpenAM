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
 * $Id: FSAuthnDecisionHandler.java,v 1.4 2008/06/25 05:46:53 qcheng Exp $
 *
 */


package com.sun.identity.federation.services;

import com.sun.identity.common.SystemConfigurationUtil;
import com.sun.identity.federation.common.FSException;
import com.sun.identity.federation.common.FSUtils;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;
import com.sun.identity.federation.services.util.FSServiceUtils;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;

/**
 * Used by <code>IDP</code> to decide which authentication to use to meet the
 * need of requested authentication context.
 */
public class FSAuthnDecisionHandler {
 
    private Map idpAuthContextMap = null;
    private String loginURL = null;
    private static IDFFMetaManager metaManager = null;
    private int compAuthType = 0;
    
    static {
        metaManager = FSUtils.getIDFFMetaManager();
    }
    
    /**
     * Constructs a new <code>FSAuthnDecisionHandler</code> object. It handles
     * authentication decision based on the configuration per identity provider.
     * @param realm The realm under which the entity resides.
     * @param entityID hosted identity provider entity ID
     * @param request http servlet request
     */
    public FSAuthnDecisionHandler(
        String realm, String entityID,HttpServletRequest request)
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSAuthnDecisionHandler::Constructor called "
                + "with entityID" + entityID);
        }

        loginURL = SystemConfigurationUtil.getProperty(
            IFSConstants.IDP_LOGIN_URL);
        if ((loginURL == null) || (loginURL.trim().length() == 0)) {
            loginURL = FSServiceUtils.getBaseURL(request) +
                IFSConstants.LOGIN_PAGE;
        }
        loginURL = loginURL + IFSConstants.QUESTION_MARK 
            + IFSConstants.ARGKEY + IFSConstants.EQUAL_TO 
            + IFSConstants.NEWSESSION;
        getIDPAuthContextInfo(realm, entityID);
    }
    
    private void getIDPAuthContextInfo(String realm, String entityID) {
        if (metaManager == null) {
            return;
        }
        try {
            IDPDescriptorConfigElement entityConfig = 
                metaManager.getIDPDescriptorConfig(realm, entityID);
            if (entityConfig == null) {
                return;
            }
            Map attributes = IDFFMetaUtils.getAttributes(entityConfig);
            List mappings = (List) attributes.get(
                IFSConstants.IDP_AUTHNCONTEXT_MAPPING);
            if (mappings != null && !mappings.isEmpty()) {
                idpAuthContextMap = new HashMap();
                Iterator iter = mappings.iterator();
                while (iter.hasNext()) {
                    String infoString = (String) iter.next();
                    try {
                        FSIDPAuthenticationContextInfo info = 
                            new FSIDPAuthenticationContextInfo(infoString);
                        idpAuthContextMap.put(
                            info.getAuthenticationContext(), info);
                    } catch (FSException fe) {
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message(
                                "FSAuthContextHandler.getIDPAuthContextInfo: " +
                                "info is not valid:" + infoString + " ",fe);
                        }
                        continue;
                    }
                }
            }
        } catch (IDFFMetaException e) {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSAuthContextHandler.getIDPAuthContextInfo: ", e);
            }
        }
    }

    /**
     * Finds higher level authentication context.
     */
    private FSIDPAuthenticationContextInfo
        getHigherAuthContext(int currentStrength) 
    {
        FSUtils.debug.message(
            "FSAuthnDecisionHandler::getHigherAuthContext called.");
        Iterator iter = idpAuthContextMap.entrySet().iterator();
        while (iter.hasNext()) {
            FSIDPAuthenticationContextInfo returnObj 
                = (FSIDPAuthenticationContextInfo) iter.next();
            if (returnObj != null &&
                returnObj.getLevel() > currentStrength)
            {
                return returnObj;
            }
        }
        FSUtils.debug.message(
            "FSAuthnDecisionHandler::getHigherAuthContext returning null");
        return null;
    }

    /**
     * Finds highest authentication context lower than current one.
     */
    private FSIDPAuthenticationContextInfo
        getLowerAuthContext(int currentStrength) 
    {
        FSUtils.debug.message(
            "FSAuthnDecisionHandler::getHigherAuthContext called.");
        FSIDPAuthenticationContextInfo returnObj = null;
        Iterator iter = idpAuthContextMap.entrySet().iterator();
        while (iter.hasNext()) {
            FSIDPAuthenticationContextInfo tempObj 
                = (FSIDPAuthenticationContextInfo) iter.next();
            if (tempObj != null &&
                tempObj.getLevel() < currentStrength)
            {
                if (returnObj == null ||
                    returnObj.getLevel() < tempObj.getLevel())
                {
                    returnObj = tempObj;
                }
            }
        }
        return returnObj;
    }
    
    /**
     * Decides if present authentication context is sufficient comparing to
     * the requested authentication context.
     * @param authContextRef requested authentication contexts
     * @param presentAuthContext present authentication context
     * @param authType authentication context comparison type. The possible
     *  values are <code>exact</code>, <code>minimum</code>,
     *  <code>better</code>, and <code>maximum</code>.
     * @return <code>FSAuthContextResult</code> object with login url set if
     *  the present authentication context is not sufficient; login url set to
     *  <code>null</code> if the present authentication context is sufficient.
     *  Return <code>null</code> if it cannot be decided or appropriate
     *  authentication context cannot be obtained.
     */
    public FSAuthContextResult decideAuthnContext(
        List authContextRef,
        String presentAuthContext,
        String authType)
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSAuthnDecisionHandler::" 
                + "decideAuthnContext called with list. " + authContextRef
                + " and authComparisonType " + authType);
        }
        if (authType == null) {
            authType = IFSConstants.MINIMUM;
        }

        FSAuthContextResult returnObj = new FSAuthContextResult();
        if (authContextRef != null) {
            // by default, compAuthType is set to 0, which is EXACT
            if (authType.equals(IFSConstants.MINIMUM)) {
                compAuthType = 1;
            } else if (authType.equals(IFSConstants.BETTER)) {
                compAuthType = 2;
            } else if (authType.equals(IFSConstants.MAXIMUM)) {
                compAuthType = 3;
            }

            Iterator authIter = authContextRef.iterator();
            while (authIter.hasNext()) {
                String authCntxt = (String) authIter.next();
                returnObj = decideAuthnContext(authCntxt,presentAuthContext);
                if (returnObj != null) {
                    // either present is sufficient, or new one is created
                    return returnObj;    
                } // else cannot decide
            }
        }
        return returnObj;
    }
    
    /**
     * Finds authentication context result based on the request authentication
     * context and comparison type.
     * @param authContextClassRef list of requested authentication context
     *  class references
     * @param authType requested authentication context comparison type.
     *  Possible values are <code>exact</code>, <code>minimum</code>,
     *  <code>better</code>, and <code>maximum</code>
     * @return <code>FSAuthContextResult</code> object
     */
    public FSAuthContextResult getURLForAuthnContext(
        List authContextClassRef,
        String authType)
    {
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSAuthnDecisionHandler::" 
                + "getURLForAuthnContext called with list. " 
                + authContextClassRef
                + " and authComparisonType " + authType);
        }
        if (authType == null) {
            authType = IFSConstants.MINIMUM;
        }
        FSAuthContextResult returnObj = null;
        if (authContextClassRef != null && authType != null ) {
            // compAuthType was set to 0 which is EXACT.
            if (authType.equals(IFSConstants.MINIMUM)) {
                compAuthType = 1;
            } else if (authType.equals(IFSConstants.BETTER)) {
                compAuthType = 2;
            } else if (authType.equals(IFSConstants.MAXIMUM)) {
                compAuthType = 3;
            }
 
            Iterator authIter = authContextClassRef.iterator();
            while (authIter.hasNext()) {
                String authCntxt = (String) authIter.next();
                returnObj = getURLForAuthnContext(authCntxt);
                if (returnObj != null && returnObj.getLoginURL() != null) {
                    return returnObj;
                }
            }
        }
        return returnObj;
    }

    /**
     * Finds authentication context result based on the request authentication
     * context. Comparison type is set to minimum.
     * @param authContextClassRef list of requested authentication context
     *  class references
     * @return <code>FSAuthContextResult</code> object
     */
    public FSAuthContextResult getURLForAuthnContext(List authContextClassRef) {
        return getURLForAuthnContext(authContextClassRef, null);
    }
    
    /**
     * Searches for the login page URL corresponding to the request
     * authentication context class reference using the comparison type set
     * in the class previously.
     * @param authContextRef request authentication context class reference
     * @return FSAuthContextResult object which contains the Login page URL 
     *  and the corresponding AuthContext.
     */
    private FSAuthContextResult getURLForAuthnContext(String authContextRef)
    {
        FSUtils.debug.message(
            "FSAuthDecisionHandler::getURLForAuthContext. Entered method");
        if (authContextRef != null && idpAuthContextMap != null) {
            FSIDPAuthenticationContextInfo authInfo =
                (FSIDPAuthenticationContextInfo)idpAuthContextMap.get(
                    authContextRef);
            if (FSUtils.debug.messageEnabled()) {    
                FSUtils.debug.message(
                    "FSAuthnDecisionHandler::getURLForAuthnContext " 
                    +"in auth context checking for " 
                    + authContextRef); 
            }
            if (authInfo != null) {
                String returnURL = new String();
                
                if (loginURL != null) {
                    FSAuthContextResult authResult = new FSAuthContextResult();
                    String moduleIndicator = 
                        (String) authInfo.getModuleIndicatorValue();
                    String authKey = (String) authInfo.getModuleIndicatorKey();
                    if (!authKey.equalsIgnoreCase("none") && 
                            moduleIndicator != null) 
                    {
                        returnURL = loginURL + "&" 
                            + authInfo.getModuleIndicatorKey() + "="
                            + moduleIndicator ;
                    } else {
                        returnURL = loginURL;
                    }
                    authResult.setLoginURL(returnURL);
                    authResult.setAuthContextRef(
                        authInfo.getAuthenticationContext());
                    return authResult;
                } else {
                    FSUtils.debug.error("FSAuthnDecisionHandler::"
                        + "getURLForAuthnContext."
                        + "login url is null, or auth info is not found");
                }
            } else {
                FSUtils.debug.error("FSAuthnDecisionHandler::"
                    + "getURLForAuthnContext. Could not get any authcontext");
            }
        } else {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSAuthnDecisionHandler::"
                    + "getURLForAuthContext. Method called with"
                    + "authContextMinRef null");
            }
        }
        return null;
    }
    
    /**
     * Decides if present authentication context is sufficient comparing to
     * the requested authentication context using the comparison type set
     * previously.
     * @param authContextMinRef requested authentication context
     * @param presentAuthContext present authentication context
     * @return <code>FSAuthContextResult</code> object with login url set if
     *  the present authentication context is not sufficient; login url set to
     *  <code>null</code> if the present authentication context is sufficient.
     *  Return <code>null</code> if it cannot be decided or appropriate
     *  authentication context cannot be obtained.
     */
    private FSAuthContextResult decideAuthnContext(
        String authContextMinRef,
        String presentAuthContext) 
    {
        FSUtils.debug.message(
            "FSAuthnDecisionHandler::decideAuthnContext. Entered method");
        if (authContextMinRef != null && idpAuthContextMap != null) {
            FSIDPAuthenticationContextInfo presentAuthObj = 
                (FSIDPAuthenticationContextInfo) idpAuthContextMap.get(
                    presentAuthContext);
            FSIDPAuthenticationContextInfo newAuthObj = 
                (FSIDPAuthenticationContextInfo)idpAuthContextMap.get(
                    authContextMinRef);
            if (presentAuthObj != null && newAuthObj != null) {
                if (presentAuthObj.getLevel() >= newAuthObj.getLevel()) {
                    if (FSUtils.debug.messageEnabled()) {
                        FSUtils.debug.message("FSAuthnDecisionHandler::"
                            + "decideAuthnContext.Present Auth Level" 
                            + " higher than needed.");
                    }
                    return new FSAuthContextResult();
                } else {
                    return getURLForAuthnContext(authContextMinRef);
                }
            } else {
                FSUtils.debug.error("FSAuthnDecisionHandler::decideAuthnContext"
                    +" Not Supported AuthContext");
                return null;
            }
        } else {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSAuthnDecisionHandler::decideAuthnContext."
                    + " Method called with authContextMinRef null");
            }
            return null;
        }
    }
}
