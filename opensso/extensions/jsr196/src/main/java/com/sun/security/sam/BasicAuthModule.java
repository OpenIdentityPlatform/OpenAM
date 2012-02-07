package com.sun.security.sam;

import java.io.IOException;
import java.util.Map;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.message.callback.CallerPrincipalCallback;
import javax.security.auth.message.callback.GroupPrincipalCallback;
import javax.security.auth.message.callback.PasswordValidationCallback;
import javax.security.auth.message.module.ServerAuthModule;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * http://blogs.sun.com/enterprisetechtips/entry/
 * adding_authentication_mechanisms_to_the
 * 
 * supported properties:
 * 
 * realm.name = Name of security realm to be used by server
 * 
 * assign.groups = Name of Group to be assigend to the GroupPrincipal
 * 
 * Note: the configured realm need to be the default realm of glassfish security
 * settings
 * 
 * @author monzillo, rsoika
 */

public class BasicAuthModule implements ServerAuthModule {

	protected static final Class[] supportedMessageTypes = new Class[] {
			HttpServletRequest.class, HttpServletResponse.class };

	private MessagePolicy requestPolicy;
	private MessagePolicy responsePolicy;
	private CallbackHandler handler;
	private Map options;
	private String realmName = null;
	private String defaultGroup[] = null;
	private static final String REALM_PROPERTY_NAME = "realm.name";
	private static final String GROUP_PROPERTY_NAME = "assign.groups";
	private static final String BASIC = "Basic";
	static final String AUTHORIZATION_HEADER = "authorization";
	static final String AUTHENTICATION_HEADER = "WWW-Authenticate";

	public void initialize(MessagePolicy reqPolicy, MessagePolicy resPolicy,
			CallbackHandler cBH, Map opts) throws AuthException {
		requestPolicy = reqPolicy;
		responsePolicy = resPolicy;
		handler = cBH;
		options = opts;
		if (options != null) {
			realmName = (String) options.get(REALM_PROPERTY_NAME);
			if (options.containsKey(GROUP_PROPERTY_NAME)) {
				defaultGroup = new String[] { (String) options
						.get(GROUP_PROPERTY_NAME) };
			}
		}
	}

	public Class[] getSupportedMessageTypes() {
		return supportedMessageTypes;
	}

	public AuthStatus validateRequest(MessageInfo msgInfo, Subject client,
			Subject server) throws AuthException {
		try {

			String username = processAuthorizationToken(msgInfo, client);
			if (username == null && requestPolicy.isMandatory()) {
				return sendAuthenticateChallenge(msgInfo);
			}

			setAuthenticationResult(username, client, msgInfo);
			return AuthStatus.SUCCESS;

		} catch (Exception e) {
			AuthException ae = new AuthException();
			ae.initCause(e);
			throw ae;
		}
	}

	private String processAuthorizationToken(MessageInfo msgInfo, Subject s)
			throws AuthException {

		HttpServletRequest request = (HttpServletRequest) msgInfo
				.getRequestMessage();

		String token = request.getHeader(AUTHORIZATION_HEADER);

		if (token != null && token.startsWith(BASIC + " ")) {

			token = token.substring(6).trim();

			// Decode and parse the authorization token
			String decoded = new String(Base64Helper.decode(token.getBytes()));

			int colon = decoded.indexOf(':');
			if (colon <= 0 || colon == decoded.length() - 1) {
				return (null);
			}

			String username = decoded.substring(0, colon);

			// use the callback to ask the container to
			// validate the password
			PasswordValidationCallback pVC = new PasswordValidationCallback(s,
					username, decoded.substring(colon + 1).toCharArray());
			try {
				handler.handle(new Callback[] { pVC });
				pVC.clearPassword();
			} catch (Exception e) {
				AuthException ae = new AuthException();
				ae.initCause(e);
				throw ae;
			}

			if (pVC.getResult()) {
				return username;
			}
		}
		return null;
	}

	private AuthStatus sendAuthenticateChallenge(MessageInfo msgInfo) {

		String realm = realmName;
		// if the realm property is set use it,
		// otherwise use the name of the server
		// as the realm name.
		if (realm == null) {

			HttpServletRequest request = (HttpServletRequest) msgInfo
					.getRequestMessage();

			realm = request.getServerName();
		}

		HttpServletResponse response = (HttpServletResponse) msgInfo
				.getResponseMessage();

		String header = BASIC + " realm=\"" + realm + "\"";
		response.setHeader(AUTHENTICATION_HEADER, header);
		response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
		return AuthStatus.SEND_CONTINUE;
	}

	public AuthStatus secureResponse(MessageInfo msgInfo, Subject service)
			throws AuthException {
		return AuthStatus.SEND_SUCCESS;
	}

	public void cleanSubject(MessageInfo msgInfo, Subject subject)
			throws AuthException {
		if (subject != null) {
			subject.getPrincipals().clear();
		}
	}

	private static final String AUTH_TYPE_INFO_KEY = "javax.servlet.http.authType";

	// distinguish the caller principal
	// and assign default groups
	private void setAuthenticationResult(String name, Subject s, MessageInfo m)
			throws IOException, UnsupportedCallbackException {
		handler.handle(new Callback[] { new CallerPrincipalCallback(s, name) });
		if (name != null) {
			// add the default group if the property is set
			if (defaultGroup != null) {
				handler.handle(new Callback[] { new GroupPrincipalCallback(s,
						defaultGroup) });
			}
			m.getMap().put(AUTH_TYPE_INFO_KEY, "BasicAuthSAM");
		}
	}

}
