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
 * $Id: UMUserPasswordResetOptionsData.java,v 1.2 2008/06/25 05:43:24 qcheng Exp $
 *
 */

package com.sun.identity.console.user.model;

import com.sun.identity.console.base.model.AMConsoleException;
import java.io.Serializable;

/* - NEED NOT LOG - */

/**
 * <code>UMUserPasswordResetOptionsData</code> defines a set of methods that
 * are required by user password reset option viewbean.
 */
public class UMUserPasswordResetOptionsData 
    implements Serializable
{
    /**
     * Name of default question unselected constant
     */
    public final static int DEFAULT_OFF = 0;

    /**
     * Name of default question selected constant
     */
    public final static int DEFAULT_ON = 1;

    /**
     * Name of personal question unselected constant
     */
    public final static int PERSONAL_OFF = 2;

    /**
     * Name of personal question selected constant
     */
    public final static int PERSONAL_ON = 3;

    private String question = null;
    private String questionLocalizedName = null;
    private String answer = null;
    private int dataStatus = DEFAULT_OFF;

    /**
     * Constructs a user password reset options data object
     *
     * @param answer user answer to the secret question 
     * @param dataStatus status of question selected or not
     */
    public UMUserPasswordResetOptionsData(
        String question,
        String questionLocalizedName,
        String answer,
        int dataStatus
    ) {
        this.question = question.trim();
        this.questionLocalizedName = questionLocalizedName;
        this.answer = answer.trim();
        this.dataStatus = dataStatus;
    }

    /**
     * Returns the answer to the users secret question.
     *
     * @return the answer stored in this object
     */
    public String getAnswer() {
        return answer;
    }

    /**
     * Set the answer to the users secret question.
     *
     * @param answer Answer to user question.
     */
    public void setAnswer(String answer) {
        this.answer = answer.trim();
    }

    /**
     * Returns the data status stored in this object
     *
     * @return the data status stored in this object
     */
    public int getDataStatus() {
        return dataStatus;
    }

    /**
     * Returns question.
     *
     * @return question.
     */
    public String getQuestion() {
        return question;
    }

    /**
     * Set question.
     *
     * @param question The password reset question
     */
    public void setQuestion(String question) {
        if (isPersonalQuestion()) {
            this.question = question.trim();
            this.questionLocalizedName = question.trim();
        }
    }

    /**
     * Returns question localized name.
     *
     * @return question localized name.
     */
    public String getQuestionLocalizedName() {
        return questionLocalizedName;
    }

    /**
     * Returns true if the user's secret question stored in this object is
     * selected.
     *
     * @return true if the question is selected.
     */
    public boolean isSelected() {
        return (dataStatus == DEFAULT_ON || dataStatus == PERSONAL_ON);
    }

    /**
     * Set status of user;s secret question selection.
     *
     * @param selected true if the question is selected.
     */
    public void setSelected(boolean selected) {
        if (selected) {
            dataStatus = isPersonalQuestion() ? PERSONAL_ON : DEFAULT_ON;
        } else {
            dataStatus = isPersonalQuestion() ? PERSONAL_OFF : DEFAULT_OFF;
        }
    }

    /**
     * Returns true if the question is personal
     *
     * @return true if question is personal, false otherwise
     */
    public boolean isPersonalQuestion() {
        return (dataStatus == PERSONAL_ON || dataStatus == PERSONAL_OFF);
    }

    /**
     * Validates data for correctness. e.g. selected question need to have an
     * answer.
     */
    public void validate()
        throws AMConsoleException {
        if (isSelected()) {
            if (isPersonalQuestion()) {
                if ((question.length() == 0) || (answer.length() == 0)) {
                    throw new AMConsoleException(
                "user.password.reset.missing.personal.question.answer.message");
                }
            } else {
                if (answer.length() == 0) {
                    throw new AMConsoleException(
                        "user.password.reset.missing.answer.message");
                }
            }
        }
    }

    public String toString() {
        return "Question=\"" + question + "\"\n" +
            "Question Localized Name=\"" + questionLocalizedName + "\"\n" +
            "Answer=\"" + answer + "\"\n" +
            "Selected Status=\"" + dataStatus + "\"\n";
    }
}
