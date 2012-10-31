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
 * $Id: UMUserPasswordResetOptionsModelImpl.java,v 1.5 2010/01/27 18:21:37 veiming Exp $
 *
 */

package com.sun.identity.console.user.model;

import com.iplanet.sso.SSOException;
import com.iplanet.sso.SSOToken;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.delegation.DelegationEvaluator;
import com.sun.identity.delegation.DelegationException;
import com.sun.identity.delegation.DelegationPermission;
import com.sun.identity.idm.AMIdentity;
import com.sun.identity.idm.IdRepoException;
import com.sun.identity.idm.IdUtils;
import com.sun.identity.security.DecryptAction;
import com.sun.identity.security.EncryptAction;
import com.sun.identity.shared.Constants;
import com.sun.identity.sm.OrganizationConfigManager;
import com.sun.identity.sm.SchemaType;
import com.sun.identity.sm.ServiceSchemaManager;
import com.sun.identity.sm.SMSException;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;

/* - LOG COMPLETE - */

public class UMUserPasswordResetOptionsModelImpl
    extends AMModelBase
    implements UMUserPasswordResetOptionsModel
{
    private OrganizationConfigManager orgCfgMgr;
    private static SSOToken adminSSOToken =
        AMAdminUtils.getSuperAdminSSOToken();

    public UMUserPasswordResetOptionsModelImpl(
        HttpServletRequest req,
        Map map
    ) {
        super(req, map);
    }

    public UMUserPasswordResetOptionsModelImpl() {
        // do nothing
    }
 
    /**
     * Returns a map of question to its localized name.
     *
     * @param realmName Name of realm.
     * @return a map of question to its localized name.
     */
    private Map getQuestions(String realmName) {
        Map mapQuestions = null;
        Set questions = getAttributeValues(
            realmName, PW_RESET_QUESTION, AMAdminConstants.PW_RESET_SERVICE);

        if ((questions != null) && !questions.isEmpty()) {
            mapQuestions = new HashMap(questions.size() *2);
            ResourceBundle rb = getServiceResourceBundle(
                AMAdminConstants.PW_RESET_SERVICE);
            
            for (Iterator iter = questions.iterator(); iter.hasNext(); ) {
                String val = (String)iter.next();
                String label = val;
                try {
                    label = (rb == null) ? val : rb.getString(val);
                } catch (MissingResourceException mre) {
                    debug.warning("no i18nKey defined for question " +
                        mre.getMessage());
                }
                mapQuestions.put(val, label);
            }
        } else {
            debug.message("UMUserPasswordResetOptionsModelImpl " + 
                "there were no questions defined for this user");
        }
        return mapQuestions;
    }

    /**
     * Returns true if user personal question/answer feature is enabled.
     *
     * @param realmName Name of realm.
     * @return true if user personal question/answer feature is enabled.
     */
    public boolean isUserQuestionEnabled(String realmName) {
        Set values = getAttributeValues(
            realmName, PW_RESET_PERSONAL_ANSWER,
            AMAdminConstants.PW_RESET_SERVICE);
        return ((String)AMAdminUtils.getValue(values)).equalsIgnoreCase("true");
    }

    /**
     * Returns the maximum number of question that can be display in
     * the reset password page.
     *
     * @return maximum number of question which can be in reset password page
     */
    public int getMaxNumQuestions(String realmName) {
        int maxNum = 1;
        Set set = getAttributeValues(realmName,
            PW_RESET_MAX_NUM_OF_QUESTIONS, AMAdminConstants.PW_RESET_SERVICE);

        if (set != null && !set.isEmpty()) {
            String value = (String)set.iterator().next();
            try {
                maxNum = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                debug.warning(
                    "UMUserPasswordResetOptionsModelImplxNumQuestions.", e);
            }
        }

        return maxNum;
    }


    /**
     * Modifies user's password reset option.
     *
     * @param questionAnswers List of
     *        <code>UMUserPasswordResetOptionsData</code> objects.
     * @param userId Universal ID of user.
     * @param forceReset true to force reset.
     * @throws AMConsoleException if password reset option cannot be modified.
     */
    public void modifyUserOption(
        List questionAnswers,
        String userId,
        boolean forceReset
    ) throws AMConsoleException {
        Map mapData = new HashMap(4);
        if (isLoggedInUser(userId)) {
            Set attribVals = formatOptionData(questionAnswers);
            mapData.put(PW_RESET_QUESTION_ANSWER, attribVals);
        }

        if (isRealmAdmin()) {
            Set set = new HashSet(2);
            set.add(String.valueOf(forceReset));
            mapData.put(PW_RESET_FORCE_RESET, set);
        }

        if (!mapData.isEmpty()) {
            String[] params = {userId, PW_RESET_QUESTION_ANSWER};
            logEvent("ATTEMPT_MODIFY_IDENTITY_ATTRIBUTE_VALUE", params);
            try {
                AMIdentity amid = IdUtils.getIdentity(
                    getUserSSOToken(), userId);
                amid.setAttributes(mapData);
                amid.store();
                logEvent("SUCCEED_MODIFY_IDENTITY_ATTRIBUTE_VALUE", params);
            } catch (SSOException e) {
                String strError = getErrorString(e);
                String[] paramsEx = {userId, PW_RESET_QUESTION_ANSWER,
                    strError};
                logEvent("SSO_EXCEPTION_MODIFY_IDENTITY_ATTRIBUTE_VALUE",
                    paramsEx);
                throw new AMConsoleException(strError);
            } catch (IdRepoException e) {
                String strError = getErrorString(e);
                String[] paramsEx = {userId, PW_RESET_QUESTION_ANSWER,
                    strError};
                logEvent("IDM_EXCEPTION_MODIFY_IDENTITY_ATTRIBUTE_VALUE",
                    paramsEx);
                throw new AMConsoleException(strError);
            }
        }
    }

    /**
     * Returns true if force reset is set.
     *
     * @param userId Universal ID of user.
     * @return true if force reset is set.
     */
    public boolean isForceReset(String userId) {
        boolean forcedReset = false;

        try {
            String[] params = {userId, PW_RESET_FORCE_RESET};
            logEvent("ATTEMPT_READ_IDENTITY_ATTRIBUTE_VALUE", params);
            AMIdentity amid = IdUtils.getIdentity(getUserSSOToken(), userId);
            Set set = amid.getAttribute(PW_RESET_FORCE_RESET);
            logEvent("SUCCEED_READ_IDENTITY_ATTRIBUTE_VALUE", params);

            if ((set != null) && !set.isEmpty()) {
                forcedReset = ((String)AMAdminUtils.getValue(set))
                    .equalsIgnoreCase("true");
            }
        } catch (SSOException e) {
            String[] paramsEx = {userId, PW_RESET_FORCE_RESET,
                getErrorString(e)};
            logEvent("SSO_EXCEPTION_READ_IDENTITY_ATTRIBUTE_VALUE", paramsEx);
            debug.warning(
                "UMUserPasswordResetOptionsModelImpl.isForceReset", e);
        } catch (IdRepoException e) {
            String[] paramsEx = {userId, PW_RESET_FORCE_RESET,
                getErrorString(e)};
            logEvent("IDM_EXCEPTION_READ_IDENTITY_ATTRIBUTE_VALUE", paramsEx);
            debug.warning(
                "UMUserPasswordResetOptionsModelImpl.isForceReset", e);
        }

        return forcedReset;
    }


    private Set formatOptionData(List questionAnswers)
        throws AMConsoleException
    {
        Set attribVals = null;

        /*
         * If the question and answer map is empty then user has selected
         * nothing or has unselected previous selected question. Stored
         * empty string to overwrite previous data.
         */
        if ((questionAnswers == null) || questionAnswers.isEmpty()) {
            attribVals = new HashSet(2);
            attribVals.add("");
        } else {
            attribVals = new HashSet(questionAnswers.size() *2);

            // Data Format: question \t answer \t selection status
            for (Iterator iter = questionAnswers.iterator(); iter.hasNext(); ) {
                UMUserPasswordResetOptionsData data =
                    (UMUserPasswordResetOptionsData)iter.next();
                data.validate();

                if (data != null) {
                    String str = data.getQuestion() + DELIMITER +
                        data.getAnswer() + DELIMITER +
                        data.getDataStatus();
                    String encryptStr = (String)AccessController.doPrivileged(
                        new EncryptAction(str));
                    attribVals.add(encryptStr);
                }
            }
        }

        return attribVals;
    }

    /**
     * Returns user's answers for password reset questions.
     *
     * @param userId Universal ID of user.
     * @return user's answers for password reset questions.
     * @throws AMConsoleException if answers cannot be retrieved.
     */
    public List getUserAnswers(String userId)
        throws AMConsoleException {
        try {
            String[] params = {userId, PW_RESET_QUESTION_ANSWER};
            logEvent("ATTEMPT_READ_IDENTITY_ATTRIBUTE_VALUE", params);

            AMIdentity amid = IdUtils.getIdentity(getUserSSOToken(), userId);
            Set questions = amid.getAttribute(PW_RESET_QUESTION_ANSWER);
            logEvent("SUCCEED_READ_IDENTITY_ATTRIBUTE_VALUE", params);
            return getUserPasswordResetAnswers(amid, questions);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {userId, PW_RESET_QUESTION_ANSWER, strError};
            logEvent("SSO_EXCEPTION_READ_IDENTITY_ATTRIBUTE_VALUE", paramsEx);
            throw new AMConsoleException(strError);
        } catch (IdRepoException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {userId, PW_RESET_QUESTION_ANSWER, strError};
            logEvent("IDM_EXCEPTION_READ_IDENTITY_ATTRIBUTE_VALUE", paramsEx);
            throw new AMConsoleException(getErrorString(e));
        }
    }

    private List getUserPasswordResetAnswers(AMIdentity amid, Set questions) {
        List mapQuestionAnswer = new ArrayList();
        String realmName = amid.getRealm();
        boolean showUserQn = isUserQuestionEnabled(realmName);
        UMUserPasswordResetOptionsData personalQn = null;
        Map localizedQuestions = getQuestions(realmName);
        if ((localizedQuestions == null) || localizedQuestions.isEmpty()) {
            return mapQuestionAnswer;
        }
        
        Set selectedQns = new HashSet(localizedQuestions.size() *2);
        Map userAnswers = parseUserQuestionAnswers(
            questions, showUserQn, localizedQuestions);

        for (Iterator i = localizedQuestions.keySet().iterator();
            i.hasNext();
        ) {
            String qn = (String)i.next();
            UMUserPasswordResetOptionsData data =
                (UMUserPasswordResetOptionsData)userAnswers.get(qn);
            if (data != null) {
                mapQuestionAnswer.add(data);
                selectedQns.add(qn);
            } else {
                mapQuestionAnswer.add(
                    new UMUserPasswordResetOptionsData(qn, 
                        (String)localizedQuestions.get(qn),
                        "", UMUserPasswordResetOptionsData.DEFAULT_OFF));
            }
        }

        if (personalQn == null) {
            personalQn = getPersonalQuestionAnswer(userAnswers);
        }

        if (showUserQn) {
            if (personalQn == null) {
                personalQn = new UMUserPasswordResetOptionsData("", "", "",
                    UMUserPasswordResetOptionsData.PERSONAL_OFF);
            }
            mapQuestionAnswer.add(personalQn);
        }

        return mapQuestionAnswer;
    }

    private UMUserPasswordResetOptionsData getPersonalQuestionAnswer(
        Map<String, UMUserPasswordResetOptionsData> userAnswers) {
        for (UMUserPasswordResetOptionsData data : userAnswers.values()) {
            if (data.isPersonalQuestion()) {
                return data;
            }
        }
        return null;
    }

    private Map parseUserQuestionAnswers(
        Set questions,
        boolean showUserQn,
        Map localizedQuestions
    ) {
        Map map = new HashMap();
        if ((questions != null) && !questions.isEmpty()) {
            for (Iterator i = questions.iterator(); i.hasNext(); ) {
                String value = (String)i.next();
                String decryptStr = (String)AccessController.doPrivileged(
                    new DecryptAction(value));
                StringTokenizer st = new StringTokenizer(decryptStr, DELIMITER);
                if (st.countTokens() == 3) {
                    UMUserPasswordResetOptionsData data = getPwdResetOptionData(
                        st, localizedQuestions, showUserQn);
                    if (data != null) {
                        map.put(data.getQuestion(), data);
                    }
                }
            }
        }
        return map;
    }

    private UMUserPasswordResetOptionsData getPwdResetOptionData(
        StringTokenizer st,
        Map localizedQuestions,
        boolean showUserQn
    ) {
        UMUserPasswordResetOptionsData optionsData = null;
        String question = st.nextToken();
        String answer = st.nextToken();
        String dataStatus = st.nextToken();

        try {
            int status = Integer.parseInt(dataStatus); 
            String questionLocalizedName = null;

            switch (status) {
            case UMUserPasswordResetOptionsData.DEFAULT_ON:
            case UMUserPasswordResetOptionsData.DEFAULT_OFF:
                questionLocalizedName =
                    (String)localizedQuestions.get(question);
            }

            if ((questionLocalizedName == null) && showUserQn) {
                questionLocalizedName = question;
            }

            if (questionLocalizedName != null) {
                optionsData = new UMUserPasswordResetOptionsData(question,
                    questionLocalizedName, answer, status);
            }
        } catch (NumberFormatException e) {
            if (debug.warningEnabled()) {
                debug.warning("UMUserPasswordResetOptionsModelImpl." +
                    "getUserPasswordResetAnswers: " + dataStatus, e);
            }
        }

        return optionsData;
    }

    private Set getAttributeValues(
        String realmName,
        String attributeName,
        String serviceName
    ) {
        Set values = null;

        try {
            String[] params = {realmName, serviceName, attributeName};
            logEvent("ATTEMPT_GET_ATTR_VALUE_OF_SERVICE_UNDER_REALM",
                params);
            OrganizationConfigManager orgMgr = getOrganizationConfigManager(
                realmName);
            Map map = orgMgr.getServiceAttributes(serviceName);
            values = (Set)map.get(attributeName);
            logEvent("SUCCEED_GET_ATTR_VALUE_OF_SERVICE_UNDER_REALM", params);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {realmName, serviceName, attributeName,
                strError};
            logEvent("SMS_EXCEPTION_GET_ATTR_VALUE_OF_SERVICE_UNDER_REALM",
                paramsEx);
            //fall back to global defaults.
            values = getGlobalAttributeValues(attributeName, serviceName);
        }

        return (values == null) ? Collections.EMPTY_SET : values;
    }

    private Set getGlobalAttributeValues(
        String attributeName,
        String serviceName
    ) {
        Set values = null;

        try {
            String[] params = {serviceName, SchemaType.GLOBAL.getType(),
                attributeName};
            logEvent("ATTEMPT_GET_ATTR_VALUE_SCHEMA_TYPE", params);
            ServiceSchemaManager mgr = new ServiceSchemaManager(
                serviceName, getUserSSOToken());
            values = AMAdminUtils.getAttribute(
                mgr, SchemaType.GLOBAL, attributeName);
            logEvent("SUCCEED_GET_ATTR_VALUE_SCHEMA_TYPE", params);
        } catch (SSOException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {serviceName, SchemaType.GLOBAL.getType(),
                attributeName, strError};
            logEvent("SSO_EXCEPTION_GET_ATTR_VALUE_SCHEMA_TYPE", paramsEx);
            debug.error(
                "UMUserPasswordResetOptionsModelImpl.getGlobalAttributeValues",
                e);
        } catch (SMSException e) {
            String strError = getErrorString(e);
            String[] paramsEx = {serviceName, SchemaType.GLOBAL.getType(),
                attributeName, strError};
            logEvent("SMS_EXCEPTION_GET_ATTR_VALUE_SCHEMA_TYPE", paramsEx);
            debug.error(
                "UMUserPasswordResetOptionsModelImpl.getGlobalAttributeValues",
                e);
        }

        return (values == null) ? Collections.EMPTY_SET : values;
    }

    private OrganizationConfigManager getOrganizationConfigManager(
        String realmName) {
        if (orgCfgMgr == null) {
            try {
                orgCfgMgr = new OrganizationConfigManager(
                    adminSSOToken, realmName);
            } catch (SMSException e) {
                debug.error(
            "UMUserPasswordResetOptionsModelImpl.getOrganizationConfigManager",
                e);
            }
        }
        return orgCfgMgr;
    }

    /**
     * Returns true if <code>userId</code> is the same as the logged in user.
     *
     * @param userId Universal ID of user.
     * @return true if <code>userId</code> is the same as the logged in user.
     */
    public boolean isLoggedInUser(String userId) {
        return userId.equals(getUserName());
    }

    /**
     * Returns <code>true</code> if current user is an realm administrator.
     *
     * @return <code>true</code> if current user is an realm administrator.
     */
    public boolean isRealmAdmin() {
        SSOToken token = getUserSSOToken();
        try {
            Set actionNames = new HashSet();
            actionNames.add("MODIFY");
            DelegationEvaluator de = new DelegationEvaluator();
            DelegationPermission permission =
                    new DelegationPermission(token.getProperty(
                    Constants.ORGANIZATION), "sunAMRealmService",
                    "1.0", "organization", "default", actionNames, null);
            return de.isAllowed(token, permission, null);
        } catch (SSOException e) {
            debug.warning("UserPasswordResetOptionsModelImpl.isRealmAdmin", e);
        } catch (DelegationException e) {
            debug.warning("UserPasswordResetOptionsModelImpl.isRealmAdmin", e);
        }
        return false;
    }
}
