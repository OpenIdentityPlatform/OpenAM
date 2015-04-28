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
package org.forgerock.openam.entitlement.conditions.environment;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.verify;
import static org.mockito.Mockito.mock;

import com.google.inject.Binder;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.entitlement.ConditionDecision;
import com.sun.identity.entitlement.EntitlementException;
import com.sun.identity.entitlement.util.AuthSPrincipal;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import org.forgerock.guice.core.GuiceTestCase;
import org.forgerock.guice.core.InjectorConfiguration;
import org.forgerock.http.client.RestletHttpClient;
import org.forgerock.openam.core.CoreWrapper;
import org.forgerock.openam.entitlement.PolicyConstants;
import org.forgerock.openam.scripting.ScriptConstants;
import org.forgerock.openam.scripting.ScriptConstants.ScriptContext;
import org.forgerock.openam.scripting.ScriptEvaluator;
import org.forgerock.openam.scripting.ScriptException;
import org.forgerock.openam.scripting.ScriptObject;
import org.forgerock.openam.scripting.SupportedScriptingLanguage;
import org.forgerock.openam.scripting.service.ScriptConfiguration;
import org.forgerock.openam.scripting.service.ScriptingService;
import org.forgerock.openam.scripting.service.ScriptingServiceFactory;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import javax.inject.Named;
import javax.script.Bindings;
import javax.security.auth.Subject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Unit test for script condition.
 *
 * @since 13.0.0
 */
public class ScriptConditionTest extends GuiceTestCase {

    private ScriptCondition scriptCondition;

    @Mock
    private ScriptingServiceFactory<ScriptConfiguration> scriptingServiceFactory;
    @Mock
    private ScriptingService<ScriptConfiguration> scriptingService;
    @Mock
    private ScriptEvaluator scriptEvaluator;
    @Mock
    private RestletHttpClient restletHttpClient;
    @Mock
    private CoreWrapper coreWrapper;
    @Captor
    private ArgumentCaptor<Bindings> bindingsCaptor;
    @Captor
    private ArgumentCaptor<ScriptObject> scriptObjectCaptor;

    @BeforeClass
    public void classUp() {
        // Setting an arbitrary annotation so that production modules are not loaded.
        InjectorConfiguration.setModuleAnnotation(Named.class);
    }

    @BeforeMethod
    @Override
    public void setupGuiceModules() throws Exception {
        MockitoAnnotations.initMocks(this);
        super.setupGuiceModules();

        scriptCondition = new ScriptCondition();
    }

    @Override
    public void configure(Binder binder) {
        binder
                .bind(new TypeLiteral<ScriptingServiceFactory<ScriptConfiguration>>() {})
                .toInstance(scriptingServiceFactory);

        binder
                .bind(ScriptEvaluator.class)
                .annotatedWith(Names
                        .named(ScriptConstants.ScriptContext.AUTHORIZATION_ENTITLEMENT_CONDITION.name()))
                .toInstance(scriptEvaluator);

        binder
                .bind(RestletHttpClient.class)
                .annotatedWith(Names
                        .named(SupportedScriptingLanguage.JAVASCRIPT.name()))
                .toInstance(restletHttpClient);

        binder
                .bind(CoreWrapper.class)
                .toInstance(coreWrapper);
    }

    @Test
    public void setValidState() {
        // When
        scriptCondition.setState("{ \"scriptId\": \"abc-def-ghi\" }");

        // Then
        assertThat(scriptCondition.getScriptId()).isEqualTo("abc-def-ghi");
    }

    @Test
    public void generatesCorrectJson() {
        // Given
        scriptCondition.setScriptId("abc-def-ghi");

        // When
        String json = scriptCondition.getState();

        // Then
        assertThat(json).isEqualTo("{ \"scriptId\": \"abc-def-ghi\" }");
    }

    @Test(expectedExceptions = EntitlementException.class,
            expectedExceptionsMessageRegExp = "Property value 'scriptId' should be defined.")
    public void invalidScriptIdValue() throws EntitlementException {
        // Given
        scriptCondition.setScriptId("");

        // When
        scriptCondition.validate();
    }

    @Test
    public void successfulEvaluation()
            throws EntitlementException, ScriptException, javax.script.ScriptException, IdRepoException, SSOException {
        // Given
        Subject subject = new Subject();
        SSOToken token = mock(SSOToken.class);
        subject.getPrivateCredentials().add(token);
        subject.getPrincipals().add(new AuthSPrincipal("user"));
        Map<String, Set<String>> env = new HashMap<>();

        given(scriptingServiceFactory.create(subject, "/abc")).willReturn(scriptingService);

        ScriptConfiguration configuration = ScriptConfiguration
                .builder()
                .setId("123-456-789")
                .setName("test-script")
                .setContext(ScriptContext.AUTHORIZATION_ENTITLEMENT_CONDITION)
                .setLanguage(SupportedScriptingLanguage.JAVASCRIPT)
                .setScript("some-script-here")
                .build();
        given(scriptingService.get("123-456-789")).willReturn(configuration);
        given(coreWrapper.getIdentity(token)).willReturn(mock(AMIdentity.class));

        // When
        scriptCondition.setScriptId("123-456-789");
        ConditionDecision decision = scriptCondition.evaluate("/abc", subject, "http://a:b/c", env);

        // Then
        assertThat(decision.isSatisfied()).isFalse(); // Hard to test true scenario
        verify(scriptEvaluator).evaluateScript(scriptObjectCaptor.capture(), bindingsCaptor.capture());

        ScriptObject scriptObject = scriptObjectCaptor.getValue();
        assertThat(scriptObject.getName()).isEqualTo("test-script");
        assertThat(scriptObject.getLanguage()).isEqualTo(SupportedScriptingLanguage.JAVASCRIPT);
        assertThat(scriptObject.getScript()).isEqualTo("some-script-here");

        Bindings bindings = bindingsCaptor.getValue();
        assertThat(bindings.get("logger")).isEqualTo(PolicyConstants.DEBUG);
        assertThat(bindings.get("username")).isEqualTo("user");
        assertThat(bindings.get("resourceURI")).isEqualTo("http://a:b/c");
        assertThat(bindings.get("environment")).isEqualTo(env);
        assertThat(bindings.get("httpClient")).isEqualTo(restletHttpClient);
        assertThat(bindings.get("authorised")).isEqualTo(Boolean.FALSE);
    }

    @Test(expectedExceptions = EntitlementException.class,
            expectedExceptionsMessageRegExp = "Script condition is unable to load script 123-456-789.")
    public void missingScriptConfiguration() throws ScriptException, EntitlementException {
        // Given
        Subject subject = new Subject();
        subject.getPrincipals().add(new AuthSPrincipal("user"));
        Map<String, Set<String>> env = new HashMap<>();

        given(scriptingServiceFactory.create(subject, "/abc")).willReturn(scriptingService);
        given(scriptingService.get("123-456-789")).willReturn(null);

        // When
        scriptCondition.setScriptId("123-456-789");
        scriptCondition.evaluate("/abc", subject, "http://a:b/c", env);
    }

}