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
 * Copyright 2019-2025 3A-Systems LLC. All rights reserved.
 */

package org.openidentityplatform.openam.authentication.modules.webauthn;

import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.converter.exception.DataConversionException;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.PublicKeyCredentialDescriptor;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.UserVerificationRequirement;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.validator.exception.ValidationException;
import org.apache.commons.lang3.ArrayUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class WebAuthnAuthenticationProcessor {

	private final WebAuthnManager webAuthnManager;
	
	private final Challenge challenge;
	
	long timeout;
	
	public WebAuthnAuthenticationProcessor(String sessionId, long timeout) {
		this.challenge = new DefaultChallenge(sessionId.getBytes());
		this.timeout = timeout;
		webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();
	}
	
	public PublicKeyCredentialRequestOptions requestCredentials(
			HttpServletRequest request,
			Set<Authenticator> authenticators) {
		
        String rpId = request.getServerName();

        List<PublicKeyCredentialDescriptor> allowCredentials = new ArrayList<>();

        for(Authenticator authenticator : authenticators) {
            PublicKeyCredentialDescriptor publicKeyCredentialDescriptor = new PublicKeyCredentialDescriptor(
                    PublicKeyCredentialType.PUBLIC_KEY,
                    authenticator.getAttestedCredentialData().getCredentialId(),
                    authenticator.getTransports()
            );

            allowCredentials.add(publicKeyCredentialDescriptor);
        }
        UserVerificationRequirement userVerification = UserVerificationRequirement.PREFERRED;

        PublicKeyCredentialRequestOptions publicKeyCredentialRequestOptions = new PublicKeyCredentialRequestOptions(
                challenge, timeout, rpId, allowCredentials, userVerification, null
        );
        
        return publicKeyCredentialRequestOptions;
	}
	
	public AuthenticatorData<?> processCredentials(HttpServletRequest request, String idStr, 
			String authenticatorDataStr, String clientDataJSONStr, String signatureStr, byte[] userHandle,
			Set<Authenticator> authenticators) {

		byte[] id = Base64Utils.decodeFromUrlSafeString(idStr);
		byte[] clientDataJSON = Base64Utils.decodeFromUrlSafeString(clientDataJSONStr);
		byte[] authenticatorData = Base64Utils.decodeFromUrlSafeString(authenticatorDataStr);
		byte[] signature = Base64Utils.decodeFromUrlSafeString(signatureStr);

		Authenticator foundAuthenticator = null;
		for(Authenticator authenticator : authenticators ) {
			if(ArrayUtils.isEquals(authenticator.getAttestedCredentialData().getCredentialId(), id)) {
				foundAuthenticator = authenticator;
				break;
			}
		}

		if(foundAuthenticator == null) {
			return null;
		}

		Origin origin = new Origin(request.getHeader("Origin"));
        String rpId = request.getServerName();


		byte[] tokenBindingId = null;
		ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge, tokenBindingId);
		List<byte[]> allowCredentials = null;
		boolean userVerificationRequired = false;
		boolean userPresenceRequired = true;

		Authenticator authenticator = authenticators.stream().filter(a ->
						Objects.deepEquals(a.getAttestedCredentialData().getCredentialId(), id))
				.findFirst().orElse(null);

		AuthenticationParameters authenticationParameters =
				new AuthenticationParameters(
						serverProperty,
						authenticator,
						allowCredentials,
						userVerificationRequired,
						userPresenceRequired
				);

		AuthenticationRequest authenticationRequest = new AuthenticationRequest(
				id, userHandle, authenticatorData, clientDataJSON, null, signature
		);

		AuthenticationData authenticationData;
		try {
			authenticationData = webAuthnManager.parse(authenticationRequest);
		} catch (DataConversionException e) {
			// If you would like to handle WebAuthn data structure parse error, please catch DataConversionException
			throw e;
		}
		try {
			webAuthnManager.validate(authenticationData, authenticationParameters);
		} catch (ValidationException e) {
			// If you would like to handle WebAuthn data validation error, please catch ValidationException
			throw e;
		}
// please update the counter of the authenticator record TODO
//        updateCounter(
//                authenticationData.getCredentialId(),
//                authenticationData.getAuthenticatorData().getSignCount()
//        );

		return authenticationData.getAuthenticatorData();
	}
}
