package com.sun.identity.saml2.jaxb.metadata;

import java.util.ArrayList;
import java.util.List;
import com.sun.identity.saml2.jaxb.assertion.AttributeType;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Hand-maintained replacement for the XJC-generated IDPSSODescriptorType.
 *
 * <p>Changes endpoint list fields to use concrete {@code *Element} subclasses
 * so that JAXB 4.x unmarshals the expected element instances rather than plain
 * {@code EndpointType} objects, preventing {@code ClassCastException} in
 * SAML2 metadata code.
 *
 * <p>Also overrides {@link #isWantAuthnRequestsSigned()} to return
 * {@code Boolean.FALSE} instead of {@code null} when the optional XML attribute
 * is absent, preventing {@code NullPointerException} from auto-unboxing in
 * {@code SPSSOFederate} and {@code IDPProxyUtil}.
 *
 * <p>This file shadows the XJC output; the XJC-generated copy is deleted from
 * {@code target/generated-sources/jaxb/} during the {@code process-sources}
 * phase by the {@code maven-antrun-plugin} execution in {@code pom.xml}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IDPSSODescriptorType", propOrder = {
    "singleSignOnService",
    "nameIDMappingService",
    "assertionIDRequestService",
    "attributeProfile",
    "attribute"
})
public class IDPSSODescriptorType extends SSODescriptorType {

    @XmlElement(name = "SingleSignOnService", required = true, type = SingleSignOnServiceElement.class)
    protected List<EndpointType> singleSignOnService;
    @XmlElement(name = "NameIDMappingService", type = NameIDMappingServiceElement.class)
    protected List<NameIDMappingServiceElement> nameIDMappingService;
    @XmlElement(name = "AssertionIDRequestService", type = AssertionIDRequestServiceElement.class)
    protected List<AssertionIDRequestServiceElement> assertionIDRequestService;
    @XmlElement(name = "AttributeProfile")
    @XmlSchemaType(name = "anyURI")
    protected List<String> attributeProfile;
    @XmlElement(name = "Attribute", namespace = "urn:oasis:names:tc:SAML:2.0:assertion")
    protected List<AttributeType> attribute;
    @XmlAttribute(name = "WantAuthnRequestsSigned")
    protected Boolean wantAuthnRequestsSigned;

    public List<EndpointType> getSingleSignOnService() {
        if (singleSignOnService == null) {
            singleSignOnService = new ArrayList<EndpointType>();
        }
        return this.singleSignOnService;
    }

    public List<NameIDMappingServiceElement> getNameIDMappingService() {
        if (nameIDMappingService == null) {
            nameIDMappingService = new ArrayList<NameIDMappingServiceElement>();
        }
        return this.nameIDMappingService;
    }

    public List<AssertionIDRequestServiceElement> getAssertionIDRequestService() {
        if (assertionIDRequestService == null) {
            assertionIDRequestService = new ArrayList<AssertionIDRequestServiceElement>();
        }
        return this.assertionIDRequestService;
    }

    public List<String> getAttributeProfile() {
        if (attributeProfile == null) {
            attributeProfile = new ArrayList<String>();
        }
        return this.attributeProfile;
    }

    public List<AttributeType> getAttribute() {
        if (attribute == null) {
            attribute = new ArrayList<AttributeType>();
        }
        return this.attribute;
    }

    /** Returns {@code false} (not {@code null}) when the attribute is absent. */
    public Boolean isWantAuthnRequestsSigned() {
        return (wantAuthnRequestsSigned != null) ? wantAuthnRequestsSigned : Boolean.FALSE;
    }

    public void setWantAuthnRequestsSigned(Boolean value) { this.wantAuthnRequestsSigned = value; }
}
