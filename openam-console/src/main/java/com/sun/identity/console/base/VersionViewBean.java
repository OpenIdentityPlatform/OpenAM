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
 * $Id: VersionViewBean.java,v 1.2 2008/06/25 05:42:48 qcheng Exp $
 *
 */


package com.sun.identity.console.base;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMSystemConfig;
import javax.servlet.http.HttpServletRequest;

/**
 * This class displays the version information page. The version information
 * includes the product version, the date the product was built (not installed),
 * and copyright information.
 */
public class VersionViewBean
    extends AMViewBeanBase
{
    private static final String PAGE_NAME = "Version";
    private static final String DEFAULT_DISPLAY_URL 
        = "/console/base/Version.jsp";

    /**
     * Constructs a login view bean
     */
    public VersionViewBean() {
        super(PAGE_NAME);
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    public void forwardTo(RequestContext rc) {
        super.bypassForwardTo(rc);
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        setDisplayFieldValue("txtVersion", AMSystemConfig.version);
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new AMModelBase(req, getPageSessionAttributes());
    }

}
