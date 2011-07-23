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
 * $Id: PWResetUncaughtExceptionViewBean.java,v 1.2 2008/06/25 05:43:42 qcheng Exp $
 *
 */

package com.sun.identity.password.ui;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.password.ui.model.PWResetModel;
import com.sun.identity.password.ui.model.PWResetUncaughtExceptionModel;
import com.sun.identity.password.ui.model.PWResetUncaughtExceptionModelImpl;

/**
 * <code>PWResetInvalidURLViewBean</code> is invoked when Module Servlet gets an
 * invalid URL.
 */
public class PWResetUncaughtExceptionViewBean extends PWResetViewBeanBase  {

    /** 
     * Page Name  
     */
    public static final String PAGE_NAME = "PWResetUncaughtException";   

    /** 
     * Default display URL   
     */
    public static final String DEFAULT_DISPLAY_URL =
        "/password/ui/PWResetUncaughtException.jsp";


    /**
     * Constructs a user validation view bean
     */
    public PWResetUncaughtExceptionViewBean() {
        super(PAGE_NAME);
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        registerChildren();
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
        super.beginDisplay(event);
        PWResetUncaughtExceptionModel model = 
            (PWResetUncaughtExceptionModel)getModel();
        setErrorMessage(model.getErrorTitle(), model.getErrorMessage());
    }

    /**
     * Gets model for this view bean.
     *
     * @return model for this view bean.
     */
    protected PWResetModel getModel() {
        if (model == null) {
            model = new PWResetUncaughtExceptionModelImpl();
        }
        return model;
    }
}
