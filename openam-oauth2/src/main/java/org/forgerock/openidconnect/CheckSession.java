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
 * Copyright 2013-2016 ForgeRock AS.
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openidconnect;

import static org.forgerock.openam.oauth2.OAuth2Constants.JWTTokenParams.*;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.shared.debug.Debug;

import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.json.jose.jws.SigningManager;
import org.forgerock.json.jose.jws.handlers.SigningHandler;
import org.forgerock.json.jose.jwt.Jwt;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openam.cts.CTSPersistentStore;
import org.forgerock.openam.cts.adapters.TokenAdapter;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.openam.utils.OpenAMSettings;
import org.forgerock.openam.utils.StringUtils;

/**
 * Interface is to define what needs to be implemented to do the OpenID Connect check session endpoint.
 *
 * @since 12.0.0
 * 
 */
public class CheckSession {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final SSOTokenManager ssoTokenManager;
    private final OpenAMSettings openAMSettings;
    private final SigningManager signingManager;
    private final ClientRegistrationStore clientRegistrationStore;
    private final CTSPersistentStore cts;
    private final TokenAdapter<JsonValue> tokenAdapter;

    /**
     * Constructs a new CheckSession.
     */
    public CheckSession() {
        ssoTokenManager = InjectorHolder.getInstance(SSOTokenManager.class);
        openAMSettings = InjectorHolder.getInstance(OpenAMSettings.class);
        signingManager = InjectorHolder.getInstance(SigningManager.class);
        clientRegistrationStore = InjectorHolder.getInstance(ClientRegistrationStore.class);
        cts = InjectorHolder.getInstance(CTSPersistentStore.class);
        tokenAdapter = InjectorHolder.getInstance(Key.get(new TypeLiteral<TokenAdapter<JsonValue>>() { },
                Names.named(OAuth2Constants.CoreTokenParams.OAUTH_TOKEN_ADAPTER)));
    }

    /**
     * Get the cookie name containing the session information.
     *
     * @return The cookie name.
     */
    public String getCookieName() {
        return openAMSettings.getSSOCookieName();
    }

    /**
     * Get the URL the postMessage must be coming from (registered in client) to process the message.
     *
     * @param request The HttpServletRequest.
     * @return The url as a string or empty String.
     */
    public String getClientSessionURI(HttpServletRequest request) throws UnauthorizedClientException,
            InvalidClientException, NotFoundException {

        SignedJwt jwt = getIDToken(request);

        if (jwt == null) {
            return "";
        }

        final ClientRegistration clientRegistration = getClientRegistration(jwt);

        if (clientRegistration != null && !isJwtValid(jwt, clientRegistration)) {
            return "";
        }

        return clientRegistration.getClientSessionURI();
    }

    /**
     * Gets the Client's registration based from the audience set in the JWT.
     *
     * @param jwt The JWT.
     * @return The Client's registration.
     * @throws InvalidClientException If the client's registration is not found.
     */
    private ClientRegistration getClientRegistration(Jwt jwt) throws InvalidClientException, NotFoundException {

        List<String> clients = jwt.getClaimsSet().getAudience();
        final String realm = (String)jwt.getClaimsSet().getClaim(REALM);
        if (clients != null && !clients.isEmpty()) {
            String client = clients.iterator().next();

            ClientRegistration clientRegistration = clientRegistrationStore.get(client, OAuth2Request.forRealm(realm));
            return clientRegistration;
        }
        return null;
    }

    /**
     * Determines if the specified signed JWT is valid.
     *
     * @param jwt The signed JWT.
     * @param clientRegistration The client's registration.
     * @return {@code true} if the JWT is valid.
     */
    private boolean isJwtValid(SignedJwt jwt, ClientRegistration clientRegistration) {
        //client_secret for newHmacSigningHandler shouldn't be null
        String clientSecret = clientRegistration.getClientSecret();
        if (StringUtils.isEmpty(clientSecret)) {
            return false;
        }
        final SigningHandler signingHandler = signingManager.newHmacSigningHandler(
                clientSecret.getBytes(Charset.forName("UTF-8")));
        return jwt != null && jwt.verify(signingHandler);
    }

    /**
     * Check if the JWT contains a valid session id.
     *
     * @param request The HttpServletRequset.
     * @return {@code true} if valid.
     */
    public boolean getValidSession(HttpServletRequest request) {
        SignedJwt jwt = getIDToken(request);

        if (jwt == null) {
            return false;
        }

        try {
            final ClientRegistration clientRegistration = getClientRegistration(jwt);

            if (clientRegistration != null && !isJwtValid(jwt, clientRegistration)) {
                return false;
            }

            String opsId = (String) jwt.getClaimsSet().getClaim(OPS);
            if (opsId == null) {
                opsId = (String) jwt.getClaimsSet().getClaim(LEGACY_OPS);
            }
            JsonValue idTokenUserSessionToken = tokenAdapter.fromToken(cts.read(opsId));
            String sessionId = idTokenUserSessionToken.get(LEGACY_OPS).asString();

            SSOToken ssoToken = ssoTokenManager.createSSOToken(sessionId);
            return ssoTokenManager.isValidToken(ssoToken);
        } catch (Exception e){
            logger.error("Unable to get the SSO token", e);
            return false;
        }
    }

    private SignedJwt getIDToken(HttpServletRequest request) {
        URI referer = null;
        try {
            referer = new URI(request.getHeader("Referer"));
        } catch (Exception e){
            logger.error("No id_token supplied to the checkSesison endpoint", e);
            return null;
        }
        Map<String, String> map = null;
        if (referer != null && referer.getQuery() != null && !referer.getQuery().isEmpty()){
            String query =  referer.getQuery();
            String[] params = query.split("&");
            map = new HashMap<String, String>();
            for (String param : params){
                int split = param.indexOf('=');
                String name = param.substring(0, split);
                String value = param.substring(split+1, param.length());
                map.put(name, value);
            }
        }

        if (map != null && map.containsKey(ID_TOKEN)){
            String id_token = map.get(ID_TOKEN);

            JwtReconstruction jwtReconstruction = new JwtReconstruction();
            return jwtReconstruction.reconstructJwt(id_token, SignedJwt.class);
        }
        return null;
    }
}
