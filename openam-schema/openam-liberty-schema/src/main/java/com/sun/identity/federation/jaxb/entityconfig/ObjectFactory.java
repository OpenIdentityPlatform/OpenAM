package com.sun.identity.federation.jaxb.entityconfig;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

/**
 * Hand-maintained replacement for the XJC-generated ObjectFactory for the
 * {@code com.sun.identity.federation.jaxb.entityconfig} package.
 *
 * <p>Adds legacy no-arg {@code create*Element()} factory methods that JAXB 1.x
 * XJC generated but JAXB 4.x XJC no longer generates.  Callers such as
 * {@code IDFFModelImpl} use these methods to create config objects.
 *
 * <p>This file shadows the XJC output; the XJC-generated copy is deleted from
 * {@code target/generated-sources/jaxb/} during the {@code process-sources}
 * phase by the {@code maven-antrun-plugin} execution in {@code pom.xml}.
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _EntityConfig_QNAME =
        new QName("urn:sun:fm:ID-FF:entityconfig", "EntityConfig");
    private static final QName _IDPDescriptorConfig_QNAME =
        new QName("urn:sun:fm:ID-FF:entityconfig", "IDPDescriptorConfig");
    private static final QName _SPDescriptorConfig_QNAME =
        new QName("urn:sun:fm:ID-FF:entityconfig", "SPDescriptorConfig");
    private static final QName _AffiliationDescriptorConfig_QNAME =
        new QName("urn:sun:fm:ID-FF:entityconfig", "AffiliationDescriptorConfig");
    private static final QName _Attribute_QNAME =
        new QName("urn:sun:fm:ID-FF:entityconfig", "Attribute");
    private static final QName _Value_QNAME =
        new QName("urn:sun:fm:ID-FF:entityconfig", "Value");

    public ObjectFactory() {}

    public EntityConfigType createEntityConfigType() { return new EntityConfigType(); }
    public AttributeType createAttributeType() { return new AttributeType(); }

    // No-arg convenience factory methods for legacy callers (JAXB 1.x API compatibility)
    public EntityConfigElement createEntityConfigElement() { return new EntityConfigElement(); }
    public IDPDescriptorConfigElement createIDPDescriptorConfigElement() { return new IDPDescriptorConfigElement(); }
    public SPDescriptorConfigElement createSPDescriptorConfigElement() { return new SPDescriptorConfigElement(); }
    public AffiliationDescriptorConfigElement createAffiliationDescriptorConfigElement() { return new AffiliationDescriptorConfigElement(); }

    /** Returns an {@link AttributeElement} typed as {@link AttributeType} for legacy compatibility. */
    public AttributeType createAttributeElement() { return new AttributeElement(); }

    @XmlElementDecl(namespace = "urn:sun:fm:ID-FF:entityconfig", name = "EntityConfig")
    public JAXBElement<EntityConfigType> createEntityConfig(EntityConfigType value) {
        return new JAXBElement<>(_EntityConfig_QNAME, EntityConfigType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:ID-FF:entityconfig", name = "IDPDescriptorConfig")
    public JAXBElement<BaseConfigType> createIDPDescriptorConfig(BaseConfigType value) {
        return new JAXBElement<>(_IDPDescriptorConfig_QNAME, BaseConfigType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:ID-FF:entityconfig", name = "SPDescriptorConfig")
    public JAXBElement<BaseConfigType> createSPDescriptorConfig(BaseConfigType value) {
        return new JAXBElement<>(_SPDescriptorConfig_QNAME, BaseConfigType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:ID-FF:entityconfig", name = "AffiliationDescriptorConfig")
    public JAXBElement<BaseConfigType> createAffiliationDescriptorConfig(BaseConfigType value) {
        return new JAXBElement<>(_AffiliationDescriptorConfig_QNAME, BaseConfigType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:ID-FF:entityconfig", name = "Attribute")
    public JAXBElement<AttributeType> createAttribute(AttributeType value) {
        return new JAXBElement<>(_Attribute_QNAME, AttributeType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:ID-FF:entityconfig", name = "Value")
    public JAXBElement<String> createValue(String value) {
        return new JAXBElement<>(_Value_QNAME, String.class, null, value);
    }
}
