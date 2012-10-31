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
 * $Id: AMAdminFrameViewBean.java,v 1.3 2008/11/20 22:22:08 babysunil Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.base;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import javax.servlet.http.HttpServletRequest;

public class AMAdminFrameViewBean
    extends AMViewBeanBase
{
    public static final String PAGE_NAME = "AMAdminFrame";
    public static final String DEFAULT_DISPLAY_URL 
        = "/console/base/AMAdminFrame.jsp";
    private static final String VIEWBEAN_URL = "txtViewbeanURL";

    /** 
     * Creates an Administrator Frame view bean
     */
    public AMAdminFrameViewBean() {
        super(PAGE_NAME);
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        registerChildren();
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        AMViewConfig config = AMViewConfig.getInstance();
        RequestContext rc = getRequestContext();
        String startDN = AMModelBase.getStartDN(rc.getRequest());
        String url = config.getDefaultViewBeanURL(startDN, rc.getRequest());

        if ((url != null) && (url.length() > 0)) {
            setDisplayFieldValue(VIEWBEAN_URL, url);
        } else {
            /* 
             * We couldn't retrieve the default view to display.
             * This may be due to permission problems, or a configuration
             * issue. If the users profile is set to ignore, display
             * the "Authenticated View". 
             */
            AMModel model = getModel();
            if (model.ignoreUserProfile()) {
                AuthenticatedViewBean vb = (AuthenticatedViewBean)
                    getViewBean(AuthenticatedViewBean.class);
                passPgSessionMap(vb);
                vb.forwardTo(rc);
            }

            //set the non admin end user view
            setDisplayFieldValue(VIEWBEAN_URL, "../idm/EndUser");
        }
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
