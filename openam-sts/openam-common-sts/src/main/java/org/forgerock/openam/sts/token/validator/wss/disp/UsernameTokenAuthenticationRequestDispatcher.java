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
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2013-2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token.validator.wss.disp;

import org.apache.ws.security.message.token.UsernameToken;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.TokenValidationException;
import org.restlet.engine.header.Header;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import java.net.URI;
/**
 * This class is responsible for dispatching the credential state encapsulated in UsernameTokens to the
 * OpenAM REST authN context.
 */
public class UsernameTokenAuthenticationRequestDispatcher implements TokenAuthenticationRequestDispatcher<UsernameToken> {
    private static final String USERNAME = "X-OpenAM-Username";
    private static final String PASSWORD = "X-OpenAM-Password";
    private static final String CONTENT_TYPE = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    @Override
    public Representation dispatch(URI uri, AuthTargetMapping.AuthTarget target, UsernameToken token) throws TokenValidationException {
        ClientResource resource = new ClientResource(uri);
        resource.setFollowingRedirects(false);
        Series<Header> headers = (Series<Header>)resource.getRequestAttributes().get(AMSTSConstants.RESTLET_HEADER_KEY);
        if (headers == null) {
            headers = new Series<Header>(Header.class);
            resource.getRequestAttributes().put(AMSTSConstants.RESTLET_HEADER_KEY, headers);
        }
        headers.set(USERNAME, token.getName());
        headers.set(PASSWORD, token.getPassword());
        headers.set(CONTENT_TYPE, APPLICATION_JSON);
        try {
            return resource.post(null);
        } catch (ResourceException e) {
            throw new TokenValidationException(e.getStatus().getCode(), "Exception caught posting to json client: " + e, e);
        }
    }
}
