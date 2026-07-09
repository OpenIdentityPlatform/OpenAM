package com.sun.identity.liberty.ws.disco.plugins.jaxb;

import com.sun.identity.liberty.ws.disco.jaxb.InsertEntryType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Compatibility shim for JAXB 1.x DiscoEntryElement.
 * In JAXB 4.x XJC this element is handled via JAXBElement factory methods.
 */
@XmlRootElement(name = "DiscoEntry", namespace = "urn:com:sun:identityserver:liberty:ws:disco:discoentry")
public class DiscoEntryElement extends InsertEntryType {}
