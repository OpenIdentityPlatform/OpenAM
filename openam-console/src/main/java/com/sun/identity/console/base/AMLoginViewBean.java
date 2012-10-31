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
 * $Id: AMLoginViewBean.java,v 1.2 2008/06/25 05:42:47 qcheng Exp $
 *
 */

package com.sun.identity.console.base;

import com.iplanet.jato.view.View;
import com.iplanet.jato.view.ViewBeanBase;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.html.StaticTextField;

/**
 * This view bean takes a login URL and make sure that login page occupies the
 * entire browser.
 */
public class AMLoginViewBean
    extends ViewBeanBase
{
    /** page name of view bean */
    public static final String PAGE_NAME = "AMLogin";

    /**
     * Default display URL of this view bean
     */
    public static final String DEFAULT_DISPLAY_URL 
        = "/console/base/AMLogin.jsp";

    private String loginURL;

    /**
     * Redirect URL text
     */
    public static final String REDIRECT_URL = "redirectURL";

    /**
     * Constructs a login view bean
     */
    public AMLoginViewBean() {
        super(PAGE_NAME);
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    /** 
     * Registers child components/views
     */
    protected void registerChildren() {
        registerChild(REDIRECT_URL, StaticTextField.class);
    }

    /**
     * Creates user interface components used by this view bean.
     *
     * @param name of component
     * @return child component
     */
    protected View createChild(String name) {
        View child = null;
        if (name.equals(REDIRECT_URL)) {
            child = new StaticTextField(this, REDIRECT_URL, "");
        } else {
            throw new IllegalArgumentException(
                "Invalid child name [" + name + "]");
        }
        return child;
    }

    /**
     * Begins displaying page.
     *
     * @param event   display event
     */
    public void beginDisplay(DisplayEvent event) {
        setDisplayFieldValue(REDIRECT_URL, loginURL);
    }

    /**
     * Set login URL. This URL needs to be set in order for this view bean
     * to work.
     *
     * @param URL to redirect
     */
    void setLoginURL(String URL) {
        loginURL = URL;
    }
}
