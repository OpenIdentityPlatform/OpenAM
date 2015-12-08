/*
* The contents of this file are subject to the terms of the Common Development and
* Distribution License (the License). You may not use this file except in compliance with the
* License.
*
* You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
* specific language governing permission and limitations under the License.
*
* When distributing Covered Software, include this CDDL Header Notice in each file and include
* the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
* Header, with the fields enclosed by brackets [] replaced by your own identifying
* information: "Portions copyright [year] [name of copyright owner]".
*
* Copyright 2015 ForgeRock AS.
*/
package org.forgerock.openam.authentication.modules.saml2;

import static org.forgerock.openam.authentication.modules.saml2.Constants.*;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.authentication.spi.AMPostAuthProcessInterface;
import com.sun.identity.authentication.spi.AuthenticationException;
import com.sun.identity.plugin.session.SessionException;
import com.sun.identity.plugin.session.SessionManager;
import com.sun.identity.plugin.session.SessionProvider;
import com.sun.identity.saml2.assertion.Issuer;
import com.sun.identity.saml2.assertion.NameID;
import com.sun.identity.saml2.assertion.impl.NameIDImplWithoutSPNameQualifier;
import com.sun.identity.saml2.common.NameIDInfo;
import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.common.SAML2Exception;
import com.sun.identity.saml2.common.SAML2FailoverUtils;
import com.sun.identity.saml2.common.SAML2Utils;
import com.sun.identity.saml2.jaxb.metadata.EndpointType;
import com.sun.identity.saml2.jaxb.metadata.IDPSSODescriptorElement;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import com.sun.identity.saml2.plugins.SAML2ServiceProviderAdapter;
import com.sun.identity.saml2.profile.CacheObject;
import com.sun.identity.saml2.profile.IDPProxyUtil;
import com.sun.identity.saml2.profile.LogoutUtil;
import com.sun.identity.saml2.profile.ResponseInfo;
import com.sun.identity.saml2.profile.SPACSUtils;
import com.sun.identity.saml2.profile.SPCache;
import com.sun.identity.saml2.protocol.AuthnRequest;
import com.sun.identity.saml2.protocol.LogoutRequest;
import com.sun.identity.saml2.protocol.ProtocolFactory;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.DNMapper;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.openam.federation.saml2.SAML2TokenRepositoryException;
import org.forgerock.openam.saml2.SAML2Store;
import org.forgerock.openam.xui.XUIState;
import org.owasp.esapi.ESAPI;
import org.owasp.esapi.errors.EncodingException;

/**
 * Plugin that gets activated for SLO for the SAML2 auth module. Supports HTTP-Redirect
 * for logout-sending messages only.
 */
public class SAML2PostAuthenticationPlugin implements AMPostAuthProcessInterface {

    private static final Debug DEBUG = Debug.getInstance(AM_AUTH_SAML2);
    private static final SAML2MetaManager META_MANAGER = SAML2Utils.getSAML2MetaManager();

    private static final String SLO_SESSION_LOCATION = "saml2SLOLoc";
    private static final String SLO_SESSION_REFERENCE = "saml2SLORef";

    /**
     * Default Constructor.
     */
    public SAML2PostAuthenticationPlugin() {

    }

    /**
     * If enabled, performs the first-stage of SLO - by recording the currently logged in user.
     * The information relating to a remote user is stored alongside their local information, and upon
     * active-logout is used to trigger a call to the IdP requesting their logout.
     *
     * @param requestParamsMap map containing <code>HttpServletRequest</code>
     *        parameters
     * @param request <code>HttpServletRequest</code> object.
     * @param response <code>HttpServletResponse</code> object.
     * @param ssoToken authenticated user's single sign token.
     */
    @Override
    public void onLoginSuccess(Map requestParamsMap, HttpServletRequest request, HttpServletResponse response,
                               SSOToken ssoToken) {

        try {
            final String metaAlias = ssoToken.getProperty(SAML2Constants.METAALIAS);
            final String sessionIndex = ssoToken.getProperty(SAML2Constants.SESSION_INDEX);
            final String spEntityId = ssoToken.getProperty(SAML2Constants.SPENTITYID);
            final String idpEntityId = ssoToken.getProperty(SAML2Constants.IDPENTITYID);
            final String nameIdXML = ssoToken.getProperty(SAML2Constants.NAMEID);
            final NameID nameId = new NameIDImplWithoutSPNameQualifier(nameIdXML);
            final boolean isTransient = Boolean.parseBoolean(ssoToken.getProperty(Constants.IS_TRANSIENT));
            final String requestId = ssoToken.getProperty(Constants.REQUEST_ID);
            final SessionProvider sessionProvider = SessionManager.getProvider();
            final NameIDInfo info = new NameIDInfo(spEntityId, idpEntityId, nameId, SAML2Constants.SP_ROLE, false);
            final String ssOutEnabled = ssoToken.getProperty(SAML2Constants.SINGLE_LOGOUT);
            final String cacheKey = ssoToken.getProperty(Constants.CACHE_KEY);
            final String realm =
                    DNMapper.orgNameToRealmName(ssoToken.getProperty(com.sun.identity.shared.Constants.ORGANIZATION));

            SAML2ResponseData data = (SAML2ResponseData) SAML2Store.getTokenFromStore(cacheKey);

            if (data == null && SAML2FailoverUtils.isSAML2FailoverEnabled()) {
                data = (SAML2ResponseData) SAML2FailoverUtils.retrieveSAML2Token(cacheKey);
            }

            if (data == null) {
                throw new SAML2Exception("Unable to retrieve response map from data cache.");
            }

            if (Boolean.parseBoolean(ssOutEnabled)) {
                setupSingleLogOut(ssoToken, metaAlias, sessionIndex, spEntityId, idpEntityId, nameId);
            }

            configureIdpInitSLO(sessionProvider, ssoToken, sessionIndex, metaAlias, info, isTransient, requestId);
            configurePostSSO(spEntityId, realm, request, response, ssoToken, sessionProvider, data.getResponseInfo(),
                    cacheKey);
            clearSession(ssoToken);
        } catch (SAML2Exception | SessionException | SSOException | SAML2TokenRepositoryException e) {
            //debug warning and fall through
            DEBUG.warning("Error saving SAML assertion information in memory. SLO not configured for this session.", e);
        }
    }

    private void configurePostSSO(String spEntityId, String realm, HttpServletRequest request,
                                  HttpServletResponse response, SSOToken session, SessionProvider sessionProvider,
                                  ResponseInfo respInfo, String cacheKey)
            throws SAML2Exception {

        AuthnRequest authnReq = (AuthnRequest) SPCache.authnRequestHash.get(cacheKey);
        boolean writeFedInfo = Boolean.parseBoolean((String) SPCache.fedAccountHash.get(cacheKey));

        final SAML2ServiceProviderAdapter spAdapter = SAML2Utils.getSPAdapterClass(spEntityId, realm);
        if (spAdapter != null) {
            final boolean redirected = spAdapter.postSingleSignOnSuccess(spEntityId, realm, request, response,
                    null, session, authnReq, respInfo.getResponse(), respInfo.getProfileBinding(), writeFedInfo);
            final String[] value = new String[] { String.valueOf(redirected) };
            try {
                sessionProvider.setProperty(session, SAML2Constants.RESPONSE_REDIRECTED, value);
            } catch (SessionException | UnsupportedOperationException ex) {
                DEBUG.warning("SAML2PostAuthenticationPlugin.configurePostSSO :: failed to set properties in session.",
                        ex);
            }
        }

        SPCache.authnRequestHash.remove(cacheKey);
        SPCache.fedAccountHash.remove(cacheKey);
    }

    private void configureIdpInitSLO(SessionProvider sessionProvider, SSOToken session, String sessionIndex,
                                     String metaAlias, NameIDInfo info, boolean isTransient, String requestID)
            throws SessionException, SAML2Exception, SSOException {
        SPACSUtils.saveInfoInMemory(sessionProvider, session, sessionIndex, metaAlias,
                info, IDPProxyUtil.isIDPProxyEnabled(requestID), isTransient);
    }

    private void setupSingleLogOut(SSOToken ssoToken, String metaAlias, String sessionIndex, String spEntityId,
                                   String idpEntityId, NameID nameId)
            throws SSOException, SAML2Exception, SessionException {
        final SAML2MetaManager sm = new SAML2MetaManager();
        final String realm = SAML2MetaUtils.getRealmByMetaAlias(metaAlias);
        final String relayState = ssoToken.getProperty(SAML2Constants.RELAY_STATE);
        final String binding = SAML2Constants.HTTP_REDIRECT;
        final IDPSSODescriptorElement idpsso = sm.getIDPSSODescriptor(realm, idpEntityId);

        final List<EndpointType> slosList = idpsso.getSingleLogoutService();

        EndpointType logoutEndpoint = null;
        for (EndpointType endpoint : slosList) {
            if (binding.equals(endpoint.getBinding())) {
                logoutEndpoint = endpoint;
                break;
            }
        }

        if (logoutEndpoint == null) {
            DEBUG.warning("Unable to determine SLO endpoint. Aborting SLO attempt. Please note this PAP "
                + "only supports HTTP-Redirect as a valid binding.");
            return;
        }

        final LogoutRequest logoutReq = createLogoutRequest(metaAlias, realm, idpEntityId,
                logoutEndpoint, nameId, sessionIndex);

        //survival time is one hours
        final long sessionExpireTime = System.currentTimeMillis() / 1000 + SPCache.interval; //counted in seconds

        final String sloRequestXMLString = logoutReq.toXMLString(true, true);
        final String redirect = getRedirectURL(sloRequestXMLString, relayState, realm, idpEntityId,
                logoutEndpoint.getLocation(), spEntityId);

        if (SAML2FailoverUtils.isSAML2FailoverEnabled()) {
            try {
                SAML2FailoverUtils.saveSAML2TokenWithoutSecondaryKey(logoutReq.getID(), logoutReq, sessionExpireTime);
            } catch (SAML2TokenRepositoryException e) {
                DEBUG.warning("Unable to set SLO redirect location. Aborting SLO attempt.");
                return;
            }
        } else {
            SAML2Store.saveTokenWithKey(logoutReq.getID(), logoutReq);
        }

        ssoToken.setProperty(SLO_SESSION_LOCATION, logoutEndpoint.getLocation());
        ssoToken.setProperty(SLO_SESSION_REFERENCE, redirect);
    }

    /**
     * Clears the session of all the temp data we passed to set up SLO.
     */
    private void clearSession(SSOToken ssoToken) throws SSOException {
        ssoToken.setProperty(SAML2Constants.RELAY_STATE, "");
        ssoToken.setProperty(SAML2Constants.SESSION_INDEX, "");
        ssoToken.setProperty(SAML2Constants.IDPENTITYID, "");
        ssoToken.setProperty(SAML2Constants.SPENTITYID, "");
        ssoToken.setProperty(SAML2Constants.METAALIAS, "");
        ssoToken.setProperty(SAML2Constants.REQ_BINDING, "");
        ssoToken.setProperty(SAML2Constants.NAMEID, "");
        ssoToken.setProperty(Constants.IS_TRANSIENT, "");
        ssoToken.setProperty(Constants.REQUEST_ID, "");
        ssoToken.setProperty(Constants.CACHE_KEY, "");
    }

    @Override
    public void onLoginFailure(Map requestParamsMap, HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        // This section intentionally left blank.
    }

    @Override
    public void onLogout(HttpServletRequest request, HttpServletResponse response, SSOToken ssoToken)
            throws AuthenticationException {
        try {
            final String ssOutEnabled = ssoToken.getProperty(SAML2Constants.SINGLE_LOGOUT);
            if (Boolean.parseBoolean(ssOutEnabled)) {
                final XUIState xuiState = InjectorHolder.getInstance(XUIState.class);
                final StringBuilder logoutLocation = new StringBuilder();
                logoutLocation.append(ssoToken.getProperty(SLO_SESSION_LOCATION));
                if (xuiState.isXUIEnabled()) {
                    logoutLocation.append(ESAPI.encoder().encodeForURL(ssoToken.getProperty(SLO_SESSION_REFERENCE)));
                } else {
                    logoutLocation.append(ssoToken.getProperty(SLO_SESSION_REFERENCE));
                }
                request.setAttribute(AMPostAuthProcessInterface.POST_PROCESS_LOGOUT_URL, logoutLocation.toString());
            }
        } catch (EncodingException | SSOException e) {
            //debug warning and fall through
            DEBUG.warning("Error loading SAML assertion information in memory. SLO failed for this session.", e);
        }
    }

    private LogoutRequest createLogoutRequest(String metaAlias, String realm, String idpEntityId,
                                              EndpointType logoutEndpoint, NameID nameId, String sessionIndex)
            throws SAML2Exception, SessionException {

        // generate unique request ID
        final String requestID = SAML2Utils.generateID();
        if ((requestID == null) || (requestID.length() == 0)) {
            DEBUG.warning("SAML2 PAP :: Unable to perform single logout, unable to generate request ID - {}",
                    SAML2Utils.bundle.getString("cannotGenerateID"));
            throw new SAML2Exception(SAML2Utils.BUNDLE_NAME, "cannotGenerateID", new Object[0]);
        }

        final String spEntityID = META_MANAGER.getEntityByMetaAlias(metaAlias);
        final Issuer issuer = SAML2Utils.createIssuer(spEntityID);

        final LogoutRequest logoutReq = ProtocolFactory.getInstance().createLogoutRequest();
        logoutReq.setID(requestID);
        logoutReq.setVersion(SAML2Constants.VERSION_2_0);
        logoutReq.setIssueInstant(new Date());
        logoutReq.setIssuer(issuer);

        if (sessionIndex != null) {
            logoutReq.setSessionIndex(Collections.singletonList(sessionIndex));
        }

        String location = logoutEndpoint.getLocation();
        logoutReq.setDestination(XMLUtils.escapeSpecialCharacters(location));

        LogoutUtil.setNameIDForSLORequest(logoutReq, nameId, realm, spEntityID, SAML2Constants.SP_ROLE, idpEntityId);

        return logoutReq;
    }

    private String getRedirectURL(String sloRequestXMLString, String relayState, String realm,
                               String idpEntityId, String sloURL, String hostEntity) throws SAML2Exception {

        // encode the xml string
        String encodedXML = SAML2Utils.encodeForRedirect(sloRequestXMLString);

        StringBuilder queryString = new StringBuilder()
                .append(SAML2Constants.SAML_REQUEST)
                .append(SAML2Constants.EQUAL)
                .append(encodedXML);

        if ((relayState != null) && (relayState.length() > 0)) {
            String tmp = SAML2Utils.generateID();
            SPCache.relayStateHash.put(tmp, new CacheObject(relayState));
            queryString.append("&").append(SAML2Constants.RELAY_STATE).append("=").append(tmp);
        }

        boolean needToSign = SAML2Utils.getWantLogoutRequestSigned(realm, idpEntityId, SAML2Constants.IDP_ROLE);

        String signedQueryString = queryString.toString();
        if (needToSign) {
            signedQueryString = SAML2Utils.signQueryString(signedQueryString, realm, hostEntity,
                    SAML2Constants.SP_ROLE);
        }

        return (sloURL.contains("?") ? "&" : "?") + signedQueryString;
    }

}
