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
 * $Id: EntityResourceOfferingViewBean.java,v 1.2 2008/06/25 05:49:41 qcheng Exp $
 *
 */

package com.sun.identity.console.idm;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.idm.model.EntityResourceOfferingModel;
import com.sun.identity.console.idm.model.EntityResourceOfferingModelImpl;
import com.sun.identity.console.service.model.DiscoveryDataCache;
import com.sun.identity.console.service.model.SMDiscoveryServiceData;
import com.sun.identity.console.service.model.SMDiscoEntryData;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import java.text.MessageFormat;
import java.util.Iterator;
import javax.servlet.http.HttpServletRequest;

public class EntityResourceOfferingViewBean
    extends AMPrimaryMastHeadViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/idm/EntityResourceOffering.jsp";
    public static final String DATA_ID = "discoveryDataId";

    private static final String TBL_ENTRIES = "tblEntries";
    private static final String TBL_BUTTON_ADD = "tblButtonAdd";
    private static final String TBL_BUTTON_DELETE = "tblButtonDelete";

    private static final String TBL_COL_SERVICE_TYPE = "tblColServiceType";
    private static final String TBL_COL_ABSTRACT = "tblColAbstract";
    private static final String TBL_DATA_SERVICE_TYPE = "tblDataServiceType";
    private static final String TBL_DATA_ABSTRACT = "tblDataAbstract";
    private static final String TBL_DATA_ACTION_HREF = "tblDataActionHref";

    private static final String PROPERTY_ATTRIBUTE =
	"bootstrapRefPropertyAttributes";
    private static final String PAGETITLE = "pgtitle";

    private CCActionTableModel tblModel = null;
    private CCPageTitleModel ptModel;
    protected AMPropertySheetModel propertySheetModel;
    private boolean submitCycle = false;

    public EntityResourceOfferingViewBean() {
	super("EntityResourceOffering");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
	if (!initialized) {
	    super.initialize();
	    initialized = true;
	    createPageTitleModel();
	    createTableModel();
	    registerChildren();
	}
    }

    private void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/simplePageTitle.xml"));
    }

    protected void createTableModel() {
        tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblEntityResourceOffering.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_BUTTON_ADD,
	    "table.idm.resource.offerings.button.new");
        tblModel.setActionValue(TBL_BUTTON_DELETE,
	    "table.idm.resource.offerings.button.delete");
	tblModel.setActionValue(TBL_COL_SERVICE_TYPE,
	    "table.idm.resource.offerings.column.service.name");
        tblModel.setActionValue(TBL_COL_ABSTRACT,
	    "table.idm.resource.offerings.column.abstract");
    }

    protected void registerChildren() {
	super.registerChildren();
	registerChild(PAGETITLE, CCPageTitle.class);
	registerChild(TBL_ENTRIES, CCActionTable.class);
	ptModel.registerChildren(this);
	tblModel.registerChildren(this);
    }

    protected View createChild(String name) {
	View view = null;

	if (name.equals(PAGETITLE)) {
	    view = new CCPageTitle(this, ptModel, name);
	} else if (name.equals(TBL_ENTRIES)) {
	    String cacheID = (String)getPageSessionAttribute(DATA_ID);
	    if (cacheID != null) {
		DiscoveryDataCache cache = DiscoveryDataCache.getInstance();
		SMDiscoveryServiceData data = cache.getData(
		    getModel().getUserSSOToken(), cacheID);
		populateTableModel(data);
	    }
	    view = new CCActionTable(this, tblModel, name);
	} else if (tblModel.isChildSupported(name)) {
	    view = tblModel.createChild(this, name);
	} else if (ptModel.isChildSupported(name)) {
	    view = ptModel.createChild(this, name);
	} else {
	    view = super.createChild(name);
	}

	return view;
    }

    private void populateTableModel(SMDiscoveryServiceData data) {
        tblModel.clearAll();

        if (data != null) {
	    int counter = 0;
	    EntityResourceOfferingModel model =
		(EntityResourceOfferingModel)getModel();
            for (Iterator i = data.getResourceData().iterator(); i.hasNext();
		counter++
	    ) {
                if (counter > 0) {
                    tblModel.appendRow();
                }
                                                                                
                SMDiscoEntryData entry = (SMDiscoEntryData)i.next();
                tblModel.setValue(TBL_DATA_SERVICE_TYPE, entry.serviceType);
                tblModel.setValue(TBL_DATA_ABSTRACT, entry.abstractValue);
                tblModel.setValue(TBL_DATA_ACTION_HREF,
		    Integer.toString(counter));
            }

	    DiscoveryDataCache cache = DiscoveryDataCache.getInstance();
	    String id = cache.cacheData(model.getUserSSOToken(), data);
	    setPageSessionAttribute(DATA_ID, id);
        }
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException {
	super.beginDisplay(event);
	EntityResourceOfferingModel model =
	    (EntityResourceOfferingModel)getModel();
	ptModel.setPageTitleText("title.idm.resource.offering");
	String univId = (String)getPageSessionAttribute(
	    EntityEditViewBean.UNIVERSAL_ID);

	try {
	    populateTableModel(model.getEntityDiscoEntry(univId));
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		e.getMessage());
	}
    }

    protected AMModel getModelInternal() {
	HttpServletRequest req = getRequestContext().getRequest();
	return new EntityResourceOfferingModelImpl(
	    req, getPageSessionAttributes());
    }

    public void handleTblButtonAddRequest(RequestInvocationEvent event) {
	EntityResourceOfferingAddViewBean vb = 
	    (EntityResourceOfferingAddViewBean)getViewBean(
		EntityResourceOfferingAddViewBean.class);
	removePageSessionAttribute(PROPERTY_ATTRIBUTE);
	passPgSessionMap(vb);
	vb.forwardTo(getRequestContext());
    }

    public void handleTblDataActionHrefRequest(RequestInvocationEvent event) {
	String index = (String)getDisplayFieldValue(TBL_DATA_ACTION_HREF);
	EntityResourceOfferingEditViewBean vb =
	    (EntityResourceOfferingEditViewBean)getViewBean(
		EntityResourceOfferingEditViewBean.class);
	removePageSessionAttribute(PROPERTY_ATTRIBUTE);
	passPgSessionMap(vb);
	vb.populateValues(index);
	vb.forwardTo(getRequestContext());
    }

    public void handleTblButtonDeleteRequest(RequestInvocationEvent event)
	throws ModelControlException {
        CCActionTable table = (CCActionTable)getChild(TBL_ENTRIES);
        table.restoreStateData();
	EntityResourceOfferingModel model =
            (EntityResourceOfferingModel)getModel();
	String univId = (String)getPageSessionAttribute(
	    EntityEditViewBean.UNIVERSAL_ID);

        DiscoveryDataCache cache = DiscoveryDataCache.getInstance();
        String cacheID = (String)getPageSessionAttribute(
            EntityResourceOfferingViewBean.DATA_ID);
        SMDiscoveryServiceData smEntry = cache.getData(
            model.getUserSSOToken(), cacheID);
        Integer[] selected = tblModel.getSelectedRows();
	smEntry.deleteDiscoEntries(selected);

        try {
            model.setEntityDiscoEntry(univId, smEntry);

            if (selected.length == 1) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "discovery.service.table.entry.deleted");
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "discovery.service.table.entry.deleted.pural");
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        forwardTo();
    }

    protected String getBreadCrumbDisplayName() {
	EntityResourceOfferingModel model =
	    (EntityResourceOfferingModel)getModel();
        String[] arg = {model.getLocalizedServiceName(
	    AMAdminConstants.DISCOVERY_SERVICE)};
        return MessageFormat.format(model.getLocalizedString(
            "breadcrumbs.idm.services.editService"), arg);
    }

    protected boolean startPageTrail() {
        return false;
    }
}
