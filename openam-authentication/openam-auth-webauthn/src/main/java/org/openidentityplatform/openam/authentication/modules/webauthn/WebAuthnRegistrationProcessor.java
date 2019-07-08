package org.openidentityplatform.openam.authentication.modules.webauthn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.AuthenticatorAttachment;
import com.webauthn4j.data.AuthenticatorSelectionCriteria;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import com.webauthn4j.data.PublicKeyCredentialDescriptor;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialRpEntity;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.PublicKeyCredentialUserEntity;
import com.webauthn4j.data.UserVerificationRequirement;
import com.webauthn4j.data.WebAuthnRegistrationContext;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.Challenge;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.validator.WebAuthnRegistrationContextValidationResponse;
import com.webauthn4j.validator.WebAuthnRegistrationContextValidator;

public class WebAuthnRegistrationProcessor {
	
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
		
	}
	

	public PublicKeyCredentialCreationOptions requestCredentials(String username, HttpServletRequest request) throws AuthLoginException, JsonProcessingException {
		
		String rpId = request.getServerName();
		
        PublicKeyCredentialRpEntity rp = new PublicKeyCredentialRpEntity(rpId, rpId);

        PublicKeyCredentialUserEntity user = new PublicKeyCredentialUserEntity(username.getBytes(),
        		username,
        		username);
        
        List<PublicKeyCredentialParameters> pubKeyCredParams = new ArrayList<PublicKeyCredentialParameters>();
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

        
        UserVerificationRequirement userVerificationRequirement = UserVerificationRequirement.PREFERRED;

        List<PublicKeyCredentialDescriptor> excludeCredentials = Collections.emptyList();

        AuthenticatorSelectionCriteria authenticatorSelectionCriteria =
                new AuthenticatorSelectionCriteria(
                        this.authenticatorAttachment,
                        false,
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
	
	public Authenticator processCredentials(String id, String type, String attestationObjectStr, 
			String clientDataJSONStr, HttpServletRequest request) 
					throws AuthLoginException {
		
		 Origin origin = new Origin(request.getScheme(), request.getServerName(), request.getServerPort());
		 String rpId = request.getServerName();
		 
		 byte[] clientDataJSON = Base64Utils.decodeFromUrlSafeString(clientDataJSONStr);
	     byte[] attestationObject = Base64Utils.decodeFromUrlSafeString(attestationObjectStr);
	     byte[] tokenBindingId = null;

	     ServerProperty serverProperty = new ServerProperty(origin, rpId, challenge, tokenBindingId);
	     boolean userVerificationRequired = false;

	     WebAuthnRegistrationContext registrationContext = new WebAuthnRegistrationContext(
                clientDataJSON,
                attestationObject,
                serverProperty,
                userVerificationRequired
        );
        WebAuthnRegistrationContextValidator webAuthnRegistrationContextValidator =
                WebAuthnRegistrationContextValidator.createNonStrictRegistrationContextValidator();

        WebAuthnRegistrationContextValidationResponse response = webAuthnRegistrationContextValidator.validate(registrationContext);

        Authenticator authenticator =
                new AuthenticatorImpl( // You may create your own Authenticator implementation to save friendly authenticator name
                        response.getAttestationObject().getAuthenticatorData().getAttestedCredentialData(),
                        response.getAttestationObject().getAttestationStatement(),
                        response.getAttestationObject().getAuthenticatorData().getSignCount()
                );
        return authenticator;
	}
}
