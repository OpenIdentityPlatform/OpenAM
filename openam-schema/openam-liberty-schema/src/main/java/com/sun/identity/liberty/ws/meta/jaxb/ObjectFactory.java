package com.sun.identity.liberty.ws.meta.jaxb;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

/**
 * Hand-maintained replacement for the XJC-generated ObjectFactory for the
 * {@code com.sun.identity.liberty.ws.meta.jaxb} package.
 *
 * <p>Adds the legacy {@code createSPDescriptorTypeAssertionConsumerServiceURLType()}
 * factory method that {@code IDFFModelImpl} calls (JAXB 1.x generated this method;
 * JAXB 4.x XJC generates {@code createSPDescriptorTypeAssertionConsumerServiceURL()}).
 *
 * <p>This file shadows the XJC output; the XJC-generated copy is deleted from
 * {@code target/generated-sources/jaxb/} during the {@code process-sources}
 * phase by the {@code maven-antrun-plugin} execution in {@code pom.xml}.
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _SPDescriptor_QNAME =
        new QName("urn:liberty:metadata:2003-08", "SPDescriptor");
    private static final QName _IDPDescriptor_QNAME =
        new QName("urn:liberty:metadata:2003-08", "IDPDescriptor");
    private static final QName _EntityDescriptor_QNAME =
        new QName("urn:liberty:metadata:2003-08", "EntityDescriptor");
    private static final QName _EntitiesDescriptor_QNAME =
        new QName("urn:liberty:metadata:2003-08", "EntitiesDescriptor");
    private static final QName _AffiliationDescriptor_QNAME =
        new QName("urn:liberty:metadata:2003-08", "AffiliationDescriptor");
    private static final QName _Organization_QNAME =
        new QName("urn:liberty:metadata:2003-08", "Organization");
    private static final QName _ContactPerson_QNAME =
        new QName("urn:liberty:metadata:2003-08", "ContactPerson");

    public ObjectFactory() {}

    public SPDescriptorType createSPDescriptorType() { return new SPDescriptorType(); }
    public IDPDescriptorType createIDPDescriptorType() { return new IDPDescriptorType(); }
    public EntityDescriptorType createEntityDescriptorType() { return new EntityDescriptorType(); }
    public EntitiesDescriptorType createEntitiesDescriptorType() { return new EntitiesDescriptorType(); }
    public AffiliationDescriptorType createAffiliationDescriptorType() { return new AffiliationDescriptorType(); }
    public OrganizationType createOrganizationType() { return new OrganizationType(); }
    public ContactType createContactType() { return new ContactType(); }
    public ProviderDescriptorType createProviderDescriptorType() { return new ProviderDescriptorType(); }
    public KeyDescriptorType createKeyDescriptorType() { return new KeyDescriptorType(); }

    /** JAXB 4.x name — returns inner-class {@link SPDescriptorType.AssertionConsumerServiceURL}. */
    public SPDescriptorType.AssertionConsumerServiceURL createSPDescriptorTypeAssertionConsumerServiceURL() {
        return new SPDescriptorType.AssertionConsumerServiceURL();
    }

    /**
     * Legacy JAXB 1.x method kept for binary / source compatibility.
     * Returns the JAXB 1.x inner-class alias
     * {@link SPDescriptorType.AssertionConsumerServiceURLType}.
     */
    public SPDescriptorType.AssertionConsumerServiceURLType createSPDescriptorTypeAssertionConsumerServiceURLType() {
        return new SPDescriptorType.AssertionConsumerServiceURLType();
    }

    @XmlElementDecl(namespace = "urn:liberty:metadata:2003-08", name = "SPDescriptor")
    public JAXBElement<SPDescriptorType> createSPDescriptor(SPDescriptorType value) {
        return new JAXBElement<>(_SPDescriptor_QNAME, SPDescriptorType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:metadata:2003-08", name = "IDPDescriptor")
    public JAXBElement<IDPDescriptorType> createIDPDescriptor(IDPDescriptorType value) {
        return new JAXBElement<>(_IDPDescriptor_QNAME, IDPDescriptorType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:metadata:2003-08", name = "EntityDescriptor")
    public JAXBElement<EntityDescriptorType> createEntityDescriptor(EntityDescriptorType value) {
        return new JAXBElement<>(_EntityDescriptor_QNAME, EntityDescriptorType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:metadata:2003-08", name = "EntitiesDescriptor")
    public JAXBElement<EntitiesDescriptorType> createEntitiesDescriptor(EntitiesDescriptorType value) {
        return new JAXBElement<>(_EntitiesDescriptor_QNAME, EntitiesDescriptorType.class, null, value);
    }
}
