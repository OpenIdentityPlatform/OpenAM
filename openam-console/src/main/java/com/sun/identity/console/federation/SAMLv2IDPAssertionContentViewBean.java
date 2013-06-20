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
 * $Id: SAMLv2IDPAssertionContentViewBean.java,v 1.5 2008/09/25 01:52:20 babysunil Exp $
 *
 */

/**
 * Portions Copyrighted 2013 ForgeRock, Inc.
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.ContainerView;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.View;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMTableTiledView;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.security.EncodeAction;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.federation.model.SAMLv2Model;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.view.table.CCActionTable;

import java.security.AccessController;
import java.util.*;

public class SAMLv2IDPAssertionContentViewBean extends SAMLv2Base {
    public static final String DEFAULT_DISPLAY_URL =
            "/console/federation/SAMLv2IDPAssertionContent.jsp";
        
    public static final String CHILD_AUTH_CONTEXT_TILED_VIEW = "tableTiledView";
    public static final String TBL_AUTHENTICATION_CONTEXTS =
        "tblAuthenticationContext";
    public static final String TBL_COL_SUPPORTED = "tblColSupported";
    public static final String TBL_DATA_SUPPORTED = "tblDataSupported";
    public static final String TBL_COL_CONTEXT_REFERENCE =
        "tblColContextReference";
    public static final String TBL_DATA_CONTEXT_REFERENCE =
        "tblDataContextReference";
    public static final String TBL_DATA_LABEL = "tblDataLabel";
    public static final String TBL_COL_KEY = "tblColKey";
    public static final String TBL_DATA_KEY = "tblDataKey";
    public static final String TBL_COL_VALUE = "tblColValue";
    public static final String TBL_DATA_VALUE = "tblDataValue";
    public static final String TBL_COL_LEVEL = "tblColLevel";
    public static final String TBL_DATA_LEVEL = "tblDataLevel";
    
    protected CCActionTableModel tblAuthContextsModel;
    
    public SAMLv2IDPAssertionContentViewBean() {
        super("SAMLv2IDPAssertionContent");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
        
    protected void registerChildren() {
	super.registerChildren();
        if (isHosted()) {
	    registerChild(CHILD_AUTH_CONTEXT_TILED_VIEW, 
                AMTableTiledView.class);
        }
    }
    
    protected View createChild(String name) {
        View view = null;
        if ( isHosted() && (name.equals(CHILD_AUTH_CONTEXT_TILED_VIEW))) {
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
                authContexts = model.getIDPAuthenticationContexts(
                    realm,
                    entityName);               
            } catch (AMConsoleException e){                
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }           
            populateAuthenticationContext(authContexts);
        }
    }
    
    private void populateAuthenticationContext(SAMLv2AuthContexts authContexts) {
        
        List names = AUTH_CONTEXT_REF_NAMES;
        // We know that names from model contains 25 elements
        int sz = names.size();
        tblAuthContextsModel.clear();
        for (int i = 0; i < sz; i++) {
            String name = (String)names.get(i);
            populateAuthenticationContext(name, authContexts, i);            
        }
    }
    
    private void populateAuthenticationContext(
        String name,
        SAMLv2AuthContexts authContexts,
        int index
        ) {
        if (index != 0) {
            tblAuthContextsModel.appendRow();
        }
        
        SAMLv2Model model =
            (SAMLv2Model)getModelInternal();
        tblAuthContextsModel.setValue(TBL_DATA_CONTEXT_REFERENCE, name);
        tblAuthContextsModel.setValue(TBL_DATA_LABEL,
            model.getLocalizedString(getAuthContextI18nKey(name)));
        
        SAMLv2AuthContexts.SAMLv2AuthContext authContextObj = null;
        if (authContexts != null) {
            authContextObj = authContexts.get(name);
        }
        
        if (authContextObj == null) {
            tblAuthContextsModel.setValue(TBL_DATA_LEVEL, "0");
            tblAuthContextsModel.setValue(TBL_DATA_KEY, "none");
            tblAuthContextsModel.setValue(TBL_DATA_SUPPORTED, "");
            tblAuthContextsModel.setValue(TBL_DATA_VALUE, "");
        }else{
            tblAuthContextsModel.setValue(TBL_DATA_LEVEL, 
                authContextObj.level);
            tblAuthContextsModel.setValue(TBL_DATA_KEY, 
                authContextObj.key);
            tblAuthContextsModel.setValue(TBL_DATA_SUPPORTED, 
                authContextObj.supported);
            tblAuthContextsModel.setValue(TBL_DATA_VALUE, 
                authContextObj.value);
            if(authContextObj.isDefault){
                setDisplayFieldValue(
                    model.IDP_AUTHN_CONTEXT_CLASS_REF_MAPPING_DEFAULT, 
                    authContextObj.name);
            }
        }        
    }
    
    private String getAuthContextI18nKey(String name) {
        int idx = name.lastIndexOf(":");
        String key = (idx != -1) ? name.substring(idx+1) : name;
        return "samlv2.authenticationContext." + key + ".label";
    }
    
    protected void createPropertyModel() {
        retrieveCommonProperties();
        if (isHosted()) {
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertySAMLv2IDPAssertionContentHosted.xml"));
            createAuthContextsModel();
            psModel.setModel(TBL_AUTHENTICATION_CONTEXTS,
                 tblAuthContextsModel);

        } else {
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertySAMLv2IDPAssertionContentRemote.xml"));
        }
        psModel.clear();
    }
        
    private void createAuthContextsModel() {
        tblAuthContextsModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblSAMLv2IDPAuthenticationContext.xml"));
        tblAuthContextsModel.setTitleLabel("label.items");
        tblAuthContextsModel.setActionValue(TBL_COL_CONTEXT_REFERENCE,
            "samlv2.idp.authenticationContext.table.name.contextReference.name");
        tblAuthContextsModel.setActionValue(TBL_COL_SUPPORTED,
            "samlv2.idp.authenticationContext.table.name.supported.name");
        tblAuthContextsModel.setActionValue(TBL_COL_KEY,
            "samlv2.idp.authenticationContext.table.name.key.name");
        tblAuthContextsModel.setActionValue(TBL_COL_VALUE,
            "samlv2.idp.authenticationContext.table.name.value.name");
        tblAuthContextsModel.setActionValue(TBL_COL_LEVEL,
            "samlv2.idp.authenticationContext.table.name.level.name");
    }
    
    private SAMLv2AuthContexts getAuthenticationContexts()
        throws ModelControlException 
    {
        CCActionTable tbl = (CCActionTable)getChild(
            TBL_AUTHENTICATION_CONTEXTS);
        tbl.restoreStateData();        
                
        SAMLv2AuthContexts authContexts = new SAMLv2AuthContexts();                      
        String defaultAuthnContext = 
            (String)getDisplayFieldValue("idpDefaultAuthnContext");  
        for (int i = 0; i < AUTH_CONTEXT_REF_COUNT; i++) {           
            tblAuthContextsModel.setLocation(i);                       
            String name = (String)tblAuthContextsModel.getValue(
                TBL_DATA_CONTEXT_REFERENCE);
            String supported = (String)tblAuthContextsModel.getValue(
                TBL_DATA_SUPPORTED);
            String key = (String)tblAuthContextsModel.getValue(TBL_DATA_KEY);
            String value = (String)tblAuthContextsModel.getValue(
                TBL_DATA_VALUE);
            String level = (String)tblAuthContextsModel.getValue(
                TBL_DATA_LEVEL);                             
            boolean isDefault = false;
            if(name.equals(defaultAuthnContext)){
                isDefault = true;                
                supported = "true";                
            }            
            authContexts.put(name, supported, key, value, level, isDefault);
        }              
        return authContexts;
    }
    
    public void handleButton1Request(RequestInvocationEvent event)
    throws ModelControlException {
        try {
            SAMLv2Model model = (SAMLv2Model)getModel();
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTES);
            
            //retrieve the standard metadata values from the property sheet
            Map idpStdValues = ps.getAttributeValues(
                    model.getStandardIdentityProviderAttributes(realm, entityName), false, model);
            
            //retrieve the extended metadata values from the property sheet
            Map idpExtValues = getExtendedValues();
            Map new_idpExtValues = ps.getAttributeValues(model.getIDPEXACDataMap(), false, model);

            // Check if the signing keypass has been updated, if it hasn't then remove it from the update since
            // password fields are set to AMPropertySheetModel.passwordRandom before they are displayed to the user.
            if (new_idpExtValues.containsKey(SAMLv2Model.IDP_SIGN_CERT_KEYPASS)) {
                Set value = (Set)new_idpExtValues.get(SAMLv2Model.IDP_SIGN_CERT_KEYPASS);
                if (value != null && !value.isEmpty()) {
                    String keyPass = (String)value.iterator().next();
                    if (AMPropertySheetModel.passwordRandom.equals(keyPass)) {
                        // User did not change the password => remove fake value to avoid it overriding the stored value
                        new_idpExtValues.remove(SAMLv2Model.IDP_SIGN_CERT_KEYPASS);
                    } else {
                        // The value has been updated
                        Set<String> encodedValue = new HashSet<String>(1);
                        // If the value is blank, don't encode
                        if (keyPass.isEmpty()) {
                            encodedValue.add(keyPass);
                        } else {
                            //Since it is plain text we need to encrypt it before storing
                            encodedValue.add(AccessController.doPrivileged(new EncodeAction(keyPass)));
                        }
                        new_idpExtValues.put(SAMLv2Model.IDP_SIGN_CERT_KEYPASS, encodedValue);
                    }
                }
            }
            idpExtValues.putAll(new_idpExtValues);
 
            //save the standard metadata values for the Idp
            model.setIDPStdAttributeValues(realm, entityName, idpStdValues);
            
            //save the extended metadata values for the Idp
            model.setIDPExtAttributeValues(realm, entityName, idpExtValues, location);
           
           if (isHosted()) {
                //update Authentication Contexts
                model.updateIDPAuthenticationContexts(realm, entityName, getAuthenticationContexts());
                
                //save the encryption and signing info
                 model.updateKeyinfo(realm, entityName, idpExtValues, idpStdValues, true);
            }
                       
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information", "samlv2.idp.property.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", e.getMessage());
        }
        forwardTo();
    }
    
    private Map getStandardValues() {
        Map map = new HashMap();
        try {
            
            //gets standard metadata values
            SAMLv2Model model = (SAMLv2Model)getModel();
            map = model.getStandardIdentityProviderAttributes(
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
            Map attr = model.getExtendedIdentityProviderAttributes(
                    realm, entityName);
            Set entries = attr.entrySet();
            Iterator iterator = entries.iterator();
            
            //the list of values is converted to a set
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry)iterator.next();
                extendedValues.put((String)entry.getKey(),
                        convertListToSet((List)entry.getValue()) );
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage() );
        }
        return extendedValues;
    }

}

