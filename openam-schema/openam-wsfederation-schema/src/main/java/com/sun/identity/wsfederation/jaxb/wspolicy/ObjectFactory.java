
package com.sun.identity.wsfederation.jaxb.wspolicy;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.sun.identity.wsfederation.jaxb.wspolicy package. 
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

    private final static QName _All_QNAME = new QName("http://schemas.xmlsoap.org/ws/2004/09/policy", "All");
    private final static QName _ExactlyOne_QNAME = new QName("http://schemas.xmlsoap.org/ws/2004/09/policy", "ExactlyOne");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.sun.identity.wsfederation.jaxb.wspolicy
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link PolicyElement }
     * 
     */
    public PolicyElement createPolicyElement() {
        return new PolicyElement();
    }

    /**
     * Create an instance of {@link OperatorContentType }
     * 
     */
    public OperatorContentType createOperatorContentType() {
        return new OperatorContentType();
    }

    /**
     * Create an instance of {@link PolicyReferenceElement }
     * 
     */
    public PolicyReferenceElement createPolicyReferenceElement() {
        return new PolicyReferenceElement();
    }

    /**
     * Create an instance of {@link PolicyAttachmentElement }
     * 
     */
    public PolicyAttachmentElement createPolicyAttachmentElement() {
        return new PolicyAttachmentElement();
    }

    /**
     * Create an instance of {@link AppliesToElement }
     * 
     */
    public AppliesToElement createAppliesToElement() {
        return new AppliesToElement();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link OperatorContentType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link OperatorContentType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2004/09/policy", name = "All")
    public JAXBElement<OperatorContentType> createAll(OperatorContentType value) {
        return new JAXBElement<OperatorContentType>(_All_QNAME, OperatorContentType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link OperatorContentType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link OperatorContentType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2004/09/policy", name = "ExactlyOne")
    public JAXBElement<OperatorContentType> createExactlyOne(OperatorContentType value) {
        return new JAXBElement<OperatorContentType>(_ExactlyOne_QNAME, OperatorContentType.class, null, value);
    }

    // ---- No-arg element factory methods for JAXB 1.x compatibility ----

    public ExactlyOneElement createExactlyOneElement() { return new ExactlyOneElement(); }
    public AllElement createAllElement() { return new AllElement(); }

}
