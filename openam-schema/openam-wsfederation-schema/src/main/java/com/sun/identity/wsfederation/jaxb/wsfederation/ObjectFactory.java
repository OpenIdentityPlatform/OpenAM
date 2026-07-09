
package com.sun.identity.wsfederation.jaxb.wsfederation;

import javax.xml.namespace.QName;
import com.sun.identity.wsfederation.jaxb.wsaddr.EndpointReferenceType;
import com.sun.identity.wsfederation.jaxb.wsspolicy.NestedPolicyType;
import com.sun.identity.wsfederation.jaxb.wsspolicy.TokenAssertionType;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.sun.identity.wsfederation.jaxb.wsfederation package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _FederationMetadata_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "FederationMetadata");
    private final static QName _Federation_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "Federation");
    private final static QName _TokenSigningKeyInfo_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "TokenSigningKeyInfo");
    private final static QName _TokenKeyTransferKeyInfo_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "TokenKeyTransferKeyInfo");
    private final static QName _IssuerNamesOffered_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "IssuerNamesOffered");
    private final static QName _TokenIssuerName_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "TokenIssuerName");
    private final static QName _TokenIssuerEndpoint_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "TokenIssuerEndpoint");
    private final static QName _PsuedonymServiceEndpoint_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "PsuedonymServiceEndpoint");
    private final static QName _AttributeServiceEndpoint_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "AttributeServiceEndpoint");
    private final static QName _SingleSignOutSubscriptionEndpoint_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "SingleSignOutSubscriptionEndpoint");
    private final static QName _SingleSignOutNotificationEndpoint_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "SingleSignOutNotificationEndpoint");
    private final static QName _TokenTypesOffered_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "TokenTypesOffered");
    private final static QName _UriNamedClaimTypesOffered_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "UriNamedClaimTypesOffered");
    private final static QName _AutomaticPseudonyms_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "AutomaticPseudonyms");
    private final static QName _FederationMetadataHandler_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "FederationMetadataHandler");
    private final static QName _SignOut_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "SignOut");
    private final static QName _Realm_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "Realm");
    private final static QName _FilterPseudonyms_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "FilterPseudonyms");
    private final static QName _PseudonymBasis_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "PseudonymBasis");
    private final static QName _RelativeTo_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "RelativeTo");
    private final static QName _Pseudonym_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "Pseudonym");
    private final static QName _SecurityToken_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "SecurityToken");
    private final static QName _ProofToken_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "ProofToken");
    private final static QName _RequestPseudonym_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "RequestPseudonym");
    private final static QName _ReferenceToken_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "ReferenceToken");
    private final static QName _FederationID_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "FederationID");
    private final static QName _RequestProofToken_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "RequestProofToken");
    private final static QName _ClientPseudonym_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "ClientPseudonym");
    private final static QName _Freshness_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "Freshness");
    private final static QName _RequireReferenceToken_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "RequireReferenceToken");
    private final static QName _ReferenceToken11_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "ReferenceToken11");
    private final static QName _WebBinding_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "WebBinding");
    private final static QName _AuthenticationToken_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "AuthenticationToken");
    private final static QName _RequireSignedTokens_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "RequireSignedTokens");
    private final static QName _RequireBearerTokens_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "RequireBearerTokens");
    private final static QName _RequireSharedCookies_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "RequireSharedCookies");
    private final static QName _RequiresGenericClaimDialect_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "RequiresGenericClaimDialect");
    private final static QName _IssuesSpecificPolicyFault_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "IssuesSpecificPolicyFault");
    private final static QName _AdditionalContextProcessed_QNAME = new QName("http://schemas.xmlsoap.org/ws/2006/12/federation", "AdditionalContextProcessed");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.sun.identity.wsfederation.jaxb.wsfederation
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link FederationMetadataType }
     * 
     */
    public FederationMetadataType createFederationMetadataType() {
        return new FederationMetadataType();
    }

    /**
     * Create an instance of {@link FederationType }
     * 
     */
    public FederationType createFederationType() {
        return new FederationType();
    }

    /**
     * Create an instance of {@link TokenKeyInfoType }
     * 
     */
    public TokenKeyInfoType createTokenKeyInfoType() {
        return new TokenKeyInfoType();
    }

    /**
     * Create an instance of {@link IssuerNamesOfferedType }
     * 
     */
    public IssuerNamesOfferedType createIssuerNamesOfferedType() {
        return new IssuerNamesOfferedType();
    }

    /**
     * Create an instance of {@link AttributeExtensibleURI }
     * 
     */
    public AttributeExtensibleURI createAttributeExtensibleURI() {
        return new AttributeExtensibleURI();
    }

    /**
     * Create an instance of {@link TokenTypesOfferedType }
     * 
     */
    public TokenTypesOfferedType createTokenTypesOfferedType() {
        return new TokenTypesOfferedType();
    }

    /**
     * Create an instance of {@link UriNamedClaimTypesOfferedType }
     * 
     */
    public UriNamedClaimTypesOfferedType createUriNamedClaimTypesOfferedType() {
        return new UriNamedClaimTypesOfferedType();
    }

    /**
     * Create an instance of {@link FederationMetadataHandlerType }
     * 
     */
    public FederationMetadataHandlerType createFederationMetadataHandlerType() {
        return new FederationMetadataHandlerType();
    }

    /**
     * Create an instance of {@link SignOutType }
     * 
     */
    public SignOutType createSignOutType() {
        return new SignOutType();
    }

    /**
     * Create an instance of {@link FilterPseudonymsType }
     * 
     */
    public FilterPseudonymsType createFilterPseudonymsType() {
        return new FilterPseudonymsType();
    }

    /**
     * Create an instance of {@link PseudonymBasisType }
     * 
     */
    public PseudonymBasisType createPseudonymBasisType() {
        return new PseudonymBasisType();
    }

    /**
     * Create an instance of {@link RelativeToType }
     * 
     */
    public RelativeToType createRelativeToType() {
        return new RelativeToType();
    }

    /**
     * Create an instance of {@link PseudonymType }
     * 
     */
    public PseudonymType createPseudonymType() {
        return new PseudonymType();
    }

    /**
     * Create an instance of {@link SecurityTokenType }
     * 
     */
    public SecurityTokenType createSecurityTokenType() {
        return new SecurityTokenType();
    }

    /**
     * Create an instance of {@link ProofTokenType }
     * 
     */
    public ProofTokenType createProofTokenType() {
        return new ProofTokenType();
    }

    /**
     * Create an instance of {@link RequestPseudonymType }
     * 
     */
    public RequestPseudonymType createRequestPseudonymType() {
        return new RequestPseudonymType();
    }

    /**
     * Create an instance of {@link ReferenceTokenType }
     * 
     */
    public ReferenceTokenType createReferenceTokenType() {
        return new ReferenceTokenType();
    }

    /**
     * Create an instance of {@link RequestProofTokenType }
     * 
     */
    public RequestProofTokenType createRequestProofTokenType() {
        return new RequestProofTokenType();
    }

    /**
     * Create an instance of {@link ClientPseudonymType }
     * 
     */
    public ClientPseudonymType createClientPseudonymType() {
        return new ClientPseudonymType();
    }

    /**
     * Create an instance of {@link Freshness }
     * 
     */
    public Freshness createFreshness() {
        return new Freshness();
    }

    /**
     * Create an instance of {@link AssertionType }
     * 
     */
    public AssertionType createAssertionType() {
        return new AssertionType();
    }

    /**
     * Create an instance of {@link IssuerNameType }
     * 
     */
    public IssuerNameType createIssuerNameType() {
        return new IssuerNameType();
    }

    /**
     * Create an instance of {@link TokenType }
     * 
     */
    public TokenType createTokenType() {
        return new TokenType();
    }

    /**
     * Create an instance of {@link ClaimType }
     * 
     */
    public ClaimType createClaimType() {
        return new ClaimType();
    }

    /**
     * Create an instance of {@link DisplayNameType }
     * 
     */
    public DisplayNameType createDisplayNameType() {
        return new DisplayNameType();
    }

    /**
     * Create an instance of {@link DescriptionType }
     * 
     */
    public DescriptionType createDescriptionType() {
        return new DescriptionType();
    }

    /**
     * Create an instance of {@link SignOutBasisType }
     * 
     */
    public SignOutBasisType createSignOutBasisType() {
        return new SignOutBasisType();
    }

    /**
     * Create an instance of {@link ReferenceDigestType }
     * 
     */
    public ReferenceDigestType createReferenceDigestType() {
        return new ReferenceDigestType();
    }

    /**
     * Create an instance of {@link AttributeExtensibleString }
     * 
     */
    public AttributeExtensibleString createAttributeExtensibleString() {
        return new AttributeExtensibleString();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FederationMetadataType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link FederationMetadataType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "FederationMetadata")
    public JAXBElement<FederationMetadataType> createFederationMetadata(FederationMetadataType value) {
        return new JAXBElement<FederationMetadataType>(_FederationMetadata_QNAME, FederationMetadataType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FederationType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link FederationType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "Federation")
    public JAXBElement<FederationType> createFederation(FederationType value) {
        return new JAXBElement<FederationType>(_Federation_QNAME, FederationType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TokenKeyInfoType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link TokenKeyInfoType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "TokenSigningKeyInfo")
    public JAXBElement<TokenKeyInfoType> createTokenSigningKeyInfo(TokenKeyInfoType value) {
        return new JAXBElement<TokenKeyInfoType>(_TokenSigningKeyInfo_QNAME, TokenKeyInfoType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TokenKeyInfoType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link TokenKeyInfoType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "TokenKeyTransferKeyInfo")
    public JAXBElement<TokenKeyInfoType> createTokenKeyTransferKeyInfo(TokenKeyInfoType value) {
        return new JAXBElement<TokenKeyInfoType>(_TokenKeyTransferKeyInfo_QNAME, TokenKeyInfoType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IssuerNamesOfferedType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link IssuerNamesOfferedType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "IssuerNamesOffered")
    public JAXBElement<IssuerNamesOfferedType> createIssuerNamesOffered(IssuerNamesOfferedType value) {
        return new JAXBElement<IssuerNamesOfferedType>(_IssuerNamesOffered_QNAME, IssuerNamesOfferedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AttributeExtensibleURI }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AttributeExtensibleURI }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "TokenIssuerName")
    public JAXBElement<AttributeExtensibleURI> createTokenIssuerName(AttributeExtensibleURI value) {
        return new JAXBElement<AttributeExtensibleURI>(_TokenIssuerName_QNAME, AttributeExtensibleURI.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "TokenIssuerEndpoint")
    public JAXBElement<EndpointReferenceType> createTokenIssuerEndpoint(EndpointReferenceType value) {
        return new JAXBElement<EndpointReferenceType>(_TokenIssuerEndpoint_QNAME, EndpointReferenceType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "PsuedonymServiceEndpoint")
    public JAXBElement<EndpointReferenceType> createPsuedonymServiceEndpoint(EndpointReferenceType value) {
        return new JAXBElement<EndpointReferenceType>(_PsuedonymServiceEndpoint_QNAME, EndpointReferenceType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "AttributeServiceEndpoint")
    public JAXBElement<EndpointReferenceType> createAttributeServiceEndpoint(EndpointReferenceType value) {
        return new JAXBElement<EndpointReferenceType>(_AttributeServiceEndpoint_QNAME, EndpointReferenceType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "SingleSignOutSubscriptionEndpoint")
    public JAXBElement<EndpointReferenceType> createSingleSignOutSubscriptionEndpoint(EndpointReferenceType value) {
        return new JAXBElement<EndpointReferenceType>(_SingleSignOutSubscriptionEndpoint_QNAME, EndpointReferenceType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link EndpointReferenceType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "SingleSignOutNotificationEndpoint")
    public JAXBElement<EndpointReferenceType> createSingleSignOutNotificationEndpoint(EndpointReferenceType value) {
        return new JAXBElement<EndpointReferenceType>(_SingleSignOutNotificationEndpoint_QNAME, EndpointReferenceType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TokenTypesOfferedType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link TokenTypesOfferedType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "TokenTypesOffered")
    public JAXBElement<TokenTypesOfferedType> createTokenTypesOffered(TokenTypesOfferedType value) {
        return new JAXBElement<TokenTypesOfferedType>(_TokenTypesOffered_QNAME, TokenTypesOfferedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link UriNamedClaimTypesOfferedType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link UriNamedClaimTypesOfferedType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "UriNamedClaimTypesOffered")
    public JAXBElement<UriNamedClaimTypesOfferedType> createUriNamedClaimTypesOffered(UriNamedClaimTypesOfferedType value) {
        return new JAXBElement<UriNamedClaimTypesOfferedType>(_UriNamedClaimTypesOffered_QNAME, UriNamedClaimTypesOfferedType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link Boolean }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "AutomaticPseudonyms")
    public JAXBElement<Boolean> createAutomaticPseudonyms(Boolean value) {
        return new JAXBElement<Boolean>(_AutomaticPseudonyms_QNAME, Boolean.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FederationMetadataHandlerType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link FederationMetadataHandlerType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "FederationMetadataHandler")
    public JAXBElement<FederationMetadataHandlerType> createFederationMetadataHandler(FederationMetadataHandlerType value) {
        return new JAXBElement<FederationMetadataHandlerType>(_FederationMetadataHandler_QNAME, FederationMetadataHandlerType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SignOutType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link SignOutType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "SignOut")
    public JAXBElement<SignOutType> createSignOut(SignOutType value) {
        return new JAXBElement<SignOutType>(_SignOut_QNAME, SignOutType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link String }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "Realm")
    public JAXBElement<String> createRealm(String value) {
        return new JAXBElement<String>(_Realm_QNAME, String.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link FilterPseudonymsType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link FilterPseudonymsType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "FilterPseudonyms")
    public JAXBElement<FilterPseudonymsType> createFilterPseudonyms(FilterPseudonymsType value) {
        return new JAXBElement<FilterPseudonymsType>(_FilterPseudonyms_QNAME, FilterPseudonymsType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PseudonymBasisType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link PseudonymBasisType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "PseudonymBasis")
    public JAXBElement<PseudonymBasisType> createPseudonymBasis(PseudonymBasisType value) {
        return new JAXBElement<PseudonymBasisType>(_PseudonymBasis_QNAME, PseudonymBasisType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RelativeToType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RelativeToType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "RelativeTo")
    public JAXBElement<RelativeToType> createRelativeTo(RelativeToType value) {
        return new JAXBElement<RelativeToType>(_RelativeTo_QNAME, RelativeToType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link PseudonymType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link PseudonymType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "Pseudonym")
    public JAXBElement<PseudonymType> createPseudonym(PseudonymType value) {
        return new JAXBElement<PseudonymType>(_Pseudonym_QNAME, PseudonymType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SecurityTokenType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link SecurityTokenType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "SecurityToken")
    public JAXBElement<SecurityTokenType> createSecurityToken(SecurityTokenType value) {
        return new JAXBElement<SecurityTokenType>(_SecurityToken_QNAME, SecurityTokenType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ProofTokenType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ProofTokenType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "ProofToken")
    public JAXBElement<ProofTokenType> createProofToken(ProofTokenType value) {
        return new JAXBElement<ProofTokenType>(_ProofToken_QNAME, ProofTokenType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RequestPseudonymType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RequestPseudonymType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "RequestPseudonym")
    public JAXBElement<RequestPseudonymType> createRequestPseudonym(RequestPseudonymType value) {
        return new JAXBElement<RequestPseudonymType>(_RequestPseudonym_QNAME, RequestPseudonymType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ReferenceTokenType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ReferenceTokenType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "ReferenceToken")
    public JAXBElement<ReferenceTokenType> createReferenceToken(ReferenceTokenType value) {
        return new JAXBElement<ReferenceTokenType>(_ReferenceToken_QNAME, ReferenceTokenType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AttributeExtensibleURI }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AttributeExtensibleURI }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "FederationID")
    public JAXBElement<AttributeExtensibleURI> createFederationID(AttributeExtensibleURI value) {
        return new JAXBElement<AttributeExtensibleURI>(_FederationID_QNAME, AttributeExtensibleURI.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link RequestProofTokenType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link RequestProofTokenType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "RequestProofToken")
    public JAXBElement<RequestProofTokenType> createRequestProofToken(RequestProofTokenType value) {
        return new JAXBElement<RequestProofTokenType>(_RequestProofToken_QNAME, RequestProofTokenType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link ClientPseudonymType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link ClientPseudonymType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "ClientPseudonym")
    public JAXBElement<ClientPseudonymType> createClientPseudonym(ClientPseudonymType value) {
        return new JAXBElement<ClientPseudonymType>(_ClientPseudonym_QNAME, ClientPseudonymType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Freshness }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link Freshness }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "Freshness")
    public JAXBElement<Freshness> createFreshness(Freshness value) {
        return new JAXBElement<Freshness>(_Freshness_QNAME, Freshness.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TokenAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link TokenAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "RequireReferenceToken")
    public JAXBElement<TokenAssertionType> createRequireReferenceToken(TokenAssertionType value) {
        return new JAXBElement<TokenAssertionType>(_RequireReferenceToken_QNAME, TokenAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "ReferenceToken11")
    public JAXBElement<AssertionType> createReferenceToken11(AssertionType value) {
        return new JAXBElement<AssertionType>(_ReferenceToken11_QNAME, AssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "WebBinding")
    public JAXBElement<NestedPolicyType> createWebBinding(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_WebBinding_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "AuthenticationToken")
    public JAXBElement<NestedPolicyType> createAuthenticationToken(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_AuthenticationToken_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "RequireSignedTokens")
    public JAXBElement<AssertionType> createRequireSignedTokens(AssertionType value) {
        return new JAXBElement<AssertionType>(_RequireSignedTokens_QNAME, AssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "RequireBearerTokens")
    public JAXBElement<AssertionType> createRequireBearerTokens(AssertionType value) {
        return new JAXBElement<AssertionType>(_RequireBearerTokens_QNAME, AssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "RequireSharedCookies")
    public JAXBElement<AssertionType> createRequireSharedCookies(AssertionType value) {
        return new JAXBElement<AssertionType>(_RequireSharedCookies_QNAME, AssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "RequiresGenericClaimDialect")
    public JAXBElement<AssertionType> createRequiresGenericClaimDialect(AssertionType value) {
        return new JAXBElement<AssertionType>(_RequiresGenericClaimDialect_QNAME, AssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "IssuesSpecificPolicyFault")
    public JAXBElement<AssertionType> createIssuesSpecificPolicyFault(AssertionType value) {
        return new JAXBElement<AssertionType>(_IssuesSpecificPolicyFault_QNAME, AssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link AssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link AssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://schemas.xmlsoap.org/ws/2006/12/federation", name = "AdditionalContextProcessed")
    public JAXBElement<AssertionType> createAdditionalContextProcessed(AssertionType value) {
        return new JAXBElement<AssertionType>(_AdditionalContextProcessed_QNAME, AssertionType.class, null, value);
    }

    // ---- No-arg element factory methods for JAXB 1.x compatibility ----

    public FederationElement createFederationElement() { return new FederationElement(); }
    public FederationMetadataElement createFederationMetadataElement() { return new FederationMetadataElement(); }
    public TokenSigningKeyInfoElement createTokenSigningKeyInfoElement() { return new TokenSigningKeyInfoElement(); }
    public TokenIssuerNameElement createTokenIssuerNameElement() { return new TokenIssuerNameElement(); }
    public TokenIssuerEndpointElement createTokenIssuerEndpointElement() { return new TokenIssuerEndpointElement(); }
    public TokenTypesOfferedElement createTokenTypesOfferedElement() { return new TokenTypesOfferedElement(); }
    public UriNamedClaimTypesOfferedElement createUriNamedClaimTypesOfferedElement() { return new UriNamedClaimTypesOfferedElement(); }
    public SingleSignOutNotificationEndpointElement createSingleSignOutNotificationEndpointElement() { return new SingleSignOutNotificationEndpointElement(); }

}
