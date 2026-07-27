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
 * Portions Copyrighted 2019 Open Source Solution Technology Corp.
 * Portions Copyrighted 2025-2026 3A Systems LLC.
 */
package org.forgerock.oauth2.restlet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.forgerock.json.JsonValue;
import org.forgerock.openam.oauth2.OAuth2Constants;
import org.forgerock.oauth2.core.CsrfProtection;
import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.oauth2.core.ResourceOwnerSessionValidator;
import org.forgerock.oauth2.core.exceptions.ResourceOwnerConsentRequired;
import org.forgerock.openam.oauth2.OAuth2Utils;
import org.forgerock.openam.rest.service.RouterContextResource;
import org.forgerock.openam.services.baseurl.BaseURLProviderFactory;
import org.forgerock.openam.xui.XUIState;
import org.owasp.esapi.ESAPI;
import org.restlet.data.Language;
import org.restlet.data.Preference;
import org.restlet.data.Reference;
import org.forgerock.openam.rest.jakarta.servlet.ServletUtils;
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
    private final ResourceOwnerSessionValidator resourceOwnerSessionValidator;
    private final CsrfProtection csrfProtection;

    public ConsentRequiredResource(Router router, BaseURLProviderFactory baseURLProviderFactory,
            XUIState xuiState, ResourceOwnerSessionValidator resourceOwnerSessionValidator,
            CsrfProtection csrfProtection) {
        super(router);
        this.baseURLProviderFactory = baseURLProviderFactory;
        this.xuiState = xuiState;
        this.resourceOwnerSessionValidator = resourceOwnerSessionValidator;
        this.csrfProtection = csrfProtection;
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
        data.put("saveConsentEnabled", consentRequired.isSaveConsentEnabled());

        // Mint a dedicated, random CSRF token bound to this authorization request instead of reusing the
        // script-readable SSO cookie value. This allows the SSO cookie to ship as HttpOnly.
        data.put("csrf", csrfProtection.createCsrfToken(request));
        List<String> locale = new ArrayList<>();
        for (Preference<Language> language : getRequest().getClientInfo().getAcceptedLanguages()) {
            locale.add(language.getMetadata().getName());
        }
        data.put("locale", OAuth2Utils.joinStatic(locale, " "));

        return data;
    }

    /**
     * Gets the consent details as JSON, for clients that drive the consent flow programmatically instead of
     * rendering the consent page. Carries the same information as {@link #getDataModel}, but unencoded and
     * without the presentation-specific fields.
     *
     * <p>Mints the CSRF token, so it must be called at most once per request - exactly like
     * {@link #getDataModel}, and on a mutually exclusive branch from it.</p>
     *
     * @param consentRequired The details for requesting consent.
     * @param request The OAuth2 request.
     * @return The consent model.
     */
    protected JsonValue getConsentModel(ResourceOwnerConsentRequired consentRequired, OAuth2Request request) {
        final Reference resRef = getRequest().getResourceRef();
        final String query = resRef.getQuery();
        // Relative, exactly like the consent form's action: the caller resolves it against the URL it just
        // requested, which is what works behind a reverse proxy.
        final String target = StringUtils.isBlank(query) ? resRef.getPath() : resRef.getPath() + "?" + query;

        final JsonValue model = json(object(
                field("csrf", csrfProtection.createCsrfToken(request)),
                field("target", target),
                field("client_id", request.<String>getParameter(OAuth2Constants.Params.CLIENT_ID)),
                field("client_name", consentRequired.getClientName()),
                field("client_description", consentRequired.getClientDescription()),
                field("user_name", consentRequired.getUserDisplayName()),
                field("user_code", request.<String>getParameter(OAuth2Constants.DeviceCode.USER_CODE)),
                field("save_consent_enabled", consentRequired.isSaveConsentEnabled())));
        model.asMap().putAll(rawScopesAndClaims(consentRequired).asMap());
        return model;
    }

    /**
     * Builds the requested scopes and claims for the JSON consent representation. Unlike
     * {@link #addDisplayScopesAndClaims} the values are left raw: HTML encoding is a concern of the consent
     * page, and would corrupt the values for an API client.
     *
     * <p>Claims that belong to a requested scope are nested under it; the remainder are listed separately.
     * Values are rendered as text, matching what the consent page displays.</p>
     *
     * @param consentRequired The details for requesting consent.
     * @return An object holding a {@code scopes} and a {@code claims} array.
     */
    static JsonValue rawScopesAndClaims(ResourceOwnerConsentRequired consentRequired) {
        final JsonValue scopes = json(array());
        final Set<String> consumedClaims = new HashSet<>();
        final Map<String, List<String>> compositeScopes = consentRequired.getClaims().getCompositeScopes();
        final Map<String, String> claimDescriptions = consentRequired.getClaimDescriptions();
        final Map<String, Object> claimValues = new LinkedHashMap<>(consentRequired.getClaims().getValues());

        for (Map.Entry<String, String> scope : consentRequired.getScopeDescriptions().entrySet()) {
            final JsonValue claims = json(array());
            final List<String> scopeClaims = compositeScopes.get(scope.getKey());
            if (scopeClaims != null) {
                for (String claim : scopeClaims) {
                    if (claimValues.get(claim) != null) {
                        claims.add(rawClaim(claim, claimDescriptions, claimValues).getObject());
                        consumedClaims.add(claim);
                    }
                }
            }
            scopes.add(object(
                    field("name", scope.getKey()),
                    field("description", scope.getValue()),
                    field("claims", claims.getObject())));
        }

        claimValues.keySet().removeAll(consumedClaims);
        final JsonValue remainingClaims = json(array());
        for (String claim : claimValues.keySet()) {
            remainingClaims.add(rawClaim(claim, claimDescriptions, claimValues).getObject());
        }

        return json(object(field("scopes", scopes.getObject()), field("claims", remainingClaims.getObject())));
    }

    private static JsonValue rawClaim(String claim, Map<String, String> descriptions, Map<String, Object> values) {
        return json(object(
                field("name", claim),
                field("description", descriptions.get(claim)),
                field("value", values.get(claim).toString())));
    }

    private void addDisplayScopesAndClaims(ResourceOwnerConsentRequired consentRequired, Map<String, Object> data) {
        JsonValue scopes = json(array());
        List<String> scopeNames = new ArrayList<>();
        Set<String> allScopeClaims = new HashSet<>();
        final Map<String, List<String>> compositeScopes = consentRequired.getClaims().getCompositeScopes();
        final Map<String, String> claimDescriptions = consentRequired.getClaimDescriptions();
        final Map<String, Object> claimValues = new LinkedHashMap<>(consentRequired.getClaims().getValues());

        for (Map.Entry<String, String> scope : consentRequired.getScopeDescriptions().entrySet()) {
            if (scope.getValue() != null) {
                scopeNames.add(scope.getValue());
            }
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
        // Raw (unencoded) scope descriptions for the WAP consent template, which iterates them as a
        // FreeMarker sequence and applies ?html escaping at the template layer. Set after the raw query
        // parameters have been copied into the model so an attacker cannot supply display_scope themselves.
        data.put("display_scope", scopeNames);

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
