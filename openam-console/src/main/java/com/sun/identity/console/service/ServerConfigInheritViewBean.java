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
 * $Id: ServerConfigInheritViewBean.java,v 1.5 2008/08/25 22:15:38 veiming Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.service.model.ServerSiteModel;
import com.sun.identity.console.service.model.ServerSiteModelImpl;
import com.sun.web.ui.view.html.CCStaticTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

/**
 * Servers and Sites Management main page.
 */
public class ServerConfigInheritViewBean
    extends AMPrimaryMastHeadViewBean
{
    private static final String DEFAULT_DISPLAY_URL =
        "/console/service/ServerConfigInherit.jsp";
    private static final String TBL_PROPERTY_NAMES = "tblPropertyNames";
    private static final String PGTITLE_THREE_BTNS = "pgtitleThreeBtns";
    
    static final String PG_ATTR_PROPERTY_NAMES = "pgAttrPropertyNames";
    static final String PG_ATTR_CONFIG_PAGE = "pgAttrConfigPage";
    
    private static final String TBL_COL_PROPERTY_NAME = "tblColPropertyName";
    private static final String TBL_COL_VALUE = "tblColValue";
    private static final String TBL_DATA_PROPERTY_NAME = "tblDataPropertyName";
    private static final String TBL_DATA_VALUE = "tblDataValue";
    
    private CCActionTableModel tblPropertyNamesModel = null;
    private CCPageTitleModel ptModel;
    private boolean submitCycle;
    
    private static final String CHILD_STATICTEXT = "StaticText";
    /**
     * Creates a servers and sites view bean.
     */
    public ServerConfigInheritViewBean() {
        super("ServerConfigInherit");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
    
    protected void initialize() {
        if (!initialized) {
            String serverName = (String)getPageSessionAttribute(
                ServerEditViewBeanBase.PG_ATTR_SERVER_NAME);
            if (serverName != null) {
                super.initialize();
                createPageTitleModel();
                createTableModel();
                registerChildren();
                initialized = true;
            }
        }
    }
    
    protected void registerChildren() {
        super.registerChildren();
        ptModel.registerChildren(this);
        registerChild(TBL_PROPERTY_NAMES, CCActionTable.class);
        tblPropertyNamesModel.registerChildren(this);
        registerChild(CHILD_STATICTEXT, CCStaticTextField.class);
    }

    protected View createChild(String name) {
        View view = null;
        if (name.equals(TBL_PROPERTY_NAMES)) {
            SerializedField szCache = (SerializedField)getChild(
                SZ_CACHE);
            populatePropertyNameTableModel((List)szCache.getSerializedObj());
            view = new CCActionTable(this, tblPropertyNamesModel, name);
        } else if (name.equals(PGTITLE_THREE_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (tblPropertyNamesModel.isChildSupported(name)) {
            view = tblPropertyNamesModel.createChild(this, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(CHILD_STATICTEXT)) {
            view = new CCStaticTextField(this, name, null);           
        } else {
            view = super.createChild(name);
        }
        return view;
    }
    
    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
    }

    /**
     * Displays servers and sites information.
     *
     * @param event Display Event.
     * @throws ModelControlException if unable to initialize model.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        Set propertyNames = (Set)getPageSessionAttribute(
            PG_ATTR_PROPERTY_NAMES);
        populatePropertyNameTableModel(propertyNames);
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new ServerSiteModelImpl(req, getPageSessionAttributes());
    }

    private void createTableModel() {
        tblPropertyNamesModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblPropertyNames.xml"));
        tblPropertyNamesModel.setTitleLabel("label.items");
        tblPropertyNamesModel.setActionValue(TBL_COL_PROPERTY_NAME,
            "table.inherit.property.name.column.name");
        tblPropertyNamesModel.setActionValue(TBL_COL_VALUE,
            "table.inherit.property.name.column.value");
    }

    private void populatePropertyNameTableModel(Collection propertyNames) {
        if (!submitCycle && (propertyNames != null)) {
            tblPropertyNamesModel.clearAll();
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
            
            String serverName = (String)getPageSessionAttribute(
                ServerEditViewBeanBase.PG_ATTR_SERVER_NAME);
            ServerSiteModel model = (ServerSiteModel)getModel();
            Map defaultValues = model.getServerDefaults();
            ArrayList cache = new ArrayList();
            
            try {
                Map serverProperties = model.getServerConfiguration(serverName);
                int counter = 0;
                boolean first = false;
                
                for (Iterator iter = propertyNames.iterator(); iter.hasNext();
                counter++) {
                    if (counter > 0) {
                        tblPropertyNamesModel.appendRow();
                    }
                    
                    String name = (String)iter.next();
                    String displayName = name.substring(3);
                    displayName = "amconfig." +
                        displayName.replaceAll("-", ".");

                    String actualPropertyName = ServerEditViewBeanBase.
                        getActualPropertyName(name);
                    tblPropertyNamesModel.setValue(TBL_DATA_PROPERTY_NAME,
                        displayName);
                    tblPropertyNamesModel.setValue(TBL_DATA_VALUE,
                        (String)defaultValues.get(actualPropertyName));
                    tblPropertyNamesModel.setSelectionVisible(counter, true);
                    tblPropertyNamesModel.setRowSelected(
                        !serverProperties.containsKey(actualPropertyName));
                    cache.add(name);
                }
                szCache.setValue(cache);
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }
        }
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }

    /**
     * Handles save profile request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException {
        submitCycle = true;
        CCActionTable table = (CCActionTable)getChild(TBL_PROPERTY_NAMES);
        table.restoreStateData();
        Integer[] selected = tblPropertyNamesModel.getSelectedRows();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        List list = (List)szCache.getSerializedObj();
        Set toInherit = new HashSet(selected.length *2);

        for (int i = 0; i < selected.length; i++) {
            String name = (String)list.get(selected[i].intValue());
            toInherit.add(ServerEditViewBeanBase.getActualPropertyName(name));
        }
        Set notToInherit = new HashSet(list.size() *2);

        for (Iterator i = list.iterator(); i.hasNext(); ) {
            String name = (String)i.next();
            notToInherit.add(
                ServerEditViewBeanBase.getActualPropertyName(name));
        }
        notToInherit.removeAll(toInherit);

        try {
            ServerSiteModel model = (ServerSiteModel)getModel();
            String serverName = (String)getPageSessionAttribute(
                ServerEditViewBeanBase.PG_ATTR_SERVER_NAME);
            model.updateServerConfigInheritance(
                serverName, toInherit, notToInherit);
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "servercfg.inheritance.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }

        forwardTo();        
    }

    /**
     * Handles return to server configuration page request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        try {
            AMViewBeanBase vb = (AMViewBeanBase)getViewBean(
                Class.forName((String)
                    getPageSessionAttribute(PG_ATTR_CONFIG_PAGE)));
            removePageSessionAttribute(PG_ATTR_CONFIG_PAGE);
            removePageSessionAttribute(PG_ATTR_PROPERTY_NAMES);
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (ClassNotFoundException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }
    
    protected String getBackButtonLabel() {
        return getBackButtonLabel("page.title.server.config");
    }
}
