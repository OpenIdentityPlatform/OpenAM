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
 * Copyright 2019-2026 3A-Systems LLC. All rights reserved.
 */

package org.openidentityplatform.openam.authentication.modules.webauthn;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iplanet.sso.SSOException;
import com.sun.identity.authentication.service.AuthD;
import com.sun.identity.authentication.service.AuthException;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.spi.InvalidPasswordException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdType;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.data.PublicKeyCredentialRequestOptions;
import com.webauthn4j.data.attestation.authenticator.AuthenticatorData;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.TextOutputCallback;
import javax.security.auth.login.LoginException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;
import java.security.Principal;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 
 * WebAuthn Login Authentication Module 
 *
 */
public class WebAuthnAuthentication extends AMLoginModule {

	final Debug debug;

	public WebAuthnAuthentication() {
		debug = Debug.getInstance("amWebAuthnAuthentication");
	}
	
	final static ObjectMapper mapper = new ObjectMapper();
	static {
		mapper.setSerializationInclusion(Include.NON_NULL);
	}
	
	private final static int LOGIN_REQUEST_CREDENTIALS_STATE = 2;
	private static final int CREDENTIALS_CB_INDEX = 0;
	private static final int CREDENTIAL_REQUEST_CB_INDEX = 1;

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
			debug.error("process: AuthLoginException {0}", e.toString());
			throw e;
		} catch(Exception e) {
			debug.error("process: Exception {0}", e.toString());
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

		final String credentialsStr = new String(((PasswordCallback) callbacks[CREDENTIALS_CB_INDEX]).getPassword());
		final Map<String, String> credentials;
		try {
			credentials = mapper.readValue(credentialsStr, new TypeReference<Map<String, String>>() {});
		} catch (Exception e) {
			debug.error("invalid credentials data: " + credentialsStr, e);
			throw new AuthLoginException(e);
		}

		final String assertionId = credentials.get("assertionId");
		final String authenticatorDataStr = credentials.get("authenticatorData");
		final String clientDataJSONStr = credentials.get("clientDataJSON");
		final String signatureStr = credentials.get("signature");
		final String userHandleStr = credentials.get("userHandle");

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
				getHttpServletRequest(), assertionId, authenticatorDataStr,
				clientDataJSONStr, signatureStr, userHandle, authenticators);
		
		if(authenticatorData == null) {
			debug.warning("processCredentials: authenticator data with id {0} not found for identity: {1}", assertionId, username);
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

	private static final Set<String> ALLOWED_AUTHENTICATOR_CLASSES = Set.of(
			AuthenticatorImpl.class.getName()
	);

	// The whole AuthenticatorImpl object graph is made up of webauthn4j data
	// objects, so the library's own package is allowlisted as a prefix.
	private static final String WEBAUTHN4J_PACKAGE_PREFIX = "com.webauthn4j.";

	// Safe JDK value/container types that legitimately appear in the
	// AuthenticatorImpl graph (UUID inside AAGUID, java.lang.Enum as the
	// superclass of the COSE/transport enums, boxed primitives and the
	// collections used for transports / keyOps / extensions). Every element of
	// these containers is itself checked by the same filter, so allowing a
	// container cannot smuggle in a gadget class.
	private static final Set<String> ALLOWED_JDK_CLASSES = Set.of(
			"java.lang.Boolean",
			"java.lang.Byte",
			"java.lang.Character",
			"java.lang.Short",
			"java.lang.Integer",
			"java.lang.Long",
			"java.lang.Number",
			"java.lang.String",
			"java.lang.Enum",
			"java.math.BigInteger",
			"java.util.UUID",
			"java.util.ArrayList",
			"java.util.LinkedList",
			"java.util.HashSet",
			"java.util.LinkedHashSet",
			"java.util.TreeSet",
			"java.util.HashMap",
			"java.util.LinkedHashMap",
			"java.util.TreeMap",
			"java.util.Arrays$ArrayList",
			"java.util.Collections$EmptyList",
			"java.util.Collections$EmptyMap",
			"java.util.Collections$EmptySet",
			"java.util.Collections$SingletonList",
			"java.util.Collections$SingletonMap",
			"java.util.Collections$SingletonSet",
			"java.util.Collections$UnmodifiableCollection",
			"java.util.Collections$UnmodifiableList",
			"java.util.Collections$UnmodifiableRandomAccessList",
			"java.util.Collections$UnmodifiableMap",
			"java.util.Collections$UnmodifiableSet"
	);

	@SuppressWarnings("unchecked")
	protected Set<Authenticator> loadAuthenticators(AMIdentity identity) throws AuthLoginException {
		Set<Authenticator> authenticators = new HashSet<>();

		try {
			Set<String> authenticatorsMarshalled = identity.getAttribute(userAttribute);
			for(String authenticatorMarshalled: authenticatorsMarshalled) {
				byte[] bytesDecoded = Base64Utils.decodeFromUrlSafeString(authenticatorMarshalled);
				Authenticator authenticator = deserialize(bytesDecoded);
				authenticators.add(authenticator);
			}
		} catch (SSOException | IdRepoException e) {
			debug.error("loadAuthenticators: error getting authenticators from user : {0}", e.toString());
			throw new AuthLoginException(e);
		}
		return authenticators;
	}

	private Authenticator deserialize(byte[] authBytes) throws AuthLoginException {

		try (ObjectInputStream ois =
					 new ObjectInputStream(new ByteArrayInputStream(authBytes))) {
			ois.setObjectInputFilter(filterInfo -> {
				Class<?> cls = filterInfo.serialClass();
				if (cls == null) {
					// Not a class-resolution check (e.g. array length); leave the
					// decision to the JVM-wide default filter.
					return ObjectInputFilter.Status.UNDECIDED;
				}
				// Validate the element type rather than the array type itself.
				while (cls.isArray()) {
					cls = cls.getComponentType();
				}
				if (cls.isPrimitive()) {
					return ObjectInputFilter.Status.ALLOWED;
				}
				final String className = cls.getName();
				// The root object (depth 1) must be exactly AuthenticatorImpl;
				// its superclasses and the whole nested graph are at depth > 1.
				if (filterInfo.depth() == 1 && !ALLOWED_AUTHENTICATOR_CLASSES.contains(className)) {
					return ObjectInputFilter.Status.REJECTED;
				}
				if (className.startsWith(WEBAUTHN4J_PACKAGE_PREFIX)
						|| ALLOWED_JDK_CLASSES.contains(className)) {
					return ObjectInputFilter.Status.ALLOWED;
				}
				return ObjectInputFilter.Status.REJECTED;
			});
			return (AuthenticatorImpl) ois.readObject();
		} catch (InvalidClassException | ClassNotFoundException e) {
			throw new AuthLoginException("Rejected non-allowlisted class: " + e.getMessage());
		} catch (IOException e) {
			throw new AuthLoginException("Deserialization stream error: " + e.getMessage());
		}
	}
}
