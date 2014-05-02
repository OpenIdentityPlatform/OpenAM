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

package org.forgerock.openam.oauth2;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.oauth2.core.ClientType;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.restlet.Request;

import java.net.URI;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Models an OpenAM OAuth2 and OpenId Connect client registration in the OAuth2 provider.
 *
 *
 * @since 12.0.0
 */
public class OpenAMClientRegistration implements OpenIdConnectClientRegistration {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final AMIdentity amIdentity;

    /**
     * Constructs a new OpenAMClientRegistration.
     *
     * @param amIdentity The client's identity.
     */
    OpenAMClientRegistration(AMIdentity amIdentity) {
        this.amIdentity = amIdentity;
    }

    /**
     * {@inheritDoc}
     */
    public Set<URI> getRedirectUris() {
        Set<URI> redirectionURIs = null;
        try {
            Set<String> redirectionURIsSet = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.REDIRECT_URI);
            redirectionURIsSet = convertAttributeValues(redirectionURIsSet);
            redirectionURIs = new HashSet<URI>();
            for (String uri : redirectionURIsSet){
                redirectionURIs.add(URI.create(uri));
            }
        } catch (Exception e){
            logger.error("Unable to get "+ OAuth2Constants.OAuth2Client.REDIRECT_URI +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.REDIRECT_URI +" from repository");
        }
        return redirectionURIs;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getAllowedResponseTypes() {
        Set<String> set = null;
        try {
            set = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.RESPONSE_TYPES);
        } catch (Exception e){
            logger.error("Unable to get "+ OAuth2Constants.OAuth2Client.RESPONSE_TYPES +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.RESPONSE_TYPES +" from repository");
        }
        return convertAttributeValues(set);
    }

    /**
     * {@inheritDoc}
     */
    public String getClientSecret() {
        Set<String> set;
        try {
            set = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.USERPASSWORD);
        } catch (Exception e) {
            logger.error("Unable to get "+ OAuth2Constants.OAuth2Client.USERPASSWORD +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.USERPASSWORD +" from repository");
        }
        return set.iterator().next();
    }

    /**
     * {@inheritDoc}
     */
    public String getClientId() {
        return amIdentity.getName();
    }

    /**
     * {@inheritDoc}
     */
    public String getAccessTokenType() {
        return "Bearer";
    }

    private Set<String> getDisplayName() {
        Set<String> displayName = null;
        try {
            displayName = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.NAME);
        } catch (Exception e){
            logger.error("Unable to get "+ OAuth2Constants.OAuth2Client.RESPONSE_TYPES +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.RESPONSE_TYPES +" from repository");
        }
        return convertAttributeValues(displayName);
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayName(String locale) {
        String defaultName = null;
        final String DELIMITER = "|";
        for (String name : getDisplayName()) {
            if (name.contains(DELIMITER)) {
                int locationOfDelimiter = name.indexOf(DELIMITER);
                if (name.substring(0, locationOfDelimiter).equalsIgnoreCase(locale)) {
                    return name.substring(locationOfDelimiter+1, name.length());
                }
            } else {
                defaultName = name;
            }
        }

        return defaultName;
    }

    private Set<String> getDisplayDescription() {
        Set<String> displayDescription = null;
        try {
            displayDescription = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.DESCRIPTION);
        } catch (Exception e){
            logger.error("Unable to get "+ OAuth2Constants.OAuth2Client.RESPONSE_TYPES +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.RESPONSE_TYPES +" from repository");
        }
        return convertAttributeValues(displayDescription);
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayDescription(String locale) {
        String defaultName = null;
        final String DELIMITER = "|";
        for (String name : getDisplayDescription()) {
            if (name.contains(DELIMITER)) {
                int locationOfDelimiter = name.indexOf(DELIMITER);
                if (name.substring(0, locationOfDelimiter).equalsIgnoreCase(locale)) {
                    return name.substring(locationOfDelimiter+1, name.length());
                }
            } else {
                defaultName = name;
            }
        }

        return defaultName;
    }

    private Set<String> getAllowedGrantScopes() {
        Set<String> scopes = null;
        try {
            scopes = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.SCOPES);
        } catch (Exception e) {
            logger.error("Unable to get "+ OAuth2Constants.OAuth2Client.SCOPES +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.SCOPES +" from repository");
        }
        return convertAttributeValues(scopes);
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getAllowedScopeDescriptions(String locale) {
        final String DELIMITER = "\\|";
        final Map<String, String> scopeDescriptions = new LinkedHashMap<String, String>();
        for (final String scopeDescription : getAllowedGrantScopes()) {
            final String[] parts = scopeDescription.split(DELIMITER);
            if (parts != null) {
                //no description or locale
                if (parts.length == 1){
                    continue;
                } else if (parts.length == 2){
                    //no locale add description
                    scopeDescriptions.put(parts[0], parts[1]);
                } else if (parts.length == 3){
                    //locale and description
                    if (parts[1].equalsIgnoreCase(locale)){
                        scopeDescriptions.put(parts[0], parts[2]);
                    } else {
                        //not the right locale
                        continue;
                    }
                } else {
//                        OAuth2Utils.DEBUG.warn("Scope was input into the client settings in the wrong format for scope: " + scopeDescription);
                    continue;
                }
            }
        }
        return scopeDescriptions;
    }

    private Set<String> getDefaultGrantScopes() {
        Set<String> scopes = null;
        try {
            scopes = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.DEFAULT_SCOPES);
        } catch (Exception e){
            logger.error("Unable to get "+ OAuth2Constants.OAuth2Client.DEFAULT_SCOPES +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.DEFAULT_SCOPES +" from repository");
        }
        return convertAttributeValues(scopes);
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getDefaultScopes() {
        return parseScope(getDefaultGrantScopes());
    }

    private Set<String> parseScope(final Set<String> maximumScope) {
        Set<String> cleanScopes = new TreeSet<String>();
        for (String s : maximumScope) {
            int index = s.indexOf("|");
            if (index == -1){
                cleanScopes.add(s);
                continue;
            }
            cleanScopes.add(s.substring(0,index));
        }

        return cleanScopes;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getAllowedScopes() {
        return parseScope(getAllowedGrantScopes());
    }

    /**
     * {@inheritDoc}
     */
    public boolean isConfidential() {
        return ClientType.CONFIDENTIAL.equals(getClientType());
    }

    /**
     * {@inheritDoc}
     */
    public String getClientSessionURI() {
        Set<String> set = null;
        try {
            set = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.CLIENT_SESSION_URI);
        } catch (Exception e) {
            logger.error("Unable to get "+ OAuth2Constants.OAuth2Client.CLIENT_SESSION_URI +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.CLIENT_SESSION_URI +" from repository");
        }
        return set.iterator().next();
    }

    /**
     * {@inheritDoc}
     */
    public ClientType getClientType() {
        final ClientType clientType;
        try {
            Set<String> clientTypeSet = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.CLIENT_TYPE);
            if (clientTypeSet.iterator().next().equalsIgnoreCase("CONFIDENTIAL")){
                clientType = ClientType.CONFIDENTIAL;
            } else {
                clientType = ClientType.PUBLIC;
            }
        } catch (Exception e) {
            logger.error("Unable to get "+ OAuth2Constants.OAuth2Client.CLIENT_TYPE +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.CLIENT_TYPE +" from repository");
        }
        return clientType;
    }

    private Set<String> convertAttributeValues(Set<String> input) {
        Set<String> result = new HashSet<String>();
        for (String param : input) {
            int idx = param.indexOf('=');
            if (idx != -1) {
                String value = param.substring(idx + 1).trim();
                if (!value.isEmpty()) {
                    result.add(value);
                }
            }
        }

        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String getIDTokenSignedResponseAlgorithm() {
        final Set<String> set;
        try {
            set = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG);
        } catch (Exception e) {
            logger.error("Unable to get "+ OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG +" from repository");
        }
        if (set.iterator().hasNext()){
            return set.iterator().next();
        }
        return null;
    }
}
