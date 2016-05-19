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
 * Copyright 2015 ForgeRock AS.
 */

package org.forgerock.openam.uma;

import static org.forgerock.json.JsonValue.*;

import java.security.KeyPair;

import com.google.inject.Inject;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.JwsAlgorithm;
import org.forgerock.json.jose.jws.JwsAlgorithmType;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.utils.Utils;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.OAuth2Uris;
import org.forgerock.oauth2.core.OAuth2UrisFactory;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.core.RealmInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Claim gatherer for ID Token claim tokens.
 *
 * @since 13.0.0
 */
public class IdTokenClaimGatherer implements ClaimGatherer {

    /**
     * Expecting format value for an ID Token claim token.
     */
    public static final String FORMAT = "http://openid.net/specs/openid-connect-core-1_0.html#HybridIDToken";

    private final Logger logger = LoggerFactory.getLogger("UmaProvider");
    private final OAuth2ProviderSettingsFactory oauth2ProviderSettingsFactory;
    private final OAuth2UrisFactory<RealmInfo> oAuth2UrisFactory;
    private final ClientRegistrationStore clientRegistrationStore;
    private final JwtReconstruction jwtReconstruction;
    private final SigningManager signingManager;

    @Inject
    public IdTokenClaimGatherer(OAuth2ProviderSettingsFactory oauth2ProviderSettingsFactory,
            OAuth2UrisFactory<RealmInfo> oAuth2UrisFactory, ClientRegistrationStore clientRegistrationStore,
            JwtReconstruction jwtReconstruction, SigningManager signingManager) {
        this.oauth2ProviderSettingsFactory = oauth2ProviderSettingsFactory;
        this.oAuth2UrisFactory = oAuth2UrisFactory;
        this.clientRegistrationStore = clientRegistrationStore;
        this.jwtReconstruction = jwtReconstruction;
        this.signingManager = signingManager;
    }

    @Override
    public String getRequestingPartyId(OAuth2Request oAuth2Request, AccessToken authorizationApiToken,
            JsonValue claimToken) {

        try {
            SignedJwt idToken = jwtReconstruction.reconstructJwt(claimToken.asString(), SignedJwt.class);

            OAuth2ProviderSettings oAuth2ProviderSettings = oauth2ProviderSettingsFactory.get(oAuth2Request);
            OAuth2Uris oAuth2Uris = oAuth2UrisFactory.get(oAuth2Request);
            byte[] clientSecret = clientRegistrationStore.get(authorizationApiToken.getClientId(), oAuth2Request)
                    .getClientSecret().getBytes(Utils.CHARSET);
            KeyPair keyPair = oAuth2ProviderSettings.getServerKeyPair();

            if (!idToken.getClaimsSet().getIssuer().equals(oAuth2Uris.getIssuer())) {
                logger.warn("Issuer of id token, {0}, does not match issuer of authorization server, {1}.",
                        idToken.getClaimsSet().getIssuer(), oAuth2Uris.getIssuer());
                return null;
            }

            if (!verify(clientSecret, keyPair, idToken)) {
                logger.warn("Signature of id token is invalid.");
                return null;
            }

            return idToken.getClaimsSet().getSubject();
        } catch (InvalidClientException e) {
            logger.error("Failed to find client", e);
            return null;
        } catch (NotFoundException | ServerException e) {
            logger.error("Failed to find OAuth2 settings", e);
            return null;
        }
    }

    @Override
    public JsonValue getRequiredClaimsDetails(String issuer) {
        return json(object(
                field("name", "id_token"),
                field("claim_type", "urn:ietf:params:oauth:token-type:jwt"),
                field("claim_token_format", array(FORMAT)),
                field("issuer", array(issuer))));
    }

    private boolean verify(byte[] clientSecret, KeyPair keyPair, SignedJwt signedJwt) {
        JwsAlgorithm jwsAlgorithm = signedJwt.getHeader().getAlgorithm();
        SigningHandler signingHandler;
        if (JwsAlgorithmType.RSA.equals(jwsAlgorithm.getAlgorithmType())) {
            signingHandler = signingManager.newRsaSigningHandler(keyPair.getPublic());
        } else {
            signingHandler = signingManager.newHmacSigningHandler(clientSecret);
        }
        return signedJwt.verify(signingHandler);
    }
}
