/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2009-2010 Sun Microsystems Inc. All Rights Reserved
 * 
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 * 
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 * 
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 * 
 * $Id: Saml2Constants.cs,v 1.6 2010/01/12 18:04:54 ggennaro Exp $
 */
/*
 * Portions Copyrighted 2013-2016 ForgeRock AS.
 */

namespace Sun.Identity.Saml2
{
    /// <summary>
    /// Constants used in the SAMLv2 context.
    /// </summary>
    public static class Saml2Constants
    {
        /// <summary>
        /// Constant for the request parameter to specify AllowCreate
        /// in the AuthnRequest.
        /// </summary>
        public const string AllowCreate = "AllowCreate";

        /// <summary>
        /// Constant for the artifact parameter for SAML responses.
        /// </summary>
        public const string ArtifactParameter = "SAMLart";

        /// <summary>
        /// Constant for the AssertionConsumerServiceIndex parameter.
        /// </summary>
        public const string AssertionConsumerServiceIndex = "AssertionConsumerServiceIndex";

        /// <summary>
        /// Constant for the AuthnContext Class Reference for Password
        /// Protected Transport.
        /// </summary>
        public const string AuthClassRefPasswordProtectedTransport = "urn:oasis:names:tc:SAML:2.0:ac:classes:PasswordProtectedTransport";

        /// <summary>
        /// Constant for the request parameter for specifying the comparison
        /// to be used by AuthnContextClassRef or AuthnContextDeclRef.
        /// </summary>
        public const string AuthComparison = "AuthComparison";

        /// <summary>
        /// Constant for the request parameter for specifying the 
        /// auth level mapped to the corresponding AuthnContextClassRef
        /// within the sp-extended.xml metadata.
        /// </summary>
        public const string AuthLevel = "AuthLevel";

        /// <summary>
        /// Constant for the request parameter for specifying the |
        /// delimited list of values for AuthnContextClassRef.
        /// </summary>
        public const string AuthnContextClassRef = "AuthnContextClassRef";

        /// <summary>
        /// Constant for the request parameter for specifying the |
        /// delimited list of values for AuthnContextDeclRef.
        /// </summary>
        public const string AuthnContextDeclRef = "AuthnContextDeclRef";

        /// <summary>
        /// Constant for the "binding" parameter name.
        /// </summary>
        public const string Binding = "Binding";

        /// <summary>
        /// Constant for specifying certificate usage for encryption.
        /// </summary>
        public const string CertificateForEncryption = "encryption";

        /// <summary>
        /// Constant for specifying certificate usage for signing.
        /// </summary>
        public const string CertificateForSigning = "signing";

        /// <summary>
        /// Constant for the request parameter to specify the consent
        /// in a SAML request.
        /// </summary>
        public const string Consent = "Consent";

        /// <summary>
        /// Constant for the request parameter to specify the destination
        /// in a SAML request.
        /// </summary>
        public const string Destination = "Destination";

        /// <summary>
        /// Constant for the request parameter to specify ForceAuthn
        /// in the AuthnRequest.
        /// </summary>
        public const string ForceAuthn = "ForceAuthn";

        /// <summary>
        /// Constant for specifying HTTP-Artifact protocol binding.
        /// </summary>
        public const string HttpArtifactProtocolBinding = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Artifact";

        /// <summary>
        /// Constant for specifying HTTP-POST protocol binding.
        /// </summary>
        public const string HttpPostProtocolBinding = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST";

        /// <summary>
        /// Constant for specifying HTTP-Redirect protocol binding.
        /// </summary>
        public const string HttpRedirectProtocolBinding = "urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect";

        /// <summary>
        /// Constant for specifying SOAP protocol binding.
        /// </summary>
        public const string HttpSoapProtocolBinding = "urn:oasis:names:tc:SAML:2.0:bindings:SOAP";

        /// <summary>
        /// Constant for defining the length of IDs used in SAMLv2
        /// assertions, requests, and responses.
        /// </summary>
        public const int IdLength = 20;

        /// <summary>
        /// Constant for idpEntityId parameter.
        /// </summary>
        public const string IdpEntityId = "idpEntityID";

        /// <summary>
        /// Constant for the request parameter for specifying if the 
        /// AuthnRequest is passive.
        /// </summary>
        public const string IsPassive = "IsPassive";

        /// <summary>
        /// Constant for the SAMLv2 namespace for metadata.
        /// </summary>
        public const string NamespaceMetadata = "urn:oasis:names:tc:SAML:2.0:metadata";

        /// <summary>
        /// Constant for the SAMLv2 namespace for assertion.
        /// </summary>
        public const string NamespaceSamlAssertion = "urn:oasis:names:tc:SAML:2.0:assertion";

        /// <summary>
        /// Constant for the SAMLv2 namespace for protocol.
        /// </summary>
        public const string NamespaceSamlProtocol = "urn:oasis:names:tc:SAML:2.0:protocol";

        /// <summary>
        /// Constant for the RelayState parameter.
        /// </summary>
        public const string RelayState = "RelayState";

        /// <summary>
        /// Constant for the response parameter for SAML responses.
        /// </summary>
        public const string ResponseParameter = "SAMLResponse";

        /// <summary>
        /// Constant for the request parameter for SAML requests.
        /// </summary>
        public const string RequestParameter = "SAMLRequest";

        /// <summary>
        /// Constant for the reqBinding parameter.
        /// </summary>
        public const string RequestBinding = "ReqBinding";

        /// <summary>
        /// Constant for the SignatureAlgorithm parameter.
        /// </summary>
        public const string SignatureAlgorithm = "SigAlg";

        /// <summary>
        /// Constant for the DSA type of signature algorithm. 
        /// </summary>
        public const string SignatureAlgorithmDsa = "http://www.w3.org/2000/09/xmldsig#dsa-sha1";

        /// <summary>
        /// Constant for the RSA type of signature algorithm with SHA1. 
        /// </summary>
        public const string SignatureAlgorithmRsa = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";

        /// <summary>
        /// Constant for the RSA type of signature algorithm with SHA256. 
        /// </summary>
        public const string SignatureAlgorithmRsa256 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

        /// <summary>
        /// Constant for the RSA type of signature algorithm with SHA384. 
        /// </summary>
        public const string SignatureAlgorithmRsa384 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha384";

        /// <summary>
        /// Constant for the RSA type of signature algorithm with SHA512. 
        /// </summary>
        public const string SignatureAlgorithmRsa512 = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha512";

        /// <summary>
        /// Constant for the Signature parameter.
        /// </summary>
        public const string Signature = "Signature";

        /// <summary>
        /// Constant for SessionIndex parameter.
        /// </summary>
        public const string SessionIndex = "SessionIndex";

        /// <summary>
        /// Constant for SubjectNameId parameter.
        /// </summary>
        public const string SubjectNameId = "SubjectNameId";

        /// <summary>
        /// Constant for status codes used in SAML responses.
        /// </summary>
        public const string Success = "urn:oasis:names:tc:SAML:2.0:status:Success";

        /// <summary>
        /// Constant for the MutualAuthCertAlias parameter.
        /// </summary>
        public const string MutualAuthCertAlias = "fedletMutualAuthCertAlias";

        /// <summary>
        /// Constant for the X509SubjectName parameter.
        /// </summary>
        public const string X509SubjectName = "X509SubjectName";

        /// <summary>
        /// Constant for the SAMLv2 namespace for basic attribute name format.
        /// </summary>
        public const string AttributeNameFormatBasic = "urn:oasis:names:tc:SAML:2.0:attrname-format:basic";

        /// <summary>
        /// Constant for the SAMLv2 namespace for X500 attribute name format.
        /// </summary>
        public const string AttributeNameFormatX500 = "urn:oasis:names:tc:SAML:2.0:attrname-format:uri";

    }
}
