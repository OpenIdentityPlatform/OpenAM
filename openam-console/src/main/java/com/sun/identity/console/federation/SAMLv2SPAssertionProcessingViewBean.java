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
 * $Id: SAMLv2SPAssertionProcessingViewBean.java,v 1.3 2008/07/30 21:45:03 babysunil Exp $
 *
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
import com.sun.identity.console.federation.model.SAMLv2Model;
import com.sun.identity.console.federation.SAMLv2AuthContexts;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.table.CCActionTable;
import com.sun.identity.console.base.model.AMConsoleException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.List;

public class SAMLv2SPAssertionProcessingViewBean extends SAMLv2Base {
    public static final String DEFAULT_DISPLAY_URL =
            "/console/federation/SAMLv2SPAssertionProcessing.jsp";
    
    public SAMLv2SPAssertionProcessingViewBean() {
        super("SAMLv2SPAssertionProcessing");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
    
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        super.beginDisplay(event);
        AMPropertySheet ps = (AMPropertySheet) getChild(PROPERTIES);
        ps.init();
        SAMLv2Model model = (SAMLv2Model)getModel();
        //no std attributes
        
        ps.setAttributeValues(getExtendedValues(), model);
    }
    
    protected void createPropertyModel() {
        retrieveCommonProperties();
        if (isHosted()) {
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertySAMLv2SPAssertionProcessingHosted.xml"));
        } else {
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertySAMLv2SPAssertionProcessingRemote.xml"));
        }
        psModel.clear();
    }
    
    public void handleButton1Request(RequestInvocationEvent event)
    throws ModelControlException {
        try {
            
            SAMLv2Model model = (SAMLv2Model)getModel();
            AMPropertySheet ps =
                    (AMPropertySheet)getChild(PROPERTY_ATTRIBUTES);
            
            //retrieve the extended metadata values from the property sheet
            Map spExtValues = getExtendedValues();
            Map new_spExtValues = ps.getAttributeValues(
                    model.getSPEXAPDataMap(), false, model);
            spExtValues.putAll(new_spExtValues);
 
            //save the extended metadata values for the Idp
            model.setSPExtAttributeValues(realm, entityName, spExtValues,
                    location);
            
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "samlv2.sp.property.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
        forwardTo();
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
