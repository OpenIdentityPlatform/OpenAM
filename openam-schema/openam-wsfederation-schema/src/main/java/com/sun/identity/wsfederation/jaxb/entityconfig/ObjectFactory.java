package com.sun.identity.wsfederation.jaxb.entityconfig;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

/**
 * Hand-maintained replacement for the XJC-generated ObjectFactory for the
 * {@code com.sun.identity.wsfederation.jaxb.entityconfig} package.
 *
 * <p>Adds the legacy no-arg {@code create*Element()} factory methods that JAXB
 * 1.x XJC generated but JAXB 4.x XJC no longer generates. Callers such as
 * {@code WSFedPropertiesModelImpl} use these factory methods.
 *
 * <p>This file shadows the XJC output; the XJC-generated copy is deleted from
 * {@code target/generated-sources/jaxb/} during the {@code process-sources}
 * phase by the {@code maven-antrun-plugin} execution in {@code pom.xml}.
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _FederationConfig_QNAME =
        new QName("urn:sun:fm:wsfederation:1.0:federationconfig", "FederationConfig");
    private static final QName _IDPSSOConfig_QNAME =
        new QName("urn:sun:fm:wsfederation:1.0:federationconfig", "IDPSSOConfig");
    private static final QName _SPSSOConfig_QNAME =
        new QName("urn:sun:fm:wsfederation:1.0:federationconfig", "SPSSOConfig");
    private static final QName _Attribute_QNAME =
        new QName("urn:sun:fm:wsfederation:1.0:federationconfig", "Attribute");
    private static final QName _Value_QNAME =
        new QName("urn:sun:fm:wsfederation:1.0:federationconfig", "Value");

    public ObjectFactory() {}

    public FederationConfigType createFederationConfigType() { return new FederationConfigType(); }
    public AttributeType createAttributeType() { return new AttributeType(); }

    // No-arg convenience factory methods for legacy callers (JAXB 1.x API compatibility)
    public FederationConfigElement createFederationConfigElement() { return new FederationConfigElement(); }
    public IDPSSOConfigElement createIDPSSOConfigElement() { return new IDPSSOConfigElement(); }
    public SPSSOConfigElement createSPSSOConfigElement() { return new SPSSOConfigElement(); }

    /** Returns an {@link AttributeElement} typed as {@link AttributeElement} for legacy compatibility. */
    public AttributeElement createAttributeElement() { return new AttributeElement(); }

    @XmlElementDecl(namespace = "urn:sun:fm:wsfederation:1.0:federationconfig", name = "FederationConfig")
    public JAXBElement<FederationConfigType> createFederationConfig(FederationConfigType value) {
        return new JAXBElement<>(_FederationConfig_QNAME, FederationConfigType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:wsfederation:1.0:federationconfig", name = "IDPSSOConfig")
    public JAXBElement<BaseConfigType> createIDPSSOConfig(BaseConfigType value) {
        return new JAXBElement<>(_IDPSSOConfig_QNAME, BaseConfigType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:wsfederation:1.0:federationconfig", name = "SPSSOConfig")
    public JAXBElement<BaseConfigType> createSPSSOConfig(BaseConfigType value) {
        return new JAXBElement<>(_SPSSOConfig_QNAME, BaseConfigType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:wsfederation:1.0:federationconfig", name = "Attribute")
    public JAXBElement<AttributeType> createAttribute(AttributeType value) {
        return new JAXBElement<>(_Attribute_QNAME, AttributeType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:wsfederation:1.0:federationconfig", name = "Value")
    public JAXBElement<String> createValue(String value) {
        return new JAXBElement<>(_Value_QNAME, String.class, null, value);
    }
}
