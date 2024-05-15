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

import java.security.Principal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.login.LoginException;

import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.idm.IdType;
import com.sun.identity.sm.DNMapper;
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
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;

/**
 * 
 * WebAuthn Login Authentication Module 
 *
 */
public class WebAuthnAuthentication extends AMLoginModule {

	final static Logger logger = LoggerFactory.getLogger(WebAuthnAuthentication.class);
	
	final static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Include.NON_NULL);
	}
	
	private final static int LOGIN_REQUEST_CREDENTIALS_STATE = 2;
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

	private int authLevel = 0;


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
		
		this.authLevel = Integer.parseInt(CollectionHelper.getMapAttr(options, 
				WebAuthnAuthentication.class.getName().concat(".authlevel"),
				"0"));

		webAuthnAuthenticationProcessor = new WebAuthnAuthenticationProcessor(
				getSessionId(), this.timeout);
	}

	@Override
	public int process(Callback[] callbacks, int state) throws LoginException {


		try {
			switch (state) {
			case ISAuthConstants.LOGIN_START:
				return requestCredentials();
			case LOGIN_REQUEST_CREDENTIALS_STATE:
				return processCredentials(callbacks);
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

		PublicKeyCredentialRequestOptions credentialCreationOptions =
				webAuthnAuthenticationProcessor.requestCredentials(getHttpServletRequest(), Collections.emptySet());
        
	    String credentialCreationOptionsString = mapper.writeValueAsString(credentialCreationOptions);
        TextOutputCallback credentialCreationOptionsCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, credentialCreationOptionsString);
        replaceCallback(LOGIN_REQUEST_CREDENTIALS_STATE, CREDENTIAL_REQUEST_CB_INDEX, credentialCreationOptionsCallback);
        
        return LOGIN_REQUEST_CREDENTIALS_STATE;
	}
	
	private int processCredentials(Callback[] callbacks) throws AuthLoginException {
		
        String id = 					new String(((PasswordCallback) callbacks[CHALLENGE_ID_CB_INDEX]).getPassword());
		String authenticatorDataStr = 	new String(((PasswordCallback) callbacks[CHALLENGE_AUTH_DATA_CB_INDEX]).getPassword());
		String clientDataJSONStr = 		new String(((PasswordCallback) callbacks[CHALLENGE_CLIENT_DATA_CB_INDEX]).getPassword());
		String signatureStr = 			new String(((PasswordCallback) callbacks[CHALLENGE_SIGNATURE_CB_INDEX]).getPassword());
		String userHandleStr = 			new String(((PasswordCallback) callbacks[CHALLENGE_USER_HANDLE_CB_INDEX]).getPassword());

		String realm = DNMapper.orgNameToRealmName(getRequestOrg());
		byte[] userHandle = Base64Utils.decodeFromUrlSafeString(userHandleStr);
		String userId = new String(userHandle);

		final AMIdentity identity;
		try {
			identity = AuthD.getAuth().getIdentity(IdType.USER, userId, realm);
		} catch (AuthException e) {
			throw new AuthLoginException(e);
		}
		final Set<Authenticator> authenticators = loadAuthenticators(identity);
		
		AuthenticatorData<?> authenticatorData = webAuthnAuthenticationProcessor.processCredentials(
				getHttpServletRequest(), id, authenticatorDataStr,
				clientDataJSONStr, signatureStr, userHandle, authenticators);
		
		if(authenticatorData == null) {
			logger.warn("processCredentials: authenticator data with id {} not found for identity: {}", id, username);
			throw new InvalidPasswordException("authenticator not found");
		}
		this.username = userId;
		setAuthLevel(this.authLevel);
		
		return ISAuthConstants.LOGIN_SUCCEED;
	}
	
	

	@Override
	public Principal getPrincipal() {
		return new WebAuthnPrincipal(username);
	}
	
	@SuppressWarnings("unchecked")
	protected Set<Authenticator> loadAuthenticators(AMIdentity identity) throws AuthLoginException {
		Set<Authenticator> authenticators = new HashSet<>();

		try {
			Set<String> authenticatorsMarshalled =  identity.getAttribute(userAttribute);
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
