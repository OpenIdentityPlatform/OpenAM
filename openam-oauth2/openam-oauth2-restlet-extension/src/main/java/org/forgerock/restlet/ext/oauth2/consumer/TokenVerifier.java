/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
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
 * "Portions Copyrighted [year] [name of copyright owner]"
 */
package org.forgerock.restlet.ext.oauth2.consumer;

import org.forgerock.restlet.ext.oauth2.OAuth2Utils;
import org.forgerock.restlet.ext.oauth2.OAuthProblemException;
import org.restlet.Request;
import org.restlet.Response;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.representation.Representation;
import org.restlet.security.User;
import org.restlet.security.Verifier;

/**
 * A NAME does ...
 * 
 * @author Laszlo Hordos
 */
public abstract class TokenVerifier<T extends AccessTokenExtractor<U>, U extends AbstractAccessToken> {

    public abstract User createUser(U token);

    protected abstract T getTokenExtractor();

    protected abstract AccessTokenValidator<U> getTokenValidator();

    public Verifier getVerifier(OAuth2Utils.ParameterLocation tokenLocation) {
        return new InnerTokenVerifier(tokenLocation);
    }

    protected class InnerTokenVerifier implements Verifier {

        public InnerTokenVerifier(OAuth2Utils.ParameterLocation tokenLocation) {
            if (null == tokenLocation) {
                throw new RuntimeException("Missing required tokenLocation parameter");
            }
            this.tokenLocation = tokenLocation;
        }

        private OAuth2Utils.ParameterLocation tokenLocation;

        @Override
        public int verify(Request request, Response response) {
            int result = RESULT_INVALID;
            try {
                U token = null;
                switch (tokenLocation) {
                case HTTP_BODY: {
                    // Methods without request entity
                    if (Method.GET.equals(request.getMethod())
                            || Method.HEAD.equals(request.getMethod())) {
                        token =
                                getTokenExtractor().extractToken(
                                        OAuth2Utils.ParameterLocation.HTTP_QUERY, request);
                        break;
                    }
                }
                case HTTP_HEADER: {
                    if (request.getChallengeResponse() == null) {
                        return RESULT_MISSING;
                    }
                }
                default: {
                    token = getTokenExtractor().extractToken(tokenLocation, request);
                }
                }
                if (null == token) {
                    result = RESULT_MISSING;
                } else {
                    U t = getTokenValidator().verify(token);
                    if (null != t || t.isValid()) {
                        request.getClientInfo().setUser(createUser(t));
                        result = RESULT_VALID;
                    }
                }

            } catch (OAuthProblemException e) {
                // TODO add logging
                throw e;
            }
            return result;
        }
    }

    protected Form getAuthenticationParameters(Request request) throws OAuthProblemException {
        Form result = null;
        // Use the parameters which was populated with the AuthenticatorHelper
        if (request.getChallengeResponse() != null) {
            result = new Form(request.getChallengeResponse().getParameters());
            // getLogger().fine("Found Authorization header" +
            // result.getFirst(OAuth2.Params.ACCESS_TOKEN));
        }
        if ((result == null)) {
            // getLogger().fine("No Authorization header - checking query");
            result = request.getOriginalRef().getQueryAsForm();
            // getLogger().fine("Found Token in query" +
            // result.getFirst(OAuth2.Params.ACCESS_TOKEN));

            // check body if all else fail:
            if (result == null) {
                if ((request.getMethod() == Method.POST) || (request.getMethod() == Method.PUT)
                        || (request.getMethod() == Method.DELETE)) {
                    Representation r = request.getEntity();
                    if ((r != null) && MediaType.APPLICATION_WWW_FORM.equals(r.getMediaType())) {
                        // Search for an OAuth Token
                        result = new Form(r);
                        // restore the entity body
                        request.setEntity(result.getWebRepresentation());

                    }
                }
            }
        }
        return result;
    }
}
