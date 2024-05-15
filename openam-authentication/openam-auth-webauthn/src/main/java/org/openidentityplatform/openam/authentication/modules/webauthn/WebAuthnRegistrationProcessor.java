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
 * Copyright 2024 3A-Systems LLC. All rights reserved.
 */

package org.openidentityplatform.openam.authentication.modules.webauthn;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.converter.exception.DataConversionException;
import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.AuthenticatorAttachment;
import com.webauthn4j.data.AuthenticatorSelectionCriteria;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialDescriptor;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialRpEntity;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.PublicKeyCredentialUserEntity;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.UserVerificationRequirement;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.validator.exception.ValidationException;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class WebAuthnRegistrationProcessor {

    private final WebAuthnManager webAuthnManager;
    private final List<PublicKeyCredentialParameters> pubKeyCredParams;
    private Challenge challenge;
    private AuthenticatorAttachment authenticatorAttachment;
    private AttestationConveyancePreference attestation;
    private long timeout;


    public WebAuthnRegistrationProcessor(String sessionId,
                                         AttestationConveyancePreference attestation,
                                         AuthenticatorAttachment authenticatorAttachment,
                                         long timeout) {

        this.challenge = new DefaultChallenge(sessionId.getBytes());
        this.attestation = attestation;
        this.authenticatorAttachment = authenticatorAttachment;
        this.timeout = timeout;

        webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();

        pubKeyCredParams = new ArrayList<>();
        pubKeyCredParams.add(
                new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256));
        pubKeyCredParams.add(
                new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES384));
        pubKeyCredParams.add(
                new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES512));
        pubKeyCredParams.add(
                new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256));
        pubKeyCredParams.add(
                new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS384));
        pubKeyCredParams.add(
                new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS512));
    }


    public PublicKeyCredentialCreationOptions requestCredentials(String username, HttpServletRequest request) {

        String rpId = request.getServerName();

        PublicKeyCredentialRpEntity rp = new PublicKeyCredentialRpEntity(rpId, rpId);

        PublicKeyCredentialUserEntity user = new PublicKeyCredentialUserEntity(username.getBytes(),
                username,
                username);

        UserVerificationRequirement userVerificationRequirement = UserVerificationRequirement.PREFERRED;

        List<PublicKeyCredentialDescriptor> excludeCredentials = Collections.emptyList();

        AuthenticatorSelectionCriteria authenticatorSelectionCriteria =
                new AuthenticatorSelectionCriteria(
                        this.authenticatorAttachment,
                        true,
                        userVerificationRequirement);

        PublicKeyCredentialCreationOptions credentialCreationOptions = new PublicKeyCredentialCreationOptions(
                rp,
                user,
                challenge,
                pubKeyCredParams,
                timeout,
                excludeCredentials,
                authenticatorSelectionCriteria,
                this.attestation,
                null
        );

        return credentialCreationOptions;
	}
	
	public Authenticator processCredentials(String attestationObjectStr,
			String clientDataJSONStr, HttpServletRequest request) {
		
		 Origin origin = new Origin(request.getHeader("Origin"));
		 String rpId = request.getServerName();
		 
		 byte[] clientDataJSON = Base64Utils.decodeFromUrlSafeString(clientDataJSONStr);
	     byte[] attestationObject = Base64Utils.decodeFromUrlSafeString(attestationObjectStr);
	     byte[] tokenBindingId = null;

        ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge, tokenBindingId);
        boolean userVerificationRequired = false;
        boolean userPresenceRequired = true;

        RegistrationRequest registrationRequest = new RegistrationRequest(attestationObject, clientDataJSON);

        RegistrationParameters registrationParameters = new RegistrationParameters(serverProperty,
                pubKeyCredParams,
                userVerificationRequired,
                userPresenceRequired);
        RegistrationData registrationData;
        try {
            registrationData = webAuthnManager.parse(registrationRequest);
        } catch (DataConversionException e) {
            throw e;
        }

        try {
            webAuthnManager.validate(registrationData, registrationParameters);
        } catch (ValidationException e) {
            throw e;
        }

        Authenticator authenticator =
                new AuthenticatorImpl(
                        registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData(),
                        registrationData.getAttestationObject().getAttestationStatement(),
                        registrationData.getAttestationObject().getAuthenticatorData().getSignCount(),
                        registrationData.getTransports()
                );
        return authenticator;
	}
}
