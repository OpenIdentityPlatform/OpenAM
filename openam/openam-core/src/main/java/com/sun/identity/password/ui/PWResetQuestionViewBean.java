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
 * $Id: PWResetQuestionViewBean.java,v 1.3 2009/12/18 03:30:13 222713 Exp $
 *
 */

package com.sun.identity.password.ui;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.Button;
import com.iplanet.jato.view.html.StaticTextField;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.password.ui.model.PWResetException;
import com.sun.identity.password.ui.model.PWResetModel;
import com.sun.identity.password.ui.model.PWResetQuestionModel;
import com.sun.identity.password.ui.model.PWResetQuestionModelImpl;
import java.util.Map;

/**
 * <code>PWResetUserValidationViewBean</code> verifies user's
 * answer to the secret question for the password reset service
 */
public class PWResetQuestionViewBean extends PWResetViewBeanBase  {

    /**
     * Name of title peer component
     */
    public static final String PW_QUESTION_TITLE = "pwQuestionTitle";

    /**
     * Name of OK button peer component
     */
    public static final String BUTTON_OK = "btnOK";

    /**
     * Name of previous button peer component
     */
    public static final String BUTTON_PREVIOUS = "btnPrevious";

    /**
     * Page Name
     */
    public static final String PAGE_NAME = "PWResetQuestion";   

    /** 
     * Name of tiled view component
     */
    public static final String PASSWORD_RESET_TILEDVIEW = "passResetTileView";

    /**
     * Name of user DN field
     */
    public static final String FLD_USER_ATTR = "fldUserAttr";


    /**
     * Default display URL
     */
    public static final String DEFAULT_DISPLAY_URL =
        "/password/ui/PWResetQuestion.jsp";


    /**
     * Constructs a password reset question view bean
     */
    public PWResetQuestionViewBean() {
        super(PAGE_NAME);
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        registerChildren();
    }

    /**
     * Registers child components/views
     */
    protected void registerChildren() {
        super.registerChildren();
        registerChild(PW_QUESTION_TITLE, StaticTextField.class);
        registerChild(BUTTON_OK, Button.class);
        registerChild(BUTTON_PREVIOUS, Button.class);
        registerChild(PASSWORD_RESET_TILEDVIEW, PWResetQuestionTiledView.class);
    }

    /**
     * Creates child component
     *
     * @param name of child component
     * @return child component
     */
    protected View createChild(String name) {
        View child = null;
        if (name.equals(BUTTON_OK)) {
            child = new Button(this, BUTTON_OK, "");
        } else if (name.equals(BUTTON_PREVIOUS)) {
            child = new Button(this, BUTTON_PREVIOUS, "");
        } else if (name.equals(PW_QUESTION_TITLE)) {
            child = new StaticTextField(this, PW_QUESTION_TITLE, "");
        } else if (name.equals(PASSWORD_RESET_TILEDVIEW)) {
            child = new PWResetQuestionTiledView(
                this, PASSWORD_RESET_TILEDVIEW);
        } else {
            child = super.createChild(name);
        }
        return child;
    }


    /**
     * Set the required information to display the page.
     *
     * @param event display event.
     * @throws ModelControlException if problem access value of component.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {

        super.beginDisplay(event);

        PWResetQuestionModel model = (PWResetQuestionModel)getModel();
        String orgDN = (String)getPageSessionAttribute(ORG_DN);
        String userDN = (String)getPageSessionAttribute(USER_DN);
        PWResetQuestionTiledView tView =
            (PWResetQuestionTiledView) getChild(PASSWORD_RESET_TILEDVIEW);
        tView.populateQuestionsList(userDN, orgDN);

        String value = (String)getPageSessionAttribute(USER_ATTR_VALUE);
        setDisplayFieldValue(PW_QUESTION_TITLE, 
            model.getPWQuestionTitleString(value));

        setDisplayFieldValue(BUTTON_OK, model.getOKBtnLabel());
        setDisplayFieldValue(BUTTON_PREVIOUS, model.getPreviousBtnLabel());
    }


    /**
     * Forwards to current view bean after populating questions. It
     * will forward to <code>PWResetUserValidationViewBean</code> if 
     * the user DN or organization does not exists.
     *
     * @param context request context
     */
    public void forwardTo(RequestContext context) {
        String orgDN = (String)getPageSessionAttribute(ORG_DN);
        String userDN = (String)getPageSessionAttribute(USER_DN);
        ISLocaleContext localeContext = new ISLocaleContext();
        localeContext.setLocale(context.getRequest());
        java.util.Locale locale = localeContext.getLocale();

        if (orgDN == null || orgDN.length() == 0 ||
            userDN == null || userDN.length() == 0) 
        {
            PWResetUserValidationViewBean vb = (PWResetUserValidationViewBean)
                getViewBean(PWResetUserValidationViewBean.class);
            vb.forwardTo(context);
        } else {
            PWResetQuestionModel model = (PWResetQuestionModel)getModel();
            model.readPWResetProfile(orgDN);
            model.setUserLocale(locale.toString());
	    populateQuestionsList(userDN, orgDN);
	    super.forwardTo(context);
        }
    }

    /**
     * Handles form submission request for next button. It will
     * forward to <code>PWResetSuccessViewBean</code> if the answers
     * are correct for the questions.
     *
     * @param event request invocation event
     */
    public void handleBtnOKRequest(RequestInvocationEvent event) {
        PWResetQuestionModel model = (PWResetQuestionModel)getModel();

        String orgDN = (String)getPageSessionAttribute(ORG_DN);
        String userDN = (String)getPageSessionAttribute(USER_DN);

        RequestContext reqContext = event.getRequestContext();
        ISLocaleContext localeContext = new ISLocaleContext();
        localeContext.setLocale(reqContext.getRequest());
        java.util.Locale localeObj = localeContext.getLocale();
        String locale = localeObj.toString();
        model.setUserLocale(locale);

        PWResetQuestionTiledView tView =
            (PWResetQuestionTiledView) getChild(PASSWORD_RESET_TILEDVIEW);
        Map map = tView.getAnswers();

        if (tView.isAnswerBlank()) {
            setErrorMessage(model.getErrorTitle(),
                model.getMissingAnswerMessage());
            forwardTo();
        } else {
            try {
                model.resetPassword(userDN, orgDN, map);
                PWResetSuccessViewBean vb = (PWResetSuccessViewBean)
                    getViewBean(PWResetSuccessViewBean.class);
                vb.setResetMessage(model.getPasswordResetMessage());
		vb.setPageSessionAttribute(URL_LOCALE, locale);
                vb.forwardTo(getRequestContext());
            } catch (PWResetException pwe) {
                if (!model.isUserLockout(userDN, orgDN)) {
                    setErrorMessage(model.getErrorTitle(), pwe.getMessage());
                }
                forwardTo();
            }
        }
    }

    /**
     * Handles form submission request for previous button. It will
     * take the user  to <code>PWResetUserValidationViewBean</code>
     *
     * @param event request invocation event
     */
    public void handleBtnPreviousRequest(RequestInvocationEvent event) {
        String orgDN = (String)getPageSessionAttribute(ORG_DN);
        String value = (String)getPageSessionAttribute(USER_ATTR_VALUE);
	String locale = (String)getPageSessionAttribute(URL_LOCALE);
        String initialOrgDN = (String)getPageSessionAttribute(INITIAL_ORG_DN);

        PWResetUserValidationViewBean vb = (PWResetUserValidationViewBean)
            getViewBean(PWResetUserValidationViewBean.class);
        vb.setPageSessionAttribute(ORG_DN, initialOrgDN);
        vb.setPageSessionAttribute(USER_ATTR_VALUE, value);
	vb.setPageSessionAttribute(URL_LOCALE,locale);

        String orgDNFlag = (String)getPageSessionAttribute(ORG_DN_FLAG);
        if (orgDNFlag != null && orgDNFlag.equals (STRING_TRUE)) {
            vb.setPageSessionAttribute(ORG_DN_FLAG, STRING_TRUE);
        }
        vb.forwardTo(getRequestContext());
    }

    /**
     * Gets model for this view bean
     *
     * @return model for this view bean
     */
    protected PWResetModel getModel() {
        if (model == null) {
            model = new PWResetQuestionModelImpl();
        }
        return model;
    }

    private void populateQuestionsList(String userDN, String orgDN) {
        PWResetQuestionModel model = (PWResetQuestionModel)getModel();

        Map map = model.getSecretQuestions(userDN, orgDN);
        if (map == null || map.isEmpty()) {
            model.setNoQuestionsInfoMsg();
        }
    }

    /**
     * Begins password secret questions content
     *
     * @param event child display event
     * @return true if password reset secret questions question is to
     *         displayed, false otherwise
     */
    public boolean beginResetPageDisplay(ChildDisplayEvent event) {
        PWResetQuestionModel model = (PWResetQuestionModel)getModel();

        String orgDN = (String)getPageSessionAttribute(ORG_DN);
        String userDN = (String)getPageSessionAttribute(USER_DN);
        return isPWResetEnabled() && model.isQuestionAvailable(userDN, orgDN) &&
            !model.isUserLockout(userDN, orgDN);
    }
}
