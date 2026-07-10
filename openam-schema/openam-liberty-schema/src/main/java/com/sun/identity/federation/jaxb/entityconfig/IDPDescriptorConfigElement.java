package com.sun.identity.federation.jaxb.entityconfig;

/**
 * Compatibility shim for JAXB 1.x IDPDescriptorConfigElement.
 * In JAXB 2.x/3.x, element declarations with abstract types are represented
 * differently. This class provides backward compatibility.
 */
public class IDPDescriptorConfigElement extends BaseConfigType {
}
