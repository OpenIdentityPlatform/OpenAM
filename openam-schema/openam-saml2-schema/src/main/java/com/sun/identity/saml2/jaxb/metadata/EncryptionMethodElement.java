package com.sun.identity.saml2.jaxb.metadata;

import com.sun.identity.saml2.jaxb.xmlenc.EncryptionMethodType;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Root-element binding for the metadata EncryptionMethod element.
 *
 * <p>In JAXB 1.x XJC this was a generated Element class that extended
 * {@link EncryptionMethodType}. JAXB 4.x XJC no longer generates Element
 * classes, so this hand-maintained replacement provides the same API for
 * legacy callers in {@code SAMLv2ModelImpl}.
 */
@XmlRootElement(name = "EncryptionMethod", namespace = "urn:oasis:names:tc:SAML:2.0:metadata")
public class EncryptionMethodElement extends EncryptionMethodType {
}
