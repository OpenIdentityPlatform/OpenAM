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
 * $Id: PWResetQuestionModelImpl.java,v 1.3 2009/11/18 20:52:18 qcheng Exp $
 *
 */
/**
 * Portions Copyrighted 2012-2013 ForgeRock Inc
 * Portions Copyrighted 2012 Open Source Solution Technology Corporation
 */

package com.sun.identity.password.ui.model;

import com.iplanet.services.cdm.G11NSettings;
import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.shared.Constants;
import com.sun.identity.password.plugins.NotifyPassword;
import com.sun.identity.password.plugins.PasswordGenerator;
import com.sun.identity.security.DecryptAction;
import com.sun.identity.sm.SMSException;
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

/**
 * <code>PWResetQuestionModelImpl</code> defines a set of methods that
 * are required by password reset question viewbean.
 */
public class PWResetQuestionModelImpl extends PWResetModelImpl
    implements PWResetQuestionModel,Constants {

    /**
     * Name of the token used to parse data
     */
    private static final String TOKEN = "\t";

    /**
     * Name of password reset service option
     */
    private static final String PW_RESET_OPTION =
        "iplanet-am-password-reset-option";

    /**
     * Name of password reset service notification option
     */
    private static final String PW_RESET_NOTIFICATION =
        "iplanet-am-password-reset-notification";

    /**
     * Name of user question answer attribute
     */
    private static final String PW_RESET_QUESTION_ANSWER =
        "iplanet-am-user-password-reset-question-answer";

    /**
     * Name of password reset question attribute
     */
    private static final String PW_RESET_QUESTION =
        "iplanet-am-password-reset-question";

    /**
     * Name of password reset personal question attribute
     */
    private static final String PW_RESET_PERSONAL_ANSWER =
        "iplanet-am-password-reset-user-personal-question";

    /**
     * Name of user password attribute
     */
    private static final String USER_PASSWORD_ATTR = "userpassword";

    /**
     * Name of password reset force reset attribute.
     */
    private static final String PASSWORD_RESET_FORCE_RESET = 
        "iplanet-am-password-reset-force-reset";

    /**
     * Name of user service force reset attribute.
     */
    private static final String USER_PASSWORD_RESET_FORCE_RESET = 
        "iplanet-am-user-password-reset-force-reset";

    /**
     * Name of password expiration time attribute.
     */
    private static final String PASSWORD_EXPIRATION_TIME_ATTR = 
        "passwordExpirationTime";

    /**
     * Name of password reset max number of questions
     */
    private static final String PW_RESET_MAX_NUM_OF_QUESTIONS =
        "iplanet-am-password-reset-max-num-of-questions";

    /**
     * Password Reset Service Name.
     */
    String PW_RESET_SERVICE = "iPlanetAMPasswordResetService";

    /**
     * Name of default question selected constant
     */
    private static final int DEFAULT_QUESTION_ON = 1;

    /**
     * Name of personal question selected constant
     */
    private static final int PERSONAL_QUESTION_ON = 3;

    private Map secretQuestionsMap = null;

    private static G11NSettings g11nSettings = G11NSettings.getInstance();

    /**
     * Name of password expiration time value for force reset.
     */
    private static final String PASSWORD_EXPIRATION_TIME_VALUE = 
        "19700101000000Z";

    /**
     * Constructs a password reset question model object
     *
     */
    public PWResetQuestionModelImpl() {
	super();
    } 

    private boolean isUserAnswersCorrect(
        Map map, 
        AMIdentity user,
        String realm
    ) throws SSOException, IdRepoException {
        Map optionMap = getSecretQuestions(user, realm);
        if ((optionMap == null) || optionMap.isEmpty() ||
            (map == null) || map.isEmpty()
        ) {
            return false;
        }

        Set set = map.keySet();
        for (Iterator iter = set.iterator(); iter.hasNext(); ) {
            String attrName = (String)iter.next();
            String value = (String)map.get(attrName);
            String answer = (String)optionMap.get(attrName);
            if (!value.equals(answer)) {
                return false;
            }  
        }
        return true;
    }

    private String getPasswordResetValue(String realm, AMIdentity user) {
        String password = null;
        Object obj = getPluginObject(realm, PW_RESET_OPTION);
        
        try {
            if ((obj != null) && (obj instanceof PasswordGenerator)) {
                PasswordGenerator pwGenerator = (PasswordGenerator)obj;
                password = pwGenerator.generatePassword(user);
            }
        } catch (PWResetException e) {
            debug.error("PWResetQuestionModelImpl.getPasswordResetValue", e);
        }
        return password;
    }

    /**
     * Gets the handler to the plugin object.
     *
     * @param orgDN Realm name.
     * @param attribute attribute name
     * @return handler to the plugin object
     */
    private Object getPluginObject(String orgDN, String attribute) {
        Object obj = null;

        try {
            String plugin = getAttributeValue(orgDN, attribute);
            Class c = Class.forName(plugin);
            obj = c.newInstance();
        } catch (ClassNotFoundException e) {
            debug.error("PWResetQuestionModelImpl.getPluginObject", e);
        } catch (InstantiationException e) {
            debug.error("PWResetQuestionModelImpl.getPluginObject", e);
        } catch (IllegalAccessException e) {
            debug.error("PWResetQuestionModelImpl.getPluginObject", e);
        } catch (SMSException e) {
            debug.error("PWResetQuestionModelImpl.getPluginObject", e);
        } catch (SSOException e) {
            debug.warning("PWResetQuestionModelImpl.getPluginObject", e);
        }
        return obj;

    }

    /**
     * Gets the handler to the notify password plugin.
     *
     * @param realm Realm name.
     * @return handler to the notify password plugin
     */
    private NotifyPassword getNotifyPassword(String realm) {
        NotifyPassword passwordNotify = null;

        Object obj = getPluginObject(realm, PW_RESET_NOTIFICATION);
        if ((obj != null) && (obj instanceof NotifyPassword)) {
            passwordNotify = (NotifyPassword)obj;
        }
        return passwordNotify;
    }

    /**
     * Resets the user password.
     *
     * @param uuid User Id.
     * @param realm Realm name.
     * @param map  map of user question and answer
     * @throws PWResetException if unable to reset the password
     */
    public void resetPassword(
        String uuid,
        String realm, 
        Map map)
        throws PWResetException 
    { 
        populateLockoutValues(realm);
        PWResetAccountLockout pwResetLockout = new PWResetAccountLockout(this);
        
        try {
            localeContext.setOrgLocale(realm);
            AMIdentity user = IdUtils.getIdentity(getSSOToken(), uuid);
            sendAttemptEmail(user);

            if (isUserAnswersCorrect(map, user, realm)) {
                pwResetLockout.removeUserLockoutEntry(uuid);
                String password = getPasswordResetValue(realm, user);
                if (password != null &&  password.length() > 0) {
                    NotifyPassword passwordNotify = getNotifyPassword(realm);
                    if (passwordNotify != null) {
                        changePassword(user, password, passwordNotify, 
                            uuid, realm);
                        writeLog("pwResetSuccess.message", uuid);
                    } else {
                        errorMsg = getLocalizedString("passResetError.message");
                    }
                } else {
                    errorMsg = getLocalizedString("passResetError.message");
                }
            } else {
                pwResetLockout.invalidAnswer(user);
                if (!isLockoutWarning(pwResetLockout, uuid)) {
                    errorMsg = getLocalizedString("wrongAnswer.message");
                }
            }
        } catch (SSOException e) {
            debug.warning("PWResetQuestionModelImpl.resetPassword", e);
            errorMsg = getErrorString(e);
        } catch (IdRepoException e) {
            debug.warning("PWResetQuestionModelImpl.resetPassword", e);
            errorMsg = getErrorString(e);
        }
        
        if ((errorMsg != null) && (errorMsg.length() > 0)) {
            writeLog("pwResetFail.message", errorMsg, uuid);
            throw new PWResetException(errorMsg);
        } else if ((informationMsg != null) && (informationMsg.length() > 0)) {
            writeLog("pwResetFail.message", uuid);
            throw new PWResetException(informationMsg);
        }
    }

    /**
     * Returns map of secret questions that is displayed in reset page.
     *
     * @param uuid User Id.
     * @param realm Realm name
     * @return map of secret question.
     */
    public Map getSecretQuestions(String uuid, String realm) {
        if (secretQuestionsMap == null) {
            try {
                AMIdentity user = IdUtils.getIdentity(getSSOToken(), uuid);
                getSecretQuestions(user, realm);
            } catch (SSOException e) {
                debug.warning("PWResetQuestionModelImp.getSecretQuestions", e);
            } catch (IdRepoException e) {
                debug.error("PWResetQuestionModelImp.getSecretQuestions", e);
            }
        }
        return secretQuestionsMap;
    }

    private Map getSecretQuestions(AMIdentity user, String realm) 
        throws SSOException, IdRepoException {
        if (secretQuestionsMap == null) {
            try {
                Set defaults = getDefaultQuestions(realm);
                if (user != null) {
                    Set set = user.getAttribute(PW_RESET_QUESTION_ANSWER);
                    if (set != null && !set.isEmpty()) {
                        secretQuestionsMap = getQuestionsAnswers(
                            set, defaults, realm); 
                    }
                }
            } catch (SMSException e) {
                debug.error("PWResetQuestionModelImpl.getSecretQuestions", e);
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
     * @param user User object
     */
    private void sendAttemptEmail(AMIdentity user) {
        Set<String> set = Collections.EMPTY_SET;
	Set<String> localeSet = null;

        try {
            set = user.getAttribute(getMailAttribute(user.getRealm()));
	    localeSet = user.getAttribute(USER_LOCALE_ATTR);
        } catch (SSOException e) {
            debug.error("PWResetQuestionModelImpl.sendAttemptEmail", e);
        } catch (IdRepoException e) {
            debug.error("PWResetQuestionModelImpl.sendAttemptEmail", e);
        }

	Locale userLocale = null;
	if (localeSet != null && !localeSet.isEmpty()) {
	    String localeStr = localeSet.iterator().next();
	    userLocale = (localeStr != null) ? com.sun.identity.shared.locale.Locale.getLocale(localeStr) : null;
	}
	if (userLocale == null) {
	    userLocale = localeContext.getLocale();
        }
	ResourceBundle rb = PWResetResBundleCacher.getBundle(DEFAULT_RB, userLocale);

        if (set != null && !set.isEmpty()) {
            String to[] = { set.iterator().next() };
            String msg = rb.getString("attemptEmail.message");
            String subject = rb.getString("attemptSubject.message");
	    String from = rb.getString("fromAddress.label");
	    String charset = g11nSettings.getDefaultCharsetForLocale(userLocale);
            sendEmailToUser(from, to, subject, msg,charset);
        }
    }

    /**
     * Gets password reset question title
     *
     * @param attrValue user attribute value 
     * @return password reset question title
     */
    public String getPWQuestionTitleString(String attrValue) {
        String obj[] = new String[1];
        obj[0] = attrValue;
        return MessageFormat.format(
            getLocalizedString("pwQuestion.title"), (Object[])obj);
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
    private Map getQuestionsAnswers(Set set, Set defaults, String orgDN) {
        if (set == null || set.isEmpty()) {
            return null;
        }
        Map map = new HashMap(set.size());

        Iterator iter = set.iterator();
        boolean enabled = isUserQuestionEnabled(orgDN);
        while (iter.hasNext()) {
            String value = (String)iter.next();
            String decryptStr = (String)AccessController.doPrivileged(
                new DecryptAction(value));

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
    protected Set getDefaultQuestions(String realm)
        throws SSOException, SMSException {
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
            debug.warning("PWResetQuestionModelImpl.isUserQuestionEnabled", e);
        } catch (SMSException e) {
            debug.error("PWResetQuestionModelImpl.isUserQuestionEnabled", e);
        }
        
        return enabled;
    }

    private void changePassword(
        AMIdentity user, 
        String password, 
        NotifyPassword passwordNotify,
        String uuid,
        String orgDN
    ) throws PWResetException, SSOException, IdRepoException {
        boolean forceReset = isForceReset(user, orgDN);
        SSOToken token = getSSOToken();
        
        if (token == null) {
            errorMsg = getLocalizedString("passResetError.message");
            throw new PWResetException(errorMsg);
        } else {
            ssoToken = token;
            user = IdUtils.getIdentity(token, uuid);
        }
        changeUserAttribute(user, USER_PASSWORD_ATTR, password);
        if (forceReset) {
            setUserPasswordChangedEntry(uuid, password);
        }
        notifyUser(user, passwordNotify, password,orgDN);
    }

    private void notifyUser(
        AMIdentity user, 
        NotifyPassword passwordNotify, 
        String resetValue,
	String realm
    ) {
        passwordResetMsg = getLocalizedString("emailNotify.message");
        try {
            passwordNotify.notifyPassword(
                user, resetValue, localeContext.getLocale());
        } catch (PWResetException e) {
            debug.warning("PWResetQuestionModelImpl.notifyUser", e);
            passwordResetMsg = e.getMessage();
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
        Map map = getSecretQuestions(uuid, realm);
        return (map != null) && !map.isEmpty();
    }

    /**
     * Returns <code>true</code> if the lockout warning message is to be displayed.
     *
     * @param pwResetLockout <code>PWResetAccountLockout</code> object
     * @param uuid User Id.
     * @return <code>true</code> if the lockout warning message is to be displayed
     */
    private boolean isLockoutWarning(
        PWResetAccountLockout pwResetLockout,
        String uuid
    ) {
        boolean warnUser = false;
        int warningCount = pwResetLockout.getWarnUserCount(uuid);
        
        if (warningCount < 0) {
            informationMsg = getLocalizedString("lockoutMsg.message");
            warnUser = true;
            writeLog("accountLockout.message", uuid);
        } else if (warningCount > 0) {
            String obj[] = { String.valueOf(warningCount) };
            errorMsg = MessageFormat.format(
                getLocalizedString("lockoutWarning.message"), (Object[])obj);
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
        return !isUserStatusActive(uuid) ||
            super.isUserLockout(uuid, realm);
    }

    private boolean isUserStatusActive(String uuid) {
        boolean active = false;
        try {
            AMIdentity user = IdUtils.getIdentity(getSSOToken(), uuid);
            active = user.isActive();
        } catch (SSOException e) {
           debug.warning("PWResetQuestionModelImpl.isUserStatusActive", e);
           errorMsg = getErrorString(e);
       } catch (IdRepoException e) {
           debug.warning("PWResetQuestionModelImpl.isUserStatusActive", e);
           errorMsg = getErrorString(e);
       }
       return active;
    }

    private void changeUserAttribute(
        AMIdentity user,
        String attributeName,
        String value)
        throws SSOException, IdRepoException
    {
        Map map = new HashMap(2);
        Set attribVals = new HashSet(2);
        attribVals.add(value);
        map.put(attributeName, attribVals);
        user.setAttributes(map);
        user.store();
    }

    /**
     * Returns <code>true</code> if the password reset service or user's 
     * force change password on next login is enabled.
     *
     * @param user User object.
     * @param realm Realm name.
     * @return <code>true</code> if force reset is enabled.
     */
    private boolean isForceReset(AMIdentity user, String realm) {
        boolean forceReset = false;
        
        try {
            forceReset = isAttributeSet(realm, PASSWORD_RESET_FORCE_RESET);
        } catch (SSOException e) {
            debug.warning("PWResetQuestionModelImpl.isForceReset", e);
        } catch (SMSException e) {
            debug.error("PWResetQuestionModelImpl.isForceReset", e);
        }
        
        if (!forceReset) {
            try {
                Set set = user.getAttribute(USER_PASSWORD_RESET_FORCE_RESET);
                String value = getFirstElement(set);
                forceReset = (value != null) && 
                    value.equalsIgnoreCase(STRING_TRUE);
            } catch (SSOException e) {
                debug.error("PWResetQuestionModelImpl.isForceReset", e);
            } catch (IdRepoException e) {
                debug.error("PWResetQuestionModelImpl.isForceReset", e);
            }
        }
        return forceReset;
    }

    /**
     * Sets the password expiration time attribute value to special value 
     * which will force the user to change their password when they login 
     * into admin console. It will use admin's sso token to write the value 
     * for this attribute.
     *
     * @param uuid User Id.
     * @param password Password of the user.
     */
    private void setUserPasswordChangedEntry(String uuid, String password) {
        try {
            SSOToken token = getSSOToken();
            if (token != null) {
                ssoToken = token;
                AMIdentity user = IdUtils.getIdentity(token, uuid);
                changeUserAttribute(
                    user, PASSWORD_EXPIRATION_TIME_ATTR, 
                    PASSWORD_EXPIRATION_TIME_VALUE);
            } else {
                debug.error(
                    "PWResetQuestionModelImpl.setUserPasswordChangedEntry" +
                    " Cannot not get admin sso token");
            }
        } catch (SSOException e) {
            debug.error(
                "PWResetQuestionModelImpl.setUserPasswordChangedEntry", e);
        } catch (IdRepoException e) {
            debug.error(
                "PWResetQuestionModelImpl.setUserPasswordChangedEntry", e);
        }
    }

    /**
     * Returns the maximum number of question that can be display in
     * the reset password page.
     *
     * @return maximum number of question which can be in reset password page
     */
    public int getMaxNumQuestions(String realmName) {
        int maxNum = 1;
        try {
            Set<String> set = getAttributeValues(realmName, PW_RESET_MAX_NUM_OF_QUESTIONS);

            if (set != null && !set.isEmpty()) {
                String value = set.iterator().next();
                try {
                    maxNum = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    debug.warning(
                            "PWResetQuestionModelImpl.getMaxNumQuestions", e);
                }
            }
        } catch (Exception ex) {
            debug.error("PWResetQuestionModelImpl.getMaxNumQuestions", ex);
        }

        return maxNum;
    }
}
