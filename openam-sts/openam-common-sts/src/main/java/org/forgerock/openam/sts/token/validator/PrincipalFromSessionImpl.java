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

package org.forgerock.openam.sts.token.validator;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.STSPrincipal;
import org.forgerock.openam.sts.TokenValidationException;
import org.forgerock.openam.sts.token.UrlConstituentCatenator;
import org.restlet.engine.header.Header;
import org.restlet.representation.Representation;
import org.restlet.resource.ClientResource;
import org.restlet.util.Series;
import org.slf4j.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.IOException;
import java.security.Principal;
import java.util.Map;

/**
 * @see org.forgerock.openam.sts.token.validator.PrincipalFromSession
 */
public class PrincipalFromSessionImpl implements PrincipalFromSession {
    private static final String ID = "id";

    private final UrlConstituentCatenator urlConstituentCatenator;
    private final String amDeploymentUrl;
    private final String amJsonRestBase;
    private final String realm;
    private final String amRestIdFromSessionUriElement;
    private final String amSessionCookieName;
    private final Logger logger;

    @Inject
    public PrincipalFromSessionImpl(
            @Named(AMSTSConstants.AM_DEPLOYMENT_URL) String amDeploymentUrl,
            @Named(AMSTSConstants.AM_REST_AUTHN_JSON_ROOT) String jsonRestBase,
            @Named(AMSTSConstants.REST_ID_FROM_SESSION_URI_ELEMENT) String idFromSessionUriElement,
            @Named(AMSTSConstants.AM_SESSION_COOKIE_NAME) String amSessionCookieName,
            @Named (AMSTSConstants.REALM) String realm,
            UrlConstituentCatenator urlConstituentCatenator,
            Logger logger) {
        this.amDeploymentUrl = amDeploymentUrl;
        this.amJsonRestBase = jsonRestBase;
        this.amRestIdFromSessionUriElement = idFromSessionUriElement;
        this.amSessionCookieName = amSessionCookieName;
        this.realm = realm;
        this.urlConstituentCatenator = urlConstituentCatenator;
        this.logger = logger;
    }
    @Override
    public Principal getPrincipalFromSession(String sessionId) throws TokenValidationException {
        return obtainPrincipalFromSession(constitutePrincipalFromSessionUrl(), sessionId);
    }

    /**
     * Creates the String representing the url at which the principal id from session token functionality can be
     * consumed.
     * @return A String representing the url of OpenAM's Restful principal from session id service
     */
    private String constitutePrincipalFromSessionUrl() {
        StringBuilder sb = new StringBuilder(urlConstituentCatenator.catenateUrlConstituents(amDeploymentUrl, amJsonRestBase));
        if (!AMSTSConstants.ROOT_REALM.equals(realm)) {
            sb = urlConstituentCatenator.catentateUrlConstituent(sb, realm);
        }
        sb = urlConstituentCatenator.catentateUrlConstituent(sb, amRestIdFromSessionUriElement);
        return sb.toString();
    }

    private Principal obtainPrincipalFromSession(String sessionToUsernameUrl, String sessionId) throws TokenValidationException {
        if ((sessionId == null) || sessionId.isEmpty()) {
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR,
                    "the sessionId passed to PrincipalFromSession is null or empty.");
        }
        logger.debug("sessionToUsernameUrl: " + sessionToUsernameUrl);
        ClientResource resource = new ClientResource(sessionToUsernameUrl);
        resource.setFollowingRedirects(false);
        Series<Header> headers = (Series<Header>)resource.getRequestAttributes().get(AMSTSConstants.RESTLET_HEADER_KEY);
        if (headers == null) {
            headers = new Series<Header>(Header.class);
            resource.getRequestAttributes().put(AMSTSConstants.RESTLET_HEADER_KEY, headers);
        }
        headers.set(AMSTSConstants.COOKIE, amSessionCookieName + AMSTSConstants.EQUALS + sessionId);
        headers.set(AMSTSConstants.CONTENT_TYPE, AMSTSConstants.APPLICATION_JSON);
        headers.set(AMSTSConstants.ACCEPT, AMSTSConstants.APPLICATION_JSON);
        Representation representation;
        try {
            representation = resource.post(null);
        } catch (org.restlet.resource.ResourceException e) {
            throw new TokenValidationException(e.getStatus().getCode(), "Exception caught POSTing rest call to " +
                    "validate OpenAM session: " + e, e);
        }
        Map<String,Object> responseAsMap;
        try {
            responseAsMap = new ObjectMapper().readValue(representation.getText(),
                    new TypeReference<Map<String,Object>>() {});
        } catch (IOException ioe) {
            String message = "Exception caught getting the text of idFromSession response: " + ioe;
            logger.error(message, ioe);
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR, message, ioe);
        }
        String principalName = (String)responseAsMap.get(ID);
        if ((principalName != null) && !principalName.isEmpty()) {
            return new STSPrincipal(principalName);
        } else {
            throw new TokenValidationException(ResourceException.INTERNAL_ERROR,
                    "id returned from idFromSession is null or empty. The returned value: " + principalName);
        }
    }
}
