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
package org.forgerock.openidconnect;

import java.util.Set;
import javax.inject.Inject;
import org.forgerock.oauth2.core.AuthorizeRequestValidator;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2ProviderSettings;
import org.forgerock.oauth2.core.OAuth2ProviderSettingsFactory;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.InvalidClientException;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;

/**
 * Checks that - since registering - the client's subject type is still valid
 * for requests to this provider, as providers have control over whether they
 * support these clients during operation via the interface.
 */
public class SubjectTypeValidator implements AuthorizeRequestValidator {

    private final OAuth2ProviderSettingsFactory providerSettingsFactory;
    private final OpenIdConnectClientRegistrationStore clientRegistrationStore;

    @Inject
    public SubjectTypeValidator(OAuth2ProviderSettingsFactory providerSettingsFactory,
                                OpenIdConnectClientRegistrationStore clientRegistrationStore) {
        this.providerSettingsFactory = providerSettingsFactory;
        this.clientRegistrationStore = clientRegistrationStore;
    }

    @Override
    public void validateRequest(OAuth2Request request) throws InvalidClientException,
            NotFoundException, ServerException {

        final OAuth2ProviderSettings settings = providerSettingsFactory.get(request);

        final Set<String> subjectTypesSupported = settings.getSupportedSubjectTypes();
        final String subjectType = clientRegistrationStore.get((String)
                request.getParameter(OAuth2Constants.Params.CLIENT_ID), request).getSubjectType().toLowerCase();

        for (String supported : subjectTypesSupported) {
            if (supported.toLowerCase().equals(subjectType)) {
                return;
            }
        }

        throw new InvalidClientException("Server does not support this client's subject type.");

    }
}