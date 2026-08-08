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
 * Copyright 2026 3A Systems, LLC.
 */
package org.forgerock.openam.upgrade.steps.scripting;

import static java.util.Collections.singleton;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Set;

import org.forgerock.openam.sm.datalayer.api.ConnectionFactory;
import org.forgerock.openam.upgrade.UpgradeException;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

import com.iplanet.sso.SSOToken;
import com.sun.identity.shared.xml.XMLUtils;
import com.sun.identity.sm.ServiceConfig;

/**
 * Unit test to exercise the behaviour of {@link UpgradeScriptingSubConfigsStep}.
 */
public class UpgradeScriptingSubConfigsStepTest {

    private static final String EXISTING_CONTEXT = "OIDC_CLAIMS";
    private static final String EXISTING_SCRIPT_ID = "36863ffb-40ec-48b9-94b1-9a99f71cc3b5";
    private static final String NEW_CONTEXT = "OAUTH2_ACCESS_TOKEN_MODIFICATION";
    private static final String NEW_SCRIPT_ID = "d22f9a0c-426a-4466-b95e-d0f125b0d5fa";
    private static final String GLOBAL_SCRIPTS = "globalScripts";
    private static final String ENGINE_CONFIGURATION = "engineConfiguration";

    private UpgradeScriptingSubConfigsStep upgradeStep;

    private ServiceConfig globalConfig;
    private ServiceConfig existingContextConfig;
    private ServiceConfig existingEngineConfig;
    private ServiceConfig globalScriptsConfig;
    private ServiceConfig existingScriptConfig;
    private ServiceConfig createdContextConfig;

    @BeforeMethod
    public void setUp() throws Exception {
        globalConfig = mock(ServiceConfig.class);
        existingContextConfig = mock(ServiceConfig.class);
        existingEngineConfig = mock(ServiceConfig.class);
        globalScriptsConfig = mock(ServiceConfig.class);
        existingScriptConfig = mock(ServiceConfig.class);
        createdContextConfig = mock(ServiceConfig.class);

        when(globalConfig.getSubConfig(EXISTING_CONTEXT)).thenReturn(existingContextConfig);
        when(existingContextConfig.getSubConfig(ENGINE_CONFIGURATION)).thenReturn(existingEngineConfig);
        when(globalConfig.getSubConfig(GLOBAL_SCRIPTS)).thenReturn(globalScriptsConfig);
        when(globalScriptsConfig.getSubConfig(EXISTING_SCRIPT_ID)).thenReturn(existingScriptConfig);
        when(existingContextConfig.exists()).thenReturn(true);
        when(existingEngineConfig.exists()).thenReturn(true);
        when(globalScriptsConfig.exists()).thenReturn(true);
        when(existingScriptConfig.exists()).thenReturn(true);

        PrivilegedAction<SSOToken> adminTokenAction = mock(PrivilegedAction.class);
        ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
        upgradeStep = new SafeUpgradeScriptingSubConfigsStep(adminTokenAction, connectionFactory);
    }

    @Test
    public void addsMissingContextAndGlobalScript() throws Exception {
        // The new script context and its default global script do not exist yet; the context is only
        // resolvable after it has been created.
        when(globalConfig.getSubConfig(NEW_CONTEXT)).thenReturn(null, createdContextConfig);

        upgradeStep.initialize();

        assertThat(upgradeStep.isApplicable()).isTrue();
        assertThat(upgradeStep.getShortReport("-")).isEqualTo("New Scripting Service configurations (3)-");
        assertThat(upgradeStep.getDetailedReport("-"))
                .contains(NEW_CONTEXT)
                .contains(NEW_CONTEXT + "/" + ENGINE_CONFIGURATION)
                .contains(GLOBAL_SCRIPTS + "/" + NEW_SCRIPT_ID);

        upgradeStep.perform();

        ArgumentCaptor<Map<String, Set<String>>> contextAttributes = ArgumentCaptor.forClass(Map.class);
        verify(globalConfig).addSubConfig(eq(NEW_CONTEXT), eq("scriptContext"), eq(0), contextAttributes.capture());
        assertThat(contextAttributes.getValue()).containsEntry("defaultScript", singleton(NEW_SCRIPT_ID));

        // The service definition declares the new engine configuration as an empty element without an id,
        // so the id falls back to the name and the attribute map is empty.
        ArgumentCaptor<Map<String, Set<String>>> engineAttributes = ArgumentCaptor.forClass(Map.class);
        verify(createdContextConfig).addSubConfig(eq(ENGINE_CONFIGURATION), eq(ENGINE_CONFIGURATION), eq(0),
                engineAttributes.capture());
        assertThat(engineAttributes.getValue()).isEmpty();

        ArgumentCaptor<Map<String, Set<String>>> scriptAttributes = ArgumentCaptor.forClass(Map.class);
        verify(globalScriptsConfig).addSubConfig(eq(NEW_SCRIPT_ID), eq("globalScript"), eq(0),
                scriptAttributes.capture());
        assertThat(scriptAttributes.getValue())
                .containsEntry("name", singleton("OAuth2 Access Token Modification Script"))
                .containsEntry("context", singleton(NEW_CONTEXT))
                .containsEntry("language", singleton("GROOVY"))
                .containsEntry("script", singleton("// access token modification script"));

        verify(existingContextConfig, never()).addSubConfig(anyString(), anyString(), anyInt(), any(Map.class));
    }

    @Test
    public void addsEngineConfigurationWhenContextExistsWithoutIt() throws Exception {
        // For an absent entry whose name matches its sub-schema name (engineConfiguration), getSubConfig
        // returns a non-null config wrapping a non-existent SMSEntry instead of null, e.g. after a manual
        // ssoadm workaround created the context but not its engine configuration.
        when(existingEngineConfig.exists()).thenReturn(false);
        stubNewContextAndScriptAsExisting();

        upgradeStep.initialize();

        assertThat(upgradeStep.isApplicable()).isTrue();
        assertThat(upgradeStep.getShortReport("-")).isEqualTo("New Scripting Service configurations (1)-");
        assertThat(upgradeStep.getDetailedReport("-")).contains(EXISTING_CONTEXT + "/" + ENGINE_CONFIGURATION);

        upgradeStep.perform();

        ArgumentCaptor<Map<String, Set<String>>> engineAttributes = ArgumentCaptor.forClass(Map.class);
        verify(existingContextConfig).addSubConfig(eq(ENGINE_CONFIGURATION), eq(ENGINE_CONFIGURATION), eq(0),
                engineAttributes.capture());
        assertThat(engineAttributes.getValue()).containsEntry("whiteList", singleton("java.lang.String"));

        verify(globalConfig, never()).addSubConfig(anyString(), anyString(), anyInt(), any(Map.class));
        verify(globalScriptsConfig, never()).addSubConfig(anyString(), anyString(), anyInt(), any(Map.class));
    }

    @Test
    public void doesNothingWhenAllSubConfigsExist() throws Exception {
        ServiceConfig newContextConfig = stubNewContextAndScriptAsExisting();

        upgradeStep.initialize();

        assertThat(upgradeStep.isApplicable()).isFalse();
        assertThat(upgradeStep.getShortReport("-")).isEmpty();

        upgradeStep.perform();

        verify(globalConfig, never()).addSubConfig(anyString(), anyString(), anyInt(), any(Map.class));
        verify(globalScriptsConfig, never()).addSubConfig(anyString(), anyString(), anyInt(), any(Map.class));
        verify(newContextConfig, never()).addSubConfig(anyString(), anyString(), anyInt(), any(Map.class));
    }

    private ServiceConfig stubNewContextAndScriptAsExisting() throws Exception {
        ServiceConfig newContextConfig = mock(ServiceConfig.class);
        ServiceConfig newEngineConfig = mock(ServiceConfig.class);
        ServiceConfig newScriptConfig = mock(ServiceConfig.class);
        when(globalConfig.getSubConfig(NEW_CONTEXT)).thenReturn(newContextConfig);
        when(newContextConfig.getSubConfig(ENGINE_CONFIGURATION)).thenReturn(newEngineConfig);
        when(globalScriptsConfig.getSubConfig(NEW_SCRIPT_ID)).thenReturn(newScriptConfig);
        when(newContextConfig.exists()).thenReturn(true);
        when(newEngineConfig.exists()).thenReturn(true);
        when(newScriptConfig.exists()).thenReturn(true);
        return newContextConfig;
    }

    /**
     * Test class with the data store and service definition access mocked out, so to work against the test
     * xml and the mocked global configuration instead.
     */
    private final class SafeUpgradeScriptingSubConfigsStep extends UpgradeScriptingSubConfigsStep {

        SafeUpgradeScriptingSubConfigsStep(PrivilegedAction<SSOToken> adminTokenAction,
                ConnectionFactory connectionFactory) {
            super(adminTokenAction, connectionFactory);
        }

        @Override
        protected Document getScriptingServiceXML() throws UpgradeException {
            try {
                return XMLUtils.getXMLDocument(ClassLoader.getSystemResourceAsStream("test-scripting.xml"));
            } catch (Exception e) {
                throw new UpgradeException(e);
            }
        }

        @Override
        protected ServiceConfig getScriptingGlobalConfig() {
            return globalConfig;
        }
    }
}
