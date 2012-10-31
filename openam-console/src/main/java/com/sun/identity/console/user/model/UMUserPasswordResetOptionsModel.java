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
 * $Id: UMUserPasswordResetOptionsModel.java,v 1.3 2008/09/22 20:17:37 veiming Exp $
 *
 */

package com.sun.identity.console.user.model;

import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;
import java.util.List;

/* - NEED NOT LOG - */

public interface UMUserPasswordResetOptionsModel
    extends AMModel
{
    /**
     * Name of password reset question attribute
     */
    String PW_RESET_QUESTION = "iplanet-am-password-reset-question";

    /**
     * Name of user question answer attribute.
     */
    String PW_RESET_QUESTION_ANSWER =
        "iplanet-am-user-password-reset-question-answer";

    /**
     * Name of password reset personal question attribute
     */
    String PW_RESET_PERSONAL_ANSWER =
        "iplanet-am-password-reset-user-personal-question";

    /**
     * Name of user force reset attribute
     */
    String PW_RESET_FORCE_RESET = "iplanet-am-user-password-reset-force-reset";

    /**
     * Name of password reset max number of questions
     */
    String PW_RESET_MAX_NUM_OF_QUESTIONS =
        "iplanet-am-password-reset-max-num-of-questions";

    /**
     * Delimiter for password option data.
     */
    String DELIMITER = "\t";

    /**
     * Returns true if user personal question/answer feature is enabled.
     *
     * @param realmName Name of realm.
     * @return true if user personal question/answer feature is enabled.
     */
    boolean isUserQuestionEnabled(String realmName);

    /**
     * Modifies user's password reset option.
     *
     * @param questionAnswers List of
     *        <code>UMUserPasswordResetOptionsData</code> objects.
     * @param userId Universal ID of user.
     * @param forceReset true to force reset.
     * @throws AMConsoleException if password reset option cannot be modified.
     */
    void modifyUserOption(
        List questionAnswers,
        String userId,
        boolean forceReset
    ) throws AMConsoleException;

    /**
     * Returns user's answers for password reset questions.
     *
     * @param userId Universal ID of user.
     * @return user's answers for password reset questions.
     * @throws AMConsoleException if answers cannot be retrieved.
     */
    List getUserAnswers(String userId)
        throws AMConsoleException;

    /**
     * Returns true if force reset is set.
     *
     * @param userId Universal ID of user.
     * @return true if force reset is set.
     */
    boolean isForceReset(String userId);

    /**
     * Returns the maximum number of question that can be display in
     * the reset password page.
     *
     * @return maximum number of question which can be in reset password page
     */
    int getMaxNumQuestions(String realmName);

    /**
     * Returns true if <code>userId</code> is the same as the logged in user.
     *
     * @param userId Universal ID of user.
     * @return true if <code>userId</code> is the same as the logged in user.
     */
    boolean isLoggedInUser(String userId);

    /**
     * Returns <code>true</code> if current user is an realm administrator.
     *
     * @return <code>true</code> if current user is an realm administrator.
     */
    boolean isRealmAdmin();
}
