/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014 ForgeRock AS. All rights reserved.
 */

package org.forgerock.openam.sts.service.invocation;

import com.sun.identity.shared.encode.Base64;
import org.forgerock.json.fluent.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.openam.sts.AMSTSConstants;
import org.forgerock.openam.sts.TokenMarshalException;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.forgerock.json.fluent.JsonValue.field;
import static org.forgerock.json.fluent.JsonValue.json;
import static org.forgerock.json.fluent.JsonValue.object;

/**
 * When issuing SAML2 Holder-of-Key assertions, the proof token is usually an X509Certificate. This state must be
 * specified in the invocation, both to the REST-STS, and in the call to the TokenGenerationService made by the
 * REST/SOAP STS. This is the analogue to the UseKey element in the WS-Trust defined RequestSecurityToken, which is
 * defined as 'generally used when the client supplies a public-key that it wishes to be embedded in T as the proof key.'
 * See http://docs.oasis-open.org/ws-sx/ws-trust/v1.4/errata01/os/ws-trust-1.4-errata01-os-complete.html for details.
 * The CXF-STS parses out the KeyInfo element included in the UseKey to create the org.apache.cxf.sts.request.ReceivedKey
 * which encapsulates this public key. Thus the SOAP-STS can use this ReceivedKey to constitute the ProofTokenState,
 * and the REST-STS will be invoked with the json representation of this class, which can then be forwarded on to the
 * TokenGenerationService when SAML2 HoK tokens are being issued.
 *
 * Note that the WS-Trust spec allows for the UseKey to include symmetric key information, resulting in a SAML2 HoK with
 * a KeyInfo element which contains symmetric key information. The TokenGenerationService and the REST-STS will not
 * support proof tokens based on symmetric key information for the moment.
 *
 * It may be that PublicKey based proof tokens need to be supported in the future. If so, this class will add a ctor
 * which takes a PublicKey, and encode which sort of proof-token-state has been provided (e.g. X509Certificate or PublicKey).
 *
 */
public class ProofTokenState {
    public static class ProofTokenStateBuilder {
        private X509Certificate certificate;

        public ProofTokenStateBuilder x509Certificate(X509Certificate certificate) {
            this.certificate = certificate;
            return this;
        }

        public ProofTokenState build() throws TokenMarshalException {
            return new ProofTokenState(this);
        }
    }
    private static final String BASE_64_ENCODED_CERTIFICATE = "base64EncodedCertificate";
    private static final String X_509 = "X.509";

    private final X509Certificate certificate;

    private ProofTokenState(ProofTokenStateBuilder builder) throws TokenMarshalException {
        certificate = builder.certificate;
        if (certificate == null) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST, "the X509Certificate state must be set.");
        }
    }

    public X509Certificate getX509Certificate() {
        return certificate;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof ProofTokenState) {
            ProofTokenState otherState = (ProofTokenState)other;
            return (certificate.equals(otherState.certificate));
        }
        return false;
    }

    @Override
    public int hashCode() {
        return certificate.hashCode();
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    public static ProofTokenStateBuilder builder() {
        return new ProofTokenStateBuilder();
    }

    public static ProofTokenState fromJson(JsonValue jsonValue) throws TokenMarshalException {
        final String certString = jsonValue.get(BASE_64_ENCODED_CERTIFICATE).asString();
        try {
            final X509Certificate x509Certificate = (X509Certificate)CertificateFactory.getInstance(X_509).generateCertificate(
                    new ByteArrayInputStream(Base64.decode(certString.getBytes(AMSTSConstants.UTF_8_CHARSET_ID))));
            return ProofTokenState.builder().x509Certificate(x509Certificate).build();
        } catch (CertificateException e) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                    "Exception caught marshalling from json to X509 cert: " + e, e);
        } catch (UnsupportedEncodingException e) {
            throw new TokenMarshalException(ResourceException.BAD_REQUEST,
                    "Exception caught marshalling from json to X509 cert: " + e, e);
        }
    }

    public JsonValue toJson() throws IllegalStateException {
        try {
            String base64EncodedCertificate = Base64.encode(certificate.getEncoded());
            return json(object(field(BASE_64_ENCODED_CERTIFICATE, base64EncodedCertificate)));
        } catch (CertificateEncodingException e) {
            throw new IllegalStateException("Exception getting base64 representation of certificate: " + e, e);
        }
    }
}
