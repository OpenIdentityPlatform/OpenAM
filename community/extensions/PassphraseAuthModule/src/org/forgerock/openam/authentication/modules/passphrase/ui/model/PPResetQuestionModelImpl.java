/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: PPResetQuestionModelImpl.java,v 1.3 2009/11/18 20:52:18 qcheng Exp $
 *
 *    "Portions Copyrighted [2012] [Forgerock AS]"
 */

package org.forgerock.openam.authentication.modules.passphrase.ui.model;

import java.security.AccessController;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;

import com.iplanet.services.cdm.G11NSettings;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import org.forgerock.openam.authentication.modules.passphrase.common.utility.CommonUtilities;
import org.forgerock.openam.authentication.modules.passphrase.common.utility.PassphraseConstants;
import org.forgerock.openam.authentication.modules.passphrase.plugins.EmailPassphrase;
import org.forgerock.openam.authentication.modules.passphrase.plugins.NotifyPassphrase;
import org.forgerock.openam.authentication.modules.passphrase.plugins.PassphraseGenerator;
import org.forgerock.openam.authentication.modules.passphrase.plugins.RandomPassphraseGenerator;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.DecryptAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.SMSException;

/**
 * <code>PPResetQuestionModelImpl</code> defines a set of methods that are
 * required by password reset question viewbean.
 */
public class PPResetQuestionModelImpl extends PPResetModelImpl implements PPResetQuestionModel, PassphraseConstants {

	/**
	 * Name of the token used to parse data
	 */
	private static final String TOKEN = "\t";

	/**
	 * Name of user question answer attribute
	 */
	private static final String PW_RESET_QUESTION_ANSWER = CommonUtilities.getProperty(PassphraseConstants.USER_RESET_QUESTION_ANSWER_ATTRIBUTE);

	/**
	 * Name of password reset question attribute
	 */
	private static final String PW_RESET_QUESTION = CommonUtilities.getProperty(PassphraseConstants.RESET_QUESTIONS_ATTRIBUTE);

	/**
	 * Name of password reset personal question attribute
	 */
	private static final String PW_RESET_PERSONAL_ANSWER = "iplanet-am-password-reset-user-personal-question";

	/**
	 * Name of user password attribute
	 */
	private static final String USER_PASSPHRASE_ATTR = CommonUtilities.getProperty(PassphraseConstants.USER_PASSPHRASE_ATTRIBUTE);

	/**
	 * Name of default question selected constant
	 */
	private static final int DEFAULT_QUESTION_ON = 1;

	/**
	 * Name of personal question selected constant
	 */
	private static final int PERSONAL_QUESTION_ON = 3;

	private Map<String, String> secretQuestionsMap = null;

	private static G11NSettings g11nSettings = G11NSettings.getInstance();

	/**
	 * Constructs a passphrase reset question model object
	 */
	public PPResetQuestionModelImpl() {
		super();
	}

	private boolean isUserAnswersCorrect(Map<String, String> map, AMIdentity user, String realm)
			throws SSOException, IdRepoException {
		Map<String, String> optionMap = getSecretQuestions(user, realm);
		if ((optionMap == null) || optionMap.isEmpty() || (map == null) || map.isEmpty()) {
			return false;
		}
		Set<String> set = map.keySet();
		for (Iterator<String> iter = set.iterator(); iter.hasNext();) {
			String attrName = iter.next();
			String value = map.get(attrName);
			String answer = optionMap.get(attrName);
			if (!value.equals(answer)) {
				return false;
			}
		}
		return true;
	}

	private String getPassphraseResetValue(String realm, AMIdentity user) {
		String password = null;
		PassphraseGenerator pwGenerator = new RandomPassphraseGenerator();
		try {
			password = pwGenerator.generatePassphrase(user);
		}
		catch (Exception e) {
			debug.error("PPResetQuestionModelImpl.getPassphraseResetValue", e);
		}
		return password;
	}

	/**
	 * Gets the handler to the notify password plugin.
	 * 
	 * @param realm Realm name.
	 * @return handler to the notify password plugin
	 */
	private NotifyPassphrase getNotifyPassword(String realm) {
		return new EmailPassphrase();
	}

	/**
	 * Resets the user password.
	 * 
	 * @param uuid User Id.
	 * @param realm Realm name.
	 * @param map map of user question and answer
	 * @throws PPResetException if unable to reset the passphrase
	 */
	public void resetPassphrase(String uuid, String realm, Map<String, String> map) throws PPResetException {
		populateLockoutValues(realm);
		PPResetAccountLockout ppResetLockout = new PPResetAccountLockout(this);
		try {
			localeContext.setOrgLocale(realm);
			AMIdentity user = IdUtils.getIdentity(getSSOToken(), uuid);

			sendAttemptEmail(user);
			if (isUserAnswersCorrect(map, user, realm)) {
				ppResetLockout.removeUserLockoutEntry(uuid);
				String passphrase = getPassphraseResetValue(realm, user);
				if (passphrase != null && passphrase.length() > 0) {
					NotifyPassphrase passwordNotify = getNotifyPassword(realm);
					if (passwordNotify != null) {
						changePassphrase(user, passphrase, passwordNotify, uuid, realm);
						writeLog("ppResetSuccess.message", uuid);
					} else {
						errorMsg = getLocalizedString("passResetError.message");
					}
				} else {
					errorMsg = getLocalizedString("passResetError.message");
				}
			} else {
				ppResetLockout.invalidAnswer(user);
				if (!isLockoutWarning(ppResetLockout, uuid)) {
					errorMsg = getLocalizedString("wrongAnswer.message");
				}
			}
		} catch (SSOException e) {
			debug.warning("PPResetQuestionModelImpl.resetPassword", e);
			errorMsg = getErrorString(e);
		} catch (IdRepoException e) {
			debug.warning("PPResetQuestionModelImpl.resetPassword", e);
			errorMsg = getErrorString(e);
		}

		if ((errorMsg != null) && (errorMsg.length() > 0)) {
			throw new PPResetException(errorMsg);
		} else if ((informationMsg != null) && (informationMsg.length() > 0)) {
			writeLog("ppResetFail.message", uuid);
			throw new PPResetException(informationMsg);
		}
	}

	/**
	 * Returns map of secret questions that is displayed in reset page.
	 * 
	 * @param uuid User Id.
	 * @param realm Realm name
	 * @return map of secret question.
	 */
	public Map<String, String> getSecretQuestions(String uuid, String realm) {
		if (secretQuestionsMap == null) {
			try {
				AMIdentity user = IdUtils.getIdentity(getSSOToken(), uuid);
				getSecretQuestions(user, realm);
			} catch (SSOException e) {
				debug.warning("PPResetQuestionModelImp.getSecretQuestions", e);
			} catch (IdRepoException e) {
				debug.error("PPResetQuestionModelImp.getSecretQuestions", e);
			}
		}
		return secretQuestionsMap;
	}

	@SuppressWarnings("unchecked")
	private Map<String, String> getSecretQuestions(AMIdentity user, String realm)
			throws SSOException, IdRepoException {
		if (secretQuestionsMap == null) {
			try {
				Set<String> defaults = getDefaultQuestions(realm);
				if (user != null) {
					Set<String> set = user.getAttribute(PW_RESET_QUESTION_ANSWER);
					if (set != null && !set.isEmpty()) {
						secretQuestionsMap = getQuestionsAnswers(set, defaults, realm);
					}
				}
			} catch (SMSException e) {
				debug.error("PPResetQuestionModelImpl.getSecretQuestions", e);
			}
		}
		return secretQuestionsMap;
	}

	/**
	 * Gets the localized string for the question
	 * 
	 * @param question i8n key for the question
	 * @return localized string for the question
	 */
	public String getLocalizedStrForQuestion(String question) {
		return getL10NAttributeName(PW_RESET_SERVICE, question);
	}

	/**
	 * Sents reset password attempt email to the user.
	 * 
	 * @param user
	 *            User object
	 */
	@SuppressWarnings("unchecked")
	private void sendAttemptEmail(AMIdentity user) {
		Set<String> set = Collections.EMPTY_SET;
		Set<String> localeSet = null;

		try {
			set = (Set<String>) user.getAttribute(USER_MAIL_ATTR);
			localeSet = (Set) user.getAttribute(Constants.USER_LOCALE_ATTR);
		} catch (SSOException e) {
			debug.error("PPResetQuestionModelImpl.sendAttemptEmail", e);
		} catch (IdRepoException e) {
			debug.error("PPResetQuestionModelImpl.sendAttemptEmail", e);
		}

		Locale userLocale = null;
		if (localeSet != null && !localeSet.isEmpty()) {
			String localeStr = localeSet.iterator().next().toString();
			userLocale = (localeStr != null) ? com.iplanet.am.util.Locale.getLocale(localeStr) : null;
		}
		if (userLocale == null) {
			userLocale = localeContext.getLocale();
		}
		ResourceBundle rb = PPResetResBundleCacher.getBundle(DEFAULT_RB, userLocale);

		if (set != null && !set.isEmpty()) {
			String to[] = { (String) set.iterator().next() };
			String msg = rb.getString("attemptEmail.message");
			String subject = rb.getString("attemptSubject.message");
			String from = rb.getString("fromAddress.label");
			String charset = g11nSettings.getDefaultCharsetForLocale(userLocale);
			sendEmailToUser(from, to, subject, msg, charset);
		}
	}

	/**
	 * Gets password reset question title
	 * 
	 * @param attrValue
	 *            user attribute value
	 * @return password reset question title
	 */
	public String getPWQuestionTitleString(String attrValue) {
		Object[] obj = { attrValue };
		return MessageFormat.format(getLocalizedString("ppQuestion.title"), obj);
	}

	/**
	 * Gets ok button label
	 * 
	 * @return ok button label
	 */
	public String getOKBtnLabel() {
		return getLocalizedString("ok.button");
	}

	/**
	 * Gets previous button label
	 * 
	 * @return previous button label
	 */
	public String getPreviousBtnLabel() {
		return getLocalizedString("previous.button");
	}

	/**
	 * Sets no questions configured message
	 */
	public void setNoQuestionsInfoMsg() {
		informationMsg = getLocalizedString("noQuestions.message");
	}

	/**
	 * Gets secret questions that display in the password reset page.
	 * 
	 * @param set question and answer set
	 * @param defaults administrator configured default questions
	 * @param orgDN DN of organization
	 * @return secret questions that are displayed in the password reset page
	 */
	@SuppressWarnings("unchecked")
	private Map<String, String> getQuestionsAnswers(Set<String> set, Set<String> defaults, String orgDN) {
		if (set == null || set.isEmpty()) {
			return null;
		}
		Map<String, String> map = new HashMap<String, String>(set.size());

		Iterator<String> iter = set.iterator();
		boolean enabled = isUserQuestionEnabled(orgDN);
		while (iter.hasNext()) {
			String value = (String) iter.next();
			
			String decryptStr = (String)AccessController.doPrivileged(new DecryptAction(value));

			StringTokenizer st = new StringTokenizer(decryptStr, TOKEN);
			if (st.countTokens() == 3) {
				String question = st.nextToken();
				String answer = st.nextToken();
				int dataStatus = Integer.parseInt(st.nextToken());
				if (enabled) {
					if (dataStatus == PERSONAL_QUESTION_ON) {
						map.put(question, answer);
					} else if (dataStatus == DEFAULT_QUESTION_ON) {
						if (defaults.contains(question)) {
							map.put(question, answer);
						}
					}
				} else {
					if (dataStatus == DEFAULT_QUESTION_ON) {
						if (defaults.contains(question)) {
							map.put(question, answer);
						}
					}
				}
			}
		}
		return map;
	}

	/**
	 * Returns default administrator configured questions
	 * 
	 * @param realm Realm name.
	 * @return default questions
	 */
	@SuppressWarnings("unchecked")
	protected Set<String> getDefaultQuestions(String realm) throws SSOException, SMSException {
		return getAttributeValues(realm, PW_RESET_QUESTION);
	}

	/**
	 * Returns <code>true</code> if user personal question/answer feature is enabled
	 * 
	 * @param orgDN DN of organization
	 * @return <code>true</code> if the feature is enabled, false otherwise
	 */
	public boolean isUserQuestionEnabled(String orgDN) {
		boolean enabled = false;
		try {
			String value = getAttributeValue(orgDN, PW_RESET_PERSONAL_ANSWER);
			enabled = (value != null) && value.equalsIgnoreCase(STRING_TRUE);
		} catch (SSOException e) {
			debug.warning("PPResetQuestionModelImpl.isUserQuestionEnabled", e);
		} catch (SMSException e) {
			debug.error("PPResetQuestionModelImpl.isUserQuestionEnabled", e);
		}
		return enabled;
	}

	private void changePassphrase(AMIdentity user, String passphrase, NotifyPassphrase passwordNotify, String uuid, String orgDN)
			throws PPResetException, SSOException, IdRepoException {
		SSOToken token = getSSOToken();

		if (token == null) {
			errorMsg = getLocalizedString("passphraseResetError.message");
			throw new PPResetException(errorMsg);
		} else {
			ssoToken = token;
			user = IdUtils.getIdentity(token, uuid);
		}
		changeUserAttribute(user, USER_PASSPHRASE_ATTR, passphrase);
		notifyUser(user, passwordNotify, passphrase, orgDN);
	}

	private void notifyUser(AMIdentity user, NotifyPassphrase passwordNotify, String resetValue, String realm) {
		passphraseResetMsg = getLocalizedString("passphraseEmailNotify.message");
		try {
			passwordNotify.notifyPassphrase(user, resetValue, localeContext.getLocale());
		} catch (Exception e) {
			debug.warning("PPResetQuestionModelImpl.notifyUser", e);
			passphraseResetMsg = e.getMessage();
		}
	}

	/**
	 * Returns missing answer message .
	 * 
	 * @return missing answer message.
	 */
	public String getMissingAnswerMessage() {
		errorMsg = getLocalizedString("missingAnswer.message");
		return errorMsg;
	}

	/**
	 * Returns <code>true</code> if the secret questions are available for a user
	 * 
	 * @param uuid User Id.
	 * @param realm Realm name.
	 * @return <code>true</code> if the questions are available, false otherwise
	 */
	public boolean isQuestionAvailable(String uuid, String realm) {
		Map<String, String> map = getSecretQuestions(uuid, realm);
		return (map != null) && !map.isEmpty();
	}

	/**
	 * Returns <code>true</code> if the lockout warning message is to be displayed.
	 * 
	 * @param pwResetLockout <code>PWResetAccountLockout</code> object
	 * @param uuid User Id.
	 * @return <code>true</code> if the lockout warning message is to be displayed
	 */
	private boolean isLockoutWarning(PPResetAccountLockout pwResetLockout, String uuid) {
		boolean warnUser = false;
		int warningCount = pwResetLockout.getWarnUserCount(uuid);

		if (warningCount < 0) {
			informationMsg = getLocalizedString("lockoutMsg.message");
			warnUser = true;
			writeLog("accountLockout.message", uuid);
		} else if (warningCount > 0) {
			Object obj[] = { String.valueOf(warningCount) };
			errorMsg = MessageFormat.format(getLocalizedString("lockoutWarning.message"), obj);
			warnUser = true;
		}
		return warnUser;
	}

	/**
	 * Returns <code>true</code> if the user is not active or is lockout.
	 * 
	 * @param uuid User Id.
	 * @param realm Realm name.
	 * @return <code>true</code> if the user is active and is not lockout.
	 */
	public boolean isUserLockout(String uuid, String realm) {
		return !isUserStatusActive(uuid) || super.isUserLockout(uuid, realm);
	}

	@SuppressWarnings("unchecked")
	private boolean isUserStatusActive(String uuid) {
		boolean active = false;
		try {
			AMIdentity user = IdUtils.getIdentity(getSSOToken(), uuid);
			Set<String> set = user.getAttribute(USER_SERVICE_ACTIVE_STATUS);
			String userStatus = getFirstElement(set);
			active = userStatus.equalsIgnoreCase(ACTIVE);
		} catch (SSOException e) {
			debug.warning("PPResetQuestionModelImpl.isUserStatusActive", e);
			errorMsg = getErrorString(e);
		} catch (IdRepoException e) {
			debug.warning("PPResetQuestionModelImpl.isUserStatusActive", e);
			errorMsg = getErrorString(e);
		}
		return active;
	}

	private void changeUserAttribute(AMIdentity user, String attributeName,
			String value) throws SSOException, IdRepoException {
		Map<String, Set<String>> map = new HashMap<String, Set<String>>(2);
		Set<String> attribVals = new HashSet<String>(2);
		attribVals.add(value);
		map.put(attributeName, attribVals);
		user.setAttributes(map);
		user.store();
	}
}