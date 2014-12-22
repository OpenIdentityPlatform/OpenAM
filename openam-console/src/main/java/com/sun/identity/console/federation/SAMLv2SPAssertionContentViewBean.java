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
 * $Id: SAMLv2SPAssertionContentViewBean.java,v 1.6 2008/12/11 18:52:06 babysunil Exp $
 *
 * Portions Copyrighted 2014 ForgeRock AS.
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.ContainerView;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.View;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMTableTiledView;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.federation.model.SAMLv2Model;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.table.CCActionTable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SAMLv2SPAssertionContentViewBean extends SAMLv2Base {
    public static final String DEFAULT_DISPLAY_URL =
            "/console/federation/SAMLv2SPAssertionContent.jsp";

    protected CCActionTableModel tblAuthContextsModel;

    public SAMLv2SPAssertionContentViewBean() {
        super("SAMLv2SPAssertionContent");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
   
    protected void registerChildren() {
	super.registerChildren();
        if(isHosted()){
	    registerChild(CHILD_AUTH_CONTEXT_TILED_VIEW, 
                AMTableTiledView.class);
        }
    }
    
    protected View createChild(String name) {
        View view = null;
        if (isHosted() && (name.equals(CHILD_AUTH_CONTEXT_TILED_VIEW))) {
            view = new AMTableTiledView(this, tblAuthContextsModel, name);
        } else if (isHosted() && (name.equals(TBL_AUTHENTICATION_CONTEXTS))) {
            CCActionTable child = new CCActionTable(
                this, tblAuthContextsModel, name);
            child.setTiledView((ContainerView)getChild(
                CHILD_AUTH_CONTEXT_TILED_VIEW));
            view = child;
        } else {
            view = super.createChild(name);
        }
        return view;
    }
        
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        super.beginDisplay(event);
        AMPropertySheet ps = (AMPropertySheet) getChild(PROPERTIES);
        ps.init();
        SAMLv2Model model = (SAMLv2Model)getModel();
        ps.setAttributeValues(getStandardValues(), model);
        ps.setAttributeValues(getExtendedValues(), model);
                
         if (isHosted()) {
            SAMLv2AuthContexts authContexts = null;
            try {
                authContexts = model.getSPAuthenticationContexts(
                    realm,
                    entityName);                
            } catch (AMConsoleException e){               
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }            
            populateAuthenticationContext(authContexts, tblAuthContextsModel,
                    SAMLv2Model.SP_AUTHN_CONTEXT_CLASS_REF_MAPPING_DEFAULT);
        }
    }
    
    protected void createPropertyModel() {
        retrieveCommonProperties();
        if (isHosted()) {
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertySAMLv2SPAssertionContentHosted.xml"));
            createAuthContextsModel();
            psModel.setModel(TBL_AUTHENTICATION_CONTEXTS,
                tblAuthContextsModel);
        } else {
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertySAMLv2SPAssertionContentRemote.xml"));
        }
        psModel.clear();
    }
    
    private void createAuthContextsModel() {
        tblAuthContextsModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblSAMLv2SPAuthenticationContext.xml"));
        tblAuthContextsModel.setTitleLabel("label.items");
        tblAuthContextsModel.setActionValue(TBL_COL_SUPPORTED,
            "samlv2.sp.authenticationContext.table.name.supported.name");
        tblAuthContextsModel.setActionValue(TBL_COL_CONTEXT_REFERENCE,
            "samlv2.sp.authenticationContext.table.name.contextReference.name");
        tblAuthContextsModel.setActionValue(TBL_COL_LEVEL,
            "samlv2.sp.authenticationContext.table.name.level.name");
    }
    
    private SAMLv2AuthContexts getAuthenticationContexts()
        throws ModelControlException 
    {
        CCActionTable tbl = (CCActionTable)getChild(
            TBL_AUTHENTICATION_CONTEXTS);
        tbl.restoreStateData();
                
        SAMLv2AuthContexts authContexts = new SAMLv2AuthContexts();      
        String defaultAuthnContext = 
            (String)getDisplayFieldValue(SAMLv2Model.SP_AUTHN_CONTEXT_CLASS_REF_MAPPING_DEFAULT);
        for (int i = 0; i < tblAuthContextsModel.getSize(); i++) {
            tblAuthContextsModel.setLocation(i);
            String name = (String)tblAuthContextsModel.getValue(
                TBL_DATA_CONTEXT_REFERENCE);
            String supported = (String)tblAuthContextsModel.getValue(
                TBL_DATA_SUPPORTED);
            String level = (String)tblAuthContextsModel.getValue(
                TBL_DATA_LEVEL);            
            boolean isDefault = false;
            if(name.equals(defaultAuthnContext)){
                isDefault = true;               
                supported = "true";
            }                       
            authContexts.put(name, supported, level, isDefault);
        }          
        return authContexts;
    }
    
    public void handleButton1Request(RequestInvocationEvent event)
    throws ModelControlException {
        try {
            
            SAMLv2Model model = (SAMLv2Model)getModel();
            AMPropertySheet ps =
                    (AMPropertySheet)getChild(PROPERTY_ATTRIBUTES);
            
            //retrieve the standard metadata values from the property sheet
            Map spStdValues = ps.getAttributeValues(
                    model.getStandardServiceProviderAttributes(
                    realm, entityName), false, model);
            
            //retrieve the extended metadata values from the property sheet
            Map spExtValues = getExtendedValues();
            Map new_spExtValues = ps.getAttributeValues(
                    model.getSPEXACDataMap(), false, model);
            spExtValues.putAll(new_spExtValues);
 
            //save the standard metadata values for the Idp
            List acsList = new ArrayList();
            model.setSPStdAttributeValues(realm, entityName, spStdValues,
                    acsList);            
            
            //save the extended metadata values for the Idp
            model.setSPExtAttributeValues(realm, entityName, spExtValues,
                    location);
            
            if (isHosted()) {                               
                // update Authentication Contexts
                model.updateSPAuthenticationContexts(realm, entityName, 
                        getAuthenticationContexts());
                
                //save the encryption and signing info
                 model.updateKeyinfo(realm, entityName, spExtValues, 
                      spStdValues, false); 
            }
            
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "samlv2.sp.property.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
        forwardTo();
    }
    
    private Map getStandardValues() {
        Map map = new HashMap();
        try {
            
            //gets standard metadata values
            SAMLv2Model model = (SAMLv2Model)getModel();
            map = model.getStandardServiceProviderAttributes(
                    realm, entityName);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage() );
        }
        return map;
    }
    
    private Map getExtendedValues() {
        Map extendedValues = new HashMap();
        try {
            
            //gets extended metadata values
            SAMLv2Model model = (SAMLv2Model)getModel();
            Map attrs = model.getExtendedServiceProviderAttributes(
                    realm, entityName);
            Set entries = attrs.entrySet();
            Iterator iterator = entries.iterator();
            
            //the list of values is converted to a set
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry)iterator.next();
                extendedValues.put(
                        (String)entry.getKey(),
                        convertListToSet((List)entry.getValue()) );
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage() );
        }
        return extendedValues;
    }
}
