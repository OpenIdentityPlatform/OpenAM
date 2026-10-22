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
 * Portions copyright 2025-2026 3A Systems LLC.
 */

package org.forgerock.openam.uma;

import static org.forgerock.json.JsonValue.json;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import com.iplanet.am.util.SystemProperties;
import com.iplanet.sso.SSOException;
import com.sun.identity.common.SsrfUrlValidator;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;
import org.forgerock.http.Client;
import org.forgerock.http.protocol.Request;
import org.forgerock.http.protocol.Response;
import org.forgerock.http.protocol.Status;
import org.forgerock.json.JsonValue;
import org.forgerock.oauth2.core.exceptions.NotFoundException;
import org.forgerock.oauth2.core.exceptions.ServerException;
import org.forgerock.openam.utils.CollectionUtils;
import org.forgerock.openam.utils.OpenAMSettings;
import org.forgerock.util.Pair;

/**
 * A service for retrieving the email subject and body for the various pending
 * request email types, using the resource owner or requesting party's
 * preferred locale.
 *
 * @since 13.0.0
 */
@Singleton
public class PendingRequestEmailTemplate {

    /**
     * When set to {@code true}, a scope description URI may resolve to a loopback, private or
     * link-local address. The {@code http}/{@code https} scheme allowlist still applies. Default
     * {@code false}.
     */
    public static final String ALLOW_ANY_SCOPE_DESCRIPTION_ADDRESS =
            "org.openidentityplatform.uma.scope-description.allow-any-address";

    private final Debug debug = Debug.getInstance("UmaProvider");
    private final UmaProviderSettingsFactory settingsFactory;
    private final Client client;
    private final OpenAMSettings authServiceSettings;

    @Inject
    public PendingRequestEmailTemplate(UmaProviderSettingsFactory settingsFactory, @Named("UMA") Client client,
            @Named("iPlanetAMAuthService") OpenAMSettings authServiceSettings) {
        this.settingsFactory = settingsFactory;
        this.client = client;
        this.authServiceSettings = authServiceSettings;
    }

    /**
     * Gets the subject and body for the Pending Request creation email.
     *
     * <p>The body contains 5 arguments:
     * <ul>
     *     <li>The requesting party username</li>
     *     <li>The resource set name</li>
     *     <li>The requested scopes</li>
     *     <li>The AM base URL</li>
     *     <li>The pending request ID</li>
     * </ul></p>
     *
     * @param resourceOwnerId The resource owner username.
     * @param realm The realm.
     * @return A {@code Pair} containing the subject and body of the email template.
     */
    Pair<String, String> getCreationTemplate(String resourceOwnerId, String realm) {
        ResourceBundle resourceBundle = getResourceBundle(resourceOwnerId, realm);
        return Pair.of(resourceBundle.getString("UmaPendingRequestCreationEmailSubject"),
                resourceBundle.getString("UmaPendingRequestCreationEmailTemplate"));
    }

    /**
     * Gets the subject and body for the Pending Request approval email.
     *
     * <p>The body contains 3 arguments:
     * <ul>
     *     <li>The resource owner username</li>
     *     <li>The resource set name</li>
     *     <li>The requested scopes</li>
     * </ul></p>
     *
     * @param requestingPartyId The resource owner username.
     * @param realm The realm.
     * @return A {@code Pair} containing the subject and body of the email template.
     */
    Pair<String, String> getApprovalTemplate(String requestingPartyId, String realm) {
        ResourceBundle resourceBundle = getResourceBundle(requestingPartyId, realm);
        return Pair.of(resourceBundle.getString("UmaPendingRequestApprovalEmailSubject"),
                resourceBundle.getString("UmaPendingRequestApprovalEmailTemplate"));
    }

    private ResourceBundle getResourceBundle(String username, String realm) {
        return ResourceBundle.getBundle("UmaProvider", getLocale(username, realm));
    }

    private Locale getLocale(String username, String realm) {
        try {
            String localeAttributeName = settingsFactory.get(realm).getUserProfilePreferredLocaleAttribute();
            if (localeAttributeName != null) {
                AMIdentity identity = IdUtils.getIdentity(username, realm);
                @SuppressWarnings("unchecked")
                Set<String> localeAttribute = identity.getAttribute(localeAttributeName);
                if (localeAttribute != null && !localeAttribute.isEmpty()) {
                    return Locale.forLanguageTag(CollectionUtils.getFirstItem(localeAttribute, ""));
                }
            }
            String defaultLocale = authServiceSettings.getStringSetting(realm, "iplanet-am-auth-locale");
            if (defaultLocale != null) {
                return Locale.forLanguageTag(defaultLocale);
            }
        } catch (SSOException | IdRepoException | ServerException | SMSException | NotFoundException e) {
            debug.warning("Failed to get locale for user, " + username + ", in realm, " + realm, e);
        }
        return Locale.ROOT;
    }

    String buildScopeString(Collection<String> scopes, String username, String realm) {
        Locale locale = getLocale(username, realm);
        StringBuilder sb = new StringBuilder();
        for (String scope : scopes) {
            sb.append(resolveScope(scope, locale)).append(", ");
        }
        if (scopes.isEmpty()) {
            return "NONE";
        } else {
            return sb.substring(0, sb.length() - 2);
        }
    }

    /**
     * Whether a scope that is a URI may be fetched to resolve its scope description
     * (GHSA-7q42-6xr7-9rmw).
     *
     * <p>Scopes are chosen by whoever registers the resource set, so the URI must use {@code http}
     * or {@code https} and must not resolve to a loopback, private, link-local or otherwise
     * special-purpose address, as decided by {@link SsrfUrlValidator}. Redirects need no check of
     * their own: the {@code UMA} client is built with redirect handling disabled.
     *
     * <p>The host is resolved here and resolved again when the client connects, so the {@code UMA}
     * client ({@link ScopeDescriptionClientProvider}) repeats the address check in its own DNS
     * resolver, on the addresses it connects to; a host whose DNS answer changes in between (DNS
     * rebinding) is refused there. With the system proxy enabled only this check applies, also to
     * hosts that bypass the proxy (e.g. {@code http.nonProxyHosts}).
     *
     * <p>The address check — never the scheme check — can be lifted with
     * {@link #ALLOW_ANY_SCOPE_DESCRIPTION_ADDRESS} for resource servers that publish their scope
     * descriptions on an internal address.
     *
     * @param uri the scope, parsed as a URI with a scheme.
     * @return {@code true} if the scope description may be fetched.
     */
    static boolean isScopeDescriptionUriAllowed(URI uri) {
        final String scheme = uri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
            return false;
        }
        return isAnyScopeDescriptionAddressAllowed() || SsrfUrlValidator.isSafeRemoteUrl(uri.toString());
    }

    /**
     * @return {@code true} if {@link #ALLOW_ANY_SCOPE_DESCRIPTION_ADDRESS} lifts the address check.
     */
    static boolean isAnyScopeDescriptionAddressAllowed() {
        return SystemProperties.getAsBoolean(ALLOW_ANY_SCOPE_DESCRIPTION_ADDRESS, false);
    }

    private String resolveScope(String scope, Locale locale) {
        final URI uri;
        try {
            uri = URI.create(scope);
        } catch (IllegalArgumentException e) {
            // A plain scope name such as "read write" is not a URI; it is shown as it is.
            return scope;
        }
        if (uri.getScheme() != null) {
            if (!isScopeDescriptionUriAllowed(uri)) {
                debug.warning("Not fetching the scope description at " + scope + ": it does not use http or "
                        + "https, or it resolves to a private, loopback or link-local address. Set "
                        + ALLOW_ANY_SCOPE_DESCRIPTION_ADDRESS + "=true if the resource server is intentionally "
                        + "on an internal address.");
                return scope;
            }
            Request request = new Request().setMethod("GET").setUri(uri);
            request.getHeaders().put("Accept-Language", locale.toLanguageTag());
            // Closing the response returns its connection to the client's pool; reading the JSON
            // does not, as it stops at the end of the value and leaves the stream open.
            try (Response response = client.send(request).getOrThrowUninterruptibly()) {
                if (Status.OK.equals(response.getStatus())) {
                    try {
                        JsonValue json = json(response.getEntity().getJson());
                        if (json.isDefined("name") && json.get("name").isString()) {
                            return json.get("name").asString();
                        }
                    } catch (IOException e) {
                        debug.warning("Failed to parse Scope description JSON", e);
                    }
                }
            }
        }
        return scope;
    }
}
