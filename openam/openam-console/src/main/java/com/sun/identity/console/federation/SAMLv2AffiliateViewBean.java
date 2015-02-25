/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SAMLv2AffiliateViewBean.java,v 1.3 2008/08/12 17:15:21 babysunil Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.federation.model.SAMLv2Model;
import com.sun.web.ui.model.CCAddRemoveModel;
import com.sun.web.ui.view.addremove.CCAddRemove;
import com.sun.web.ui.view.alert.CCAlert;
import javax.servlet.http.HttpServletRequest;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collections;
import java.util.Set;
import java.util.Iterator;
import java.util.List;

public class SAMLv2AffiliateViewBean extends SAMLv2Base {
    public static final String DEFAULT_DISPLAY_URL =
            "/console/federation/SAMLv2Affiliate.jsp";
    private CCAddRemoveModel samladdRemoveModel;
    
    /** Creates a new instance of SAMLv2AffiliateViewBean */
    public SAMLv2AffiliateViewBean() {
        super("SAMLv2Affiliate");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
    
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        AMPropertySheet ps = (AMPropertySheet) getChild(PROPERTIES);
        ps.init();
        SAMLv2Model model = (SAMLv2Model)getModel();
        try {
            Map values = getStandardAffiliationValues();
            Set allSPEntities = model.getallSPEntities(realm);
            Set affiliateMembers = (Set)values.get(model.AFFILIATE_MEMBER);
            allSPEntities.removeAll(affiliateMembers);
            if(samladdRemoveModel == null){
                samladdRemoveModel =  new CCAddRemoveModel();
            }
            if ((allSPEntities != null) && !allSPEntities.isEmpty()) {
                samladdRemoveModel.setAvailableOptionList(
                        createOptionList(allSPEntities));
            }
            if ((affiliateMembers != null) && !affiliateMembers.isEmpty()) {
                samladdRemoveModel.setSelectedOptionList(
                        createOptionList(affiliateMembers));
            }
            ps.setAttributeValues(values, model);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
        ps.setAttributeValues(getExtendedAffiliationValues(), model);
    }
    
    protected void createPropertyModel() {
        SAMLv2Model model = (SAMLv2Model)getModel();
        retrieveCommonProperties();
        psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertySAMLv2Affiliation.xml"));
        psModel.clear();
        if(samladdRemoveModel == null){
            samladdRemoveModel =  new CCAddRemoveModel();
        }
        psModel.setModel(model.AFFILIATE_MEMBER, samladdRemoveModel); 
    }
    
    public void handleButton1Request(RequestInvocationEvent event)
    throws ModelControlException {
        try {
            SAMLv2Model model = (SAMLv2Model)getModel();
            CCAddRemove addRemoveList =
                (CCAddRemove)getChild(model.AFFILIATE_MEMBER);
            addRemoveList.restoreStateData();
            CCAddRemoveModel addRemoveModel =
                (CCAddRemoveModel)addRemoveList.getModel();
            Set members = new HashSet(getSelectedValues(addRemoveModel));
            if(members.isEmpty() || members == null) {
                throw new AMConsoleException(
                        model.getLocalizedString(
                        "samlv2.create.provider.missing.affiliation.members"));
            }
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTES);
            Map orig =  getStandardAffiliationValues();
            Map values = ps.getAttributeValues(orig, false, model);
            model.setStdAffilationValues(realm, entityName, values, members);
            
            //save for ext will be done once backend api is ready
            
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "samlv2.affiliation.property.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
        forwardTo();
    }
    
    private Map getStandardAffiliationValues() {
        Map map = new HashMap();
        try {
            SAMLv2Model model = (SAMLv2Model)getModel();
            map = model.getStandardAffiliationAttributes(
                    realm, entityName);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage() );
        }
        return map;
    }
    
    private Map getExtendedAffiliationValues() {
        Map extendedValues = new HashMap();
        try {
            SAMLv2Model model = (SAMLv2Model)getModel();
            Map attr = model.getExtendedAffiliationyAttributes(
                    realm, entityName);
            Set entries = attr.entrySet();
            Iterator iterator = entries.iterator();
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry)iterator.next();
                String tmp = (String)entry.getKey();
                extendedValues.put((String)entry.getKey(),
                        convertListToSet((List)entry.getValue()) );                
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage() );
        }
        return extendedValues;
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
