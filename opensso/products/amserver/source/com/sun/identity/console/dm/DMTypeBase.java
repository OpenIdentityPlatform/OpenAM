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
 * $Id: DMTypeBase.java,v 1.2 2008/06/25 05:42:54 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.dm;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.HREF;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.dm.model.DMModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCBreadCrumbsModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.breadcrumb.CCBreadCrumbs;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.view.tabs.CCTabs;
import java.util.Iterator;
import java.util.List;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;

public abstract class DMTypeBase
    extends AMPrimaryMastHeadViewBean
{
    protected static final String PARENTAGE_PATH = "parentagepath";
    protected static final String TXT_ROOT = "txtRoot";
    protected static final String PARENTAGE_PATH_HREF = "parentagepathHref";

    protected static final String TF_FILTER = "tfFilter";
    protected static final String BTN_SEARCH = "btnSearch";

    protected static final String TBL_SEARCH = "tblSearch";
    protected static final String TBL_BUTTON_ADD = "tblButtonAdd";
    protected static final String TBL_BUTTON_DELETE = "tblButtonDelete";

    protected static final String TBL_COL_NAME = "tblColName";
    protected static final String TBL_DATA_NAME = "tblDataName";
    protected static final String TBL_DATA_HREF = "tblDataHref";

    protected static final String TBL_COL_PATH = "tblColPath";
    protected static final String TBL_DATA_PATH = "tblDataPath";

    protected static final String TBL_DATA_ACTION_HREF = "tblDataActionHref";

    protected static final String PAGETITLE = "pgtitle";

    protected CCActionTableModel tblModel = null;
    protected CCPageTitleModel ptModel;

    public DMTypeBase(String name) {
	super(name);
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/simplePageTitle.xml"));
        createTableModel();
        registerChildren();
    }

    public void forwardTo(RequestContext rc) {
	String location = (String)getPageSessionAttribute(
	    AMAdminConstants.CURRENT_ORG);
	if (location == null) {
	    setPageSessionAttribute(AMAdminConstants.CURRENT_ORG,
		getModel().getStartDSDN());
	}
	super.forwardTo(rc);
    }

    protected void registerChildren() {
	super.registerChildren();
	registerChild(PARENTAGE_PATH, CCBreadCrumbs.class);
	registerChild(PARENTAGE_PATH_HREF, HREF.class);
	registerChild(TF_FILTER, CCTextField.class);
	registerChild(BTN_SEARCH, CCButton.class);
	registerChild(PAGETITLE, CCPageTitle.class);
	registerChild(TBL_SEARCH, CCActionTable.class);
	registerChild(TAB_COMMON, CCTabs.class);
	ptModel.registerChildren(this);
	tblModel.registerChildren(this);
    }

    protected View createChild(String name) {
	View view = null;

	if (name.equals(TBL_SEARCH)) {
	    SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
	    populateTableModel((Set)szCache.getSerializedObj());
	    view = new CCActionTable(this, tblModel, name);
	} else if (name.equals(PAGETITLE)) {
	    view = new CCPageTitle(this, ptModel, name);
	} else if (tblModel.isChildSupported(name)) {
	    view = tblModel.createChild(this, name);
	} else if (name.equals(PARENTAGE_PATH)) {                  
	    view = createParentagePath(name);
	} else if (name.equals(PARENTAGE_PATH_HREF)) {
	    view = new HREF(this, name, null);
	} else {
	    view = super.createChild(name);
	}

	return view;
    }

    protected void createTableModel() {
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(getTableXML()));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_BUTTON_ADD, "table.dm.button.new");
        tblModel.setActionValue(TBL_BUTTON_DELETE, "table.dm.button.delete");
        tblModel.setActionValue(TBL_COL_NAME, "table.dm.name.column.name");
        tblModel.setActionValue(TBL_COL_PATH, "table.dm.path.column.name");
    }

    protected void createTabModel() {
        DMModel model = (DMModel)getModel();
        AMViewConfig amconfig = AMViewConfig.getInstance();
        amconfig.setTabViews(
            AMAdminConstants.ORGANIZATION_NODE_ID, model.getTabMenu());

        tabModel = amconfig.getTabsModel(
            getCurrentRealm(), getRequestContext().getRequest());
        registerChild(TAB_COMMON, CCTabs.class);
    }  

    /**
     * Returns the default table definition 
     */
    protected String getTableXML() {
        return "com/sun/identity/console/tblDMTypes.xml";
    }

    private CCBreadCrumbs createParentagePath(String name) {
        CCBreadCrumbsModel model = null;
        DMModel dmmodel = (DMModel)getModel();
        
        String location = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_ORG);
        if ((location == null) || (location.length() == 0)) {
            location = dmmodel.getStartDSDN();
        }
        
        List nodes = dmmodel.pathToDisplayString(
            location, dmmodel.getStartDSDN(), false);
        
        if (nodes.size() > 1) {
            int size = nodes.size()-1;
            model = new CCBreadCrumbsModel(AMFormatUtils.DNToName(dmmodel,
                (String)nodes.get(size)));
            
            for (int x = 0; x < size; x++) {
                String tok = (String)nodes.get(x);
                model.appendRow();
                model.setValue(CCBreadCrumbsModel.LABEL,
                    AMFormatUtils.DNToName(dmmodel, tok));
                model.setValue(CCBreadCrumbsModel.COMMANDFIELD,
                    PARENTAGE_PATH_HREF);
                model.setValue(CCBreadCrumbsModel.HREF_VALUE, tok);
            }
        } else {
            model = new CCBreadCrumbsModel();
            setDisplayFieldValue(TXT_ROOT, AMFormatUtils.DNToName(
                dmmodel, location));
        }
        return new CCBreadCrumbs(this, model, name);
    }

    /**
     * Returns the value of the search filter field. If there is no value 
     * set, the default value * is returned.
     */
    protected String getFilter() {
        CCTextField filter = (CCTextField)getChild(TF_FILTER);
        String  value = (String)filter.getValue();
        if ((value == null) || (value.length() == 0)) {
            value = "*";
        }
        return value;
    }

    protected void populateTableModel(Set realmNames) {
	tblModel.clearAll();
	DMModel model = (DMModel)getModel();
	tblModel.setMaxRows(model.getPageSize());
	SerializedField szCache = (SerializedField)getChild(SZ_CACHE);

	if ((realmNames != null) && !realmNames.isEmpty()) {
	    boolean firstEntry = true;
	    String startDN = model.getStartDSDN();

	    for (Iterator iter = realmNames.iterator(); iter.hasNext(); ) {
		if (firstEntry) {
		    firstEntry = false;                                  
		} else {
		    tblModel.appendRow();
		}
		String name = (String)iter.next();

		// organization name column
                tblModel.setValue(TBL_DATA_NAME, model.DNToName(name, true));
		tblModel.setSelectionVisible(!name.equals(startDN));
	        tblModel.setValue(TBL_DATA_ACTION_HREF, name);

		// path name column
		tblModel.setValue(TBL_DATA_PATH, getPath(model, name));
		tblModel.setValue(TBL_DATA_HREF, name);
	    }
	    szCache.setValue((Serializable)realmNames);
	} else {
	    szCache.setValue(null);
	}
    }
										
    protected String getPath(DMModel model, String dn) {
	StringBuilder path = new StringBuilder(64);

	List nodes = model.pathToDisplayString(dn);
	int size = nodes.size();
	for (int x = 0; x < size; x++) {
	    String tmp  = (String)nodes.get(x);

	    path.append(AMFormatUtils.DNToName(model, tmp));
	    if (x < size-1) {
	        path.append(" > ");
	    }
	}

	return path.toString();
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException
    {
	super.beginDisplay(event);
	populateTableModel(getEntries());

        DMModel model = (DMModel)getModel();
        String location = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_ORG);
        setSelectedTabNode(location);

        // set the state for the add and delete buttons
        setAddButtonState(location);
        resetButtonState(TBL_BUTTON_DELETE);

        // display any messages set by the model while retrieving the data
	String error = model.getErrorMessage();
	if (error != null && error.length() > 0) {
            setInlineAlertMessage(
		CCAlert.TYPE_INFO, "message.information", error);
	}    

        createTabModel();
    }

    /**
     * Handles parentage path request.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleParentagepathHrefRequest(RequestInvocationEvent event) {
	String path = (String)getDisplayFieldValue(PARENTAGE_PATH_HREF);
	setPageSessionAttribute(AMAdminConstants.CURRENT_ORG, path);
	setCurrentLocation(path);
	forwardTo();
    }

    protected String deleteEntries(DMModel model) 
        throws AMConsoleException, ModelControlException
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
                                                     
        Map failed = model.deleteObject(names); 
        int size = failed.size();
	String message = "message.delete.entries";
        if (!failed.isEmpty()) {
            // construct error message from failed map list
            String tmp = (String)failed.keySet().iterator().next();
            throw new AMConsoleException((String)failed.get(tmp));
        }   
        return message;
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
	try {
            String message = deleteEntries((DMModel)getModel());
            setInlineAlertMessage(
                CCAlert.TYPE_INFO, "message.information", message);
        } catch (AMConsoleException e) {
           setInlineAlertMessage(
               CCAlert.TYPE_ERROR, "message.error", e.getMessage());
        }
        forwardTo();
    }    
    
    /**
     * Handles the drill down navigation request.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblDataHrefRequest(RequestInvocationEvent event)
	throws ModelControlException 
    {
        String tmp = (String)getDisplayFieldValue(TBL_DATA_HREF);
	setPageSessionAttribute(AMAdminConstants.CURRENT_ORG, tmp);
        getModel().setLocationDN(tmp);
	forwardTo();
    }

    protected void setAddButtonState(String location) {
        // leave blank in case view doesn't have an add button
    }
    
    protected abstract Set getEntries();
}
