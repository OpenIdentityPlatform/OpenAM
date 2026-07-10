
package com.sun.identity.wsfederation.jaxb.wsspolicy;

import javax.xml.namespace.QName;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.annotation.XmlElementDecl;
import jakarta.xml.bind.annotation.XmlRegistry;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the com.sun.identity.wsfederation.jaxb.wsspolicy package. 
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

    private final static QName _SignedParts_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "SignedParts");
    private final static QName _EncryptedParts_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "EncryptedParts");
    private final static QName _SignedElements_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "SignedElements");
    private final static QName _EncryptedElements_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "EncryptedElements");
    private final static QName _RequiredElements_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RequiredElements");
    private final static QName _UsernameToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "UsernameToken");
    private final static QName _NoPassword_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "NoPassword");
    private final static QName _HashPassword_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "HashPassword");
    private final static QName _WssUsernameToken10_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssUsernameToken10");
    private final static QName _WssUsernameToken11_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssUsernameToken11");
    private final static QName _IssuedToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "IssuedToken");
    private final static QName _RequireDerivedKeys_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RequireDerivedKeys");
    private final static QName _RequireImplicitDerivedKeys_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RequireImplicitDerivedKeys");
    private final static QName _RequireExplicitDerivedKeys_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RequireExplicitDerivedKeys");
    private final static QName _RequireExternalReference_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RequireExternalReference");
    private final static QName _RequireInternalReference_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RequireInternalReference");
    private final static QName _X509Token_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "X509Token");
    private final static QName _RequireKeyIdentifierReference_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RequireKeyIdentifierReference");
    private final static QName _RequireIssuerSerialReference_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RequireIssuerSerialReference");
    private final static QName _RequireEmbeddedTokenReference_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RequireEmbeddedTokenReference");
    private final static QName _RequireThumbprintReference_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RequireThumbprintReference");
    private final static QName _WssX509V3Token10_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssX509V3Token10");
    private final static QName _WssX509Pkcs7Token10_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssX509Pkcs7Token10");
    private final static QName _WssX509PkiPathV1Token10_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssX509PkiPathV1Token10");
    private final static QName _WssX509V1Token11_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssX509V1Token11");
    private final static QName _WssX509V3Token11_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssX509V3Token11");
    private final static QName _WssX509Pkcs7Token11_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssX509Pkcs7Token11");
    private final static QName _WssX509PkiPathV1Token11_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssX509PkiPathV1Token11");
    private final static QName _KerberosToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "KerberosToken");
    private final static QName _WssKerberosV5ApReqToken11_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssKerberosV5ApReqToken11");
    private final static QName _WssGssKerberosV5ApReqToken11_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssGssKerberosV5ApReqToken11");
    private final static QName _SpnegoContextToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "SpnegoContextToken");
    private final static QName _SecurityContextToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "SecurityContextToken");
    private final static QName _RequireExternalUriReference_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RequireExternalUriReference");
    private final static QName _SC200502SecurityContextToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "SC200502SecurityContextToken");
    private final static QName _SecureConversationToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "SecureConversationToken");
    private final static QName _BootstrapPolicy_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "BootstrapPolicy");
    private final static QName _SamlToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "SamlToken");
    private final static QName _WssSamlV11Token10_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssSamlV11Token10");
    private final static QName _WssSamlV11Token11_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssSamlV11Token11");
    private final static QName _WssSamlV20Token11_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssSamlV20Token11");
    private final static QName _RelToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RelToken");
    private final static QName _WssRelV10Token10_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssRelV10Token10");
    private final static QName _WssRelV20Token10_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssRelV20Token10");
    private final static QName _WssRelV10Token11_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssRelV10Token11");
    private final static QName _WssRelV20Token11_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "WssRelV20Token11");
    private final static QName _HttpsToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "HttpsToken");
    private final static QName _HttpBasicAuthentication_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "HttpBasicAuthentication");
    private final static QName _HttpDigestAuthentication_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "HttpDigestAuthentication");
    private final static QName _RequireClientCertificate_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RequireClientCertificate");
    private final static QName _AlgorithmSuite_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "AlgorithmSuite");
    private final static QName _Basic256_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Basic256");
    private final static QName _Basic192_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Basic192");
    private final static QName _Basic128_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Basic128");
    private final static QName _TripleDes_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "TripleDes");
    private final static QName _Basic256Rsa15_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Basic256Rsa15");
    private final static QName _Basic192Rsa15_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Basic192Rsa15");
    private final static QName _Basic128Rsa15_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Basic128Rsa15");
    private final static QName _TripleDesRsa15_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "TripleDesRsa15");
    private final static QName _Basic256Sha256_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Basic256Sha256");
    private final static QName _Basic192Sha256_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Basic192Sha256");
    private final static QName _Basic128Sha256_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Basic128Sha256");
    private final static QName _TripleDesSha256_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "TripleDesSha256");
    private final static QName _Basic256Sha256Rsa15_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Basic256Sha256Rsa15");
    private final static QName _Basic192Sha256Rsa15_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Basic192Sha256Rsa15");
    private final static QName _Basic128Sha256Rsa15_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Basic128Sha256Rsa15");
    private final static QName _TripleDesSha256Rsa15_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "TripleDesSha256Rsa15");
    private final static QName _InclusiveC14N_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "InclusiveC14N");
    private final static QName _SOAPNormalization10_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "SOAPNormalization10");
    private final static QName _STRTransform10_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "STRTransform10");
    private final static QName _XPath10_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "XPath10");
    private final static QName _XPathFilter20_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "XPathFilter20");
    private final static QName _AbsXPath_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "AbsXPath");
    private final static QName _Layout_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Layout");
    private final static QName _Strict_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Strict");
    private final static QName _Lax_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Lax");
    private final static QName _LaxTsFirst_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "LaxTsFirst");
    private final static QName _LaxTsLast_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "LaxTsLast");
    private final static QName _TransportBinding_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "TransportBinding");
    private final static QName _TransportToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "TransportToken");
    private final static QName _IncludeTimestamp_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "IncludeTimestamp");
    private final static QName _SymmetricBinding_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "SymmetricBinding");
    private final static QName _EncryptionToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "EncryptionToken");
    private final static QName _SignatureToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "SignatureToken");
    private final static QName _ProtectionToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "ProtectionToken");
    private final static QName _EncryptBeforeSigning_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "EncryptBeforeSigning");
    private final static QName _EncryptSignature_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "EncryptSignature");
    private final static QName _ProtectTokens_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "ProtectTokens");
    private final static QName _OnlySignEntireHeadersAndBody_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "OnlySignEntireHeadersAndBody");
    private final static QName _AsymmetricBinding_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "AsymmetricBinding");
    private final static QName _InitiatorToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "InitiatorToken");
    private final static QName _InitiatorSignatureToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "InitiatorSignatureToken");
    private final static QName _InitiatorEncryptionToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "InitiatorEncryptionToken");
    private final static QName _RecipientToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RecipientToken");
    private final static QName _RecipientSignatureToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RecipientSignatureToken");
    private final static QName _RecipientEncryptionToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RecipientEncryptionToken");
    private final static QName _SupportingTokens_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "SupportingTokens");
    private final static QName _SignedSupportingTokens_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "SignedSupportingTokens");
    private final static QName _EndorsingSupportingTokens_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "EndorsingSupportingTokens");
    private final static QName _SignedEndorsingSupportingTokens_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "SignedEndorsingSupportingTokens");
    private final static QName _SignedEncryptedSupportingTokens_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "SignedEncryptedSupportingTokens");
    private final static QName _EndorsingEncryptedSupportingTokens_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "EndorsingEncryptedSupportingTokens");
    private final static QName _SignedEndorsingEncryptedSupportingTokens_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "SignedEndorsingEncryptedSupportingTokens");
    private final static QName _Wss10_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Wss10");
    private final static QName _MustSupportRefKeyIdentifier_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "MustSupportRefKeyIdentifier");
    private final static QName _MustSupportRefIssuerSerial_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "MustSupportRefIssuerSerial");
    private final static QName _MustSupportRefExternalURI_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "MustSupportRefExternalURI");
    private final static QName _MustSupportRefEmbeddedToken_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "MustSupportRefEmbeddedToken");
    private final static QName _Wss11_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Wss11");
    private final static QName _MustSupportRefThumbprint_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "MustSupportRefThumbprint");
    private final static QName _MustSupportRefEncryptedKey_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "MustSupportRefEncryptedKey");
    private final static QName _RequireSignatureConfirmation_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RequireSignatureConfirmation");
    private final static QName _Trust10_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "Trust10");
    private final static QName _MustSupportClientChallenge_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "MustSupportClientChallenge");
    private final static QName _MustSupportServerChallenge_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "MustSupportServerChallenge");
    private final static QName _RequireClientEntropy_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RequireClientEntropy");
    private final static QName _RequireServerEntropy_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RequireServerEntropy");
    private final static QName _MustSupportIssuedTokens_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "MustSupportIssuedTokens");
    private final static QName _RequireRequestSecurityTokenCollection_QNAME = new QName("http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", "RequireRequestSecurityTokenCollection");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: com.sun.identity.wsfederation.jaxb.wsspolicy
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link TokenAssertionType }
     * 
     */
    public TokenAssertionType createTokenAssertionType() {
        return new TokenAssertionType();
    }

    /**
     * Create an instance of {@link NestedPolicyType }
     * 
     */
    public NestedPolicyType createNestedPolicyType() {
        return new NestedPolicyType();
    }

    /**
     * Create an instance of {@link SePartsType }
     * 
     */
    public SePartsType createSePartsType() {
        return new SePartsType();
    }

    /**
     * Create an instance of {@link SerElementsType }
     * 
     */
    public SerElementsType createSerElementsType() {
        return new SerElementsType();
    }

    /**
     * Create an instance of {@link QNameAssertionType }
     * 
     */
    public QNameAssertionType createQNameAssertionType() {
        return new QNameAssertionType();
    }

    /**
     * Create an instance of {@link IssuedTokenType }
     * 
     */
    public IssuedTokenType createIssuedTokenType() {
        return new IssuedTokenType();
    }

    /**
     * Create an instance of {@link SpnegoContextTokenType }
     * 
     */
    public SpnegoContextTokenType createSpnegoContextTokenType() {
        return new SpnegoContextTokenType();
    }

    /**
     * Create an instance of {@link SecureConversationTokenType }
     * 
     */
    public SecureConversationTokenType createSecureConversationTokenType() {
        return new SecureConversationTokenType();
    }

    /**
     * Create an instance of {@link EmptyType }
     * 
     */
    public EmptyType createEmptyType() {
        return new EmptyType();
    }

    /**
     * Create an instance of {@link HeaderType }
     * 
     */
    public HeaderType createHeaderType() {
        return new HeaderType();
    }

    /**
     * Create an instance of {@link RequestSecurityTokenTemplateType }
     * 
     */
    public RequestSecurityTokenTemplateType createRequestSecurityTokenTemplateType() {
        return new RequestSecurityTokenTemplateType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SePartsType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link SePartsType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "SignedParts")
    public JAXBElement<SePartsType> createSignedParts(SePartsType value) {
        return new JAXBElement<SePartsType>(_SignedParts_QNAME, SePartsType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SePartsType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link SePartsType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "EncryptedParts")
    public JAXBElement<SePartsType> createEncryptedParts(SePartsType value) {
        return new JAXBElement<SePartsType>(_EncryptedParts_QNAME, SePartsType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SerElementsType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link SerElementsType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "SignedElements")
    public JAXBElement<SerElementsType> createSignedElements(SerElementsType value) {
        return new JAXBElement<SerElementsType>(_SignedElements_QNAME, SerElementsType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SerElementsType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link SerElementsType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "EncryptedElements")
    public JAXBElement<SerElementsType> createEncryptedElements(SerElementsType value) {
        return new JAXBElement<SerElementsType>(_EncryptedElements_QNAME, SerElementsType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SerElementsType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link SerElementsType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RequiredElements")
    public JAXBElement<SerElementsType> createRequiredElements(SerElementsType value) {
        return new JAXBElement<SerElementsType>(_RequiredElements_QNAME, SerElementsType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TokenAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link TokenAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "UsernameToken")
    public JAXBElement<TokenAssertionType> createUsernameToken(TokenAssertionType value) {
        return new JAXBElement<TokenAssertionType>(_UsernameToken_QNAME, TokenAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "NoPassword")
    public JAXBElement<QNameAssertionType> createNoPassword(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_NoPassword_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "HashPassword")
    public JAXBElement<QNameAssertionType> createHashPassword(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_HashPassword_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssUsernameToken10")
    public JAXBElement<QNameAssertionType> createWssUsernameToken10(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssUsernameToken10_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssUsernameToken11")
    public JAXBElement<QNameAssertionType> createWssUsernameToken11(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssUsernameToken11_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link IssuedTokenType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link IssuedTokenType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "IssuedToken")
    public JAXBElement<IssuedTokenType> createIssuedToken(IssuedTokenType value) {
        return new JAXBElement<IssuedTokenType>(_IssuedToken_QNAME, IssuedTokenType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RequireDerivedKeys")
    public JAXBElement<QNameAssertionType> createRequireDerivedKeys(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_RequireDerivedKeys_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RequireImplicitDerivedKeys")
    public JAXBElement<QNameAssertionType> createRequireImplicitDerivedKeys(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_RequireImplicitDerivedKeys_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RequireExplicitDerivedKeys")
    public JAXBElement<QNameAssertionType> createRequireExplicitDerivedKeys(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_RequireExplicitDerivedKeys_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RequireExternalReference")
    public JAXBElement<QNameAssertionType> createRequireExternalReference(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_RequireExternalReference_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RequireInternalReference")
    public JAXBElement<QNameAssertionType> createRequireInternalReference(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_RequireInternalReference_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TokenAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link TokenAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "X509Token")
    public JAXBElement<TokenAssertionType> createX509Token(TokenAssertionType value) {
        return new JAXBElement<TokenAssertionType>(_X509Token_QNAME, TokenAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RequireKeyIdentifierReference")
    public JAXBElement<QNameAssertionType> createRequireKeyIdentifierReference(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_RequireKeyIdentifierReference_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RequireIssuerSerialReference")
    public JAXBElement<QNameAssertionType> createRequireIssuerSerialReference(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_RequireIssuerSerialReference_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RequireEmbeddedTokenReference")
    public JAXBElement<QNameAssertionType> createRequireEmbeddedTokenReference(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_RequireEmbeddedTokenReference_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RequireThumbprintReference")
    public JAXBElement<QNameAssertionType> createRequireThumbprintReference(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_RequireThumbprintReference_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssX509V3Token10")
    public JAXBElement<QNameAssertionType> createWssX509V3Token10(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssX509V3Token10_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssX509Pkcs7Token10")
    public JAXBElement<QNameAssertionType> createWssX509Pkcs7Token10(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssX509Pkcs7Token10_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssX509PkiPathV1Token10")
    public JAXBElement<QNameAssertionType> createWssX509PkiPathV1Token10(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssX509PkiPathV1Token10_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssX509V1Token11")
    public JAXBElement<QNameAssertionType> createWssX509V1Token11(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssX509V1Token11_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssX509V3Token11")
    public JAXBElement<QNameAssertionType> createWssX509V3Token11(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssX509V3Token11_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssX509Pkcs7Token11")
    public JAXBElement<QNameAssertionType> createWssX509Pkcs7Token11(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssX509Pkcs7Token11_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssX509PkiPathV1Token11")
    public JAXBElement<QNameAssertionType> createWssX509PkiPathV1Token11(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssX509PkiPathV1Token11_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TokenAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link TokenAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "KerberosToken")
    public JAXBElement<TokenAssertionType> createKerberosToken(TokenAssertionType value) {
        return new JAXBElement<TokenAssertionType>(_KerberosToken_QNAME, TokenAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssKerberosV5ApReqToken11")
    public JAXBElement<QNameAssertionType> createWssKerberosV5ApReqToken11(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssKerberosV5ApReqToken11_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssGssKerberosV5ApReqToken11")
    public JAXBElement<QNameAssertionType> createWssGssKerberosV5ApReqToken11(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssGssKerberosV5ApReqToken11_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SpnegoContextTokenType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link SpnegoContextTokenType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "SpnegoContextToken")
    public JAXBElement<SpnegoContextTokenType> createSpnegoContextToken(SpnegoContextTokenType value) {
        return new JAXBElement<SpnegoContextTokenType>(_SpnegoContextToken_QNAME, SpnegoContextTokenType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TokenAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link TokenAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "SecurityContextToken")
    public JAXBElement<TokenAssertionType> createSecurityContextToken(TokenAssertionType value) {
        return new JAXBElement<TokenAssertionType>(_SecurityContextToken_QNAME, TokenAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RequireExternalUriReference")
    public JAXBElement<QNameAssertionType> createRequireExternalUriReference(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_RequireExternalUriReference_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "SC200502SecurityContextToken")
    public JAXBElement<QNameAssertionType> createSC200502SecurityContextToken(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_SC200502SecurityContextToken_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link SecureConversationTokenType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link SecureConversationTokenType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "SecureConversationToken")
    public JAXBElement<SecureConversationTokenType> createSecureConversationToken(SecureConversationTokenType value) {
        return new JAXBElement<SecureConversationTokenType>(_SecureConversationToken_QNAME, SecureConversationTokenType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "BootstrapPolicy")
    public JAXBElement<NestedPolicyType> createBootstrapPolicy(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_BootstrapPolicy_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TokenAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link TokenAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "SamlToken")
    public JAXBElement<TokenAssertionType> createSamlToken(TokenAssertionType value) {
        return new JAXBElement<TokenAssertionType>(_SamlToken_QNAME, TokenAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssSamlV11Token10")
    public JAXBElement<QNameAssertionType> createWssSamlV11Token10(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssSamlV11Token10_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssSamlV11Token11")
    public JAXBElement<QNameAssertionType> createWssSamlV11Token11(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssSamlV11Token11_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssSamlV20Token11")
    public JAXBElement<QNameAssertionType> createWssSamlV20Token11(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssSamlV20Token11_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TokenAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link TokenAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RelToken")
    public JAXBElement<TokenAssertionType> createRelToken(TokenAssertionType value) {
        return new JAXBElement<TokenAssertionType>(_RelToken_QNAME, TokenAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssRelV10Token10")
    public JAXBElement<QNameAssertionType> createWssRelV10Token10(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssRelV10Token10_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssRelV20Token10")
    public JAXBElement<QNameAssertionType> createWssRelV20Token10(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssRelV20Token10_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssRelV10Token11")
    public JAXBElement<QNameAssertionType> createWssRelV10Token11(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssRelV10Token11_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "WssRelV20Token11")
    public JAXBElement<QNameAssertionType> createWssRelV20Token11(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_WssRelV20Token11_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link TokenAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link TokenAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "HttpsToken")
    public JAXBElement<TokenAssertionType> createHttpsToken(TokenAssertionType value) {
        return new JAXBElement<TokenAssertionType>(_HttpsToken_QNAME, TokenAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "HttpBasicAuthentication")
    public JAXBElement<QNameAssertionType> createHttpBasicAuthentication(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_HttpBasicAuthentication_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "HttpDigestAuthentication")
    public JAXBElement<QNameAssertionType> createHttpDigestAuthentication(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_HttpDigestAuthentication_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RequireClientCertificate")
    public JAXBElement<QNameAssertionType> createRequireClientCertificate(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_RequireClientCertificate_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "AlgorithmSuite")
    public JAXBElement<NestedPolicyType> createAlgorithmSuite(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_AlgorithmSuite_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Basic256")
    public JAXBElement<QNameAssertionType> createBasic256(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_Basic256_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Basic192")
    public JAXBElement<QNameAssertionType> createBasic192(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_Basic192_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Basic128")
    public JAXBElement<QNameAssertionType> createBasic128(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_Basic128_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "TripleDes")
    public JAXBElement<QNameAssertionType> createTripleDes(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_TripleDes_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Basic256Rsa15")
    public JAXBElement<QNameAssertionType> createBasic256Rsa15(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_Basic256Rsa15_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Basic192Rsa15")
    public JAXBElement<QNameAssertionType> createBasic192Rsa15(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_Basic192Rsa15_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Basic128Rsa15")
    public JAXBElement<QNameAssertionType> createBasic128Rsa15(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_Basic128Rsa15_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "TripleDesRsa15")
    public JAXBElement<QNameAssertionType> createTripleDesRsa15(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_TripleDesRsa15_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Basic256Sha256")
    public JAXBElement<QNameAssertionType> createBasic256Sha256(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_Basic256Sha256_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Basic192Sha256")
    public JAXBElement<QNameAssertionType> createBasic192Sha256(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_Basic192Sha256_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Basic128Sha256")
    public JAXBElement<QNameAssertionType> createBasic128Sha256(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_Basic128Sha256_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "TripleDesSha256")
    public JAXBElement<QNameAssertionType> createTripleDesSha256(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_TripleDesSha256_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Basic256Sha256Rsa15")
    public JAXBElement<QNameAssertionType> createBasic256Sha256Rsa15(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_Basic256Sha256Rsa15_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Basic192Sha256Rsa15")
    public JAXBElement<QNameAssertionType> createBasic192Sha256Rsa15(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_Basic192Sha256Rsa15_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Basic128Sha256Rsa15")
    public JAXBElement<QNameAssertionType> createBasic128Sha256Rsa15(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_Basic128Sha256Rsa15_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "TripleDesSha256Rsa15")
    public JAXBElement<QNameAssertionType> createTripleDesSha256Rsa15(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_TripleDesSha256Rsa15_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "InclusiveC14N")
    public JAXBElement<QNameAssertionType> createInclusiveC14N(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_InclusiveC14N_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "SOAPNormalization10")
    public JAXBElement<QNameAssertionType> createSOAPNormalization10(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_SOAPNormalization10_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "STRTransform10")
    public JAXBElement<QNameAssertionType> createSTRTransform10(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_STRTransform10_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "XPath10")
    public JAXBElement<QNameAssertionType> createXPath10(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_XPath10_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "XPathFilter20")
    public JAXBElement<QNameAssertionType> createXPathFilter20(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_XPathFilter20_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "AbsXPath")
    public JAXBElement<QNameAssertionType> createAbsXPath(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_AbsXPath_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Layout")
    public JAXBElement<NestedPolicyType> createLayout(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_Layout_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Strict")
    public JAXBElement<QNameAssertionType> createStrict(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_Strict_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Lax")
    public JAXBElement<QNameAssertionType> createLax(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_Lax_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "LaxTsFirst")
    public JAXBElement<QNameAssertionType> createLaxTsFirst(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_LaxTsFirst_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "LaxTsLast")
    public JAXBElement<QNameAssertionType> createLaxTsLast(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_LaxTsLast_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "TransportBinding")
    public JAXBElement<NestedPolicyType> createTransportBinding(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_TransportBinding_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "TransportToken")
    public JAXBElement<NestedPolicyType> createTransportToken(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_TransportToken_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "IncludeTimestamp")
    public JAXBElement<QNameAssertionType> createIncludeTimestamp(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_IncludeTimestamp_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "SymmetricBinding")
    public JAXBElement<NestedPolicyType> createSymmetricBinding(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_SymmetricBinding_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "EncryptionToken")
    public JAXBElement<NestedPolicyType> createEncryptionToken(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_EncryptionToken_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "SignatureToken")
    public JAXBElement<NestedPolicyType> createSignatureToken(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_SignatureToken_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "ProtectionToken")
    public JAXBElement<NestedPolicyType> createProtectionToken(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_ProtectionToken_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "EncryptBeforeSigning")
    public JAXBElement<QNameAssertionType> createEncryptBeforeSigning(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_EncryptBeforeSigning_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "EncryptSignature")
    public JAXBElement<QNameAssertionType> createEncryptSignature(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_EncryptSignature_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "ProtectTokens")
    public JAXBElement<QNameAssertionType> createProtectTokens(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_ProtectTokens_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "OnlySignEntireHeadersAndBody")
    public JAXBElement<QNameAssertionType> createOnlySignEntireHeadersAndBody(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_OnlySignEntireHeadersAndBody_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "AsymmetricBinding")
    public JAXBElement<NestedPolicyType> createAsymmetricBinding(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_AsymmetricBinding_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "InitiatorToken")
    public JAXBElement<NestedPolicyType> createInitiatorToken(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_InitiatorToken_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "InitiatorSignatureToken")
    public JAXBElement<NestedPolicyType> createInitiatorSignatureToken(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_InitiatorSignatureToken_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "InitiatorEncryptionToken")
    public JAXBElement<NestedPolicyType> createInitiatorEncryptionToken(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_InitiatorEncryptionToken_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RecipientToken")
    public JAXBElement<NestedPolicyType> createRecipientToken(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_RecipientToken_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RecipientSignatureToken")
    public JAXBElement<NestedPolicyType> createRecipientSignatureToken(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_RecipientSignatureToken_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RecipientEncryptionToken")
    public JAXBElement<NestedPolicyType> createRecipientEncryptionToken(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_RecipientEncryptionToken_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "SupportingTokens")
    public JAXBElement<NestedPolicyType> createSupportingTokens(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_SupportingTokens_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "SignedSupportingTokens")
    public JAXBElement<NestedPolicyType> createSignedSupportingTokens(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_SignedSupportingTokens_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "EndorsingSupportingTokens")
    public JAXBElement<NestedPolicyType> createEndorsingSupportingTokens(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_EndorsingSupportingTokens_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "SignedEndorsingSupportingTokens")
    public JAXBElement<NestedPolicyType> createSignedEndorsingSupportingTokens(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_SignedEndorsingSupportingTokens_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "SignedEncryptedSupportingTokens")
    public JAXBElement<NestedPolicyType> createSignedEncryptedSupportingTokens(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_SignedEncryptedSupportingTokens_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "EndorsingEncryptedSupportingTokens")
    public JAXBElement<NestedPolicyType> createEndorsingEncryptedSupportingTokens(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_EndorsingEncryptedSupportingTokens_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "SignedEndorsingEncryptedSupportingTokens")
    public JAXBElement<NestedPolicyType> createSignedEndorsingEncryptedSupportingTokens(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_SignedEndorsingEncryptedSupportingTokens_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Wss10")
    public JAXBElement<NestedPolicyType> createWss10(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_Wss10_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "MustSupportRefKeyIdentifier")
    public JAXBElement<QNameAssertionType> createMustSupportRefKeyIdentifier(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_MustSupportRefKeyIdentifier_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "MustSupportRefIssuerSerial")
    public JAXBElement<QNameAssertionType> createMustSupportRefIssuerSerial(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_MustSupportRefIssuerSerial_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "MustSupportRefExternalURI")
    public JAXBElement<QNameAssertionType> createMustSupportRefExternalURI(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_MustSupportRefExternalURI_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "MustSupportRefEmbeddedToken")
    public JAXBElement<QNameAssertionType> createMustSupportRefEmbeddedToken(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_MustSupportRefEmbeddedToken_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Wss11")
    public JAXBElement<NestedPolicyType> createWss11(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_Wss11_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "MustSupportRefThumbprint")
    public JAXBElement<QNameAssertionType> createMustSupportRefThumbprint(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_MustSupportRefThumbprint_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "MustSupportRefEncryptedKey")
    public JAXBElement<QNameAssertionType> createMustSupportRefEncryptedKey(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_MustSupportRefEncryptedKey_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RequireSignatureConfirmation")
    public JAXBElement<QNameAssertionType> createRequireSignatureConfirmation(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_RequireSignatureConfirmation_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link NestedPolicyType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "Trust10")
    public JAXBElement<NestedPolicyType> createTrust10(NestedPolicyType value) {
        return new JAXBElement<NestedPolicyType>(_Trust10_QNAME, NestedPolicyType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "MustSupportClientChallenge")
    public JAXBElement<QNameAssertionType> createMustSupportClientChallenge(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_MustSupportClientChallenge_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "MustSupportServerChallenge")
    public JAXBElement<QNameAssertionType> createMustSupportServerChallenge(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_MustSupportServerChallenge_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RequireClientEntropy")
    public JAXBElement<QNameAssertionType> createRequireClientEntropy(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_RequireClientEntropy_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RequireServerEntropy")
    public JAXBElement<QNameAssertionType> createRequireServerEntropy(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_RequireServerEntropy_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "MustSupportIssuedTokens")
    public JAXBElement<QNameAssertionType> createMustSupportIssuedTokens(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_MustSupportIssuedTokens_QNAME, QNameAssertionType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     * 
     * @param value
     *     Java instance representing xml element's value.
     * @return
     *     the new instance of {@link JAXBElement }{@code <}{@link QNameAssertionType }{@code >}
     */
    @XmlElementDecl(namespace = "http://docs.oasis-open.org/ws-sx/ws-securitypolicy/200512", name = "RequireRequestSecurityTokenCollection")
    public JAXBElement<QNameAssertionType> createRequireRequestSecurityTokenCollection(QNameAssertionType value) {
        return new JAXBElement<QNameAssertionType>(_RequireRequestSecurityTokenCollection_QNAME, QNameAssertionType.class, null, value);
    }

    // ---- No-arg element factory methods for JAXB 1.x compatibility ----

    public AsymmetricBindingElement createAsymmetricBindingElement() { return new AsymmetricBindingElement(); }
    public SymmetricBindingElement createSymmetricBindingElement() { return new SymmetricBindingElement(); }
    public InitiatorTokenElement createInitiatorTokenElement() { return new InitiatorTokenElement(); }
    public RecipientTokenElement createRecipientTokenElement() { return new RecipientTokenElement(); }
    public ProtectionTokenElement createProtectionTokenElement() { return new ProtectionTokenElement(); }
    public AlgorithmSuiteElement createAlgorithmSuiteElement() { return new AlgorithmSuiteElement(); }
    public LayoutElement createLayoutElement() { return new LayoutElement(); }
    public X509TokenElement createX509TokenElement() { return new X509TokenElement(); }
    public UsernameTokenElement createUsernameTokenElement() { return new UsernameTokenElement(); }
    public SamlTokenElement createSamlTokenElement() { return new SamlTokenElement(); }
    public KerberosTokenElement createKerberosTokenElement() { return new KerberosTokenElement(); }
    public WssX509V3Token10Element createWssX509V3Token10Element() { return new WssX509V3Token10Element(); }
    public WssUsernameToken10Element createWssUsernameToken10Element() { return new WssUsernameToken10Element(); }
    public WssSamlV20Token11Element createWssSamlV20Token11Element() { return new WssSamlV20Token11Element(); }
    public WssSamlV11Token11Element createWssSamlV11Token11Element() { return new WssSamlV11Token11Element(); }
    public Basic128Element createBasic128Element() { return new Basic128Element(); }
    public Basic192Element createBasic192Element() { return new Basic192Element(); }
    public Basic256Element createBasic256Element() { return new Basic256Element(); }
    public TripleDesElement createTripleDesElement() { return new TripleDesElement(); }
    public WssKerberosV5ApReqToken11Element createWssKerberosV5ApReqToken11Element() { return new WssKerberosV5ApReqToken11Element(); }
    public IncludeTimestampElement createIncludeTimestampElement() { return new IncludeTimestampElement(); }
    public OnlySignEntireHeadersAndBodyElement createOnlySignEntireHeadersAndBodyElement() { return new OnlySignEntireHeadersAndBodyElement(); }
    public LaxElement createLaxElement() { return new LaxElement(); }
    public SignedPartsElement createSignedPartsElement() { return new SignedPartsElement(); }
    public EncryptedPartsElement createEncryptedPartsElement() { return new EncryptedPartsElement(); }
    public IssuedTokenElement createIssuedTokenElement() { return new IssuedTokenElement(); }

}
