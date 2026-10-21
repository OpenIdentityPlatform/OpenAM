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

package org.openidentityplatform.openam.federation.plugins;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.forgerock.openam.shared.security.whitelist.ValidDomainExtractor;
import org.openidentityplatform.openam.federation.plugins.ValidRelayStateExtractor.ConfigurationUnavailableException;
import org.openidentityplatform.openam.federation.plugins.ValidRelayStateExtractor.IDFFEntityInfo;
import org.testng.annotations.Test;

import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.AttributeType;
import com.sun.identity.federation.jaxb.entityconfig.IDPDescriptorConfigElement;
import com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement;
import com.sun.identity.federation.meta.IDFFMetaException;
import com.sun.identity.federation.meta.IDFFMetaManager;

/**
 * The no-arg constructor resolves the meta manager through {@code FSUtils.getIDFFMetaManager()}
 * on every call rather than caching it, so a manager that was unavailable at construction time
 * recovers without a restart. That wiring needs a static stub this module's mockito cannot
 * provide; what is pinned here is the fail-closed contract it exists to serve.
 */
public class ValidRelayStateExtractorTest {

    private static final String REALM = "/";
    private static final String ENTITY = "https://sp.example.com/idff";

    @Test
    public void shouldReturnConfiguredRelayStateListForSpRole() throws Exception {
        List<AttributeType> attributes = attributeList(
            IFSConstants.RELAY_STATE_URL_LIST, "https://trusted.example.com/*");
        IDFFMetaManager metaManager = mock(IDFFMetaManager.class);
        SPDescriptorConfigElement config = mock(SPDescriptorConfigElement.class);
        when(config.getAttribute()).thenReturn(attributes);
        when(metaManager.getSPDescriptorConfig(REALM, ENTITY)).thenReturn(config);

        ValidDomainExtractor<IDFFEntityInfo> extractor = new ValidRelayStateExtractor(metaManager);

        assertThat(extractor.extractValidDomains(IDFFEntityInfo.from(REALM, ENTITY, IFSConstants.SP)))
            .containsExactly("https://trusted.example.com/*");
    }

    @Test
    public void shouldReadIdpDescriptorConfigForIdpRole() throws Exception {
        List<AttributeType> attributes = attributeList(
            IFSConstants.RELAY_STATE_URL_LIST, "https://idp-trusted.example.com/*");
        IDFFMetaManager metaManager = mock(IDFFMetaManager.class);
        IDPDescriptorConfigElement config = mock(IDPDescriptorConfigElement.class);
        when(config.getAttribute()).thenReturn(attributes);
        when(metaManager.getIDPDescriptorConfig(REALM, ENTITY)).thenReturn(config);

        ValidRelayStateExtractor extractor = new ValidRelayStateExtractor(metaManager);

        assertThat(extractor.extractValidDomains(IDFFEntityInfo.from(REALM, ENTITY, IFSConstants.IDP)))
            .containsExactly("https://idp-trusted.example.com/*");
        verify(metaManager).getIDPDescriptorConfig(REALM, ENTITY);
    }

    @Test
    public void shouldReturnNullWhenRelayStateAttributeAbsent() throws Exception {
        List<AttributeType> attributes = attributeList("someOtherAttribute", "value");
        IDFFMetaManager metaManager = mock(IDFFMetaManager.class);
        SPDescriptorConfigElement config = mock(SPDescriptorConfigElement.class);
        when(config.getAttribute()).thenReturn(attributes);
        when(metaManager.getSPDescriptorConfig(REALM, ENTITY)).thenReturn(config);

        ValidRelayStateExtractor extractor = new ValidRelayStateExtractor(metaManager);

        assertThat(extractor.extractValidDomains(IDFFEntityInfo.from(REALM, ENTITY, IFSConstants.SP)))
            .isNull();
    }

    @Test
    public void shouldFailClosedWhenConfigMissing() throws Exception {
        // The role resolved from the metaAlias but has no extended config: the metadata is
        // inconsistent, so an empty allow-list must not be inferred from it.
        IDFFMetaManager metaManager = mock(IDFFMetaManager.class);
        when(metaManager.getSPDescriptorConfig(REALM, ENTITY)).thenReturn(null);

        assertFailsClosed(new ValidRelayStateExtractor(metaManager));
    }

    /**
     * The value crosses an untyped {@code Map} from the JAXB layer. An unchecked cast let a
     * {@code ClassCastException} surface from inside {@code RedirectUrlValidator} as a bare
     * {@code RuntimeException}, losing the "could not read" / "not configured" distinction the
     * fail-closed design is built on.
     */
    @Test
    public void shouldFailClosedWhenRelayStateAttributeIsNotACollectionOfStrings()
        throws Exception {

        AttributeType attribute = mock(AttributeType.class);
        when(attribute.getName()).thenReturn(IFSConstants.RELAY_STATE_URL_LIST);
        when(attribute.getValue()).thenReturn(new ArrayList(Arrays.asList(new Object())));
        List<AttributeType> attributes = new ArrayList<>();
        attributes.add(attribute);
        IDFFMetaManager metaManager = mock(IDFFMetaManager.class);
        SPDescriptorConfigElement config = mock(SPDescriptorConfigElement.class);
        when(config.getAttribute()).thenReturn(attributes);
        when(metaManager.getSPDescriptorConfig(REALM, ENTITY)).thenReturn(config);

        assertFailsClosed(new ValidRelayStateExtractor(metaManager));
    }

    @Test
    public void shouldFailClosedWhenMetaManagerUnavailable() {
        assertFailsClosed(new ValidRelayStateExtractor((IDFFMetaManager) null));
    }

    @Test
    public void shouldFailClosedOnMetaException() throws Exception {
        IDFFMetaManager metaManager = mock(IDFFMetaManager.class);
        when(metaManager.getSPDescriptorConfig(REALM, ENTITY))
            .thenThrow(new IDFFMetaException("directory unavailable"));

        assertFailsClosed(new ValidRelayStateExtractor(metaManager));
    }

    @Test
    public void shouldFailClosedOnUncheckedException() throws Exception {
        // A directory hiccup surfaces as an unchecked exception from the SMS/JAXB layer just
        // as often as a checked one, and must not open the allow-list either.
        IDFFMetaManager metaManager = mock(IDFFMetaManager.class);
        when(metaManager.getSPDescriptorConfig(REALM, ENTITY))
            .thenThrow(new IllegalStateException("boom"));

        assertFailsClosed(new ValidRelayStateExtractor(metaManager));
    }

    @Test
    public void shouldResolveMetaManagerOnEveryCall() throws Exception {
        // A manager that was unavailable when the extractor was built must not be baked in:
        // FSUtils.getIDFFMetaManager() retries after a transient failure, so the extractor
        // has to ask again each call and recover without a JVM restart.
        List<AttributeType> attributes = attributeList(
            IFSConstants.RELAY_STATE_URL_LIST, "https://trusted.example.com/*");
        SPDescriptorConfigElement config = mock(SPDescriptorConfigElement.class);
        when(config.getAttribute()).thenReturn(attributes);
        IDFFMetaManager recovered = mock(IDFFMetaManager.class);
        when(recovered.getSPDescriptorConfig(REALM, ENTITY)).thenReturn(config);

        final AtomicInteger calls = new AtomicInteger();
        ValidRelayStateExtractor extractor = new ValidRelayStateExtractor(
            () -> calls.getAndIncrement() == 0 ? null : recovered);
        IDFFEntityInfo entity = IDFFEntityInfo.from(REALM, ENTITY, IFSConstants.SP);

        // first call: manager unavailable -> fail closed
        try {
            extractor.extractValidDomains(entity);
            failBecauseExceptionWasNotThrown(ConfigurationUnavailableException.class);
        } catch (ConfigurationUnavailableException expected) {
            // expected
        }

        // second call: manager now available -> the allow-list is read, no restart needed
        assertThat(extractor.extractValidDomains(entity))
            .containsExactly("https://trusted.example.com/*");
        assertThat(calls.get()).isEqualTo(2);
    }

    @Test
    public void shouldNotCacheDescriptorConfigBetweenCalls() throws Exception {
        List<AttributeType> attributes = attributeList(
            IFSConstants.RELAY_STATE_URL_LIST, "https://trusted.example.com/*");
        IDFFMetaManager metaManager = mock(IDFFMetaManager.class);
        SPDescriptorConfigElement config = mock(SPDescriptorConfigElement.class);
        when(config.getAttribute()).thenReturn(attributes);
        when(metaManager.getSPDescriptorConfig(REALM, ENTITY)).thenReturn(config);

        ValidRelayStateExtractor extractor = new ValidRelayStateExtractor(metaManager);
        IDFFEntityInfo entity = IDFFEntityInfo.from(REALM, ENTITY, IFSConstants.SP);

        extractor.extractValidDomains(entity);
        extractor.extractValidDomains(entity);

        verify(metaManager, times(2)).getSPDescriptorConfig(REALM, ENTITY);
    }

    private static void assertFailsClosed(ValidRelayStateExtractor extractor) {
        try {
            extractor.extractValidDomains(IDFFEntityInfo.from(REALM, ENTITY, IFSConstants.SP));
            failBecauseExceptionWasNotThrown(ConfigurationUnavailableException.class);
        } catch (ConfigurationUnavailableException expected) {
            // an unreadable allow-list is not an absent one
        }
    }

    /** Builds the raw attribute list that {@code IDFFMetaUtils.getAttributes} iterates over. */
    private static List<AttributeType> attributeList(String name, String... values) {
        AttributeType attribute = mock(AttributeType.class);
        when(attribute.getName()).thenReturn(name);
        when(attribute.getValue()).thenReturn(new ArrayList<>(Arrays.asList(values)));
        List<AttributeType> list = new ArrayList<>();
        list.add(attribute);
        return list;
    }
}
