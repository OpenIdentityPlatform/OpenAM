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
 * $Id: FileChooserViewBean.java,v 1.2 2008/06/25 05:49:36 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.masthead.CCSecondaryMasthead;
import com.sun.web.ui.view.filechooser.CCFileChooser;
import com.sun.web.ui.model.CCFileChooserModel;
import java.io.File;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;

import com.sun.web.ui.view.alert.CCAlert;

public class FileChooserViewBean
    extends AMViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/federation/FileChooser.jsp";
    
    private CCPageTitleModel ptModel;
    private CCFileChooserModel fcModel;
    private CCFileChooserModel extFileModel;
    private static final String STANDARD_FILE = "chooseFile";
    
    /**
     * Creates a authentication domains view bean.
     */
    public FileChooserViewBean() {
        super("FileChooser");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);                   
        createPageTitleModel();
        createFileModel();
        registerChildren();    
    }

    protected void registerChildren() {       
        ptModel.registerChildren(this);
        registerChild("secondaryMasthead", CCSecondaryMasthead.class);
        registerChild(STANDARD_FILE, CCFileChooser.class);       
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
        } else if (name.equals(STANDARD_FILE)) {
            view = new CCFileChooser(this, fcModel, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }
     
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
    }    

     private void createFileModel() {
         fcModel = new CCFileChooserModel();
         fcModel.setFileListBoxHeight(10);
         fcModel.setHomeDirectory(File.separator);   
     }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/fileChooserPageTitle.xml"));
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        return new AMModelBase(req, getPageSessionAttributes());
    }
    
    // file chooser callbacks
    public void handleMoveUpRequest(RequestInvocationEvent event) {
        String directory = (String)getDisplayFieldValue("lookIn");
        if (directory.equals(File.separator)) {
            forwardTo();
        }        
    }
} 
