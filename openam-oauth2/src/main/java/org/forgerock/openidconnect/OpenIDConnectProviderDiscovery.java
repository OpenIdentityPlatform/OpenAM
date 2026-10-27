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
 * Copyright 2014-2015 ForgeRock AS.
 * Portions copyright 2025-2026 3A Systems LLC.
 */

package org.forgerock.openidconnect;

import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
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

    /**
     * Returns the response to a request to discover the OpenId Connect provider.
     * <p>
     * The account that the resource names is not looked up. The request is anonymous and the issuer is the same
     * for every account, so an answer that differed for an account that does not exist would only tell the
     * caller which accounts exist.
     *
     * @param resource The resource.
     * @param rel The rel.
     * @param deploymentUrl The deployment url of the OpenId Connect provider.
     * @param request The OAuth2 request.
     * @return A {@code Map} of the OpenId Connect provider urls.
     * @throws BadRequestException If the request is malformed.
     */
    public Map<String, Object> discover(String resource, String rel, String deploymentUrl, OAuth2Request request)
            throws BadRequestException {

        if (resource == null || resource.isEmpty()) {
            logger.error("No resource provided in discovery.");
            throw new BadRequestException("No resource provided in discovery.");
        }

        if (rel == null || rel.isEmpty() || !rel.equalsIgnoreCase("http://openid.net/specs/connect/1.0/issuer")) {
            logger.error("No or invalid rel provided in discovery.");
            throw new BadRequestException("No or invalid rel provided in discovery.");
        }

        final URI object;
        try {
            object = new URI(resource);
        } catch (URISyntaxException e) {
            logger.error("Invalid parameters.", e);
            throw new BadRequestException("Invalid parameters.");
        }
        //resource is of the form of https://example.com/ or https://example.com/joe
        final boolean isUrl = ("https".equalsIgnoreCase(object.getScheme())
                || "http".equalsIgnoreCase(object.getScheme())) && !object.isOpaque();
        //resource is not uri so only option is it is an email of form acct:joe@example.com
        final boolean isAccount = "acct".equalsIgnoreCase(object.getScheme()) && resource.indexOf('@') >= 0;
        if (!isUrl && !isAccount) {
            logger.error("Invalid parameters.");
            throw new BadRequestException("Invalid parameters.");
        }

        final Map<String, Object> response = new HashMap<String, Object>();
        response.put("subject", resource);
        final Set<Object> set = new HashSet<Object>();
        final Map<String, Object> objectMap = new HashMap<String, Object>();
        objectMap.put("rel", rel);
        objectMap.put("href", deploymentUrl + "/oauth2");
        set.add(objectMap);
        response.put("links", set);

        return response;
    }
}
