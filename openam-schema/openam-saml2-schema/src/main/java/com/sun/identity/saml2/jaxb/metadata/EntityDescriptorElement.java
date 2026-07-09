package com.sun.identity.saml2.jaxb.metadata;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Root-element binding for {@code EntityDescriptor}, extending EntityDescriptorType.
 *
 * <p>{@code @XmlRootElement} is required so JAXB 4.x returns this concrete
 * class directly on unmarshal rather than wrapping the result in
 * {@code JAXBElement&lt;EntityDescriptorType&gt;}. Without it the
 * {@code instanceof EntityDescriptorElement} checks in
 * {@code FedletConfigurationImpl.getEntityID()} and
 * {@code SAML2MetaManager.getEntityDescriptor()} always fail, leaving
 * {@code entityMap} empty and causing the SP Entity ID to be null.
 */
@XmlRootElement(name = "EntityDescriptor", namespace = "urn:oasis:names:tc:SAML:2.0:metadata")
public class EntityDescriptorElement extends EntityDescriptorType {}
