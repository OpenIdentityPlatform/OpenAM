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
 * $Id: AMPostViewBean.java,v 1.2 2008/06/25 05:42:47 qcheng Exp $
 *
 */

package com.sun.identity.console.base;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.ViewBeanBase;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.html.StaticTextField;

/**
 * This view bean bridges view beans from two deployment domains.
 * For example, you want to forward a register from one view bean to
 * another and both view beans are in different file.
 */
public class AMPostViewBean
    extends ViewBeanBase
{
    private static final String PAGE_NAME = "AMPost";
    private static final String DEFAULT_DISPLAY_URL =
        "/console/base/AMPost.jsp";
    private static final String FORM_ACTION = "formAction";

    private String urlViewBean;

    /**
     * Constructs a post view bean.
     */
    public AMPostViewBean() {
        super(PAGE_NAME);
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected View createChild(String name) {
        View child = null;

        if (name.equals(FORM_ACTION)) {
            child = new StaticTextField(this, name, "");
        } else {
            throw new IllegalArgumentException(
                "Invalid child name [" + name + "]");
        }
        return child;
    }

    /**
     * Set value for form action.
     *
     * @param event display event.
     * @throws ModelControlException if default model cannot be created.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        setDisplayFieldValue(FORM_ACTION, urlViewBean);
    }

    /**
     * Set URL of target view bean.
     *
     * @param url URL of target view bean.
     */
    public void setTargetViewBeanURL(String url) {
        urlViewBean = url;
    }
}
