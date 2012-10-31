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
 * $Id: CreateCOTViewBean.java,v 1.6 2009/08/21 20:09:23 veiming Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.html.OptionList;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMFormatUtils;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.federation.model.FSAuthDomainsModel;
import com.sun.identity.console.federation.model.FSAuthDomainsModelImpl;
import com.sun.identity.sm.SMSSchema;
import com.sun.web.ui.model.CCAddRemoveModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.addremove.CCAddRemove;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCDropDownMenu;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;

public class CreateCOTViewBean
        extends AMPrimaryMastHeadViewBean 
{
    public static final String DEFAULT_DISPLAY_URL = 
        "/console/federation/CreateCOT.jsp";    
    protected static final String PROPERTIES =
        "propertyAttributes";    
    private static final String PGTITLE_THREE_BTNS =
        "pgtitleThreeBtns";    
    protected static final String SINGLECHOICE_REALM_MENU = 
        "singleChoiceShowMenu"; 
    private static final String ADD_REMOVE_PROVIDERS = 
        "addRemoveProviders";    
    private CCPageTitleModel ptModel;
    private AMPropertySheetModel psModel;
    private OptionList optList;
    private boolean initialized;
    CCAddRemoveModel addRemoveModel;
    
    /**
     * Creates a authentication domains view bean.
     */
    public CreateCOTViewBean() {
        super("CreateCOT");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
    
    protected void initialize() {
        if (!initialized) {
            initialized = true;
            createPropertyModel();
            createPageTitleModel();
            registerChildren();
            super.initialize();
        }
        super.registerChildren();
    }
    
    protected void registerChildren() {
        super.registerChildren();
        registerChild(PROPERTIES, AMPropertySheet.class);
        registerChild(PGTITLE_THREE_BTNS, CCPageTitle.class);
        registerChild(SINGLECHOICE_REALM_MENU, CCDropDownMenu.class);
        psModel.registerChildren(this);
        ptModel.registerChildren(this);
    }
    
    protected View createChild(String name) {
        View view = null;
        if (name.equals(PGTITLE_THREE_BTNS)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (name.equals(PROPERTIES)) {
            view = new AMPropertySheet(this, psModel, name);
        } else if ((psModel != null) && psModel.isChildSupported(name)) {
            view = psModel.createChild(this, name, getModel());
        } else if ((ptModel != null) && ptModel.isChildSupported(name)) {
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
        AMPropertySheet ps = (AMPropertySheet) getChild(PROPERTIES);
        ps.init();
        
        populateRealmData();
        
        FSAuthDomainsModel model = (FSAuthDomainsModel) getModel();
        String realm = "/";
        try {
            if(addRemoveModel == null){
                addRemoveModel =  new CCAddRemoveModel();
            }
            Set providers = model.getAllProviderNames(realm);            
            Map displayNames =
                FSAuthDomainsOpViewBeanBase.getProviderDisplayNames(
                    model, providers);            
            addRemoveModel.setAvailableOptionList(
                createOptionList(
                    replacePipeWithComma(displayNames)));            
            psModel.setModel(ADD_REMOVE_PROVIDERS, addRemoveModel);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
    }
    
    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/twoBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.ok");
        ptModel.setValue("button2", "button.cancel");
    }
    
    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new FSAuthDomainsModelImpl(req, getPageSessionAttributes());
    }
    
    private void createPropertyModel() {
        psModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/createCOTPropertySheet.xml"));
        if(addRemoveModel == null){
            addRemoveModel =  new CCAddRemoveModel();
        }
        psModel.setModel(ADD_REMOVE_PROVIDERS, addRemoveModel);
        psModel.clear();
    }
    
    
    private void populateRealmData() {
        FSAuthDomainsModel model = (FSAuthDomainsModel)getModel();
        try{
            Set realmNames = new TreeSet(model.getRealmNames("/", "*"));
            CCDropDownMenu menu =
                (CCDropDownMenu)getChild(SINGLECHOICE_REALM_MENU);        
            OptionList list = new OptionList();
            for (Iterator i = realmNames.iterator(); i.hasNext();) {
                String name = (String)i.next();
                list.add(getPath(name), name);
            }
            menu.setOptions(list);         
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        } 
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
    
    /**
     * Handles save button request.
     * save
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException     
    {    
        FSAuthDomainsModel model = (FSAuthDomainsModel)getModel();
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTIES);        
        try {
            Map values =
                ps.getAttributeValues(model.getDataMap(), false,model);           
            
            String name = (String)getDisplayFieldValue(model.TF_NAME);
            if ((name == null) || (name.length() < 1)) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    model.getLocalizedString(
                        "authdomain.authentication.domain.name.missing.message"));
                psModel.setErrorProperty("nameProperty", true);
                forwardTo();
            } else {
                CCAddRemove addRemoveList =
                    (CCAddRemove)getChild(ADD_REMOVE_PROVIDERS);
                addRemoveList.restoreStateData();
                CCAddRemoveModel addRemoveModel =
                    (CCAddRemoveModel)addRemoveList.getModel();
                Set providers = new HashSet(getSelectedValues(addRemoveModel));            
                model.createAuthenticationDomain(values, providers);            
                Object[] params = {name};
                String message = MessageFormat.format(model.getLocalizedString(
                    "authentication.domain.create.message"), params);
                setPageSessionAttribute(
                    FederationViewBean.MESSAGE_TEXT, message);
                backTrail();                    
                FederationViewBean vb = (FederationViewBean)
                    getViewBean(FederationViewBean.class);
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            forwardTo();
        }
    }

    /**
     * Handles page cancel request.
     * 
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event) {
        FederationViewBean vb = (FederationViewBean)
            getViewBean(FederationViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
    
     /* 
      * TBD: need to move this function to a common location.
      * Introduce this workaround because Lockhart use | as delimiter for
      * selected value in a add remove list. http://xxx.com|IDP liked entry
      * will be broken down into two entries insteads on one.
      * This is to replace | with comma.
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
