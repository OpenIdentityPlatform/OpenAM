/*
 * Copyright 2014 ForgeRock, AS.
 *
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
 */

package org.forgerock.openam.rest.resource;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.entitlement.opensso.SubjectUtils;
import org.forgerock.json.resource.Context;
import org.forgerock.json.resource.SecurityContext;
import org.forgerock.json.resource.ServerContext;
import org.forgerock.openam.forgerockrest.RestUtils;
import org.forgerock.util.Reject;

import javax.security.auth.Subject;

/**
 * CREST context that provides a convenience method for getting hold of the caller as an authenticated subject based
 * on the OpenAM SSO Token associated with the request.
 *
 * @since 12.0.0
 */
public class SSOTokenContext extends ServerContext implements SubjectContext {

    public SSOTokenContext(Context parent) {
        super(parent);
        Reject.ifFalse(parent.containsContext(SecurityContext.class), "Parent context must contain a SecurityContext");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Subject getCallerSubject() {
        try {
            return SubjectUtils.createSubject(getCallerSSOToken());
        } catch (SSOException e) {
            // No SSO token: return null to indicate no authenticated Subject
            return null;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Subject getSubject(final String tokenId) {
        try {
            return SubjectUtils.createSubject(getSSOToken(tokenId));
        } catch (SSOException ssoE) {
            return null;
        }
    }

    /**
     * Returns the SSO token associated with this request.
     *
     * @return the SSO token associated with this request.
     *
     * @throws SSOException
     *         if there is no SSO token associated with this request.
     */
    @Override
    public SSOToken getCallerSSOToken() throws SSOException {
        return getCallerSSOToken(SSOTokenManager.getInstance());
    }

    /**
     * Returns the SSO token associated with this request.
     *
     * @param tokenManager
     *         The SSOTokenManager instance that will get the SSOToken.
     *
     * @return the SSO token associated with this request.
     *
     * @throws SSOException
     *         if there is no SSO token associated with this request.
     */
    public SSOToken getCallerSSOToken(final SSOTokenManager tokenManager) throws SSOException {
        String tokenId = RestUtils.getCookieFromServerContext(this);
        return getSSOToken(tokenId, tokenManager);
    }

    /**
     * Given a valid non-null token Id, returns its SSO token representation.
     *
     * @param tokenId
     *         valid non-null token id
     *
     * @return SSO token representation
     *
     * @throws SSOException
     *         if there is no SSO token associated with this request.
     */
    public SSOToken getSSOToken(final String tokenId) throws SSOException {
        return getSSOToken(tokenId, SSOTokenManager.getInstance());
    }

    /**
     * Given a valid non-null token Id, returns its SSO token representation.
     *
     * @param tokenId
     *         valid non-null token id
     * @param tokenManager
     *         non-null token managed used to assist with retrieving the SSO token
     *
     * @return SSO token representation
     *
     * @throws SSOException
     *         if there is no SSO token associated with this request.
     */
    public SSOToken getSSOToken(final String tokenId, final SSOTokenManager tokenManager) throws SSOException {
        Reject.ifNull(tokenManager, "A valid SSO token manager is required");
        return tokenManager.createSSOToken(tokenId);
    }

}
