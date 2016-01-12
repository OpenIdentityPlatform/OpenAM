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
 * Copyright 2014-2015 ForgeRock AS.
 */

package org.forgerock.openam.authentication.modules.oidc;

import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import org.forgerock.openam.utils.MappingUtils;
import org.forgerock.util.Reject;

import java.util.Map;
import java.util.Set;

/**
 * This class contains all of the module configuration state.
 */
class OpenIdConnectConfig extends JwtHandlerConfig {
    private static Debug logger = Debug.getInstance("amAuth");

    static final String RESOURCE_BUNDLE_NAME = "amAuthOpenIdConnect";
    static final String HEADER_NAME_KEY = "openam-auth-openidconnect-header-name";
    static final String PRINCIPAL_MAPPER_CLASS_KEY = "openam-auth-openidconnect-principal-mapper-class";
    static final String ACCOUNT_PROVIDER_CLASS_KEY = "openam-auth-openidconnect-account-provider-class";
    static final String JWK_TO_LOCAL_ATTRIBUTE_MAPPINGS_KEY = "openam-auth-openidconnect-jwt-to-local-attribute-mappings";
    static final String AUDIENCE_NAME_KEY = "openam-auth-openidconnect-audience-name";
    static final String ACCEPTED_AUTHORIZED_PARTIES_KEY = "openam-auth-openidconnect-accepted-authorized-parties";

    static final String BUNDLE_KEY_VERIFICATION_FAILED = "verification_failed";
    static final String BUNDLE_KEY_JWS_SIGNING_EXCEPTION = "jws_signing_exception";
    static final String BUNDLE_KEY_ISSUER_MISMATCH = "issuer_mismatch";
    static final String BUNDLE_KEY_TOKEN_ISSUER_MISMATCH = "token_issuer_mismatch";
    static final String BUNDLE_KEY_JWT_PARSE_ERROR = "jwt_parse_error";
    static final String BUNDLE_KEY_MISSING_HEADER = "missing_header";
    static final String BUNDLE_KEY_JWK_NOT_LOADED = "jwk_not_loaded";
    static final String BUNDLE_KEY_PRINCIPAL_MAPPER_INSTANTIATION_ERROR = "principal_mapper_instantiation_error";
    static final String BUNDLE_KEY_PRINCIPAL_MAPPING_FAILURE = "principal_mapping_failure";
    static final String BUNDLE_KEY_NO_ATTRIBUTES_MAPPED = "no_attributes_mapped";
    static final String BUNDLE_KEY_ID_TOKEN_BAD_AUDIENCE = "id_token_bad_audience";
    static final String BUNDLE_KEY_INVALID_AUTHORIZED_PARTY = "invalid_authorized_party";
    static final String BUNDLE_KEY_AUTHORIZED_PARTY_NOT_IN_AUDIENCE = "authorized_party_not_in_audience";
    static final String BUNDLE_KEY_NO_AUDIENCE_CLAIM = "no_audience_claim";

    private final String headerName;
    private final String principalMapperClass;
    private final Map<String, String> jwkToLocalAttributeMappings;
    private final String audienceName;
    private final Set<String> acceptedAuthorizedParties;
    private final String accountProviderClass;

    OpenIdConnectConfig(Map options) {
        super(options);
        headerName = CollectionHelper.getMapAttr(options, HEADER_NAME_KEY);
        principalMapperClass = CollectionHelper.getMapAttr(options, PRINCIPAL_MAPPER_CLASS_KEY);
        accountProviderClass = CollectionHelper.getMapAttr(options, ACCOUNT_PROVIDER_CLASS_KEY);
        Set<String> configuredJwkToLocalAttributeMappings = (Set<String>)options.get(JWK_TO_LOCAL_ATTRIBUTE_MAPPINGS_KEY);
        audienceName = CollectionHelper.getMapAttr(options, AUDIENCE_NAME_KEY);
        acceptedAuthorizedParties = (Set<String>)options.get(ACCEPTED_AUTHORIZED_PARTIES_KEY);
        Reject.ifNull(headerName, HEADER_NAME_KEY + " must be set in LoginModule options.");
        Reject.ifNull(configuredIssuer, ISSUER_NAME_KEY + " must be set in LoginModule options.");
        Reject.ifNull(cryptoContextType, CRYPTO_CONTEXT_TYPE_KEY + " must be set in LoginModule options.");
        Reject.ifNull(cryptoContextValue, CRYPTO_CONTEXT_VALUE_KEY + " must be set in LoginModule options.");
        Reject.ifNull(principalMapperClass, PRINCIPAL_MAPPER_CLASS_KEY + " must be set in LoginModule options.");
        Reject.ifNull(configuredJwkToLocalAttributeMappings, JWK_TO_LOCAL_ATTRIBUTE_MAPPINGS_KEY + " must be set in LoginModule options.");
        Reject.ifTrue(configuredJwkToLocalAttributeMappings.isEmpty(), JWK_TO_LOCAL_ATTRIBUTE_MAPPINGS_KEY + " must contain some valid mappings.");
        jwkToLocalAttributeMappings = MappingUtils.parseMappings(configuredJwkToLocalAttributeMappings);
    }

    public String getHeaderName() {
        return headerName;
    }

    public String getPrincipalMapperClass() {
        return principalMapperClass;
    }

    public String getAccountProviderClass() {
        return accountProviderClass;
    }

    public Map<String, String> getJwkToLocalAttributeMappings() {
        return jwkToLocalAttributeMappings;
    }

    public String getAudienceName() {
        return audienceName;
    }

    public Set<String> getAcceptedAuthorizedParties() {
        return acceptedAuthorizedParties;
    }
}
