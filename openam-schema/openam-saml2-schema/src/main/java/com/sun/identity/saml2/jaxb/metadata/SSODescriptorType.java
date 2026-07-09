package com.sun.identity.saml2.jaxb.metadata;

import java.util.ArrayList;
import java.util.List;
import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlSchemaType;
import jakarta.xml.bind.annotation.XmlSeeAlso;
import jakarta.xml.bind.annotation.XmlType;

/**
 * Hand-maintained replacement for the XJC-generated SSODescriptorType.
 *
 * <p>Changes the endpoint list fields to use concrete {@code *Element}
 * subclasses so that JAXB 4.x unmarshals the expected element instances
 * rather than the base {@code EndpointType} / {@code IndexedEndpointType},
 * preventing {@code ClassCastException} in SAML2 metadata code.
 *
 * <p>This file shadows the XJC output; the XJC-generated copy is deleted from
 * {@code target/generated-sources/jaxb/} during the {@code process-sources}
 * phase by the {@code maven-antrun-plugin} execution in {@code pom.xml}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "SSODescriptorType", propOrder = {
    "artifactResolutionService",
    "singleLogoutService",
    "manageNameIDService",
    "nameIDFormat"
})
@XmlSeeAlso({
    IDPSSODescriptorType.class,
    SPSSODescriptorType.class
})
public abstract class SSODescriptorType extends RoleDescriptorType {

    @XmlElement(name = "ArtifactResolutionService", type = ArtifactResolutionServiceElement.class)
    protected List<ArtifactResolutionServiceElement> artifactResolutionService;
    @XmlElement(name = "SingleLogoutService", type = SingleLogoutServiceElement.class)
    protected List<EndpointType> singleLogoutService;
    @XmlElement(name = "ManageNameIDService", type = ManageNameIDServiceElement.class)
    protected List<ManageNameIDServiceElement> manageNameIDService;
    @XmlElement(name = "NameIDFormat")
    @XmlSchemaType(name = "anyURI")
    protected List<String> nameIDFormat;

    public List<ArtifactResolutionServiceElement> getArtifactResolutionService() {
        if (artifactResolutionService == null) {
            artifactResolutionService = new ArrayList<ArtifactResolutionServiceElement>();
        }
        return this.artifactResolutionService;
    }

    public List<EndpointType> getSingleLogoutService() {
        if (singleLogoutService == null) {
            singleLogoutService = new ArrayList<EndpointType>();
        }
        return this.singleLogoutService;
    }

    public List<ManageNameIDServiceElement> getManageNameIDService() {
        if (manageNameIDService == null) {
            manageNameIDService = new ArrayList<ManageNameIDServiceElement>();
        }
        return this.manageNameIDService;
    }

    public List<String> getNameIDFormat() {
        if (nameIDFormat == null) {
            nameIDFormat = new ArrayList<String>();
        }
        return this.nameIDFormat;
    }
}
