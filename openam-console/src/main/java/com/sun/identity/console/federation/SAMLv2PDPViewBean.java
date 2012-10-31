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
 * $Id: SAMLv2PDPViewBean.java,v 1.4 2008/10/24 00:12:02 asyhuang Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.federation.model.SAMLv2Model;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.Map;

public class SAMLv2PDPViewBean extends SAMLv2Base {
    
    public static final String DEFAULT_DISPLAY_URL =
	"/console/federation/SAMLv2PDP.jsp";

    public SAMLv2PDPViewBean() {
	super("SAMLv2PDP");
	setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }

    public void beginDisplay(DisplayEvent event)
	throws ModelControlException
    {
	super.beginDisplay(event);    
        SAMLv2Model model =
            (SAMLv2Model)getModelInternal();        
        populateValue();              
    }
                
    private void populateValue() {      
        SAMLv2Model model =
            (SAMLv2Model)getModelInternal();   
        try {           
            Map values = model.getPDPDescriptor(realm, entityName);                 
            values.putAll(model.getPDPConfig(realm, entityName, location));
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTES);
            ps.setAttributeValues(values, model);      
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    } 
    
    protected void createPropertyModel() {
        retrieveCommonProperties();
	if (isHosted()) {
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                    "com/sun/identity/console/propertySAMLv2PDPHosted.xml"));   
        } else {    
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                    "com/sun/identity/console/propertySAMLv2PDPRemote.xml"));
        }
	psModel.clear();
    }
    
    public void handleButton1Request(RequestInvocationEvent event)
	throws ModelControlException
    {                    
        retrieveCommonProperties();               
        try {
            SAMLv2Model model = 
                (SAMLv2Model)getModel();
            AMPropertySheet ps = 
                (AMPropertySheet)getChild(PROPERTY_ATTRIBUTES);
            
            // update standard metadata
            Map origStdMeta =  
                model.getPDPDescriptor(realm, entityName);
            Map stdValues = ps.getAttributeValues(origStdMeta, false, model);
            model.updatePDPDescriptor(realm, entityName, stdValues);
            
            //update extended metadata
            Map origExtMeta = 
                model.getPDPConfig(realm, entityName, location);
            Map extValues = ps.getAttributeValues(    
                    model.getXacmlPDPExtendedMetaMap(), 
                    false, 
                    model);
            origExtMeta.putAll(extValues);
            model.updatePDPConfig(realm, entityName, location, origExtMeta);
            
            setInlineAlertMessage(CCAlert.TYPE_INFO,
                "message.information",
                "samlv2.entityDescriptor.provider.pdp.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        } 
        forwardTo();
    }
}
