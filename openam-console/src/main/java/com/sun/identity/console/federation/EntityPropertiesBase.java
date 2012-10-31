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
 * $Id: EntityPropertiesBase.java,v 1.4 2008/06/25 05:49:34 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;

import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.AMViewConfig;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMSystemConfig;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMModelBase;

import com.sun.identity.console.federation.model.EntityModel;
import com.sun.identity.console.federation.model.EntityModelImpl;

import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.tabs.CCTabs;
import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

/** 
 * This is the base for all of the entity propery views. It will build the
 * tab menu based on the configurations (roles) supported by the selected
 * entity type. Extender must implement the following methods:
 *     
 *   protected abstract String getProfileName();    
 *   protected abstract void createPropertyModel(String name);
 * 
 * The value returned from getProfileName() must be the beginning of the name 
 * of the class viewbean being invoked. For example, for a SAMLv2 protocol 
 * entity, getProfileName() will return "SAMLv2" which will then be converted
 * to SAMLv2GeneralViewBean, SAMLv2General.jsp, etc...
 */
public abstract class EntityPropertiesBase 
    extends AMPrimaryMastHeadViewBean
{    
    public String entityName;
    public String realm;
    public String location;
    
    protected CCPageTitleModel ptModel = null;   
    protected AMPropertySheetModel psModel = null;    
    private static final String PAGE_TITLE = "pgtitle";
    public static final String PROPERTY_ATTRIBUTES = "propertyAttributes";
    private boolean submitCycle;
    private boolean initialized = false;
    
    public static final String ENTITY_NAME = "entityName";
    public static final String ENTITY_REALM = "entityRealm";
    public static final String ENTITY_LOCATION = "entityLocation";
    public static final String HOSTED = "hosted";

    public EntityPropertiesBase(String name) {
	super(name);
    }
    
    public void beginDisplay(DisplayEvent event)
	throws ModelControlException
    {
	super.beginDisplay(event);       
        retrieveCommonProperties();       
        setPageTitle(entityName);    
    }
   
    protected void registerChildren() {
	registerChild(PROPERTY_ATTRIBUTES, AMPropertySheet.class);
	psModel.registerChildren(this);
	ptModel.registerChildren(this);	
        registerChild(TAB_COMMON, CCTabs.class);
	super.registerChildren();
    }

    protected View createChild(String name) {
	View view = null;

	if (name.equals(PAGE_TITLE)) {
	    view = new CCPageTitle(this, ptModel, name);
	} else if (name.equals(PROPERTY_ATTRIBUTES)) {
	     view = new AMPropertySheet(this, psModel, name);
	} else if (psModel.isChildSupported(name)) {
	    view = psModel.createChild(this, name);
	} else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name); 
	} else {
	    view = super.createChild(name);
	}

	return view;
    }
 
    protected void initialize() {
	if (!initialized) {
            // get the type of entity selected, and name
	    String name = (String)getPageSessionAttribute(ENTITY_NAME);

	    if (name != null) {
		super.initialize();
		initialized = true;
		createPageTitleModel();
		createPropertyModel();
		registerChildren();
	    }
	}
    }
    
    protected void setPageTitle(String title) {
        ptModel.setPageTitleText(title);
    }
  
    protected void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", "button.back");
    }

    protected void createTabModel() {
        String profile = getProfileName();
        String entity = (String)getPageSessionAttribute(ENTITY_NAME);
        String realm = (String)getPageSessionAttribute("entityRealm");
        EntityModel eModel = (EntityModel)getEntityModel();
        
        if (tabModel == null) {
            if (profile.equals(eModel.SAMLV2)) {
                AMViewConfig amconfig = AMViewConfig.getInstance();
                List tabstoDisplay = eModel.getSAMLv2Roles(entity, realm);
                
                tabModel = amconfig.getSAMLv2TabsModel("SAMLv2", "/",
                        getRequestContext().getRequest(), tabstoDisplay);
                registerChild(TAB_COMMON, CCTabs.class);
                
                tabModel.clear();
            } else if (!profile.equals(eModel.SAMLV2)) {
                AMViewConfig amconfig = AMViewConfig.getInstance();
                List entries = eModel.getTabMenu(profile, entity, realm);
                if ((entries != null) && (entries.size() > 0)) {
                    amconfig.addTabEntries(profile, entries, true);
                }
                tabModel = amconfig.getTabsModel(
                        profile, "/", getRequestContext().getRequest());
                tabModel.clear();
                tabModel.setSelectedNode(1);
            }
        }
    }
    
    protected AMModel getEntityModel() {
        RequestContext rc = RequestManager.getRequestContext();
        EntityModel model = new EntityModelImpl(
            rc.getRequest(), getPageSessionAttributes());
        
        return model;
    } 

 /************************************************************************
  * 
  * Event Handlers for the following events: 
  * tab selection, save button, reset button, back button.
  *
  ************************************************************************/
    
    public void nodeClicked(RequestInvocationEvent event, int nodeID) {
        try {
            // get the entity tab that was selected and forward the 
            // request to that vb
            
	    AMViewBeanBase vb = getTabNodeAssociatedViewBean(
		getProfileName(), nodeID);
            String tmp = (String)getPageSessionAttribute(
                AMAdminConstants.PREVIOUS_TAB_ID);
            vb.setPageSessionAttribute(AMAdminConstants.PREVIOUS_TAB_ID, tmp);
	    passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            forwardTo();
        }
    }

    public void handleButton1Request(RequestInvocationEvent event)
	throws ModelControlException
    {
            forwardTo();
    }

    public void handleButton2Request(RequestInvocationEvent event) {
        forwardTo();
    }

    public void handleButton3Request(RequestInvocationEvent event) {        
        // reset the select tab to be the federation view
        String tmp = 
            (String)getPageSessionAttribute(AMAdminConstants.PREVIOUS_TAB_ID);
        setPageSessionAttribute(getTrackingTabIDName(), tmp);       
        
        FederationViewBean vb = (FederationViewBean)
            getViewBean(FederationViewBean.class);
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * This will pull the following properties from the page session:
     *     entity name
     *     name of the realm where the entity exists
     *     location (either hosted or remote)
     */
    public void retrieveCommonProperties() {
        entityName = (String)getPageSessionAttribute(ENTITY_NAME);        
        realm = (String)getPageSessionAttribute(ENTITY_REALM);            
        location = (String)getPageSessionAttribute(ENTITY_LOCATION);
    }
    
    public boolean isHosted() {
        return (location != null && location.equals(HOSTED)) ? true : false; 
    }
    
    protected abstract String getProfileName();    
    protected abstract void createPropertyModel();
}
