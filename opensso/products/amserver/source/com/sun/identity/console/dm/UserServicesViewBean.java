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
 * $Id: UserServicesViewBean.java,v 1.2 2008/06/25 05:42:55 qcheng Exp $
 *
 */

package com.sun.identity.console.dm;

import com.iplanet.am.util.Locale;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.dm.model.UserModel;
import com.sun.identity.console.dm.model.UserModelImpl;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.identity.console.service.model.SCUtils;
import com.sun.identity.console.realm.ServicesEditViewBean;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.view.alert.CCAlert;
import java.io.IOException;
import java.io.Serializable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.UnsupportedEncodingException;

public class UserServicesViewBean
    extends UserPropertiesBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/UserServices.jsp";

    protected static final String PAGE_TITLE = "pgtitle";

    protected static final String TBL_SEARCH = "tblSearch";
    protected static final String TBL_BUTTON_ADD = "tblButtonAdd";
    protected static final String TBL_BUTTON_DELETE = "tblButtonDelete";

    protected static final String TBL_COL_NAME = "tblColName";
    protected static final String TBL_DATA_ACTION_HREF = "tblDataActionHref";
    protected static final String TBL_DATA_NAME = "tblDataName";

    protected CCActionTableModel tblModel = null;

    /**
     * Creates a authentication domains view bean.
     */
    public UserServicesViewBean() {
	super("UserServices");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createTableModel();
	registerChildren();
    }

    protected void registerChildren() {
	super.registerChildren();
	registerChild(PAGE_TITLE, CCPageTitle.class);
	registerChild(TBL_SEARCH, CCActionTable.class);
	ptModel.registerChildren(this);
	tblModel.registerChildren(this);
    }

    protected View createChild(String name) {
	View view = null;

	if (name.equals(TBL_SEARCH)) {
	    SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
	    populateTableModel((Map)szCache.getSerializedObj());
	    view = new CCActionTable(this, tblModel, name);
	} else if (name.equals(PAGE_TITLE)) {
	    view = new CCPageTitle(this, ptModel, name);
	} else if (tblModel.isChildSupported(name)) {
	    view = tblModel.createChild(this, name);
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
	populateTableModel(getEntries());
	resetButtonState(TBL_BUTTON_DELETE);
        setPageTitle("page.title.user.services");
    }

    protected AMModel getModelInternal() {                            
	HttpServletRequest req = getRequestContext().getRequest();
	return new UserModelImpl(req, getPageSessionAttributes());
    }

    protected Map getEntries() {
        String userName = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);

        UserModel model = (UserModel)getModel();
        return (userName == null) ? 
            Collections.EMPTY_MAP : model.getAssignedServices(userName);
    }

    protected void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/simplePageTitle.xml"));
    }

    private  void createTableModel() {
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblUserServices.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_BUTTON_ADD, "add.service.button");
        tblModel.setActionValue(TBL_BUTTON_DELETE, "remove.service.button");
        tblModel.setActionValue(TBL_COL_NAME, "name.column");
    }

    protected void populateTableModel(Map services) {
	tblModel.clearAll();
	SerializedField szCache = (SerializedField)getChild(SZ_CACHE);

	AMModel model = (AMModelBase)getModel();
	if ((services != null) && !services.isEmpty()) {
	    boolean firstEntry = true;

	    for (Iterator i = services.keySet().iterator(); i.hasNext(); ) {
		if (firstEntry) {
		    firstEntry = false;
		} else {
		    tblModel.appendRow();
		}

		String name = (String)i.next();
                tblModel.setValue(TBL_DATA_NAME, services.get(name));
	        tblModel.setValue(TBL_DATA_ACTION_HREF, name);
	    }
	    szCache.setValue((Serializable)services);
	} else {
	    szCache.setValue(null);
	}
    }

    /**
     * Forwards request to creation view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblButtonAddRequest(RequestInvocationEvent event) {
	setPageSessionAttribute(
	    AMAdminConstants.SAVE_VB_NAME, getClass().getName());

        UserSelectServicesViewBean vb = (UserSelectServicesViewBean)
	    getViewBean(UserSelectServicesViewBean.class);
        unlockPageTrail();
	passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Deletes authentication domains.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblButtonDeleteRequest(RequestInvocationEvent event)
	throws ModelControlException
    {
        CCActionTable table = (CCActionTable)getChild(TBL_SEARCH);
        table.restoreStateData();
        Integer[] selected = tblModel.getSelectedRows();
        // create a set of the names selected in the table
        Set names = new HashSet(selected.length * 2);
        for (int i = 0; i < selected.length; i++) {
            tblModel.setRowIndex(selected[i].intValue());
            names.add((String)tblModel.getValue(TBL_DATA_ACTION_HREF));
        }

        UserModel model = (UserModel)getModel();
        try {
            model.removeServices(
                (String)getPageSessionAttribute(
		AMAdminConstants.CURRENT_PROFILE), names);

            String message = "removed.service";
            if (names.size() > 1) {
                message = "removed.multiple.services"; 
            }
            setInlineAlertMessage(
                CCAlert.TYPE_INFO, "message.information", message);
            forwardTo();
        } catch (AMConsoleException e) {
            setInlineAlertMessage(
            CCAlert.TYPE_ERROR, "message.error", e.getMessage());
            forwardTo();
        }
    }

   /**
     * Handles edit service request.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblDataActionHrefRequest(RequestInvocationEvent event)
        throws ModelControlException
    {
        String serviceName = (String)getDisplayFieldValue(TBL_DATA_ACTION_HREF);
        UserModel model = (UserModel)getModel();

        SCUtils utils = new SCUtils(serviceName, model);
        String propertiesViewBeanURL = utils.getServiceDisplayURL();

        if ((propertiesViewBeanURL != null) &&
            (propertiesViewBeanURL.trim().length() > 0)
        ) {
            String org = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_ORG);
            if (org == null) {
                org = model.getStartDSDN();
            }

            try {
		String pageTrailID = (String)getPageSessionAttribute(
		    PG_SESSION_PAGE_TRAIL_ID);
                propertiesViewBeanURL += "?ServiceName=" + serviceName +
                    "&Location=" +
                    Locale.URLEncodeField(org, getCharset(model)) +
                    "&Template=true&Op=" + AMAdminConstants.OPERATION_EDIT +
		    "&" + PG_SESSION_PAGE_TRAIL_ID + "=" + pageTrailID;
                HttpServletResponse response =
                    getRequestContext().getResponse();
                response.sendRedirect(propertiesViewBeanURL);
            } catch (UnsupportedEncodingException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                forwardTo();
            } catch (IOException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                forwardTo();
            }
        } else {
            EditServiceViewBean vb = (EditServiceViewBean)getViewBean(
                EditServiceViewBean.class);
            setPageSessionAttribute(ServicesEditViewBean.SERVICE_NAME,
                serviceName);
            setPageSessionAttribute(
		AMAdminConstants.SAVE_VB_NAME, getClass().getName());

            unlockPageTrail();
	    passPgSessionMap(vb);
            vb.location =  (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_ORG);
            vb.forwardTo(getRequestContext());
        }
    }
}
