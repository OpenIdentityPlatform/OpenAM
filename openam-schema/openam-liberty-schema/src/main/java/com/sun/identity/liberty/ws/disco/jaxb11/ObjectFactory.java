package com.sun.identity.liberty.ws.disco.jaxb11;

import javax.xml.namespace.QName;
import com.sun.identity.liberty.ws.disco.jaxb.DirectiveType;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

/**
 * Hand-maintained replacement for the XJC-generated ObjectFactory for the
 * {@code com.sun.identity.liberty.ws.disco.jaxb11} package.
 *
 * <p>Adds the legacy no-arg {@code createSendSingleLogOutElement()} and
 * {@code createGenerateBearerTokenElement()} factory methods that JAXB 1.x XJC
 * generated but JAXB 4.x XJC no longer generates. Callers in
 * {@code SMDiscoEntryData} use these methods.
 *
 * <p>This file shadows the XJC output; the XJC-generated copy is deleted from
 * {@code target/generated-sources/jaxb/} during the {@code process-sources}
 * phase by the {@code maven-antrun-plugin} execution in {@code pom.xml}.
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _Status_QNAME =
        new QName("urn:liberty:disco:2004-04", "Status");
    private static final QName _Extension_QNAME =
        new QName("urn:liberty:disco:2004-04", "Extension");
    private static final QName _Keys_QNAME =
        new QName("urn:liberty:disco:2004-04", "Keys");
    private static final QName _SendSingleLogOut_QNAME =
        new QName("urn:liberty:disco:2004-04", "SendSingleLogOut");
    private static final QName _GenerateBearerToken_QNAME =
        new QName("urn:liberty:disco:2004-04", "GenerateBearerToken");

    public ObjectFactory() {}

    public StatusType createStatusType() { return new StatusType(); }
    public ExtensionType createExtensionType() { return new ExtensionType(); }
    public KeysType createKeysType() { return new KeysType(); }
    public EmptyType createEmptyType() { return new EmptyType(); }

    // No-arg convenience factory methods for legacy callers (JAXB 1.x API compatibility)
    public SendSingleLogOutElement createSendSingleLogOutElement() { return new SendSingleLogOutElement(); }
    public GenerateBearerTokenElement createGenerateBearerTokenElement() { return new GenerateBearerTokenElement(); }

    @XmlElementDecl(namespace = "urn:liberty:disco:2004-04", name = "Status")
    public JAXBElement<StatusType> createStatus(StatusType value) {
        return new JAXBElement<>(_Status_QNAME, StatusType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2004-04", name = "Extension")
    public JAXBElement<ExtensionType> createExtension(ExtensionType value) {
        return new JAXBElement<>(_Extension_QNAME, ExtensionType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2004-04", name = "Keys")
    public JAXBElement<KeysType> createKeys(KeysType value) {
        return new JAXBElement<>(_Keys_QNAME, KeysType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2004-04", name = "SendSingleLogOut")
    public JAXBElement<DirectiveType> createSendSingleLogOut(DirectiveType value) {
        return new JAXBElement<>(_SendSingleLogOut_QNAME, DirectiveType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2004-04", name = "GenerateBearerToken")
    public JAXBElement<DirectiveType> createGenerateBearerToken(DirectiveType value) {
        return new JAXBElement<>(_GenerateBearerToken_QNAME, DirectiveType.class, null, value);
    }
}
