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
 * Copyright 2014 ForgeRock AS.
 * Portions copyright 2026 3A Systems LLC.
 */

package com.sun.identity.saml2.meta;

import static org.testng.Assert.*;

import com.sun.identity.saml2.jaxb.entityconfig.EntityConfigElement;
import com.sun.identity.saml2.jaxb.metadata.AssertionConsumerServiceElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorElement;
import com.sun.identity.saml2.jaxb.metadata.EntityDescriptorType;
import com.sun.identity.saml2.jaxb.metadata.SPSSODescriptorType;
import jakarta.xml.bind.JAXBElement;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SAML2MetaUtilsTest {
    
    private static final String PATH_SEPARATOR = "/";
    private static final String TEST_ENTITY = "someEntity";
    private static final String PREFIX = PATH_SEPARATOR + "abcdefg";
    private static final String TEST_SUB_REALM = "subsub";
    
    public SAML2MetaUtilsTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    @Test
    public void testGetMetaDataByURI_DefaultRealm() {
        final String uri = PREFIX 
                + PATH_SEPARATOR + SAML2MetaManager.NAME_META_ALIAS_IN_URI
                + PATH_SEPARATOR + TEST_ENTITY;
        final String result = SAML2MetaUtils.getMetaAliasByUri(uri);
        assertEquals(result, PATH_SEPARATOR + TEST_ENTITY);
    }
    
    @Test
    public void testGetMetaDataByURI_SubRealm() {
        final String uri = PREFIX 
                + PATH_SEPARATOR + SAML2MetaManager.NAME_META_ALIAS_IN_URI 
                + PATH_SEPARATOR + TEST_SUB_REALM 
                + PATH_SEPARATOR +  TEST_ENTITY;
        final String result = SAML2MetaUtils.getMetaAliasByUri(uri);
        assertEquals(result, PATH_SEPARATOR + TEST_SUB_REALM + PATH_SEPARATOR + TEST_ENTITY);        
    }

    @Test
    public void convertInputStreamToJaxbTest() throws Exception {
        try(InputStream is = getClass().getClassLoader().getResourceAsStream("idp-extended.xml")) {
            Object jaxb = SAML2MetaUtils.convertInputStreamToJAXB(is);
            assertNotNull(jaxb);
            assertTrue(jaxb instanceof EntityConfigElement);
        }

        try(InputStream is = getClass().getClassLoader().getResourceAsStream("idp-metadata.xml")) {
            Object jaxb = SAML2MetaUtils.convertInputStreamToJAXB(is);
            assertNotNull(jaxb);
            assertTrue(jaxb instanceof EntityDescriptorElement);
        }
    }

    /**
     * Metadata that omits the optional boolean attributes (WantAssertionsSigned,
     * AuthnRequestsSigned, AssertionConsumerService/@isDefault) must unmarshal
     * to nullable {@code Boolean} getters that return {@code null} rather than a
     * primitive {@code false}. This locks in the JAXB 3 generated shape so the
     * {@code Boolean.TRUE.equals(...)} guards on the SSO paths stay necessary
     * and correct (absent attribute is treated as {@code false}, never an NPE).
     */
    @Test
    public void optionalBooleanAttributesUnmarshalAsNull() throws Exception {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("sp-metadata-no-optional-booleans.xml")) {
            Object jaxb = SAML2MetaUtils.convertInputStreamToJAXB(is);
            assertTrue(jaxb instanceof EntityDescriptorElement);

            EntityDescriptorType entity = ((EntityDescriptorElement) jaxb).getValue();
            SPSSODescriptorType spsso = null;
            for (Object role : entity.getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor()) {
                Object value = (role instanceof JAXBElement) ? ((JAXBElement<?>) role).getValue() : role;
                if (value instanceof SPSSODescriptorType) {
                    spsso = (SPSSODescriptorType) value;
                    break;
                }
            }
            assertNotNull(spsso);

            // Absent optional attributes -> nullable Boolean returns null.
            assertNull(spsso.isWantAssertionsSigned());
            assertNull(spsso.isAuthnRequestsSigned());
            // Migration-safe idiom: null is treated as false, no unboxing NPE.
            assertFalse(Boolean.TRUE.equals(spsso.isWantAssertionsSigned()));
            assertFalse(Boolean.TRUE.equals(spsso.isAuthnRequestsSigned()));

            List<AssertionConsumerServiceElement> acsList = spsso.getAssertionConsumerService();
            assertFalse(acsList.isEmpty());
            for (AssertionConsumerServiceElement acs : acsList) {
                assertNull(acs.getValue().isIsDefault());
                assertFalse(Boolean.TRUE.equals(acs.getValue().isIsDefault()));
            }
        }
    }

}
