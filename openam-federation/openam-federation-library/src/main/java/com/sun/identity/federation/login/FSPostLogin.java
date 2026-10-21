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
 * $Id: FSPostLogin.java,v 1.6 2008/07/31 00:55:33 exu Exp $
 *
 * Portions Copyrighted 2025-2026 3A Systems LLC.
 */

package com.sun.identity.federation.login;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.Enumeration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import com.sun.identity.cot.COTConstants;
import com.sun.identity.cot.CircleOfTrustDescriptor;
import com.sun.identity.cot.CircleOfTrustManager;
import com.sun.identity.cot.COTException;

import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.common.FSUtils;

import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.federation.meta.IDFFMetaUtils;

import com.sun.identity.federation.services.FSLoginHelper;
import com.sun.identity.federation.services.FSLoginHelperException;
import com.sun.identity.federation.services.util.FSServiceUtils;
import com.sun.identity.federation.services.FSSessionManager;
import com.sun.identity.federation.services.FSSession;
import com.sun.identity.federation.services.logout.FSTokenListener;

import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;

import com.sun.identity.shared.encode.URLEncDec;

import com.sun.liberty.LibertyManager;

import org.forgerock.openam.shared.security.whitelist.RedirectUrlValidator;
import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;
import org.openidentityplatform.openam.federation.plugins.ValidRelayStateExtractor;
import org.openidentityplatform.openam.federation.plugins.ValidRelayStateExtractor.ConfigurationUnavailableException;
import org.openidentityplatform.openam.federation.plugins.ValidRelayStateExtractor.IDFFEntityInfo;

/**
 * This class defines methods which will be invoked post
 * Single Sign-On/Federation.
 */

public class FSPostLogin {
    
    private String federationPage = null;
    private String consentPage = null;
    private String errorPage = null;
    private static final ValidRelayStateExtractor RELAY_STATE_EXTRACTOR =
        new ValidRelayStateExtractor();
    private boolean isIDP = false;
    private String providerRole = null;
    private String entityID = null;
    private String realm = null;

    /**
     * Resolved per call rather than cached in a static initialiser. {@code getIDFFMetaManager}
     * returns null while the SMS is unreachable and retries on the next call, so caching the
     * first answer would send every subsequent /postLogin to the error page until the JVM was
     * restarted.
     */
    private static IDFFMetaManager getMetaManager() {
        return FSUtils.getIDFFMetaManager();
    }

    /**
     * Fail-closed RelayState check mirroring {@code SAML2Utils.isRelayStateURLValid}: valid only
     * when the provider entity resolved AND the URL passes that provider's allow-list. An
     * unresolved provider (null entityID/role - e.g. a metaAlias that did not resolve) is never
     * valid, so an absent allow-list cannot be abused via a non-resolving metaAlias.
     * <p>
     * Unlike the shared {@code RedirectUrlValidator}, an entity that configures no
     * <code>relayStateUrlList</code> does not permit everything here. An empty list is the
     * state every ID-FF entity is in before one is configured, so reading it as "unrestricted"
     * would leave the guard off across the whole deployment on upgrade - and the caller names
     * the entity whose list is consulted through a <code>metaAlias</code> request parameter, so
     * one unconfigured entity anywhere would re-open the redirect for every endpoint. It is
     * read as "targets this deployment serves itself" - as its configuration names them, see
     * {@link FSUtils#isDeploymentURL} - which needs no configuration to be safe; an off-host
     * RelayState requires the list to name it.
     *
     * @param realm the realm of the hosted provider.
     * @param entityID the hosted provider's entity ID, <code>null</code> if it did not resolve.
     * @param role the hosted provider's role, <code>null</code> if it did not resolve.
     * @param relayState the caller supplied RelayState.
     * @return <code>true</code> when the RelayState may be used as a target.
     */
    static boolean isRelayStateURLValid(String realm, String entityID, String role,
        String relayState) {

        if (entityID == null || role == null) {
            return false;
        }
        if (!isAbsoluteHttpURL(relayState)) {
            // RedirectUrlValidator returns true for any non-absolute URL before the allow-list
            // is consulted, so without this "/x" and "//evil.example/x" skip the list entirely.
            // An ID-FF RelayState becomes a forwardRequest target, which needs an absolute URL
            // anyway, so a relative one has nothing to fall back to.
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("FSPostLogin::isRelayStateURLValid RelayState \""
                    + FSUtils.forLog(relayState) + "\" is not an absolute http(s) URL; rejecting");
            }
            return false;
        }
        final IDFFEntityInfo entityInfo = IDFFEntityInfo.from(realm, entityID, role);
        Collection<String> patterns;
        try {
            // Read here rather than left to the validator so that "this entity configures no
            // allow-list" is a decision this method makes, not one RedirectUrlValidator makes.
            // The validator still applies the length, scheme and pattern-matching rules the
            // SAML2 path applies, over the list already in hand.
            patterns = RELAY_STATE_EXTRACTOR.extractValidDomains(entityInfo);
            if (!new RedirectUrlValidator<IDFFEntityInfo>(
                    new ResolvedDomains(patterns)).isRedirectUrlValid(relayState, entityInfo)) {
                return false;
            }
        } catch (ConfigurationUnavailableException cue) {
            // The allow-list could not be read: reject rather than treat an unreadable
            // configuration as an entity that configures no list.
            FSUtils.debug.warning("FSPostLogin::isRelayStateURLValid unable to read the "
                + "RelayState allow-list; rejecting", cue);
            return false;
        } catch (RuntimeException re) {
            // Anything else escaping the metadata layer would surface as an HTTP 500 on an
            // unauthenticated endpoint. Reject and let the caller fall back to the home URL.
            FSUtils.debug.warning("FSPostLogin::isRelayStateURLValid RelayState check failed; "
                + "rejecting", re);
            return false;
        }
        if (patterns != null && !patterns.isEmpty()) {
            return true;
        }
        if (FSUtils.isDeploymentURL(relayState)) {
            return true;
        }
        if (FSUtils.debug.warningEnabled()) {
            FSUtils.debug.warning("FSPostLogin::isRelayStateURLValid entity "
                + FSUtils.forLog(entityID) + " configures no " + IFSConstants.RELAY_STATE_URL_LIST
                + ", so only this deployment's own targets are permitted; rejecting \""
                + FSUtils.forLog(relayState) + "\"");
        }
        return false;
    }

    /** Hands {@code RedirectUrlValidator} an allow-list that has already been read. */
    private static final class ResolvedDomains implements ValidDomainExtractor<IDFFEntityInfo> {

        private final Collection<String> patterns;

        private ResolvedDomains(final Collection<String> patterns) {
            this.patterns = patterns;
        }

        @Override
        public Collection<String> extractValidDomains(final IDFFEntityInfo entityInfo) {
            return patterns;
        }
    }

    /**
     * Checks a RelayState taken straight from a request parameter against the allow-list of the
     * provider the request's metaAlias resolves to, and falls back to that provider's home URL
     * when it is not permitted. For the servlets that forward an LRURL without going through
     * {@link #doPostLogin}, which would otherwise leave the allow-list trivially bypassable.
     *
     * @param request the request carrying the metaAlias.
     * @param relayState the request supplied RelayState, <code>null</code> if none was sent.
     * @return <code>relayState</code> when the provider's allow-list permits it, otherwise that
     *  provider's home URL, which is <code>null</code> when nothing safe could be resolved.
     */
    static String checkedRelayState(HttpServletRequest request, String relayState) {
        ProviderRef provider = resolveProvider(request);
        // an absent one is not checked: nothing was refused, and it gets the home URL
        // doPostLogin substitutes for it
        if (relayState != null && !relayState.isEmpty()) {
            if (isRelayStateURLValid(
                provider.realm, provider.entityID, provider.role, relayState)) {
                return relayState;
            }
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("FSPostLogin::checkedRelayState RelayState \""
                    + FSUtils.forLog(relayState) + "\" rejected for metaAlias \""
                    + FSUtils.forLog(provider.metaAlias) + "\"; using provider home URL");
            }
        }
        if (provider.entityID == null || provider.role == null) {
            return null;
        }
        return LibertyManager.getHomeURL(provider.realm, provider.entityID, provider.role);
    }

    /**
     * Checks a caller supplied URL against the allow-list of the provider the request's
     * metaAlias resolves to, without substituting anything for it. For the callers that have
     * their own fallback and must not be sent to the provider home URL.
     *
     * @param request the request carrying the metaAlias.
     * @param relayState the request supplied URL.
     * @return <code>true</code> when the provider's allow-list permits the URL.
     */
    static boolean isRelayStateAllowed(HttpServletRequest request, String relayState) {
        ProviderRef provider = resolveProvider(request);
        return isRelayStateURLValid(
            provider.realm, provider.entityID, provider.role, relayState);
    }

    /**
     * Checks a RelayState a caller supplied for an AuthnRequest. The AuthnRequest carries it to
     * the IdP and back, and the assertion consumer redirects to it a request later without
     * checking it again, so it is checked here, before it is stored.
     *
     * @param request the request carrying the metaAlias.
     * @param relayState the request supplied RelayState.
     * @return <code>relayState</code> when the provider's allow-list permits it, otherwise
     *  <code>null</code>: a refused RelayState is treated as one the caller did not send.
     */
    public static String allowedRelayState(HttpServletRequest request, String relayState) {
        ProviderRef provider = resolveProvider(request);
        return allowedRelayState(provider.realm, provider.entityID, provider.role, relayState);
    }

    /**
     * Checks a RelayState a caller supplied against the allow-list of a hosted provider already
     * resolved, for the callers that resolved it themselves.
     *
     * @param realm the realm of the hosted provider.
     * @param entityID the hosted provider's entity ID, <code>null</code> if it did not resolve.
     * @param role the hosted provider's role, <code>null</code> if it did not resolve.
     * @param relayState the request supplied RelayState.
     * @return <code>relayState</code> when the provider's allow-list permits it, otherwise
     *  <code>null</code>: a refused RelayState is treated as one the caller did not send.
     */
    public static String allowedRelayState(String realm, String entityID, String role,
        String relayState) {

        if (relayState == null || relayState.isEmpty()) {
            // absent: nothing was refused, and each caller has its own default for it
            return relayState;
        }
        if (isRelayStateURLValid(realm, entityID, role, relayState)) {
            return relayState;
        }
        if (FSUtils.debug.warningEnabled()) {
            FSUtils.debug.warning("FSPostLogin::allowedRelayState RelayState \""
                + FSUtils.forLog(relayState) + "\" is not permitted by the provider's "
                + "RelayState allow-list; treating it as absent");
        }
        return null;
    }

    /**
     * Resolves the hosted provider a request's metaAlias names. Every field stays
     * <code>null</code> when the configuration could not be read, which
     * {@link #isRelayStateURLValid} rejects: an unreadable configuration must not be read as
     * "this provider configures no allow-list".
     */
    private static ProviderRef resolveProvider(HttpServletRequest request) {
        String metaAlias = request.getParameter(IFSConstants.META_ALIAS);
        if (metaAlias == null) {
            metaAlias = FSServiceUtils.getMetaAlias(request);
        }
        String role = null;
        String entityID = null;
        String realm = null;
        IDFFMetaManager metaManager = getMetaManager();
        if (metaManager != null) {
            try {
                role = metaManager.getProviderRoleByMetaAlias(metaAlias);
                entityID = metaManager.getEntityIDByMetaAlias(metaAlias);
                realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
            } catch (Exception ex) {
                FSUtils.debug.error("FSPostLogin::resolveProvider could not resolve the provider "
                    + "for metaAlias \"" + FSUtils.forLog(metaAlias) + "\"", ex);
            }
        }
        return new ProviderRef(metaAlias, realm, entityID, role);
    }

    /** The hosted provider a metaAlias resolved to, or nulls when it did not resolve. */
    private static final class ProviderRef {

        private final String metaAlias;
        private final String realm;
        private final String entityID;
        private final String role;

        private ProviderRef(String metaAlias, String realm, String entityID, String role) {
            this.metaAlias = metaAlias;
            this.realm = realm;
            this.entityID = entityID;
            this.role = role;
        }
    }

    /**
     * Tells whether a URL is absolute and carries an http(s) scheme.
     * @param url the URL to test, may be <code>null</code>.
     * @return <code>true</code> when it is an absolute http or https URL.
     */
    private static boolean isAbsoluteHttpURL(String url) {
        if (url == null) {
            return false;
        }
        try {
            URI uri = new URI(url);
            if (!uri.isAbsolute()) {
                return false;
            }
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (URISyntaxException use) {
            return false;
        }
    }


    /**
     * Retreives and forwards request to URL after login.
     *
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object.
     */
    public void doPostLogin(HttpServletRequest request,
        HttpServletResponse response) 
    {
        String metaAlias = request.getParameter(IFSConstants.META_ALIAS);
        String sso = request.getParameter(IFSConstants.SSOKEY);
        String lrURL = request.getParameter(IFSConstants.LRURL);
        String showFederatePage =
            request.getParameter(IFSConstants.FEDERATEKEY);
        String returnURL = null;
        
        if (metaAlias == null) {
            metaAlias = FSServiceUtils.getMetaAlias(request);
            String rqst = (new StringBuffer())
                .append(request.getRequestURL().toString())
                .append(IFSConstants.QUESTION_MARK)
                .append(request.getQueryString()).toString();
            lrURL = getValueFromURL(rqst, IFSConstants.LRURL);
            sso = getValueFromURL(rqst, IFSConstants.SSOKEY);
            // this is for LECP, we need to map the random id back to
            // original URL stored in session manager
            FSSessionManager sessMgr = FSSessionManager.getInstance(metaAlias);
            String relayStateURL = sessMgr.getRelayState(lrURL);
            if (relayStateURL != null) {
                sessMgr.removeRelayState(lrURL);
                lrURL = relayStateURL;
            }
        }
        
        try {
            setMetaInfo(metaAlias,request);
        } catch (FSPostLoginException fsexp) {
            sendResponse(request, response,errorPage);
            return;
        }

        // The home URL is the configured fallback, so it is trusted and never checked against
        // the allow-list - a provider whose own home page is not listed would otherwise be
        // rejected, and logged as rejected, on every ordinary login.
        if (lrURL == null || lrURL.length() <= 0) {
            lrURL = LibertyManager.getHomeURL(realm, entityID, providerRole);
        } else if (!isRelayStateURLValid(realm, entityID, providerRole, lrURL)) {
            // Reject any RelayState not permitted by the provider's allow-list before it becomes
            // the forward/redirect target (mirrors SAML2's isRelayStateURLValid). Fail-closed: an
            // unresolved provider yields a null home URL and is sent to the error page below.
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("FSPostLogin::doPostLogin RelayState \"" + FSUtils.forLog(lrURL)
                    + "\" rejected for entity " + FSUtils.forLog(entityID) + "; using provider home URL");
            }
            lrURL = LibertyManager.getHomeURL(realm, entityID, providerRole);
        }
        if (lrURL == null || lrURL.length() <= 0) {
            // Unresolved provider, or no home URL configured: nothing safe to forward to.
            if (FSUtils.debug.warningEnabled()) {
                FSUtils.debug.warning("FSPostLogin::doPostLogin no valid return URL for metaAlias \""
                    + FSUtils.forLog(metaAlias) + "\"; sending error page");
            }
            sendResponse(request, response, errorPage);
            return;
        }
        if ((sso != null  && sso.length() > 0
                && sso.equalsIgnoreCase(IFSConstants.SSOVALUE)) ||
             isIDP) 
        {
            // means in middle of SSO show consent to introduction page
            try {
                Set cotSet = LibertyManager.getListOfCOTs(
                    realm, entityID, providerRole);
                if (cotSet != null && !cotSet.isEmpty()) {
                    if(cotSet.size() <= 1) {
                        String cotSelected = (String)cotSet.iterator().next();
                        if (FSUtils.debug.messageEnabled()) {
                            FSUtils.debug.message("FSPostLogin::doPostLogin "
                                + "single cot present is " + cotSelected);
                        }
                        returnURL =
                            doConsentToIntro(metaAlias,lrURL,cotSelected);
                    } else {
                        returnURL = new StringBuffer().append(consentPage)
                            .append(IFSConstants.QUESTION_MARK)
                            .append(IFSConstants.META_ALIAS)
                            .append(IFSConstants.EQUAL_TO).append(metaAlias)
                            .append(IFSConstants.AMPERSAND)
                            .append(IFSConstants.LRURL)
                            .append(IFSConstants.EQUAL_TO)
                            .append(URLEncDec.encode(lrURL)).toString();
                    }
                } else {
                    if (FSUtils.debug.messageEnabled())  {
                        FSUtils.debug.message("FSPostLogin::doPostLogin: No "
                            + "COTS configured. redirecting to lrurl "
                            + lrURL);
                    }
                    returnURL = lrURL;
                }
            } catch (FSPostLoginException fsexp) {
                sendResponse(request, response, errorPage);
                return;
            }
        } else if (showFederatePage != null && 
                    !showFederatePage.equalsIgnoreCase(
                        IFSConstants.FEDERATEVALUE)) 
        {
            //show do u want to federate page with list of IDP's
            String providerID = LibertyManager.getEntityID(metaAlias);
            String univId = LibertyManager.getUser(request);
            if(univId == null) {
                String gotoUrl = new StringBuffer()
                    .append(request.getRequestURL())
                    .append(IFSConstants.QUESTION_MARK)
                    .append(IFSConstants.AMPERSAND)
                    .append(request.getQueryString()).toString();
                String preLoginURL = LibertyManager.getLoginURL(request);
                sendResponse(request, response, preLoginURL + "&goto=" +
                    URLEncDec.encode(gotoUrl));
                return;
            }
            Set providerSet = LibertyManager.getProvidersToFederate(
                realm, providerID, providerRole,univId);
            if (providerSet != null &&  providerSet.size() != 0 &&
                federationPage != null) 
            {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSPostLogin::doPostLogin: Found "
                        + "provider(s) to federated with " + providerSet);
                }
                returnURL = new StringBuffer().append(federationPage).append
                        (IFSConstants.QUESTION_MARK)
                        .append(IFSConstants.META_ALIAS)
                        .append(IFSConstants.EQUAL_TO)
                        .append(metaAlias).append(IFSConstants.AMPERSAND)
                        .append(IFSConstants.LRURL)
                        .append(IFSConstants.EQUAL_TO)
                        .append(URLEncDec.encode(lrURL)).toString();
            } else {
                if (FSUtils.debug.messageEnabled()) {
                    FSUtils.debug.message("FSPostLogin::doPostLogin: No "
                        + "provider(s) to federated with or "
                        + "federationPage null. Redirecting to LRURL "
                        + lrURL);
                }
                returnURL = lrURL;
            }
        } else {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message("FSPostLogin::doPostLogin. No cotkey or "
                    + "Federatekey found");
            }
            returnURL = lrURL;
        }
        setTokenListenerAndSessionInfo(request, metaAlias);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSPostLogin::doPostLogin return url "
                + returnURL);
        }
        sendResponse(request, response,returnURL);
    }
    
    /**
     * Sets the Session Listener and session information.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @param metaAlias the provider alias string.
     */
    private void setTokenListenerAndSessionInfo(
        HttpServletRequest request, String metaAlias) 
    {
        Object ssoToken = null;
        String sessionID = null;
        String userID = null;
        try {
            SessionProvider sessionProvider = SessionManager.getProvider();
            ssoToken = sessionProvider.getSession(request);
            sessionID = sessionProvider.getSessionID(ssoToken);
            userID = sessionProvider.getPrincipalName(ssoToken);
            sessionProvider.addListener(
                ssoToken, new FSTokenListener(metaAlias));
        } catch(SessionException ssoExp) {
            FSUtils.debug.error("FSPostLogin::setTokenListenerAndSessionInfo "
                + "Failed during trying to add token Listener:", ssoExp);
            return;
        }
        FSSessionManager sessionManager =
            FSSessionManager.getInstance(metaAlias);
        FSSession session = sessionManager.getSession(userID, sessionID);
        if(session == null)  {
            if (FSUtils.debug.messageEnabled()) {
                FSUtils.debug.message(
                    "FSPostLogin::setTokenListenerAndSessionInfo. "
                    + "No existing session found for user " + userID
                    + " And SessionID: " + sessionID
                    + " Creating a new Session");
            }
            FSSession newSession = new FSSession(sessionID);
            sessionManager.addSession(userID, newSession);
        }
    }
    
    /**
     * Forwards request to the Return URL.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @param request the <code>HttpServletResponse</code> object.
     * @param returnURL the url to forward to.
     */
    private void sendResponse(HttpServletRequest request,
        HttpServletResponse response,
        String returnURL) 
    {
        if (returnURL == null || returnURL.length() == 0) {
            // setMetaInfo throws before errorPage is assigned when the meta manager is
            // unavailable, so this is reachable with no page to send. forwardRequest would
            // swallow the MalformedURLException from new URL(null) and commit an empty 200.
            FSUtils.debug.error("FSPostLogin::sendResponse no error page resolved; "
                + "sending an error status instead");
            try {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } catch (Exception exp) {
                FSUtils.debug.error(
                    "FSPostLogin:: sendError Error during sending error status");
            }
            return;
        }
        try {
            FSUtils.forwardRequest(request, response, returnURL);
        } catch (Exception exp) {
            FSUtils.debug.error(
                "FSPreLogin:: sendError Error during sending error page");
        }
    }
    
    /**
     * Returns the introduction URL.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @return a String the Introduction URL.
     * @exception FSPostLoginException on error.
     */
    public String doConsentToIntro(HttpServletRequest request)
        throws FSPostLoginException 
    {
        String metaAlias = request.getParameter(IFSConstants.META_ALIAS);
        String targetURL = request.getParameter(IFSConstants.LRURL);
        String cotSelected = request.getParameter(IFSConstants.COTKEY);
        // doPostLogin hands the overload below an LRURL it has already checked; this entry
        // point takes it straight from the request, and whenever no introduction URL resolves
        // - unknown or inactive COT, or a COTException - it comes back out unchanged and is
        // forwarded to. So it needs the same allow-list check before it can become a target.
        // Runs for an absent RelayState too, not only a present one: the overload below appends
        // URLEncDec.encode(targetURL) to the introduction URL whenever the COT resolves, so a
        // null left an NPE to escape the servlet as a 500 with a stack trace on an
        // unauthenticated endpoint. checkedRelayState answers the provider home URL for it,
        // which is the same substitution doPostLogin makes when no RelayState is supplied.
        targetURL = checkedRelayState(request, targetURL);
        if (targetURL == null) {
            return null;
        }
        return doConsentToIntro(metaAlias,targetURL,cotSelected);
    }
    
    /**
     * Retrieves the entityID of the provider.
     *
     * @param metaAlias the provider alias.
     * @param request the <code>HttpServletRequest</code> object.
     * @exception FSPostLoginException on error.
     */
    private void setMetaInfo(String metaAlias,HttpServletRequest request)
        throws FSPostLoginException 
    {
        IDFFMetaManager metaManager = getMetaManager();
        if (metaManager != null) {
            BaseConfigType hostedConfig = null;
            try {
                providerRole = 
                    metaManager.getProviderRoleByMetaAlias(metaAlias);
                entityID = metaManager.getEntityIDByMetaAlias(
                    metaAlias);
                realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
                if (providerRole != null &&
                    providerRole.equals(IFSConstants.IDP))
                {
                    isIDP = true;
                    hostedConfig = metaManager.getIDPDescriptorConfig(
                        realm, entityID);
                } else if (providerRole != null &&
                    providerRole.equalsIgnoreCase(IFSConstants.SP))
                {
                    hostedConfig = metaManager.getSPDescriptorConfig(
                        realm, entityID);
                }
            } catch (IDFFMetaException ie) {
                FSUtils.debug.error("FSPostLogin::setMetaInfo: exception:",ie);
            }
            consentPage = FSServiceUtils.getConsentPageURL(
                request, hostedConfig);
            federationPage = FSServiceUtils.getDoFederatePageURL(
                request, hostedConfig);
            errorPage = FSServiceUtils.getErrorPageURL(
                request, hostedConfig, metaAlias);
        } else {
            FSUtils.debug.error("FSPostLogin::setMetaInfo "
                + "could not get alliane manager handle "
                + "Cannot proceed so throwing error page");
            throw new FSPostLoginException(
                "FSPostLogin:: could not get meta manager handle.");
        }
    }
    
    /**
     * Returns the Introduction Writer URL.
     *
     * @param metaAlias the provider alias.
     * @param targetURL the url the writer servlet will redirect to.
     * @param cotSelected the name of the Circle fo Trust.
     * @return the writer url.
     * @exception FSPostLoginException on error.
     */
    private String doConsentToIntro(String metaAlias,String targetURL,
        String cotSelected )
        throws FSPostLoginException 
    {
        String tldURL = null;
        try {
            if (entityID == null) {
                IDFFMetaManager metaManager = getMetaManager();
                if (metaManager != null) {
                    entityID = metaManager.getEntityIDByMetaAlias(metaAlias);
                }
            }
            if (realm == null) {
                realm = IDFFMetaUtils.getRealmByMetaAlias(metaAlias);
            }
            CircleOfTrustManager cotManager = new CircleOfTrustManager();
            CircleOfTrustDescriptor cotDesc = cotManager.getCircleOfTrust(
                realm, cotSelected);
            if (cotDesc != null &&
                (cotDesc.getCircleOfTrustStatus())
                    .equalsIgnoreCase(IFSConstants.ACTIVE)) 
            {
                tldURL = cotDesc.getIDFFWriterServiceURL();
            }
        } catch (IDFFMetaException metaExp) {
            FSUtils.debug.error(
                "FSPostLogin::doConsentToIntro in cot managment expextion:",
                metaExp);
            tldURL = null;
        } catch (COTException meta2Exp) {
            FSUtils.debug.error(
                "FSPostLogin::doConsentToIntro in cot managment expextion:",
                meta2Exp);
            tldURL = null;
        }
        
        String redirectURL = targetURL;
        if (tldURL != null && entityID != null) {
            redirectURL =  new StringBuffer().append(tldURL)
                .append(IFSConstants.QUESTION_MARK)
                .append(IFSConstants.LRURL).append(IFSConstants.EQUAL_TO)
                .append(URLEncDec.encode(targetURL))
                .append(IFSConstants.AMPERSAND)
                .append(IFSConstants.PROVIDER_ID_KEY)
                .append(IFSConstants.EQUAL_TO)
                .append(URLEncDec.encode(entityID)).toString();
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSPostLogin::doConsentToIntro return url"
                + redirectURL);
        }
        return redirectURL;
    }

    /**
     * Returns the URL to which request should be redirected
     * for federation. This method reads the request parameters
     * and creates an Authentication Request to send to
     * initiate the Single Sign-On / Federation process.
     *
     * @param request the <code>HttpServletRequest</code> object.
     * @param response the <code>HttpServletResponse</code> object.
     * @return the URL to redirect request to.
     * @exception FSPostLoginException on error.
     */
    public String doFederation(HttpServletRequest request,
        HttpServletResponse response )
    throws FSPostLoginException {

        String metaAlias = request.getParameter(IFSConstants.META_ALIAS);
        String LRURL = allowedRelayState(request, request.getParameter(IFSConstants.LRURL));
        String selectedProvider = 
            request.getParameter(IFSConstants.SELECTEDPROVIDER);
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSPostLogin::doFederation metaAlias "
                + metaAlias);
            FSUtils.debug.message("FSPostLogin::doFederation lrurl "
                + LRURL);
            FSUtils.debug.message("FSPostLogin::doFederation selected provider"
                + selectedProvider);
        }
        Map headerMap = new HashMap();
        Enumeration headerNames = request.getHeaderNames();
        while(headerNames.hasMoreElements()) {
            String hn = headerNames.nextElement().toString();
            String hv = request.getHeader(hn);
            headerMap.put(hn, hv);
        }
        FSLoginHelper plh = new FSLoginHelper(request);
        Map retMap = new HashMap();
        String authLevel = null;
        HttpSession httpSession = request.getSession(false);
        if (httpSession != null) {
            authLevel = (String) httpSession.getAttribute(
                IFSConstants.AUTH_LEVEL_KEY);
        }
        try {
            retMap = plh.createAuthnRequest(headerMap,
                    LRURL,
                    authLevel,
                    metaAlias,
                    selectedProvider,
                    true);
        } catch (FSLoginHelperException fsLoginExp) {
            FSUtils.debug.error("FSPostLogin::doFederate in exception ",
                fsLoginExp);
            throw new FSPostLoginException("FSPostLogin::doFederate exception "
                + fsLoginExp.getMessage());
        }
        Map retHeaderMap = (Map)retMap.get(IFSConstants.HEADER_KEY);
        Iterator hdrNames = retHeaderMap.keySet().iterator();
        while (hdrNames.hasNext()) {
            String name = hdrNames.next().toString();
            String value = (String)retHeaderMap.get(name);
            response.addHeader(name, value);
        }
        String urlKey = (String) retMap.get(IFSConstants.URL_KEY);
        if (urlKey == null) {
            // A LECP request has no URL to send anyone to: createAuthnRequest answers with the
            // AuthnRequestEnvelope on the stream instead, which is what FSPreLogin does with
            // the same map. Without this the caller sees a null URL and answers 400.
            String responseData = (String) retMap.get(IFSConstants.RESPONSE_DATA_KEY);
            if (responseData != null) {
                try {
                    response.getOutputStream().write(
                        responseData.getBytes(StandardCharsets.UTF_8));
                    response.flushBuffer();
                } catch (Exception exp) {
                    throw new FSPostLoginException(
                        "FSPostLogin::doFederation could not write the LECP envelope: "
                        + exp.getMessage());
                }
            }
        }
        if (FSUtils.debug.messageEnabled()) {
            FSUtils.debug.message("FSPostLogin::doFederation returning with "
                + FSUtils.forLog(urlKey));
        }
        return urlKey;
    }

    
    /**
     * Retrieves the value of a parameter from the URL. This is
     * an utility method.
     *
     * @param reqURLStr the url string.
     * @param name the value of the parameter to be retrieved.
     * @return value of the parameter
     */
    private static String getValueFromURL(String reqURLStr, String name) {
        String returnStr = null;
        int iIndex =0 ;
        if (reqURLStr != null &&
            ((iIndex = reqURLStr.lastIndexOf(name)) != -1)) 
        {
            iIndex = iIndex + name.length() + 1;
            String newStr = reqURLStr.substring(iIndex);
            byte strInBytes[] = newStr.getBytes();
            int endOfString;
            int len = newStr.length();
            for (endOfString = 0; endOfString < len; endOfString++) {
                if (strInBytes[endOfString] == '/' ||
                    strInBytes[endOfString] == '?') 
                {
                    break;
                }
            }
            returnStr = newStr.substring(0, endOfString);
        }
        return returnStr;
    }
}
