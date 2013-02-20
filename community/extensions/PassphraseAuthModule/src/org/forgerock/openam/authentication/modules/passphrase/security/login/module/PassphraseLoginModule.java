/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [2012] [Forgerock AS]"
 */

package org.forgerock.openam.authentication.modules.passphrase.security.login.module;

import java.security.AccessController;
import java.security.Principal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import com.iplanet.sso.SSOToken;
import com.iplanet.sso.SSOTokenManager;
import org.forgerock.openam.authentication.modules.passphrase.common.utility.CommonUtilities;
import org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants;
import org.forgerock.openam.authentication.modules.passphrase.security.login.principal.PassPhrasePrincipal;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.authentication.util.ISAuthConstants;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.AMIdentityRepository;
import com.sun.identity.idm.IdSearchControl;
import com.sun.identity.idm.IdSearchOpModifier;
import com.sun.identity.idm.IdSearchResults;
import com.sun.identity.idm.IdType;
import com.sun.identity.security.AdminTokenAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;

/**
 * This login module is used to do the second level validation of the user
 * based on the inputs of three random position of the user's passphrase.
 */
@SuppressWarnings("unchecked")
public class PassphraseLoginModule extends AMLoginModule {
	private java.security.Principal userPrincipal = null;
	private String userTokenId;
	private String userName;
	private Map sharedState;
	private StringBuffer passphraseEntered = new StringBuffer();
	private static Debug debug = Debug.getInstance("PassphraseLoginModule");
	private String realmName;
	AMIdentity user = null;
	

	/**
	 * initialize this object
	 * 
	 * @param subject
	 * @param sharedState
	 * @param options
	 */
	public void init(Subject subject, Map sharedState, Map options) {
		this.sharedState = sharedState;
	}

	/**
	 * This method does the authentication of the subject
	 * 
	 * @param callbacks the array of callbacks from the module configuration file
	 * @param state the current state of the authentication process
	 * @throws AuthLoginException if an error occurs
	 */
	public int process(Callback[] callbacks, int state) throws AuthLoginException {
		int position[] = new int[3];
		HttpServletRequest request = getHttpServletRequest();
		try {
			String positions = request.getParameter("position");
			positions = positions.substring(1, positions.length() - 1);

			String splitChars[] = positions.split(",");

			if (positions != null) {
				for (int i = 0; i < splitChars.length; i++) {
					debug.message("positions " + i + " = " + splitChars[i]);
					position[i] = Integer.parseInt(splitChars[i].trim());
				}
			}
		} catch (Exception e) {
			debug.error("Error occure while getting passphrase positions", e);
			throw new AuthLoginException("Invlid Passphrase");
		}

		if (callbacks.length < 1) {
			throw new AuthLoginException("Fatal configuration error, wrong number of callbacks");
		}
		
		try {
			if (state == 1) {
				for (int i = 0; i < callbacks.length; i++) {
					if (passphraseEntered == null) {
						passphraseEntered = new StringBuffer(new String(((PasswordCallback) callbacks[0]).getPassword()));
					} else {
						passphraseEntered = passphraseEntered.append(new String(((PasswordCallback) callbacks[i]).getPassword()));
					}
				}
				debug.message("passphrase character array " + passphraseEntered.toString());
				
				String uuid = (String) sharedState.get("javax.security.auth.login.name");
				debug.message("uuid is :" + uuid);
				
				// This happens when the authentication chain is configured as separate modules.
				// i.e., Multiple chains and not multiple modules within same Chain.
				if (uuid == null) {
					userName = getUserName(request);
					debug.message("Before token manager creation" );
					com.iplanet.sso.SSOTokenManager mgr = com.iplanet.sso.SSOTokenManager.getInstance();
					debug.message("After token manager creation" );
					com.iplanet.sso.SSOToken token = mgr.createSSOToken(request);
					debug.message("After token createion " );
					mgr.refreshSession(token);
					mgr.validateToken(token);
					debug.message("Principal name :"+token.getPrincipal().getName());
					
					debug.message("isValid token after " + mgr.isValidToken(token));
					user = new AMIdentity(token);
					debug.message("Universal ID :"+user.getUniversalId());
					debug.message("user.getDN() :"+user.getDN());
					
					
				} else {
					userName = uuid;
					user = getUser(userName);
				}
				
				String passphrase = CollectionHelper.getMapAttr(user.getAttributes(), CommonUtilities.getProperty(PassphraseConstants.USER_PASSPHRASE_ATTRIBUTE));

				// should not happen, but if it does...
				if (StringUtils.isBlank(passphrase)) {
					throw new AuthLoginException("null response from authenticator system");
				} else if (passphrase.length() > 2) {
					for (int i = 0; i < 3; i++) {
						if (passphraseEntered.toString().charAt(i) != passphrase.charAt(position[i]-1)) {
							throw new AuthLoginException("Invalid Passphrase");
						}
					}
					userTokenId = userName;
				}
			}
		} catch (Exception ex) {
			debug.error("Error occured while validating Passphrase", ex);
			throw new AuthLoginException("Invalid Passphrase");
		}
		return ISAuthConstants.LOGIN_SUCCEED;
	}

	/**
	 * This method is invoked at the end of successful authentication
	 * session. Relies on userTokenID being set by process()
	 * 
	 * @return the Principal object or null if userTokenId is null
	 */
	public Principal getPrincipal() {
		Principal thePrincipal = null;
		if (userPrincipal != null) {
			thePrincipal = userPrincipal;
		} else if (userTokenId != null) {
			debug.message("Creating user principal for user :" + userName);
			//userPrincipal = new Principal(userName);
			userPrincipal = new PassPhrasePrincipal(user.getUniversalId());
			
			thePrincipal = userPrincipal;
		}
		return thePrincipal;
	}

	/**
	 * Returns JAAS shared state user key.
	 * 
	 * @return user key.
	 */
	public String getUserKey() {
		return ISAuthConstants.SHARED_STATE_USERNAME;
	}

	/**
	 * Returns JAAS shared state password key.
	 * 
	 * @return password key
	 */
	public String getPwdKey() {
		return ISAuthConstants.SHARED_STATE_PASSWORD;
	}

	private AMIdentity getUser(String userId) {
		AMIdentity amid = null;
		try {
			debug.message("Into getUser method (" + userId + ")");
			SSOToken token = (SSOToken) AccessController.doPrivileged(AdminTokenAction.getInstance());
			
			AMIdentityRepository amir =null;
			
			if(realmName!=null){
				amir = new AMIdentityRepository(token, realmName);
				debug.message("getUser: Realm Name is set for search as  "+realmName);
			}
			else{
				debug.message("getUser: Realm Name is set for search as  "+realmName);
				amir = new AMIdentityRepository(token, "/");
			}
			
			Map<String, Set<String>> searchMap = new HashMap<String, Set<String>>(2);
			Set<String> searchSet = new HashSet<String>(2);
			searchSet.add(userId);
			searchMap.put("uid", searchSet);

			IdSearchControl isCtl = new IdSearchControl();
			isCtl.setSearchModifiers(IdSearchOpModifier.AND, searchMap);
			IdSearchResults isr = amir.searchIdentities(IdType.USER, "*", isCtl);
			Set<?> results = isr.getSearchResults();

			if ((results != null) && !results.isEmpty()) {
				if (results.size() == 1) {
					amid = (AMIdentity) results.iterator().next();
					debug.message("Got AMIdentity :" + amid);
				}
			}
		} catch (Exception e) {
			debug.error("Error retrieving the user object", e);
		}
		return amid;
	}

	private String getUserName(HttpServletRequest request) {
		String strUserName = null;
		try {
			SSOTokenManager mgr = SSOTokenManager.getInstance();
			SSOToken token = mgr.createSSOToken(request);

			AMIdentity user = new AMIdentity(token);
			realmName=user.getRealm();
			debug.message("UID is :" + user.getUniversalId());
			debug.message("Realm name is :" + user.getRealm());
			
			String uid = user.getUniversalId();
			strUserName = uid.substring(uid.indexOf("=") + 1, uid.indexOf(","));
			debug.message("Username got from token is :" + strUserName);
		} catch (Exception e) {
			debug.error("Error retrieving the user id from the request", e);
		}
		return strUserName;
	}
}