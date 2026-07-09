package com.sun.identity.saml2.jaxb.metadata;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Root-element binding for {@code EntitiesDescriptor}, extending EntitiesDescriptorType.
 *
 * <p>{@code @XmlRootElement} is required so JAXB 4.x returns this concrete
 * class directly on unmarshal rather than wrapping in
 * {@code JAXBElement&lt;EntitiesDescriptorType&gt;}.
 * The metadata ObjectFactory omits {@code @XmlElementDecl} for
 * {@code EntitiesDescriptor} so this annotation takes effect.
 */
@XmlRootElement(name = "EntitiesDescriptor", namespace = "urn:oasis:names:tc:SAML:2.0:metadata")
public class EntitiesDescriptorElement extends EntitiesDescriptorType {}
