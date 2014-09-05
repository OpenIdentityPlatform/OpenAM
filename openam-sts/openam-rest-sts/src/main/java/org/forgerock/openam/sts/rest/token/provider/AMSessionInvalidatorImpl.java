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
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
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
    private final URI logoutUri;
    private final String amSessionCookieName;
    private final String crestVersion;
    private final Logger logger;

    public AMSessionInvalidatorImpl(String amDeploymentUrl,
                                    String jsonRestRoot,
                                    String realm,
                                    String restLogoutUriElement,
                                    String amSessionCookieName,
                                    UrlConstituentCatenator urlConstituentCatenator,
                                    String crestVersion,
                                    Logger logger) throws URISyntaxException {
        this.logoutUri = constituteLogoutUri(amDeploymentUrl, jsonRestRoot, realm, restLogoutUriElement, urlConstituentCatenator);
        this.amSessionCookieName = amSessionCookieName;
        this.crestVersion = crestVersion;
        this.logger = logger;
    }

    private URI constituteLogoutUri(String amDeploymentUrl,
                                    String jsonRestRoot,
                                    String realm,
                                    String restLogoutUriElement,
                                    UrlConstituentCatenator urlConstituentCatenator) throws URISyntaxException {
        StringBuilder sb = new StringBuilder(urlConstituentCatenator.catenateUrlConstituents(amDeploymentUrl, jsonRestRoot));
        if (!AMSTSConstants.ROOT_REALM.equals(realm)) {
            sb = urlConstituentCatenator.catentateUrlConstituent(sb, realm);
        }
        sb = urlConstituentCatenator.catentateUrlConstituent(sb, restLogoutUriElement);
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
        headers.set(amSessionCookieName, sessionId);
        headers.set(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
        headers.set(AMSTSConstants.CREST_VERSION_HEADER_KEY, crestVersion);
        try {
            resource.post(null);
        } catch (ResourceException e) {
            throw new TokenCreationException(e.getStatus().getCode(),
                    "Exception caught in AM Session invalidation invocation against url: " + logoutUri +
                            ". Exception: " + e.getMessage(), e);
        }
    }
}
