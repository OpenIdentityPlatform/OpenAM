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
 * "Portions Copyrighted [year] [name of copyright owner]
 *
 * $Id: WSFedSPViewBean.java,v 1.6 2008/10/16 20:43:36 babysunil Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.federation.model.WSFedPropertiesModel;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class WSFedSPViewBean extends WSFedGeneralBase {
    public static final String DEFAULT_DISPLAY_URL =
            "/console/federation/WSFedSP.jsp";
    
    public WSFedSPViewBean() {
        super("WSFedSP");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
    
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        
        super.beginDisplay(event);
        WSFedPropertiesModel model = (WSFedPropertiesModel)getModel();
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTES);
        
        // set extended meta data values for the SP
        ps.setAttributeValues(getExtendedValues(), model);        
        setDisplayFieldValue(WSFedPropertiesModel.TFUSR_AGENT_NAME, "Key");
        setDisplayFieldValue(WSFedPropertiesModel.TFCOKKI_NAME, "Name");
    }
    
    protected void createPropertyModel() {
        retrieveCommonProperties();
        if (isHosted()) {
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                    "com/sun/identity/console/propertyWSFedSPViewHosted.xml"));
        } else {
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                    "com/sun/identity/console/propertyWSFedSPViewRemote.xml"));
        }
        psModel.clear();
    }
    
    public void handleButton1Request(RequestInvocationEvent event)
    throws ModelControlException {
        retrieveCommonProperties();
        try {
            WSFedPropertiesModel model = (WSFedPropertiesModel)getModel();
            AMPropertySheet ps =
                    (AMPropertySheet)getChild(PROPERTY_ATTRIBUTES);
            
            //retrieve all the extended metadata values from the property sheet
            Map spExtValues =
                ps.getAttributeValues(model.getSPEXDataMap(), false, model);
            
            // should retain the value of the displayname from General page
            Set entries = getExtendedValues().entrySet();
            Iterator iterator = entries.iterator();
             while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry)iterator.next();
                if (entry.getKey().equals("displayName")) {
                    spExtValues.put((String)entry.getKey(),
                        returnEmptySetIfValueIsNull((Set)entry.getValue()));
                }
            } 
            
            //save the extended metadata values for the SP
            model.setSPExtAttributeValues(realm, entityName, 
                                            spExtValues, location);
            
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "wsfed.sp.property.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
        forwardTo();
    }
    
    private Map getExtendedValues() {
        Map map = new HashMap();
        Map tmpMap = new HashMap();
        WSFedPropertiesModel model = (WSFedPropertiesModel)getModel();
        try {
            
            //gets extended metadata values
            map = model.getServiceProviderAttributes(realm, entityName);
            Set entries = map.entrySet();
            Iterator iterator = entries.iterator();
            
            //the list of values is converted to a set
            while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry)iterator.next();
                tmpMap.put((String)entry.getKey(),
                        returnEmptySetIfValueIsNull(
                        convertListToSet((List)entry.getValue())));
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage() );
        }
        return tmpMap;
    }
    
}
