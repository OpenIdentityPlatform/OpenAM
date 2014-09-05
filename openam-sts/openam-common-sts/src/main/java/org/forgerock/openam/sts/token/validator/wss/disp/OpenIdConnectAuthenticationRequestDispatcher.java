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
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.token.validator.wss.disp;

import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.config.user.AuthTargetMapping;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.token.model.OpenIdConnectIdToken;
import org.restlet.engine.header.Header;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import javax.inject.Inject;
import javax.inject.Named;
import java.net.URI;

/**
 * This class is responsible for dispatching OpenID Connect ID Tokens to the OpenAM Rest authN context. The OpenIDConnect
 * authN module can be configured to pull the ID Token out of a specific header value, and this class must insure that
 * it references the ID Token value with the same header value.
 */
public class OpenIdConnectAuthenticationRequestDispatcher implements TokenAuthenticationRequestDispatcher<OpenIdConnectIdToken> {
    private final String crestVersion;

    @Inject
    OpenIdConnectAuthenticationRequestDispatcher(@Named(AMSTSConstants.CREST_VERSION) String crestVersion) {
        this.crestVersion = crestVersion;
    }

    @Override
    public Representation dispatch(URI uri, AuthTargetMapping.AuthTarget target, OpenIdConnectIdToken token) throws TokenValidationException {
        ClientResource resource = new ClientResource(uri);
        resource.setFollowingRedirects(false);
        Series<Header> headers = (Series<Header>)resource.getRequestAttributes().get(AMSTSConstants.RESTLET_HEADER_KEY);
        if (headers == null) {
            headers = new Series<Header>(Header.class);
            resource.getRequestAttributes().put(AMSTSConstants.RESTLET_HEADER_KEY, headers);
        }
        if (target == null) {
            throw new TokenValidationException(org.forgerock.json.resource.ResourceException.BAD_REQUEST,
                    "When validatating OIDC tokens, an AuthTarget needs to be configured with a Map containing a String " +
                            "entry referenced by key" + AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_AUTH_TARGET_HEADER_KEY +
                            " which specifies the header name which will reference the OIDC ID Token.");
        }
        Object headerKey = target.getContext().get(AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_AUTH_TARGET_HEADER_KEY);
        if (!(headerKey instanceof String)) { //checks both for null and String
            throw new TokenValidationException(org.forgerock.json.resource.ResourceException.BAD_REQUEST,
                    "When validatating OIDC tokens, an AuthTarget needs to be configured with a Map containing a String " +
                            "entry referenced by key" + AMSTSConstants.OPEN_ID_CONNECT_ID_TOKEN_AUTH_TARGET_HEADER_KEY +
                            " which specifies the header name which will reference the OIDC ID Token.");
        }
        headers.set((String)headerKey, token.getTokenValue());
        headers.set(AMSTSConstants.CREST_VERSION_HEADER_KEY, crestVersion);
        try {
            return resource.post(null);
        } catch (ResourceException e) {
            throw new TokenValidationException(e.getStatus().getCode(), "Exception caught posting OpenIdConnectIdToken " +
                    "to rest authN: " + e, e);
        }
    }
}
