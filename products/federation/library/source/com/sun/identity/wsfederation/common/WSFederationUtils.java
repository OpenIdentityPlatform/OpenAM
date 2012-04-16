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
 * $Id: WSFederationUtils.java,v 1.6 2009/10/28 23:58:58 exu Exp $
 *
 */

package com.sun.identity.wsfederation.common;

import com.sun.identity.multiprotocol.SingleLogoutManager;
import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.datastore.DataStoreProviderManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import java.io.IOException;
import java.util.logging.Level;

import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.locale.Locale;

import com.sun.identity.saml.assertion.Assertion;
import com.sun.identity.saml.xmlsig.XMLSignatureManager; 
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.xmlsig.SigManager;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.key.KeyUtil;
import com.sun.identity.wsfederation.logging.LogUtil;
import com.sun.identity.wsfederation.meta.WSFederationMetaManager;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Utility methods for WS-Federation implementation.
 */
public class WSFederationUtils {    
    /**
     * <code>Debug</code> instance for use by WS-Federation implementation.
     */
    public static Debug debug = 
        Debug.getInstance(WSFederationConstants.AM_WSFEDERATION);
    /**
     * Resource bundle for the WS-Federation implementation.
     */
    public static ResourceBundle bundle = Locale.
        getInstallResourceBundle(WSFederationConstants.BUNDLE_NAME);    
    /*
     * Map from reply URL to wctx parameter.
     */
    private static HashMap wctxMap = new HashMap();
    
    private static WSFederationMetaManager metaManager = null;

    public static DataStoreProvider dsProvider;
    
    public static SessionProvider sessionProvider = null;
    
    static {
        String classMethod = "WSFederationUtils static initializer: ";
        try {
            DataStoreProviderManager dsManager =
                    DataStoreProviderManager.getInstance();
            dsProvider = dsManager.getDataStoreProvider(
                WSFederationConstants.WSFEDERATION);
        } catch (DataStoreProviderException dse) {
            debug.error(classMethod + "DataStoreProviderException : ", dse);
            throw new ExceptionInInitializerError(dse);
        }

        try {
            sessionProvider = SessionManager.getProvider();
        } catch (SessionException se) {
            debug.error( classMethod + "Error getting SessionProvider.", se);
            throw new ExceptionInInitializerError(se);
        }                   
        try {
            metaManager = new WSFederationMetaManager();
        } catch (WSFederationMetaException we) {
            debug.error( classMethod + "Error getting meta service.", we);
            throw new ExceptionInInitializerError(we);
        }
    }
    
    /*
     * Private constructor ensure that no instance is ever created
     */
    private WSFederationUtils() {
    }

    /**
     * Returns an instance of <code>WSFederationMetaManager</code>.
     * @return an instance of <code>WSFederationMetaManager</code>.
     */
    public static WSFederationMetaManager getMetaManager() {
        return metaManager;
    }

    /**
     * Extracts the home account realm from the user agent HTTP header.
     * @param uaHeader user agent HTTP header. User agent header must be 
     * semi-colon separated, of the form <code>Mozilla/4.0 (compatible; 
     * MSIE 6.0; Windows NT 5.1; SV1; .NET CLR 1.1.4322; InfoPath.1; 
     * amWSFederationAccountRealm:Adatum Corp)</code>.
     * @param accountRealmCookieName identifier with which to search user agent
     * HTTP header.
     * @return the home account realm name.
     */
    public static String accountRealmFromUserAgent( String uaHeader, 
        String accountRealmCookieName )
    {
        String classMethod = "WSFederationUtils.accountRealmFromUserAgent";
        
        // UA String is of form "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 
        // 5.1; SV1; .NET CLR 1.1.4322; InfoPath.1; 
        // amWSFederationAccountRealm:Adatum Corp)"
        int leftBracket = uaHeader.indexOf('(');
        if ( leftBracket == -1 ) {
            if (debug.warningEnabled()) {
                debug.warning(classMethod + "Can't find left bracket");
            }
            return null;
        }
        
        int rightBracket = uaHeader.lastIndexOf(')');
        if ( rightBracket == -1 || rightBracket < leftBracket ) {
            if (debug.warningEnabled()) {
                debug.warning(classMethod + "Can't find right bracket");
            }
            return null;
        }
        
        String insideBrackets = uaHeader.substring(leftBracket+1,rightBracket);
        if ( insideBrackets.length() == 0 ) {
            if (debug.warningEnabled()) {
                debug.warning(classMethod + "zero length between brackets");
            }
            return null;
        }
        
        // insideBrackets is of form "compatible; MSIE 6.0; Windows NT 5.1; SV1; 
        // .NET CLR 1.1.4322; InfoPath.1; 
        // amWSFederationAccountRealm:Adatum Corp"
        
        // Split string on matches of any amount of whitespace surrounding a 
        // semicolon
        String uaFields[] = insideBrackets.split("[\\s]*;[\\s]*");
        if ( uaFields == null ) {
            if (debug.warningEnabled()) {
                debug.warning(classMethod + "zero length between brackets");
            }
            return null;
        }
        
        // uaFields[] is of form {"compatible", "MSIE 6.0", "Windows NT 5.1", 
        // "SV1", ".NET CLR 1.1.4322", "InfoPath.1", 
        // "amWSFederationAccountRealm:Adatum Corp"}
        
        for ( int i = 0; i < uaFields.length; i++ ) {
            if ( uaFields[i].indexOf(accountRealmCookieName) != -1 ) {
                // Split this field on matches of any amount of whitespace 
                // surrounding a colon
                String keyValue[] = uaFields[i].split("[\\s]*:[\\s]*");
                if ( keyValue.length < 2 ) {
                    if (debug.warningEnabled()) {
                        debug.warning(classMethod + 
                            "can't see accountRealm in " + uaFields[i]);
                    }
                    return null;
                }
                
                if ( ! keyValue[0].equals(accountRealmCookieName)) {
                    if (debug.warningEnabled()) {
                        debug.warning(classMethod + "can't understand " + 
                            uaFields[i]);
                    }
                    return null;
                }
                
                return keyValue[1];
            }
        }
        
        return null;
    }

    /**
     * Put a reply URL in the wctx-&gt;wreply map.
     * @param wreply reply URL
     * @return value for WS-Federation context parameter (wctx).
     */
    public static String putReplyURL(String wreply) {
        String wctx = SAML2Utils.generateID();
        synchronized (wctxMap)
        {
            wctxMap.put(wctx,wreply);
        }
        return wctx;
    }

    /**
     * Remove and return a reply URL from the wctx-&gt;wreply map.
     * @param wctx WS-Federation context parameter
     * @return reply URL
     */
    public static String removeReplyURL(String wctx) {
        String wreply = null;
        synchronized (wctxMap)
        {
            wreply = (String) wctxMap.remove(wctx);
        }
        return wreply;
    }

    /**
     * Determine the validity of the signature on the <code>Assertion</code>
     * @param assertion SAML 1.1 Assertion
     * @param realm Realm for the issuer
     * @param issuer Assertion issuer - used to retrieve certificate for 
     * signature validation.
     * @return true if the signature on the object is valid; false otherwise.
     */
    public static boolean isSignatureValid(Assertion assertion, String realm, 
        String issuer)
    {
        boolean valid = false;

        String signedXMLString = assertion.toString(true,true);
        String id = assertion.getAssertionID();
        
        try {
            FederationElement idp = 
                metaManager.getEntityDescriptor(realm, issuer);
            X509Certificate cert = KeyUtil.getVerificationCert(idp, issuer, 
                true);
            XMLSignatureManager manager = XMLSignatureManager.getInstance();
            valid = SigManager.getSigInstance().verify(
                signedXMLString, id, cert);
        } catch (WSFederationMetaException ex) {
            valid = false;
        } catch (SAML2Exception ex) {
            valid = false;
        }
        
        if ( ! valid )
        {
            String[] data = {LogUtil.isErrorLoggable(Level.FINER) ? 
                signedXMLString : id,
                realm, issuer
            };
            LogUtil.error(Level.INFO,
                    LogUtil.INVALID_SIGNATURE_ASSERTION,
                    data,
                    null);
        }
        return valid;
    }
    
    /**
     * Determines the timeliness of the assertion.
     * @param assertion SAML 1.1 Assertion
     * @param timeskew in seconds
     * @return true if the current time is after the Assertion's notBefore time
     * - timeskew AND the current time is before the Assertion's notOnOrAfter 
     * time + timeskew
     */
    public static boolean isTimeValid(Assertion assertion, int timeskew)
    {
        String classMethod = "WSFederationUtils.isTimeValid: ";
        
        long timeNow = System.currentTimeMillis();
        Date notOnOrAfter = assertion.getConditions().getNotOnorAfter();
        String assertionID = assertion.getAssertionID();
        if (notOnOrAfter == null ) {
            String[] data = {LogUtil.isErrorLoggable(Level.FINER) ? 
                assertion.toString(true,true) : assertionID};
            LogUtil.error(Level.INFO,
                    LogUtil.MISSING_CONDITIONS_NOT_ON_OR_AFTER,
                    data,
                    null);
            return false;
        } else if ((notOnOrAfter.getTime() + timeskew * 1000) < timeNow ) {
            String[] data = {LogUtil.isErrorLoggable(Level.FINER) ? 
                assertion.toString(true,true) : assertionID,
                notOnOrAfter.toString(), 
                Integer.toString(timeskew),
                (new Date(timeNow)).toString()};
            LogUtil.error(Level.INFO,
                    LogUtil.ASSERTION_EXPIRED,
                    data,
                    null);
            return false;
        }
        Date notBefore = assertion.getConditions().getNotBefore();
        if ( notBefore == null ) {
            String[] data = {LogUtil.isErrorLoggable(Level.FINER) ? 
                assertion.toString(true,true) : assertionID};
            LogUtil.error(Level.INFO,
                    LogUtil.MISSING_CONDITIONS_NOT_BEFORE,
                    data,
                    null);
            return false;
        } else if ((notBefore.getTime() - timeskew * 1000) > timeNow ) {
            String[] data = {LogUtil.isErrorLoggable(Level.FINER) ? 
                assertion.toString(true,true) : assertionID,
                notBefore.toString(), 
                Integer.toString(timeskew),
                (new Date(timeNow)).toString()};
            LogUtil.error(Level.INFO,
                    LogUtil.ASSERTION_NOT_YET_VALID,
                    data,
                    null);
            return false;
        }
        return true;
    }


    /**
     * Processes Single Logout cross multiple federation protocols
     * @param request HttpServletRequest object.
     * @param response HttpServletResponse object
     */
    public static void processMultiProtocolLogout(HttpServletRequest request,
        HttpServletResponse response, Object userSession) {
        debug.message("WSFederationUtils.processMPSingleLogout");
        try {
            String wreply = (String)
                request.getAttribute(WSFederationConstants.LOGOUT_WREPLY);
            String realm = (String)
                request.getAttribute(WSFederationConstants.REALM_PARAM);
            String idpEntityId = (String)
                request.getAttribute(WSFederationConstants.ENTITYID_PARAM);
            Set sessSet = new HashSet();
            sessSet.add(userSession);
            String sessUser = 
                SessionManager.getProvider().getPrincipalName(userSession); 
            // assume WS-Federation logout always succeed as there is not
            // logout status from the specification
            SingleLogoutManager manager = SingleLogoutManager.getInstance();
            // TODO : find out spEntityID/logout request if any
            int status = manager.doIDPSingleLogout(sessSet, sessUser,
                request, response, false, true, SingleLogoutManager.WS_FED, 
                realm, idpEntityId, null, wreply, null, null, 
                SingleLogoutManager.LOGOUT_SUCCEEDED_STATUS);
            if (status != SingleLogoutManager.LOGOUT_REDIRECTED_STATUS) {
                response.sendRedirect(wreply);
            }
        } catch (SessionException ex) {
            // ignore;
            debug.message("WSFederationUtils.processMultiProtocolLogout", ex);
        } catch (IOException ex) {
            // ignore;
            debug.message("WSFederationUtils.processMultiProtocolLogout", ex);
        } catch (Exception ex) {
            // ignore;
            debug.message("WSFederationUtils.processMultiProtocolLogout", ex);
        }
    }
}
