/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of company]"
 */
package org.forgerock.restlet.ext.oauth2.consumer;

import java.util.Map;
import java.util.logging.Level;

import org.forgerock.openam.oauth2.utils.OAuth2Utils;
import org.forgerock.openam.oauth2.exceptions.OAuthProblemException;
import org.restlet.Context;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.ChallengeRequest;
import org.restlet.data.Status;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.security.ChallengeAuthenticator;
import org.restlet.security.Verifier;

/**
 * An OAuth2Authenticator authenticates the subject sending the request and gets the token verifier.
 * Used in the Demo application
 */
public class OAuth2Authenticator extends ChallengeAuthenticator {

    /**
     * The token tokenVerifier.
     */
    private volatile TokenVerifier tokenVerifier;

    private OAuth2Utils.ParameterLocation parameterLocation =
            OAuth2Utils.ParameterLocation.HTTP_HEADER;

    public OAuth2Authenticator(Context context, String realm,
            OAuth2Utils.ParameterLocation tokenLocation, TokenVerifier verifier) {
        super(context, null, realm);
        tokenVerifier = verifier;
        parameterLocation = tokenLocation;
    }

    public OAuth2Authenticator(Context context, boolean optional, String realm,
            OAuth2Utils.ParameterLocation tokenLocation, TokenVerifier verifier) {
        super(context, optional, null, realm, verifier
                .getVerifier(tokenLocation));
        parameterLocation = tokenLocation;
    }

    /**
     * Returns the credentials tokenVerifier.
     * 
     * @return The credentials tokenVerifier.
     */
    public Verifier getVerifier() {
        return getTokenVerifier().getVerifier(parameterLocation);
    }

    /**
     * Returns the token tokenVerifier.
     * 
     * @return The token tokenVerifier.
     */
    public TokenVerifier getTokenVerifier() {
        return tokenVerifier;
    }

    /**
     * Attempts to authenticate the subject sending the request.
     * 
     * @param request
     *            The request sent.
     * @param response
     *            The response to update.
     * @return True if the authentication succeeded.
     */
    protected boolean authenticate(Request request, Response response) {
        try {
            return super.authenticate(request, response);
        } catch (OAuthProblemException e) {
            doError(request, response, e);
        }
        return false;
    }

    @Override
    public void challenge(Response response, boolean stale) {
        if (OAuth2Utils.ParameterLocation.HTTP_HEADER.equals(parameterLocation)) {
            super.challenge(response, stale);
        }
    }

    protected void doError(Request request, Response response, OAuthProblemException exception) {
        if (OAuth2Utils.ParameterLocation.HTTP_HEADER.equals(parameterLocation)) {
            if (!isOptional()) {
                if (isRechallenging()) {
                    boolean loggable =
                            response.getRequest().isLoggable()
                                    && getLogger().isLoggable(Level.FINE);

                    if (loggable) {
                        getLogger().log(Level.FINE, "An authentication challenge was requested.");
                    }

                    response.setStatus(Status.CLIENT_ERROR_UNAUTHORIZED);
                    response.getChallengeRequests().add(
                            getTokenVerifier().getTokenExtractor().createChallengeRequest(
                                    getRealm(), exception));
                    response.setEntity(new JacksonRepresentation<Map>(exception.getErrorMessage()));
                } else {
                    forbid(response);
                }
            }
        } else {
            response.setStatus(exception.getStatus());
            response.setEntity(new JacksonRepresentation<Map>(exception.getErrorMessage()));
        }
    }

    /**
     * Creates a new challenge request.
     * 
     * @param stale
     *            Indicates if the new challenge is due to a stale response.
     * @return A new challenge request.
     */
    protected ChallengeRequest createChallengeRequest(boolean stale) {
        return getTokenVerifier().getTokenExtractor().createChallengeRequest(getRealm());
    }
}
