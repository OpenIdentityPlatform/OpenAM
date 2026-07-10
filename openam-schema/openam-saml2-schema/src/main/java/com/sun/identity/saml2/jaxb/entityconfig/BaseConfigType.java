package com.sun.identity.saml2.jaxb.entityconfig;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Hand-maintained replacement for the XJC-generated BaseConfigType.
 *
 * The XJC-generated version declares this class as {@code abstract}.
 * JAXB 4.x cannot instantiate an abstract class and throws
 * {@code InstantiationException} when unmarshaling an {@code EntityConfig}
 * XML document that contains an {@code AffiliationConfig} element (which is
 * typed directly as {@code BaseConfigType} in the schema).
 *
 * Removing {@code abstract} makes the class concrete so JAXB 4.x can
 * instantiate it for that element while all concrete subclasses
 * ({@code SPSSOConfigElement}, {@code IDPSSOConfigElement}, etc.) continue
 * to extend this class unchanged.
 *
 * This file shadows the XJC output; the XJC-generated copy is deleted from
 * {@code target/generated-sources/jaxb/} during the {@code process-sources}
 * phase by the {@code maven-antrun-plugin} execution in {@code pom.xml}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "BaseConfigType", propOrder = {
    "attribute"
})
public class BaseConfigType {

    @XmlElement(name = "Attribute")
    protected List<AttributeType> attribute;
    @XmlAttribute(name = "metaAlias")
    protected String metaAlias;

    public List<AttributeType> getAttribute() {
        if (attribute == null) {
            attribute = new ArrayList<AttributeType>();
        }
        return this.attribute;
    }

    public String getMetaAlias() {
        return metaAlias;
    }

    public void setMetaAlias(String value) {
        this.metaAlias = value;
    }
}
