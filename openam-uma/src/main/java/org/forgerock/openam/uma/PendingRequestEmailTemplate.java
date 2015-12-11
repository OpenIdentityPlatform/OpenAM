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

import static org.forgerock.json.JsonValue.json;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import com.iplanet.sso.SSOException;
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

    private String resolveScope(String scope, Locale locale) {
        if (URI.create(scope).getScheme() != null) {
            Request request = new Request().setMethod("GET").setUri(URI.create(scope));
            request.getHeaders().put("Accept-Language", locale.toLanguageTag());
            Response response = client.send(request).getOrThrowUninterruptibly();
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
        return scope;
    }
}
