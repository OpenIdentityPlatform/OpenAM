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
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import org.forgerock.openam.authentication.modules.passphrase.common.utility.CommonUtilities;
import org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants;
import org.forgerock.openam.authentication.modules.passphrase.security.login.principal.PassPhrasePrincipal;
import org.forgerock.openam.authentication.modules.passphrase.security.model.PasswordResetOptionsData;
import org.forgerock.openam.authentication.modules.passphrase.security.model.PasswordResetOptionsModel;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.console.user.model.UMUserPasswordResetOptionsData;
import com.sun.identity.console.user.model.UMUserPasswordResetOptionsModel;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.security.EncryptAction;
import com.sun.identity.shared.datastruct.CollectionHelper;
import com.sun.identity.shared.debug.Debug;

/**
 * This class validates if the user has provided answers for all the
 * password/passphrase reset questions at the time of logging in.
 * Also it checks if force password reset flag is enabled and forces
 * the user to change the password.
 * 
 * @author Satheesh M
 */
@SuppressWarnings("unchecked")
public class QNValidationModule extends AMLoginModule {
	
	private static Debug debug = Debug.getInstance("QNValidationModule");
	
	private Map sharedState;
	private Map options;
	private Principal userPrincipal = null;
	private String userTokenId;
	private String userName;

	/**
	 * initialize this object
	 * 
	 * @param subject
	 * @param sharedState
	 * @param options
	 */
	
	public void init(Subject subject, Map sharedState, Map options) {
		this.sharedState = sharedState;
		this.options = options;
	}

	/**
	 * This method checks if the answers for the user's reset questions are entered
	 * at the time of login. 
	 */
	public int process(Callback[] callbacks, int state) throws AuthLoginException {
		try {
			userName = (String) sharedState.get("javax.security.auth.login.name");
			if (userName == null) {
				debug.error("The sharedState userId is null, hence getting user from request.");
				HttpServletRequest request = getHttpServletRequest();
				debug.message("Session token id is :"+getSSOSession().getTokenID().toString());
				userName = CommonUtilities.getUserName(request);
				debug.message("userName got from request is "+userName);
			}
			
			if (userName == null) {
				debug.error("The sharedState userId is null, hence bypassing the QNValidation module.");
				return -1;
			}
			
	        int returnState = -1;

	        switch (state) {
	        case 1:
	            if (isPasswordResetEnabled()) {
	            	returnState = 2;
	            	replaceHeader(2, " ");
	            } else if (!isResetQNsAnswered()) {
	            	returnState = 3;
	            }
	            break;
	        case 2:
	            returnState = processState2(callbacks);
	            break;
	        case 3:
	            returnState = processState3(callbacks);
	            break;
	        default:
	            throw new AuthLoginException("Invalid state : " + state);
	        }

	        return returnState;
		} catch (Exception e) {
			debug.error("Error occured while validating reset questions:", e);
		}
		return 0;
	}

	private boolean isPasswordResetEnabled() {
		AMIdentity user = CommonUtilities.getUser(userName, getHttpServletRequest());
		String isresetenabled = null;
		try {
			isresetenabled = CollectionHelper.getMapAttr(user.getAttributes(), CommonUtilities.getProperty(PassphraseConstants.USER_PASSWORD_RESET_FLAG_ATTRIBUTE));
		} catch (Exception e) {
			debug.error("Error occured while validating force password reset attribute: ", e);
		}
		return (isresetenabled != null && isresetenabled.equalsIgnoreCase("true"));
	}

	private boolean isResetQNsAnswered() {
		Map<String, String> qaMap = new HashMap<String, String>();
		int i = 0;
		boolean isAnswered = true;
		
		try {
			PasswordResetOptionsModel model = new PasswordResetOptionsModel(getHttpServletRequest(), null);
			List<PasswordResetOptionsData> answers = model.getUserAnswers(CommonUtilities.getUser(userName, getHttpServletRequest()));
			
			for (PasswordResetOptionsData qn : answers) {
				replaceCallback(3, i++, new NameCallback(qn.getQuestion()));
				qaMap.put(qn.getQuestionLocalizedName(), qn.getQuestion());
				if (qn.getDataStatus() == PasswordResetOptionsData.DEFAULT_OFF || StringUtils.isEmpty(qn.getAnswer()))
					isAnswered = false;
			}
			sharedState.put("QAMap", qaMap);
		} catch (Exception e) {
			debug.error("Error occured while validating reset questions:", e);
		}
		return isAnswered;
	}

	private int processState2(Callback[] callbacks) throws Exception {
        String newPassword = new String(((PasswordCallback)callbacks[0]).getPassword());
        String confirmPassword = new String(((PasswordCallback)callbacks[1]).getPassword());
           
        validatePassword(newPassword);
        // check minimal password length requirement
        int newPasswordLength = 0;
        if (newPassword != null) {
            newPasswordLength = newPassword.length();
        }
        
        String pwdLength = CollectionHelper.getMapAttr(options, "iplanet-am-auth-ldap-min-password-length");
        // TODO - Need to be updated..
        pwdLength = pwdLength == null? "8":pwdLength;
        
        if (newPasswordLength < Integer.parseInt(pwdLength)) {
            if (debug.messageEnabled()) {
                debug.message("New password is less than the minimal length of " + pwdLength);
            }
            getLoginState("QNValidationModule").logFailed("PasswdMinChars", "CHANGE_USER_PASSWORD_FAILED");
            replaceHeader(2, "The password should not be less than " + pwdLength + " characters.");
            return 2;
        
        } else if (!newPassword.equals(confirmPassword)) {
        	getLoginState("QNValidationModule").logFailed("PasswordsDoNotMatch", "CHANGE_USER_PASSWORD_FAILED");
            replaceHeader(2, "The password and the confirm password do not match.");
        	return 2;
        
        } else {
			Map<String, Set> attrMap = new HashMap<String, Set>();
			Set<String> attribVals = new HashSet<String>();
			attribVals.add(newPassword);
			attrMap.put("userpassword", attribVals);
			
			// remove the force password reset flag
			Set<String> passwordResetFlag = new HashSet<String>();
			passwordResetFlag.add("false");
			attrMap.put("iplanet-am-user-password-reset-force-reset", passwordResetFlag);

			AMIdentity user = CommonUtilities.getUser(userName, getHttpServletRequest());
			user.setAttributes(attrMap);
			user.store();
        }
        
        if (!isResetQNsAnswered())
        	return 3;
        else
        	return -1;
	}
	
	private int processState3(Callback[] callbacks) throws Exception {
		Map<String, String> qaMap = (Map<String, String>) sharedState.get("QAMap");
		Set attribVals = new HashSet();
		for (int i = 0; i < callbacks.length; i++) {
			if (callbacks[i] instanceof NameCallback) {
				NameCallback callback = (NameCallback)callbacks[i];
		        if (StringUtils.isNotEmpty(callback.getName())) {
		            String str = qaMap.get(callback.getPrompt()) + "\t" + callback.getName() + "\t" + UMUserPasswordResetOptionsData.DEFAULT_ON;
		            String encryptStr = (String)AccessController.doPrivileged(new EncryptAction(str));
		            attribVals.add(encryptStr);
		        } else {
		        	throw new AuthLoginException("Answers cannot be empty");
		        }
			}
		}
		
		AMIdentity user = CommonUtilities.getUser(userName, getHttpServletRequest());
		Map mapData = new HashMap();
		
		mapData.put(UMUserPasswordResetOptionsModel.PW_RESET_QUESTION_ANSWER, attribVals);
		user.setAttributes(mapData);
		user.store();
		return -1;
	}
	
	/**
	 * This method is invoked at the end of successful authentication session.
	 * Relies on userTokenID being set by process()
	 * 
	 * @return the Principal object or null if userTokenId is null
	 */
	public Principal getPrincipal() {
		Principal thePrincipal = null;
		if (userPrincipal != null) {
			thePrincipal = userPrincipal;
		} else if (userTokenId != null) {
			userPrincipal = new PassPhrasePrincipal(userName);
			thePrincipal = userPrincipal;
		}
		return thePrincipal;
	}
	
}