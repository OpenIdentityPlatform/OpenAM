package com.sun.identity.wsfederation.jaxb.wsfederation;

/**
 * Compatibility shim for JAXB 1.x FederationMetadataElement.
 *
 * <p>In JAXB 1.x XJC this was generated as a JAXB element class.
 * JAXB 4.x XJC no longer generates such classes; the parent type
 * {@link FederationMetadataType} is used directly.
 *
 * <p>This shim extends {@code FederationMetadataType} and provides the old
 * JAXB 1.x name so that {@code instanceof} checks in
 * {@code ImportEntityModelImpl} compile and work correctly.
 */
public class FederationMetadataElement extends FederationMetadataType {
}
