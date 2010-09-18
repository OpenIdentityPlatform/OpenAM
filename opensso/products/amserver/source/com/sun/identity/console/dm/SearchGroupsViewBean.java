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
 * $Id: SearchGroupsViewBean.java,v 1.2 2008/06/25 05:42:55 qcheng Exp $
 *
 */

package com.sun.identity.console.dm;

import com.iplanet.am.sdk.AMConstants;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.dm.model.SearchModel;
import com.sun.identity.console.dm.model.SearchModelImpl;
import com.sun.identity.console.realm.RMRealmOpViewBeanBase;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCRadioButton;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.util.Map;
import java.util.HashMap;

public class SearchGroupsViewBean
    extends RMRealmOpViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/dm/SearchGroups.jsp";

    private static final String PGTITLE_TWO_BTNS = "pgtitleTwoBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private static final String BREAD_CRUMB_TRAIL = 
	"breadcrumbs.directorymanager.group.add";

    private AMPropertySheetModel propertySheetModel;
    private SearchModel model = null;

    public SearchGroupsViewBean() {
	super("SearchGroups");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createPageTitleModel();
        createPropertyModel();
        registerChildren();
    }

    protected void registerChildren() {
	super.registerChildren();
	registerChild(PGTITLE_TWO_BTNS, CCPageTitle.class);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
	propertySheetModel.registerChildren(this);
    }

    protected View createChild(String name) {
	View view = null;

	if (name.equals(PGTITLE_TWO_BTNS)) {
	    view = new CCPageTitle(this, ptModel, name);
	} else if (name.equals(PROPERTY_ATTRIBUTE)) {
	    view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
	    view = propertySheetModel.createChild(this, name);
	} else {
	    view = super.createChild(name);
	}

	return view;
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException
    {
	super.beginDisplay(event);
	disableButton("button1", true);

	// default search scope to the current organization which is scope 1
	CCRadioButton rb = (CCRadioButton)getChild(SearchModel.SEARCH_SCOPE);
	rb.setValue("" + AMConstants.SCOPE_ONE);

        ptModel.setPageTitleText(
            model.getLocalizedString("page.title.search.groups"));
    }

    private void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/threeBtnsPageTitle.xml"));
	ptModel.setValue("button1", "button.back");
	ptModel.setValue("button2", "button.next");
	ptModel.setValue("button3", "button.cancel");
    }

    private void createPropertyModel() {
	model = (SearchModel)getModel();
	propertySheetModel = new AMPropertySheetModel(
	    model.getGroupSearchXML());
	propertySheetModel.clear();
    }
    
    protected AMModel getModelInternal() {
	if (model == null) {
            RequestContext rc = RequestManager.getRequestContext();
	    model = new SearchModelImpl(
		rc.getRequest(),getPageSessionAttributes());
	}
        return model;
    }

    public void handleButton3Request(RequestInvocationEvent event) {
        GroupMembersViewBean vb = (GroupMembersViewBean)getViewBean(
	    GroupMembersViewBean.class);
	backTrail();
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    /**
     * Handles next page request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event)
	throws ModelControlException
    {
	SearchModel model = (SearchModel)getModel();
	String location = (String)getPageSessionAttribute(
	    AMAdminConstants.CURRENT_ORG);
        if (location == null || location.length() == 0) {
            location = model.getStartDSDN();
	}
        // construct the filter and forward it on to the search results page
	try {
            AMPropertySheet ps = 
	        (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);

	    Map values = ps.getAttributeValues(
		model.getGroupDataMap(), false, model);
		
            if (values == null) {
                values = new HashMap();
            }

	    // the view view bean to display when the back button is pressed
	    // from the search results page.
	    setPageSessionAttribute(SearchModel.BACK_VB_NAME,
		getClass().getName());

	    // forward to the search results page.
            setPageSessionAttribute(
                SearchModel.SEARCH_TYPE, SearchModel.GROUP_SEARCH);
            SearchResultsViewBean vb = (SearchResultsViewBean)
                getViewBean(SearchResultsViewBean.class);
            unlockPageTrailForSwapping();
            passPgSessionMap(vb);
            vb.setFilterData(values);
            vb.setBreadCrumbDisplayName(BREAD_CRUMB_TRAIL);
            vb.setPageType(SearchModel.GROUP_SEARCH);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
	        e.getMessage());
	    forwardTo();
	}
    }

    protected String getBreadCrumbDisplayName() {
	return BREAD_CRUMB_TRAIL;
    }

    protected boolean startPageTrail() {
	return false;
    }
}
