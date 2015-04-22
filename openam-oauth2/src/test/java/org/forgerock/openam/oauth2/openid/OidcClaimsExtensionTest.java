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

package org.forgerock.openam.oauth2.openid;

import static org.assertj.core.api.Assertions.*;
import static org.forgerock.json.fluent.JsonValue.*;
import static org.forgerock.openam.utils.CollectionUtils.*;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.script.Bindings;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

import org.codehaus.groovy.jsr223.GroovyScriptEngineFactory;
import org.forgerock.oauth2.core.AccessToken;
import org.forgerock.oauth2.core.OAuth2Constants;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.StandardScriptEngineManager;
import org.forgerock.openam.scripting.StandardScriptEvaluator;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.utils.IOUtils;
import org.slf4j.Logger;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.shared.debug.Debug;

public class OidcClaimsExtensionTest {

    private ScriptObject script;
    private Debug logger;
    private SSOToken ssoToken;
    private AMIdentity identity;
    private AccessToken accessToken;

    private ScriptEvaluator scriptEvaluator;

    @BeforeClass
    public void setupScript() throws Exception {
        String rawScript = IOUtils.readStream(this.getClass().getClassLoader().getResourceAsStream("oidc-claims-extension.groovy"));
        SupportedScriptingLanguage scriptType = SupportedScriptingLanguage.GROOVY;

        this.script = new ScriptObject("oidc-claims-script", rawScript, scriptType, null);

        StandardScriptEngineManager scriptEngineManager = new StandardScriptEngineManager();
        scriptEngineManager.registerEngineName(SupportedScriptingLanguage.GROOVY_ENGINE_NAME, new GroovyScriptEngineFactory());
        scriptEvaluator = new StandardScriptEvaluator(scriptEngineManager);
    }

    @BeforeMethod
    public void setup() throws Exception {
        this.logger = mock(Debug.class);
        this.ssoToken = mock(SSOToken.class);
        this.identity = mock(AMIdentity.class);
        this.accessToken = new AccessToken(json(object()), OAuth2Constants.Token.OAUTH_ACCESS_TOKEN, "id");
    }

    @Test
    public void testProfileScope() throws Exception {
        // Given
        Bindings variables = testBindings(asSet("profile"));
        when(identity.getAttribute("givenname")).thenReturn(asSet("joe"));
        when(identity.getAttribute("sn")).thenReturn(asSet("bloggs"));
        when(identity.getAttribute("preferredtimezone")).thenReturn(asSet("Europe/London"));
        when(identity.getAttribute("preferredlocale")).thenReturn(asSet("en"));
        when(identity.getAttribute("cn")).thenReturn(asSet("Joe Bloggs"));

        // When
        Map<String, Object> result = scriptEvaluator.evaluateScript(script, variables);

        // Then
        assertThat(result).containsOnly(entry("given_name", "joe"),
                entry("family_name", "bloggs"),
                entry("name", "Joe Bloggs"),
                entry("zoneinfo", "Europe/London"),
                entry("locale", "en"));
    }

    @Test
    public void testRequestedClaims() throws Exception {
        // Given
        Map<String, Set<String>> requestedClaims = new HashMap<String, Set<String>>();
        requestedClaims.put("given_name", asSet("fred"));
        requestedClaims.put("family_name", asSet("flintstone"));
        Bindings variables = testBindings(asSet("profile"), requestedClaims);
        when(identity.getAttribute("cn")).thenReturn(asSet("Joe Bloggs"));

        // When
        Map<String, Object> result = scriptEvaluator.evaluateScript(script, variables);

        // Then
        assertThat(result).containsOnly(
                entry("given_name", "fred"),
                entry("family_name", "flintstone"),
                entry("name", "Joe Bloggs"));

        verify(identity).getAttribute("cn");
        verify(identity).getAttribute("preferredlocale");
        verify(identity).getAttribute("preferredtimezone");
        verifyNoMoreInteractions(identity);
    }

    @Test
    public void testRequestedClaimsNoScope() throws Exception {
        // Given
        Map<String, Set<String>> requestedClaims = new HashMap<String, Set<String>>();
        requestedClaims.put("given_name", asSet("fred"));
        requestedClaims.put("family_name", asSet("flintstone"));
        Bindings variables = testBindings(asSet("openid"), requestedClaims);

        // When
        Map<String, Object> result = scriptEvaluator.evaluateScript(script, variables);

        // Then
        assertThat(result).containsOnly(
                entry("given_name", "fred"),
                entry("family_name", "flintstone"));
    }

    @Test
    public void testRequestedClaimsSelect() throws Exception {
        // Given
        Bindings variables = testBindings(asSet("profile"), Collections.singletonMap("given_name", asSet("fred", "george")));
        when(identity.getAttribute("cn")).thenReturn(asSet("Joe Bloggs"));

        // When/Then
        try {
            scriptEvaluator.evaluateScript(script, variables);
        } catch (Throwable e) {
            Throwable last = null;
            while (last != e) {
                if (e.getClass().equals(RuntimeException.class)) {
                    break;
                }
                last = e;
                e = e.getCause();
            }
            assertThat(e.getMessage()).isEqualTo("No selection logic for given_name defined. Values: [george, fred]");
        }
    }

    @Test
    public void testRequestedClaimsSelectNoScope() throws Exception {
        // Given
        Bindings variables = testBindings(asSet("openid"), Collections.singletonMap("given_name", asSet("fred", "george")));
        when(identity.getAttribute("cn")).thenReturn(asSet("Joe Bloggs"));

        // When/Then
        try {
            scriptEvaluator.evaluateScript(script, variables);
        } catch (Throwable e) {
            Throwable last = null;
            while (last != e) {
                if (e.getClass().equals(RuntimeException.class)) {
                    break;
                }
                last = e;
                e = e.getCause();
            }
            assertThat(e.getMessage()).isEqualTo("No selection logic for given_name defined. Values: [george, fred]");
        }
    }

    private Bindings testBindings(Set<String> scopes) {
        return testBindings(scopes, new HashMap<String, Set<Object>>());
    }
    private <T> Bindings testBindings(Set<String> scopes, Map<String, Set<T>> requestedClaims) {
        Bindings scriptVariables = new SimpleBindings();
        scriptVariables.put("logger", logger);
        scriptVariables.put("claims", new HashMap<String, Object>());
        scriptVariables.put("accessToken", accessToken);
        scriptVariables.put("session", ssoToken);
        scriptVariables.put("identity", identity);
        scriptVariables.put("scopes", scopes);
        scriptVariables.put("requestedClaims", requestedClaims);
        return scriptVariables;
    }
}
