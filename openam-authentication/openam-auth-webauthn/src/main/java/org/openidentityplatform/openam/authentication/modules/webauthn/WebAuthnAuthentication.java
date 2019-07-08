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
 * Copyright 2019 ForgeRock AS. All rights reserved.
 */

package org.openidentityplatform.openam.authentication.modules.webauthn;

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.login.LoginException;

import org.apache.commons.lang.SerializationUtils;
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
import com.sun.xml.wss.impl.callback.PasswordCallback;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;

/**
 * 
 *
 */
public class WebAuthnAuthentication extends AMLoginModule {

	final static Logger logger = LoggerFactory.getLogger(WebAuthnAuthentication.class);
	
	final static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Include.NON_NULL);
	}
	
	private final static int LOGIN_REQUEST_CREDENTIALS_STATE = 2;
	private final static int LOGIN_REGISTRATION_SUCCESS_STATE = 3;

	private static final int CHALLENGE_ID_CB_INDEX = 0;

	private static final int CHALLENGE_AUTH_DATA_CB_INDEX = 1;

	private static final int CHALLENGE_CLIENT_DATA_CB_INDEX = 2;

	private static final int CHALLENGE_SIGNATURE_CB_INDEX = 3;

	private static final int CHALLENGE_USER_HANDLE_CB_INDEX = 4;
	
	private static final int CREDENTIAL_REQUEST_CB_INDEX = 5;

	private WebAuthnAuthenticationProcessor webAuthnAuthenticationProcessor = null;
	
	private Map<String, Object> sharedState = null;
	
	private String username = null;
	
	private String userAttribute = null;
	
	private long timeout;
	
	private AccountProvider accountProvider = new DefaultAccountProvider();
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public void init(Subject subject, Map sharedState, Map options) {
		this.sharedState = sharedState;
		this.timeout = Long.parseLong(CollectionHelper.getMapAttr(options, 
				WebAuthnAuthentication.class.getName().concat(".timeout"),
						"60000"));
		
		this.userAttribute = CollectionHelper.getMapAttr(options, 
				WebAuthnAuthentication.class.getName().concat(".userAttribute"),
				"sunIdentityServerPPSignKey");
	}

	@Override
	public int process(Callback[] callbacks, int state) throws LoginException {
		if(webAuthnAuthenticationProcessor == null) {
			webAuthnAuthenticationProcessor = new WebAuthnAuthenticationProcessor(
						getSessionId(), this.timeout);
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
		
		Set<Authenticator> authenticators = loadAuthenticators();
		PublicKeyCredentialRequestOptions credentialCreationOptions = 
				webAuthnAuthenticationProcessor.requestCredentials(username, getHttpServletRequest(), authenticators);
        
	    String credentialCreationOptionsString = mapper.writeValueAsString(credentialCreationOptions);
        TextOutputCallback credentialCreationOptionsCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, credentialCreationOptionsString);
        replaceCallback(LOGIN_REQUEST_CREDENTIALS_STATE, CREDENTIAL_REQUEST_CB_INDEX, credentialCreationOptionsCallback);
        
		return LOGIN_REQUEST_CREDENTIALS_STATE;
	}
	
	private int processCredentials(Callback[] callbacks) throws AuthLoginException {
		
        String id = 					((PasswordCallback) callbacks[CHALLENGE_ID_CB_INDEX]).getPassword();
		String authenticatorDataStr = 	((PasswordCallback) callbacks[CHALLENGE_AUTH_DATA_CB_INDEX]).getPassword();
		String clientDataJSONStr = 		((PasswordCallback) callbacks[CHALLENGE_CLIENT_DATA_CB_INDEX]).getPassword();
		String signatureStr = 			((PasswordCallback) callbacks[CHALLENGE_SIGNATURE_CB_INDEX]).getPassword();
		String userHandleStr = 			((PasswordCallback) callbacks[CHALLENGE_USER_HANDLE_CB_INDEX]).getPassword();
		
		webAuthnAuthenticationProcessor.processCredentials(getHttpServletRequest(), id, authenticatorDataStr, 
				clientDataJSONStr, signatureStr, userHandleStr);
		
		return ISAuthConstants.LOGIN_SUCCEED;
	}
	
	

	@Override
	public Principal getPrincipal() {
		return new WebAuthnPrincipal(username);
	}
	
	@SuppressWarnings("unchecked")
	protected Set<Authenticator> loadAuthenticators() throws AuthLoginException {
		Set<Authenticator> authenticators = new HashSet<>();
		
		Map<String, Set<String>> attributes = new HashMap<>();
    	attributes.put("uid", Collections.singleton(username));
		AMIdentity user = accountProvider.searchUser(getAMIdentityRepository(getRequestOrg()), attributes);
		try {
			Set<String> authenticatorsMarshalled =  user.getAttribute(userAttribute);
			for(String authenticatorMarshalled: authenticatorsMarshalled) {
				byte[] bytesDecoded = Base64Utils.decodeFromUrlSafeString(authenticatorMarshalled);
				Authenticator decoded = (Authenticator)SerializationUtils.deserialize(bytesDecoded);
				authenticators.add(decoded);
			}
		} catch (SSOException | IdRepoException e) {
			logger.error("loadAuthenticators: error getting authenticators from user : {}", e);
			throw new AuthLoginException(e);
		}
		return authenticators;
	}
	
}
