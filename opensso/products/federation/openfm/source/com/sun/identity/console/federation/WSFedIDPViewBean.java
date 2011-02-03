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
 * $Id: WSFedIDPViewBean.java,v 1.8 2008/10/16 20:43:00 babysunil Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.federation.model.WSFedPropertiesModel;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.identity.wsfederation.common.WSFederationConstants;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

public class WSFedIDPViewBean extends WSFedGeneralBase {
    public static final String DEFAULT_DISPLAY_URL =
            "/console/federation/WSFedIDP.jsp";
    private static final String strUPN = "User Principal Name";
    
    public WSFedIDPViewBean() {
        super("WSFedIDP");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
    
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        super.beginDisplay(event);
        WSFedPropertiesModel model = (WSFedPropertiesModel)getModel();
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTES);
        
        //sets the extended meta data values for the Idp
        ps.setAttributeValues(getExtendedValues(), model);
        
        //sets the claimtype display name
        setDisplayFieldValue(WSFedPropertiesModel.TFCLAIM_TYPES, 
                getStandardValues());       
    }
    
    protected void createPropertyModel() {
        retrieveCommonProperties();
        if (isHosted()) {
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                   "com/sun/identity/console/propertyWSFedIDPViewHosted.xml"));
        } else {
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                   "com/sun/identity/console/propertyWSFedIDPViewRemote.xml"));
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
            Map idpExtValues =
                ps.getAttributeValues(model.getIDPEXDataMap(), false, model);
            
            // should retain the value of the displayname from General page
            Set entries = getExtendedValues().entrySet();
            Iterator iterator = entries.iterator();
             while (iterator.hasNext()) {
                Map.Entry entry = (Map.Entry)iterator.next();
                if (entry.getKey().equals("displayName")) {
                    idpExtValues.put((String)entry.getKey(),
                        returnEmptySetIfValueIsNull((Set)entry.getValue()));
                }
            } 

            //retrieve all the standard metadata values from the property sheet
            Map idpStdValues =
                ps.getAttributeValues(model.getIDPSTDDataMap(), false, model);

            //save the standard metadata values for the Idp
            model.setIDPSTDAttributeValues(entityName, idpStdValues, realm,
                idpExtValues, location);
            
            //save the extended metadata values for the Idp
            model.setIDPExtAttributeValues(realm, entityName, idpExtValues, 
                location); 
            
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "wsfed.idp.property.updated");
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
            map = model.getIdentityProviderAttributes(realm, entityName);
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
    
    private String getStandardValues() {
        String claimName = null;
        WSFedPropertiesModel model = (WSFedPropertiesModel)getModel();
        try {
            FederationElement fedElem =
                        model.getEntityDesc(realm, entityName);
            claimName = model.getClaimType(fedElem);
            if (claimName.equals(strUPN)) {
                claimName = WSFederationConstants.NAMED_CLAIM_DISPLAY_NAMES[
                    WSFederationConstants.NAMED_CLAIM_UPN];                
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
        return claimName;
    }
}
