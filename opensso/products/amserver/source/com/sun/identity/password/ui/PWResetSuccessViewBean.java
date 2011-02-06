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
 * $Id: PWResetSuccessViewBean.java,v 1.3 2009/12/18 03:29:24 222713 Exp $
 *
 */

package com.sun.identity.password.ui;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.html.StaticTextField;
import com.sun.identity.common.ISLocaleContext;
import com.sun.identity.password.ui.model.PWResetModel;
import com.sun.identity.password.ui.model.PWResetSuccessModel;
import com.sun.identity.password.ui.model.PWResetSuccessModelImpl;

/**
 * <code>PWResetSuccessViewBean</code> displays reset 
 * password success message.
 */
public class PWResetSuccessViewBean extends PWResetViewBeanBase  {

    /**
     * Name of title peer component
     */
    public static final String CC_TITLE = "ccTitle";

    /**
     * Page name
     */
    public static final String PAGE_NAME = "PWResetSuccess";   

    /**
     * Name of reset message stored in this view bean
     */
    public static final String RESET_MESSAGE = "resetMsg";

    /**
     * Default display URL
     */
    public static final String DEFAULT_DISPLAY_URL =
        "/password/ui/PWResetSuccess.jsp";

    private String resetMsg = null;


    /**
     * Constructs a reset password success view bean
     */
    public PWResetSuccessViewBean() {
        super(PAGE_NAME);
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        registerChildren();
    }

    /**
     * Registers child components/views
     */
    protected void registerChildren() {
        super.registerChildren();
        registerChild(CC_TITLE, StaticTextField.class);
        registerChild(RESET_MESSAGE, StaticTextField.class);
    }

    /**
     * Creates child component
     *
     * @param name of child component
     * @return child component
     */
    protected View createChild(String name) {
        View child = null;
        if (name.equals(CC_TITLE)) {
            child = new StaticTextField(this, CC_TITLE, "");
        } else if (name.equals(RESET_MESSAGE)) {
            child = new StaticTextField(this, RESET_MESSAGE, "");
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
        PWResetSuccessModel model = (PWResetSuccessModel)getModel();

        setDisplayFieldValue(CC_TITLE, model.getTitleString());
        setDisplayFieldValue(RESET_MESSAGE, resetMsg);
    }

    /**
     * Forwards to current view bean after after verifying 
     * that reset reset message exists. It will forward to 
     * <code>PWResetUserValidationViewBean</code> if the user DN 
     * or organization DN does not exists.
     *
     * @param context  request context
     */
    public void forwardTo(RequestContext context) {
        PWResetSuccessModel model = (PWResetSuccessModel)getModel();
        ISLocaleContext localeContext = new ISLocaleContext();
        localeContext.setLocale(context.getRequest());
        java.util.Locale locale = localeContext.getLocale();
        model.setUserLocale(locale.toString());

        if (resetMsg == null || resetMsg.length() == 0) {
            PWResetUserValidationViewBean vb = (PWResetUserValidationViewBean)
                getViewBean(PWResetUserValidationViewBean.class);
            vb.forwardTo(context);
        } else {
           super.forwardTo(context);
        }
    }

    /**
     * Gets model for this view bean
     *
     * @return model for this view bean
     */
    protected PWResetModel getModel() {
        if (model == null) {
            model = new PWResetSuccessModelImpl();
        }
        return model;
    }

    /**
     * Sets password reset success message
     *
     * @param msg password reset message
     */
    public void setResetMessage(String msg) {
        resetMsg = msg;
    }

}
