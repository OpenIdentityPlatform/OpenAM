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
 * $Id: WSFederationUtils.java,v 1.6 2009/10/28 23:58:58 exu Exp $
 *
 * Portions Copyrighted 2015-2016 ForgeRock AS.
 */
package com.sun.identity.wsfederation.common;

import static org.forgerock.openam.utils.Time.*;

import com.sun.identity.multiprotocol.SingleLogoutManager;
import com.sun.identity.plugin.datastore.DataStoreProvider;
import com.sun.identity.plugin.datastore.DataStoreProviderException;
import com.sun.identity.plugin.datastore.DataStoreProviderManager;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml.assertion.NameIdentifier;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.shared.DateUtils;
import com.sun.identity.wsfederation.jaxb.entityconfig.IDPSSOConfigElement;
import com.sun.identity.wsfederation.meta.WSFederationMetaException;
import java.io.IOException;
import java.util.Collections;
import java.text.ParseException;
import java.util.List;
import java.util.Map;
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
import com.sun.identity.wsfederation.meta.WSFederationMetaUtils;
import com.sun.identity.wsfederation.plugins.IDPAccountMapper;
import com.sun.identity.wsfederation.plugins.IDPAttributeMapper;
import com.sun.identity.wsfederation.plugins.whitelist.ValidWReplyExtractor;
import com.sun.identity.wsfederation.profile.SAML11RequestedSecurityToken;

import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.ResourceBundle;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.forgerock.openam.shared.security.whitelist.RedirectUrlValidator;
import org.forgerock.openam.utils.StringUtils;

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


    private static final RedirectUrlValidator<ValidWReplyExtractor.WSFederationEntityInfo> WREPLY_VALIDATOR =
            new RedirectUrlValidator<ValidWReplyExtractor.WSFederationEntityInfo>(new ValidWReplyExtractor());

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
                signedXMLString, id, Collections.singleton(cert));
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

        long timeNow = currentTimeMillis();
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


    /**
     * Convenience method to validate a WSFederation wreply URL, often called from a JSP.
     *
     * @param request    Used to help establish the realm and hostEntityID.
     * @param relayState The URL to validate.
     * @return <code>true</code> if the wreply is valid.
     */
    public static boolean isWReplyURLValid(HttpServletRequest request, String relayState) {
        String metaAlias = WSFederationMetaUtils.getMetaAliasByUri(request.getRequestURI());

        try {
            WSFederationMetaManager metaManager = new WSFederationMetaManager();
            return isWReplyURLValid(metaAlias, relayState, metaManager.getRoleByMetaAlias(metaAlias));
        } catch (WSFederationMetaException e) {
            debug.warning("Can't get metaManager.", e);
            return false;
        }
    }

    /**
     * Convenience method to validate a WSFederation wreply URL, often called from a JSP.
     *
     * @param metaAlias  The metaAlias of the hosted entity.
     * @param wreply The URL to validate.
     * @param role       The role of the caller.
     * @return <code>true</code> if the wreply is valid.
     */
    public static boolean isWReplyURLValid(String metaAlias, String wreply, String role) {
        boolean result = false;

        if (metaAlias != null) {
            String realm = WSFederationMetaUtils.getRealmByMetaAlias(metaAlias);
            try {
                String hostEntityID = WSFederationUtils.getMetaManager().getEntityByMetaAlias(metaAlias);
                if (hostEntityID != null) {
                    validateWReplyURL(realm, hostEntityID, wreply, role);
                    result = true;
                }
            } catch (WSFederationException e) {
                if (debug.messageEnabled()) {
                    debug.message("WSFederationUtils.isWReplyURLValid(): wreply " + wreply +
                            " for role " + role + " triggered an exception: " + e.getMessage(), e);
                }
                result = false;
            }
        }

        if (debug.messageEnabled()) {
            debug.message("WSFederationUtils.isWReplyURLValid(): wreply " + wreply +
                    " for role " + role + " was valid? " + result);
        }

        return result;
    }

    /**
     * Validates the Wreply URL against a list of wreply State
     * URLs created on the hosted service provider.
     *
     * @param orgName      realm or organization name the provider resides in.
     * @param hostEntityId Entity ID of the hosted provider.
     * @param wreply       wreply URL.
     * @param role         IDP/SP Role.
     * @throws WSFederationException if the processing failed.
     */
    public static void validateWReplyURL(
            String orgName,
            String hostEntityId,
            String wreply,
            String role) throws WSFederationException {

        // Check for the validity of the RelayState URL.
        if (wreply != null && !wreply.isEmpty()) {
            if (!WREPLY_VALIDATOR.isRedirectUrlValid(wreply,
                    ValidWReplyExtractor.WSFederationEntityInfo.from(orgName, hostEntityId, role))) {
                throw new WSFederationException(WSFederationUtils.bundle.getString("invalidWReplyUrl"));
            }
        }
    }

    /**
     * Creates a SAML 1.1 token object based on the provided details.
     *
     * @param realm The realm of the WS-Fed entities
     * @param idpEntityId The WS-Fed IdP (IP) entity ID.
     * @param spEntityId The WS-Fed SP (RP) entity ID.
     * @param session The authenticated session object.
     * @param spTokenIssuerName The name of the token issuer corresponding to the SP (RP).
     * @param authMethod The authentication method to specify in the AuthenticationStatement.
     * @param wantAssertionSigned Whether the assertion should be signed.
     * @return A SAML1.1 token.
     * @throws WSFederationException If there was an error while creating the SAML1.1 token.
     */
    public static SAML11RequestedSecurityToken createSAML11Token(String realm, String idpEntityId, String spEntityId,
            Object session, String spTokenIssuerName, String authMethod, boolean wantAssertionSigned)
            throws WSFederationException {
        final IDPSSOConfigElement idpConfig = metaManager.getIDPSSOConfig(realm, idpEntityId);
        if (idpConfig == null) {
            debug.error("Cannot find configuration for IdP " + idpEntityId);
            throw new WSFederationException(WSFederationUtils.bundle.getString("unableToFindIDPConfiguration"));
        }

        String authSSOInstant;
        try {
            authSSOInstant = WSFederationUtils.sessionProvider.getProperty(session, SessionProvider.AUTH_INSTANT)[0];
        } catch (SessionException se) {
            throw new WSFederationException(se);
        }

        IDPAttributeMapper attrMapper = getIDPAttributeMapper(WSFederationMetaUtils.getAttributes(idpConfig));
        IDPAccountMapper accountMapper = getIDPAccountMapper(WSFederationMetaUtils.getAttributes(idpConfig));

        List attributes = attrMapper.getAttributes(session, idpEntityId, spEntityId, realm);

        final Date authInstant;
        if (StringUtils.isEmpty(authSSOInstant)) {
            authInstant = newDate();
        } else {
            try {
                authInstant = DateUtils.stringToDate(authSSOInstant);
            } catch (ParseException pe) {
                throw new WSFederationException(pe);
            }
        }

        NameIdentifier nameIdentifier = accountMapper.getNameID(session, realm, idpEntityId, spEntityId);

        int notBeforeSkew = WSFederationMetaUtils.getIntAttribute(idpConfig,
                SAML2Constants.ASSERTION_NOTBEFORE_SKEW_ATTRIBUTE, SAML2Constants.NOTBEFORE_ASSERTION_SKEW_DEFAULT);

        int effectiveTime = WSFederationMetaUtils.getIntAttribute(idpConfig,
                SAML2Constants.ASSERTION_EFFECTIVE_TIME_ATTRIBUTE, SAML2Constants.ASSERTION_EFFECTIVE_TIME);

        String certAlias = WSFederationMetaUtils.getAttribute(idpConfig, SAML2Constants.SIGNING_CERT_ALIAS);

        if (wantAssertionSigned && certAlias == null) {
            // SP wants us to sign the assertion, but we don't have a signing cert
            debug.error("SP wants signed assertion, but no signing cert is configured");
            throw new WSFederationException(WSFederationUtils.bundle.getString("noIdPCertAlias"));
        }

        if (!wantAssertionSigned) {
            // SP doesn't want us to sign the assertion, so pass null certAlias to indicate no assertion signature
            // required
            certAlias = null;
        }

        return new SAML11RequestedSecurityToken(realm, spTokenIssuerName, idpEntityId,
                notBeforeSkew, effectiveTime, certAlias, authMethod, authInstant,
                nameIdentifier, attributes);
    }

    private static IDPAccountMapper getIDPAccountMapper(Map<String, List<String>> attributes)
            throws WSFederationException {
        IDPAccountMapper accountMapper = null;
        List<String> accountMapperList = attributes.get( SAML2Constants.IDP_ACCOUNT_MAPPER);
        if (accountMapperList != null) {
            try {
                accountMapper = Class.forName(accountMapperList.get(0)).asSubclass(IDPAccountMapper.class)
                        .newInstance();
            } catch (ReflectiveOperationException roe) {
                throw new WSFederationException(roe);
            }
        }
        if (accountMapper == null) {
            throw new WSFederationException(WSFederationUtils.bundle.getString("failedAcctMapper"));
        }
        return accountMapper;
    }

    private static IDPAttributeMapper getIDPAttributeMapper(Map<String, List<String>> attributes)
            throws WSFederationException {
        IDPAttributeMapper attrMapper = null;
        List<String> attrMapperList = attributes.get(SAML2Constants.IDP_ATTRIBUTE_MAPPER);
        if (attrMapperList != null) {
            try {
                attrMapper = Class.forName(attrMapperList.get(0)).asSubclass(IDPAttributeMapper.class).newInstance();
            } catch (ReflectiveOperationException roe) {
                throw new WSFederationException(roe);
            }
        }
        if (attrMapper == null) {
            throw new WSFederationException(WSFederationUtils.bundle.getString("failedAttrMapper"));
        }
        return attrMapper;
    }
}
