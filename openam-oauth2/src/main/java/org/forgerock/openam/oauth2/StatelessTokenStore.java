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
 * Copyright 2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.oauth2;

import static com.sun.identity.shared.DateUtils.stringToDate;
import static org.forgerock.json.JsonValue.json;
import static org.forgerock.openam.oauth2.OAuth2Constants.Bearer.BEARER;
import static org.forgerock.openam.oauth2.OAuth2Constants.CoreTokenParams.*;
import static org.forgerock.openam.oauth2.OAuth2Constants.Custom.CLAIMS;
import static org.forgerock.openam.oauth2.OAuth2Constants.Custom.NONCE;
import static org.forgerock.openam.oauth2.OAuth2Constants.JWTTokenParams.ACR;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.EXPIRES_IN;
import static org.forgerock.openam.oauth2.OAuth2Constants.Params.GRANT_TYPE;
import static org.forgerock.openam.oauth2.OAuth2Constants.Token.*;
import static org.forgerock.openam.utils.Time.currentTimeMillis;
import static org.forgerock.openam.utils.Time.newDate;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.iplanet.ums.IDynamicMembership;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.builders.JwtBuilderFactory;
import org.forgerock.json.jose.builders.JwtClaimsSetBuilder;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.InvalidJwtException;
import org.forgerock.json.jose.jwe.CompressionAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithmType;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AuthorizationCode;
import org.forgerock.oauth2.core.DeviceCode;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.RefreshToken;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.TokenStore;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.blacklist.Blacklist;
import org.forgerock.openam.blacklist.BlacklistException;
import org.forgerock.openam.blacklist.Blacklistable;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.TokenAdapter;
import org.forgerock.openam.cts.api.filter.TokenFilterBuilder;
import org.forgerock.openam.cts.api.tokens.Token;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.oauth2.OAuth2Constants.ProofOfPossession;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.RealmNormaliser;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.forgerock.util.encode.Base64;
import org.forgerock.util.query.QueryFilter;
import org.joda.time.Duration;

/**
 * Stateless implementation of the OAuth2 Token Store.
 */
public class StatelessTokenStore implements TokenStore {

    private final Debug logger;
    private final TokenStore statefulTokenStore;
    private final JwtBuilderFactory jwtBuilder;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final OpenIdConnectClientRegistrationStore clientRegistrationStore;
    private final RealmNormaliser realmNormaliser;
    private final OAuth2UrisFactory oAuth2UrisFactory;
    private final Blacklist<Blacklistable> tokenBlacklist;
    private final CTSPersistentStore cts;
    private final TokenAdapter<StatelessTokenMetadata> tokenAdapter;
    private final OAuth2Utils utils;


    /**
     * Constructs a new StatelessTokenStore.
     *
     * @param statefulTokenStore An instance of the stateful TokenStore.
     * @param jwtBuilder An instance of the JwtBuilderFactory.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param logger An instance of OAuth2AuditLogger.
     * @param clientRegistrationStore An instance of the OpenIdConnectClientRegistrationStore.
     * @param realmNormaliser An instance of the RealmNormaliser.
     * @param oAuth2UrisFactory An instance of the OAuth2UrisFactory.
     * @param tokenBlacklist An instance of the token blacklist.
     * @param cts An instance of the CTSPersistentStoreImpl
     * @param tokenAdapter An instance of the StatelessTokenCtsAdapter
     * @param utils OAuth2 utilities
     */
    @Inject
    public StatelessTokenStore(StatefulTokenStore statefulTokenStore, JwtBuilderFactory jwtBuilder,
            OAuth2ProviderSettingsFactory providerSettingsFactory, @Named(OAuth2Constants.DEBUG_LOG_NAME) Debug logger,
            OpenIdConnectClientRegistrationStore clientRegistrationStore, RealmNormaliser realmNormaliser,
            OAuth2UrisFactory oAuth2UrisFactory, Blacklist<Blacklistable> tokenBlacklist,
            CTSPersistentStore cts, TokenAdapter<StatelessTokenMetadata> tokenAdapter, OAuth2Utils utils) {
        this.statefulTokenStore = statefulTokenStore;
        this.jwtBuilder = jwtBuilder;
        this.providerSettingsFactory = providerSettingsFactory;
        this.logger = logger;
        this.clientRegistrationStore = clientRegistrationStore;
        this.realmNormaliser = realmNormaliser;
        this.oAuth2UrisFactory = oAuth2UrisFactory;
        this.tokenBlacklist = tokenBlacklist;
        this.cts = cts;
        this.tokenAdapter = tokenAdapter;
        this.utils = utils;
    }

    @Override
    public AuthorizationCode createAuthorizationCode(Set<String> scope, ResourceOwner resourceOwner, String clientId,
            String redirectUri, String nonce, OAuth2Request request, String codeChallenge, String codeChallengeMethod)
            throws ServerException, NotFoundException {
        return statefulTokenStore.createAuthorizationCode(scope, resourceOwner, clientId, redirectUri, nonce, request,
                codeChallenge, codeChallengeMethod);
    }

    @Override
    public AccessToken createAccessToken(String grantType, String accessTokenType, String authorizationCode, String
            resourceOwnerId, String clientId, String redirectUri, Set<String> scope, RefreshToken refreshToken,
            String nonce, String claims, OAuth2Request request) throws ServerException, NotFoundException {
        return createAccessToken(grantType, accessTokenType, authorizationCode, resourceOwnerId, clientId,
                redirectUri, scope, refreshToken, nonce, claims, request,
                TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis()));
    }

    @Override
    public AccessToken createAccessToken(String grantType, String accessTokenType, String authorizationCode, String
            resourceOwnerId, String clientId, String redirectUri, Set<String> scope, RefreshToken refreshToken,
            String nonce, String claims, OAuth2Request request, long authTime) 
                    throws ServerException, NotFoundException {
        OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        OpenIdConnectClientRegistration clientRegistration = getClientRegistration(clientId, request);
        Duration currentTime = Duration.millis(currentTimeMillis());
        Duration expiresIn;
        Duration expiryTime;
        if (clientRegistration == null) {
            expiresIn = Duration.standardSeconds(providerSettings.getAccessTokenLifetime());
        } else {
            expiresIn = Duration.millis(clientRegistration.getAccessTokenLifeTime(providerSettings));
        }
        expiryTime = expiresIn.plus(currentTime);
        String realm;
        try {
            realm = realmNormaliser.normalise(request.<String>getParameter(REALM));
        } catch (org.forgerock.json.resource.NotFoundException e) {
            throw new NotFoundException(e.getMessage());
        }
        
        Map<String, Set<String>> realmAccess = new HashMap<String, Set<String>>();
        //realmAccess.put("roles", new HashSet<>(Arrays.asList( new String[] {"admin", "user"} )));
        
        AuthorizationCode authCode = request.getToken(AuthorizationCode.class);
        if (authCode != null) {
            String sessionId = authCode.getSessionId();
            if (StringUtils.isNotBlank(sessionId)) {
                try {
                    final SSOTokenManager ssoTokenManager = SSOTokenManager.getInstance();
                    final SSOToken token = ssoTokenManager.createSSOToken(sessionId);
                    AMIdentity identity = IdUtils.getIdentity(token);
                    Set<AMIdentity> memberships = identity.getMemberships(IdType.GROUP);
                    Set<String> roles = memberships.stream().map(m -> m.getName()).collect(Collectors.toSet());;
                    realmAccess.put("roles", roles);
                } catch (SSOException | IdRepoException e) {
                    logger.error("Error retrieving session from AuthorizationCode", e);
                }
            }
        }
        
        String jwtId = UUID.randomUUID().toString();
        JwtClaimsSetBuilder claimsSetBuilder = jwtBuilder.claims()
                .jti(jwtId)
                .exp(newDate(expiryTime.getMillis()))
                .aud(Collections.singletonList(clientId))
                .sub(resourceOwnerId)
                .iat(newDate(currentTime.getMillis()))
                .nbf(newDate(currentTime.getMillis()))
                .iss(oAuth2UrisFactory.get(request).getIssuer())
                .claim(SCOPE,  org.apache.commons.lang.StringUtils.join(scope, " "))
                .claim("realm_access", realmAccess)
                .claim(CLAIMS, claims)
                .claim(REALM, realm)
                .claim(NONCE, nonce)
                .claim(TOKEN_NAME, OAUTH_ACCESS_TOKEN)
                .claim(OAUTH_TOKEN_TYPE, BEARER)
                .claim("typ", BEARER)
                .claim(EXPIRES_IN, expiresIn.getMillis())
                .claim(AUDIT_TRACKING_ID, UUID.randomUUID().toString())
                .claim(AUTH_GRANT_ID, refreshToken != null ? refreshToken.getAuthGrantId() : UUID.randomUUID().toString())
                .claim(AUTH_TIME, authTime);
        
        

        JsonValue confirmationJwk = utils.getConfirmationKey(request);
        if (confirmationJwk != null) {
            claimsSetBuilder.claim(ProofOfPossession.CNF, confirmationJwk.asMap());
        }

        JwsAlgorithm signingAlgorithm = getSigningAlgorithm(request);
        CompressionAlgorithm compressionAlgorithm = getCompressionAlgorithm(request);
        
        final String encryptionKeyId = generateKid(providerSettings.getJWKSet(), signingAlgorithm.toString());
        
        SignedJwt jwt = jwtBuilder.jws(getTokenSigningHandler(request, signingAlgorithm))
                .claims(claimsSetBuilder.build())
                .headers()
                .alg(signingAlgorithm)
                .zip(compressionAlgorithm)
                .headerIfNotNull("kid", encryptionKeyId)
                .done()
                .asJwt();
        StatelessAccessToken accessToken = new StatelessAccessToken(jwt, jwt.build());
        request.setToken(AccessToken.class, accessToken);
        createStatelessTokenMetadata(jwtId, expiryTime.getMillis(), accessToken);
        return accessToken;
    }
    

    private String generateKid(JsonValue jwkSet, String algorithm) {

        final JwsAlgorithm jwsAlgorithm = JwsAlgorithm.valueOf(algorithm);
        if (JwsAlgorithmType.RSA.equals(jwsAlgorithm.getAlgorithmType()) ||
                JwsAlgorithmType.ECDSA.equals(jwsAlgorithm.getAlgorithmType())) {
            JsonValue jwks = jwkSet.get(OAuth2Constants.JWTTokenParams.KEYS);
            if (!jwks.isNull() && !jwks.asList().isEmpty()) {
                return jwks.get(0).get(OAuth2Constants.JWTTokenParams.KEY_ID).asString();
            }
        }

        return null;
    }

    private CompressionAlgorithm getCompressionAlgorithm(final OAuth2Request request)
            throws NotFoundException, ServerException {
        return providerSettingsFactory.get(request).isTokenCompressionEnabled() ? CompressionAlgorithm.DEF :
               CompressionAlgorithm.NONE;
    }

    private void createStatelessTokenMetadata(String id, long expiryTime, StatelessToken token) throws ServerException {

        try {
            String resourceOwnerId = token.getResourceOwnerId();
            String grantId = token.getAuthGrantId();
            String clientId = token.getClientId();
            Set<String> scope = token.getScope();
            String realm = token.getRealm();
            String name = token.getTokenName();
            String grantType = token.getTokenType();
            StatelessTokenMetadata meta = new StatelessTokenMetadata(id, resourceOwnerId, expiryTime, grantId,
                    clientId, scope, realm, name, grantType);
            cts.create(tokenAdapter.toToken(meta));
        } catch (CoreTokenException e) {
            logger.error("Failed to add stateless token metadata to CTS", e);
            throw new ServerException(e);
        }
    }

    private OpenIdConnectClientRegistration getClientRegistration(String clientId, OAuth2Request request)
            throws ServerException, NotFoundException {
        OpenIdConnectClientRegistration clientRegistration = null;
        try {
            clientRegistration = clientRegistrationStore.get(clientId, request);
        } catch (InvalidClientException e) {
            // If the client is not registered, then returns null.
        }
        return clientRegistration;
    }

    private SigningHandler getTokenSigningHandler(OAuth2Request request, JwsAlgorithm signingAlgorithm)
            throws NotFoundException, ServerException {
        try {
            OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
            switch (signingAlgorithm.getAlgorithmType()) {
                case HMAC: {
                    return new SigningManager().newHmacSigningHandler(
                            Base64.decode(providerSettings.getTokenHmacSharedSecret()));
                }
                case RSA: {
                    return new SigningManager().newRsaSigningHandler(
                            providerSettings.getSigningKeyPair(signingAlgorithm).getPrivate());
                }
                case ECDSA: {
                    return new SigningManager().newEcdsaSigningHandler(
                            (ECPrivateKey) providerSettings.getSigningKeyPair(signingAlgorithm).getPrivate());
                }
                default: {
                    throw new ServerException("Unsupported Token signing algorithm");
                }
            }
        } catch (IllegalArgumentException e) {
            throw new ServerException("Invalid Token signing algorithm");
        }
    }

    private SigningHandler getTokenVerificationHandler(OAuth2ProviderSettings providerSettings, JwsAlgorithm signingAlgorithm)
            throws NotFoundException, ServerException {
        try {
            switch (signingAlgorithm.getAlgorithmType()) {
                case HMAC: {
                    return new SigningManager().newHmacSigningHandler(
                            Base64.decode(providerSettings.getTokenHmacSharedSecret()));
                }
                case RSA: {
                    return new SigningManager().newRsaSigningHandler(
                            providerSettings.getSigningKeyPair(signingAlgorithm).getPublic());
                }
                case ECDSA: {
                    return new SigningManager().newEcdsaVerificationHandler(
                            (ECPublicKey) providerSettings.getSigningKeyPair(signingAlgorithm).getPublic());
                }
                default: {
                    throw new ServerException("Unsupported Token signing algorithm");
                }
            }
        } catch (IllegalArgumentException e) {
            throw new ServerException("Invalid Token signing algorithm");
        }
    }

    private JwsAlgorithm getSigningAlgorithm(OAuth2Request request) throws ServerException, NotFoundException {
        try {
            OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
            JwsAlgorithm algorithm = JwsAlgorithm.valueOf(providerSettings.getTokenSigningAlgorithm().toUpperCase());
            if (!isAlgorithmSupported(request, algorithm)) {
                throw new ServerException("Unsupported Token signing algorithm");
            }
            return algorithm;
        } catch (IllegalArgumentException e) {
            throw new ServerException("Invalid Token signing algorithm");
        }
    }

    private JwsAlgorithm getSigningAlgorithm(OAuth2ProviderSettings providerSettings) throws ServerException, NotFoundException {
        try {
            JwsAlgorithm algorithm = JwsAlgorithm.valueOf(providerSettings.getTokenSigningAlgorithm().toUpperCase());
            if (!isAlgorithmSupported(providerSettings, algorithm)) {
                throw new ServerException("Unsupported Token signing algorithm");
            }
            return algorithm;
        } catch (IllegalArgumentException e) {
            throw new ServerException("Invalid Token signing algorithm");
        }
    }

    private boolean isAlgorithmSupported(OAuth2Request request, JwsAlgorithm algorithm) throws ServerException,
            NotFoundException {
        OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        for (String supportedSigningAlgorithm : providerSettings.getSupportedIDTokenSigningAlgorithms()) {
            if (supportedSigningAlgorithm.toUpperCase().equals(algorithm.toString())) {
                return true;
            }
        }
        return false;
    }

    private boolean isAlgorithmSupported(OAuth2ProviderSettings providerSettings, JwsAlgorithm algorithm) throws ServerException,
            NotFoundException {
        for (String supportedSigningAlgorithm : providerSettings.getSupportedIDTokenSigningAlgorithms()) {
            if (supportedSigningAlgorithm.toUpperCase().equals(algorithm.toString())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId,
            String redirectUri, Set<String> scope, OAuth2Request request) throws ServerException, NotFoundException {
        return createRefreshToken(grantType, clientId, resourceOwnerId, redirectUri, scope, request, "");
    }

    @Override
    public RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId,
            String redirectUri, Set<String> scope, OAuth2Request request, String validatedClaims)
            throws ServerException, NotFoundException {
        return createRefreshToken(grantType, clientId, resourceOwnerId, redirectUri, scope, request,
                validatedClaims, TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis()));
    }

    @Override
    public RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId,
            String redirectUri, Set<String> scope, OAuth2Request request, String validatedClaims, long authTime)
            throws ServerException, NotFoundException {
        AuthorizationCode authorizationCode = request.getToken(AuthorizationCode.class);
        String authGrantId;
        if (authorizationCode != null &&  authorizationCode.getAuthGrantId() != null  ) {
            authGrantId = authorizationCode.getAuthGrantId();
        } else {
            authGrantId = UUID.randomUUID().toString();
        }
        return createRefreshToken(grantType, clientId, resourceOwnerId, redirectUri, scope, request,
                validatedClaims, authGrantId, authTime);
    }

    @Override
    public RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId,
            String redirectUri, Set<String> scope, OAuth2Request request, String validatedClaims, String authGrantId)
            throws ServerException, NotFoundException {
        return createRefreshToken(grantType, clientId, resourceOwnerId,
                redirectUri, scope, request, validatedClaims, authGrantId,
                TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis()));
    }

    @Override
    public RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId,
            String redirectUri, Set<String> scope, OAuth2Request request, String validatedClaims,
            String authGrantId, long authTime)
            throws ServerException, NotFoundException {
        String realm = null;
        try {
            realm = realmNormaliser.normalise(request.<String>getParameter(REALM));
        } catch (org.forgerock.json.resource.NotFoundException e) {
            throw new NotFoundException(e.getMessage());
        }
        OpenIdConnectClientRegistration clientRegistration = getClientRegistration(clientId, request);
        OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        Duration currentTime = Duration.millis(currentTimeMillis());
        Duration lifeTime;
        if (clientRegistration == null) {
            lifeTime = Duration.standardSeconds(providerSettings.getRefreshTokenLifetime());
        } else {
            lifeTime = Duration.millis(clientRegistration.getRefreshTokenLifeTime(providerSettings));
        }
        long expiryTime = lifeTime.isShorterThan(Duration.ZERO) ? -1 : lifeTime.plus(currentTime).getMillis();
        String jwtId = UUID.randomUUID().toString();
        JwtClaimsSetBuilder claimsSetBuilder = jwtBuilder.claims()
                .jti(jwtId)
                .exp(newDate(expiryTime))
                .aud(Collections.singletonList(clientId))
                .sub(resourceOwnerId)
                .iat(newDate(currentTime.getMillis()))
                .nbf(newDate(currentTime.getMillis()))
                .iss(oAuth2UrisFactory.get(request).getIssuer())
                .claim(SCOPE, scope)
                .claim(REALM, realm)
                .claim(OAUTH_TOKEN_TYPE, BEARER)
                .claim(EXPIRES_IN, lifeTime.getMillis())
                .claim(TOKEN_NAME, OAUTH_REFRESH_TOKEN)
                .claim(AUDIT_TRACKING_ID, UUID.randomUUID().toString())
                .claim(AUTH_GRANT_ID, authGrantId)
                .claim(AUTH_TIME, authTime);
        for(org.forgerock.oauth2.core.Token token : request.getTokens()) {
        	if(token instanceof AuthorizationCode) {
        		claimsSetBuilder.claim(NONCE, ((AuthorizationCode)token).getNonce());
        	}
        }
        String authModules = null;
        String acr = null;
        AuthorizationCode authorizationCode = request.getToken(AuthorizationCode.class);
        if (authorizationCode != null) {
            authModules = authorizationCode.getAuthModules();
            acr = authorizationCode.getAuthenticationContextClassReference();
        }

        RefreshToken currentRefreshToken = request.getToken(RefreshToken.class);
        if (currentRefreshToken != null) {
            authModules = currentRefreshToken.getAuthModules();
            acr = currentRefreshToken.getAuthenticationContextClassReference();
        }

        if (authModules != null) {
            claimsSetBuilder.claim(AUTH_MODULES, authModules);
        }
        if (acr != null) {
            claimsSetBuilder.claim(ACR, acr);
        }
        if (!StringUtils.isBlank(validatedClaims)) {
            claimsSetBuilder.claim(CLAIMS, validatedClaims);
        }
        JwsAlgorithm signingAlgorithm = getSigningAlgorithm(request);
        CompressionAlgorithm compressionAlgorithm = getCompressionAlgorithm(request);
        SignedJwt jwt = jwtBuilder.jws(getTokenSigningHandler(request, signingAlgorithm))
                .claims(claimsSetBuilder.build())
                .headers()
                .alg(signingAlgorithm)
                .zip(compressionAlgorithm)
                .done()
                .asJwt();

        StatelessRefreshToken refreshToken = new StatelessRefreshToken(jwt, jwt.build());
        request.setToken(RefreshToken.class, refreshToken);
        createStatelessTokenMetadata(jwtId, expiryTime, refreshToken);
        return refreshToken;
    }

    @Override
    public AuthorizationCode readAuthorizationCode(OAuth2Request request, String code) throws InvalidGrantException,
            ServerException, NotFoundException {
        return statefulTokenStore.readAuthorizationCode(request, code);
    }

    @Override
    public void updateAuthorizationCode(OAuth2Request request, AuthorizationCode authorizationCode)
            throws NotFoundException, ServerException {
        statefulTokenStore.updateAuthorizationCode(request, authorizationCode);
    }

    @Override
    public void updateAccessToken(OAuth2Request request, AccessToken accessToken) {
    }

    @Override
    public void deleteAuthorizationCode(OAuth2Request request, String authorizationCode) throws NotFoundException,
            ServerException {
        statefulTokenStore.deleteAuthorizationCode(request, authorizationCode);
    }

    private boolean isBlacklisted(String jwtId) throws BlacklistException {
        return tokenBlacklist.isBlacklisted(new BlacklistItem(jwtId));
    }

    private JsonValue query(QueryFilter<CoreTokenField> query) throws ServerException {
        Collection<Token> tokens = null;
        try {
            tokens = cts.query(new TokenFilterBuilder().withQuery(query).build());
        } catch (CoreTokenException e) {
            throw new ServerException("Token not found in CTS");
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (Token token : tokens) {
            results.add(tokenAdapter.fromToken(token).asMap());
        }
        return new JsonValue(results);
    }

    private void blacklist(String tokenId, long expiryTime) throws BlacklistException {
        BlacklistItem item = new BlacklistItem(tokenId, expiryTime);
        tokenBlacklist.blacklist(item);
    }

    @Override
    public void deleteAccessToken(OAuth2Request request, String jwtString) throws ServerException {
        try {
            SignedJwt jwt = new JwtReconstruction().reconstructJwt(jwtString, SignedJwt.class);
            String tokenId = jwt.getClaimsSet().getJwtId();
            if (!isBlacklisted(tokenId)) {
                verifySignature(jwt, request);
                verifyTokenType(OAUTH_ACCESS_TOKEN, jwt);
                validateTokenRealm(jwt.getClaimsSet().getClaim("realm", String.class), request);
                blacklist(tokenId, jwt.getClaimsSet().getExpirationTime().getTime());
                cts.delete(tokenId);
            } else {
                logger.warning("Token " + tokenId + " has been blacklisted");
            }
        } catch (InvalidJwtException | NotFoundException | InvalidGrantException | CoreTokenException e) {
            throw new ServerException("Token id is not a JWT");
        } catch (BlacklistException e) {
            logger.error("Could not delete token", e);
            throw new ServerException("Could not delete token");
        }
    }

    @Override
    public void deleteRefreshToken(OAuth2Request request, String jwtString) throws InvalidRequestException,
            ServerException {
        try {
            SignedJwt jwt = new JwtReconstruction().reconstructJwt(jwtString, SignedJwt.class);
            String tokenId = jwt.getClaimsSet().getJwtId();
            if (!isBlacklisted(tokenId)) {
                verifySignature(jwt, request);
                verifyTokenType(OAUTH_REFRESH_TOKEN, jwt);
                validateTokenRealm(jwt.getClaimsSet().getClaim("realm", String.class), request);
                blacklist(tokenId, jwt.getClaimsSet().getExpirationTime().getTime());
                cts.delete(tokenId);
            } else {
                logger.warning("Token " + tokenId + " has been blacklisted");
            }
        } catch (InvalidJwtException | NotFoundException | InvalidGrantException | CoreTokenException e) {
            throw new InvalidRequestException("Token id is not a JWT");
        } catch (BlacklistException e) {
            logger.error("Could not delete token", e);
            throw new InvalidRequestException("Could not delete token");
        }
    }

    @Override
    public AccessToken readAccessToken(OAuth2Request request, String jwtString) throws ServerException,
            InvalidGrantException, NotFoundException {
        try {
            SignedJwt jwt = new JwtReconstruction().reconstructJwt(jwtString, SignedJwt.class);
            String tokenId = jwt.getClaimsSet().getJwtId();
            if (!isBlacklisted(tokenId)) {
                verifySignature(jwt, request);
                verifyTokenType(OAUTH_ACCESS_TOKEN, jwt);
                validateTokenRealm(jwt.getClaimsSet().getClaim("realm", String.class), request);
                StatelessAccessToken accessToken = new StatelessAccessToken(jwt, jwtString);
                request.setToken(AccessToken.class, accessToken);
                return accessToken;
            } else {
                throw new InvalidGrantException("Token has been blacklisted");
            }
        } catch (InvalidJwtException e) {
            throw new InvalidGrantException("Token id is not a JWT");
        } catch (BlacklistException e) {
            logger.error("Could not read token", e);
            throw new InvalidGrantException("Could not read token");
        }
    }

    @Override
    public RefreshToken readRefreshToken(OAuth2Request request, String jwtString) throws ServerException,
            InvalidGrantException, NotFoundException {
        try {
            SignedJwt jwt = new JwtReconstruction().reconstructJwt(jwtString, SignedJwt.class);
            String tokenId = jwt.getClaimsSet().getJwtId();
            if (!isBlacklisted(tokenId)) {
                verifySignature(jwt, request);
                verifyTokenType(OAUTH_REFRESH_TOKEN, jwt);
                validateTokenRealm(jwt.getClaimsSet().getClaim("realm", String.class), request);
                StatelessRefreshToken refreshToken = new StatelessRefreshToken(jwt, jwtString);
                request.setToken(RefreshToken.class, refreshToken);
                return refreshToken;
            } else {
                throw new InvalidGrantException("Token has been blacklisted");
            }
        } catch (InvalidJwtException e) {
            throw new InvalidGrantException("Token id is not a JWT");
        } catch (BlacklistException e) {
            logger.error("Could not read token", e);
            throw new InvalidGrantException("Could not read token");
        }
    }

    @Override
    public DeviceCode createDeviceCode(Set<String> scope, ResourceOwner resourceOwner, String clientId, String nonce,
            String responseType, String state, String acrValues, String prompt, String uiLocales, String loginHint,
            Integer maxAge, String claims, OAuth2Request request, String codeChallenge, String codeChallengeMethod)
            throws ServerException, NotFoundException {
        return statefulTokenStore.createDeviceCode(scope, resourceOwner, clientId, nonce, responseType, state,
                acrValues, prompt, uiLocales, loginHint, maxAge, claims, request, codeChallenge, codeChallengeMethod);
    }

    @Override
    public DeviceCode readDeviceCode(String clientId, String code, OAuth2Request request) throws ServerException,
            NotFoundException, InvalidGrantException {
        return statefulTokenStore.readDeviceCode(clientId, code, request);
    }

    @Override
    public DeviceCode readDeviceCode(String userCode, OAuth2Request request) throws ServerException, NotFoundException,
            InvalidGrantException {
        return statefulTokenStore.readDeviceCode(userCode, request);
    }

    @Override
    public void updateDeviceCode(DeviceCode code, OAuth2Request request) throws ServerException, NotFoundException,
            InvalidGrantException {
        statefulTokenStore.updateDeviceCode(code, request);
    }

    @Override
    public void deleteDeviceCode(String clientId, String code, OAuth2Request request) throws ServerException,
            NotFoundException, InvalidGrantException {
        statefulTokenStore.deleteDeviceCode(clientId, code, request);
    }

    @Override
    public JsonValue queryForToken(String realm, QueryFilter<CoreTokenField> queryFilter) throws ServerException, NotFoundException {
        return query(queryFilter);
    }

    @Override
    public void delete(String realm, String tokenId) throws ServerException, NotFoundException {
        if (isJwt(tokenId)) {
            SignedJwt jwt = new JwtReconstruction().reconstructJwt(tokenId, SignedJwt.class);
            tokenId = jwt.getClaimsSet().getJwtId();
        }
        try {
            if (!isBlacklisted(tokenId)) {
                StatelessTokenMetadata metadata = tokenAdapter.fromToken(cts.read(tokenId));
                blacklist(tokenId, metadata.getExpiryTime());
                cts.delete(tokenId);
            } else {
                logger.warning("Token " + tokenId + " has been blacklisted");
            }
        } catch (BlacklistException e) {
            logger.error("Could not delete token", e);
            throw new ServerException("Could not delete token");
        } catch (CoreTokenException e) {
            throw new ServerException("Token id not found in CTS");
        }
    }

    @Override
    public JsonValue read(String tokenId) throws ServerException {
        try {
            SignedJwt jwt = new JwtReconstruction().reconstructJwt(tokenId, SignedJwt.class);
            if (!isBlacklisted(jwt.getClaimsSet().getJwtId())) {
                String tokenName = jwt.getClaimsSet().getClaim(TOKEN_NAME, String.class);
                StatelessToken token;
                if (OAUTH_ACCESS_TOKEN.equals(tokenName)) {
                    token = new StatelessAccessToken(jwt, tokenId);
                } else if (OAUTH_REFRESH_TOKEN.equals(tokenName)) {
                    token = new StatelessRefreshToken(jwt, tokenId);
                } else {
                    throw new ServerException("Unrecognised token type");
                }
                return convertToken(token);
            } else {
                logger.warning("Token " + tokenId + " has been blacklisted");
                return null;
            }
        } catch (InvalidJwtException e) {
            throw new ServerException("Token id is not a JWT");
        } catch (BlacklistException e) {
            logger.error("Could not read token", e);
            throw new ServerException("Could not read token");
        }
    }

    private Boolean isJwt(String tokenId) {
        try {
            new JwtReconstruction().reconstructJwt(tokenId, SignedJwt.class);
            return true;
        } catch (InvalidJwtException e) {
            return false;
        }
    }

    private void verifySignature(SignedJwt jwt, OAuth2Request request)
            throws NotFoundException, InvalidGrantException, ServerException {
        verifySignature(providerSettingsFactory.get(request), jwt);
    }

    private void verifySignature(OAuth2ProviderSettings providerSettings, SignedJwt jwt) throws InvalidGrantException, ServerException,
            NotFoundException {
        JwsAlgorithm signingAlgorithm = getSigningAlgorithm(providerSettings);
        if(!jwt.verify(getTokenVerificationHandler(providerSettings, signingAlgorithm))) {
            throw new InvalidGrantException();
        }
    }

    private void verifyTokenType(String requiredTokenType, SignedJwt jwt) throws InvalidGrantException {
        if (!requiredTokenType.equals(jwt.getClaimsSet().getClaim(TOKEN_NAME))) {
            throw new InvalidGrantException("Token is not an " + requiredTokenType + " token: "
                    + jwt.getClaimsSet().getJwtId());
        }
    }

    protected void validateTokenRealm(String tokenRealm, OAuth2Request request) throws InvalidGrantException,
            NotFoundException {
        try {
            final String normalisedRequestRealm = realmNormaliser.normalise(request.<String>getParameter(REALM));
            if (!tokenRealm.equals(normalisedRequestRealm) && !realmNormaliser.normalise(tokenRealm).equals(
                    normalisedRequestRealm)) {
                throw new InvalidGrantException("Grant is not valid for the requested realm");
            }
        } catch (org.forgerock.json.resource.NotFoundException e) {
            throw new NotFoundException(e.getMessage());
        }
    }

    private JsonValue convertToken(StatelessToken token) {
        Map<String, Object> map = new HashMap<>();
        map.put(USERNAME, token.getResourceOwnerId());
        map.put(CLIENT_ID, token.getClientId());
        map.put(GRANT_TYPE, token.getTokenType());
        map.put(REALM, token.getRealm());
        map.put(EXPIRE_TIME, token.getExpiryTime());
        map.put(ID, token.getJwtId());
        map.put(TOKEN_NAME, token.getTokenName());
        map.put(AUTH_GRANT_ID, token.getAuthGrantId());
        map.put(SCOPE, token.getScope());
        return json(map);
    }
}
