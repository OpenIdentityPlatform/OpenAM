
package com.sun.identity.liberty.ws.disco.jaxb;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlIDREF;
import jakarta.xml.bind.annotation.XmlRegistry;

/**
 * ObjectFactory for com.sun.identity.liberty.ws.disco.jaxb.
 * This is a hand-maintained shim that adds the no-arg create*Element()
 * convenience methods that JAXB 1.x generated but JAXB 4.x does not.
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _Status_QNAME = new QName("urn:liberty:disco:2003-08", "Status");
    private final static QName _Extension_QNAME = new QName("urn:liberty:disco:2003-08", "Extension");
    private final static QName _ServiceType_QNAME = new QName("urn:liberty:disco:2003-08", "ServiceType");
    private final static QName _ResourceID_QNAME = new QName("urn:liberty:disco:2003-08", "ResourceID");
    private final static QName _EncryptedResourceID_QNAME = new QName("urn:liberty:disco:2003-08", "EncryptedResourceID");
    private final static QName _ResourceOffering_QNAME = new QName("urn:liberty:disco:2003-08", "ResourceOffering");
    private final static QName _Options_QNAME = new QName("urn:liberty:disco:2003-08", "Options");
    private final static QName _Query_QNAME = new QName("urn:liberty:disco:2003-08", "Query");
    private final static QName _QueryResponse_QNAME = new QName("urn:liberty:disco:2003-08", "QueryResponse");
    private final static QName _Modify_QNAME = new QName("urn:liberty:disco:2003-08", "Modify");
    private final static QName _AuthenticateRequester_QNAME = new QName("urn:liberty:disco:2003-08", "AuthenticateRequester");
    private final static QName _AuthorizeRequester_QNAME = new QName("urn:liberty:disco:2003-08", "AuthorizeRequester");
    private final static QName _AuthenticateSessionContext_QNAME = new QName("urn:liberty:disco:2003-08", "AuthenticateSessionContext");
    private final static QName _EncryptResourceID_QNAME = new QName("urn:liberty:disco:2003-08", "EncryptResourceID");
    private final static QName _ModifyResponse_QNAME = new QName("urn:liberty:disco:2003-08", "ModifyResponse");
    private final static QName _DescriptionTypeCredentialRef_QNAME = new QName("urn:liberty:disco:2003-08", "CredentialRef");

    public ObjectFactory() {
    }

    public QueryResponseType createQueryResponseType() {
        return new QueryResponseType();
    }

    public QueryType createQueryType() {
        return new QueryType();
    }

    public InsertEntryType createInsertEntryType() {
        return new InsertEntryType();
    }

    public StatusType createStatusType() {
        return new StatusType();
    }

    public ExtensionType createExtensionType() {
        return new ExtensionType();
    }

    public ResourceIDType createResourceIDType() {
        return new ResourceIDType();
    }

    public EncryptedResourceIDType createEncryptedResourceIDType() {
        return new EncryptedResourceIDType();
    }

    public ResourceOfferingType createResourceOfferingType() {
        return new ResourceOfferingType();
    }

    public OptionsType createOptionsType() {
        return new OptionsType();
    }

    public ModifyType createModifyType() {
        return new ModifyType();
    }

    public DirectiveType createDirectiveType() {
        return new DirectiveType();
    }

    public ModifyResponseType createModifyResponseType() {
        return new ModifyResponseType();
    }

    public EmptyType createEmptyType() {
        return new EmptyType();
    }

    public DescriptionType createDescriptionType() {
        return new DescriptionType();
    }

    public ServiceInstanceType createServiceInstanceType() {
        return new ServiceInstanceType();
    }

    public RemoveEntryType createRemoveEntryType() {
        return new RemoveEntryType();
    }

    public QueryResponseType.Credentials createQueryResponseTypeCredentials() {
        return new QueryResponseType.Credentials();
    }

    public QueryType.RequestedServiceType createQueryTypeRequestedServiceType() {
        return new QueryType.RequestedServiceType();
    }

    // ---- no-arg Element factory methods (JAXB 1.x compat) ----

    public AuthenticateRequesterElement createAuthenticateRequesterElement() {
        return new AuthenticateRequesterElement();
    }

    public AuthorizeRequesterElement createAuthorizeRequesterElement() {
        return new AuthorizeRequesterElement();
    }

    public AuthenticateSessionContextElement createAuthenticateSessionContextElement() {
        return new AuthenticateSessionContextElement();
    }

    public EncryptResourceIDElement createEncryptResourceIDElement() {
        return new EncryptResourceIDElement();
    }

    // ---- JAXBElement wrapping methods ----

    @XmlElementDecl(namespace = "urn:liberty:disco:2003-08", name = "Status")
    public JAXBElement<StatusType> createStatus(StatusType value) {
        return new JAXBElement<StatusType>(_Status_QNAME, StatusType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2003-08", name = "Extension")
    public JAXBElement<ExtensionType> createExtension(ExtensionType value) {
        return new JAXBElement<ExtensionType>(_Extension_QNAME, ExtensionType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2003-08", name = "ServiceType")
    public JAXBElement<String> createServiceType(String value) {
        return new JAXBElement<String>(_ServiceType_QNAME, String.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2003-08", name = "ResourceID")
    public JAXBElement<ResourceIDType> createResourceID(ResourceIDType value) {
        return new JAXBElement<ResourceIDType>(_ResourceID_QNAME, ResourceIDType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2003-08", name = "EncryptedResourceID")
    public JAXBElement<EncryptedResourceIDType> createEncryptedResourceID(EncryptedResourceIDType value) {
        return new JAXBElement<EncryptedResourceIDType>(_EncryptedResourceID_QNAME, EncryptedResourceIDType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2003-08", name = "ResourceOffering")
    public JAXBElement<ResourceOfferingType> createResourceOffering(ResourceOfferingType value) {
        return new JAXBElement<ResourceOfferingType>(_ResourceOffering_QNAME, ResourceOfferingType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2003-08", name = "Options")
    public JAXBElement<OptionsType> createOptions(OptionsType value) {
        return new JAXBElement<OptionsType>(_Options_QNAME, OptionsType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2003-08", name = "Query")
    public JAXBElement<QueryType> createQuery(QueryType value) {
        return new JAXBElement<QueryType>(_Query_QNAME, QueryType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2003-08", name = "QueryResponse")
    public JAXBElement<QueryResponseType> createQueryResponse(QueryResponseType value) {
        return new JAXBElement<QueryResponseType>(_QueryResponse_QNAME, QueryResponseType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2003-08", name = "Modify")
    public JAXBElement<ModifyType> createModify(ModifyType value) {
        return new JAXBElement<ModifyType>(_Modify_QNAME, ModifyType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2003-08", name = "AuthenticateRequester")
    public JAXBElement<DirectiveType> createAuthenticateRequester(DirectiveType value) {
        return new JAXBElement<DirectiveType>(_AuthenticateRequester_QNAME, DirectiveType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2003-08", name = "AuthorizeRequester")
    public JAXBElement<DirectiveType> createAuthorizeRequester(DirectiveType value) {
        return new JAXBElement<DirectiveType>(_AuthorizeRequester_QNAME, DirectiveType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2003-08", name = "AuthenticateSessionContext")
    public JAXBElement<DirectiveType> createAuthenticateSessionContext(DirectiveType value) {
        return new JAXBElement<DirectiveType>(_AuthenticateSessionContext_QNAME, DirectiveType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2003-08", name = "EncryptResourceID")
    public JAXBElement<DirectiveType> createEncryptResourceID(DirectiveType value) {
        return new JAXBElement<DirectiveType>(_EncryptResourceID_QNAME, DirectiveType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2003-08", name = "ModifyResponse")
    public JAXBElement<ModifyResponseType> createModifyResponse(ModifyResponseType value) {
        return new JAXBElement<ModifyResponseType>(_ModifyResponse_QNAME, ModifyResponseType.class, null, value);
    }

    @XmlElementDecl(namespace = "urn:liberty:disco:2003-08", name = "CredentialRef", scope = DescriptionType.class)
    @XmlIDREF
    public JAXBElement<Object> createDescriptionTypeCredentialRef(Object value) {
        return new JAXBElement<Object>(_DescriptionTypeCredentialRef_QNAME, Object.class, DescriptionType.class, value);
    }
}
