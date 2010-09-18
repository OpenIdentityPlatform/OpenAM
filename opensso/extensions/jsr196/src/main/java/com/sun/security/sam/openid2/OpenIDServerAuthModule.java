/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://opensso.dev.java.net/public/CDDLv1.0.html or
 * opensso/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at opensso/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id: OpenIDServerAuthModule.java,v 1.4 2009/06/23 21:10:16 rsoika Exp $
 */

package com.sun.security.sam.openid2;

import java.io.PrintWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.security.auth.Subject;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.message.AuthException;
import javax.security.auth.message.AuthStatus;
import javax.security.auth.message.MessageInfo;
import javax.security.auth.message.MessagePolicy;
import javax.security.auth.x500.X500Principal;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sun.security.sam.ServletAuthModule;

/**
 * 
 * @author monzillo, rsoika
 */
public class OpenIDServerAuthModule extends ServletAuthModule {

	private static ReentrantReadWriteLock initLock = new ReentrantReadWriteLock();
	private static Lock init_rLock = initLock.readLock();
	private static Lock init_wLock = initLock.writeLock();
	private static final String loginAction = "openid_login";
	private static final String loginURI = "/" + loginAction;

	private static final int DEBUG_TRACE = 1;
	private static final int DEBUG_LOGIN_FORM = 2;
	private static final int DEBUG_ID_PAGE = 4;
	private static final int DEBUG_ASSOCIATION = 8;
	private static final int DEBUG_CHECKID = 16;
	private static final int DEBUG_TRUST = 32;
	private static final HashMap debugStagesMap = new HashMap();

	static {
		debugStagesMap.put("all", DEBUG_TRACE + DEBUG_LOGIN_FORM
				+ DEBUG_ID_PAGE + DEBUG_ASSOCIATION + DEBUG_CHECKID
				+ DEBUG_TRUST);
		debugStagesMap.put("trace", DEBUG_TRACE);
		debugStagesMap.put("form", DEBUG_LOGIN_FORM);
		debugStagesMap.put("idpage", DEBUG_ID_PAGE);
		debugStagesMap.put("association", DEBUG_ASSOCIATION);
		debugStagesMap.put("checkid", DEBUG_CHECKID);
		debugStagesMap.put("trust", DEBUG_TRUST);
	}
	private int debugStagesMask;
	private ArrayList trustedProviders;
	private String[] trustedGroups;

	private HostnameVerifier hostnameVerifier;
	private static AssociationManager assocManager;
	private static String OPENID_SESSION_TYPE_OPTIONS_KEY = "openid.session.type";
	private static String OPENID_CHECKID_SETUP_OPTIONS_KEY = "openid.session.type";
	private static String DEBUG_STAGES_OPTIONS_KEY = "debug.stages";
	private static String TRUSTED_PROVIDER_OPTIONS_KEY = "trusted.server";
	private static String ASSIGN_GROUPS_TRUSTED_OPTIONS_KEY = "assign.groups.trusted";

	private static String IDPAGE_CONTENT_TYPE_OPTIONS_KEY = "openid.content.type";

	// DH-SHA1 or blank - defines encrption mode used on association to provider
	private String sessionType;

	private boolean checkSetup;

	public boolean checkLogCriteria(int criteria) {
		return (criteria != 0 && ((debugStagesMask & criteria) == criteria));
	}

	public void logInfo(int criteria, String tag) {
		if (checkLogCriteria(criteria)) {
			logger.log(Level.INFO, tag);
		}
	}

	public void logInfo(int criteria, String tag, String msg) {
		if (checkLogCriteria(criteria)) {
			// logger.log(Level.INFO, tag, msg);
			logger.log(Level.INFO, tag + ": " + msg);
		}
	}

	private static boolean parseCheckIDSetupOption(Map options) {
		return (options == null ? false : options
				.containsKey(OPENID_CHECKID_SETUP_OPTIONS_KEY));
	}

	private static String parseSessionTypeOption(Map options) {

		String type = "DH-SHA1";

		if (options != null) {

			String option = null;

			if (options.containsKey(OPENID_SESSION_TYPE_OPTIONS_KEY)) {
				option = (String) options.get(OPENID_SESSION_TYPE_OPTIONS_KEY);
			}

			if (option != null) {
				type = option;
			}
		}

		return type;
	}

	private static int parseDebugStagesOption(Map options) {

		int bitMap = 0;

		if (options != null) {

			String option = ((String) options.get(DEBUG_STAGES_OPTIONS_KEY));

			if (option != null) {

				StringTokenizer tokenizer = new StringTokenizer(option, ",");

				while (tokenizer.hasMoreTokens()) {

					String token = tokenizer.nextToken();

					Integer value = (Integer) debugStagesMap.get(token);

					if (value != null) {

						bitMap += value.intValue();

					}
				}
			}
		}

		return bitMap;
	}

	private ArrayList parseTrustedProviderOption(Map options) {

		ArrayList providers = null;

		if (options != null) {

			for (int i = 0; true; i++) {

				String key = TRUSTED_PROVIDER_OPTIONS_KEY + i;

				String token = (String) options.get(key);

				if (token != null) {

					X500Principal provider = new X500Principal(token);

					if (providers == null) {
						providers = new ArrayList();
					}

					if (!providers.contains(provider)) {
						providers.add(provider);
					}
				} else {
					break;
				}
			}
		}

		return providers;
	}

	/*
	 * this is only called when trusted providers are configured; in which case
	 * trusted (assigned) groups must also be configured.
	 */
	private String[] parseAssignGroupsTrustedOption(Map options)
			throws AuthException {

		String[] groupNames = new String[0];
		String groupList = (String) options
				.get(ASSIGN_GROUPS_TRUSTED_OPTIONS_KEY);
		if (groupList != null) {
			StringTokenizer tokenizer = new StringTokenizer(groupList, " ,:,;");
			Set<String> groupSet = null;
			while (tokenizer.hasMoreTokens()) {
				String name = tokenizer.nextToken();
				if (groupSet == null) {
					groupSet = new HashSet<String>();
				}
				groupSet.add(name);
			}
			if (groupSet != null && !groupSet.isEmpty()) {
				return groupSet.toArray(groupNames);
			}
		}
		if (groupNames.length == 0) {
			throw new AuthException("no trusted groups defined");
		}
		return groupNames;
	}

	/**
	 * Module specific options as configured in options Map
	 * 
	 * openid.session_type=sessionType
	 * 
	 * openid.content.type = contenttype value set in Accept header of identity
	 * page request.
	 * 
	 * 
	 * trusted.providerX=providerName (where X starts at 0, and increments X+1)
	 * each providerName is an X500 name, as established by SSL, of a trusted
	 * identity provider. When one or more trusted identity providers are
	 * configured, the assign.groups.trusted.server option must also be
	 * configured. untrusted providers are only accepted if the generic
	 * assign.groups (see below) option is specified.
	 * 
	 * assign.groups.trusted.server=groupList shared groups added only as a
	 * side-effect of authentication with a trusted server. Must be specified if
	 * trusted providers are configured.
	 * 
	 * debug-stages=all or subset {trace,form,idpage,association,checkid,trust}
	 * trace - log trace of the message processing form - log login form
	 * processing association - log openid association processing checkid - log
	 * check id processing trust - trusted server evaluation all - log all of
	 * the above.
	 * 
	 * shared options:
	 * 
	 * debug - enable debug logging by superclass
	 * 
	 * assign.groups=groupList shared groups added as a side-effect of
	 * authentication with an untrusted provider. if this option, is not
	 * specified, this module will only accept trusted providers as noted above.
	 * 
	 * javax.security.jacc.PolicyContext - policy context identifier
	 */
	@Override
	public void initialize(MessagePolicy requestPolicy,
			MessagePolicy responsePolicy, CallbackHandler handler, Map options)
			throws AuthException {

		super.initialize(requestPolicy, responsePolicy, handler, options);

		debugStagesMask = parseDebugStagesOption(options);

		sessionType = parseSessionTypeOption(options);

		checkSetup = parseCheckIDSetupOption(options);

		trustedProviders = parseTrustedProviderOption(options);

		if (trustedProviders != null) {
			trustedGroups = parseAssignGroupsTrustedOption(options);
		}

		hostnameVerifier = (new HostnameVerifier() {

			// assume that SSLSession validated trust in cert
			// now check name of server
			public boolean verify(String h, SSLSession s) {
				boolean rvalue = false;
				Principal p = null;
				try {
					if (s != null) {
						p = s.getPeerPrincipal();
					}

					logInfo(DEBUG_TRUST, "openid.hostname.verifier",
							hostnameVerifier.toString());

					if (trustedProviders == null) {
						rvalue = true;
					} else if (trustedProviders.contains(p)) {
						rvalue = true;
					}
				} catch (Exception e) {

					logger.log(Level.WARNING, "openid.hostname.verifier", e);

					rvalue = false;
				}

				logInfo(DEBUG_TRUST, "openid.hostname.verifier", "session: "
						+ s + " principal: " + p + " result: " + rvalue);

				return rvalue;
			}
		});

		// initialize shared association manager
		init_rLock.lock();
		try {
			if (assocManager != null) {
				return;
			}
		} finally {
			init_rLock.unlock();
		}
		init_wLock.lock();
		try {

			if (assocManager != null) {
				return;
			}

			assocManager = new AssociationManager(hostnameVerifier, logger,
					((debugStagesMask & DEBUG_ASSOCIATION) != 0));

		} finally {
			init_wLock.unlock();
		}
	}

	/**
	 * Authenticate a received service request.
	 * 
	 * This method is called to transform the mechanism-specific request message
	 * acquired by calling getRequestMessage (on messageInfo) into the validated
	 * application message to be returned to the message processing runtime. If
	 * the received message is a (mechanism-specific) meta-message, the method
	 * implementation must attempt to transform the meta-message into a
	 * corresponding mechanism-specific response message, or to the validated
	 * application request message. The runtime will bind a validated
	 * application message into the the corresponding service invocation.
	 * <p>
	 * This method conveys the outcome of its message processing either by
	 * returning an AuthStatus value or by throwing an AuthException.
	 * 
	 * @param messageInfo
	 *            A contextual object that encapsulates the client request and
	 *            server response objects, and that may be used to save state
	 *            across a sequence of calls made to the methods of this
	 *            interface for the purpose of completing a secure message
	 *            exchange.
	 * 
	 * @param clientSubject
	 *            A Subject that represents the source of the service request.
	 *            It is used by the method implementation to store Principals
	 *            and credentials validated in the request.
	 * 
	 * @param serviceSubject
	 *            A Subject that represents the recipient of the service
	 *            request, or null. It may be used by the method implementation
	 *            as the source of Principals or credentials to be used to
	 *            validate the request. If the Subject is not null, the method
	 *            implementation may add additional Principals or credentials
	 *            (pertaining to the recipient of the service request) to the
	 *            Subject.
	 * 
	 * @return An AuthStatus object representing the completion status of the
	 *         processing performed by the method. The AuthStatus values that
	 *         may be returned by this method are defined as follows:
	 * 
	 *         <ul>
	 *         <li> AuthStatus.SUCCESS when the application request message was
	 *         successfully validated. The validated request message is
	 *         available by calling getRequestMessage on messageInfo.
	 * 
	 *         <li> AuthStatus.SEND_SUCCESS to indicate that
	 *         validation/processing of the request message successfully
	 *         produced the secured application response message (in
	 *         messageInfo). The secured response message is available by
	 *         calling getResponseMessage on messageInfo.
	 * 
	 *         <li> AuthStatus.SEND_CONTINUE to indicate that message validation
	 *         is incomplete, and that a preliminary response was returned as
	 *         the response message in messageInfo.
	 * 
	 *         When this status value is returned to challenge an application
	 *         request message, the challenged request must be saved by the
	 *         authentication module such that it can be recovered when the
	 *         module's validateRequest message is called to process the request
	 *         returned for the challenge.
	 * 
	 *         <li> AuthStatus.SEND_FAILURE to indicate that message validation
	 *         failed and that an appropriate failure response message is
	 *         available by calling getResponseMessage on messageInfo.
	 *         </ul>
	 * 
	 * @exception AuthException
	 *                When the message processing failed without establishing a
	 *                failure response message (in messageInfo).
	 */
	public AuthStatus validateRequest(MessageInfo messageInfo,
			Subject clientSubject, Subject serviceSubject) throws AuthException {

		AuthStatus status = AuthStatus.SEND_CONTINUE;

		assert (messageInfo.getMap().containsKey(IS_MANDATORY_INFO_KEY) == isMandatory);

		HttpServletRequest request = (HttpServletRequest) messageInfo
				.getRequestMessage();

		HttpServletResponse response = (HttpServletResponse) messageInfo
				.getResponseMessage();

		debugRequest(request);

		// is it a response from an OpenID Login page?
		if (isRequestURILogin(request)) {

			logInfo(DEBUG_TRACE, "openid.received_login_form");

			// remove old openid.token if available
			request.getSession().removeAttribute(
					OpenIDToken.OPENID_SESSION_TOKEN);

			saveRequest(request);
			// initialize openid.token...
			AssociationManager.storeOpenIDParam(request, OpenIDToken.RETURN_TO,
					getReturnTo(request));
			AssociationManager.storeOpenIDParam(request,
					OpenIDToken.TRUST_ROOT, getTrustRoot(request));
			AssociationManager.storeOpenIDParam(request,
					OpenIDToken.SESSION_TYPE, this.sessionType);

			// get query to send to identity provider
			String idpURL = getIdentityProviderURL(request, null);

			if (idpURL == null) {
				logInfo(DEBUG_TRACE, "openid.empty_login_form");
				respondWithLoginForm(request, response);
			} else {
				logInfo(DEBUG_CHECKID, "openid.redirecting_to_idp");
				redirect(idpURL, response);
			}
		} else {
			// try to find a token stored in the current session...
			Map token = null;

			token = (Map) request.getSession().getAttribute(
					OpenIDToken.OPENID_SESSION_TOKEN);

			// no token found - so lets try to fetch a new one from request url
			// if a openid.identity is available...
			// automatic login can be done by urls like:
			//      http://myhost/myapp?openid.identity=MYID&openid.return_to=MY_TARGET
			if (token == null) {
				String query = request.getQueryString();				
				if (query != null && query.length() > 0
						&& query.indexOf("openid.identity") > -1) {
					
					query = URLDecoder.decode(query);
					AssociationManager.parseOpenIDToken(request, logger,
							checkLogCriteria(DEBUG_CHECKID));
					
					AssociationManager.storeOpenIDParam(request,
							OpenIDToken.TRUST_ROOT, getTrustRoot(request));
					AssociationManager.storeOpenIDParam(request,
							OpenIDToken.SESSION_TYPE, this.sessionType);
					
					String idpURL = (String) AssociationManager
						.getOpenIDParam(request, OpenIDToken.IDENTITY);

					getIdentityProviderURL(request, idpURL);
					
		
					AssociationManager.removeOpenIDParam(request,
							OpenIDToken.IS_VERIFIED);
					
				}
				token = (Map) request.getSession().getAttribute(
						OpenIDToken.OPENID_SESSION_TOKEN);

				if (token != null) {
					// verify host name?
					// remove token if host not allowed?
				}
			}

			if (token == null && isMandatory) {
				// if still no token is available response with a login form to
				// force the user to give us a openid url

				// this situation also happens typlial after a session timeout
				// saveReuest always throws the following exception
				// javax.security.auth.message.AuthException:
				// jmac.Save_http_servlet_request_failure
				// saveRequest(request);
				logInfo(DEBUG_TRACE, "openid.directing_to_login_page");
				respondWithLoginForm(request, response);

			} else if (token != null) {
				// we have a token - so verify it and complete the request....

				// if verifymode == 'once' and is_verified=='true'
				// verifyOpenIDToken will not be called
				if ("once".equals((String) options.get("verifymode"))
						&& "true".equals(token.get(OpenIDToken.IS_VERIFIED))) {
					status = AuthStatus.SUCCESS;
				} else
					// Default mode - verifytoken for each request
					status = verifyOpenIDToken(request, response);

				if (status == AuthStatus.SUCCESS) {

					logInfo(DEBUG_TRACE, "openid.do_request_authenticated");

					HttpServletRequest restored = restoreRequest(request, true);
					if (restored != null) {
						messageInfo.setRequestMessage(restored);
					}

					String sID = (String) AssociationManager.getOpenIDParam(
							request, OpenIDToken.IDENTITY);
					setCallerPrincipal(sID, clientSubject);
					messageInfo.getMap().put(AUTH_TYPE_INFO_KEY, "OpenID");

					// if verifymode == 'once' set is_verified='true' to
					// indicate that verifiyOpenIDToken should not be called
					// next time
					AssociationManager.storeOpenIDParam(request,
							OpenIDToken.IS_VERIFIED, "true");

				}

			} else {
				logInfo(DEBUG_TRACE, "openid.resource_not_protected");
				return AuthStatus.SUCCESS;
			}
		}

		return status;
	}

	URL completeIdentityURL(String uri) throws AuthException {

		URL url = null;

		try {

			boolean hasSchema = (uri.indexOf("http://") >= 0 || uri
					.indexOf("https://") >= 0);

			if (hasSchema) {
				url = new URL(uri);
			} else if (trustedProviders == null) {
				url = new URL("http://" + uri);
			} else {
				url = new URL("https://" + uri);
			}

			if (url.getPath().length() == 0) {
				int port = url.getPort();
				if (port < 0) {
					url = new URL(url.getProtocol(), url.getHost(), "/");
				} else {
					url = new URL(url.getProtocol(), url.getHost(), port, "/");
				}
			}

		} catch (Exception e) {
			String msg = "openid.invalid_identity_url";

			logger.log(Level.WARNING, msg, uri);

			AuthException ae = new AuthException(msg);
			ae.initCause(e);
			throw ae;
		}

		if (trustedProviders != null && !url.getProtocol().equals("https")) {
			String msg = "openid.untrusted_identity_url";

			logger.log(Level.SEVERE, msg, url.toString());

			AuthException ae = new AuthException(msg);
			throw ae;
		}

		logInfo(DEBUG_LOGIN_FORM, "openid.identity_url", url.toString());

		return url;

	}

	/**
	 * gets the identity url from the current request, fetches the identity page
	 * at the url, parses the page to extract the url of the identity provider,
	 * gets/create an association with the identity provider, and uses the
	 * association to create a retirection url to the identity provider.
	 * 
	 * The Method normalizes the user input
	 * 
	 * Param openIDURL is optional and did not query the openid url form the
	 * request object. This optinal param is used form verifyOpenIDToken
	 * 
	 * @param request
	 * @return the redirction url to the identity provider
	 * @throws javax.security.auth.message.AuthException
	 */
	String getIdentityProviderURL(HttpServletRequest request, String openIDuri)
			throws AuthException {

		String uri;

		if (openIDuri != null)
			uri = openIDuri;
		else {
			// get openid identifier from query param...
			uri = getQueryParameter(request, "openid_identifier");
			if (uri == null)
				uri = getQueryParameter(request, "openid_url");

			uri = normalizeIdentifier(uri);
		}
		logInfo(DEBUG_LOGIN_FORM, "openid.query_parameter:", "openid_url" + "="
				+ uri);

		// empty value for openid_url
		if (uri == null || uri.length() == 0) {
			return null;
		}

		URL identityURL = completeIdentityURL(uri);

		AssociationManager.storeOpenIDParam(request, OpenIDToken.IDENTITY,
				identityURL.toString());

		Properties properties = null;

		try {

			logInfo(DEBUG_ID_PAGE, "openid.parsing_page_from_server",
					identityURL.toString());

			properties = OpenIDPageParser.parse(identityURL, hostnameVerifier,
					logger, checkLogCriteria(DEBUG_ID_PAGE));
		} catch (Exception e) {
			String msg = "openid.connect_failed_to_idp";
			logger.log(Level.WARNING, msg, identityURL);
			logger.log(Level.WARNING, msg, e);
			AuthException ae = new AuthException(msg);
			ae.initCause(e);
			throw ae;
		}

		// Determine OpenID Version
		String openidversion = properties.getProperty("openid.version");
		logInfo(DEBUG_ID_PAGE, "openid.version", openidversion);
		AssociationManager.storeOpenIDParam(request, OpenIDToken.VERSION,
				openidversion);

		String idP = null;

		if ("2.0".equals(openidversion))
			idP = properties.getProperty("openid2.provider");
		else
			idP = properties.getProperty("openid.server");

		logInfo(DEBUG_ID_PAGE, "openid.identity_form_value", "openid.server"
				+ "=" + idP);
		// Store IDProvider url into session
		AssociationManager.storeOpenIDParam(request, OpenIDToken.IDP, idP);

		// if the identity url uses https the idp utl must use https
		try {
			if (identityURL.getProtocol().equals("https")) {
				URL idpURL = new URL(idP);
				if (!idpURL.getProtocol().equals("https")) {
					throw new RuntimeException("openid.idp_url_not_secure: "
							+ idpURL.toString());
				}
			}
		} catch (Exception e) {
			String msg = "openid.invalid_idp_url";
			logger.log(Level.WARNING, msg, uri);
			AuthException ae = new AuthException(msg);
			ae.initCause(e);
			throw ae;
		}

		String delegate = null;

		if ("2.0".equals(openidversion))
			delegate = properties.getProperty("openid2.local_id");
		else
			delegate = properties.getProperty("openid.delegate");

		logInfo(DEBUG_ID_PAGE, "openid.identity_form_value", "openid.delegate"
				+ "=" + delegate);

		// replace IDP if delegate is set
		if (delegate != null)
			AssociationManager.storeOpenIDParam(request, OpenIDToken.IDP,
					delegate);

		String rvalue = null;

		try {
			if (checkSetup) {
				rvalue = assocManager.makeCheckSetup(request);
			} else {
				rvalue = assocManager.makeCheckImmediate(request);
			}

		} catch (Exception e) {
			String msg = "openid.associate_failed_to_idp";
			logger.log(Level.WARNING, msg, idP);
			AuthException ae = new AuthException(msg);
			ae.initCause(e);
			throw ae;
		}

		if (rvalue == null) {
			String msg = "openid.idp_check_url.not.acquired";
			logger.log(Level.WARNING, msg);
			AuthException ae = new AuthException(msg);
			throw ae;
		}

		return rvalue;

	}

	/**
	 * Normalization
	 * 
	 * The end user's input MUST be normalized into an Identifier, as follows:
	 * 
	 * 1. If the user's input starts with the "xri://" prefix, it MUST be
	 * stripped off, so that XRIs are used in the canonical form.
	 * 
	 * 2. If the first character of the resulting string is an XRI Global
	 * Context Symbol ("=", "@", "+", "$", "!") or "(", as defined in Section
	 * 2.2.1 of [XRI_Syntax_2.0] (Reed, D. and D. McAlpin, “Extensible Resource
	 * Identifier (XRI) Syntax V2.0,” .), then the input SHOULD be treated as an
	 * XRI.
	 * 
	 * 3. Otherwise, the input SHOULD be treated as an http URL; if it does not
	 * include a "http" or "https" scheme, the Identifier MUST be prefixed with
	 * the string "http://". If the URL contains a fragment part, it MUST be
	 * stripped off together with the fragment delimiter character "#". See
	 * Section 11.5.2 (HTTP and HTTPS URL Identifiers) for more information.
	 * 
	 * 4. URL Identifiers MUST then be further normalized by both following
	 * redirects when retrieving their content and finally applying the rules in
	 * Section 6 of [RFC3986] (Berners-Lee, T., “Uniform Resource Identifiers
	 * (URI): Generic Syntax,” .) to the final destination URL. This final URL
	 * MUST be noted by the Relying Party as the Claimed Identifier and be used
	 * when requesting authentication (Requesting Authentication).
	 * 
	 * @param indentifier
	 * @return
	 */
	private String normalizeIdentifier(String indentifier) {
		boolean isXRI = false;
		String normalizedID = indentifier.toLowerCase();

		// 1.) "xri://" prefix
		if (normalizedID.startsWith("xri://"))
			normalizedID = normalizedID.substring(6);

		// 2.) is xri? "=", "@", "+", "$", "!", "("
		if (normalizedID.startsWith("=") || normalizedID.startsWith("@")
				|| normalizedID.startsWith("+") || normalizedID.startsWith("$")
				|| normalizedID.startsWith("!") || normalizedID.startsWith("("))
			isXRI = true;

		// 3.) http ? strip #
		if (!isXRI) {
			if (!normalizedID.startsWith("http"))
				normalizedID = "http://" + normalizedID;

			if (normalizedID.indexOf("#") > -1)
				normalizedID = normalizedID.substring(0, normalizedID
						.indexOf("#"));

			// trailing slash
			if (normalizedID.substring(7).indexOf("/") == -1)
				normalizedID = normalizedID + "/";
		}

		logInfo(DEBUG_LOGIN_FORM, "openid.Identifier normalized:", indentifier
				+ "->" + normalizedID);

		return normalizedID;
	}

	static boolean isRequestURILogin(HttpServletRequest request) {
		String uri = request.getRequestURI();
		if (uri==null)
			return false;
		// is it a response from an OpenID Login page?
		return  uri.endsWith(loginURI);
	}

	String getQueryParameter(HttpServletRequest request, String parameter) {
		String rvalue = null;
		String query = request.getQueryString();
		if (query != null) {

			StringTokenizer tokenizer = new StringTokenizer(query, "&");

			while (tokenizer.hasMoreTokens()) {

				String token = tokenizer.nextToken();

				if (token.startsWith(parameter)) {
					rvalue = token.substring(parameter.length() + 1);
					if (rvalue.length() > 0) {
						rvalue = URLDecoder.decode(rvalue);
					}
					break;
				}

			}
		}

		return rvalue;
	}

	/**
	 * This Methods verify the OpenIDToken.
	 * 
	 * The Method parses the QueryString for openid params if available. OpenID
	 * params will be stored in a hashmap which is stored in the request session
	 * object.
	 * 
	 * If the current response form the provider contains a user_setup_url param
	 * the method will redirect the user to the proivders setuppage.
	 * 
	 * In OpenID 1.0 the setupmode will be indicated by the param
	 * "openid.mode=id_res"
	 * 
	 * in OpenID 2.0 the param is "openid.mode=setup_needed"
	 * 
	 * In both cases the param openid.user_setup_url holds the url to redirect
	 * the user.
	 * 
	 * When no Setupmode is requested by the provider the method verifies the
	 * actual openid token. See: assocManager.verifyToken(token);
	 * 
	 * @param request
	 * @param response
	 * @return
	 * @throws AuthException
	 */
	AuthStatus verifyOpenIDToken(HttpServletRequest request,
			HttpServletResponse response) throws AuthException {

		AuthStatus status = AuthStatus.SEND_FAILURE;

		// if mode is id_res parse token...
		String query = request.getQueryString();
		if (query != null && query.length() > 0) {
			query = URLDecoder.decode(query);
			AssociationManager.parseOpenIDToken(request, logger,
					checkLogCriteria(DEBUG_CHECKID));
		}

		Map token = (Map) request.getSession().getAttribute(
				OpenIDToken.OPENID_SESSION_TOKEN);

		// if still no token is available return
		if (token == null)
			return status;

		logInfo(DEBUG_CHECKID, "openid.mode", (String) token.get("openid.mode"));

		if (AssociationManager.tokenModeIsResult(token)
				|| AssociationManager.tokenModeSetupNeeded(token)) {
			String setupURL = AssociationManager.getSetupURL(token);
			if (setupURL == null) {
				try {
					boolean bIsValidToken = assocManager.verifyToken(token);
					if (bIsValidToken) {
						// HttpSession session = request.getSession(true);
						// session.putValue(OPENID_SESSION_TOKEN, sToken);
						status = AuthStatus.SUCCESS;
					} else {
						// token verification failed
						logInfo(DEBUG_CHECKID, "openid.verify.failed");
						// Remove Token form session object now
						request.getSession().removeAttribute(
								OpenIDToken.OPENID_SESSION_TOKEN);

					}
				} catch (Exception e) {
					AuthException ae = new AuthException();
					ae.initCause(e);
					throw ae;
				}
			} else {
				logInfo(DEBUG_CHECKID, "openid.received_setup_url", setupURL);

				status = AuthStatus.SEND_CONTINUE;

				try {
					setupURL = assocManager.makeCheckSetup(request);
				} catch (Exception e) {
					e.printStackTrace();
				}
				redirect(setupURL, response);
			}
		} else if (AssociationManager.tokenModeIsCancel(token)) {
			logInfo(DEBUG_TRACE + DEBUG_CHECKID, "openid.received_cancel");
		} else {
			logInfo(DEBUG_CHECKID, "openid.received_invalid_openid_mode");

			// token is invalid - so try to get new token from provider
			// get new return_url from requestURL
			String new_return_url = request.getRequestURL().toString();
			AssociationManager.storeOpenIDParam(request, OpenIDToken.RETURN_TO,
					new_return_url);
			String idpURL = (String) AssociationManager.getOpenIDParam(request,
					OpenIDToken.IDENTITY);

			// compute idp URL and initialize setup or checkimidiate...
			idpURL = getIdentityProviderURL(request, idpURL);
			if (idpURL == null) {
				logInfo(DEBUG_TRACE, "openid.empty_login_form");
				respondWithLoginForm(request, response);
			} else {
				logInfo(DEBUG_CHECKID, "openid.redirecting_to_idp");
				redirect(idpURL, response);
			}
			return AuthStatus.SEND_CONTINUE;
		}

		if (status == AuthStatus.SEND_FAILURE) {
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
		}

		return status;
	}

	String getReturnTo(HttpServletRequest request) throws AuthException {

		String rvalue = getQueryParameter(request, "return_to");

		// To do: should add self signed nonce with consumer local timestamp
		// to prevent replay attacks

		logInfo(DEBUG_LOGIN_FORM, "openid.query_parameter:", "return_to" + "="
				+ rvalue);

		if (rvalue == null) {

			String msg = "openid.invalid_login_form";
			logger.log(Level.WARNING, msg);
			throw new AuthException(msg);
		}

		return rvalue;
	}

	String makeReturnTo(HttpServletRequest request) {

		StringBuffer return_to = request.getRequestURL();

		String queryString = request.getQueryString();
		if (queryString != null) {
			return_to.append("?" + queryString);
		}
		return return_to.toString();
	}

	String getTrustRoot(HttpServletRequest request) {

		StringBuffer trust_root = request.getRequestURL();
		int i = trust_root.lastIndexOf(request.getRequestURI());
		if (i > 0) {
			trust_root = new StringBuffer(trust_root.substring(0, i));
			trust_root.append(request.getContextPath());
		}
		return trust_root.toString();
	}

	/**
	 * Generate OpenID login form and write to response. It might be good to
	 * also include the Glassfish logo, although if we do that we will need to
	 * make sure it can be turned off via the module options. NB: we are only
	 * saving in respond_to the parts of the initial request that appear to be
	 * permitted (in respond_to) by openid 1.0. we have also saved the entire
	 * request in the session.
	 * 
	 * @param request
	 * @param response
	 * @throws javax.security.auth.message.AuthException
	 */

	void respondWithLoginForm(HttpServletRequest request,
			HttpServletResponse response) throws AuthException {
		try {

			String loginPage = (String) options.get("loginpage");

			if (loginPage != null && !"".equals(loginPage)) {
				// add ReturnTo URL
				// saveRequest(request);
				loginPage += "?return_to=" + makeReturnTo(request);
				// redirect(loginPage, response);
				// return;
				// RequestDispatcher d =
				// request.getRequestDispatcher(loginPage);
				// d.forward(request,response);
				// d.include(request, response);

				PrintWriter writer = response.getWriter();

				response.setContentType("text/html");

				writer.println("<html>");
				writer
						.println("<head><meta http-equiv=\"refresh\" content=\"0; URL="
								+ loginPage + "\" /></head>");
				writer.println("</html>");
				writer.flush();

				logInfo(DEBUG_LOGIN_FORM,
						"openid.responding_with_external_login_form", loginURI);

			} else {
				PrintWriter writer = response.getWriter();

				response.setContentType("text/html");

				writer.println("<html>");
				writer.println("<head></head>");
				writer.println("<br>");
				writer.println("Please Enter OpenID URL\n");
				writer.println("<hr>");
				writer.print("<form action=\"");
				writer.print(loginAction);
				writer.println("\" method=\"get\">");
				writer.println("<img src=\"http://openid.net/login-bg.gif\">");
				writer
						.println("<INPUT TYPE=\"text\" NAME=\"openid_url\" VALUE=\"\" SIZE=\"80\">");
				writer.println("<br><br>");
				writer
						.println("<INPUT TYPE=\"submit\" value=\"Login\"> <INPUT TYPE=\"reset\" value=\"Clear\">");
				writer
						.print("<INPUT TYPE=\"hidden\" NAME=\"return_to\" value=\"");
				writer.print(makeReturnTo(request));
				writer.println("\">");

				writer.println("</FORM>");
				writer.println("</html>");
				writer.flush();

				logInfo(DEBUG_LOGIN_FORM, "openid.responding_with_login_form",
						loginURI);
			}
		} catch (Exception e) {
			logger.log(Level.WARNING, "openid.error_writing_login_form");
			AuthException ae = new AuthException();
			ae.initCause(e);
			throw ae;
		}
	}

	void redirect(String url, HttpServletResponse response)
			throws AuthException {
		try {
			logInfo(DEBUG_TRACE, "openid.redirecting_to", url);

			response.setHeader("Location", url);
			response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);

		} catch (Exception e) {
			logger.log(Level.WARNING, "openid.error_redirecting_to", url);
			AuthException ae = new AuthException();
			ae.initCause(e);
			throw ae;
		}
	}

}