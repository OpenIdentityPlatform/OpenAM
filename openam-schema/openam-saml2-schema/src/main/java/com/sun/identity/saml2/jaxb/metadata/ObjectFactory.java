package com.sun.identity.saml2.jaxb.metadata;

import javax.xml.namespace.QName;
import com.sun.identity.saml2.jaxb.xmlenc.EncryptionMethodType;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;

/**
 * Hand-maintained replacement for the XJC-generated ObjectFactory for the
 * {@code com.sun.identity.saml2.jaxb.metadata} package.
 *
 * <p>The XJC-generated version declares {@code @XmlElementDecl} for both
 * {@code EntityDescriptor} and {@code EntitiesDescriptor} with
 * {@code declaredType = EntityDescriptorType.class / EntitiesDescriptorType.class}.
 * In JAXB 4.x, {@code @XmlElementDecl} takes precedence over
 * {@code @XmlRootElement} for the same QName, so unmarshaling those documents
 * returns {@code JAXBElement<EntityDescriptorType>} instead of the concrete
 * {@code EntityDescriptorElement}. This breaks the {@code instanceof} checks in
 * {@code FedletConfigurationImpl.getEntityID()} and
 * {@code SAML2MetaManager.getEntityDescriptor()}.
 *
 * <p>Fix: omit {@code @XmlElementDecl} from {@code createEntityDescriptor} and
 * {@code createEntitiesDescriptor} so that JAXB uses {@code @XmlRootElement} on
 * the {@code *Element} subclasses and returns them directly.
 *
 * <p>This file shadows the XJC output; the XJC-generated copy is deleted from
 * {@code target/generated-sources/jaxb/} during the {@code process-sources}
 * phase by the {@code maven-antrun-plugin} execution in {@code pom.xml}.
 */
@XmlRegistry
public class ObjectFactory {

    private static final QName _Extensions_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "Extensions");
    private static final QName _EntitiesDescriptor_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "EntitiesDescriptor");
    private static final QName _EntityDescriptor_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "EntityDescriptor");
    private static final QName _Organization_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "Organization");
    private static final QName _OrganizationName_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "OrganizationName");
    private static final QName _OrganizationDisplayName_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "OrganizationDisplayName");
    private static final QName _OrganizationURL_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "OrganizationURL");
    private static final QName _ContactPerson_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "ContactPerson");
    private static final QName _Company_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "Company");
    private static final QName _GivenName_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "GivenName");
    private static final QName _SurName_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "SurName");
    private static final QName _EmailAddress_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "EmailAddress");
    private static final QName _TelephoneNumber_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "TelephoneNumber");
    private static final QName _AdditionalMetadataLocation_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "AdditionalMetadataLocation");
    private static final QName _RoleDescriptor_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "RoleDescriptor");
    private static final QName _KeyDescriptor_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "KeyDescriptor");
    private static final QName _EncryptionMethod_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "EncryptionMethod");
    private static final QName _ArtifactResolutionService_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "ArtifactResolutionService");
    private static final QName _SingleLogoutService_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "SingleLogoutService");
    private static final QName _ManageNameIDService_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "ManageNameIDService");
    private static final QName _NameIDFormat_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "NameIDFormat");
    private static final QName _IDPSSODescriptor_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "IDPSSODescriptor");
    private static final QName _SingleSignOnService_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "SingleSignOnService");
    private static final QName _NameIDMappingService_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "NameIDMappingService");
    private static final QName _AssertionIDRequestService_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "AssertionIDRequestService");
    private static final QName _AttributeProfile_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "AttributeProfile");
    private static final QName _SPSSODescriptor_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "SPSSODescriptor");
    private static final QName _AssertionConsumerService_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "AssertionConsumerService");
    private static final QName _AttributeConsumingService_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "AttributeConsumingService");
    private static final QName _ServiceName_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "ServiceName");
    private static final QName _ServiceDescription_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "ServiceDescription");
    private static final QName _RequestedAttribute_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "RequestedAttribute");
    private static final QName _AuthnAuthorityDescriptor_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "AuthnAuthorityDescriptor");
    private static final QName _AuthnQueryService_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "AuthnQueryService");
    private static final QName _PDPDescriptor_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "PDPDescriptor");
    private static final QName _AuthzService_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "AuthzService");
    private static final QName _XACMLPDPDescriptor_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "XACMLPDPDescriptor");
    private static final QName _XACMLAuthzService_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "XACMLAuthzService");
    private static final QName _AttributeAuthorityDescriptor_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "AttributeAuthorityDescriptor");
    private static final QName _AttributeService_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "AttributeService");
    private static final QName _AffiliationDescriptor_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "AffiliationDescriptor");
    private static final QName _AffiliateMember_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "AffiliateMember");
    private static final QName _QueryDescriptor_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "QueryDescriptor");
    private static final QName _XACMLAuthzDecisionQueryDescriptor_QNAME = new QName("urn:oasis:names:tc:SAML:2.0:metadata", "XACMLAuthzDecisionQueryDescriptor");

    public ObjectFactory() {}

    // No-arg convenience factory methods for legacy callers (JAXB 1.x API compatibility)
    public SingleSignOnServiceElement createSingleSignOnServiceElement() { return new SingleSignOnServiceElement(); }
    public SingleLogoutServiceElement createSingleLogoutServiceElement() { return new SingleLogoutServiceElement(); }
    public ManageNameIDServiceElement createManageNameIDServiceElement() { return new ManageNameIDServiceElement(); }
    public NameIDMappingServiceElement createNameIDMappingServiceElement() { return new NameIDMappingServiceElement(); }
    public AssertionConsumerServiceElement createAssertionConsumerServiceElement() { return new AssertionConsumerServiceElement(); }
    public ArtifactResolutionServiceElement createArtifactResolutionServiceElement() { return new ArtifactResolutionServiceElement(); }
    public AttributeServiceElement createAttributeServiceElement() { return new AttributeServiceElement(); }
    public AssertionIDRequestServiceElement createAssertionIDRequestServiceElement() { return new AssertionIDRequestServiceElement(); }
    public AuthnQueryServiceElement createAuthnQueryServiceElement() { return new AuthnQueryServiceElement(); }

    public ExtensionsType createExtensionsType() { return new ExtensionsType(); }
    public EntitiesDescriptorType createEntitiesDescriptorType() { return new EntitiesDescriptorType(); }
    public EntityDescriptorType createEntityDescriptorType() { return new EntityDescriptorType(); }
    public OrganizationType createOrganizationType() { return new OrganizationType(); }
    public LocalizedNameType createLocalizedNameType() { return new LocalizedNameType(); }
    public LocalizedURIType createLocalizedURIType() { return new LocalizedURIType(); }
    public ContactType createContactType() { return new ContactType(); }
    public AdditionalMetadataLocationType createAdditionalMetadataLocationType() { return new AdditionalMetadataLocationType(); }
    public KeyDescriptorType createKeyDescriptorType() { return new KeyDescriptorType(); }
    public IndexedEndpointType createIndexedEndpointType() { return new IndexedEndpointType(); }
    public EndpointType createEndpointType() { return new EndpointType(); }
    public IDPSSODescriptorType createIDPSSODescriptorType() { return new IDPSSODescriptorType(); }
    public SPSSODescriptorType createSPSSODescriptorType() { return new SPSSODescriptorType(); }
    public AttributeConsumingServiceType createAttributeConsumingServiceType() { return new AttributeConsumingServiceType(); }
    public RequestedAttributeType createRequestedAttributeType() { return new RequestedAttributeType(); }
    public AuthnAuthorityDescriptorType createAuthnAuthorityDescriptorType() { return new AuthnAuthorityDescriptorType(); }
    public PDPDescriptorType createPDPDescriptorType() { return new PDPDescriptorType(); }
    public XACMLPDPDescriptorType createXACMLPDPDescriptorType() { return new XACMLPDPDescriptorType(); }
    public AttributeAuthorityDescriptorType createAttributeAuthorityDescriptorType() { return new AttributeAuthorityDescriptorType(); }
    public AttributeServiceType createAttributeServiceType() { return new AttributeServiceType(); }
    public AffiliationDescriptorType createAffiliationDescriptorType() { return new AffiliationDescriptorType(); }
    public XACMLAuthzDecisionQueryDescriptorType createXACMLAuthzDecisionQueryDescriptorType() { return new XACMLAuthzDecisionQueryDescriptorType(); }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "Extensions")
    public JAXBElement<ExtensionsType> createExtensions(ExtensionsType value) {
        return new JAXBElement<>(_Extensions_QNAME, ExtensionsType.class, null, value);
    }

    // NOTE: @XmlElementDecl intentionally absent for EntitiesDescriptor and EntityDescriptor.
    // In JAXB 4.x @XmlElementDecl takes precedence over @XmlRootElement for the same QName,
    // so these would cause unmarshal to return JAXBElement<*Type> instead of the concrete
    // *Element subclass, breaking instanceof checks in FedletConfigurationImpl.getEntityID()
    // and SAML2MetaManager.getEntityDescriptor(). Omitting @XmlElementDecl here allows
    // @XmlRootElement on EntitiesDescriptorElement / EntityDescriptorElement to take effect.
    public JAXBElement<EntitiesDescriptorElement> createEntitiesDescriptor(EntitiesDescriptorElement value) {
        return new JAXBElement<>(_EntitiesDescriptor_QNAME, EntitiesDescriptorElement.class, null, value);
    }

    public JAXBElement<EntityDescriptorElement> createEntityDescriptor(EntityDescriptorElement value) {
        return new JAXBElement<>(_EntityDescriptor_QNAME, EntityDescriptorElement.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "Organization")
    public JAXBElement<OrganizationType> createOrganization(OrganizationType value) {
        return new JAXBElement<>(_Organization_QNAME, OrganizationType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "OrganizationName")
    public JAXBElement<LocalizedNameType> createOrganizationName(LocalizedNameType value) {
        return new JAXBElement<>(_OrganizationName_QNAME, LocalizedNameType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "OrganizationDisplayName")
    public JAXBElement<LocalizedNameType> createOrganizationDisplayName(LocalizedNameType value) {
        return new JAXBElement<>(_OrganizationDisplayName_QNAME, LocalizedNameType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "OrganizationURL")
    public JAXBElement<LocalizedURIType> createOrganizationURL(LocalizedURIType value) {
        return new JAXBElement<>(_OrganizationURL_QNAME, LocalizedURIType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "ContactPerson")
    public JAXBElement<ContactType> createContactPerson(ContactType value) {
        return new JAXBElement<>(_ContactPerson_QNAME, ContactType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "Company")
    public JAXBElement<String> createCompany(String value) {
        return new JAXBElement<>(_Company_QNAME, String.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "GivenName")
    public JAXBElement<String> createGivenName(String value) {
        return new JAXBElement<>(_GivenName_QNAME, String.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "SurName")
    public JAXBElement<String> createSurName(String value) {
        return new JAXBElement<>(_SurName_QNAME, String.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "EmailAddress")
    public JAXBElement<String> createEmailAddress(String value) {
        return new JAXBElement<>(_EmailAddress_QNAME, String.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "TelephoneNumber")
    public JAXBElement<String> createTelephoneNumber(String value) {
        return new JAXBElement<>(_TelephoneNumber_QNAME, String.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "AdditionalMetadataLocation")
    public JAXBElement<AdditionalMetadataLocationType> createAdditionalMetadataLocation(AdditionalMetadataLocationType value) {
        return new JAXBElement<>(_AdditionalMetadataLocation_QNAME, AdditionalMetadataLocationType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "RoleDescriptor")
    public JAXBElement<RoleDescriptorType> createRoleDescriptor(RoleDescriptorType value) {
        return new JAXBElement<>(_RoleDescriptor_QNAME, RoleDescriptorType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "KeyDescriptor")
    public JAXBElement<KeyDescriptorType> createKeyDescriptor(KeyDescriptorType value) {
        return new JAXBElement<>(_KeyDescriptor_QNAME, KeyDescriptorType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "EncryptionMethod")
    public JAXBElement<EncryptionMethodType> createEncryptionMethod(EncryptionMethodType value) {
        return new JAXBElement<>(_EncryptionMethod_QNAME, EncryptionMethodType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "ArtifactResolutionService")
    public JAXBElement<IndexedEndpointType> createArtifactResolutionService(IndexedEndpointType value) {
        return new JAXBElement<>(_ArtifactResolutionService_QNAME, IndexedEndpointType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "SingleLogoutService")
    public JAXBElement<EndpointType> createSingleLogoutService(EndpointType value) {
        return new JAXBElement<>(_SingleLogoutService_QNAME, EndpointType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "ManageNameIDService")
    public JAXBElement<EndpointType> createManageNameIDService(EndpointType value) {
        return new JAXBElement<>(_ManageNameIDService_QNAME, EndpointType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "NameIDFormat")
    public JAXBElement<String> createNameIDFormat(String value) {
        return new JAXBElement<>(_NameIDFormat_QNAME, String.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "IDPSSODescriptor")
    public JAXBElement<IDPSSODescriptorType> createIDPSSODescriptor(IDPSSODescriptorType value) {
        return new JAXBElement<>(_IDPSSODescriptor_QNAME, IDPSSODescriptorType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "SingleSignOnService")
    public JAXBElement<EndpointType> createSingleSignOnService(EndpointType value) {
        return new JAXBElement<>(_SingleSignOnService_QNAME, EndpointType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "NameIDMappingService")
    public JAXBElement<EndpointType> createNameIDMappingService(EndpointType value) {
        return new JAXBElement<>(_NameIDMappingService_QNAME, EndpointType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "AssertionIDRequestService")
    public JAXBElement<EndpointType> createAssertionIDRequestService(EndpointType value) {
        return new JAXBElement<>(_AssertionIDRequestService_QNAME, EndpointType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "AttributeProfile")
    public JAXBElement<String> createAttributeProfile(String value) {
        return new JAXBElement<>(_AttributeProfile_QNAME, String.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "SPSSODescriptor")
    public JAXBElement<SPSSODescriptorType> createSPSSODescriptor(SPSSODescriptorType value) {
        return new JAXBElement<>(_SPSSODescriptor_QNAME, SPSSODescriptorType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "AssertionConsumerService")
    public JAXBElement<IndexedEndpointType> createAssertionConsumerService(IndexedEndpointType value) {
        return new JAXBElement<>(_AssertionConsumerService_QNAME, IndexedEndpointType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "AttributeConsumingService")
    public JAXBElement<AttributeConsumingServiceType> createAttributeConsumingService(AttributeConsumingServiceType value) {
        return new JAXBElement<>(_AttributeConsumingService_QNAME, AttributeConsumingServiceType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "ServiceName")
    public JAXBElement<LocalizedNameType> createServiceName(LocalizedNameType value) {
        return new JAXBElement<>(_ServiceName_QNAME, LocalizedNameType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "ServiceDescription")
    public JAXBElement<LocalizedNameType> createServiceDescription(LocalizedNameType value) {
        return new JAXBElement<>(_ServiceDescription_QNAME, LocalizedNameType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "RequestedAttribute")
    public JAXBElement<RequestedAttributeType> createRequestedAttribute(RequestedAttributeType value) {
        return new JAXBElement<>(_RequestedAttribute_QNAME, RequestedAttributeType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "AuthnAuthorityDescriptor")
    public JAXBElement<AuthnAuthorityDescriptorType> createAuthnAuthorityDescriptor(AuthnAuthorityDescriptorType value) {
        return new JAXBElement<>(_AuthnAuthorityDescriptor_QNAME, AuthnAuthorityDescriptorType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "AuthnQueryService")
    public JAXBElement<EndpointType> createAuthnQueryService(EndpointType value) {
        return new JAXBElement<>(_AuthnQueryService_QNAME, EndpointType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "PDPDescriptor")
    public JAXBElement<PDPDescriptorType> createPDPDescriptor(PDPDescriptorType value) {
        return new JAXBElement<>(_PDPDescriptor_QNAME, PDPDescriptorType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "AuthzService")
    public JAXBElement<EndpointType> createAuthzService(EndpointType value) {
        return new JAXBElement<>(_AuthzService_QNAME, EndpointType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "XACMLPDPDescriptor")
    public JAXBElement<XACMLPDPDescriptorType> createXACMLPDPDescriptor(XACMLPDPDescriptorType value) {
        return new JAXBElement<>(_XACMLPDPDescriptor_QNAME, XACMLPDPDescriptorType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "XACMLAuthzService")
    public JAXBElement<EndpointType> createXACMLAuthzService(EndpointType value) {
        return new JAXBElement<>(_XACMLAuthzService_QNAME, EndpointType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "AttributeAuthorityDescriptor")
    public JAXBElement<AttributeAuthorityDescriptorType> createAttributeAuthorityDescriptor(AttributeAuthorityDescriptorType value) {
        return new JAXBElement<>(_AttributeAuthorityDescriptor_QNAME, AttributeAuthorityDescriptorType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "AttributeService")
    public JAXBElement<AttributeServiceType> createAttributeService(AttributeServiceType value) {
        return new JAXBElement<>(_AttributeService_QNAME, AttributeServiceType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "AffiliationDescriptor")
    public JAXBElement<AffiliationDescriptorType> createAffiliationDescriptor(AffiliationDescriptorType value) {
        return new JAXBElement<>(_AffiliationDescriptor_QNAME, AffiliationDescriptorType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "AffiliateMember")
    public JAXBElement<String> createAffiliateMember(String value) {
        return new JAXBElement<>(_AffiliateMember_QNAME, String.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "QueryDescriptor")
    public JAXBElement<QueryDescriptorType> createQueryDescriptor(QueryDescriptorType value) {
        return new JAXBElement<>(_QueryDescriptor_QNAME, QueryDescriptorType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:oasis:names:tc:SAML:2.0:metadata", name = "XACMLAuthzDecisionQueryDescriptor")
    public JAXBElement<XACMLAuthzDecisionQueryDescriptorType> createXACMLAuthzDecisionQueryDescriptor(XACMLAuthzDecisionQueryDescriptorType value) {
        return new JAXBElement<>(_XACMLAuthzDecisionQueryDescriptor_QNAME, XACMLAuthzDecisionQueryDescriptorType.class, null, value);
    }
}
