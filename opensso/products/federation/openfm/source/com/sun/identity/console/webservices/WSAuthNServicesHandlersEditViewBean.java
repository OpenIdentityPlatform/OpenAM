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
 * $Id: WSAuthNServicesHandlersEditViewBean.java,v 1.3 2008/11/26 18:21:43 farble1670 Exp $
 *
 */

package com.sun.identity.console.webservices;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.webservices.model.WSAuthNServicesModelImpl;
import com.sun.identity.console.webservices.model.WSAuthHandlerEntry;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import java.text.MessageFormat;
import java.util.Map;

public class WSAuthNServicesHandlersEditViewBean
    extends WSAuthNServicesHandlersViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/webservices/WSAuthNServicesHandlersEdit.jsp";
    private static final String PGATTR_INDEX = "handlerTblIndex";
    private boolean populateValues = false;
    private String mechanismEntry = null;
    private String mechanismName = null;

    public WSAuthNServicesHandlersEditViewBean() {
	super("WSAuthNServicesHandlersEdit", DEFAULT_DISPLAY_URL);
    }

    void populateValues(String index) {
	setPageSessionAttribute(PGATTR_INDEX, index);
	populateValues = true;
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException
    {
	super.beginDisplay(event);

	if (populateValues) {
            if (mechanismEntry == null) {
                getRequestData();
            }
	    setValues(mechanismEntry);
	}
    }

    protected void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/threeBtnsPageTitle.xml"));
	ptModel.setPageTitleText(
            "webservices.authentication.service.handlers.edit.page.title");

	ptModel.setValue("button1", "button.save");
	ptModel.setValue("button2", "button.reset");
	ptModel.setValue("button3", 
            getBackButtonLabel("breadcrumbs.webservices.authentication"));
    }

    /**
     * Used to extract the mechanism handler data passed in the page session.
     * The mechanism name is needed to construct the breadcrumb trail.
     */
    private void getRequestData() {
        int index = Integer.parseInt(
            (String)getPageSessionAttribute(PGATTR_INDEX));

        Map mapAttrs = (Map)getPageSessionAttribute(
            WSAuthNServicesViewBean.PROPERTY_ATTRIBUTE);

        OrderedSet set = (OrderedSet)mapAttrs.get(
            WSAuthNServicesModelImpl.ATTRIBUTE_NAME_HANDLERS);

        mechanismEntry = (String)set.get(index);
        WSAuthHandlerEntry entry = new WSAuthHandlerEntry(mechanismEntry);
        mechanismName = entry.strKey;
    }

    /**
     * Handles reset request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton2Request(RequestInvocationEvent event) {
	populateValues = true;
	forwardTo();
    }

    protected void handleButton1Request(Map values) {
	WSAuthNServicesViewBean vb = (WSAuthNServicesViewBean)
	    getViewBean(WSAuthNServicesViewBean.class);

	Map mapAttrs = (Map)getPageSessionAttribute(
	    WSAuthNServicesViewBean.PROPERTY_ATTRIBUTE);
	OrderedSet handlers = (OrderedSet)mapAttrs.get(
	    WSAuthNServicesModelImpl.ATTRIBUTE_NAME_HANDLERS);
	int index = Integer.parseInt((String)
	    getPageSessionAttribute(PGATTR_INDEX));

        String key = (String)values.get(ATTR_KEY);
        String val = (String)values.get(ATTR_CLASS);
        WSAuthHandlerEntry e = new WSAuthHandlerEntry(key, val);
        
        if (handlerExists(handlers, e)) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", "webservices.authentication.service.handlers.duplicates");
            forwardTo(getRequestContext());
            return;
        }        
        
	handlers.set(index, e.toString());
	setPageSessionAttribute(WSAuthNServicesViewBean.PAGE_MODIFIED, "1");
	backTrail();
	unlockPageTrailForSwapping();
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    /**
     * Handles reset request.
     *
     * @param event Request Invocation Event.
     */
    public void handleButton3Request(RequestInvocationEvent event) {
	forwardToAuthServicesView(event);
    }

    protected void createPropertyModel() {
	DelegationConfig dConfig = DelegationConfig.getInstance();
	boolean canModify = dConfig.hasPermission("/", null,
	    AMAdminConstants.PERMISSION_MODIFY,
	    getRequestContext().getRequest(), getClass().getName());

	String xmlFile = (canModify) ?
	    "com/sun/identity/console/propertyWSAuthNServicesHandlers.xml" :
	    "com/sun/identity/console/propertyWSAuthNServicesHandlers_Readonly.xml";

	propertySheetModel = new AMPropertySheetModel(
	    getClass().getClassLoader().getResourceAsStream(xmlFile));
	propertySheetModel.clear();
    }

    protected String getBreadCrumbDisplayName() {
	if (mechanismName == null) {
            getRequestData();
        }
        String[] arg = { mechanismName };
        String tmp = getModel().getLocalizedString(
            "breadcrumbs.webservices.authentication.mechanism.handler.edit");

        return MessageFormat.format(tmp, arg);
    }

    protected boolean startPageTrail() {
	return false;
    }
}
