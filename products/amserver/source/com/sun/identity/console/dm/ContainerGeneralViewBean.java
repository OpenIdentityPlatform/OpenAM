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
 * $Id: ContainerGeneralViewBean.java,v 1.2 2008/06/25 05:42:53 qcheng Exp $
 *
 */

package com.sun.identity.console.dm;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.dm.model.ContainerModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;

public class ContainerGeneralViewBean
    extends ContainerPropertiesBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/ContainerGeneral.jsp";

    public ContainerGeneralViewBean() {
        super("ContainerGeneral");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
	if (!initialized) {
	    String realmName = (String)getPageSessionAttribute(
		AMAdminConstants.CURRENT_PROFILE);
	    if (realmName != null) {
		super.initialize();
		initialized = true;
		createPageTitleModel();
		createTabModel();
		registerChildren();
	    }
	}
    }

    protected void registerChildren() {
        ptModel.registerChildren(this);
        super.registerChildren();
    }

    protected View createChild(String name) {
        View view = null;

        if (name.equals(PAGE_TITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException
    {
        super.beginDisplay(event);

        model = (ContainerModel)getModel();
        if (!model.hasDisplayProperties()) {
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                model.getLocalizedString("no.properties"));
        }

        setPageTitle("page.title.container.properties");
    }

    /**
     * Handles create realm request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) 
        throws ModelControlException
    {
        model = (ContainerModel)getModel();
        if (model.hasDisplayProperties()) {
            forwardTo();
        } else {
            forwardToContainerView(event);
        }
    }

    /**
     * Handles create realm request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) 
        throws ModelControlException
    {
        forwardTo();
    }

    /**
     * Handles create realm request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) 
        throws ModelControlException
    {
        forwardToContainerView(event);
    }

    protected void createPageTitleModel() {
        model = (ContainerModel)getModel();
        if (model.hasDisplayProperties()) {
            ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                    "com/sun/identity/console/threeBtnsPageTitle.xml"));
            ptModel.setValue("button1", "button.save");
            ptModel.setValue("button2", "button.reset");
            ptModel.setValue("button3", getBackButtonLabel());
        } else {
            ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                    "com/sun/identity/console/oneBtnPageTitle.xml"));
            ptModel.setValue("button1", getBackButtonLabel());
        }
    }
}
