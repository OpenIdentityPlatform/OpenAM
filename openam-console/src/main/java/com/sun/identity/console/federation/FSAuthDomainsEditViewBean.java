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
 * $Id: FSAuthDomainsEditViewBean.java,v 1.3 2008/06/25 05:49:34 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.NavigationException;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.federation.model.FSAuthDomainsModel;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.web.ui.view.addremove.CCAddRemove;
import com.sun.web.ui.model.CCAddRemoveModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCTextField;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FSAuthDomainsEditViewBean
    extends FSAuthDomainsOpViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
	"/console/federation/FSAuthDomainsEditViewBean.jsp";
    private static final String PGTITLE_THREE_BTNS = "pgtitleThreeBtns";
    private static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    private AMPropertySheetModel propertySheetModel;        
    private static final String ADD_REMOVE_PROVIDERS = "addRemoveProviders";
    CCAddRemoveModel addRemoveModel;

    /**
     * Creates a authentication domains creation view bean.
     */
    public FSAuthDomainsEditViewBean() {
	super("FSAuthDomainsEdit");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    protected void initialize() {
	if (!initialized) {
	    super.initialize();
	    initialized = true;
	    createPageTitleModel();	  
	    createPropertyModel();
	    registerChildren();
	}
    }

    public void forwardTo(RequestContext reqContext)
	throws NavigationException 
    {
	String name = (String)getPageSessionAttribute(
	    FSAuthDomainsModel.TF_NAME);        
	if (name != null) {
	    setDisplayFieldValue(FSAuthDomainsModel.TF_NAME, name);
	}
	super.forwardTo(reqContext);
    }

    protected void registerChildren() {        
	super.registerChildren();
	registerChild(FSAuthDomainsModel.TF_NAME, CCTextField.class);
	registerChild(PGTITLE_THREE_BTNS, CCPageTitle.class);	
	registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
	propertySheetModel.registerChildren(this);	
    }

    protected View createChild(String name) {
	View view = null;
        if (name.equals(PGTITLE_THREE_BTNS)) {
	    view = new CCPageTitle(this, ptModel, name);
	} else if (name.equals(PROPERTY_ATTRIBUTE)) {
	    view = new AMPropertySheet(this, propertySheetModel, name);	
	} else if (propertySheetModel.isChildSupported(name)) {
	    view = propertySheetModel.createChild(this, name, getModel());
	} else {
	    view = super.createChild(name);
	}
	return view;
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException
    {
	super.beginDisplay(event);
	String name = (String)getDisplayFieldValue(FSAuthDomainsModel.TF_NAME);
	setPageSessionAttribute(FSAuthDomainsModel.TF_NAME, name);
	FSAuthDomainsModel model = (FSAuthDomainsModel)getModel();             
	try {
            String realm = model.getRealm(name);            
	    Map values = model.getAttributeValues(realm, name);      
	    AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
	    ps.setAttributeValues(values, model);	    	 
            
            if(addRemoveModel==null){
                addRemoveModel =  new CCAddRemoveModel();       
            }
	    Set providers = model.getAllProviderNames(realm);                           
            List selectedProviders = 
                    new ArrayList(model.getTrustedProviderNames(realm,name));                                    
            if ((selectedProviders != null) || 
                (!selectedProviders.isEmpty()))
            {                
                providers.removeAll(selectedProviders);
                Map displayNames = 
		    FSAuthDomainsOpViewBeanBase.getProviderDisplayNames(
			model, selectedProviders);                               
                addRemoveModel.setSelectedOptionList(
		    createOptionList(replacePipeWithComma(displayNames)));
            }           	   
	    Map displayNames =
		FSAuthDomainsOpViewBeanBase.getProviderDisplayNames(
		    model, providers);            
	    addRemoveModel.setAvailableOptionList(
		createOptionList(replacePipeWithComma(displayNames)));          
            propertySheetModel.setModel(ADD_REMOVE_PROVIDERS, addRemoveModel);            
	} catch (AMConsoleException e) {
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		e.getMessage());
	}	
    }

    private void createPageTitleModel() {
	ptModel = new CCPageTitleModel(
	    getClass().getClassLoader().getResourceAsStream(
		"com/sun/identity/console/threeBtnsPageTitle.xml"));
	ptModel.setValue("button1", "button.save");
	ptModel.setValue("button2", "button.reset");
	ptModel.setValue("button3", "button.back");
    }

    private void createPropertyModel() {
	DelegationConfig dConfig = DelegationConfig.getInstance();
	String startDN = AMModelBase.getStartDN(
	    getRequestContext().getRequest());
	boolean canModify = dConfig.hasPermission(startDN, null,
	    AMAdminConstants.PERMISSION_MODIFY,
	    getRequestContext().getRequest(), getClass().getName());
        
        // TBD: need to have one propertysheet which is read_only
	String xmlFile = (canModify) ?
	    "com/sun/identity/console/propertyCOTEdit.xml" :
	    "com/sun/identity/console/propertyCOTEdit.xml";
	propertySheetModel = new AMPropertySheetModel(
	    getClass().getClassLoader().getResourceAsStream(xmlFile));
        
        if(addRemoveModel==null)
                addRemoveModel =  new CCAddRemoveModel();
        propertySheetModel.setModel(ADD_REMOVE_PROVIDERS, addRemoveModel);
	propertySheetModel.clear();
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
     * Handles create authentication domains view bean.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
	throws ModelControlException
    {
	FSAuthDomainsModel model = (FSAuthDomainsModel)getModel();
	String name = (String)getDisplayFieldValue(model.TF_NAME);
	AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);      
	try {
            String realm = model.getRealm(name); 
            Map values = 
                ps.getAttributeValues(model.getDataMap(), false,model);             
	    model.setAttributeValues(realm, name, values);           
	    SerializedField szCache = (SerializedField)getChild(SZ_CACHE);           
            FederationViewBean vb = 
                (FederationViewBean) getViewBean(FederationViewBean.class);
            
            CCAddRemove addRemoveList = 
                (CCAddRemove)getChild(ADD_REMOVE_PROVIDERS);
            addRemoveList.restoreStateData();
            CCAddRemoveModel addRemoveModel =
                (CCAddRemoveModel)addRemoveList.getModel();
            List list = new ArrayList(getSelectedValues(addRemoveModel));                       
            model.addProviders(realm,name,list);               
	    setInlineAlertMessage(
                CCAlert.TYPE_INFO, 
                "message.information",
		"authentication.domain.updated");                        
            forwardTo();	   
	} catch (AMConsoleException e) {                      
	    setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
		e.getMessage());
            forwardTo();
	}	
    }

    protected String getBreadCrumbDisplayName() {
	String name = (String)getPageSessionAttribute(
	    FSAuthDomainsModel.TF_NAME);
	String[] arg = {name};
	FSAuthDomainsModel model = (FSAuthDomainsModel)getModel();
	return MessageFormat.format(model.getLocalizedString(
	    "breadcrumbs.federation.authdomains.edit"), arg);
    }

    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event) {
        forwardToAuthDomainView(event);
    }

    protected boolean startPageTrail() {
	return false;
    }
    
    private Set getSelectedValues(CCAddRemoveModel addRemoveModel) {
        Set results = null;
        Set selected = getValues(addRemoveModel.getSelectedOptionList());
        if ((selected != null) && !selected.isEmpty()) {
            results = new HashSet(selected.size() *2);
            for (Iterator iter = selected.iterator(); iter.hasNext(); ) {
                String n = (String)iter.next();
                results.add(n.replace(',', '|'));
            }
        }
        return (results == null) ? Collections.EMPTY_SET : results;
    }
     /* TBD: need to move this function to a common location.
     * HACK. Introduce this hack because Lockhart use | as delimiter for
     * selected value in a add remove list. http://xxx.com|IDP liked entry
     * will be broken down into two entries insteads on one.
     * This hack here is to replace | with comma.
     */
    private Map replacePipeWithComma(Map map) {
	if ((map != null) && !map.isEmpty()) {
	    Map altered = new HashMap(map.size() *2);
	    for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
		String key = (String)i.next();
		altered.put(key.replace('|', ','), map.get(key));
	    }                        
	    return altered;
	} else {
	    return map;
	}
    }
}
