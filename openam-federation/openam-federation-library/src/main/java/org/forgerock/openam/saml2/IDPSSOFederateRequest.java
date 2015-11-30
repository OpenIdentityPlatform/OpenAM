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
package org.forgerock.openam.saml2;

import com.sun.identity.saml2.assertion.AuthnContext;
import com.sun.identity.saml2.plugins.SAML2IdentityProviderAdapter;
import com.sun.identity.saml2.protocol.AuthnRequest;
import org.forgerock.openam.saml2.audit.SAML2EventLogger;

/**
 * Represents the data necessary for a request to be handled by the IdP SSO Federate's authenticate
 * and authentication lookup systems, as well as all related utility functions.
 */
public class IDPSSOFederateRequest {

    private static final SAML2EventLogger DEFAULT_SMAL2_EVENT_AUDITOR = new DoNothingSAML2EventLogger();

    private final String reqID;
    private final String realm;
    private final SAML2IdentityProviderAdapter idpAdapter;

    private final String idpMetaAlias;
    private final String idpEntityID;

    private String spEntityID;
    private String relayState;

    private AuthnRequest authnReq;
    private AuthnContext matchingAuthnContext;

    private SAML2EventLogger eventAuditor;

    private Object session;

    /**
     * Data necessary for either authentication or authentication lookup via SAML2 IdP.
     *
     * @param reqID        Request identifier.
     * @param realm        The realm we're operating in.
     * @param idpAdapter   The IdP adapter to use.
     * @param idpMetaAlias The meta alias to use.
     * @param idpEntityID  The IdP's entity ID.
     */
    public IDPSSOFederateRequest(String reqID, String realm, SAML2IdentityProviderAdapter idpAdapter,
                                 String idpMetaAlias, String idpEntityID) {
        this.reqID = reqID;
        this.realm = realm;
        this.idpAdapter = idpAdapter;
        this.idpMetaAlias = idpMetaAlias;
        this.idpEntityID = idpEntityID;
    }

    /**
     * Sets the current session.
     *
     * @param session Session to set.
     */
    public void setSession(Object session) {
        this.session = session;
    }

    /**
     * Gets the current session.
     *
     * @return current session as an Object.
     */
    public Object getSession() {
        return session;
    }

    /**
     * Sets the current SP Entity ID.
     *
     * @param spEntityID ID of the SP Entity.
     */
    public void setSpEntityID(String spEntityID) {
        this.spEntityID = spEntityID;
    }

    /**
     * Gets the current SP Entity ID.
     *
     * @return current SP entity ID.
     */
    public String getSpEntityID() {
        return spEntityID;
    }

    /**
     * Sets the relay state.
     *
     * @param relayState the relay state.
     */
    public void setRelayState(String relayState) {
        this.relayState = relayState;
    }

    /**
     * Gets the relay state.
     *
     * @return the relay state.
     */
    public String getRelayState() {
        return relayState;
    }

    /**
     * Sets the authentication request.
     *
     * @param authnReq the authentication request.
     */
    public void setAuthnRequest(AuthnRequest authnReq) {
        this.authnReq = authnReq;
    }

    /**
     * Gets the authentication request.
     *
     * @return the authentication request.
     */
    public AuthnRequest getAuthnRequest() {
        return authnReq;
    }

    /**
     * Gets the request ID.  If the request ID is null returns the ID of the authentication request if it is available.
     *
     * @return the request ID.
     */
    public String getRequestID() {
        if (null == reqID && null != authnReq) {
            return authnReq.getID();
        }
        return reqID;
    }

    /**
     * Gets the current realm.
     *
     * @return current realm.
     */
    public String getRealm() {
        return realm;
    }

    /**
     * Gets the IdP Meta Alias.
     *
     * @return the IdPMetaAlias used for this federation activity.
     */
    public String getIdpMetaAlias() {
        return idpMetaAlias;
    }

    /**
     * Gets the IdP entity ID.
     *
     * @return the IdP entity ID used for this federation activity.
     */
    public String getIdpEntityID() {
        return idpEntityID;
    }

    /**
     * Sets the matching authentication context for this request.
     *
     * @param matchingAuthnContext context to set.
     */
    public void setMatchingAuthnContext(AuthnContext matchingAuthnContext) {
        this.matchingAuthnContext = matchingAuthnContext;
    }

    /**
     * Gets the matching authentication context for this request.
     *
     * @return the matching authentication context.
     */
    public AuthnContext getMatchingAuthnContext() {
        return matchingAuthnContext;
    }

    /**
     * Gets the IdPAdaptor used to perform SSO operations for this request.
     *
     * @return The IdPAdaptor used for this request.
     */
    public SAML2IdentityProviderAdapter getIdpAdapter() {
        return idpAdapter;
    }

    /**
     * @return the Audit Event Logger for this request
     */
    public SAML2EventLogger getEventAuditor() {
        if (null == this.eventAuditor) {
            return DEFAULT_SMAL2_EVENT_AUDITOR;
        }
        return this.eventAuditor;

    }

    /**
     * @param eventAuditor the Audit Event Logger for this request. The Default SAML2EventLogger will not perform
     *                     any actions.
     */
    public void setEventAuditor(SAML2EventLogger eventAuditor) {
        this.eventAuditor = eventAuditor;
    }

    // this inner class saves the caller of getEventAuditor from having to perform null checks
    private static class DoNothingSAML2EventLogger implements SAML2EventLogger {

        @Override
        public void auditAccessAttempt() {
            // Do Nothing
        }

        @Override
        public void auditAccessSuccess() {
            // Do Nothing
        }

        @Override
        public void auditAccessFailure(String errorCode, String message) {
            // Do Nothing
        }

        @Override
        public void setSessionTrackingId(String trackingId) {
            // Do Nothing
        }

        @Override
        public void setUserId(String userId) {
            // Do Nothing
        }

        @Override
        public void setRealm(String realm) {
            // Do Nothing
        }

        @Override
        public void setMethod(String method) {
            // Do Nothing
        }

        @Override
        public void auditForwardToProxy() {
            // Do Nothing
        }

        @Override
        public void auditForwardToLocalUserLogin() {
            // Do Nothing
        }

        @Override
        public void setRequestId(String authnRequestId) {
            // Do Nothing
        }

        @Override
        public void setSSOTokenId(Object ssoTokenId) {
            // Do Nothing
        }

        @Override
        public void setAuthTokenId(Object session) {
            // Do Nothing
        }
    }
}
