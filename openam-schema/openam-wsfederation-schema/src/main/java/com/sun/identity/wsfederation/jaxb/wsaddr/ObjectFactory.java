
package com.sun.identity.wsfederation.jaxb.wsaddr;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.sun.identity.wsfederation.jaxb.wsaddr package. 
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

    private final static QName _EndpointReference_QNAME = new QName("http://www.w3.org/2005/08/addressing", "EndpointReference");
    private final static QName _ReferenceParameters_QNAME = new QName("http://www.w3.org/2005/08/addressing", "ReferenceParameters");
    private final static QName _Metadata_QNAME = new QName("http://www.w3.org/2005/08/addressing", "Metadata");
    private final static QName _MessageID_QNAME = new QName("http://www.w3.org/2005/08/addressing", "MessageID");
    private final static QName _RelatesTo_QNAME = new QName("http://www.w3.org/2005/08/addressing", "RelatesTo");
    private final static QName _ReplyTo_QNAME = new QName("http://www.w3.org/2005/08/addressing", "ReplyTo");
    private final static QName _From_QNAME = new QName("http://www.w3.org/2005/08/addressing", "From");
    private final static QName _FaultTo_QNAME = new QName("http://www.w3.org/2005/08/addressing", "FaultTo");
    private final static QName _To_QNAME = new QName("http://www.w3.org/2005/08/addressing", "To");
    private final static QName _Action_QNAME = new QName("http://www.w3.org/2005/08/addressing", "Action");
    private final static QName _RetryAfter_QNAME = new QName("http://www.w3.org/2005/08/addressing", "RetryAfter");
    private final static QName _ProblemHeaderQName_QNAME = new QName("http://www.w3.org/2005/08/addressing", "ProblemHeaderQName");
    private final static QName _ProblemIRI_QNAME = new QName("http://www.w3.org/2005/08/addressing", "ProblemIRI");
    private final static QName _ProblemAction_QNAME = new QName("http://www.w3.org/2005/08/addressing", "ProblemAction");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.sun.identity.wsfederation.jaxb.wsaddr
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link EndpointReferenceType }
     * 
     */
    public EndpointReferenceType createEndpointReferenceType() {
        return new EndpointReferenceType();
    }

    /**
     * Create an instance of {@link ReferenceParametersType }
     * 
     */
    public ReferenceParametersType createReferenceParametersType() {
        return new ReferenceParametersType();
    }

    /**
     * Create an instance of {@link MetadataType }
     * 
     */
    public MetadataType createMetadataType() {
        return new MetadataType();
    }

    /**
     * Create an instance of {@link AttributedURIType }
     * 
     */
    public AttributedURIType createAttributedURIType() {
        return new AttributedURIType();
    }

    /**
     * Create an instance of {@link RelatesToType }
     * 
     */
    public RelatesToType createRelatesToType() {
        return new RelatesToType();
    }

    /**
     * Create an instance of {@link AttributedUnsignedLongType }
     * 
     */
    public AttributedUnsignedLongType createAttributedUnsignedLongType() {
        return new AttributedUnsignedLongType();
    }

    /**
     * Create an instance of {@link AttributedQNameType }
     * 
     */
    public AttributedQNameType createAttributedQNameType() {
        return new AttributedQNameType();
    }

    /**
     * Create an instance of {@link ProblemActionType }
     * 
     */
    public ProblemActionType createProblemActionType() {
        return new ProblemActionType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2005/08/addressing", name = "EndpointReference")
    public JAXBElement<EndpointReferenceType> createEndpointReference(EndpointReferenceType value) {
        return new JAXBElement<EndpointReferenceType>(_EndpointReference_QNAME, EndpointReferenceType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReferenceParametersType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ReferenceParametersType }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2005/08/addressing", name = "ReferenceParameters")
    public JAXBElement<ReferenceParametersType> createReferenceParameters(ReferenceParametersType value) {
        return new JAXBElement<ReferenceParametersType>(_ReferenceParameters_QNAME, ReferenceParametersType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link MetadataType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link MetadataType }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2005/08/addressing", name = "Metadata")
    public JAXBElement<MetadataType> createMetadata(MetadataType value) {
        return new JAXBElement<MetadataType>(_Metadata_QNAME, MetadataType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AttributedURIType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AttributedURIType }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2005/08/addressing", name = "MessageID")
    public JAXBElement<AttributedURIType> createMessageID(AttributedURIType value) {
        return new JAXBElement<AttributedURIType>(_MessageID_QNAME, AttributedURIType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RelatesToType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RelatesToType }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2005/08/addressing", name = "RelatesTo")
    public JAXBElement<RelatesToType> createRelatesTo(RelatesToType value) {
        return new JAXBElement<RelatesToType>(_RelatesTo_QNAME, RelatesToType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2005/08/addressing", name = "ReplyTo")
    public JAXBElement<EndpointReferenceType> createReplyTo(EndpointReferenceType value) {
        return new JAXBElement<EndpointReferenceType>(_ReplyTo_QNAME, EndpointReferenceType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2005/08/addressing", name = "From")
    public JAXBElement<EndpointReferenceType> createFrom(EndpointReferenceType value) {
        return new JAXBElement<EndpointReferenceType>(_From_QNAME, EndpointReferenceType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2005/08/addressing", name = "FaultTo")
    public JAXBElement<EndpointReferenceType> createFaultTo(EndpointReferenceType value) {
        return new JAXBElement<EndpointReferenceType>(_FaultTo_QNAME, EndpointReferenceType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AttributedURIType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AttributedURIType }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2005/08/addressing", name = "To")
    public JAXBElement<AttributedURIType> createTo(AttributedURIType value) {
        return new JAXBElement<AttributedURIType>(_To_QNAME, AttributedURIType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AttributedURIType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AttributedURIType }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2005/08/addressing", name = "Action")
    public JAXBElement<AttributedURIType> createAction(AttributedURIType value) {
        return new JAXBElement<AttributedURIType>(_Action_QNAME, AttributedURIType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AttributedUnsignedLongType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AttributedUnsignedLongType }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2005/08/addressing", name = "RetryAfter")
    public JAXBElement<AttributedUnsignedLongType> createRetryAfter(AttributedUnsignedLongType value) {
        return new JAXBElement<AttributedUnsignedLongType>(_RetryAfter_QNAME, AttributedUnsignedLongType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AttributedQNameType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AttributedQNameType }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2005/08/addressing", name = "ProblemHeaderQName")
    public JAXBElement<AttributedQNameType> createProblemHeaderQName(AttributedQNameType value) {
        return new JAXBElement<AttributedQNameType>(_ProblemHeaderQName_QNAME, AttributedQNameType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AttributedURIType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AttributedURIType }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2005/08/addressing", name = "ProblemIRI")
    public JAXBElement<AttributedURIType> createProblemIRI(AttributedURIType value) {
        return new JAXBElement<AttributedURIType>(_ProblemIRI_QNAME, AttributedURIType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ProblemActionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ProblemActionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://www.w3.org/2005/08/addressing", name = "ProblemAction")
    public JAXBElement<ProblemActionType> createProblemAction(ProblemActionType value) {
        return new JAXBElement<ProblemActionType>(_ProblemAction_QNAME, ProblemActionType.class, null, value);
    }

    // ---- No-arg element factory methods for JAXB 1.x compatibility ----

    public EndpointReferenceElement createEndpointReferenceElement() { return new EndpointReferenceElement(); }

}
