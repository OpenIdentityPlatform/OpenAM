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
 */
package org.forgerock.openam.oauth2;

import java.net.URI;
import java.security.Key;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;
import org.forgerock.oauth2.core.OAuth2Jwt;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.agent.AgentConstants;
import org.forgerock.openidconnect.Client;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;

import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;

/**
 * Models an OpenAM J2EE agent registration in the OAuth2 provider.
 */
public class AgentClientRegistration implements OpenIdConnectClientRegistration {

    private static final Debug logger = Debug.getInstance("OAuth2Provider");
    private final AMIdentity identity;

    /**
     * Creates a new instance of {@code AgentClientRegistration}.
     *
     * @param identity The agent's identity.
     */
    AgentClientRegistration(AMIdentity identity) {
        this.identity = identity;
    }

    /**
     * Returns the underlying identity.
     *
     * @return The underlying identity.
     */
    public AMIdentity getIdentity() {
        return identity;
    }

    @Override
    public Set<URI> getRedirectUris() {
        Set<URI> redirectUris = new HashSet<>();
        final String cdssoRedirectUri = getCdssoRedirectUri();
        final Set<URI> agentRootUris = getAgentRootUrisForCDSSO();
        for (URI agentRootUri : agentRootUris) {
            redirectUris.add(agentRootUri.resolve(cdssoRedirectUri));
        }
        return redirectUris;
    }

    private Set<URI> getAgentRootUrisForCDSSO() {
        return Utils.getAttributeValuesAsUris(identity, AgentConstants.AGENT_ROOT_URL_FOR_CDSSO_ATTRIBUTE_NAME, logger);
    }

    private String getCdssoRedirectUri() {
        try {
            final Set<String> cdssoRedirectUris =
                    identity.getAttribute(AgentConstants.CDSSO_REDIRECT_URI_ATTRIBUTE_NAME);
            return cdssoRedirectUris.isEmpty() ? "" : cdssoRedirectUris.iterator().next();
        } catch (Exception e) {
            throw Utils.createException(AgentConstants.CDSSO_REDIRECT_URI_ATTRIBUTE_NAME, e, logger);
        }
    }

    @Override
    public Set<URI> getPostLogoutRedirectUris() {
        return Utils.getAttributeValuesAsUris(identity, AgentConstants.LOGOUT_ENTRY_UTI_ATTRIBUTE_NAME, logger);
    }

    @Override
    public Set<String> getAllowedResponseTypes() {
        return Sets.newHashSet(OAuth2Constants.AuthorizationEndpoint.ID_TOKEN);
    }

    @Override
    public String getClientId() {
        return identity.getName();
    }

    @Override
    public String getClientSecret() {
        return Utils.getAttributeValueFromSet(identity, AgentConstants.USER_PASSWORD_ATTRIBUTE_NAME, logger);
    }

    @Override
    public String getAccessTokenType() {
        return OAuth2Constants.Bearer.BEARER;
    }

    @Override
    public Set<String> getDefaultScopes() {
        return Sets.newHashSet(
                OAuth2Constants.Scopes.OPENID);
    }

    @Override
    public Set<String> getAllowedScopes() {
        return getDefaultScopes();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Agents are treated as confidential clients.
     */
    @Override
    public boolean isConfidential() {
        return true;
    }

    @Override
    public String getSubjectType() {
        return "Public";
    }

    /**
     * {@inheritDoc}
     * <p>
     * ID tokens granted to agents are currently not given back to OpenAM and therefore never verified.
     */
    @Override
    public boolean verifyJwtIdentity(OAuth2Jwt jwt) {
        return false;
    }

    @Override
    public String getIDTokenSignedResponseAlgorithm() {
        return "HS256";
    }

    @Override
    public String getTokenEndpointAuthMethod() {
        return Client.TokenEndpointAuthMethod.CLIENT_SECRET_BASIC.getType();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Not used as subject type is "Public".
     */
    @Override
    public URI getSectorIdentifierUri() {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Not used as subject type is "Public".
     */
    @Override
    public String getSubValue(String id, OAuth2ProviderSettings providerSettings) {
        return null;
    }

    @Override
    public long getAuthorizationCodeLifeTime(OAuth2ProviderSettings providerSettings) throws ServerException {
        return providerSettings.getAuthorizationCodeLifetime();
    }

    @Override
    public long getAccessTokenLifeTime(OAuth2ProviderSettings providerSettings) throws ServerException {
        return providerSettings.getAccessTokenLifetime();
    }

    @Override
    public long getRefreshTokenLifeTime(OAuth2ProviderSettings providerSettings) throws ServerException {
        return providerSettings.getRefreshTokenLifetime();
    }

    @Override
    public long getJwtTokenLifeTime(OAuth2ProviderSettings providerSettings) throws ServerException {
        return providerSettings.getOpenIdTokenLifetime();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The consent is always implied for agents' OpenId Connect client registration.
     */
    @Override
    public boolean isConsentImplied() {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The consent is always implied, so the display name is never used.
     */
    @Override
    public String getDisplayName(Locale locale) {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The consent is always implied, so the display description is never used.
     */
    @Override
    public String getDisplayDescription(Locale locale) {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The consent is always implied, so the scope descriptions are never used.
     */
    @Override
    public Map<String, String> getScopeDescriptions(Locale locale) throws ServerException {
        return new HashMap<>();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The consent is always implied, so the claim descriptions are never used.
     */
    @Override
    public Map<String, String> getClaimDescriptions(Locale locale) throws ServerException {
        return new HashMap<>();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Client Session URI is not supported for agents' client registration.
     */
    @Override
    public String getClientSessionURI() {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Encryption is currently not enabled.
     */
    @Override
    public boolean isIDTokenEncryptionEnabled() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Encryption is currently not enabled.
     */
    @Override
    public String getIDTokenEncryptionResponseAlgorithm() {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Encryption is currently not enabled.
     */
    @Override
    public String getIDTokenEncryptionResponseMethod() {
        return null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Encryption is currently not enabled.
     */
    @Override
    public Key getIDTokenEncryptionKey() {
        return null;
    }
}
