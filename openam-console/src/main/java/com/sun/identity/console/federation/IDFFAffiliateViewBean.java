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
 * $Id: IDFFAffiliateViewBean.java,v 1.4 2008/06/25 05:49:36 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.federation.model.IDFFModel;
import com.sun.identity.console.federation.model.IDFFModelImpl;
import com.sun.identity.federation.common.IFSConstants;
import com.sun.web.ui.model.CCAddRemoveModel;
import com.sun.web.ui.view.addremove.CCAddRemove;
import com.sun.web.ui.view.alert.CCAlert;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class IDFFAffiliateViewBean
    extends IDFFViewBeanBase 
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/federation/IDFFAffiliate.jsp";
    private CCAddRemoveModel affiliateMembersModel;
    
    public IDFFAffiliateViewBean() {
        super("IDFFAffiliate");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
    
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException 
    {
        super.beginDisplay(event);
        IDFFModel model =
            (IDFFModel)getModelInternal();       
        String[] args = { entityName };
        String pageTitle = MessageFormat.format(
            model.getLocalizedString(
            "idff.page.title.entityDescriptors.Affiliate"), args);
        setPageTitle(pageTitle);
        psModel.setValue(IDFFModel.ATTR_PROVIDER_TYPE,
            (String)getPageSessionAttribute(ENTITY_LOCATION));
        populateValue(entityName);
    }
        
    private void populateValue(String name) {
        IDFFModel model =
            (IDFFModel)getModelInternal();
        
        try {
            Map values = model.getAffiliateProfileAttributeValues(
                realm, name);
            
            Set availableEntities = model.getAllEntityDescriptorNames(
                realm);
            Set affliliatEntities = model.getAllAffiliateEntityDescriptorNames(
                realm);
            Set affiliateMembers = model.getAllAffiliateMembers(
                realm, name);
            availableEntities.removeAll(affiliateMembers);
            availableEntities.removeAll(affliliatEntities);
            
            if(affiliateMembersModel == null){
                affiliateMembersModel =  new CCAddRemoveModel();
            }
            if ((availableEntities != null) && !availableEntities.isEmpty()) {
                affiliateMembersModel.setAvailableOptionList(
                    createOptionList(availableEntities));
            }
            
            if ((affiliateMembers != null) && !affiliateMembers.isEmpty()) {
                affiliateMembersModel.setSelectedOptionList(
                    createOptionList(affiliateMembers));
            }
            
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTES);
            ps.setAttributeValues(values, model);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }
    
    protected AMModel getModelInternal() {
        HttpServletRequest req = getRequestContext().getRequest();
        return new IDFFModelImpl(req, getPageSessionAttributes());
    }
    
    protected void createPropertyModel() {       
        psModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/propertyIDFFAffiliate.xml"));
        psModel.clear();
        
        if(affiliateMembersModel == null){
            affiliateMembersModel =  new CCAddRemoveModel();
        }
        psModel.setModel("arlistAffiliateMembers",
            affiliateMembersModel);
    }
    
    /**
     * Handles save
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException 
    {
        retrieveCommonProperties();
        IDFFModel model = (IDFFModel)getModel();
        
        try{
            // get affiliate members
            CCAddRemove addRemoveList =
                (CCAddRemove)getChild(model.ATTR_AFFILIATE_MEMBERS);
            addRemoveList.restoreStateData();
            CCAddRemoveModel addRemoveModel =
                (CCAddRemoveModel)addRemoveList.getModel();
            Set members = new HashSet(getSelectedValues(addRemoveModel));
            
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTES);
            Map orig =  model.getAffiliateProfileAttributeValues(
                realm, entityName);
            Map values = ps.getAttributeValues(orig, false, model);
            model.updateAffiliateProfile(
                realm, entityName, values, members);
            
            setInlineAlertMessage(CCAlert.TYPE_INFO,
                "message.information","idff.entityDescriptor.Affiliate.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            debug.warning("IDFFAffiliateViewBean.handleButton1Request", e);
        }
        forwardTo();
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
}
