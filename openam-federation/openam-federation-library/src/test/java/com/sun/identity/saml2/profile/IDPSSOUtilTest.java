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
package com.sun.identity.saml2.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.sun.identity.saml2.common.SAML2Constants;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.ObjectFactory;
import com.sun.identity.saml2.jaxb.metadata.RoleDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import com.sun.identity.saml2.meta.SAML2MetaManager;
import com.sun.identity.saml2.meta.SAML2MetaUtils;
import jakarta.xml.bind.JAXBElement;
import java.io.InputStream;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * The SP metadata used here omits {@code AssertionConsumerService/@isDefault}, which JAXB 3 maps to a
 * nullable {@code Boolean}. The ACS lookups must fall back to the first endpoint instead of unboxing null.
 */
@Test
public class IDPSSOUtilTest {

    private static final String REALM = "/";
    private static final String SP_ENTITY_ID = "https://sp.example.com/no-optional-booleans";
    private static final String FIRST_ACS_URL = "https://sp.example.com/AssertionConsumer/metaAlias/sp";

    private SAML2MetaManager originalMetaManager;

    @BeforeMethod
    public void setUp() throws Exception {
        originalMetaManager = IDPSSOUtil.metaManager;
        SAML2MetaManager metaManager = mock(SAML2MetaManager.class);
        when(metaManager.getSPSSODescriptor(REALM, SP_ENTITY_ID)).thenReturn(loadSPSSODescriptor());
        IDPSSOUtil.metaManager = metaManager;
    }

    @AfterMethod
    public void tearDown() {
        IDPSSOUtil.metaManager = originalMetaManager;
    }

    public void defaultACSurlFallsBackToFirstEndpointWhenIsDefaultIsAbsent() throws Exception {
        StringBuffer binding = new StringBuffer();

        String acsURL = IDPSSOUtil.getDefaultACSurl(SP_ENTITY_ID, REALM, binding);

        assertThat(acsURL).isEqualTo(FIRST_ACS_URL);
        assertThat(binding.toString()).isEqualTo(SAML2Constants.HTTP_POST);
    }

    public void acsURLByUnknownBindingFallsBackToFirstEndpointWhenIsDefaultIsAbsent() throws Exception {
        StringBuffer binding = new StringBuffer();

        String acsURL = IDPSSOUtil.getACSurlFromMetaByBinding(SP_ENTITY_ID, REALM, SAML2Constants.PAOS, binding);

        assertThat(acsURL).isEqualTo(FIRST_ACS_URL);
        assertThat(binding.toString()).isEqualTo(SAML2Constants.HTTP_POST);
    }

    public void acsURLByUnknownIndexFallsBackToFirstEndpointWhenIsDefaultIsAbsent() throws Exception {
        StringBuffer binding = new StringBuffer();

        String acsURL = IDPSSOUtil.getACSurlFromMetaByIndex(SP_ENTITY_ID, REALM, 5, binding);

        assertThat(acsURL).isEqualTo(FIRST_ACS_URL);
        assertThat(binding.toString()).isEqualTo(SAML2Constants.HTTP_POST);
    }

    private SPSSODescriptorElement loadSPSSODescriptor() throws Exception {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("sp-metadata-no-optional-booleans.xml")) {
            EntityDescriptorElement entity = (EntityDescriptorElement) SAML2MetaUtils.convertInputStreamToJAXB(is);
            for (JAXBElement<? extends RoleDescriptorType> role
                    : entity.getValue().getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor()) {
                if (role.getValue() instanceof SPSSODescriptorType) {
                    return new ObjectFactory().createSPSSODescriptorElement((SPSSODescriptorType) role.getValue());
                }
            }
        }
        throw new AssertionError("No SPSSODescriptor in sp-metadata-no-optional-booleans.xml");
    }
}
