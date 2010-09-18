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
 * $Id: RoleServicesViewBean.java,v 1.2 2008/06/25 05:42:55 qcheng Exp $
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
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.dm.model.RoleModel;
import com.sun.identity.console.dm.model.RoleModelImpl;
import com.sun.identity.console.realm.ServicesEditViewBean;
import com.sun.identity.console.service.model.SCUtils;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class RoleServicesViewBean
    extends RolePropertiesBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/RoleServices.jsp";

    protected static final String PAGE_TITLE = "pgtitle";
    protected static final String TBL_SEARCH = "tblSearch";
    protected static final String TBL_COL_NAME = "tblColName";
    protected static final String TBL_DATA_ACTION_HREF = "tblDataActionHref";
    protected static final String TBL_DATA_NAME = "tblDataName";

    protected CCActionTableModel tblModel = null;

    /**
     * Creates a authentication domains view bean.
     */
    public RoleServicesViewBean() {
	super("RoleServices");
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
        setPageTitle("page.title.role.services");
    }

    protected AMModel getModelInternal() {                            
	HttpServletRequest req = getRequestContext().getRequest();
	return new RoleModelImpl(req, getPageSessionAttributes());
    }

    protected Map getEntries() {
	String location = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_ORG);

        RoleModel model = (RoleModel)getModel();
        if (location == null) {
            location = model.getStartDSDN();
        }
        return model.getAssignedServices(location);
    }

    protected void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/simplePageTitle.xml"));
    }

    private  void createTableModel() {
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblRoleServices.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_COL_NAME, "table.dm.name.column.name");
    }

    protected void populateTableModel(Map services) {
	tblModel.clearAll();
	SerializedField szCache = (SerializedField)getChild(SZ_CACHE);

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
     * Handles edit realm request.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblDataActionHrefRequest(RequestInvocationEvent event)
	throws ModelControlException
    {
        String serviceName = (String)getDisplayFieldValue(TBL_DATA_ACTION_HREF);
        RoleModel model = (RoleModel)getModel();

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
            ServicesEditViewBean vb = (ServicesEditViewBean)getViewBean(
                ServicesEditViewBean.class);
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
