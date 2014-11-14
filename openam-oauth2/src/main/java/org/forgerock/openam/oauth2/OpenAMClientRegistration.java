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
import org.forgerock.guava.common.annotations.VisibleForTesting;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.oauth2.core.ClientType;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.PEMDecoder;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.restlet.Request;

import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

    private static final String DELIMITER = "\\|";
    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final AMIdentity amIdentity;
    private final SigningManager signingManager = new SigningManager();
    private final PEMDecoder pemDecoder;

    /**
     * Constructs a new OpenAMClientRegistration.
     *
     * @param amIdentity The client's identity.
     * @param pemDecoder A {@code PEMDecoder} instance.
     */
    OpenAMClientRegistration(AMIdentity amIdentity, PEMDecoder pemDecoder) {
        this.amIdentity = amIdentity;
        this.pemDecoder = pemDecoder;
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

    private Set<String[]> getDisplayName() {
        Set<String> displayName = null;
        try {
            displayName = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.NAME);
        } catch (Exception e){
            logger.error("Unable to get "+ OAuth2Constants.OAuth2Client.RESPONSE_TYPES +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.RESPONSE_TYPES +" from repository");
        }
        return splitPipeDelimited(convertAttributeValues(displayName));
    }

    @VisibleForTesting Set<String[]> splitPipeDelimited(Set<String> values) {
        Set<String[]> result = new HashSet<String[]>();
        for (String value : values) {
            if (value != null) {
                result.add(value.split(DELIMITER));
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayName(Locale locale) {
        return findLocaleSpecificString(getDisplayName(), locale);
    }

    private Set<String[]> getDisplayDescription() {
        Set<String> displayDescription = null;
        try {
            displayDescription = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.DESCRIPTION);
        } catch (Exception e){
            logger.error("Unable to get "+ OAuth2Constants.OAuth2Client.RESPONSE_TYPES +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.RESPONSE_TYPES +" from repository");
        }
        return splitPipeDelimited(convertAttributeValues(displayDescription));
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayDescription(Locale locale) {
        return findLocaleSpecificString(getDisplayDescription(), locale);
    }

    @VisibleForTesting String findLocaleSpecificString(Set<String[]> delimitedStrings, Locale locale) {
        String defaultValue = null;
        for (String language : languageStrings(locale)) {
            for (String[] value : delimitedStrings) {
                if (value.length == 2) {
                    if (value[0].equalsIgnoreCase(language)) {
                        return value[1];
                    }
                } else {
                    defaultValue = value[0];
                }
            }
        }

        return defaultValue;
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

    @VisibleForTesting List<String> languageStrings(Locale locale) {
        List<String> strings = new ArrayList<String>();
        String localeString = locale.toString();
        for (int separator = localeString.lastIndexOf('_'); separator > -1; separator = localeString.lastIndexOf('_')) {
            if (!localeString.endsWith("_")) {
                strings.add(localeString);
            }
            localeString = localeString.substring(0, separator);
        }
        if (!localeString.isEmpty()) {
            strings.add(localeString);
        }
        return strings;
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getScopeDescriptions(Locale locale) {
        final Map<String, String> descriptions = new LinkedHashMap<String, String>();
        final Map<String, String> i18nDescriptions = new LinkedHashMap<String, String>();
        final Set<String> combinedScopes = new HashSet<String>();
        combinedScopes.addAll(getAllowedGrantScopes());
        combinedScopes.addAll(getDefaultGrantScopes());

        if (combinedScopes.isEmpty()) {
            return descriptions;
        }

        Set<String[]> scopes = splitPipeDelimited(combinedScopes);
        List<String> languageStrings = languageStrings(locale);
        for (String language : languageStrings) {
            Iterator<String[]> i = scopes.iterator();
            while (i.hasNext()) {
                String[] parts = i.next();
                if (parts.length == 1) {
                    //no description or locale
                    i.remove();
                } else if (parts.length == 2) {
                    //no locale - default description
                    descriptions.put(parts[0], parts[1]);
                    i.remove();
                } else if (parts.length == 3) {
                    //locale and description
                    if (parts[1].equals(language) && !i18nDescriptions.containsKey(parts[0])) {
                        i18nDescriptions.put(parts[0], parts[2]);
                        i.remove();
                    }
                }
            }
        }

        descriptions.putAll(i18nDescriptions);
        return descriptions;
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

    @Override
    public SigningHandler getClientJwtSigningHandler() {

        try {
            Set<String> set = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.CLIENT_JWT_PUBLIC_KEY);

            if (set == null || set.isEmpty()) {
                throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                        "No Client Bearer Jwt Public key certificate set");
            }

            String encodedCert = set.iterator().next();
            X509Certificate certificate = pemDecoder.decodeX509Certificate(encodedCert);

            return signingManager.newRsaSigningHandler(certificate.getPublicKey());

        } catch (Exception e) {
            logger.error("Unable to get Client Bearer Jwt Public key from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get Client Bearer Jwt Public key from repository");
        }
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
