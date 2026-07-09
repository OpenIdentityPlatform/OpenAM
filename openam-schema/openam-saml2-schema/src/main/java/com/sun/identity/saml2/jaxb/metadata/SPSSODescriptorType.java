package com.sun.identity.saml2.jaxb.metadata;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Hand-maintained replacement for the XJC-generated SPSSODescriptorType.
 *
 * <p>Changes {@code assertionConsumerService} from
 * {@code List<IndexedEndpointType>} to
 * {@code List<AssertionConsumerServiceElement>} so that JAXB 4.x unmarshals
 * concrete {@code AssertionConsumerServiceElement} instances instead of the
 * base {@code IndexedEndpointType}, preventing {@code ClassCastException} in
 * {@code SPSSOFederate.initiateAuthnRequest}.
 *
 * <p>Also overrides {@link #isAuthnRequestsSigned()} and
 * {@link #isWantAssertionsSigned()} to return {@code Boolean.FALSE} instead
 * of {@code null} when the optional XML attributes are absent, preventing
 * {@code NullPointerException} from auto-unboxing.
 *
 * <p>This file shadows the XJC output; the XJC-generated copy is deleted from
 * {@code target/generated-sources/jaxb/} during the {@code process-sources}
 * phase by the {@code maven-antrun-plugin} execution in {@code pom.xml}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SPSSODescriptorType", propOrder = {
    "assertionConsumerService",
    "attributeConsumingService"
})
public class SPSSODescriptorType extends SSODescriptorType {

    @XmlElement(name = "AssertionConsumerService", required = true, type = AssertionConsumerServiceElement.class)
    protected List<AssertionConsumerServiceElement> assertionConsumerService;
    @XmlElement(name = "AttributeConsumingService")
    protected List<AttributeConsumingServiceType> attributeConsumingService;
    @XmlAttribute(name = "AuthnRequestsSigned")
    protected Boolean authnRequestsSigned;
    @XmlAttribute(name = "WantAssertionsSigned")
    protected Boolean wantAssertionsSigned;

    public List<AssertionConsumerServiceElement> getAssertionConsumerService() {
        if (assertionConsumerService == null) {
            assertionConsumerService = new ArrayList<AssertionConsumerServiceElement>();
        }
        return this.assertionConsumerService;
    }

    public List<AttributeConsumingServiceType> getAttributeConsumingService() {
        if (attributeConsumingService == null) {
            attributeConsumingService = new ArrayList<AttributeConsumingServiceType>();
        }
        return this.attributeConsumingService;
    }

    /** Returns {@code false} (not {@code null}) when the attribute is absent. */
    public Boolean isAuthnRequestsSigned() {
        return (authnRequestsSigned != null) ? authnRequestsSigned : Boolean.FALSE;
    }

    public void setAuthnRequestsSigned(Boolean value) { this.authnRequestsSigned = value; }

    /** Returns {@code false} (not {@code null}) when the attribute is absent. */
    public Boolean isWantAssertionsSigned() {
        return (wantAssertionsSigned != null) ? wantAssertionsSigned : Boolean.FALSE;
    }

    public void setWantAssertionsSigned(Boolean value) { this.wantAssertionsSigned = value; }
}
