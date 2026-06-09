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

import static org.assertj.core.api.Assertions.assertThat;
import static org.forgerock.openam.utils.CollectionUtils.asSet;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.script.Bindings;
import javax.script.SimpleBindings;
import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.StandardScriptEngineManager;
import org.forgerock.openam.scripting.StandardScriptEvaluator;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.utils.IOUtils;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Tests the default OAuth2 Access Token Modification script
 * ({@code access-token-modification.groovy}) by evaluating it through the scripting engine,
 * mirroring {@link org.forgerock.openam.openidconnect.OidcClaimsExtensionTest} for the OIDC
 * claims script.
 */
public class AccessTokenModificationTest {

    private ScriptObject script;
    private Debug logger;
    private SSOToken ssoToken;
    private AMIdentity identity;

    private ScriptEvaluator scriptEvaluator;

    @BeforeClass
    public void setupScript() throws Exception {
        String rawScript = IOUtils.readStream(
                this.getClass().getClassLoader().getResourceAsStream("access-token-modification.groovy"));
        SupportedScriptingLanguage scriptType = SupportedScriptingLanguage.GROOVY;

        this.script = new ScriptObject("access-token-modification-script", rawScript, scriptType, null);

        StandardScriptEngineManager scriptEngineManager = new StandardScriptEngineManager();
        scriptEngineManager.registerEngineName(SupportedScriptingLanguage.GROOVY_ENGINE_NAME,
                new GroovyScriptEngineFactory());
        scriptEvaluator = new StandardScriptEvaluator(scriptEngineManager);
    }

    @BeforeMethod
    public void setup() throws Exception {
        this.logger = mock(Debug.class);
        this.ssoToken = mock(SSOToken.class);
        this.identity = mock(AMIdentity.class);
    }

    @Test
    public void propagatesAcrAndAmrFromContext() throws Exception {
        // Given
        Map<String, Object> context = new HashMap<>();
        context.put("acr", "urn:mace:incommon:iap:silver");
        context.put("amr", "pwd");
        ScriptableAccessToken accessToken = new ScriptableAccessToken(context);
        Bindings variables = testBindings(accessToken, asSet("openid", "profile"));

        // When
        scriptEvaluator.evaluateScript(script, variables);

        // Then
        assertThat(accessToken.getFields())
                .containsEntry("acr", "urn:mace:incommon:iap:silver")
                .containsEntry("amr", "pwd");
        assertThat(accessToken.getRemovedFields()).isEmpty();
    }

    @Test
    public void doesNotAddClaimsWhenAcrAndAmrAreAbsent() throws Exception {
        // Given
        ScriptableAccessToken accessToken = new ScriptableAccessToken(null);
        Bindings variables = testBindings(accessToken, asSet("openid"));

        // When
        scriptEvaluator.evaluateScript(script, variables);

        // Then
        assertThat(accessToken.getFields()).isEmpty();
        assertThat(accessToken.getRemovedFields()).isEmpty();
    }

    @Test
    public void logsExecutionWhenMessageLoggingEnabled() throws Exception {
        // Given
        when(logger.messageEnabled()).thenReturn(true);
        ScriptableAccessToken accessToken = new ScriptableAccessToken(null);
        Bindings variables = testBindings(accessToken, asSet("openid", "profile"));

        // When
        scriptEvaluator.evaluateScript(script, variables);

        // Then
        verify(logger).message(anyString());
    }

    @Test
    public void doesNotLogWhenMessageLoggingDisabled() throws Exception {
        // Given
        when(logger.messageEnabled()).thenReturn(false);
        ScriptableAccessToken accessToken = new ScriptableAccessToken(null);
        Bindings variables = testBindings(accessToken, asSet("openid"));

        // When
        scriptEvaluator.evaluateScript(script, variables);

        // Then
        verify(logger, never()).message(anyString());
    }

    private Bindings testBindings(ScriptableAccessToken accessToken, Set<String> scopes) {
        Map<String, Object> requestProperties = new HashMap<>();
        requestProperties.put("clientId", "myClient");
        requestProperties.put("realm", "/");
        requestProperties.put("grantType", "authorization_code");

        Bindings scriptVariables = new SimpleBindings();
        scriptVariables.put("logger", logger);
        scriptVariables.put("accessToken", accessToken);
        scriptVariables.put("session", ssoToken);
        scriptVariables.put("identity", identity);
        scriptVariables.put("scopes", scopes);
        scriptVariables.put("requestProperties", requestProperties);
        return scriptVariables;
    }
}

