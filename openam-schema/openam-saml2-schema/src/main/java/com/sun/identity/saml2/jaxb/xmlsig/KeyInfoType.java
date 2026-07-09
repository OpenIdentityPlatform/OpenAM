package com.sun.identity.saml2.jaxb.xmlsig;

import java.util.ArrayList;
import java.util.List;
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
 * Hand-maintained replacement for the XJC-generated KeyInfoType.
 *
 * <p>The XJC-generated version uses {@code @XmlElementRefs} / {@code @XmlMixed}
 * for the {@code content} field. In JAXB 4.x this causes
 * {@code JAXBElement<X509DataType>} wrappers to appear in the list.
 * {@code KeyUtil.getCert()} iterates the list and checks
 * {@code if (obj instanceof X509DataElement)}, which always fails against a
 * JAXBElement wrapper, so no certificate is ever extracted and the signing
 * certificate check throws "No X509DataElement".
 *
 * <p>Fix: replace {@code @XmlElementRefs} with {@code @XmlElements} and map the
 * {@code X509Data} entry to {@code type = X509DataElement.class}. JAXB 4.x
 * then places {@code X509DataElement} instances directly in the list.
 * {@code @XmlMixed} and {@code @XmlAnyElement} are intentionally omitted —
 * they are incompatible with {@code @XmlElements} in JAXB 4.x and cause an
 * {@code IllegalAnnotationsException} that nullifies the entire JAXBContext.
 *
 * <p>This file shadows the XJC output; the XJC-generated copy is deleted from
 * {@code target/generated-sources/jaxb/} during the {@code process-sources}
 * phase by the {@code maven-antrun-plugin} execution in {@code pom.xml}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "KeyInfoType", namespace = "http://www.w3.org/2000/09/xmldsig#",
    propOrder = { "content" })
public class KeyInfoType {

    @XmlElements({
        @XmlElement(name = "KeyName",         namespace = "http://www.w3.org/2000/09/xmldsig#"),
        @XmlElement(name = "KeyValue",        namespace = "http://www.w3.org/2000/09/xmldsig#", type = KeyValueType.class),
        @XmlElement(name = "RetrievalMethod", namespace = "http://www.w3.org/2000/09/xmldsig#", type = RetrievalMethodType.class),
        @XmlElement(name = "X509Data",        namespace = "http://www.w3.org/2000/09/xmldsig#", type = X509DataElement.class),
        @XmlElement(name = "PGPData",         namespace = "http://www.w3.org/2000/09/xmldsig#", type = PGPDataType.class),
        @XmlElement(name = "SPKIData",        namespace = "http://www.w3.org/2000/09/xmldsig#", type = SPKIDataType.class),
        @XmlElement(name = "MgmtData",        namespace = "http://www.w3.org/2000/09/xmldsig#")
    })
    protected List<Object> content;

    @XmlAttribute(name = "Id")
    @XmlJavaTypeAdapter(CollapsedStringAdapter.class)
    @XmlID
    @XmlSchemaType(name = "ID")
    protected String id;

    public List<Object> getContent() {
        if (content == null) {
            content = new ArrayList<Object>();
        }
        return this.content;
    }

    public String getId() { return id; }
    public void setId(String value) { this.id = value; }
}
