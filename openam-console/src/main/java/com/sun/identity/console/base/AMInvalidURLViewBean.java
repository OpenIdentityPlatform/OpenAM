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
 * $Id: AMInvalidURLViewBean.java,v 1.2 2008/06/25 05:42:47 qcheng Exp $
 *
 */

package com.sun.identity.console.base;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import javax.servlet.http.HttpServletRequest;

/**
 * <code>AMInvalidURLViewBean</code> is invoked when Module Servlet gets an
 * invalid URL.
 */
public class AMInvalidURLViewBean
    extends AMViewBeanBase
{
    public static final String PAGE_NAME = "AMInvalidURL";
    public static final String DEFAULT_DISPLAY_URL
        = "/console/base/AMInvalidURL.jsp";

    /**
     * Creates Invalid URL view bean
     */
    public AMInvalidURLViewBean() {
        super(PAGE_NAME);
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    /**
     * Begins displaying page. Setup message box.
     *
     * @param event Display event.
     * @throws ModelControlException if problem access value of component.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
            "invalid.url.message");
    }

    /**
     * Returns model for this view bean.
     *
     * @return model for this view bean.
     */
    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new AMModelBase(req, getPageSessionAttributes());
    }
}
