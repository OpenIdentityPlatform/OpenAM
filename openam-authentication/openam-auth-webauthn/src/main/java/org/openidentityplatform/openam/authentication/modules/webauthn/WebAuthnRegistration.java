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

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iplanet.dpro.session.service.InternalSession;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.data.AttestationConveyancePreference;
import com.webauthn4j.data.AuthenticatorAttachment;
import com.webauthn4j.data.PublicKeyCredentialCreationOptions;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.login.LoginException;
import java.security.Principal;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * 
 *
 */
public class WebAuthnRegistration extends AMLoginModule {

	protected Debug debug = null;
	
	final static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Include.NON_NULL);
	}

	public WebAuthnRegistration() {
		debug = Debug.getInstance("amWebAuthnRegistration");
	}
	
	private Map<String, Object> sharedState = null;
	
	private final static int LOGIN_REQUEST_CREDENTIALS_STATE = 2;
	
	private final static int CREDENTIALS_CB_INDEX = 0;
    private final static int CREDENTIAL_REQUEST_CB_INDEX = 1;
	
	private WebAuthnRegistrationProcessor webAuthnRegistrationProcessor = null;
	private	AttestationConveyancePreference attestation = null;
	private	AuthenticatorAttachment authenticatorAttachment = null;
	
	protected String userId = null;
	private long timeout;
	
	private String userAttribute = null;
	
	private int authLevel = 0;


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
		
		this.authLevel  = Integer.parseInt(CollectionHelper.getMapAttr(options, 
				WebAuthnRegistration.class.getName().concat(".authlevel"),
				"0"));


		webAuthnRegistrationProcessor = new WebAuthnRegistrationProcessor(
				getSessionId(),
				attestation, authenticatorAttachment, timeout);

		initUserId();
	}

	protected void initUserId() {
		try {
			SSOTokenManager mgr = SSOTokenManager.getInstance();
			InternalSession is = getLoginState(WebAuthnRegistration.class.getName()).getOldSession();
			if (is == null) {
				throw new AuthLoginException("amAuth", "noInternalSession", null);
			}
			SSOToken token = mgr.createSSOToken(is.getID().toString());
			userId = token.getProperty("UserToken");
			if (debug.messageEnabled()) {
				debug.message("WebAuthnRegistration.initUserId() : Username from SSOToken : " + userId);
			}
		} catch (Exception e) {
			debug.error("WebAuthnRegistration.initUserId() : Exception", e);
		}
	}

	@Override
	public int process(Callback[] callbacks, int state) throws LoginException {
		if (StringUtils.isBlank(userId)) {
			throw new AuthLoginException("amAuth", "noUserName", null);
		}

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
			debug.error("process: AuthLoginException {}", e.toString());
			throw e;
		} catch(Exception e) {
			debug.error("process: Exception {}", e.toString());
			throw new AuthLoginException(e);
		}

		return ISAuthConstants.LOGIN_START;
	}
	
	public int requestCredentials() throws AuthLoginException, JsonProcessingException {

		PublicKeyCredentialCreationOptions credentialCreationOptions = webAuthnRegistrationProcessor.requestCredentials(userId, getHttpServletRequest());
        
	    String credentialCreationOptionsString = mapper.writeValueAsString(credentialCreationOptions);
        TextOutputCallback credentialCreationOptionsCallback = new TextOutputCallback(TextOutputCallback.INFORMATION, credentialCreationOptionsString);
        replaceCallback(LOGIN_REQUEST_CREDENTIALS_STATE, CREDENTIAL_REQUEST_CB_INDEX, credentialCreationOptionsCallback);
        
		return LOGIN_REQUEST_CREDENTIALS_STATE;
	}
	
	private int processCredentials(Callback[] callbacks) throws AuthLoginException {
		final String credentialsStr = new String(((PasswordCallback) callbacks[CREDENTIALS_CB_INDEX]).getPassword());
		final Map<String, String> credentials;
		try {
			credentials = mapper.readValue(credentialsStr, new TypeReference<Map<String, String>>() {});
		} catch (Exception e) {
			debug.error("invalid credentials data: " + credentialsStr, e);
			throw new AuthLoginException(e);
		}
		String attestationObjectStr = 	credentials.get("attestationObject");
		String clientDataJSONStr = 		credentials.get("clientDataJSON");

		Authenticator authenticator = webAuthnRegistrationProcessor.processCredentials(attestationObjectStr, clientDataJSONStr, getHttpServletRequest());

		save(authenticator);
		
		setAuthLevel(authLevel);
		
		return ISAuthConstants.LOGIN_SUCCEED;
	}
	
    protected void save(Authenticator authenticator) throws AuthLoginException {
		String realm = DNMapper.orgNameToRealmName(getRequestOrg());
		try {
			AMIdentity id = AuthD.getAuth().getIdentity(IdType.USER, userId, realm);
			Set<String> authenticators = id.getAttribute(userAttribute);
			byte[] bytes = SerializationUtils.serialize(authenticator);
			String authStr = Base64Utils.encodeToUrlSafeString(bytes);
			authenticators.add(authStr);
			id.setAttributes(Collections.singletonMap(userAttribute, authenticators));
			id.store();
		} catch (SSOException | IdRepoException | AuthException e) {
			debug.error("WebAuthnRegistration: save(): error update user : {}", e);
			throw new AuthLoginException(e);
		}
	}


	@Override
	public Principal getPrincipal() {
		return new WebAuthnPrincipal(userId);
	}


}
