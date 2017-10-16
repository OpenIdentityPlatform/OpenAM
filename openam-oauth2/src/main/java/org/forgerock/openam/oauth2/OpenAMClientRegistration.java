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
 * Copyright 2014-2016 ForgeRock AS.
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd.
 */

package org.forgerock.openam.oauth2;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.google.common.annotations.VisibleForTesting;
import org.forgerock.http.util.MultiValueMap;
import org.forgerock.jaspi.modules.openid.exceptions.FailedToLoadJWKException;
import org.forgerock.jaspi.modules.openid.exceptions.OpenIdConnectVerificationException;
import org.forgerock.jaspi.modules.openid.helpers.JWKSetParser;
import org.forgerock.jaspi.modules.openid.resolvers.OpenIdResolver;
import org.forgerock.jaspi.modules.openid.resolvers.SharedSecretOpenIdResolverImpl;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverService;
import org.forgerock.json.jose.exceptions.JweException;
import org.forgerock.json.jose.jwe.EncryptionMethod;
import org.forgerock.json.jose.jwe.JweAlgorithm;
import org.forgerock.json.jose.jwk.JWKSet;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithmType;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.oauth2.core.ClientType;
import org.forgerock.oauth2.core.OAuth2Jwt;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.PEMDecoder;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.JsonValueBuilder;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openidconnect.Client;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.restlet.Request;

import com.iplanet.sso.SSOException;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.shared.encode.Base64;

/**
 * Models an OpenAM OAuth2 and OpenId Connect client registration in the OAuth2 provider.
 *
 * @since 12.0.0
 */
public class OpenAMClientRegistration implements OpenIdConnectClientRegistration {

    private static final String DELIMITER = "\\|";
    private static final Comparator<? super String[]> I18N_SPECIFICITY_COMPARATOR = new Comparator<String[]>() {
        @Override
        public int compare(String[] o1, String[] o2) {
            return o1.length == o2.length ? o2[0].length() - o1[0].length() : o2.length - o1.length;
        }
    };
    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final AMIdentity amIdentity;
    private final SigningManager signingManager = new SigningManager();
    private final PEMDecoder pemDecoder;
    private final OpenIdResolverService resolverService;
    private final MessageDigest digest;
    private final OAuth2ProviderSettings providerSettings;


    /**
     * Constructs a new OpenAMClientRegistration.
     *
     * @param amIdentity The client's identity.
     * @param pemDecoder A {@code PEMDecoder} instance.
     */
    OpenAMClientRegistration(AMIdentity amIdentity, PEMDecoder pemDecoder, OpenIdResolverService resolverService,
            OAuth2ProviderSettings providerSettings, ClientAuthenticationFailureFactory failureFactory)
            throws InvalidClientException {
        this.amIdentity = amIdentity;
        this.pemDecoder = pemDecoder;
        this.resolverService = resolverService;
        this.providerSettings = providerSettings;
        try {
            this.digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw failureFactory.getException("SHA-256 algorithm MessageDigest not available");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<URI> getRedirectUris() {
        return Utils.getAttributeValuesAsUris(amIdentity, OAuth2Constants.OAuth2Client.REDIRECT_URI, logger);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<URI> getPostLogoutRedirectUris() {
        return Utils.getAttributeValuesAsUris(amIdentity, OAuth2Constants.OAuth2Client.POST_LOGOUT_URI, logger);
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getAllowedResponseTypes() {
        return Utils.getAttributeValuesAsSet(amIdentity, OAuth2Constants.OAuth2Client.RESPONSE_TYPES, logger);
    }

    /**
     * {@inheritDoc}
     */
    public String getClientSecret() {
        return Utils.getAttributeValueFromSet(amIdentity, OAuth2Constants.OAuth2Client.USERPASSWORD, logger);
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
        return OAuth2Constants.Bearer.BEARER;
    }

    private List<String[]> getDisplayName(String attributeName) {
        try {
            Set<String> displayName = amIdentity.getAttribute(attributeName);
            return splitPipeDelimited(Utils.stripAttributeNameFromValue(displayName), "name").get("name");
        } catch (Exception e){
            throw Utils.createException(OAuth2Constants.OAuth2Client.NAME, e, logger);
        }
    }

    @VisibleForTesting
    MultiValueMap<String, String[]> splitPipeDelimited(Set<String> values, String defaultKey) {
        MultiValueMap<String, String[]> result = new MultiValueMap<>(new HashMap<String, List<String[]>>());
        for (String value : values) {
            if (value != null) {
                String[] split;
                if ((value.indexOf("|") == value.length())) {
                    // If one pipe is included in and ended with.
                    split = value.split(DELIMITER, 2);
                } else {
                    split = value.split(DELIMITER, 3);
                }
                if (defaultKey != null) {
                    result.add(defaultKey, split);
                } else {
                    result.add(split[0], Arrays.copyOfRange(split, 1, split.length));
                }
            }
        }
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayName(Locale locale) {
        List<String[]> displayName = getDisplayName(OAuth2Constants.OAuth2Client.NAME);
        if (displayName == null || displayName.isEmpty()) {
            displayName = getDisplayName(OAuth2Constants.OAuth2Client.CLIENT_NAME);
        }
        return findLocaleSpecificString(displayName, locale);
    }

    private List<String[]> getDisplayDescription() {
        try {
            Set<String> displayDescription = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.DESCRIPTION);
            return splitPipeDelimited(Utils.stripAttributeNameFromValue(displayDescription), "name").get("name");
        } catch (Exception e){
            throw Utils.createException(OAuth2Constants.OAuth2Client.DESCRIPTION, e, logger);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayDescription(Locale locale) {
        return findLocaleSpecificString(getDisplayDescription(), locale);
    }

    @VisibleForTesting String findLocaleSpecificString(Collection<String[]> delimitedStrings, Locale locale) {
        String defaultValue = null;
        if (delimitedStrings == null) {
            return defaultValue;
        }
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
            throw Utils.createException(OAuth2Constants.OAuth2Client.SCOPES, e, logger);
        }
        return Utils.stripAttributeNameFromValue(scopes);
    }

    private Set<String> getClaimStrings() {
        Set<String> scopes = null;
        try {
            scopes = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.CLAIMS);
        } catch (Exception e) {
            throw Utils.createException(OAuth2Constants.OAuth2Client.SCOPES, e, logger);
        }
        return Utils.stripAttributeNameFromValue(scopes);
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
    public Map<String, String> getScopeDescriptions(Locale locale) throws ServerException {
        final Set<String> combinedScopes = new HashSet<>();
        combinedScopes.addAll(getAllowedGrantScopes());
        combinedScopes.addAll(getDefaultGrantScopes());
        return getTranslations(locale, combinedScopes, providerSettings.getSupportedScopesWithTranslations());
    }

    /**
     * {@inheritDoc}
     */
    public Map<String, String> getClaimDescriptions(Locale locale) throws ServerException {
        final HashSet<String> clientClaims = new HashSet<>(getClaimStrings());
        return getTranslations(locale, clientClaims, providerSettings.getSupportedClaimsWithTranslations());
    }

    private Map<String, String> getTranslations(Locale locale, Set<String> configured, Set<String> defaults) {
        final Map<String, String> descriptions = new LinkedHashMap<>();
        final Set<String> hiddenI18nDescriptions = new HashSet<>();

        if (configured.isEmpty() && defaults.isEmpty()) {
            return descriptions;
        }

        MultiValueMap<String, String[]> allTranslations = splitPipeDelimited(configured, null);
        List<String> languageStrings = languageStrings(locale);
        for (Map.Entry<String, List<String[]>> translation : allTranslations.entrySet()) {
            setTranslation(languageStrings, translation.getKey(), translation.getValue(), descriptions,
                    hiddenI18nDescriptions, false);
        }

        MultiValueMap<String, String[]> defaultTranslations = splitPipeDelimited(defaults, null);
        for (Map.Entry<String, List<String[]>> defaultTranslation : defaultTranslations.entrySet()) {
            String translated = defaultTranslation.getKey();
            if (!descriptions.containsKey(translated) && !hiddenI18nDescriptions.contains(translated)) {
                setTranslation(languageStrings, translated, defaultTranslation.getValue(), descriptions,
                        hiddenI18nDescriptions, true);
            }
        }

        return descriptions;
    }

    private void setTranslation(List<String> languageStrings, String key, List<String[]> values,
            Map<String, String> descriptions, Set<String> hiddenI18nDescriptions, boolean addKeyAsDefault) {
        if (values == null) {
            return;
        }

        Collections.sort(values, I18N_SPECIFICITY_COMPARATOR);

        // Check for config specifc to a preferred language
        for (String language : languageStrings) {
            for (String[] value : values) {
                if (value.length == 2 && value[0].equals(language)) {
                    if (StringUtils.isNotBlank(value[1])) {
                        descriptions.put(key, value[1]);
                    } else {
                        hiddenI18nDescriptions.add(key);
                    }
                    return;
                }
            }
        }

        // No language match, use default value
        for (String[] value : values) {
            if (value.length == 1) {
                if (StringUtils.isNotBlank(value[0])) {
                    descriptions.put(key, value[0]);
                } else {
                    hiddenI18nDescriptions.add(key);
                }
                return;
            }
        }

        if (addKeyAsDefault) {
            descriptions.put(key, key);
        }
    }

    private Set<String> getDefaultGrantScopes() {
        Set<String> scopes = null;
        try {
            scopes = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.DEFAULT_SCOPES);
        } catch (Exception e){
            throw Utils.createException(OAuth2Constants.OAuth2Client.DEFAULT_SCOPES, e, logger);
        }
        return Utils.stripAttributeNameFromValue(scopes);
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
            throw Utils.createException(OAuth2Constants.OAuth2Client.CLIENT_SESSION_URI, e, logger);
        }
        return set.iterator().next();
    }

    /**
     * Gets the client's type.
     *
     * @return The client's type.
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
            throw Utils.createException(OAuth2Constants.OAuth2Client.CLIENT_TYPE, e, logger);
        }
        return clientType;
    }

    /**
     * {@inheritDoc}
     */
    public long getAuthorizationCodeLifeTime(OAuth2ProviderSettings providerSettings) throws ServerException {
        return getTokenLifeTime(OAuth2Constants.OAuth2Client.AUTHORIZATION_CODE_LIFE_TIME,
                providerSettings.getAuthorizationCodeLifetime());
    }

    /**
     * {@inheritDoc}
     */
    public long getAccessTokenLifeTime(OAuth2ProviderSettings providerSettings) throws ServerException {
        return getTokenLifeTime(OAuth2Constants.OAuth2Client.ACCESS_TOKEN_LIFE_TIME,
                providerSettings.getAccessTokenLifetime());
    }

    /**
     * {@inheritDoc}
     */
    public long getRefreshTokenLifeTime(OAuth2ProviderSettings providerSettings) throws ServerException {
        return getTokenLifeTime(OAuth2Constants.OAuth2Client.REFRESH_TOKEN_LIFE_TIME,
                providerSettings.getRefreshTokenLifetime());
    }

    /**
     * {@inheritDoc}
     */
    public long getJwtTokenLifeTime(OAuth2ProviderSettings providerSettings) throws ServerException {
        return getTokenLifeTime(OAuth2Constants.OAuth2Client.JWT_TOKEN_LIFE_TIME,
                providerSettings.getOpenIdTokenLifetime());
    }

    private long getTokenLifeTime(String tokenLifeTimeProperty, long defaultLifeTime) {
        long tokenLifeTime = 0L;
        try {
            Set<String> lifeTimeSet = amIdentity.getAttribute(tokenLifeTimeProperty);
            if (lifeTimeSet != null && !lifeTimeSet.isEmpty()) {
                tokenLifeTime = Long.parseLong(lifeTimeSet.iterator().next());
            }
        } catch (SSOException | IdRepoException e) {
            throw Utils.createException(tokenLifeTimeProperty, e, logger);
        }
        if (tokenLifeTime == 0) {
            tokenLifeTime = defaultLifeTime;
        }
        return tokenLifeTime * 1000;
    }

    /**
     * {@inheritDoc}
     */
    public String getIDTokenSignedResponseAlgorithm() {
        final Set<String> set;
        try {
            set = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG);
        } catch (Exception e) {
            throw Utils.createException(OAuth2Constants.OAuth2Client.IDTOKEN_SIGNED_RESPONSE_ALG, e, logger);
        }
        if (set.iterator().hasNext()){
            return set.iterator().next();
        }
        return null;
    }

    @Override
    public boolean isIDTokenEncryptionEnabled() {
        try {
            Set<String> attribute = amIdentity.getAttribute("idTokenEncryptionEnabled");
            if (attribute == null || attribute.isEmpty()) {
                return false;
            }
            return Boolean.valueOf(attribute.iterator().next());
        } catch (Exception e) {
            throw Utils.createException("idTokenEncryptionEnabled", e, logger);
        }
    }

    @Override
    public String getIDTokenEncryptionResponseAlgorithm() {
        return getAttribute("idTokenEncryptionAlgorithm");
    }

    @Override
    public String getIDTokenEncryptionResponseMethod() {
        return getAttribute("idTokenEncryptionMethod");
    }

    @Override
    public Key getIDTokenEncryptionKey() {
        String algorithmStr = getIDTokenEncryptionResponseAlgorithm();
        if (algorithmStr == null) {
            return null;
        }
        try {
            JweAlgorithm algorithm = JweAlgorithm.parseAlgorithm(algorithmStr);
            switch (algorithm) {
                case RSAES_PKCS1_V1_5:
                case RSA_OAEP:
                case RSA_OAEP_256:
                    return getRSAPublicEncryptionKey();
                case A128KW:
                    return getSymmetricEncryptionKey(128);
                case A192KW:
                    return getSymmetricEncryptionKey(192);
                case A256KW:
                    return getSymmetricEncryptionKey(256);
                case DIRECT:
                    final EncryptionMethod encryptionMethod =
                            EncryptionMethod.parseMethod(getIDTokenEncryptionResponseMethod());
                    return getSymmetricEncryptionKey(encryptionMethod.getKeySize());
                default:
                    throw new JweException("Unknown JWE Algorithm: " + algorithm);
            }
        } catch (JweException e) {
            throw Utils.createException("encryption key", e, logger);
        }
    }

    private PublicKey getRSAPublicEncryptionKey() {
        try {
            Set<String> set = amIdentity.getAttribute("idTokenPublicEncryptionKey");
            if (set == null || set.isEmpty()) {
                return null;
            }
            String encodedCert = set.iterator().next();
            return pemDecoder.decodeRSAPublicKey(encodedCert);
        } catch (Exception e) {
            throw Utils.createException("idTokenPublicEncryptionKey", e, logger);
        }
    }

    private SecretKey getSymmetricEncryptionKey(final int keySize) {
        if (keySize > 512) {
            throw new JweException("Cannot derive symmetric key for keySize > 512 bits");
        }
        if (!isConfidential()) {
            throw new JweException("Symmetric encryption can only be used by confidential clients");
        }
        // See http://openid.net/specs/openid-connect-core-1_0.html#Encryption for the details of this algorithm
        final String hashAlgorithm = keySize <= 256 ? "SHA-256" : keySize <= 384 ? "SHA-384" : "SHA-512";

        byte[] secret = null;
        String clientSecret = getClientSecret();
        if (StringUtils.isEmpty(clientSecret)) {
            //SecretKeySpec will throw error if algorithm is null or key is null or empty.
            throw new IllegalArgumentException("client secret must not be null.");
        } else {
            secret = clientSecret.getBytes(StandardCharsets.UTF_8);
            try {
                final MessageDigest hash = MessageDigest.getInstance(hashAlgorithm);
                final byte[] digest = Arrays.copyOfRange(hash.digest(secret), 0, keySize / 8);
                return new SecretKeySpec(digest, "AES");
            } catch (GeneralSecurityException e) {
                throw new JweException(e);
            }
        }
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
            throw Utils.createException(OAuth2Constants.OAuth2Client.TOKEN_ENDPOINT_AUTH_METHOD, e, logger);
        }

        if (CollectionUtils.isNotEmpty(authMethodSet)){
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
            throw Utils.createException(OAuth2Constants.OAuth2Client.SUBJECT_TYPE, e, logger);
        }

        subjectType = CollectionUtils.getFirstItem(subjectTypeSet, Client.SubjectType.PUBLIC.getType());

        return subjectType;
    }

    @Override
    public boolean verifyJwtIdentity(final OAuth2Jwt jwt) {
        final JwsAlgorithm signatureAlgorithm = JwsAlgorithm.valueOf(getIDTokenSignedResponseAlgorithm());
        if (signatureAlgorithm.getAlgorithmType() == JwsAlgorithmType.HMAC) {
            return verifyJwtBySharedSecret(jwt);
        } else {
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
                throw Utils.createException("Client Bearer Jwt Public key", e, logger);
            }
        }
    }

    @Override
    public boolean isConsentImplied() {
        return Boolean.parseBoolean(getAttribute(OAuth2Constants.OAuth2Client.IS_CONSENT_IMPLIED));
    }

    private boolean verifyJwtBySharedSecret(final OAuth2Jwt jwt) {
        final String issuer = jwt.getSignedJwt().getClaimsSet().getIssuer();
        OpenIdResolver resolver = new SharedSecretOpenIdResolverImpl(issuer, getClientSecret());
        try {
            resolver.validateIdentity(jwt.getSignedJwt());
            return jwt.isContentValid() && jwt.isIntendedForAudience(getClientId());
        } catch (OpenIdConnectVerificationException e) {
            return false;
        }
    }

    private boolean byJWKs(OAuth2Jwt jwt) throws IdRepoException, SSOException,
            MalformedURLException, FailedToLoadJWKException {
        Set<String> set = amIdentity.getAttribute(OAuth2Constants.OAuth2Client.JWKS);

        final String jwkSetStr = CollectionUtils.getFirstItem(set);
        if (jwkSetStr == null) {
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "No Client Bearer JWK set.");
        }

        final JWKSet jwkSet = new JWKSet(JsonValueBuilder.toJsonValue(jwkSetStr)
                .get(OAuth2Constants.JWTTokenParams.KEYS));
        final JWKSetParser setParser = new JWKSetParser(0, 0); //0 values as not using for inet comms

        final Map<String, Key> jwkMap = setParser.jwkSetToMap(jwkSet);

        final Key key = jwkMap.get(jwt.getSignedJwt().getHeader().getKeyId());

        return key != null && jwt.isValid(getSigningHandlerForKey(key));
    }

    /**
     * Get an appropriate signing handler for the given key.
     *
     * @param key the signature verification key.
     * @return the appropriate signing handler for the key.
     */
    private SigningHandler getSigningHandlerForKey(final Key key) {
        if (key instanceof RSAPublicKey) {
            return signingManager.newRsaSigningHandler(key);
        } else if (key instanceof ECPublicKey) {
            return signingManager.newEcdsaVerificationHandler((ECPublicKey) key);
        } else if (key instanceof SecretKey) {
            return signingManager.newHmacSigningHandler(key.getEncoded());
        } else {
            throw new IllegalArgumentException("Unsupported verification key type");
        }
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
        } catch (IdRepoException | SSOException e) {
            throw Utils.createException(OAuth2Constants.OAuth2Client.PUBLIC_KEY_SELECTOR, e, logger);
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

        } catch (SSOException | IdRepoException | URISyntaxException e) {
            throw Utils.createException(OAuth2Constants.OAuth2Client.SECTOR_IDENTIFIER_URI, e, logger);
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

    private String getAttribute(String attributeName) {
        final Set<String> set;
        try {
            set = amIdentity.getAttribute(attributeName);
        } catch (Exception e) {
            logger.error("Unable to get {} from repository", attributeName, e);
            throw OAuthProblemException.OAuthError.SERVER_ERROR.handle(Request.getCurrent(),
                    "Unable to get " + attributeName + " from repository");
        }
        if (set.iterator().hasNext()){
            return set.iterator().next();
        }
        return null;
    }
}