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

import java.security.Principal;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.ChoiceCallback;
import javax.security.auth.callback.NameCallback;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;

import org.forgerock.openam.authentication.modules.passphrase.common.utility.CommonUtilities;
import org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants;
import org.forgerock.openam.authentication.modules.passphrase.ui.model.PPResetResBundleCacher;
import org.forgerock.openam.authentication.modules.passphrase.security.model.PasswordResetOptionsData;
import org.forgerock.openam.authentication.modules.passphrase.security.model.PasswordResetOptionsModel;
import org.forgerock.openam.authentication.modules.passphrase.security.password.EmailPasswordWithForceReset;
import com.sun.identity.authentication.spi.AMLoginModule;
import com.sun.identity.authentication.spi.AuthLoginException;
import com.sun.identity.console.user.model.UMUserPasswordResetOptionsModelImpl;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.password.plugins.RandomPasswordGenerator;
import com.sun.identity.password.ui.model.PWResetAccountLockout;
import com.sun.identity.password.ui.model.PWResetModelImpl;
import com.sun.identity.shared.debug.Debug;
import com.sun.identity.sm.DNMapper;

/**
 * This module is used to perform the password reset.
 * 
 * @author Satheesh M
 */
@SuppressWarnings("unchecked")
public class PasswordResetModule extends AMLoginModule {
	
	private static Debug debug = Debug.getInstance("PasswordResetModule");
	
	private PWResetAccountLockout resetLockout = null;
	
	private Map sharedState;
	
	private String userName;

	private AMIdentity user;
	
	public static final int MODULE_ENTRY_POINT = 1; 
	
	public static final int MODULE_VALIDATE_ANSWERS = 2; 
	
	public static final int MODULE_SUCCESS_MSG = 3; 

	/**
	 * Initialize the module.
	 * 
	 * @param subject
	 * @param sharedState
	 * @param options
	 */
	public void init(Subject subject, Map sharedState, Map options) {
		this.sharedState = sharedState;
		sharedState.put("PROFILE_UPDATED",false);
	}

	/**
	 * This method checks the current state and decides the validation to be performed
	 * corresponding to the state.
	 */
	public int process(Callback[] callbacks, int state) throws AuthLoginException {
		try {
			if (state == 1)
				sharedState.put("javax.security.auth.login.name", ((NameCallback) callbacks[0]).getName());
				
			userName = (String) sharedState.get("javax.security.auth.login.name");
			HttpServletRequest request = getHttpServletRequest();
			
			if (StringUtils.isNotBlank(userName)) {
				user = CommonUtilities.getUser(userName, request);				
			} else {
				replaceHeader(MODULE_ENTRY_POINT, getLocalizedString("missingUserAttr.message"));
				return MODULE_ENTRY_POINT;
			}
			
			String submitedState = getSubmitedState(callbacks);
			if (state == MODULE_SUCCESS_MSG || (submitedState != null && !String.valueOf(state).equals(submitedState))) {
				return state;
			}

	        int returnState = 0;

	        switch (state) {
		        case MODULE_ENTRY_POINT:
		        	returnState = setResetQuestions();
		            break;
		        case MODULE_VALIDATE_ANSWERS:
		        	returnState = validateAnswers(callbacks);
		        	break;
		        default:
		            throw new AuthLoginException("Invalid state: " + state);
	        }
	        if (state != returnState && returnState == MODULE_VALIDATE_ANSWERS)
	        	replaceHeader(returnState, " ");
	        
	        return returnState;
		} catch (Exception e) {
			debug.error("Error occured while processing password reset module:", e);
			throw new AuthLoginException(e);
		}
	}

	/**
	 * This method is used to validate the security answers and on on successful
	 * validation, it resets the password and sends a mail to the user with the 
	 * generated password.
	 * 
	 * @param callbacks
	 * @return
	 * @throws Exception
	 */
	private int validateAnswers(Callback[] callbacks) throws Exception {
		for (Callback callback_ : callbacks) {
			if (callback_ instanceof NameCallback) {
				NameCallback callback = (NameCallback) callback_;
				if (StringUtils.isEmpty(callback.getName())) {
					replaceHeader(MODULE_VALIDATE_ANSWERS, getLocalizedString("missingAnswer.message"));
					return MODULE_VALIDATE_ANSWERS;
				}
			}
		}
		Map<String, String> qaMap = (Map<String, String>) sharedState.get("QAMap");
		for (Callback callback_ : callbacks) {
			if (callback_ instanceof NameCallback) {
				NameCallback callback = (NameCallback) callback_;
				if (!qaMap.get(callback.getPrompt()).equals(callback.getName().trim())) {
					resetLockout.invalidAnswer(user);
					debug.error("Invalid password reset answers provided for " + user.getUniversalId());
					int warningCount = resetLockout.getWarnUserCount(user.getUniversalId());
					if (warningCount < 0) {
						replaceHeader(MODULE_SUCCESS_MSG, "<font color=\"red\">" + getLocalizedString("lockoutMsg.message") + "</font>");
						return MODULE_SUCCESS_MSG;
					} else if (warningCount > 0) {
						replaceHeader(MODULE_VALIDATE_ANSWERS, MessageFormat.format(getLocalizedString("lockoutWarning.message"), new Object[]{warningCount}));
						return MODULE_VALIDATE_ANSWERS;
					} else {
						replaceHeader(MODULE_VALIDATE_ANSWERS, getLocalizedString("wrongAnswer.message"));
						return MODULE_VALIDATE_ANSWERS;
					}
				}
			}
		}
		resetLockout.removeUserLockoutEntry(user.getUniversalId());
		String resetPassword = new RandomPasswordGenerator().generatePassword(user);
		
		// ensure the random generated password meets the password criteria
		while (!isValidPWD(resetPassword))
			resetPassword = new RandomPasswordGenerator().generatePassword(user);
		
		Map<String, Set> attrMap = new HashMap<String, Set>();
		Set<String> attribVals = new HashSet<String>();
		attribVals.add(resetPassword);
		attrMap.put(CommonUtilities.getProperty(PassphraseConstants.USER_PASSWORD_ATTRIBUTE), attribVals);
		
		// Enable the force password reset flag
		Set<String> passwordResetFlag = new HashSet<String>();
		passwordResetFlag.add("true");
		attrMap.put(CommonUtilities.getProperty(PassphraseConstants.USER_PASSWORD_RESET_FLAG_ATTRIBUTE), passwordResetFlag);

		user.setAttributes(attrMap);
		user.store();
		
		debug.message("Password has been reset for the user: " + user.getUniversalId());

		try {
			new EmailPasswordWithForceReset().notifyPassword(user, resetPassword, getLoginLocale());
			replaceHeader(MODULE_SUCCESS_MSG, getLocalizedString("emailNotify.message"));
		} catch (Exception e) {
			debug.error("Error sending password reset mail:", e);
			replaceHeader(MODULE_SUCCESS_MSG, getLocalizedString("sendEmailFailed.message"));
		}
		return MODULE_SUCCESS_MSG;
	}
	
	/**
	 * This method is used to check if the user has provided answers for all the
	 * password/passphrase reset questions.
	 */
	private int setResetQuestions() throws Exception {
		if (user == null) {
			replaceHeader(MODULE_ENTRY_POINT, getLocalizedString("userNotExists.message"));
            return MODULE_ENTRY_POINT;
		}
		if (!user.isActive()) {
			replaceHeader(MODULE_ENTRY_POINT, getLocalizedString("userNotActive.message"));
			return MODULE_ENTRY_POINT;
		}
		
		PWResetModelImpl resetModel = new PWResetModelImpl();
		resetModel.populateLockoutValues(user.getRealm());
		resetLockout = new PWResetAccountLockout(resetModel);
		
		if (resetLockout.isLockout(user.getUniversalId())) {
			replaceHeader(MODULE_ENTRY_POINT, getLocalizedString("lockoutMsg.message"));
			return MODULE_ENTRY_POINT;
		}
		
		Map<String, String> qaMap = new HashMap<String, String>();
		PasswordResetOptionsModel model = new PasswordResetOptionsModel(getHttpServletRequest(), null);
		List<PasswordResetOptionsData> answers_ = model.getUserAnswers(user);
		List<PasswordResetOptionsData> answers = new ArrayList<PasswordResetOptionsData>();
		
		for (PasswordResetOptionsData ans : answers_) {
			if (ans.getDataStatus() == PasswordResetOptionsData.DEFAULT_ON && StringUtils.isNotEmpty(ans.getAnswer()))
				answers.add(ans);
		}
		
		if (answers == null || answers.size() == 0) {
			replaceHeader(MODULE_ENTRY_POINT, getLocalizedString("noQuestions.message"));
			return MODULE_ENTRY_POINT;
		}
		
		Collections.shuffle(answers);
		int maxQuestions = new UMUserPasswordResetOptionsModelImpl().getMaxNumQuestions(DNMapper.orgNameToRealmName(user.getRealm()));
		if (answers.size() > maxQuestions)
			answers = answers.subList(0, maxQuestions);
		
		int i = 0;
		for (PasswordResetOptionsData qn : answers) {
			replaceCallback(2, i++, new NameCallback(qn.getQuestion()));
			qaMap.put(qn.getQuestion(), qn.getAnswer());
		}
		sharedState.put("QAMap", qaMap);
		return MODULE_VALIDATE_ANSWERS;
	}

	/**
	 * This method is used to get the state of the submitted module to avoid the
	 * page refresh problem.
	 * 
	 * @param callbacks
	 * @return
	 */
	private String getSubmitedState(Callback[] callbacks) {
		for (Callback callback_ : callbacks) {
			if (callback_ instanceof ChoiceCallback) {
				ChoiceCallback callback = (ChoiceCallback) callback_;
				if (callback.getPrompt().equals("state"))
					return String.valueOf(callback.getSelectedIndexes()[0] + 1);
			}
		}
		return null;
	}

	/**
	 * This method is invoked at the end of successful authentication session.
	 */
	public Principal getPrincipal() {
		return null;
	}
	
	/**
	 * Returns localized string.
	 * 
	 * @param key resource string key.
	 * @return localized string.
	 */
	public String getLocalizedString(String key) {
		String i18nString = key;
		try {
			ResourceBundle rb = PPResetResBundleCacher.getBundle("amPasswordResetModuleMsgs", getLoginLocale());
			i18nString = rb.getString(key);
		} catch (MissingResourceException e) {
			debug.warning("Error loading resource bundle: ", e);
		}
		return i18nString;
	}
	
	/**
	 * This method validates if the password matches at least three from the following categories
	 * 	- English upper case character
	 * 	- English lower case character
	 * 	- Numeric value (0-9)
	 * 	- Special character (e.g. ? #, %)
	 * 
	 * @param password
	 */
	private boolean isValidPWD(String password) throws Exception {
		int test = password.matches(".*([A-Z])+.*")? 1:0;
		test += password.matches(".*([a-z])+.*")? 1:0;
		test += password.matches(".*(\\d)+.*")? 1:0;
		test += password.matches(".*(\\W)+.*")? 1:0;
		
		return test >= 3;
	}
}