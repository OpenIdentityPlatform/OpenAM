package com.sun.identity.liberty.ws.meta.jaxb;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlID;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlType;
import jakarta.xml.bind.annotation.XmlValue;
import jakarta.xml.bind.annotation.adapters.CollapsedStringAdapter;
import jakarta.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 * Hand-maintained replacement for the XJC-generated SPDescriptorType.
 *
 * <p>This is a copy of the XJC output with one addition: the inner class
 * {@link AssertionConsumerServiceURLType} which is a JAXB 1.x alias for the
 * JAXB 4.x inner class {@link AssertionConsumerServiceURL}. Legacy callers in
 * {@code IDFFModelImpl} reference<br>
 * {@code SPDescriptorType.AssertionConsumerServiceURLType} and cast list
 * elements to it — this class restores that API.
 *
 * <p>This file shadows the XJC output; the XJC-generated copy is deleted from
 * {@code target/generated-sources/jaxb/} during the {@code process-sources}
 * phase by the {@code maven-antrun-plugin} execution in {@code pom.xml}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SPDescriptorType", propOrder = {
    "assertionConsumerServiceURL",
    "authnRequestsSigned"
})
public class SPDescriptorType extends ProviderDescriptorType {

    @XmlElement(name = "AssertionConsumerServiceURL", required = true)
    protected List<AssertionConsumerServiceURL> assertionConsumerServiceURL;
    @XmlElement(name = "AuthnRequestsSigned")
    protected boolean authnRequestsSigned;

    public List<AssertionConsumerServiceURL> getAssertionConsumerServiceURL() {
        if (assertionConsumerServiceURL == null) {
            assertionConsumerServiceURL = new ArrayList<AssertionConsumerServiceURL>();
        }
        return this.assertionConsumerServiceURL;
    }

    public boolean isAuthnRequestsSigned() { return authnRequestsSigned; }
    public void setAuthnRequestsSigned(boolean value) { this.authnRequestsSigned = value; }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = { "value" })
    public static class AssertionConsumerServiceURL {
        @XmlValue
        @XmlSchemaType(name = "anyURI")
        protected String value;
        @XmlAttribute(name = "id", required = true)
        @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
        @XmlID
        @XmlSchemaType(name = "ID")
        protected String id;
        @XmlAttribute(name = "isDefault")
        protected Boolean isDefault;

        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public String getId() { return id; }
        public void setId(String value) { this.id = value; }
        public boolean isIsDefault() { return isDefault != null && isDefault; }
        public void setIsDefault(Boolean value) { this.isDefault = value; }
    }

    /**
     * JAXB 1.x alias for {@link AssertionConsumerServiceURL}.
     * Exists solely for backward-compatibility with legacy callers such as
     * {@code IDFFModelImpl} that cast to
     * {@code SPDescriptorType.AssertionConsumerServiceURLType}.
     */
    public static class AssertionConsumerServiceURLType extends AssertionConsumerServiceURL {
    }
}
