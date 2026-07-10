
package com.sun.identity.liberty.ws.disco.plugins.jaxb;

import javax.xml.namespace.QName;
import com.sun.identity.liberty.ws.disco.jaxb.InsertEntryType;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.sun.identity.liberty.ws.disco.plugins.jaxb package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _DiscoEntry_QNAME = new QName("urn:com:sun:identityserver:liberty:ws:disco:discoentry", "DiscoEntry");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.sun.identity.liberty.ws.disco.plugins.jaxb
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link InsertEntryType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link InsertEntryType }{@code >}
     */
    @XmlElementDecl(namespace = "urn:com:sun:identityserver:liberty:ws:disco:discoentry", name = "DiscoEntry")
    public JAXBElement<InsertEntryType> createDiscoEntry(InsertEntryType value) {
        return new JAXBElement<InsertEntryType>(_DiscoEntry_QNAME, InsertEntryType.class, null, value);
    }

    // ---- No-arg element factory methods for JAXB 1.x compatibility ----
    public DiscoEntryElement createDiscoEntryElement() { return new DiscoEntryElement(); }

}
