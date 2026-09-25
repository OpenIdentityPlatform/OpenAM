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
 * Copyright 2026 3A Systems, LLC.
 */

package org.forgerock.openidconnect;

import static org.forgerock.openidconnect.IdTokenSignatureVerifier.forLog;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.exceptions.JwtRuntimeException;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithmType;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.BadRequestException;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.oauth2.OAuthProblemException;
import org.forgerock.openam.utils.RealmUtils;
import org.forgerock.openam.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates the {@code id_token_hint} that the OpenID Connect end-session endpoint is given.
 *
 * <p>The hint carries the identity of the client whose registration governs the logout redirect,
 * and the identifier of the session to destroy. Both are decisions the caller must not be able to
 * make on its own, so the token has to be shown to have been issued by this provider before either
 * claim is read.
 *
 * <p>The claim naming the client is read before verification, which is safe: it only selects the
 * key the signature is checked against. A hint naming a client whose key the sender does not hold
 * fails verification. The algorithm is taken from that client's registration, and the {@code alg}
 * header of the hint has to name the same one, so the sender chooses neither the key nor the
 * digest.
 *
 * @since 16.2.0
 */
@Singleton
public class IdTokenHintValidator {

    private static final Pattern REPEATED_SEPARATORS = Pattern.compile("/+");

    private final Logger logger = LoggerFactory.getLogger("OAuth2Provider");

    private final OpenIdConnectClientRegistrationStore clientRegistrationStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final IdTokenSignatureVerifier signatureVerifier;

    /**
     * Constructs a new IdTokenHintValidator.
     *
     * @param clientRegistrationStore An instance of the OpenIdConnectClientRegistrationStore.
     * @param providerSettingsFactory An instance of the OAuth2ProviderSettingsFactory.
     */
    @Inject
    public IdTokenHintValidator(OpenIdConnectClientRegistrationStore clientRegistrationStore,
            OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this(clientRegistrationStore, providerSettingsFactory, new IdTokenSignatureVerifier());
    }

    IdTokenHintValidator(OpenIdConnectClientRegistrationStore clientRegistrationStore,
            OAuth2ProviderSettingsFactory providerSettingsFactory, IdTokenSignatureVerifier signatureVerifier) {
        this.clientRegistrationStore = clientRegistrationStore;
        this.providerSettingsFactory = providerSettingsFactory;
        this.signatureVerifier = signatureVerifier;
    }

    /**
     * Parses the given {@code id_token_hint} and verifies that this provider issued it to the
     * client it names, in the realm the request is addressed to.
     *
     * <p>Expiry is deliberately not enforced. Logout regularly happens long after the id_token it
     * refers to has expired, and the signature is what establishes that the provider issued the
     * hint; an expired hint is not a forged one.
     *
     * @param request The OAuth2 request.
     * @param idTokenHint The value of the {@literal id_token_hint} parameter.
     * @return The parsed and verified token.
     * @throws BadRequestException If the hint is missing, malformed, names no client, was issued in
     *         another realm, or its signature does not verify.
     * @throws InvalidClientException If the client named by the hint is not registered.
     * @throws NotFoundException If the realm does not exist.
     * @throws ServerException If the provider's own keys cannot be read.
     */
    public VerifiedIdTokenHint validate(OAuth2Request request, String idTokenHint)
            throws BadRequestException, InvalidClientException, NotFoundException, ServerException {

        if (StringUtils.isEmpty(idTokenHint)) {
            logger.warn("No id_token_hint parameter supplied to the endSession endpoint");
            throw new BadRequestException("The endSession endpoint requires an id_token_hint parameter");
        }

        try {
            return verify(request, idTokenHint);
        } catch (OAuthProblemException e) {
            // The identity store could not be read. That is this server's problem, not a defect of
            // the hint, and must not be reported as a bad request.
            throw e;
        } catch (RuntimeException e) {
            // The endpoint is unauthenticated and every byte of the hint is caller-written, so a
            // header or a claim of the wrong shape is a bad request rather than a server fault. Left
            // to propagate it would answer with server_error and log a stack trace per request - so
            // the stack goes to debug here too, for the same reason.
            logger.warn("The id_token_hint supplied to the endSession endpoint could not be read: {}",
                    e.getClass().getSimpleName());
            logger.debug("The id_token_hint supplied to the endSession endpoint could not be read", e);
            throw new BadRequestException("The id_token_hint is not an id_token issued by this provider");
        }
    }

    private VerifiedIdTokenHint verify(OAuth2Request request, String idTokenHint)
            throws BadRequestException, InvalidClientException, NotFoundException, ServerException {

        final SignedJwt jwt;
        try {
            jwt = new JwtReconstruction().reconstructJwt(idTokenHint, SignedJwt.class);
        } catch (JwtRuntimeException e) {
            logger.warn("The id_token_hint supplied to the endSession endpoint could not be parsed");
            logger.debug("The id_token_hint supplied to the endSession endpoint could not be parsed", e);
            throw new BadRequestException("The id_token_hint is not a valid signed JWT");
        }

        final String clientId = IdTokenSignatureVerifier.clientIdOf(jwt);
        if (StringUtils.isEmpty(clientId)) {
            logger.warn("The id_token_hint supplied to the endSession endpoint names no client");
            throw new BadRequestException("The id_token_hint must carry an azp or aud claim");
        }

        final OpenIdConnectClientRegistration clientRegistration = clientRegistrationStore.get(clientId, request);
        requireIssuedToThisClient(clientId, clientRegistration);

        if (!isSignatureValid(jwt, clientRegistration, request)) {
            logger.warn("The id_token_hint supplied to the endSession endpoint for client '{}' is not signed by "
                    + "this provider", forLog(clientId));
            throw new BadRequestException("The id_token_hint signature is not valid");
        }

        requireIdToken(jwt, clientId);
        requireSameRealm(request, jwt, clientId);

        return new VerifiedIdTokenHint(jwt, clientRegistration);
    }

    /**
     * Rejects a hint whose client is not the one the registration lookup answered with.
     *
     * <p>{@code OpenAMClientRegistrationStore.get(clientId, request)} answers from the registration
     * the request was seeded with and discards the id it was asked for, and
     * {@code OAuth2RequestFactory} seeds that from the {@code client_id} query parameter. Without
     * this the caller would still choose whose registration - and so whose post-logout redirect
     * URIs - the hint is measured against, by naming a client in the query string instead of in the
     * {@code azp} claim. That is the defect this validator exists to close, one parameter over: the
     * algorithm and the key would both be read from the named client, and for the RSA and ECDSA
     * algorithms the key is the provider's own, shared by every client in the realm.
     */
    private void requireIssuedToThisClient(String clientId, OpenIdConnectClientRegistration clientRegistration)
            throws BadRequestException {

        if (!clientId.equals(clientRegistration.getClientId())) {
            logger.warn("The id_token_hint supplied to the endSession endpoint was issued to client '{}', but the "
                    + "request resolved client '{}'", forLog(clientId),
                    forLog(clientRegistration.getClientId()));
            throw new BadRequestException("The id_token_hint was not issued to the client this request names");
        }
    }

    /**
     * Rejects a token this provider signed that is not an id_token.
     *
     * <p>Access and refresh tokens issued statelessly are signed with the same key, and carry an
     * {@code aud} naming their client, so a signature on its own does not say what kind of token
     * this is. The tokens this provider mints name themselves, and the token endpoint reads that
     * claim for the same purpose. A hint that carries no name at all predates the claim and is
     * taken as the id_token it says nothing to the contrary about.
     */
    private void requireIdToken(SignedJwt jwt, String clientId) throws BadRequestException {

        if (!IdTokenSignatureVerifier.isIdToken(jwt)) {
            logger.warn("The token supplied to the endSession endpoint as an id_token_hint for client '{}' is a "
                    + "'{}'", forLog(clientId), forLog(IdTokenSignatureVerifier.tokenNameOf(jwt)));
            throw new BadRequestException("The id_token_hint is not an id_token");
        }
    }

    private boolean isSignatureValid(SignedJwt jwt, OpenIdConnectClientRegistration clientRegistration,
            OAuth2Request request) throws ServerException, NotFoundException, BadRequestException {

        final JwsAlgorithm algorithm = IdTokenSignatureVerifier.registeredAlgorithm(
                clientRegistration.getIDTokenSignedResponseAlgorithm());
        if (algorithm == null) {
            throw new BadRequestException("The client has no usable id_token signing algorithm");
        }

        // Only the provider's own algorithms need a provider key; an HMAC id_token is signed with
        // the client's secret, and reading the keystore for one is pointless work.
        final PublicKey signingKey = JwsAlgorithmType.HMAC.equals(algorithm.getAlgorithmType())
                ? null
                : signingKeyOf(request, algorithm);

        return signatureVerifier.isSignatureValid(jwt, algorithm, clientRegistration.getClientSecret(), signingKey);
    }

    private PublicKey signingKeyOf(OAuth2Request request, JwsAlgorithm algorithm)
            throws ServerException, NotFoundException {
        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(request);
        final KeyPair signingKeyPair = providerSettings.getSigningKeyPair(algorithm);
        return signingKeyPair == null ? null : signingKeyPair.getPublic();
    }

    /**
     * Rejects a hint issued in another realm.
     *
     * <p>The RSA and ECDSA signing keys are read from the keystore by an alias that is the same in
     * every realm by default, so a signature alone does not say which realm the hint came from.
     * Without this the realm of the request would decide whose client registration - and so whose
     * post-logout redirect URIs - a hint from elsewhere is measured against.
     */
    private void requireSameRealm(OAuth2Request request, SignedJwt jwt, String clientId) throws BadRequestException {

        final String tokenRealm = realmPathOf(
                jwt.getClaimsSet().get(OAuth2Constants.JWTTokenParams.REALM).asString());
        final String requestRealm = realmPathOf(request.<String>getParameter(OAuth2Constants.Params.REALM));

        if (!tokenRealm.equalsIgnoreCase(requestRealm)) {
            logger.warn("The id_token_hint supplied to the endSession endpoint for client '{}' was issued in realm "
                    + "'{}', not in realm '{}'", forLog(clientId), forLog(tokenRealm), requestRealm);
            throw new BadRequestException("The id_token_hint was not issued in this realm");
        }
    }

    /**
     * Reduces a realm to the '/' separated form the {@literal realm} claim is written in, so that
     * the two can be compared. An absent realm is the root realm, as it is everywhere else that
     * reads this claim.
     *
     * <p>The two sides reach this from different places - the request from {@code Realm.asPath()},
     * the claim from {@code RealmNormaliser.normalise()} - so repeated separators are collapsed
     * rather than assumed away.
     */
    private static String realmPathOf(String realm) {
        if (StringUtils.isEmpty(realm)) {
            return "/";
        }
        final String path = RealmUtils.cleanRealm(REPEATED_SEPARATORS.matcher(realm.trim()).replaceAll("/"));
        return StringUtils.isEmpty(path) ? "/" : path;
    }

    /**
     * An {@code id_token_hint} whose signature has been verified, together with the registration of
     * the client it names.
     */
    public static final class VerifiedIdTokenHint {

        private final SignedJwt jwt;
        private final OpenIdConnectClientRegistration clientRegistration;

        VerifiedIdTokenHint(SignedJwt jwt, OpenIdConnectClientRegistration clientRegistration) {
            this.jwt = jwt;
            this.clientRegistration = clientRegistration;
        }

        /**
         * Returns the verified token.
         *
         * @return The verified token.
         */
        public SignedJwt getJwt() {
            return jwt;
        }

        /**
         * Returns the registration of the client the token was issued to.
         *
         * @return The client registration.
         */
        public OpenIdConnectClientRegistration getClientRegistration() {
            return clientRegistration;
        }
    }
}
