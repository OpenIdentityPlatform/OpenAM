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
 * $Id: PWResetViewBeanBase.java,v 1.2 2008/06/25 05:43:42 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.password.ui;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.ViewBeanBase;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.html.StaticTextField;
import com.sun.identity.shared.Constants;
import com.sun.identity.password.ui.model.PWResetModel;
import com.sun.identity.password.ui.model.PWResetModelImpl;


/**
 * <code>PWResetViewBeanBase</code> is the base class for password reset.
 */
public abstract class PWResetViewBeanBase extends ViewBeanBase 
             implements Constants {

    /**
     * Name of HTML title
     */
    public static final String TITLE_HTML_PAGE = "titleHtmlPage";

    /**
     * Name of the copyright text
     */
    public static final String COPYRIGHT_TEXT = "copyrightText";

    /**
     * Name of the error title 
     */
    public static final String ERROR_TITLE = "errorTitle";

    /**
     * Name of the error message 
     */
    public static final String ERROR_MSG = "errorMsg";

    /**
     * Name of the information message 
     */
    public static final String INFO_MSG = "infoMsg";

    /**
     * Name of organization DN
     */
    public static final String ORG_DN = "orgDN";

    /**
     * Name of organization DN Flag
     */
    public static final String ORG_DN_FLAG = "orgDNFlag";

    /**
     * String constant for true
     */
    public static final String STRING_TRUE = "true";

    /**
     * Name of user DN
     */
    public static final String USER_DN = "userDN";

    /**
     * Name of User value
     */
    public static final String USER_ATTR_VALUE = "userValueAttr";

    /**
     * Name of organization DN that is entered in the URL or the one where
     * to look for initial template.
     */
    public static final String INITIAL_ORG_DN = "initialOrgDN";

    /**
     * Name of the model used in the view bean
     */
    protected PWResetModel model = null;

    /**
     * Name for Sun Logo label.
     */
    public static final String LBL_SUN_LOGO = "lblSunLogo";

    /**
     * Name for product label.
     */
    public static final String LBL_PRODUCT = "lblProduct";

    /**
     * Name for Java Logo label.
     */
    public static final String LBL_JAVA_LOGO = "lblJavaLogo";

    protected static final String URL_LOCALE = "locale";

    /**
     * Constructs a password reset base view bean
     *
     * @param pageName  name of page
     */
    public PWResetViewBeanBase(String pageName) {
        super(pageName);
    }

    /**
     * Registers child components/views
     */
    protected void registerChildren() {
        registerChild(TITLE_HTML_PAGE, StaticTextField.class);
        registerChild(COPYRIGHT_TEXT, StaticTextField.class);
        registerChild(ERROR_TITLE, StaticTextField.class);
        registerChild(ERROR_MSG, StaticTextField.class);
        registerChild(INFO_MSG, StaticTextField.class);
	registerChild(LBL_SUN_LOGO, StaticTextField.class);
	registerChild(LBL_PRODUCT, StaticTextField.class);
	registerChild(LBL_JAVA_LOGO, StaticTextField.class);
    }

    /**
     * Creates child component
     *
     * @param name of child component
     * @return child component
     */
    protected View createChild(String name) {
        View child = null;
        if (name.equals(TITLE_HTML_PAGE)) {
            child = new StaticTextField(this, TITLE_HTML_PAGE, "");
        } else if (name.equals(COPYRIGHT_TEXT)) {
            child = new StaticTextField(this, COPYRIGHT_TEXT, "");
        } else if (name.equals(ERROR_TITLE)) {
            child = new StaticTextField(this, ERROR_TITLE, "");
        } else if (name.equals(ERROR_MSG)) {
            child = new StaticTextField(this, ERROR_MSG, "");
        } else if (name.equals(INFO_MSG)) {
            child = new StaticTextField(this, INFO_MSG, "");
        } else if (name.equals(LBL_SUN_LOGO)) {
	    child = new StaticTextField(this, LBL_SUN_LOGO, "");
        } else if (name.equals(LBL_PRODUCT)) {
	    child = new StaticTextField(this, LBL_PRODUCT, "");
        } else if (name.equals(LBL_JAVA_LOGO)) {
	    child = new StaticTextField(this, LBL_JAVA_LOGO, "");
	} else {
            throw new IllegalArgumentException("invalid child name " + name);
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
        throws ModelControlException
    {
        model = getModel();
        setDisplayFieldValue(TITLE_HTML_PAGE, model.getHTMLPageTitle());
        setDisplayFieldValue(COPYRIGHT_TEXT, model.getCopyRightText());
        setDisplayFieldValue(LBL_SUN_LOGO, model.getSunLogoLabel());
        setDisplayFieldValue(LBL_PRODUCT, model.getProductLabel());
        setDisplayFieldValue(LBL_JAVA_LOGO, model.getJavaLogoLabel());
    }

    /**
     * Gets model for this view bean
     *
     * @return model for this view bean
     */
    protected PWResetModel getModel() {
        if (model == null) {
            model = new PWResetModelImpl();
        }
        return model;
    }

    /**
     * Begins error message content
     *
     * @param event  child display event
     * @return true if the error message needs to be displayed, false otherwise
     */
    public boolean beginErrorBlockDisplay(ChildDisplayEvent event) {
        if (model.isError()) {
            setDisplayFieldValue(ERROR_TITLE, model.getErrorTitle());
            setDisplayFieldValue(ERROR_MSG, model.getErrorMessage());
            return true;
        }
        return false;
    }

    /**
     * Begins info message content
     *
     * @param event  child display event
     * @return true if the info message needs to be displayed, false otherwise
     */
    public boolean beginInfoBlockDisplay(ChildDisplayEvent event) {
        String msg = model.getInformationMessage();
        if (msg != null && msg.length() > 0) {
            setDisplayFieldValue(INFO_MSG, msg);
            return true;
        }
        return false;
    }


    /**
     * Begins password reset user validation content
     *
     * @param event  child display event
     * @return true if password reset user validation content is to
     *         displayed, false otherwise
     */
    public boolean beginResetPageDisplay(ChildDisplayEvent event) {
        return isPWResetEnabled();
    }



    /**
     * Determines if the password reset service is enabled or not
     *
     * @return true if the password reset service is enabled, false otherwise
     */
    protected boolean isPWResetEnabled() {
        return model.isPasswordResetEnabled();
    }

    protected void setErrorMessage(String title, String message) {
        setDisplayFieldValue(ERROR_TITLE, title);
        setDisplayFieldValue(ERROR_MSG, message);
    }
}
