package org.openidentityplatform.openam.authentication.modules.webauthn;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.ArrayUtils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.data.PublicKeyCredentialDescriptor;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.UserVerificationRequirement;
import com.webauthn4j.data.WebAuthnAuthenticationContext;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.validator.WebAuthnAuthenticationContextValidationResponse;
import com.webauthn4j.validator.WebAuthnAuthenticationContextValidator;

public class WebAuthnAuthenticationProcessor {
	
	private Challenge challenge;
	
	long timeout;
	
	public WebAuthnAuthenticationProcessor(String sessionId, long timeout) {
		this.challenge = new DefaultChallenge(sessionId.getBytes());
		this.timeout = timeout;
	}
	
	public PublicKeyCredentialRequestOptions requestCredentials(String username, HttpServletRequest request, 
			Set<Authenticator> authenticators) throws AuthLoginException, JsonProcessingException {
		
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
			String authenticatorDataStr, String clientDataJSONStr, String signatureStr, 
			String userHandleStr, Set<Authenticator> authenticators) {
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
		
		Origin origin = new Origin(request.getScheme(), request.getServerName(), request.getServerPort());
        String rpId = request.getServerName();
               
        
        byte[] tokenBindingId = null;
        ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge, tokenBindingId);
        boolean userVerificationRequired = false;

        WebAuthnAuthenticationContext authenticationContext =
                new WebAuthnAuthenticationContext(
                        id,
                        clientDataJSON,
                        authenticatorData,
                        signature,
                        serverProperty,
                        userVerificationRequired
                );
        
        WebAuthnAuthenticationContextValidator webAuthnAuthenticationContextValidator = 
        		new WebAuthnAuthenticationContextValidator();

        WebAuthnAuthenticationContextValidationResponse response = 
        		webAuthnAuthenticationContextValidator.validate(authenticationContext, foundAuthenticator);
        
        return response.getAuthenticatorData();
	}
}
