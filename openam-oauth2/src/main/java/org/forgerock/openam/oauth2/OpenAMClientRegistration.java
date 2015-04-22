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
 */

package org.forgerock.openam.oauth2;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
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
import org.forgerock.guava.common.annotations.VisibleForTesting;
import org.forgerock.jaspi.modules.openid.exceptions.FailedToLoadJWKException;
import org.forgerock.jaspi.modules.openid.exceptions.OpenIdConnectVerificationException;
import org.forgerock.jaspi.modules.openid.helpers.JWKSetParser;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverService;
import org.forgerock.json.jose.jwk.JWKSet;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.oauth2.core.ClientType;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Jwt;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.PEMDecoder;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.openidconnect.Client;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.restlet.Request;

/**
 * Models an OpenAM OAuth2 and OpenId Connect client registration in the OAuth2 provider.
 *
 * @since 12.0.0
 */
public class OpenAMClientRegistration implements OpenIdConnectClientRegistration {

    private static final String DELIMITER = "\\|";
    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final AMIdentity amIdentity;
    private final SigningManager signingManager = new SigningManager();
    private final PEMDecoder pemDecoder;
    private final OpenIdResolverService resolverService;
    private final MessageDigest digest;


    /**
     * Constructs a new OpenAMClientRegistration.
     *
     * @param amIdentity The client's identity.
     * @param pemDecoder A {@code PEMDecoder} instance.
     */
    OpenAMClientRegistration(AMIdentity amIdentity, PEMDecoder pemDecoder, OpenIdResolverService resolverService)
            throws InvalidClientException {
        this.amIdentity = amIdentity;
        this.pemDecoder = pemDecoder;
        this.resolverService = resolverService;
        try {
            this.digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidClientException("SHA-256 algorithm MessageDigest not available");
        }
    }

    /**
     * {@inheritDoc}
     */
    public Set<URI> getRedirectUris() {
        Set<URI> redirectionURIs;
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
        Set<String> set;
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

    /**
     * {@inheritDoc}
     */
    public String getTokenEndpointAuthMethod() {
        final String tokenEndpointAuthMethod;
        Set<String> authMethodSet;
        try {
            authMethodSet = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.TOKEN_ENDPOINT_AUTH_METHOD);
        } catch (Exception e) {
            logger.error("Unable to get "+ OAuth2Constants.OAuth2Client.TOKEN_ENDPOINT_AUTH_METHOD +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.TOKEN_ENDPOINT_AUTH_METHOD +" from repository");
        }

        if (authMethodSet.iterator().hasNext()){
            tokenEndpointAuthMethod = authMethodSet.iterator().next();
        } else { //default to client_secret_basic
            tokenEndpointAuthMethod = Client.TokenEndpointAuthMethod.CLIENT_SECRET_BASIC.getType();
        }

        return tokenEndpointAuthMethod;
    }

    /**
     * {@inheritDoc}
     */
    public String getSubjectType() {
        final String subjectType;
        Set<String> subjectTypeSet;
        try {
            subjectTypeSet = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.SUBJECT_TYPE);
        } catch (Exception e) {
            logger.error("Unable to get "+ OAuth2Constants.OAuth2Client.SUBJECT_TYPE +" from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.SUBJECT_TYPE +" from repository");
        }

        if (subjectTypeSet.iterator().hasNext()){
            subjectType = subjectTypeSet.iterator().next();
        } else { //default to public
            subjectType = Client.SubjectType.PUBLIC.getType();
        }

        return subjectType;
    }

    @Override
    public boolean verifyJwtIdentity(OAuth2Jwt jwt) {

        try {
            switch (getClientPublicKeySelector()) {
                case JWKS:
                    return byJWKs(jwt);
                case JWKS_URI:
                    return byJWKsURI(jwt);
                default:
                    return byX509Key(jwt);
            }
        } catch (Exception e) {
            logger.error("Unable to get Client Bearer Jwt Public key from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get Client Bearer Jwt Public key from repository");
        }
    }

    private boolean byJWKs(OAuth2Jwt jwt) throws IdRepoException, SSOException,
            MalformedURLException, FailedToLoadJWKException {
        Set<String> set = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.JWKS);

        if (set == null || set.isEmpty()) {
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "No Client Bearer JWKs_URI set.");
        }

        final String jwkSetStr = set.iterator().next();
        final JWKSet jwkSet = new JWKSet(JsonValueBuilder.toJsonValue(jwkSetStr));
        final JWKSetParser setParser = new JWKSetParser(0, 0); //0 values as not using for inet comms

        final Map<String, Key> jwkMap = setParser.jwkSetToMap(jwkSet);

        final Key key = jwkMap.get(jwt.getSignedJwt().getHeader().getKeyId());

        return key != null && jwt.isValid(signingManager.newRsaSigningHandler(key));
    }

    private boolean byJWKsURI(OAuth2Jwt jwt) throws IdRepoException, SSOException, MalformedURLException {
        final Set<String> set = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.JWKS_URI);

        if (set == null || set.isEmpty()) {
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "No Client Bearer JWKs_URI set.");
        }

        final String url = set.iterator().next();

        try {
            if (resolverService.getResolverForIssuer(jwt.getSignedJwt().getClaimsSet().getIssuer()) == null) {
                boolean success =
                        resolverService.configureResolverWithJWK(jwt.getSignedJwt().getClaimsSet().getIssuer(),
                                new URL(url));
                if (!success) {
                    throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                            "Unable to configure internal JWK resolver service.");
                }
            }

            resolverService.getResolverForIssuer(
                    jwt.getSignedJwt().getClaimsSet().getIssuer()).validateIdentity(jwt.getSignedJwt());
        } catch (OpenIdConnectVerificationException e) {
            return false;
        }

        return jwt.isContentValid();

    }

    private boolean byX509Key(OAuth2Jwt jwt) throws IdRepoException, SSOException, CertificateException {

        Set<String> set = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.CLIENT_JWT_PUBLIC_KEY);

        if (set == null || set.isEmpty()) {
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "No Client Bearer Jwt Public key certificate set");
        }

        String encodedCert = set.iterator().next();
        X509Certificate certificate = pemDecoder.decodeX509Certificate(encodedCert);

        return jwt.isValid(signingManager.newRsaSigningHandler(certificate.getPublicKey()));
    }

    /**
     * Returns which of the possible selector types has been chosen by the client as
     * the location for their public key.
     *
     * @return the client public key selector
     */
    private Client.PublicKeySelector getClientPublicKeySelector() {
        Set<String> set;
        try {
            set = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.PUBLIC_KEY_SELECTOR);
        } catch (SSOException e) {
            logger.error("Unable to get " + OAuth2Constants.OAuth2Client.PUBLIC_KEY_SELECTOR + " from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.PUBLIC_KEY_SELECTOR +" from repository");
        } catch (IdRepoException e) {
            logger.error("Unable to get " + OAuth2Constants.OAuth2Client.PUBLIC_KEY_SELECTOR + " from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.PUBLIC_KEY_SELECTOR +" from repository");
        }
        return Client.PublicKeySelector.fromString(set.iterator().next());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public URI getSectorIdentifierUri() {
        final Set<String> set;
        try {
            set = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.SECTOR_IDENTIFIER_URI);

            if (set.iterator().hasNext()){
                return new URI(set.iterator().next());
            }

        } catch (SSOException e) {
            logger.error("Unable to get " + OAuth2Constants.OAuth2Client.SECTOR_IDENTIFIER_URI + " from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.SECTOR_IDENTIFIER_URI +" from repository");
        } catch (IdRepoException e) {
            logger.error("Unable to get " + OAuth2Constants.OAuth2Client.SECTOR_IDENTIFIER_URI + " from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.SECTOR_IDENTIFIER_URI +" from repository");
        } catch (URISyntaxException e) {
            logger.error("Unable to get " + OAuth2Constants.OAuth2Client.SECTOR_IDENTIFIER_URI + " from repository", e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get "+ OAuth2Constants.OAuth2Client.SECTOR_IDENTIFIER_URI +" from repository");
        }

        return null;
    }

    public String getSubValue(String id, OAuth2ProviderSettings providerSettings) {
        if (Client.SubjectType.fromString(getSubjectType()) == Client.SubjectType.PAIRWISE) {

            final String host;

            //get redirect_uris
            if (getSectorIdentifierUri() != null) {
                host = getSectorIdentifierUri().getHost();
            } else if (getRedirectUris().size() != 1 && containsMultipleRedirectUriHosts(getRedirectUris())) {
                logger.message("Must configure sector identifier uri when multiple redirect uris are specified.");
                return null;
            } else {
                host = getRedirectUris().iterator().next().getHost();
            }
            return subValueFromHost(host, id, providerSettings);
        } else {
            return id;
        }
    }

    private String subValueFromHost(String host, String resourceOwnerId, OAuth2ProviderSettings providerSettings) {
        try {
            final String concat = host + resourceOwnerId + providerSettings.getHashSalt();
            byte[] hash = digest.digest(concat.getBytes("UTF-8"));
            return Base64.encode(hash);
        } catch (UnsupportedEncodingException e) {
            logger.message("Unable to encrypt the sub value for user.");
            return null;
        } catch (ServerException e) {
            logger.message("Unable to encrypt the sub value for user.");
            return null;
        }
    }

    private boolean containsMultipleRedirectUriHosts(Set<URI> redirectUris) {
        String host = redirectUris.iterator().next().getHost();
        for (URI uri : redirectUris) {
            if (!uri.getHost().equals(host)) {
                return false;
            }
        }

        return true;
    }



}
