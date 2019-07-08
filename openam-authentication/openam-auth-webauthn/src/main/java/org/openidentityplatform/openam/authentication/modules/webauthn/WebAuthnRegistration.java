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
 * Copyright 2019 3A-Systems. All rights reserved.
 */

package org.openidentityplatform.openam.authentication.modules.webauthn;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.forgerock.openam.authentication.modules.common.mapping.AccountProvider;
import org.forgerock.openam.authentication.modules.common.mapping.DefaultAccountProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.datastruct.CollectionHelper;
import javax.security.auth.callback.PasswordCallback;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.AuthenticatorAttachment;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;

/**
 * 
 *
 */
public class WebAuthnRegistration extends AMLoginModule {

	final static Logger logger = LoggerFactory.getLogger(WebAuthnRegistration.class);
	
	final static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Include.NON_NULL);
	}
	
	private Map<String, Object> sharedState = null;
	
	private final static int LOGIN_REQUEST_CREDENTIALS_STATE = 2;
	
	private final static int LOGIN_REGISTRATION_SUCCESS_STATE = 3;
	
	private final static int CHALLENGE_ID_CB_INDEX = 0;
	private final static int CHALLENGE_TYPE_CB_INDEX = 1;
	private final static int CHALLENGE_ATTESTATION_CB_INDEX = 2;
	private final static int CHALLENGE_CLIENT_DATA_CB_INDEX = 3;
	
	private final static int CREDENTIAL_REQUEST_CB_INDEX = 4;
	
	private WebAuthnRegistrationProcessor webAuthnRegistrationProcessor = null;
	private	AttestationConveyancePreference attestation = null;
	private	AuthenticatorAttachment authenticatorAttachment = null;
	
	private String username = null;
	private long timeout;
	
	private String userAttribute = null;
	
	private AccountProvider accountProvider = new DefaultAccountProvider();
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void init(Subject subject, Map sharedState, Map options) {
		
		this.sharedState = sharedState;
		
		this.attestation = AttestationConveyancePreference.create(
				CollectionHelper.getMapAttr(options, 
						WebAuthnRegistration.class.getName().concat(".attestation"),
						"none"));
		

		String authType = CollectionHelper.getMapAttr(options, 
				WebAuthnRegistration.class.getName().concat(".authType"), 
				"unspecified");
		this.authenticatorAttachment = null;
		if(!StringUtils.isBlank(authType) && !"unspecified".equals(authType)) {
			this.authenticatorAttachment = AuthenticatorAttachment.create(authType);
		}
		
		this.timeout = Long.parseLong(CollectionHelper.getMapAttr(options, 
				WebAuthnRegistration.class.getName().concat(".timeout"),
				"60000"));
		
		this.userAttribute = CollectionHelper.getMapAttr(options, 
				WebAuthnRegistration.class.getName().concat(".userAttribute"),
				"sunIdentityServerPPSignKey");
	}

	@Override
	public int process(Callback[] callbacks, int state) throws LoginException {

		if(webAuthnRegistrationProcessor == null) {
			webAuthnRegistrationProcessor = new WebAuthnRegistrationProcessor(
						getSessionId(),
						attestation, authenticatorAttachment, timeout);
		}
		
		try {
			switch (state) {
			case ISAuthConstants.LOGIN_START:
				if(callbacks == null) { 		//no callback init authentication
					if(username == null && sharedState.containsKey(getUserKey())) {
						username = (String)sharedState.get(getUserKey());
						return requestCredentials();
					} else {
						return ISAuthConstants.LOGIN_START;
					}
				}
				else {
					//getting username
					username = ((NameCallback)callbacks[0]).getName();
					return requestCredentials();
				}
			case LOGIN_REQUEST_CREDENTIALS_STATE:
				return processCredentials(callbacks);
			case LOGIN_REGISTRATION_SUCCESS_STATE: 
				return ISAuthConstants.LOGIN_SUCCEED;
			default:
				break;
			}
		} catch(AuthLoginException e) {
			logger.error("process: AuthLoginException {}", e.toString());
			throw e;
		} catch(Exception e) {
			logger.error("process: Exception {}", e.toString());
			throw new AuthLoginException(e);
		}

		return ISAuthConstants.LOGIN_START;
	}
	
	public int requestCredentials() throws AuthLoginException, JsonProcessingException {
		
		PublicKeyCredentialCreationOptions credentialCreationOptions = webAuthnRegistrationProcessor.requestCredentials(username, getHttpServletRequest());
        
	    String credentialCreationOptionsString = mapper.writeValueAsString(credentialCreationOptions);
        TextOutputCallback credentialCreationOptionsCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, credentialCreationOptionsString);
        replaceCallback(LOGIN_REQUEST_CREDENTIALS_STATE, CREDENTIAL_REQUEST_CB_INDEX, credentialCreationOptionsCallback);
        
		return LOGIN_REQUEST_CREDENTIALS_STATE;
	}
	
	private int processCredentials(Callback[] callbacks) throws AuthLoginException {
		String id = 					new String(((PasswordCallback) callbacks[CHALLENGE_ID_CB_INDEX]).getPassword());
		String type = 					new String(((PasswordCallback) callbacks[CHALLENGE_TYPE_CB_INDEX]).getPassword());
		String attestationObjectStr = 	new String(((PasswordCallback) callbacks[CHALLENGE_ATTESTATION_CB_INDEX]).getPassword());
		String clientDataJSONStr = 		new String(((PasswordCallback) callbacks[CHALLENGE_CLIENT_DATA_CB_INDEX]).getPassword());
		
		Authenticator authenticator = webAuthnRegistrationProcessor.processCredentials(id, type, attestationObjectStr, clientDataJSONStr, getHttpServletRequest());
		save(authenticator);
		return LOGIN_REGISTRATION_SUCCESS_STATE;
	}
	
    @SuppressWarnings("unchecked")
	protected void save(Authenticator authenticator) throws AuthLoginException {
    	Map<String, Set<String>> attributes = new HashMap<>();
    	attributes.put("uid", Collections.singleton(username));
    	AMIdentity user = accountProvider.provisionUser(getAMIdentityRepository(getRequestOrg()), attributes);
    	
    	try {
			user.setActiveStatus(true);
			Set<String> authenticators =  user.getAttribute(userAttribute);
			byte[] bytes = SerializationUtils.serialize(authenticator);
			String authStr = Base64Utils.encodeToUrlSafeString(bytes);
			authenticators.add(authStr);
			user.setAttributes(Collections.singletonMap(userAttribute, authenticators));
		} catch (SSOException | IdRepoException e) {
			logger.error("save: error update user : {}", e);
			throw new AuthLoginException(e);
		}
    	
	}


	@Override
	public Principal getPrincipal() {
		return new WebAuthnPrincipal(username);
	}
}
