package com.sun.identity.federation.jaxb.entityconfig;

/**
 * Compatibility shim for JAXB 1.x EntityConfigElement.
 * In JAXB 1.x, EntityConfigElement was an interface extending EntityConfigType.
 * In JAXB 2.x/3.x, EntityConfigType is a concrete class; this shim extends it.
 */
public class EntityConfigElement extends EntityConfigType {
}
