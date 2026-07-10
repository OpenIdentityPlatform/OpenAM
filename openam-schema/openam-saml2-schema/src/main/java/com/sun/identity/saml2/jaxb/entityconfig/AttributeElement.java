package com.sun.identity.saml2.jaxb.entityconfig;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Root-element binding for the entityconfig Attribute element.
 *
 * <p>In JAXB 1.x XJC this was a generated Element class that extended
 * {@link AttributeType}. JAXB 4.x XJC no longer generates Element classes,
 * so this hand-maintained replacement provides the same API for legacy callers
 * in {@code SAMLv2ModelImpl} and {@code IDFFModelImpl}.
 */
@XmlRootElement(name = "Attribute", namespace = "urn:sun:fm:SAML:2.0:entityconfig")
public class AttributeElement extends AttributeType {
}
