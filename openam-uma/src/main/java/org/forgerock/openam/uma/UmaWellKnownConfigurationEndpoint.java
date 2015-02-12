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

import static org.forgerock.json.fluent.JsonValue.*;

import javax.inject.Inject;
import java.net.URI;
import java.util.Map;
import java.util.Set;

import org.forgerock.json.fluent.JsonValue;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.restlet.ext.jackson.JacksonRepresentation;
import org.restlet.representation.Representation;
import org.restlet.resource.Get;
import org.restlet.resource.ServerResource;

/**
 * .well-known configuration for a UMA Authorization Server instance.
 *
 * @since 13.0.0
 */
public class UmaWellKnownConfigurationEndpoint extends ServerResource {

    private final UmaProviderSettingsFactory providerSettingsFactory;

    /**
     * Constructs a new instance of a UmaWellKnownConfigurationEndpoint.
     *
     * @param providerSettingsFactory An instance of the UmaProviderSettingFactory.
     */
    @Inject
    public UmaWellKnownConfigurationEndpoint(UmaProviderSettingsFactory providerSettingsFactory) {
        this.providerSettingsFactory = providerSettingsFactory;
    }

    /**
     * Gets the configuration for the configured UMA provider for the realm.
     *
     * @return The UMA configuration.
     * @throws NotFoundException If no UMA provider has been configured for the realm.
     * @throws ServerException If there is a problem retrieving the configuration for the store.
     */
    @Get
    public Representation getConfiguration() throws NotFoundException, ServerException {

        UmaProviderSettings providerSettings = providerSettingsFactory.get(getRequest());

        JsonValue configuration = json(object(
                field("version", providerSettings.getVersion()),
                field("issuer", providerSettings.getIssuer()),
                field("pat_profiles_supported", providerSettings.getSupportedPATProfiles()),
                field("aat_profiles_supported", providerSettings.getSupportedAATProfiles()),
                field("rpt_profiles_supported", providerSettings.getSupportedRPTProfiles()),
                field("pat_grant_types_supported", providerSettings.getSupportedPATGrantTypes()),
                field("aat_grant_types_supported", providerSettings.getSupportedAATGrantTypes()),
                field("token_endpoint", providerSettings.getTokenEndpoint()),
                field("authorization_endpoint", providerSettings.getAuthorizationEndpoint()),
                field("introspection_endpoint", providerSettings.getTokenIntrospectionEndpoint()),
                field("resource_set_registration_endpoint", providerSettings.getResourceSetRegistrationEndpoint()),
                field("permission_registration_endpoint", providerSettings.getPermissionRegistrationEndpoint()),
                field("rpt_endpoint", providerSettings.getRPTEndpoint())));

        Set<String> supportedClaimTokenProfiles = providerSettings.getSupportedClaimTokenProfiles();
        if (supportedClaimTokenProfiles != null && !supportedClaimTokenProfiles.isEmpty()) {
            configuration.add("claim_token_profiles_supported", supportedClaimTokenProfiles);
        }
        Set<URI> supportedUmaProfiles = providerSettings.getSupportedUmaProfiles();
        if (supportedUmaProfiles != null && !supportedUmaProfiles.isEmpty()) {
            configuration.add("uma_profiles_supported", supportedUmaProfiles);
        }
        URI dynamicClientEndpoint = providerSettings.getDynamicClientEndpoint();
        if (dynamicClientEndpoint != null) {
            configuration.add("dynamic_client_endpoint", dynamicClientEndpoint);
        }
        URI requestingPartyClaimsEndpoint = providerSettings.getRequestingPartyClaimsEndpoint();
        if (requestingPartyClaimsEndpoint != null) {
            configuration.add("requesting_party_claims_endpoint", requestingPartyClaimsEndpoint.toString());
        }

        return new JacksonRepresentation<Map<String, Object>>(configuration.asMap());
    }
}
