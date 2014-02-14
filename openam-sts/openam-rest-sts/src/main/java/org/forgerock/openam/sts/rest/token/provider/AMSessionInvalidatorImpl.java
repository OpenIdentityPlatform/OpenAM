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

package org.forgerock.openam.sts.rest.token.provider;

import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenCreationException;
import org.restlet.engine.header.Header;
import org.restlet.resource.ClientResource;
import org.restlet.resource.ResourceException;
import org.restlet.util.Series;

import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;

/**
 * {@link org.forgerock.openam.sts.rest.token.provider.AMSessionInvalidator}
 */
public class AMSessionInvalidatorImpl implements AMSessionInvalidator {
    /*
    TODO: is this always hard-coded, or will it correspond to the configured cookie name?
     */
    private static final String SESSION_ID_HEADER = "iplanetDirectoryPro";
    private final URI logoutUri;
    private final Logger logger;

    public AMSessionInvalidatorImpl(String amDeploymentUrl, String jsonRestRoot, String realm, String restLogoutUriElement, Logger logger) throws URISyntaxException {
        this.logoutUri = constituteLogoutUri(amDeploymentUrl, jsonRestRoot, realm, restLogoutUriElement);
        this.logger = logger;
    }

    /*
    As in the AMTokenValidator, the constitution of a proper URL on the basis of constituents should be a first class concern,
    represented by an interface, and injected by Guice. Then the logic to insure proper trailing '/' values, etc, can
    be centralized in a single place.
     */
    private URI constituteLogoutUri(String amDeploymentUrl, String jsonRestRoot, String realm, String restLogoutUriElement) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(amDeploymentUrl);
        sb.append(jsonRestRoot);
        if (!AMSTSConstants.ROOT_REALM.equals(realm)) {
            sb.append(realm);
        }
        sb.append(restLogoutUriElement);
        return new URI(sb.toString());
    }


    @Override
    public void invalidateAMSession(String sessionId) throws TokenCreationException {
        ClientResource resource = new ClientResource(logoutUri);
        resource.setFollowingRedirects(false);
        Series<Header> headers = (Series<Header>)resource.getRequestAttributes().get(AMSTSConstants.RESTLET_HEADER_KEY);
        if (headers == null) {
            headers = new Series<Header>(Header.class);
            resource.getRequestAttributes().put(AMSTSConstants.RESTLET_HEADER_KEY, headers);
        }
        headers.set(SESSION_ID_HEADER, sessionId);
        try {
            resource.post(null);
        } catch (ResourceException e) {
            throw new TokenCreationException("Exception caught in AM Session invalidation invocation: " + e.getMessage(), e);
        }
    }
}
