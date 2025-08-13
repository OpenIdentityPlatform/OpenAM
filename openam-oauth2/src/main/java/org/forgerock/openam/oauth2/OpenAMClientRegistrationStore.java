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
 * Portions Copyrighted 2015 Nomura Research Institute, Ltd
 * Portions copyright 2025 3A Systems LLC.
 */

package org.forgerock.openam.oauth2;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.forgerock.i18n.LocalizedIllegalArgumentException;
import org.forgerock.jaspi.modules.openid.resolvers.service.OpenIdResolverService;
import org.forgerock.oauth2.core.ClientRegistration;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.PEMDecoder;
import org.forgerock.oauth2.core.exceptions.ClientAuthenticationFailureFactory;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.openam.identity.idm.AMIdentityRepositoryFactory;
import org.forgerock.openam.utils.RealmNormaliser;
import org.forgerock.openidconnect.OpenIdConnectClientRegistration;
import org.forgerock.openidconnect.OpenIdConnectClientRegistrationStore;
import org.forgerock.services.context.Context;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.common.configuration.AgentConfiguration;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.debug.Debug;

/**
 * The OpenAM OAuth2 and OpenId Connect provider's store for all client registrations.
 *
 * @since 12.0.0
 */
@Singleton
public class OpenAMClientRegistrationStore implements OpenIdConnectClientRegistrationStore {

    private static final String AUTHENTICATION_FAILURE_MESSAGE = "Client authentication failed";

    private final Debug logger = Debug.getInstance("OAuth2Provider");
    private final RealmNormaliser realmNormaliser;
    private final PEMDecoder pemDecoder;
    private final OpenIdResolverService resolverService;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final ClientAuthenticationFailureFactory failureFactory;
    private final AMIdentityRepositoryFactory identityRepositoryFactory;
    private final PrivilegedAction<SSOToken> adminTokenAction;

    /**
     * Constructs a new OpenAMClientRegistrationStore.
     * @param realmNormaliser Realm normaliser.
     * @param pemDecoder PEM decoder.
     * @param resolverService OpenId Connect resolver service.
     * @param providerSettingsFactory Factory for creating/retrieving OAuth2ProviderSettings instances.
     * @param failureFactory Factory for handling the invalid_client error of the OAuth2 specification.
     * @param identityRepositoryFactory Factory for creating {@code AMIdentityRepository} instances.
     * @param adminTokenAction Privileged action to get application single sign on token.
     */
    @Inject
    public OpenAMClientRegistrationStore(RealmNormaliser realmNormaliser, PEMDecoder pemDecoder,
            @Named(OAuth2Constants.Custom.JWK_RESOLVER) OpenIdResolverService resolverService,
            OAuth2ProviderSettingsFactory providerSettingsFactory, ClientAuthenticationFailureFactory failureFactory,
            AMIdentityRepositoryFactory identityRepositoryFactory, PrivilegedAction<SSOToken> adminTokenAction) {
        this.realmNormaliser = realmNormaliser;
        this.pemDecoder = pemDecoder;
        this.resolverService = resolverService;
        this.providerSettingsFactory = providerSettingsFactory;
        this.failureFactory = failureFactory;
        this.identityRepositoryFactory = identityRepositoryFactory;
        this.adminTokenAction = adminTokenAction;
    }

    /**
     * {@inheritDoc}
     */
    public OpenIdConnectClientRegistration get(String clientId, OAuth2Request request)
            throws NotFoundException, InvalidClientException {
        OpenIdConnectClientRegistration clientRegistration =
                (OpenIdConnectClientRegistration) request.getClientRegistration();

        return (clientRegistration != null)
                ? clientRegistration
                : getClientRegistration(clientId, request.<String>getParameter(OAuth2Constants.Custom.REALM), request);
    }

    /**
     * {@inheritDoc}
     */
    public OpenIdConnectClientRegistration get(String clientId, String realm, Context context)
            throws InvalidClientException, NotFoundException {
        return getClientRegistration(clientId, realm, null);
    }

    private OpenIdConnectClientRegistration getClientRegistration(String clientId, String realm, OAuth2Request request)
            throws InvalidClientException, NotFoundException {
        try {
            final String normalisedRealm = realmNormaliser.normalise(realm);
            AMIdentity identity = getIdentity(clientId, normalisedRealm, request);
            if (isJ2eeAgent(identity) || isWebAgent(identity)) {
                return new AgentClientRegistration(identity);
            } else {
                OAuth2ProviderSettings providerSettings =
                        providerSettingsFactory.getRealmProviderSettings(normalisedRealm);
                return new OpenAMClientRegistration(identity, pemDecoder, resolverService,
                        providerSettings, failureFactory);
            }
        } catch (org.forgerock.json.resource.NotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (SSOException | IdRepoException e) {
            throw failureFactory.getException(request, AUTHENTICATION_FAILURE_MESSAGE);
        }
    }

    private AMIdentity getIdentity(String name, String realm, OAuth2Request request) throws InvalidClientException {
        final SSOToken token = AccessController.doPrivileged(adminTokenAction);
        try {
            final AMIdentity identity = searchIdentity(name, realm, token, request);
            if (identity.isActive() && name.equals(identity.getName())) { //check case sensitivity due to https://tools.ietf.org/html/rfc6749#section-1.9
                return identity;
            }
            throw failureFactory.getException(request, AUTHENTICATION_FAILURE_MESSAGE);
        } catch (SSOException | IdRepoException | LocalizedIllegalArgumentException e) {
            logger.error("Unable to get client AMIdentity: ", e);
            throw failureFactory.getException(request, AUTHENTICATION_FAILURE_MESSAGE);
        }
    }

    @SuppressWarnings("unchecked")
    private AMIdentity searchIdentity(String name, String realm, SSOToken token, OAuth2Request request)
            throws IdRepoException, SSOException, InvalidClientException {
        final AMIdentityRepository identityRepository = identityRepositoryFactory.create(realm, token);
        Set<AMIdentity> identities = Collections.emptySet();
        IdSearchResults searchResults = identityRepository.searchIdentities(IdType.AGENT, name, getSearchOptions());
        if (searchResults != null) {
            identities = searchResults.getSearchResults();
        }
        if (identities == null || identities.size() != 1) {
            throw failureFactory.getException(request, AUTHENTICATION_FAILURE_MESSAGE);

        }
        return identities.iterator().next();
    }

    private IdSearchControl getSearchOptions() {
        final IdSearchControl idSearchControl = new IdSearchControl();
        idSearchControl.setRecursive(true);
        idSearchControl.setAllReturnAttributes(true);
        idSearchControl.setMaxResults(0);

        return idSearchControl;
    }

    private boolean isJ2eeAgent(AMIdentity identity) throws IdRepoException, SSOException {
        return AgentConfiguration.AGENT_TYPE_J2EE.equalsIgnoreCase(AgentConfiguration.getAgentType(identity));
    }

    private boolean isWebAgent(AMIdentity identity) throws IdRepoException, SSOException {
        return AgentConfiguration.AGENT_TYPE_WEB.equalsIgnoreCase(AgentConfiguration.getAgentType(identity));
    }
}