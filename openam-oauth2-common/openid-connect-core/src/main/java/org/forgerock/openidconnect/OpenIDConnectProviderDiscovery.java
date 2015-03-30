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
 * Copyright 2014 ForgeRock AS.
 */

package org.forgerock.openidconnect;

import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URI;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Service for discovering the OpenId Connect provider.
 *
 * @since 12.0.0
 */
public class OpenIDConnectProviderDiscovery {

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");
    private final OpenIDConnectProvider openIDConnectProvider;

    /**
     * Constructs a new OpenIDConnectProviderDiscovery.
     *
     * @param openIDConnectProvider An instance of the OpenIDConnectProvider.
     */
    @Inject
    public OpenIDConnectProviderDiscovery(OpenIDConnectProvider openIDConnectProvider) {
        this.openIDConnectProvider = openIDConnectProvider;
    }

    /**
     * Returns the response to a request to discover the OpenId Connect provider.
     *
     * @param resource The resource.
     * @param rel The rel.
     * @param deploymentUrl The deployment url of the OpenId Connect provider.
     * @param request The OAuth2 request.
     * @return A {@code Map} of the OpenId Connect provider urls.
     * @throws BadRequestException If the request is malformed.
     * @throws NotFoundException If the user cannot be found.
     */
    public Map<String, Object> discover(String resource, String rel, String deploymentUrl, OAuth2Request request)
            throws BadRequestException, NotFoundException {

        if (resource == null || resource.isEmpty()) {
            logger.error("No resource provided in discovery.");
            throw new BadRequestException("No resource provided in discovery.");
        }

        if (rel == null || rel.isEmpty() || !rel.equalsIgnoreCase("http://openid.net/specs/connect/1.0/issuer")) {
            logger.error("No or invalid rel provided in discovery.");
            throw new BadRequestException("No or invalid rel provided in discovery.");
        }

        String userid = null;

        //test if the resource is a uri
        try {
            final URI object = new URI(resource);
            if (object.getScheme().equalsIgnoreCase("https") || object.getScheme().equalsIgnoreCase("http")) {
                //resource is of the form of https://example.com/
                if (!object.getPath().isEmpty()) {
                    //resource is of the form of https://example.com/joe
                    userid = object.getPath();
                    userid = userid.substring(1,userid.length());
                }
            } else if (object.getScheme().equalsIgnoreCase("acct")) {
                //resource is not uri so only option is it is an email of form acct:joe@example.com
                String s = new String(resource);
                s = s.replaceFirst("acct:", "");
                final int firstAt = s.indexOf('@');
                userid = s.substring(0,firstAt);
            } else {
                logger.error("Invalid parameters.");
                throw new BadRequestException("Invalid parameters.");
            }
        } catch (Exception e) {
            logger.error("Invalid parameters.", e);
            throw new BadRequestException("Invalid parameters.");
        }

        if (userid != null) {
            //check if user exists on the server.

            if (!openIDConnectProvider.isUserValid(userid, request)) {
                logger.error("Invalid parameters.");
                throw new NotFoundException("Invalid parameters.");
            }
        }

        final Map<String, Object> response = new HashMap<String, Object>();
        response.put("subject", resource);
        final Set<Object> set = new HashSet<Object>();
        final Map<String, Object> objectMap = new HashMap<String, Object>();
        objectMap.put("rel", rel);
        objectMap.put("href", deploymentUrl);
        set.add(objectMap);
        response.put("links", set);

        return response;
    }
}
