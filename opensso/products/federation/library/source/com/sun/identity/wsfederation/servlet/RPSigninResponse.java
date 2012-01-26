/**
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
 * $Id: RPSigninResponse.java,v 1.8 2009/12/14 23:42:48 mallas Exp $
 *
 */

package com.sun.identity.wsfederation.servlet;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.profile.SPACSUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.identity.wsfederation.common.WSFederationException;
import com.sun.identity.wsfederation.common.WSFederationUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.SPSSOConfigElement;
import com.sun.identity.wsfederation.logging.LogUtil;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import com.sun.identity.wsfederation.plugins.SPAccountMapper;
import com.sun.identity.wsfederation.plugins.SPAttributeMapper;
import com.sun.identity.wsfederation.profile.RequestSecurityTokenResponse;
import com.sun.identity.wsfederation.profile.RequestedSecurityToken;
import com.sun.identity.wsfederation.profile.SPCache;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * This class handles the sign-in response from the identity provider
 */
public class RPSigninResponse extends WSFederationAction {
    private static Debug debug = WSFederationUtils.debug;
    
    private String wresult;
    private String wctx;
    
    /** Creates a new instance of RPSigninResponse 
     * @param request HTTP Servlet request
     * @param response HTTP Servlet response
     * @param wresult wresult parameter from request
     * @param wctx wctx parameter from request
     */
    public RPSigninResponse(HttpServletRequest request,
        HttpServletResponse response, String wresult, String wctx) {
        super(request,response);
        this.wresult = wresult;
        this.wctx = wctx;
    }
    
    /**
     * Processes the sign-in response, redirecting the browser wreply URL 
     * supplied in the sign-in request via the HttpServletResponse passed to 
     * the constructor.
     */
    public void process() throws WSFederationException, IOException {
        String classMethod = "RPSigninResponse.process: ";
        
        if ((wresult == null) || (wresult.length() == 0)) {
            String[] data = {request.getQueryString()};
            LogUtil.error(Level.INFO,
                    LogUtil.MISSING_WRESULT,
                    data,
                    null);
            throw new WSFederationException(WSFederationUtils.bundle.
                getString("nullWresult"));
        }
        
        RequestSecurityTokenResponse rstr = null;
        try {
            rstr = RequestSecurityTokenResponse.parseXML(wresult);
        } catch (WSFederationException wsfe) {
            String[] data = {wresult};
            LogUtil.error(Level.INFO,
                    LogUtil.INVALID_WRESULT,
                    data,
                    null);
            throw new WSFederationException(WSFederationUtils.bundle.
                getString("invalidWresult"));
        }
        
        if ( debug.messageEnabled() ) {
            debug.message(classMethod +"Received RSTR: "
                    + rstr.toString());
        }
        
        String realm = null;
        
        String requestURL = request.getRequestURL().toString();
        // get entity id and orgName
        String metaAlias = 
            WSFederationMetaUtils.getMetaAliasByUri(requestURL);
        realm = WSFederationMetaUtils.getRealmByMetaAlias(metaAlias);
        WSFederationMetaManager metaManager =
            WSFederationUtils.getMetaManager();
        String spEntityId = null;
        try {
            spEntityId = metaManager.getEntityByMetaAlias(metaAlias);
        } catch (WSFederationException wsfe) {
            String[] data = {wsfe.getLocalizedMessage(), metaAlias, realm};
            LogUtil.error(Level.INFO,
                    LogUtil.CONFIG_ERROR_GET_ENTITY_CONFIG,
                    data,
                    null);
            String[] args = {metaAlias, realm};
            throw new WSFederationException(WSFederationConstants.BUNDLE_NAME,
                "invalidMetaAlias", args);
        }
        
        if (realm == null || realm.length() == 0) {
            realm = "/";
        }
        
        SPSSOConfigElement spssoconfig =
                metaManager.getSPSSOConfig(realm, spEntityId);

        int timeskew = SAML2Constants.ASSERTION_TIME_SKEW_DEFAULT;
        String timeskewStr = WSFederationMetaUtils.getAttribute(spssoconfig,
                SAML2Constants.ASSERTION_TIME_SKEW);
        if (timeskewStr != null && timeskewStr.trim().length() > 0) {
            timeskew = Integer.parseInt(timeskewStr);
            if (timeskew < 0) {
                timeskew = SAML2Constants.ASSERTION_TIME_SKEW_DEFAULT;
            }
        }
        if (debug.messageEnabled()) {
            debug.message(classMethod + "timeskew = " + timeskew);
        }

        // check Assertion and get back a Map of relevant data including,
        // Subject, SOAPEntry for the partner and the List of Assertions.
        if ( debug.messageEnabled() ) {
            debug.message(classMethod +" - verifying assertion");
        }
        
        // verifyToken will throw an exception, rather than return null, so we
        // need not test the return value
        Map<String,Object> smap = 
            rstr.getRequestedSecurityToken().verifyToken(realm, spEntityId, 
            timeskew);
        
        assert smap != null;
        
        Map attributes = WSFederationMetaUtils.getAttributes(spssoconfig);

        SPAccountMapper acctMapper = getSPAccountMapper(attributes);        
        SPAttributeMapper attrMapper = getSPAttributeMapper(attributes);
        
        String userName = acctMapper.getIdentity(rstr, spEntityId, realm);
        if ( userName == null ) {
            throw new WSFederationException(WSFederationUtils.bundle.
                getString("nullUserID"));
        }        

        String idpEntityId = metaManager.getEntityByTokenIssuerName(
            realm, rstr.getRequestedSecurityToken().getIssuer());
        List attrs = rstr.getRequestedSecurityToken().getAttributes();

        Map attrMap = null;
        if (attrs != null) {
            attrMap = attrMapper.getAttributes(attrs, userName,
               spEntityId, idpEntityId, realm);
        }
        
        String authLevel = 
            smap.get(SAML2Constants.AUTH_LEVEL).toString();
        
        // Set up Attributes for session creation
        Map sessionInfoMap = new HashMap();
        sessionInfoMap.put(SessionProvider.REALM, realm);
        sessionInfoMap.put(SessionProvider.PRINCIPAL_NAME, userName);        
        sessionInfoMap.put(SessionProvider.AUTH_LEVEL, authLevel);
        
        Object session = null;
        
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            session = sessionProvider.createSession(sessionInfoMap,
                    request, response, null);
            SPACSUtils.setAttrMapInSession(sessionProvider, attrMap, 
                session);

            String[] idpArray = {idpEntityId};
            sessionProvider.setProperty(session, 
                WSFederationConstants.SESSION_IDP, idpArray);
            RequestedSecurityToken rst = rstr.getRequestedSecurityToken();
            if(isAssertionCacheEnabled(spssoconfig)) {
               String tokenID = rst.getTokenId();
               String[] assertionID = {tokenID};
               sessionProvider.setProperty(session, "AssertionID", assertionID);
               SPCache.assertionByIDCache.put(tokenID, rst.toString());
            }
        } catch (SessionException se) {
            String[] data = {se.getLocalizedMessage(),realm, userName, 
                authLevel};
            LogUtil.error(Level.INFO,
                    LogUtil.CANT_CREATE_SESSION,
                    data,
                    null);
            throw new WSFederationException(se);
        }
        
        String target = null;
        if (wctx != null) {
            target = WSFederationUtils.removeReplyURL(wctx);
        } else {
            target = WSFederationMetaUtils.getAttribute(spssoconfig,
                SAML2Constants.DEFAULT_RELAY_STATE);
        }
        
        String[] data = {wctx, 
            LogUtil.isErrorLoggable(Level.FINER)? wresult :
                rstr.getRequestedSecurityToken().getTokenId(),
            realm,
            userName,
            authLevel,
            target};
        LogUtil.access(Level.INFO, LogUtil.SSO_SUCCESSFUL, data, session);

        if ( target == null )
        {
            // What to do? There was no wreply URL specified, and there is no
            // default target configured
            PrintWriter pw = response.getWriter();
            pw.println("Logged in");
            return;
        }

        response.sendRedirect(target);
    }
    
    private static SPAccountMapper getSPAccountMapper(
        Map attributes) throws WSFederationException {        
        SPAccountMapper acctMapper = null;
        List acctMapperList = (List)attributes.get(
            SAML2Constants.SP_ACCOUNT_MAPPER);
        if (acctMapperList != null) {
            try {
                acctMapper = (SPAccountMapper)
                    (Class.forName((String)acctMapperList.get(0)).
                     newInstance());
                if (debug.messageEnabled()) {
                    debug.message(
                        "RPSigninResponse.getSPAccountMapper: mapper = " +
                        (String)acctMapperList.get(0));
                }
            } catch (ClassNotFoundException cfe) {
                throw new WSFederationException(cfe);
            } catch (InstantiationException ie) {
                throw new WSFederationException(ie);
            } catch (IllegalAccessException iae) {
                throw new WSFederationException(iae);
            }
        }
        if (acctMapper == null) {
            throw new WSFederationException(
                WSFederationUtils.bundle.getString("failedAcctMapper"));
        }
        return acctMapper;
    }

    private SPAttributeMapper getSPAttributeMapper(Map attributes) 
        throws WSFederationException {
        SPAttributeMapper attrMapper = null;
        List attrMapperList = (List)attributes.get(
            SAML2Constants.SP_ATTRIBUTE_MAPPER);
        if (attrMapperList != null) {
            try {
                attrMapper = (SPAttributeMapper)
                    (Class.forName((String)attrMapperList.get(0)).
                     newInstance());
            } catch (ClassNotFoundException cfe) {
                throw new WSFederationException(cfe);
            } catch (InstantiationException ie) {
                throw new WSFederationException(ie);
            } catch (IllegalAccessException iae) {
                throw new WSFederationException(iae);
            }
        }
        if (attrMapper == null) {
            throw new WSFederationException(
                WSFederationUtils.bundle.getString("failedAttrMapper"));
        }
        return attrMapper;
    }
    
    /** Sets the attribute map in the session
     *
     *  @param sessionProvider Session provider
     *  @param attrMap the Attribute Map
     *  @param session the valid session object
     *  @throws com.sun.identity.plugin.session.SessionException 
     */
    public static void setAttrMapInSession(
        SessionProvider sessionProvider,
        Map attrMap, Object session)
        throws SessionException {
        if (attrMap != null && !attrMap.isEmpty()) {
            Set entrySet = attrMap.entrySet();
            for(Iterator iter = entrySet.iterator(); iter.hasNext();) {
                Map.Entry entry = (Map.Entry)iter.next();
                String attrName = (String)entry.getKey();
                Set attrValues = (Set)entry.getValue();
                if(attrValues != null && !attrValues.isEmpty()) {
                   sessionProvider.setProperty(
                       session, attrName,
                       (String[]) attrValues.toArray(
                       new String[attrValues.size()]));
                   if (WSFederationUtils.debug.messageEnabled()) {
                       WSFederationUtils.debug.message(
                           "SPACSUtils.setAttrMapInSession: AttrMap:" +
                           attrName + " , " + attrValues);
                   }
                }
            }
        }
    }

    private boolean isAssertionCacheEnabled(SPSSOConfigElement spssoconfig) {          
         String enabled = WSFederationMetaUtils.getAttribute(spssoconfig,
               SAML2Constants.ASSERTION_CACHE_ENABLED);
         if(enabled == null) {
            //TODO: until the console/cli is fixed for this attribute,
            // return true.
            return true;
         }
         return "true".equalsIgnoreCase(enabled) ? true : false;
    }
}
