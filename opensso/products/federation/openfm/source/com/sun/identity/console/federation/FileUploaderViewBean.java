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
 * $Id: FileUploaderViewBean.java,v 1.2 2008/06/25 05:49:36 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.view.View;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.masthead.CCSecondaryMasthead;
import javax.servlet.http.HttpServletRequest;

public class FileUploaderViewBean
    extends AMViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/federation/FileUploader.jsp";
    
    private CCPageTitleModel ptModel;
    
    /**
     * Creates a view bean.
     */
    public FileUploaderViewBean() {
        super("FileUploader");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);                   
        createPageTitleModel();
        registerChildren();    
    }

    protected void registerChildren() {       
        ptModel.registerChildren(this);
        registerChild("secondaryMasthead", CCSecondaryMasthead.class);
        super.registerChildren();
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals("pgtitle")) {
            view = new CCPageTitle(this, ptModel, name);
        } else if ((ptModel != null) && ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals("secondaryMasthead")) {
            view = new CCSecondaryMasthead(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }
     
    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/fileUploaderPageTitle.xml"));
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new AMModelBase(req, getPageSessionAttributes());
    }
} 
