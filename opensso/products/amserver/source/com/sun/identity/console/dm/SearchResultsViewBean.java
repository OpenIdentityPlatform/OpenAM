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
 * $Id: SearchResultsViewBean.java,v 1.2 2008/06/25 05:42:55 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.dm;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.dm.model.SearchModel;
import com.sun.identity.console.dm.model.SearchModelImpl;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SearchResultsViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/SearchResults.jsp";

    private CCActionTableModel tblModel = null;
    private CCPageTitleModel ptModel = null;
    private SearchModel model = null;
    private String bcName = "breadcrumbs.directorymanager.search.results";
    private boolean groupSearch = false;

    protected static final String SZ_CACHE_RESULTS = "szCacheResults";
    protected static final String FILTER_MAP = "filterMap";
    protected static final String TBL_SEARCH = "tblSearch";
    protected static final String PAGE_TITLE = "pgtitle";
    protected static final String TBL_DATA_NAME = "tblDataName";
    protected static final String TBL_DATA_HIDDEN_NAME = "tblDataHiddenName";
    protected static final String TBL_COL_NAME = "tblColName";
    protected static final String TBL_DATA_PATH = "tblDataPath";
    protected static final String TBL_COL_PATH = "tblColPath";

    private boolean tablePopulated;

    /**
     * Creates a authentication domains view bean.
     */
    public SearchResultsViewBean() {
	super("SearchResults");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
    }

    protected void initialize() {
        if (!initialized) {
	    createTableModel();
	    registerChildren();
	    initialized = true;
	}
    }

    protected View createChild(String name) {
	View view = null;
	if (name.equals(TBL_SEARCH)) {
	    SerializedField szCache = (SerializedField)getChild(
		SZ_CACHE_RESULTS);
	    List entries = (List)szCache.getSerializedObj();
	    if ((entries != null) && !entries.isEmpty()) {
		populateTableModel(entries);
	    }
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

    protected void registerChildren() {
        super.registerChildren();
	registerChild(SZ_CACHE_RESULTS, SerializedField.class);
        registerChild(PAGE_TITLE, CCPageTitle.class);
        registerChild(TBL_SEARCH, CCActionTable.class);
        ptModel.registerChildren(this);
        tblModel.registerChildren(this);
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException
    {
	super.beginDisplay(event);
	populateTableModel(getEntries());
    }

    protected void populateTableModel(Collection names) {
	if (!tablePopulated) {
	    tablePopulated = true;
	    tblModel.clearAll();
	    SerializedField szCache = (SerializedField)getChild(
		SZ_CACHE_RESULTS);
	    SearchModel model = (SearchModel)getModel();
	    tblModel.setMaxRows(model.getPageSize());
	    ArrayList cache = new ArrayList();
	    if ((names != null) && !names.isEmpty()) {
		boolean firstEntry = true;
                
		for (Iterator iter = names.iterator(); iter.hasNext(); ) {
		    if (firstEntry) {
			firstEntry = false;
		    } else {
			tblModel.appendRow();
		    }

		    String userName = (String)iter.next();
                    if (groupSearch) {
                        tblModel.setValue(
                            TBL_DATA_NAME, model.DNToName(userName, false));
                    } else {
                        // call getUserDisplayValue to properly construct the
                        // user name if it is multivalued.
                        tblModel.setValue(
                            TBL_DATA_NAME, model.getUserDisplayValue(userName)); 
                    }

                    tblModel.setValue(TBL_DATA_HIDDEN_NAME, userName);
                    tblModel.setValue(TBL_DATA_PATH,
                        model.getDisplayPath(userName));
		    cache.add(userName);
		}
		szCache.setValue(cache);
	    } else {
		szCache.setValue(null);
	    }
	}
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        return new SearchModelImpl(
            rc.getRequest(), getPageSessionAttributes());
    }

    public void setFilterData(Map m) {
	setPageSessionAttribute(FILTER_MAP, (Serializable)m);
    }

    protected Set getEntries() {
        String location = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_PROFILE);
        if (location == null || location.length() == 0) {
            location = model.getStartDSDN();
        }
        SearchModel model = (SearchModel)getModel();
        Set entries = null;
	Map orig = (Map)getPageSessionAttribute(FILTER_MAP);
	Map filterData = null;

	if (orig != null) {
	    /*
	     * Clone the filter data so that the one in page session
	     * remain unmodified.
	     */
	    filterData = AMAdminUtils.cloneStringToSetMap(orig);
	}

        if (filterData != null) {
            Set op = (Set)filterData.remove(
		SearchModel.ATTR_NAME_LOGICAL_OPERATOR);
            if (op != null && !op.isEmpty()) {
		// process user search
                model.setSearchType(SearchModel.MEMBERSHIP);
                String operator = (String)op.iterator().next();
                entries = model.searchUsers(operator, filterData, location);
            } else {
                // process group search
                entries = model.searchGroups(location, filterData);
            }
        }

	String error = model.getError();
	if ((error != null) && (error.length() > 0)) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", error);
        }
        return (entries == null) ? Collections.EMPTY_SET : entries;
    }
    
    protected void createTableModel() {
        SearchModel model = (SearchModel)getModel();
        
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblDMSearchResults.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_COL_NAME, "table.dm.name.column.name");
        tblModel.setActionValue(TBL_COL_PATH, "table.dm.path.column.name");
    }

    protected void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.back");
        ptModel.setValue("button2", "button.finish");
        ptModel.setValue("button3", "button.cancel");
    }

    /**
     * Handles search request.
     *
     * @param event Request Invocation Event.
     */
    public void handleBtnSearchRequest(RequestInvocationEvent event) {
	tablePopulated = false;
	populateTableModel(getEntries());
	forwardTo();
    }    

    /**
     * Handles back request.
     *
     * @param event Request invocation event.
     */
    public void handleButton1Request(RequestInvocationEvent event) {
        String backVB = 
	    (String)getPageSessionAttribute(SearchModel.BACK_VB_NAME);

        if (backVB != null) {
            try {
                Class clazz = Class.forName(backVB);
                AMViewBeanBase vb = (AMViewBeanBase)getViewBean(clazz);
	        lockPageTrail();
	        passPgSessionMap(vb);
	        vb.forwardTo(getRequestContext());
            } catch (ClassNotFoundException cnfe) {
		forwardTo();
            }
        } else {
            forwardTo();
        }
    }
    
    /**
     * Handles selection request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) 
	throws ModelControlException
    {
        CCActionTable table = (CCActionTable)getChild(TBL_SEARCH);
        table.restoreStateData();
        Integer[] selected = tblModel.getSelectedRows();
	SerializedField szCache = (SerializedField)getChild(SZ_CACHE_RESULTS);
	List entries = (List)szCache.getSerializedObj();

        // create a set of the names selected in the table
        Set names = new HashSet(selected.length * 2);
        for (int i = 0; i < selected.length; i++) {
            int idx = selected[i].intValue();
            names.add((String)entries.get(idx));
        }
        String type = (String)getPageSessionAttribute(SearchModel.SEARCH_TYPE);
        String entry = 
            (String)getPageSessionAttribute(AMAdminConstants.CURRENT_PROFILE);

        SearchModel model = (SearchModel)getModel();
        try {
            if (!names.isEmpty()) {
                model.addMembers(entry, names, type);
            }
	    forwardToCallingBean();
        } catch (AMConsoleException a) {
            setInlineAlertMessage(
                CCAlert.TYPE_ERROR, "message.error", a.getMessage());
	    forwardTo();
        }
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event.
     */
    public void handleButton3Request(RequestInvocationEvent event) {
	forwardToCallingBean();
    }

    protected void setBreadCrumbDisplayName(String displayName) {
	bcName = displayName;
    }
    protected String getBreadCrumbDisplayName() {
        return bcName;
    }

    protected boolean startPageTrail() {
	return false;
    }

    private void forwardToCallingBean() {
        AMViewBeanBase vb = getCallingView();
        if (vb != null) {       
            backTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        }
    }

    public void setPageType(String type) {
        groupSearch = type.equals(SearchModel.GROUP_SEARCH);
    }
}
