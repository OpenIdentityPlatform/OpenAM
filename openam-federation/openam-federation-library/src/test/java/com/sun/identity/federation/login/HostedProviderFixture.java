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

package com.sun.identity.federation.login;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.sun.identity.federation.common.IFSConstants;
import com.sun.identity.federation.jaxb.entityconfig.AttributeElement;
import com.sun.identity.federation.jaxb.entityconfig.AttributeType;
import com.sun.identity.federation.jaxb.entityconfig.BaseConfigType;
import com.sun.identity.federation.jaxb.entityconfig.SPDescriptorConfigElement;
import com.sun.identity.federation.meta.IDFFMetaManager;
import com.sun.identity.liberty.ws.meta.jaxb.SPDescriptorType;

/**
 * A hosted SP that resolves and configures a RelayState allow-list. Without one every
 * RelayState is refused because the extractor cannot read the configuration at all, and a
 * test asserting a refusal proves nothing about the guard it was written for.
 */
final class HostedProviderFixture {

    static final String META_ALIAS = "/idp";
    static final String ENTITY = "https://sp.example.com/idff";
    static final String ALLOWED_PATTERN = "https://trusted.example.com/*";
    static final String HOME_URL = "https://sp.example.com/home";

    static final String SECOND_ENTITY = "https://sp2.example.com/idff";
    static final String SECOND_ALLOWED_PATTERN = "https://trusted2.example.com/*";
    static final String SECOND_HOME_URL = "https://sp2.example.com/home";

    private HostedProviderFixture() {
    }

    /** A hosted SP that resolves but configures no RelayState allow-list at all. */
    static IDFFMetaManager providerWithoutAllowList() throws Exception {
        List<AttributeType> attributes = new ArrayList<AttributeType>();
        attributes.add(attribute(IFSConstants.PROVIDER_HOME_PAGE_URL, HOME_URL));

        SPDescriptorConfigElement config = spConfig(attributes, null);
        return metaManagerFor(config);
    }

    static IDFFMetaManager providerWithAllowList() throws Exception {
        List<AttributeType> attributes = new ArrayList<AttributeType>();
        attributes.add(attribute(IFSConstants.RELAY_STATE_URL_LIST, ALLOWED_PATTERN));
        attributes.add(attribute(IFSConstants.PROVIDER_HOME_PAGE_URL, HOME_URL));

        SPDescriptorConfigElement config = spConfig(attributes, null);
        return metaManagerFor(config);
    }

    /** The provider of {@link #providerWithAllowList}, with no home page to fall back to. */
    static IDFFMetaManager providerWithAllowListAndNoHomePage() throws Exception {
        List<AttributeType> attributes = new ArrayList<AttributeType>();
        attributes.add(attribute(IFSConstants.RELAY_STATE_URL_LIST, ALLOWED_PATTERN));

        SPDescriptorConfigElement config = spConfig(attributes, null);
        return metaManagerFor(config);
    }

    /**
     * The provider of {@link #providerWithAllowList}, with what {@code FSLoginHelper} also needs
     * to build it an AuthnRequest: an SP descriptor and a name identifier policy.
     */
    static IDFFMetaManager federatingProviderWithAllowList() throws Exception {
        return federatingProvider(new ArrayList<AttributeType>());
    }

    /**
     * The provider of {@link #federatingProviderWithAllowList}, with an SP adapter: the SP reads
     * the LogoutResponse an IdP returns only to hand it to one.
     */
    static IDFFMetaManager federatingProviderWithSPAdapter(Class<?> spAdapter) throws Exception {
        List<AttributeType> attributes = new ArrayList<AttributeType>();
        attributes.add(attribute(IFSConstants.FEDERATION_SP_ADAPTER, spAdapter.getName()));
        return federatingProvider(attributes);
    }

    /**
     * Another provider behind the same metaAlias, answering nothing the others answer: a meta
     * manager captured before this one was installed cannot produce any of its values. Its
     * configuration is keyed to its own entity alone, so asked about the first provider's
     * entity it has none.
     */
    static IDFFMetaManager secondFederatingProvider() throws Exception {
        List<AttributeType> attributes = new ArrayList<AttributeType>();
        attributes.add(attribute(IFSConstants.RELAY_STATE_URL_LIST, SECOND_ALLOWED_PATTERN));
        attributes.add(attribute(IFSConstants.PROVIDER_HOME_PAGE_URL, SECOND_HOME_URL));
        attributes.add(
            attribute(IFSConstants.NAMEID_POLICY, IFSConstants.NAME_ID_POLICY_FEDERATED));

        SPDescriptorConfigElement config = spConfig(attributes, META_ALIAS);
        IDFFMetaManager metaManager = mock(IDFFMetaManager.class);
        when(metaManager.getProviderRoleByMetaAlias(META_ALIAS)).thenReturn(IFSConstants.SP);
        when(metaManager.getEntityIDByMetaAlias(META_ALIAS)).thenReturn(SECOND_ENTITY);
        when(metaManager.getSPDescriptorConfig(nullable(String.class), eq(SECOND_ENTITY)))
            .thenReturn(config);
        when(metaManager.getSPDescriptor(nullable(String.class), nullable(String.class)))
            .thenReturn(mock(SPDescriptorType.class));
        return metaManager;
    }

    private static IDFFMetaManager federatingProvider(List<AttributeType> attributes)
        throws Exception {

        attributes.add(attribute(IFSConstants.RELAY_STATE_URL_LIST, ALLOWED_PATTERN));
        attributes.add(attribute(IFSConstants.PROVIDER_HOME_PAGE_URL, HOME_URL));
        attributes.add(
            attribute(IFSConstants.NAMEID_POLICY, IFSConstants.NAME_ID_POLICY_FEDERATED));

        SPDescriptorConfigElement config = spConfig(attributes, META_ALIAS);
        IDFFMetaManager metaManager = metaManagerFor(config);
        when(metaManager.getSPDescriptor(nullable(String.class), nullable(String.class)))
            .thenReturn(mock(SPDescriptorType.class));
        return metaManager;
    }

    /**
     * The extended metadata of a hosted SP holding these attributes, wrapped the way the meta
     * manager returns it.
     */
    private static SPDescriptorConfigElement spConfig(List<AttributeType> attributes,
        String metaAlias) {

        List<AttributeElement> elements = new ArrayList<AttributeElement>();
        for (AttributeType attribute : attributes) {
            elements.add(new AttributeElement(attribute));
        }
        BaseConfigType config = mock(BaseConfigType.class);
        when(config.getAttribute()).thenReturn(elements);
        when(config.getMetaAlias()).thenReturn(metaAlias);
        return new SPDescriptorConfigElement(config);
    }

    private static AttributeType attribute(String name, String value) {
        AttributeType attribute = mock(AttributeType.class);
        when(attribute.getName()).thenReturn(name);
        when(attribute.getValue()).thenReturn(new ArrayList<String>(Arrays.asList(value)));
        return attribute;
    }

    private static IDFFMetaManager metaManagerFor(SPDescriptorConfigElement config)
        throws Exception {

        IDFFMetaManager metaManager = mock(IDFFMetaManager.class);
        when(metaManager.getProviderRoleByMetaAlias(META_ALIAS)).thenReturn(IFSConstants.SP);
        when(metaManager.getEntityIDByMetaAlias(META_ALIAS)).thenReturn(ENTITY);
        when(metaManager.getSPDescriptorConfig(nullable(String.class), nullable(String.class)))
            .thenReturn(config);
        return metaManager;
    }
}
