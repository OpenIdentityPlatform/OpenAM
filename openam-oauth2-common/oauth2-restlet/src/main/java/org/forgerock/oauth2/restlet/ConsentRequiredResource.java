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
 * Copyright 2015-2016 ForgeRock AS.
 */
package org.forgerock.oauth2.restlet;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerConsentRequired;
import org.forgerock.openam.rest.service.RouterContextResource;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.xui.XUIState;
import org.owasp.esapi.ESAPI;
import org.restlet.data.Reference;
import org.restlet.ext.servlet.ServletUtils;
import org.restlet.routing.Router;

import static org.forgerock.json.JsonValue.*;

/**
 * Handles requests to the OAuth2 authorize endpoint that require consent from the user.
 *
 * @since 12.0.0
 */
public abstract class ConsentRequiredResource extends RouterContextResource {
    protected final XUIState xuiState;
    protected final BaseURLProviderFactory baseURLProviderFactory;

    public ConsentRequiredResource(Router router, BaseURLProviderFactory baseURLProviderFactory, XUIState xuiState) {
        super(router);
        this.baseURLProviderFactory = baseURLProviderFactory;
        this.xuiState = xuiState;
    }

    /**
     * Gets the data model to use when rendering the error page.
     *
     * @param consentRequired The details for requesting consent.
     * @param request The OAuth2 request.
     * @return The data model.
     */
    protected Map<String, Object> getDataModel(ResourceOwnerConsentRequired consentRequired, OAuth2Request request) {
        String displayName = consentRequired.getClientName();
        String displayDescription = consentRequired.getClientDescription();
        String userDisplayName = consentRequired.getUserDisplayName();
        Map<String, Object> data = new HashMap<>(getRequest().getAttributes());
        data.putAll(getQuery().getValuesMap());
        Reference resRef = getRequest().getResourceRef();
        String target = resRef.getPath();
        String query = resRef.getQuery();
        if (!StringUtils.isBlank(query)) {
            target = target + "?" + query;
        }
        data.put("target", target);
        data.put("display_name", ESAPI.encoder().encodeForHTML(displayName));
        data.put("display_description", ESAPI.encoder().encodeForHTML(displayDescription));
        addDisplayScopesAndClaims(consentRequired, data);
        data.put("user_name", userDisplayName);
        data.put("xui", xuiState.isXUIEnabled());
        data.put("user_code", request.getParameter(OAuth2Constants.DeviceCode.USER_CODE));
        data.put("baseUrl", baseURLProviderFactory.get(request.<String>getParameter("realm"))
                .getRootURL(ServletUtils.getRequest(getRequest())));
        return data;
    }

    private void addDisplayScopesAndClaims(ResourceOwnerConsentRequired consentRequired, Map<String, Object> data) {
        JsonValue scopes = json(array());
        Set<String> allScopeClaims = new HashSet<>();
        final Map<String, List<String>> compositeScopes = consentRequired.getClaims().getCompositeScopes();
        final Map<String, String> claimDescriptions = consentRequired.getClaimDescriptions();
        final Map<String, Object> claimValues = new LinkedHashMap<>(consentRequired.getClaims().getValues());

        for (Map.Entry<String, String> scope : consentRequired.getScopeDescriptions().entrySet()) {
            JsonValue value = json(object(field("name", encodeForHTML(scope.getValue()))));
            scopes.add(value.getObject());
            List<String> scopeClaims = compositeScopes.get(scope.getKey());
            if (scopeClaims != null) {
                final LinkedHashMap<String, Object> claims = new LinkedHashMap<>();
                value.put("values", claims);
                for (String claim : scopeClaims) {
                    Object claimValue = claimValues.get(claim);
                    if (claimValue != null) {
                        String claimDescription = claimDescriptions.get(claim);
                        if (claimDescription == null) {
                            claimDescription = claim;
                        }
                        claims.put(
                                encodeForHTML(claimDescription),
                                encodeForHTML(claimValue.toString()));
                        allScopeClaims.add(claim);
                    }
                }
            }
        }
        data.put("display_scopes", scopes.toString());

        for (String claim : allScopeClaims) {
            claimValues.remove(claim);
        }

        JsonValue claims = json(array());
        for (Map.Entry<String, Object> claim : claimValues.entrySet()) {
            claims.add(object(
                    field("name", encodeForHTML(claimDescriptions.get(claim.getKey()))),
                    field("values", encodeForHTML(claimValues.get(claim.getKey()).toString()))
            ));
        }
        data.put("display_claims", claims.toString());
    }

    /**
     * Encodes a description so that it can be displayed in a HTML page.
     *
     * @param description The {@code String} to encode.
     * @return The encoded {@code String}.
     */
    private String encodeForHTML(String description) {
        return ESAPI.encoder().encodeForHTML(description);
    }
}
