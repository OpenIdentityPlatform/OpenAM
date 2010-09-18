/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: CompleteCreateHostedIDPViewBean.java,v 1.2 2008/06/25 05:49:47 qcheng Exp $
 *
 */

package com.sun.identity.console.task;

import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import javax.servlet.http.HttpServletRequest;


/**
 * Create register product UI.
 */
public class CompleteCreateHostedIDPViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/task/CompleteCreateHostedIDP.jsp";

    public CompleteCreateHostedIDPViewBean() {
        super("CompleteCreateHostedIDP");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        registerChildren();
    }
    
    public void beginDisplay(DisplayEvent e) {
        HttpServletRequest req = this.getRequestContext().getRequest();
        setDisplayFieldValue("tfcot", req.getParameter("cot"));
        setDisplayFieldValue("tfrealm", req.getParameter("realm"));
        setDisplayFieldValue("tfentityId", req.getParameter("entityId"));
        
    }
    
    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new AMModelBase(req, getPageSessionAttributes());   
    }
}
