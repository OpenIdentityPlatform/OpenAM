package com.sun.identity.saml2.jaxb.entityconfig;

import java.util.ArrayList;
import java.util.List;
import com.sun.identity.saml2.jaxb.xmlsig.SignatureType;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Hand-maintained replacement for the XJC-generated EntityConfigType.
 *
 * <p>The XJC-generated version uses {@code @XmlElementRefs} with
 * {@code type = JAXBElement.class} for the role-config sub-elements:
 *
 * <pre>
 *   &#64;XmlElementRefs({
 *       &#64;XmlElementRef(name="IDPSSOConfig", ..., type=JAXBElement.class),
 *       &#64;XmlElementRef(name="SPSSOConfig",  ..., type=JAXBElement.class), ...
 *   })
 *   protected List&lt;JAXBElement&lt;BaseConfigType&gt;&gt; idpssoConfig...;
 * </pre>
 *
 * <p>In JAXB 4.x this causes the list to be populated with
 * {@code JAXBElement&lt;SPSSOConfigElement&gt;} wrappers rather than the
 * concrete element objects. {@code SAML2MetaManager} (written for JAXB 1.x)
 * casts list elements directly to {@code BaseConfigType} and checks
 * {@code instanceof SPSSOConfigElement / IDPSSOConfigElement}, both of which
 * fail when the items are JAXBElement wrappers.
 *
 * <p>Fix: replace {@code @XmlElementRefs} with {@code @XmlElements} referencing
 * the concrete {@code *Element} classes, and change the list type to
 * {@code List&lt;BaseConfigType&gt;}. JAXB 4.x then unmarshals each sub-element
 * into the correct concrete type with no wrapping.
 *
 * <p>This file shadows the XJC output; the XJC-generated copy is deleted from
 * {@code target/generated-sources/jaxb/} during the {@code process-sources}
 * phase by the {@code maven-antrun-plugin} execution in {@code pom.xml}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EntityConfigType", propOrder = {
    "signature",
    "attribute",
    "idpssoConfigOrSPSSOConfigOrAuthnAuthorityConfig",
    "affiliationConfig"
})
public class EntityConfigType {

    @XmlElement(name = "Signature", namespace = "http://www.w3.org/2000/09/xmldsig#")
    protected SignatureType signature;

    @XmlElement(name = "Attribute")
    protected List<AttributeType> attribute;

    /**
     * Replaced from {@code @XmlElementRefs / List<JAXBElement<BaseConfigType>>}
     * to {@code @XmlElements / List<BaseConfigType>} so JAXB 4.x returns
     * concrete element instances directly instead of JAXBElement wrappers.
     */
    @XmlElements({
        @XmlElement(name = "IDPSSOConfig",           namespace = "urn:sun:fm:SAML:2.0:entityconfig", type = IDPSSOConfigElement.class),
        @XmlElement(name = "SPSSOConfig",            namespace = "urn:sun:fm:SAML:2.0:entityconfig", type = SPSSOConfigElement.class),
        @XmlElement(name = "AuthnAuthorityConfig",   namespace = "urn:sun:fm:SAML:2.0:entityconfig", type = AuthnAuthorityConfigElement.class),
        @XmlElement(name = "AttributeAuthorityConfig", namespace = "urn:sun:fm:SAML:2.0:entityconfig", type = AttributeAuthorityConfigElement.class),
        @XmlElement(name = "AttributeQueryConfig",   namespace = "urn:sun:fm:SAML:2.0:entityconfig", type = AttributeQueryConfigElement.class),
        @XmlElement(name = "PDPConfig",              namespace = "urn:sun:fm:SAML:2.0:entityconfig", type = PDPConfigElement.class),
        @XmlElement(name = "XACMLPDPConfig",         namespace = "urn:sun:fm:SAML:2.0:entityconfig", type = XACMLPDPConfigElement.class),
        @XmlElement(name = "XACMLAuthzDecisionQueryConfig", namespace = "urn:sun:fm:SAML:2.0:entityconfig", type = XACMLAuthzDecisionQueryConfigElement.class)
    })
    protected List<BaseConfigType> idpssoConfigOrSPSSOConfigOrAuthnAuthorityConfig;

    @XmlElement(name = "AffiliationConfig")
    protected BaseConfigType affiliationConfig;

    @XmlAttribute(name = "entityID", required = true)
    @XmlSchemaType(name = "anyURI")
    protected String entityID;

    @XmlAttribute(name = "hosted")
    protected Boolean hosted;

    @XmlAttribute(name = "ID")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;

    public SignatureType getSignature() {
        return signature;
    }

    public void setSignature(SignatureType value) {
        this.signature = value;
    }

    public List<AttributeType> getAttribute() {
        if (attribute == null) {
            attribute = new ArrayList<AttributeType>();
        }
        return this.attribute;
    }

    public List<BaseConfigType> getIDPSSOConfigOrSPSSOConfigOrAuthnAuthorityConfig() {
        if (idpssoConfigOrSPSSOConfigOrAuthnAuthorityConfig == null) {
            idpssoConfigOrSPSSOConfigOrAuthnAuthorityConfig = new ArrayList<BaseConfigType>();
        }
        return this.idpssoConfigOrSPSSOConfigOrAuthnAuthorityConfig;
    }

    public BaseConfigType getAffiliationConfig() {
        return affiliationConfig;
    }

    public void setAffiliationConfig(BaseConfigType value) {
        this.affiliationConfig = value;
    }

    public String getEntityID() {
        return entityID;
    }

    public void setEntityID(String value) {
        this.entityID = value;
    }

    public Boolean isHosted() {
        return hosted;
    }

    public void setHosted(Boolean value) {
        this.hosted = value;
    }

    public String getID() {
        return id;
    }

    public void setID(String value) {
        this.id = value;
    }
}
