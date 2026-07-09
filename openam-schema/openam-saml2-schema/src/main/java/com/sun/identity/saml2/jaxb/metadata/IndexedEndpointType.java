package com.sun.identity.saml2.jaxb.metadata;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Hand-maintained replacement for the XJC-generated IndexedEndpointType.
 *
 * <p>Overrides {@link #isIsDefault()} to return {@code Boolean.FALSE} (not
 * {@code null}) when the optional {@code isDefault} XML attribute is absent,
 * preventing {@code NullPointerException} from auto-unboxing at call sites in
 * {@code SPSSOFederate}, {@code IDPSSOUtil}, and {@code SPACSUtils}.
 *
 * <p>This file shadows the XJC output; the XJC-generated copy is deleted from
 * {@code target/generated-sources/jaxb/} during the {@code process-sources}
 * phase by the {@code maven-antrun-plugin} execution in {@code pom.xml}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "IndexedEndpointType")
public class IndexedEndpointType extends EndpointType {

    @XmlAttribute(name = "index", required = true)
    @XmlSchemaType(name = "unsignedShort")
    protected int index;
    @XmlAttribute(name = "isDefault")
    protected Boolean isDefault;

    public int getIndex() { return index; }
    public void setIndex(int value) { this.index = value; }

    /**
     * Returns {@code false} when the optional {@code isDefault} XML attribute
     * is absent (i.e., the field is {@code null}), preventing NPE from
     * auto-unboxing to {@code boolean} at call sites.
     */
    public Boolean isIsDefault() {
        return (isDefault != null) ? isDefault : Boolean.FALSE;
    }

    public void setIsDefault(Boolean value) { this.isDefault = value; }
}
