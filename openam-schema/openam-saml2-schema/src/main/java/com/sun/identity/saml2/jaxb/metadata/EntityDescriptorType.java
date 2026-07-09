package com.sun.identity.saml2.jaxb.metadata;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import com.sun.identity.saml2.jaxb.metadataextquery.AttributeQueryDescriptorType;
import com.sun.identity.saml2.jaxb.xmlsig.SignatureType;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAnyAttribute;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Hand-maintained replacement for the XJC-generated EntityDescriptorType.
 *
 * <p>The XJC-generated version uses {@code @XmlElement(type = IDPSSODescriptorType.class)}
 * and {@code @XmlElement(type = SPSSODescriptorType.class)} in the
 * {@code @XmlElements} list for {@code roleDescriptorOrIDPSSODescriptorOrSPSSODescriptor}.
 * This causes JAXB 4.x to create plain {@code *Type} instances rather than the
 * concrete {@code *Element} subclasses, breaking the
 * {@code instanceof IDPSSODescriptorElement} / {@code instanceof SPSSODescriptorElement}
 * checks throughout {@code SAML2MetaUtils} and related code.
 *
 * <p>Fix: replace {@code type = *DescriptorType.class} with
 * {@code type = *DescriptorElement.class} in those entries.
 *
 * <p>This file shadows the XJC output; the XJC-generated copy is deleted from
 * {@code target/generated-sources/jaxb/} during the {@code process-sources}
 * phase by the {@code maven-antrun-plugin} execution in {@code pom.xml}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "EntityDescriptorType", propOrder = {
    "signature",
    "extensions",
    "roleDescriptorOrIDPSSODescriptorOrSPSSODescriptor",
    "affiliationDescriptor",
    "organization",
    "contactPerson",
    "additionalMetadataLocation"
})
public class EntityDescriptorType {

    @XmlElement(name = "Signature", namespace = "http://www.w3.org/2000/09/xmldsig#")
    protected SignatureType signature;
    @XmlElement(name = "Extensions")
    protected ExtensionsType extensions;
    @XmlElements({
        @XmlElement(name = "RoleDescriptor"),
        @XmlElement(name = "IDPSSODescriptor",    type = IDPSSODescriptorElement.class),
        @XmlElement(name = "SPSSODescriptor",     type = SPSSODescriptorElement.class),
        @XmlElement(name = "AuthnAuthorityDescriptor", type = AuthnAuthorityDescriptorElement.class),
        @XmlElement(name = "AttributeAuthorityDescriptor", type = AttributeAuthorityDescriptorElement.class),
        @XmlElement(name = "PDPDescriptor",       type = PDPDescriptorType.class),
        @XmlElement(name = "XACMLPDPDescriptor",  type = XACMLPDPDescriptorElement.class),
        @XmlElement(name = "QueryDescriptor",     type = QueryDescriptorType.class),
        @XmlElement(name = "XACMLAuthzDecisionQueryDescriptor", type = XACMLAuthzDecisionQueryDescriptorElement.class),
        @XmlElement(name = "AttributeQueryDescriptor",
                    namespace = "urn:oasis:names:tc:SAML:metadata:ext:query",
                    type = AttributeQueryDescriptorType.class)
    })
    protected List<RoleDescriptorType> roleDescriptorOrIDPSSODescriptorOrSPSSODescriptor;
    @XmlElement(name = "AffiliationDescriptor")
    protected AffiliationDescriptorType affiliationDescriptor;
    @XmlElement(name = "Organization")
    protected OrganizationType organization;
    @XmlElement(name = "ContactPerson")
    protected List<ContactType> contactPerson;
    @XmlElement(name = "AdditionalMetadataLocation")
    protected List<AdditionalMetadataLocationType> additionalMetadataLocation;
    @XmlAttribute(name = "entityID", required = true)
    @XmlSchemaType(name = "anyURI")
    protected String entityID;
    @XmlAttribute(name = "validUntil")
    @XmlSchemaType(name = "dateTime")
    protected XMLGregorianCalendar validUntil;
    @XmlAttribute(name = "cacheDuration")
    protected Duration cacheDuration;
    @XmlAttribute(name = "ID")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;
    @XmlAnyAttribute
    private Map<QName, String> otherAttributes = new HashMap<QName, String>();

    public SignatureType getSignature() { return signature; }
    public void setSignature(SignatureType value) { this.signature = value; }

    public ExtensionsType getExtensions() { return extensions; }
    public void setExtensions(ExtensionsType value) { this.extensions = value; }

    public List<RoleDescriptorType> getRoleDescriptorOrIDPSSODescriptorOrSPSSODescriptor() {
        if (roleDescriptorOrIDPSSODescriptorOrSPSSODescriptor == null) {
            roleDescriptorOrIDPSSODescriptorOrSPSSODescriptor = new ArrayList<RoleDescriptorType>();
        }
        return this.roleDescriptorOrIDPSSODescriptorOrSPSSODescriptor;
    }

    public AffiliationDescriptorType getAffiliationDescriptor() { return affiliationDescriptor; }
    public void setAffiliationDescriptor(AffiliationDescriptorType value) { this.affiliationDescriptor = value; }

    public OrganizationType getOrganization() { return organization; }
    public void setOrganization(OrganizationType value) { this.organization = value; }

    public List<ContactType> getContactPerson() {
        if (contactPerson == null) {
            contactPerson = new ArrayList<ContactType>();
        }
        return this.contactPerson;
    }

    public List<AdditionalMetadataLocationType> getAdditionalMetadataLocation() {
        if (additionalMetadataLocation == null) {
            additionalMetadataLocation = new ArrayList<AdditionalMetadataLocationType>();
        }
        return this.additionalMetadataLocation;
    }

    public String getEntityID() { return entityID; }
    public void setEntityID(String value) { this.entityID = value; }

    public XMLGregorianCalendar getValidUntil() { return validUntil; }
    public void setValidUntil(XMLGregorianCalendar value) { this.validUntil = value; }

    public Duration getCacheDuration() { return cacheDuration; }
    public void setCacheDuration(Duration value) { this.cacheDuration = value; }

    public String getID() { return id; }
    public void setID(String value) { this.id = value; }

    public Map<QName, String> getOtherAttributes() { return otherAttributes; }
}
