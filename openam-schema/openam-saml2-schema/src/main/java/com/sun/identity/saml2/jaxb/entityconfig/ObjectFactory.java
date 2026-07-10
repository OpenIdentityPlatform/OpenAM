package com.sun.identity.saml2.jaxb.entityconfig;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

/**
 * Fixed ObjectFactory for JAXB 4.x compatibility.
 *
 * The XJC-generated ObjectFactory (in target/generated-sources/jaxb/) maps all
 * config element factory methods to abstract BaseConfigType.class. JAXB 4.x
 * cannot instantiate an abstract class and throws InstantiationException.
 *
 * This file provides the correct concrete-type mappings.
 * It is copied over the generated ObjectFactory.java during the process-sources
 * Maven phase by the maven-antrun-plugin execution in pom.xml.
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _EntityConfig_QNAME =
        new QName("urn:sun:fm:SAML:2.0:entityconfig", "EntityConfig");
    private static final QName _IDPSSOConfig_QNAME =
        new QName("urn:sun:fm:SAML:2.0:entityconfig", "IDPSSOConfig");
    private static final QName _SPSSOConfig_QNAME =
        new QName("urn:sun:fm:SAML:2.0:entityconfig", "SPSSOConfig");
    private static final QName _AuthnAuthorityConfig_QNAME =
        new QName("urn:sun:fm:SAML:2.0:entityconfig", "AuthnAuthorityConfig");
    private static final QName _AttributeAuthorityConfig_QNAME =
        new QName("urn:sun:fm:SAML:2.0:entityconfig", "AttributeAuthorityConfig");
    private static final QName _AttributeQueryConfig_QNAME =
        new QName("urn:sun:fm:SAML:2.0:entityconfig", "AttributeQueryConfig");
    private static final QName _PDPConfig_QNAME =
        new QName("urn:sun:fm:SAML:2.0:entityconfig", "PDPConfig");
    private static final QName _XACMLPDPConfig_QNAME =
        new QName("urn:sun:fm:SAML:2.0:entityconfig", "XACMLPDPConfig");
    private static final QName _XACMLAuthzDecisionQueryConfig_QNAME =
        new QName("urn:sun:fm:SAML:2.0:entityconfig", "XACMLAuthzDecisionQueryConfig");
    private static final QName _AffiliationConfig_QNAME =
        new QName("urn:sun:fm:SAML:2.0:entityconfig", "AffiliationConfig");
    private static final QName _Attribute_QNAME =
        new QName("urn:sun:fm:SAML:2.0:entityconfig", "Attribute");
    private static final QName _Value_QNAME =
        new QName("urn:sun:fm:SAML:2.0:entityconfig", "Value");

    public ObjectFactory() {
    }

    public EntityConfigType createEntityConfigType() {
        return new EntityConfigType();
    }

    public AttributeType createAttributeType() {
        return new AttributeType();
    }

    // No-arg convenience factory methods for legacy callers (JAXB 1.x API compatibility)
    public EntityConfigElement createEntityConfigElement() { return new EntityConfigElement(); }
    public IDPSSOConfigElement createIDPSSOConfigElement() { return new IDPSSOConfigElement(); }
    public SPSSOConfigElement createSPSSOConfigElement() { return new SPSSOConfigElement(); }
    public AttributeAuthorityConfigElement createAttributeAuthorityConfigElement() { return new AttributeAuthorityConfigElement(); }
    public AuthnAuthorityConfigElement createAuthnAuthorityConfigElement() { return new AuthnAuthorityConfigElement(); }
    public AttributeQueryConfigElement createAttributeQueryConfigElement() { return new AttributeQueryConfigElement(); }
    public XACMLPDPConfigElement createXACMLPDPConfigElement() { return new XACMLPDPConfigElement(); }
    public XACMLAuthzDecisionQueryConfigElement createXACMLAuthzDecisionQueryConfigElement() { return new XACMLAuthzDecisionQueryConfigElement(); }
    /** Returns an {@link AttributeElement} typed as {@link AttributeType} for legacy compatibility. */
    public AttributeType createAttributeElement() { return new AttributeElement(); }

    // NOTE: @XmlElementDecl intentionally absent for EntityConfig.
    // In JAXB 4.x @XmlElementDecl takes precedence over @XmlRootElement for the
    // same QName, which would cause unmarshal to return JAXBElement<EntityConfigType>
    // instead of the concrete EntityConfigElement. Without this annotation JAXB uses
    // @XmlRootElement on EntityConfigElement and returns it directly — required for
    // the instanceof EntityConfigElement checks throughout SAML2MetaManager.
    public JAXBElement<EntityConfigElement> createEntityConfig(EntityConfigElement value) {
        return new JAXBElement<>(_EntityConfig_QNAME, EntityConfigElement.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:SAML:2.0:entityconfig", name = "IDPSSOConfig")
    public JAXBElement<IDPSSOConfigElement> createIDPSSOConfig(IDPSSOConfigElement value) {
        return new JAXBElement<>(_IDPSSOConfig_QNAME, IDPSSOConfigElement.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:SAML:2.0:entityconfig", name = "SPSSOConfig")
    public JAXBElement<SPSSOConfigElement> createSPSSOConfig(SPSSOConfigElement value) {
        return new JAXBElement<>(_SPSSOConfig_QNAME, SPSSOConfigElement.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:SAML:2.0:entityconfig", name = "AuthnAuthorityConfig")
    public JAXBElement<AuthnAuthorityConfigElement> createAuthnAuthorityConfig(AuthnAuthorityConfigElement value) {
        return new JAXBElement<>(_AuthnAuthorityConfig_QNAME, AuthnAuthorityConfigElement.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:SAML:2.0:entityconfig", name = "AttributeAuthorityConfig")
    public JAXBElement<AttributeAuthorityConfigElement> createAttributeAuthorityConfig(AttributeAuthorityConfigElement value) {
        return new JAXBElement<>(_AttributeAuthorityConfig_QNAME, AttributeAuthorityConfigElement.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:SAML:2.0:entityconfig", name = "AttributeQueryConfig")
    public JAXBElement<AttributeQueryConfigElement> createAttributeQueryConfig(AttributeQueryConfigElement value) {
        return new JAXBElement<>(_AttributeQueryConfig_QNAME, AttributeQueryConfigElement.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:SAML:2.0:entityconfig", name = "PDPConfig")
    public JAXBElement<PDPConfigElement> createPDPConfig(PDPConfigElement value) {
        return new JAXBElement<>(_PDPConfig_QNAME, PDPConfigElement.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:SAML:2.0:entityconfig", name = "XACMLPDPConfig")
    public JAXBElement<XACMLPDPConfigElement> createXACMLPDPConfig(XACMLPDPConfigElement value) {
        return new JAXBElement<>(_XACMLPDPConfig_QNAME, XACMLPDPConfigElement.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:SAML:2.0:entityconfig", name = "XACMLAuthzDecisionQueryConfig")
    public JAXBElement<XACMLAuthzDecisionQueryConfigElement> createXACMLAuthzDecisionQueryConfig(XACMLAuthzDecisionQueryConfigElement value) {
        return new JAXBElement<>(_XACMLAuthzDecisionQueryConfig_QNAME, XACMLAuthzDecisionQueryConfigElement.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:SAML:2.0:entityconfig", name = "AffiliationConfig")
    public JAXBElement<AffiliationConfigElement> createAffiliationConfig(AffiliationConfigElement value) {
        return new JAXBElement<>(_AffiliationConfig_QNAME, AffiliationConfigElement.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:SAML:2.0:entityconfig", name = "Attribute")
    public JAXBElement<AttributeType> createAttribute(AttributeType value) {
        return new JAXBElement<>(_Attribute_QNAME, AttributeType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:sun:fm:SAML:2.0:entityconfig", name = "Value")
    public JAXBElement<String> createValue(String value) {
        return new JAXBElement<>(_Value_QNAME, String.class, null, value);
    }
}
