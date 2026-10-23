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
import com.sun.identity.shared.xml.XMLUtils;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import org.testng.annotations.Test;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

    /**
     * Export rewrites query:AttributeQueryDescriptor into an unprefixed RoleDescriptor, which is a
     * metadata element only while the metadata namespace is the default namespace of the document.
     */
    @Test
    public void attributeQueryDescriptorSurvivesExportAndImport() throws Exception {
        String metadata = "<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\""
                + " entityID=\"https://sp.example.com/openam\">"
                + "<SPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
                + "<AssertionConsumerService index=\"0\" Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\""
                + " Location=\"https://sp.example.com/openam/Consumer/metaAlias/sp\"/>"
                + "</SPSSODescriptor>"
                + "<query:AttributeQueryDescriptor xmlns:query=\"urn:oasis:names:tc:SAML:metadata:ext:query\""
                + " protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
                + "<NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:persistent</NameIDFormat>"
                + "</query:AttributeQueryDescriptor>"
                + "</EntityDescriptor>";
        Object descriptor = SAML2MetaUtils.convertStringToJAXB(metadata);

        String exported = SAML2MetaUtils.workaroundAbstractRoleDescriptor(
                SAML2MetaUtils.convertJAXBToString(descriptor));

        Document doc = XMLUtils.toDOMDocument(exported, SAML2MetaUtils.debug);
        NodeList roles = doc.getElementsByTagNameNS("*", "RoleDescriptor");
        assertEquals(roles.getLength(), 1);
        assertEquals(roles.item(0).getNamespaceURI(), SAML2MetaConstants.NS_METADATA);
        EntityDescriptorElement imported = SAML2MetaUtils.getEntityDescriptorElement(exported);
        assertNotNull(SAML2MetaUtils.getAttributeQueryDescriptor(imported));
    }

    /**
     * The JAXB 1 runtime declared the query prefix on the AttributeQueryDescriptor element itself, and
     * declaring it a second time there would make the export malformed.
     */
    @Test
    public void exportKeepsQueryPrefixDeclaredOnTheElement() throws Exception {
        String exported = SAML2MetaUtils.workaroundAbstractRoleDescriptor(
                "<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"x\">"
                + "<query:AttributeQueryDescriptor xmlns:query=\"urn:oasis:names:tc:SAML:metadata:ext:query\""
                + " protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
                + "<NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:persistent</NameIDFormat>"
                + "</query:AttributeQueryDescriptor></EntityDescriptor>");

        Document doc = XMLUtils.toDOMDocument(exported, SAML2MetaUtils.debug);
        assertNotNull(doc);
        assertEquals(doc.getElementsByTagNameNS(SAML2MetaConstants.NS_METADATA, "RoleDescriptor").getLength(), 1);
    }

    /**
     * CreateFedlet.flipHostedParameter looks for "<EntityConfig " in the marshalled entity config,
     * so the root element must stay unprefixed.
     */
    @Test
    public void marshalledEntityConfigRootIsUnprefixed() throws Exception {
        Object config;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("idp-extended.xml")) {
            config = SAML2MetaUtils.convertInputStreamToJAXB(is);
        }
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        SAML2MetaUtils.convertJAXBToOutputStream(config, os);

        for (String xml : new String[] {SAML2MetaUtils.convertJAXBToString(config),
                os.toString(StandardCharsets.UTF_8.name())}) {
            Element root = XMLUtils.toDOMDocument(xml, SAML2MetaUtils.debug).getDocumentElement();
            assertNull(root.getPrefix());
            assertEquals(root.getLocalName(), "EntityConfig");
            assertTrue(xml.contains("<EntityConfig "));
        }
    }

    /**
     * The JAXB 1 runtime rejected metadata with an unexpected element; JAXB 3 drops the element
     * unless the unmarshaller fails on errors. Here the signing KeyDescriptor is in the wrong namespace.
     */
    @Test
    public void unexpectedElementIsRejected() throws Exception {
        final String metadata = "<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"x\">"
                + "<SPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
                + "<md:KeyDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata:wrong\" use=\"signing\"/>"
                + "<AssertionConsumerService index=\"0\" Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\""
                + " Location=\"https://sp.example.com/acs\"/>"
                + "</SPSSODescriptor></EntityDescriptor>";

        expectThrows(JAXBException.class, () -> SAML2MetaUtils.convertStringToJAXB(metadata));
        expectThrows(JAXBException.class, () -> SAML2MetaUtils.convertInputStreamToJAXB(
                new ByteArrayInputStream(metadata.getBytes(StandardCharsets.UTF_8))));
        expectThrows(JAXBException.class, () -> SAML2MetaUtils.convertNodeToJAXB(
                XMLUtils.toDOMDocument(metadata, SAML2MetaUtils.debug)));
    }

    /**
     * JAXB 3 keeps the whitespace between the children of mixed-content types such as KeyInfo and
     * EncryptionMethod, and the formatted marshaller indents around it again, so every save of the
     * metadata read back from the store would add blank lines.
     */
    @Test
    public void formattedMarshalIsStableAcrossSaves() throws Exception {
        String metadata;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("sp-metadata-encryption-keysize.xml")) {
            metadata = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        String saved = SAML2MetaUtils.convertJAXBToString(SAML2MetaUtils.convertStringToJAXB(metadata));
        String savedAgain = SAML2MetaUtils.convertJAXBToString(SAML2MetaUtils.convertStringToJAXB(saved));

        assertEquals(savedAgain, saved);
    }

    /**
     * JAXB 3 cannot instantiate the abstract RoleDescriptorType for an xsi:type it does not know, where
     * JAXB 1 ignored the xsi:type and the import then dropped the role. Partner metadata such as AD FS
     * metadata carries WS-Federation roles next to its SAML2 roles.
     */
    @Test
    public void roleDescriptorsOfUnknownTypeAreDroppedOnImport() throws Exception {
        String metadata = "<EntityDescriptor xmlns=\"urn:oasis:names:tc:SAML:2.0:metadata\""
                + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                + " xmlns:fed=\"http://docs.oasis-open.org/wsfed/federation/200706\""
                + " entityID=\"https://adfs.example.com/adfs/services/trust\">"
                + "<RoleDescriptor xsi:type=\"fed:ApplicationServiceType\""
                + " protocolSupportEnumeration=\"http://docs.oasis-open.org/wsfed/federation/200706\">"
                + "<KeyDescriptor use=\"signing\">"
                + "<KeyInfo xmlns=\"http://www.w3.org/2000/09/xmldsig#\"><KeyName>adfs</KeyName></KeyInfo>"
                + "</KeyDescriptor>"
                + "</RoleDescriptor>"
                + "<RoleDescriptor xsi:type=\"fed:SecurityTokenServiceType\""
                + " protocolSupportEnumeration=\"http://docs.oasis-open.org/wsfed/federation/200706\">"
                + "<fed:TokenTypesOffered><fed:TokenType Uri=\"urn:oasis:names:tc:SAML:2.0:assertion\"/>"
                + "</fed:TokenTypesOffered>"
                + "</RoleDescriptor>"
                + "<SPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
                + "<AssertionConsumerService index=\"0\" Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\""
                + " Location=\"https://adfs.example.com/adfs/ls/\"/>"
                + "</SPSSODescriptor>"
                + "<IDPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
                + "<SingleSignOnService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\""
                + " Location=\"https://adfs.example.com/adfs/ls/\"/>"
                + "</IDPSSODescriptor>"
                + "</EntityDescriptor>";

        EntityDescriptorElement descriptor = SAML2MetaUtils.getEntityDescriptorElement(metadata);

        assertNotNull(descriptor);
        assertEquals(descriptor.getValue().getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor().size(), 2);
        assertNotNull(SAML2MetaUtils.getSPSSODescriptor(descriptor));
        assertNotNull(SAML2MetaUtils.getIDPSSODescriptor(descriptor));
    }

    /**
     * The JAXB RI binds the default namespace to "" when its context holds an element in no namespace,
     * for example from a package added through com.sun.identity.liberty.ws.jaxb.packageList, and then
     * gives the metadata namespace a generated prefix. The exported RoleDescriptor must stay a metadata
     * element in that layout too.
     */
    @Test
    public void exportedRoleDescriptorIsMetadataElementUnderPrefixedRoot() throws Exception {
        String exported = SAML2MetaUtils.workaroundAbstractRoleDescriptor(
                "<ns8:EntityDescriptor xmlns:ns8=\"urn:oasis:names:tc:SAML:2.0:metadata\""
                + " xmlns:query=\"urn:oasis:names:tc:SAML:metadata:ext:query\" entityID=\"x\">"
                + "<query:AttributeQueryDescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">"
                + "<ns8:NameIDFormat>urn:oasis:names:tc:SAML:2.0:nameid-format:persistent</ns8:NameIDFormat>"
                + "</query:AttributeQueryDescriptor></ns8:EntityDescriptor>");

        Document doc = XMLUtils.toDOMDocument(exported, SAML2MetaUtils.debug);
        assertEquals(doc.getElementsByTagNameNS(SAML2MetaConstants.NS_METADATA, "RoleDescriptor").getLength(), 1);
        assertNotNull(SAML2MetaUtils.getAttributeQueryDescriptor(SAML2MetaUtils.getEntityDescriptorElement(exported)));
    }

}
