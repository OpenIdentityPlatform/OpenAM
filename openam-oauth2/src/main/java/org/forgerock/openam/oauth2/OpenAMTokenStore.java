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

import static org.forgerock.json.JsonValue.*;
import static org.forgerock.oauth2.core.OAuth2Constants.Params.REALM;
import static org.forgerock.openam.utils.Time.*;
import static org.forgerock.util.query.QueryFilter.equalTo;
import static org.forgerock.util.query.QueryFilter.or;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.security.KeyPair;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithmType;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.AuthorizationCode;
import org.forgerock.oauth2.core.DeviceCode;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2Uris;
import org.forgerock.oauth2.core.OAuth2UrisFactory;
import org.forgerock.oauth2.core.RefreshToken;
import org.forgerock.oauth2.core.ResourceOwner;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.InvalidGrantException;
import org.forgerock.oauth2.core.exceptions.InvalidRequestException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openam.core.RealmInfo;
import org.forgerock.openam.cts.api.fields.OAuthTokenField;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.openidconnect.OpenAMOpenIdConnectToken;
import org.forgerock.openam.tokens.CoreTokenField;
import org.forgerock.openam.utils.RealmNormaliser;
import org.forgerock.openam.utils.StringUtils;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.forgerock.openidconnect.OpenIdConnectToken;
import org.forgerock.openidconnect.OpenIdConnectTokenStore;
import org.forgerock.util.encode.Base64url;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.Request;
import org.restlet.data.Status;
import org.restlet.ext.servlet.ServletUtils;

/**
 * Implementation of the OpenId Connect Token Store which the OpenId Connect Provider will implement.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenAMTokenStore implements OpenIdConnectTokenStore {

    private final Debug logger;
    private final OAuth2AuditLogger auditLogger;
    private final OAuthTokenStore tokenStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final OAuth2UrisFactory<RealmInfo> oauth2UrisFactory;
    private final OpenIdConnectClientRegistrationStore clientRegistrationStore;
    private final RealmNormaliser realmNormaliser;
    private final SSOTokenManager ssoTokenManager;
    private final CookieExtractor cookieExtractor;
    private final SecureRandom secureRandom;
    private final ClientAuthenticationFailureFactory failureFactory;

    //removed 0, 1, U, u, 8, 9 and l due to similarities to O, I, V, v, B, g and I on some displays
    protected final static String ALPHABET = "234567ABCDEFGHIJKLMNOPQRSTVWXYZabcdefghijkmnopqrstvwxyz";

    private final static int CODE_LENGTH = 8;
    private final static int NUM_RETRIES = 10;

    /**
     * Constructs a new OpenAMTokenStore.
     * @param tokenStore An instance of the OAuthTokenStore.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     * @param clientRegistrationStore An instance of the OpenIdConnectClientRegistrationStore.
     * @param realmNormaliser An instance of the RealmNormaliser.
     * @param ssoTokenManager An instance of the SSOTokenManager
     * @param cookieExtractor An instance of the CookieExtractor
     * @param auditLogger An instance of OAuth2AuditLogger
     * @param failureFactory
     */
    @Inject
    public OpenAMTokenStore(OAuthTokenStore tokenStore, OAuth2ProviderSettingsFactory providerSettingsFactory,
            OAuth2UrisFactory<RealmInfo> oauth2UrisFactory,
            OpenIdConnectClientRegistrationStore clientRegistrationStore, RealmNormaliser realmNormaliser,
            SSOTokenManager ssoTokenManager, CookieExtractor cookieExtractor, OAuth2AuditLogger auditLogger,
            @Named(OAuth2Constants.DEBUG_LOG_NAME) Debug logger, SecureRandom secureRandom,
            ClientAuthenticationFailureFactory failureFactory) {
        this.tokenStore = tokenStore;
        this.providerSettingsFactory = providerSettingsFactory;
        this.oauth2UrisFactory = oauth2UrisFactory;
        this.clientRegistrationStore = clientRegistrationStore;
        this.realmNormaliser = realmNormaliser;
        this.ssoTokenManager = ssoTokenManager;
        this.cookieExtractor = cookieExtractor;
        this.auditLogger = auditLogger;
        this.logger = logger;
        this.secureRandom = secureRandom;
        this.failureFactory = failureFactory;
    }

    /**
     * {@inheritDoc}
     */
    public AuthorizationCode createAuthorizationCode(Set<String> scope, ResourceOwner resourceOwner, String clientId,
            String redirectUri, String nonce, OAuth2Request request, String codeChallenge, String codeChallengeMethod)
            throws ServerException, NotFoundException {

        logger.message("DefaultOAuthTokenStoreImpl::Creating Authorization code");

        OpenIdConnectClientRegistration clientRegistration = getClientRegistration(clientId, request);

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        final String code = UUID.randomUUID().toString();

        long expiryTime = 0;
        if (clientRegistration == null) {
            expiryTime = providerSettings.getAuthorizationCodeLifetime() + currentTimeMillis();
        } else {
            expiryTime = clientRegistration.getAuthorizationCodeLifeTime(providerSettings) + currentTimeMillis();
        }

        final String ssoTokenId = getSsoTokenId(request);

        final OpenAMAuthorizationCode authorizationCode = new OpenAMAuthorizationCode(code, resourceOwner.getId(), clientId,
                redirectUri, scope, getClaimsFromRequest(request), expiryTime, nonce, realmNormaliser.normalise(request.<String>getParameter(REALM)),
                getAuthModulesFromSSOToken(request), getAuthenticationContextClassReferenceFromRequest(request),
                ssoTokenId, codeChallenge, codeChallengeMethod);

        // Store in CTS
        try {
            tokenStore.create(authorizationCode);
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"CREATED_AUTHORIZATION_CODE", authorizationCode.toString()};
                auditLogger.logAccessMessage("CREATED_AUTHORIZATION_CODE", obs, null);
            }
        } catch (CoreTokenException e) {
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"FAILED_CREATE_AUTHORIZATION_CODE", authorizationCode.toString()};
                auditLogger.logErrorMessage("FAILED_CREATE_AUTHORIZATION_CODE", obs, null);
            }
            logger.error("Unable to create authorization code " + authorizationCode.getTokenInfo(), e);
            throw new ServerException("Could not create token in CTS");
        }

        request.setToken(AuthorizationCode.class, authorizationCode);

        return authorizationCode;
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

    private String getClaimsFromRequest(OAuth2Request request) {
        return request.getParameter(OAuth2Constants.Custom.CLAIMS);
    }

    private String getSsoTokenId(OAuth2Request request) {
        return cookieExtractor.extract(ServletUtils.getRequest(request.<Request>getRequest()),
                SystemProperties.get("com.iplanet.am.cookie.name"));
    }

    private String getAuthModulesFromSSOToken(OAuth2Request request) {
        String authModules = null;
        try {
            SSOToken token = ssoTokenManager.createSSOToken(ServletUtils.getRequest(request.<Request>getRequest()));
            if (token != null) {
                authModules = token.getProperty(ISAuthConstants.AUTH_TYPE);
            }
        } catch (SSOException e) {
            logger.warning("Could not get list of auth modules from authentication", e);
        }
        return authModules;
    }

    private String getAuthenticationContextClassReferenceFromRequest(OAuth2Request request) {
        return request.getParameter(OAuth2Constants.JWTTokenParams.ACR);
    }

    /**
     * {@inheritDoc}
     */
    public OpenIdConnectToken createOpenIDToken(ResourceOwner resourceOwner, String clientId,
            String authorizationParty, String nonce, String ops, OAuth2Request request)
            throws ServerException, InvalidClientException, NotFoundException {

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        OAuth2Uris oAuth2Uris = oauth2UrisFactory.get(request);

        final OpenIdConnectClientRegistration clientRegistration = clientRegistrationStore.get(clientId, request);
        final String algorithm = clientRegistration.getIDTokenSignedResponseAlgorithm();

        final long currentTimeInSeconds = TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis());
        final long exp = TimeUnit.MILLISECONDS.toSeconds(clientRegistration.getJwtTokenLifeTime(providerSettings)) +
                currentTimeInSeconds;

        final String realm = realmNormaliser.normalise(request.<String>getParameter(REALM));

        final String iss = oAuth2Uris.getIssuer();

        final List<String> amr = getAMRFromAuthModules(request, providerSettings);

        final byte[] clientSecret = clientRegistration.getClientSecret().getBytes(Utils.CHARSET);
        final KeyPair keyPair = providerSettings.getServerKeyPair();

        final String atHash = generateAtHash(algorithm, request, providerSettings);
        final String cHash = generateCHash(algorithm, request, providerSettings);

        final String acr = getAuthenticationContextClassReference(request);

        final String kid = generateKid(providerSettings.getJWKSet(), algorithm);

        final String opsId = UUID.randomUUID().toString();

        final long authTime = resourceOwner.getAuthTime();

        final String subId = clientRegistration.getSubValue(resourceOwner.getId(), providerSettings);

        try {
            tokenStore.create(json(object(
                    field(OAuth2Constants.CoreTokenParams.ID, set(opsId)),
                    field(OAuth2Constants.JWTTokenParams.LEGACY_OPS, set(ops)),
                    field(OAuth2Constants.CoreTokenParams.EXPIRE_TIME, set(Long.toString(TimeUnit.SECONDS.toMillis(exp)
                    ))))));
        } catch (CoreTokenException e) {
            logger.error("Unable to create id_token user session token", e);
            throw new ServerException("Could not create token in CTS");
        }

        final OpenAMOpenIdConnectToken oidcToken = new OpenAMOpenIdConnectToken(kid, clientSecret, keyPair, algorithm,
                iss, subId, clientId, authorizationParty, exp, currentTimeInSeconds,
                authTime, nonce, opsId, atHash, cHash, acr, amr, realm);
        request.setSession(ops);

        //See spec section 5.4. - add claims to id_token based on 'response_type' parameter
        String responseType = request.getParameter(OAuth2Constants.Params.RESPONSE_TYPE);
        if (providerSettings.isAlwaysAddClaimsToToken() ||
                (responseType != null && responseType.trim().equals(OAuth2Constants.JWTTokenParams.ID_TOKEN))) {
            appendIdTokenClaims(request, providerSettings, oidcToken);
        } else if (providerSettings.getClaimsParameterSupported()) {
            appendRequestedIdTokenClaims(request, providerSettings, oidcToken);
        }

        return oidcToken;
    }

    //return all claims from scopes + claims requested in the id_token
    private void appendIdTokenClaims(OAuth2Request request, OAuth2ProviderSettings providerSettings,
                                     OpenAMOpenIdConnectToken oidcToken)
            throws ServerException, NotFoundException, InvalidClientException {

        try {
            AccessToken accessToken = request.getToken(AccessToken.class);
            Map<String, Object> userInfo = providerSettings.getUserInfo(accessToken, request).getValues();

            for (Map.Entry<String, Object> claim : userInfo.entrySet()) {
                oidcToken.put(claim.getKey(), claim.getValue());
            }

        } catch (UnauthorizedClientException e) {
            throw failureFactory.getException(request, e.getMessage());
        }

    }

    //See spec section 5.5. - add claims to id_token based on 'claims' parameter in the access token
    private void appendRequestedIdTokenClaims(OAuth2Request request, OAuth2ProviderSettings providerSettings,
                                              OpenAMOpenIdConnectToken oidcToken)
            throws ServerException, NotFoundException, InvalidClientException {

        AccessToken accessToken = request.getToken(AccessToken.class);
        String claims;
        if (accessToken != null) {
            claims = (String) accessToken.toMap().get(OAuth2Constants.Custom.CLAIMS);
        } else {
            claims = request.getParameter(OAuth2Constants.Custom.CLAIMS);
        }

        if (claims != null) {
            try {
                JSONObject claimsObject = new JSONObject(claims);
                JSONObject idTokenClaimsRequest = claimsObject.getJSONObject(OAuth2Constants.JWTTokenParams.ID_TOKEN);
                Map<String, Object> userInfo = providerSettings.getUserInfo(accessToken, request).getValues();

                Iterator<String> it = idTokenClaimsRequest.keys();
                while (it.hasNext()) {
                    String keyName = it.next();

                    if (userInfo.containsKey(keyName)) {
                        oidcToken.put(keyName, userInfo.get(keyName));
                    }
                }
            } catch (UnauthorizedClientException e) {
                throw failureFactory.getException(request, e.getMessage());
            } catch (JSONException e) {
                //if claims object not found, fall through
            }

        }

    }

    private String generateKid(JsonValue jwkSet, String algorithm) {

        final JwsAlgorithm jwsAlgorithm = JwsAlgorithm.valueOf(algorithm);
        if (JwsAlgorithmType.RSA.equals(jwsAlgorithm.getAlgorithmType())) {
            JsonValue jwks = jwkSet.get(OAuth2Constants.JWTTokenParams.KEYS);
            if (!jwks.isNull() && !jwks.asList().isEmpty()) {
                return jwks.get(0).get(OAuth2Constants.JWTTokenParams.KEY_ID).asString();
            }
        }

        return null;
    }

    private List<String> getAMRFromAuthModules(OAuth2Request request, OAuth2ProviderSettings providerSettings) throws ServerException {
        List<String> amr = null;

        String authModules;
        if (request.getToken(AuthorizationCode.class) != null) {
            authModules = request.getToken(AuthorizationCode.class).getAuthModules();
        } else if (request.getToken(RefreshToken.class) != null) {
            authModules = request.getToken(RefreshToken.class).getAuthModules();
        } else {
            authModules = getAuthModulesFromSSOToken(request);
        }

        if (authModules != null) {
            Map<String, String> amrMappings = providerSettings.getAMRAuthModuleMappings();
            if (!amrMappings.isEmpty()) {
                amr = new ArrayList<String>();
                List<String> modulesUsed = Arrays.asList(authModules.split("\\|"));
                for (Map.Entry<String, String> amrToModuleMapping : amrMappings.entrySet()) {
                    if (modulesUsed.contains(amrToModuleMapping.getValue())) {
                        amr.add(amrToModuleMapping.getKey());
                    }
                }
            }
        }

        return amr;
    }

    private String getAuthenticationContextClassReference(OAuth2Request request) {
        if (request.getToken(AuthorizationCode.class) != null) {
            return request.getToken(AuthorizationCode.class).getAuthenticationContextClassReference();
        } else if (request.getToken(RefreshToken.class) != null) {
            return request.getToken(RefreshToken.class).getAuthenticationContextClassReference();
        } else {
            return getAuthenticationContextClassReferenceFromRequest(request);
        }
    }

    /**
     * For at_hash values, used when token and id_token exist in scope.
     */
    private String generateAtHash(String algorithm, OAuth2Request request,
                                  OAuth2ProviderSettings providerSettings) throws ServerException {

        final AccessToken accessToken = request.getToken(AccessToken.class);

        if (accessToken == null) {
            logger.message("at_hash generation requires an existing access_token.");
            return null;
        }

        final String accessTokenValue = ((String) accessToken.getTokenInfo().get(OAuth2Constants.Params.ACCESS_TOKEN));

        return generateHash(algorithm, accessTokenValue, providerSettings);

    }

    /**
     * For c_hash, used when code and id_token exist in scope.
     */
    private String generateCHash(String algorithm, OAuth2Request request,
                                 OAuth2ProviderSettings providerSettings) throws ServerException {

        final AuthorizationCode authorizationCode = request.getToken(AuthorizationCode.class);

        if (authorizationCode == null) {
            logger.message("c_hash generation requires an existing code.");
            return null;
        }

        final String codeValue = authorizationCode.getTokenId();

        return generateHash(algorithm, codeValue, providerSettings);
    }

    /**
     * Generates hash values, by hashing the valueToEncode using the requests's "alg"
     * parameter, then returning the base64url encoding of the
     * leftmost half of the returned bytes. Used for both at_hash and c_hash claims.
     */
    private String generateHash(String algorithm, String valueToEncode, OAuth2ProviderSettings providerSettings)
            throws ServerException {

        if (!providerSettings.getSupportedIDTokenSigningAlgorithms().contains(algorithm)) {
            logger.message("Unsupported signing algorithm requested for hash value.");
            return null;
        }

        final JwsAlgorithm alg = JwsAlgorithm.valueOf(algorithm);

        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(alg.getMdAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            logger.message("Unsupported signing algorithm chosen for hashing.");
            throw new ServerException("Algorithm not supported.");
        }

        final byte[] result = digest.digest(valueToEncode.getBytes(Utils.CHARSET));
        final byte[] toEncode = Arrays.copyOfRange(result, 0, result.length / 2);

        return Base64url.encode(toEncode);
    }

    /**
     * {@inheritDoc}
     */
    public AccessToken createAccessToken(String grantType, String accessTokenType, String authorizationCode,
            String resourceOwnerId, String clientId, String redirectUri, Set<String> scope,
            RefreshToken refreshToken, String nonce, String claims, OAuth2Request request)
            throws ServerException, NotFoundException {

        OpenIdConnectClientRegistration clientRegistration = getClientRegistration(clientId, request);
        
        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        final String id = UUID.randomUUID().toString();
        final String auditId = UUID.randomUUID().toString();

        String realm = realmNormaliser.normalise(request.<String>getParameter(REALM));

        long expiryTime = 0;
        if (clientRegistration == null) {
            expiryTime = providerSettings.getAccessTokenLifetime() + currentTimeMillis();
        } else {
            expiryTime = clientRegistration.getAccessTokenLifeTime(providerSettings) + currentTimeMillis();
        }
        
        final AccessToken accessToken;
        if (refreshToken == null) {
            accessToken = new OpenAMAccessToken(id, authorizationCode, resourceOwnerId, clientId, redirectUri,
                    scope, expiryTime, null, OAuth2Constants.Token.OAUTH_ACCESS_TOKEN, grantType, nonce,
                    realm, claims, auditId);
        } else {
            accessToken = new OpenAMAccessToken(id, authorizationCode, resourceOwnerId, clientId, redirectUri,
                    scope, expiryTime, refreshToken.getTokenId(), OAuth2Constants.Token.OAUTH_ACCESS_TOKEN, grantType,
                    nonce, realm, claims, auditId);
        }
        try {
            tokenStore.create(accessToken);
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"CREATED_TOKEN", accessToken.toString()};
                auditLogger.logAccessMessage("CREATED_TOKEN", obs, null);
            }
        } catch (CoreTokenException e) {
            logger.error("Could not create token in CTS: " + e.getMessage());
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"FAILED_CREATE_TOKEN", accessToken.toString()};
                auditLogger.logErrorMessage("FAILED_CREATE_TOKEN", obs, null);
            }
            throw new ServerException("Could not create token in CTS: " + e.getMessage());
        }
        request.setToken(AccessToken.class, accessToken);
        return accessToken;
    }

    @Override
    public RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId,
            String redirectUri, Set<String> scope, OAuth2Request request)
            throws ServerException, NotFoundException {
        return createRefreshToken(grantType, clientId, resourceOwnerId, redirectUri, scope, request, null);
    }

    @Override
    public RefreshToken createRefreshToken(String grantType, String clientId, String resourceOwnerId,
            String redirectUri, Set<String> scope, OAuth2Request request, String validatedClaims)
            throws ServerException, NotFoundException {
        final String realm = realmNormaliser.normalise(request.<String>getParameter(REALM));

        logger.message("Create refresh token");
        
        OpenIdConnectClientRegistration clientRegistration = getClientRegistration(clientId, request);
        
        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);

        final String id = UUID.randomUUID().toString();
        final String auditId = UUID.randomUUID().toString();

        final long lifeTime;
        if (clientRegistration == null) {
            lifeTime = providerSettings.getRefreshTokenLifetime();
        } else {
            lifeTime = clientRegistration.getRefreshTokenLifeTime(providerSettings);
        }

        long expiryTime = lifeTime < 0 ? -1 : lifeTime + currentTimeMillis();

        AuthorizationCode token = request.getToken(AuthorizationCode.class);
        String authModules = null;
        String acr = null;
        if (token != null) {
            authModules = token.getAuthModules();
            acr = token.getAuthenticationContextClassReference();
        }

        RefreshToken currentRefreshToken = request.getToken(RefreshToken.class);
        if (currentRefreshToken != null) {
            authModules = currentRefreshToken.getAuthModules();
            acr = currentRefreshToken.getAuthenticationContextClassReference();
        }

        OpenAMRefreshToken refreshToken = new OpenAMRefreshToken(id, resourceOwnerId, clientId, redirectUri, scope,
                expiryTime, OAuth2Constants.Bearer.BEARER, OAuth2Constants.Token.OAUTH_REFRESH_TOKEN, grantType,
                realm, authModules, acr, auditId);

        if (!StringUtils.isBlank(validatedClaims)) {
            refreshToken.setClaims(validatedClaims);
        }

        try {
            tokenStore.create(refreshToken);
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"CREATED_REFRESH_TOKEN", refreshToken.toString()};
                auditLogger.logAccessMessage("CREATED_REFRESH_TOKEN", obs, null);
            }
        } catch (CoreTokenException e) {
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"FAILED_CREATE_REFRESH_TOKEN", refreshToken.toString()};
                auditLogger.logErrorMessage("FAILED_CREATE_REFRESH_TOKEN", obs, null);
            }
            logger.error("Unable to create refresh token: " + refreshToken.getTokenInfo(), e);
            throw new ServerException("Could not create token in CTS: " + e.getMessage());
        }

        request.setToken(RefreshToken.class, refreshToken);

        return refreshToken;
    }

    /**
     * {@inheritDoc}
     */
    public AuthorizationCode readAuthorizationCode(OAuth2Request request, String code) 
            throws InvalidGrantException, ServerException, NotFoundException {
        AuthorizationCode loaded = request.getToken(AuthorizationCode.class);
        if (loaded != null) {
            return loaded;
        }

        logger.message("Reading Authorization code: {}", code);
        final JsonValue token;

        // Read from CTS
        try {
            token = tokenStore.read(code);
        } catch (CoreTokenException e) {
            logger.error("Unable to read authorization code corresponding to id: " + code, e);
            throw new ServerException("Could not read token from CTS: " + e.getMessage());
        }

        if (token == null) {
            logger.error("Unable to read authorization code corresponding to id: " + code);
            throw new InvalidGrantException("The provided access grant is invalid, expired, or revoked.");
        }

        OpenAMAuthorizationCode authorizationCode = new OpenAMAuthorizationCode(token);
        validateTokenRealm(authorizationCode.getRealm(), request);

        request.setToken(AuthorizationCode.class, authorizationCode);
        return authorizationCode;
    }

    /**
     * {@inheritDoc}
     */
    public void updateAuthorizationCode(AuthorizationCode authorizationCode) {
        try {
            tokenStore.update(authorizationCode);
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"UPDATED_AUTHORIZATION_CODE", authorizationCode.toString()};
                auditLogger.logAccessMessage("UPDATED_AUTHORIZATION_CODE", obs, null);
            }
        } catch (CoreTokenException e) {
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"FAILED_UPDATE_AUTHORIZATION_CODE", authorizationCode.toString()};
                auditLogger.logErrorMessage("FAILED_UPDATE_AUTHORIZATION_CODE", obs, null);
            }
            logger.error("DefaultOAuthTokenStoreImpl::Unable to update authorization code "
                    + authorizationCode.getTokenInfo(), e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not update token in CTS", null);
        }
    }

    public void updateAccessToken(AccessToken accessToken) {
        try {
            tokenStore.update(accessToken);
        } catch (CoreTokenException e) {
            logger.error("DefaultOAuthTokenStoreImpl::Unable to update access token "
                    + accessToken.getTokenId(), e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not update token in CTS", null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteAuthorizationCode(String authorizationCode) {
        if (logger.messageEnabled()){
            logger.message("DefaultOAuthTokenStoreImpl::Deleting Authorization code: " + authorizationCode);
        }
        JsonValue oAuthToken;

        // Read from CTS
        try {
            oAuthToken = tokenStore.read(authorizationCode);
        } catch (CoreTokenException e) {
            logger.error("DefaultOAuthTokenStoreImpl::Unable to read authorization code corresponding to id: "
                    + authorizationCode, e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not read token from CTS: " + e.getMessage(), null);
        }

        if (oAuthToken == null) {
            logger.error("DefaultOAuthTokenStoreImpl::Unable to read authorization code corresponding to id: "
                    + authorizationCode);
            throw new OAuthProblemException(Status.CLIENT_ERROR_NOT_FOUND.getCode(), "Not found",
                    "Could not find token using CTS", null);
        }

        // Delete the code
        try {
            tokenStore.delete(authorizationCode);
        } catch (CoreTokenException e) {
            logger.error("DefaultOAuthTokenStoreImpl::Unable to delete authorization code corresponding to id: "
                    + authorizationCode, e);
            throw new OAuthProblemException(Status.SERVER_ERROR_INTERNAL.getCode(),
                    "Internal error", "Could not delete token from CTS: " + e.getMessage(), null);
        }
    }

    /**
     * {@inheritDoc}
     */
    public JsonValue queryForToken(String tokenId) throws InvalidRequestException {

        JsonValue results;

        try {
            results = tokenStore.query(or(
                    equalTo(OAuthTokenField.PARENT.getField(), tokenId),
                    equalTo(OAuthTokenField.REFRESH_TOKEN.getField(), tokenId)));
        } catch (CoreTokenException e) {
            logger.error("Unable to query refresh token corresponding to id: " + tokenId, e);
            throw new InvalidRequestException();
        }

        return results;
    }

    /**
     * {@inheritDoc}
     */
    public void deleteAccessToken(String accessTokenId) throws ServerException {
        logger.message("Deleting access token");

        // Delete the code
        try {
            tokenStore.delete(accessTokenId);
        } catch (CoreTokenException e) {
            logger.error("Unable to delete access token corresponding to id: " + accessTokenId, e);
            throw new ServerException("Could not delete token from CTS: " + e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    public void deleteRefreshToken(String refreshTokenId) throws InvalidRequestException {

        // Delete the code
        try {
            tokenStore.delete(refreshTokenId);
        } catch (CoreTokenException e) {
            logger.error("Unable to delete refresh token corresponding to id: " + refreshTokenId, e);
            throw new InvalidRequestException();
        }
    }

    /**
     * {@inheritDoc}
     */
    public AccessToken readAccessToken(OAuth2Request request, String tokenId) throws ServerException,
            InvalidGrantException, NotFoundException {
        AccessToken loaded = request.getToken(AccessToken.class);
        if (loaded != null) {
            return loaded;
        }

        logger.message("Reading access token");

        JsonValue token;

        // Read from CTS
        try {
            token = tokenStore.read(tokenId);
        } catch (CoreTokenException e) {
            logger.error("Unable to read access token corresponding to id: " + tokenId, e);
            throw new ServerException("Could not read token in CTS: " + e.getMessage());
        }

        if (token == null) {
            logger.error("Unable to read access token corresponding to id: " + tokenId);
            throw new InvalidGrantException("Could not read token in CTS");
        }

        OpenAMAccessToken accessToken = new OpenAMAccessToken(token);
        validateTokenRealm(accessToken.getRealm(), request);

        request.setToken(AccessToken.class, accessToken);
        return accessToken;
    }

    /**
     * {@inheritDoc}
     */
    public RefreshToken readRefreshToken(OAuth2Request request, String tokenId) throws ServerException,
            InvalidGrantException, NotFoundException {
        RefreshToken loaded = request.getToken(RefreshToken.class);
        if (loaded != null) {
            return loaded;
        }

        logger.message("Read refresh token");
        JsonValue token;

        try {
            token = tokenStore.read(tokenId);
        } catch (CoreTokenException e) {
            logger.error("Unable to read refresh token corresponding to id: " + tokenId, e);
            throw new ServerException("Could not read token in CTS: " + e.getMessage());
        }

        if (token == null) {
            logger.error("Unable to read refresh token corresponding to id: " + tokenId);
            throw new InvalidGrantException("grant is invalid");
        }

        OpenAMRefreshToken refreshToken = new OpenAMRefreshToken(token);
        validateTokenRealm(refreshToken.getRealm(), request);

        request.setToken(RefreshToken.class, refreshToken);
        return refreshToken;
    }

    protected void validateTokenRealm(final String tokenRealm, final OAuth2Request request)
            throws InvalidGrantException, NotFoundException {
        final String normalisedRequestRealm = realmNormaliser.normalise(request.<String>getParameter(REALM));
        if (!tokenRealm.equals(normalisedRequestRealm) && !realmNormaliser.normalise(tokenRealm).equals(
                normalisedRequestRealm)) {
            throw new InvalidGrantException("Grant is not valid for the requested realm");
        }
    }

    /**
     * {@inheritDoc}
     */
    public DeviceCode createDeviceCode(Set<String> scope, ResourceOwner resourceOwner, String clientId, String nonce,
            String responseType, String state, String acrValues, String prompt, String uiLocales, String loginHint,
            Integer maxAge, String claims, OAuth2Request request, String codeChallenge, String codeChallengeMethod)
            throws ServerException, NotFoundException {

        logger.message("DefaultOAuthTokenStoreImpl::Creating Authorization code");

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        final String deviceCode = UUID.randomUUID().toString();
        final StringBuilder codeBuilder = new StringBuilder(CODE_LENGTH);

        String userCode = null;

        int i;
        for (i = 0; i < NUM_RETRIES; i++) {
            for (int k = 0; k < CODE_LENGTH; k++) {
                codeBuilder.append(ALPHABET.charAt(secureRandom.nextInt(ALPHABET.length())));
            }
            try {
                readDeviceCode(codeBuilder.toString(), request);
                codeBuilder.delete(0, codeBuilder.length());
                // code can be found - try again
            } catch (InvalidGrantException e) {
                // Good, it doesn't exist yet.
                userCode = codeBuilder.toString();
                break;
            } catch (ServerException e) {
                logger.message("Could not query CTS, assume duplicate to be safe", e);
            }
        }

        if (i == NUM_RETRIES) {
            throw new ServerException("Could not generate a unique user code");
        }

        long expiryTime = currentTimeMillis() + (1000 * providerSettings.getDeviceCodeLifetime());
        String resourceOwnerId = resourceOwner == null ? null : resourceOwner.getId();
        final DeviceCode code = new DeviceCode(deviceCode, userCode, resourceOwnerId, clientId, nonce,
                responseType, state, acrValues, prompt, uiLocales, loginHint, maxAge, claims, expiryTime, scope,
                realmNormaliser.normalise(request.<String>getParameter(REALM)), codeChallenge, codeChallengeMethod);

        // Store in CTS
        try {
            tokenStore.create(code);
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"CREATED_DEVICE_CODE", code.toString()};
                auditLogger.logAccessMessage("CREATED_DEVICE_CODE", obs, null);
            }
        } catch (CoreTokenException e) {
            if (auditLogger.isAuditLogEnabled()) {
                String[] obs = {"FAILED_CREATE_DEVICE_CODE", code.toString()};
                auditLogger.logErrorMessage("FAILED_CREATE_DEVICE_CODE", obs, null);
            }
            logger.error("Unable to create device code " + code, e);
            throw new ServerException("Could not create token in CTS");
        }

        request.setToken(DeviceCode.class, code);

        return code;
    }

    @Override
    public DeviceCode readDeviceCode(String clientId, String code, OAuth2Request request) throws ServerException,
            NotFoundException, InvalidGrantException {
        DeviceCode deviceCode = request.getToken(DeviceCode.class);
        if (deviceCode == null) {
            try {
                JsonValue token = tokenStore.read(code);

                if (token == null) {
                    return null;
                }

                deviceCode = new DeviceCode(token);
            } catch (CoreTokenException e) {
                logger.error("Unable to read device code corresponding to id: " + code, e);
                throw new ServerException("Could not read token in CTS: " + e.getMessage());
            }
        }
        if (!clientId.equals(deviceCode.getClientId())) {
            throw new InvalidGrantException();
        }
        validateTokenRealm(deviceCode.getRealm(), request);
        request.setToken(DeviceCode.class, deviceCode);
        return deviceCode;
    }

    @Override
    public DeviceCode readDeviceCode(String userCode, OAuth2Request request) throws ServerException, NotFoundException,
            InvalidGrantException {
        DeviceCode loaded = request.getToken(DeviceCode.class);
        if (loaded != null) {
            return loaded;
        }

        try {
            JsonValue token = tokenStore.query(equalTo(CoreTokenField.STRING_FOURTEEN, userCode));

            if (token.size() != 1) {
                throw new InvalidGrantException();
            }

            DeviceCode deviceCode = new DeviceCode(json(token.asSet().iterator().next()));
            request.setToken(DeviceCode.class, deviceCode);
            return deviceCode;
        } catch (CoreTokenException e) {
            logger.error("Unable to read device code corresponding to id: " + userCode, e);
            throw new ServerException("Could not read token in CTS: " + e.getMessage());
        }
    }

    @Override
    public void updateDeviceCode(DeviceCode code, OAuth2Request request) throws ServerException, NotFoundException,
            InvalidGrantException {
        try {
            readDeviceCode(code.getClientId(), code.getDeviceCode(), request);
            tokenStore.update(code);
        } catch (CoreTokenException e) {
            throw new ServerException("Could not update user code state");
        }
    }

    @Override
    public void deleteDeviceCode(String clientId, String code, OAuth2Request request) throws ServerException,
            NotFoundException, InvalidGrantException {
        try {
            readDeviceCode(clientId, code, request);
            tokenStore.delete(code);
        } catch (CoreTokenException e) {
            throw new ServerException("Could not delete user code state");
        }
    }
}
