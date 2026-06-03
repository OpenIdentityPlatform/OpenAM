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
 * Copyright 2026 3A Systems LLC.
 */

package org.forgerock.openam.oauth2;

import static org.forgerock.openam.scripting.ScriptConstants.EMPTY_SCRIPT_SELECTION;
import static org.forgerock.openam.scripting.ScriptConstants.OAUTH2_ACCESS_TOKEN_MODIFICATION_NAME;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import javax.script.Bindings;
import javax.script.SimpleBindings;

import org.forgerock.oauth2.core.OAuth2Request;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.scripting.service.ScriptingServiceFactory;
import org.forgerock.openam.utils.OpenAMSettingsImpl;
import org.forgerock.openam.utils.StringUtils;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.SMSException;

/**
 * Evaluates the configured OAuth2 Access Token Modification script (script context
 * {@code OAUTH2_ACCESS_TOKEN_MODIFICATION}) and merges any additional claims into the stateless
 * JWT access/refresh token being issued.
 *
 * <p>The script is provided with the following bindings: {@code accessToken}, {@code scopes},
 * {@code identity}, {@code session}, {@code requestProperties} and {@code logger}.</p>
 */
@Singleton
public class OAuth2AccessTokenModifier {

    private final ScriptEvaluator scriptEvaluator;
    private final ScriptingServiceFactory scriptingServiceFactory;
    private final IdentityManager identityManager;
    private final Debug logger;

    /**
     * Constructs a new {@code OAuth2AccessTokenModifier}.
     *
     * @param scriptEvaluator The {@link ScriptEvaluator} bound to the access token modification context.
     * @param scriptingServiceFactory The {@link ScriptingServiceFactory} used to load script configurations.
     * @param identityManager The {@link IdentityManager} used to resolve the resource owner identity.
     * @param logger The OAuth2 provider debug logger.
     */
    @Inject
    public OAuth2AccessTokenModifier(
            @Named(OAUTH2_ACCESS_TOKEN_MODIFICATION_NAME) ScriptEvaluator scriptEvaluator,
            ScriptingServiceFactory scriptingServiceFactory,
            IdentityManager identityManager,
            @Named(OAuth2Constants.DEBUG_LOG_NAME) Debug logger) {
        this.scriptEvaluator = scriptEvaluator;
        this.scriptingServiceFactory = scriptingServiceFactory;
        this.identityManager = identityManager;
        this.logger = logger;
    }

    /**
     * Runs the configured OAuth2 Access Token Modification script (if any) and returns the additional
     * claims to be merged into the token being issued. If no script is configured, or the script
     * fails, an empty map is returned and the token is issued unmodified.
     *
     * @param request The current OAuth2 request.
     * @param realm The realm the token is issued in.
     * @param resourceOwnerId The resource owner id (subject) of the token.
     * @param clientId The client id (audience) of the token.
     * @param scope The scopes granted to the token.
     * @param contextValues Read-only context values exposed to the script via {@code accessToken.getField()}.
     * @return A map of additional claims to merge into the token. Never {@code null}.
     */
    public Map<String, Object> getModifiedClaims(OAuth2Request request, String realm, String resourceOwnerId,
            String clientId, Set<String> scope, Map<String, Object> contextValues) {

        Map<String, Object> result = new HashMap<>();
        try {
            ScriptObject script = getModificationScript(realm);
            if (script == null || StringUtils.isBlank(script.getScript())) {
                return result;
            }

            ScriptableAccessToken accessToken = new ScriptableAccessToken(contextValues);

            Bindings bindings = new SimpleBindings();
            bindings.put(OAuth2Constants.ScriptParams.ACCESS_TOKEN, accessToken);
            bindings.put(OAuth2Constants.ScriptParams.SCOPES,
                    scope == null ? new HashSet<String>() : new HashSet<>(scope));
            bindings.put(OAuth2Constants.ScriptParams.IDENTITY, getIdentity(resourceOwnerId, realm));
            bindings.put(OAuth2Constants.ScriptParams.SESSION, getSession(request));
            bindings.put(OAuth2Constants.ScriptParams.REQUEST_PROPERTIES,
                    buildRequestProperties(request, clientId, realm));
            bindings.put(OAuth2Constants.ScriptParams.LOGGER, logger);

            scriptEvaluator.evaluateScript(script, bindings);

            result.putAll(accessToken.getFields());
        } catch (Exception e) {
            logger.error("Error running OAuth2 access token modification script", e);
        }
        return result;
    }

    private ScriptObject getModificationScript(String realm) {
        OpenAMSettingsImpl settings = new OpenAMSettingsImpl(OAuth2Constants.OAuth2ProviderService.NAME,
                OAuth2Constants.OAuth2ProviderService.VERSION);
        try {
            String scriptId = settings.getStringSetting(realm,
                    OAuth2Constants.OAuth2ProviderService.ACCESS_TOKEN_MODIFICATION_SCRIPT);
            if (StringUtils.isBlank(scriptId) || EMPTY_SCRIPT_SELECTION.equals(scriptId)) {
                return null;
            }
            ScriptConfiguration config = scriptingServiceFactory.create(realm).get(scriptId);
            return new ScriptObject(config.getName(), config.getScript(), config.getLanguage());
        } catch (org.forgerock.openam.scripting.ScriptException | SSOException | SMSException e) {
            logger.message("OAuth2 access token modification script not configured or unavailable", e);
            return null;
        }
    }

    private AMIdentity getIdentity(String resourceOwnerId, String realm) {
        if (StringUtils.isBlank(resourceOwnerId)) {
            return null;
        }
        try {
            return identityManager.getResourceOwnerIdentity(resourceOwnerId, realm);
        } catch (Exception e) {
            logger.message("Unable to resolve resource owner identity for access token modification script", e);
            return null;
        }
    }

    private SSOToken getSession(OAuth2Request request) {
        if (request == null) {
            return null;
        }
        try {
            String sessionId = request.getSession();
            if (StringUtils.isNotBlank(sessionId)) {
                return SSOTokenManager.getInstance().createSSOToken(sessionId);
            }
        } catch (Exception e) {
            logger.message("Unable to resolve session for access token modification script", e);
        }
        return null;
    }

    private Map<String, Object> buildRequestProperties(OAuth2Request request, String clientId, String realm) {
        Map<String, Object> properties = new HashMap<>();
        properties.put("clientId", clientId);
        properties.put("realm", realm);
        if (request != null) {
            try {
                Object grantType = request.getParameter(OAuth2Constants.Params.GRANT_TYPE);
                if (grantType != null) {
                    properties.put("grantType", grantType);
                }
            } catch (Exception e) {
                // Ignore - request parameters are best-effort.
            }
        }
        return properties;
    }
}


