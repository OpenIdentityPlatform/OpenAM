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
 * Copyright 2013-2014 ForgeRock AS.
 */

package org.forgerock.openam.openidconnect;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.guice.core.InjectorHolder;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.jose.common.JwtReconstruction;
import org.forgerock.json.jose.jws.SignedJwt;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.ClientRegistrationStore;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.UnauthorizedClientException;
import org.forgerock.openam.oauth2.IdentityManager;
import org.forgerock.openam.oauth2.OpenAMSettings;
import org.forgerock.openidconnect.CheckSession;

import javax.servlet.http.HttpServletRequest;
import java.net.URI;
import java.security.KeyPair;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Defines what is needed to do the OpenID Connect check session endpoint.
 *
 * @since 11.0.0
 */
public class CheckSessionImpl implements CheckSession {

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final SSOTokenManager ssoTokenManager;
    private final OpenAMSettings openAMSettings;
    private final IdentityManager identityManager;
    private final ClientRegistrationStore clientRegistrationStore;

    /**
     * Constructs a new CheckSessionImpl.
     */
    public CheckSessionImpl() {
        ssoTokenManager = InjectorHolder.getInstance(SSOTokenManager.class);
        openAMSettings = InjectorHolder.getInstance(OpenAMSettings.class);
        identityManager = InjectorHolder.getInstance(IdentityManager.class);
        clientRegistrationStore = InjectorHolder.getInstance(ClientRegistrationStore.class);
    }

    /**
     * {@inheritDoc}
     */
    public String getCookieName() {
        return openAMSettings.getSSOCookieName();
    }

    /**
     * {@inheritDoc}
     */
    public String getClientSessionURI(HttpServletRequest request) throws UnauthorizedClientException,
            InvalidClientException {

        SignedJwt jwt = getIDToken(request);
        final KeyPair keyPair;
        try {
            keyPair = openAMSettings.getServerKeyPair(request.getParameter("realm"));
        } catch (Exception e) {
            return "";
        }
        if (jwt == null || !jwt.verify(keyPair.getPrivate())) {
            return "";
        }
        List<String> clients = jwt.getClaimsSet().getAudience();
        final String realm = (String)jwt.getClaimsSet().getClaim("realm");
        if (clients != null && !clients.isEmpty()){
            String client = clients.iterator().next();

            AMIdentity id = identityManager.getClientIdentity(client, realm);
            ClientRegistration clientRegistration = clientRegistrationStore.get(client, new OAuth2Request() {
                public <T> T getRequest() {
                    throw new UnsupportedOperationException();
                }

                public <T> T getParameter(String name) {
                    if ("realm".equals(name)) {
                        return (T) realm;
                    }
                    throw new UnsupportedOperationException();
                }

                public JsonValue getBody() {
                    throw new UnsupportedOperationException();
                }
            });
            return clientRegistration.getClientSessionURI();
        }
        return "";
    }

    /**
     * {@inheritDoc}
     */
    public boolean getValidSession(HttpServletRequest request) {
        SignedJwt jwt = getIDToken(request);
        final KeyPair keyPair;
        try {
            keyPair = openAMSettings.getServerKeyPair(request.getParameter("realm"));
        } catch (Exception e) {
            return false;
        }
        if (jwt == null || !jwt.verify(keyPair.getPrivate())) {
            return false;
        }
        try {
            String sessionID = (String) jwt.getClaimsSet().getClaim(OAuth2Constants.JWTTokenParams.OPS);
            SSOToken ssoToken = ssoTokenManager.createSSOToken(sessionID);
            return ssoTokenManager.isValidToken(ssoToken);
        } catch (Exception e){
            logger.error("CheckSessionImpl.getValidSession():Unable to get the SSO token in the JWT", e);
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

        if (map != null && map.containsKey("id_token")){
            String id_token = map.get("id_token");

            JwtReconstruction jwtReconstruction = new JwtReconstruction();
            return jwtReconstruction.reconstructJwt(id_token, SignedJwt.class);
        }
        return null;
    }

}
