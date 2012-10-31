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
 * $Id: SCPlatform30ViewBean.java,v 1.2 2008/06/25 05:43:15 qcheng Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.service.model.SCPlatformModel;
import com.sun.identity.console.service.model.SCPlatformModelImpl;
import com.sun.web.ui.view.alert.CCAlert;
import java.text.MessageFormat;
import javax.servlet.http.HttpServletRequest;

/**
 * View bean for platform server revision 30.
 */
public class SCPlatform30ViewBean
    extends AMServiceProfileViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/SCPlatform30.jsp";
    public static final String PAGE_MODIFIED = "pageModified";

    /**
     * Creates a platform service profile view bean.
     */
    public SCPlatform30ViewBean() {
        super("SCPlatform30", DEFAULT_DISPLAY_URL,
            AMAdminConstants.PLATFORM_SERVICE);
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        try {
            return new SCPlatformModelImpl(
                req, serviceName, getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        return null;
    }

    /**
     * Populates client character set table.
     *
     * @param event Display Event.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);

        if (!isInlineAlertMessageSet()) {
            String flag = (String)getPageSessionAttribute(PAGE_MODIFIED);
            if ((flag != null) && flag.equals("1")) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "message.profile.modified");
            }
        }
    }

    /**
     * Handles save request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        removePageSessionAttribute(PAGE_MODIFIED);
        super.handleButton1Request(event);
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        removePageSessionAttribute(PAGE_MODIFIED);
        super.handleButton2Request(event);
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        backTrail();
        SCConfigSystemViewBean vb = (SCConfigSystemViewBean)
            getViewBean(SCConfigSystemViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    protected String getBreadCrumbDisplayName() {
        SCPlatformModel model = (SCPlatformModel)getModel();
        Object[] arg = {
            model.getLocalizedServiceName(AMAdminConstants.PLATFORM_SERVICE)};
        return MessageFormat.format(model.getLocalizedString(
            "breadcrumbs.services.edit"), arg);
    }

    protected boolean startPageTrail() {
        return false;
    }
}
