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
 * $Id: WSFedGeneralViewBean.java,v 1.6 2008/06/25 05:49:38 qcheng Exp $
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
import com.sun.identity.console.federation.model.EntityModel;
import com.sun.identity.console.federation.model.EntityModelImpl;
import com.sun.identity.wsfederation.jaxb.wsfederation.FederationElement;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import java.util.List;

public class WSFedGeneralViewBean extends WSFedGeneralBase {
    public static final String DEFAULT_DISPLAY_URL =
            "/console/federation/WSFedGeneral.jsp";
    
    public WSFedGeneralViewBean() {
        super("WSFedGeneral");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
    
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        super.beginDisplay(event);
        
        //setting the Name fields
        setDisplayFieldValue(WSFedPropertiesModel.TF_REALM, realm);
        setDisplayFieldValue(WSFedPropertiesModel.TF_NAME, entityName);
        try {
            WSFedPropertiesModel model = (WSFedPropertiesModel)getModel();
            FederationElement fedElement =
                    model.getEntityDesc(realm, entityName);
            
            //setting the Token Issuer Name
            setDisplayFieldValue(WSFedPropertiesModel.TFTOKENISSUER_NAME,
                    model.getTokenName(fedElement));
            
            //setting the Token Issuer End Point
            setDisplayFieldValue(WSFedPropertiesModel.TFTOKENISSUER_ENDPT,
                    model.getTokenEndpoint(fedElement));
            
            //setting the value of displayName
            setDisplayName(entityName, realm);
        } catch (AMConsoleException e) {
            debug.error("WSFedGeneralViewBean.beginDisplay", e);
        }
    }
    
    protected void createPropertyModel() {
        retrieveCommonProperties();
        List roleList = getWSFedRoles(entityName, realm);
        if (roleList.size() == 1) {
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                    "com/sun/identity/console/propertyWSFedGeneralView.xml"));
        } else {
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                    "com/sun/identity/console/propertyWSFedGeneralDual.xml"));
        }
        psModel.clear();
    }
    
    public void handleButton1Request(RequestInvocationEvent event)
    throws ModelControlException {
        
        // get the entity name, realm, and location.
        retrieveCommonProperties();
        try {
            WSFedPropertiesModel model = (WSFedPropertiesModel)getModel();
            AMPropertySheet ps =
                    (AMPropertySheet)getChild(PROPERTY_ATTRIBUTES);
            String role = "dual";
            Map attributes = null;
            List roleList = getWSFedRoles(entityName, realm);
            if (roleList.size() == 1) {
                role = (String)roleList.listIterator().next();              
                attributes =  model.getGenAttributes();                
            } else {
                attributes = model.getDualRoleAttributes();
            }
            Map values = ps.getAttributeValues(attributes, false, model);
            model.setGenAttributeValues(
                 realm, entityName, values, role, location);
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "wsfed.general.property.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
        forwardTo();
    }
    
    private void setDisplayName(String entityName, String realm) {
        WSFedPropertiesModel model = (WSFedPropertiesModel)getModel();
        Map spmap = new HashMap();
        Map idpmap = new HashMap();
        try {
            // retrieve role of entity
            List roleList = getWSFedRoles(entityName, realm);
            Iterator rIt = roleList.listIterator();
            
            // to display idp and sp display names in case of a dual entity
            if (roleList.size() > 1) {
                spmap = model.getServiceProviderAttributes(realm, entityName);
                idpmap = model.getIdentityProviderAttributes(
                    realm, entityName);
                setDisplayFieldValue(
                    WSFedPropertiesModel.TF_DISPNAME, getDisplayName(spmap));
                setDisplayFieldValue(
                    WSFedPropertiesModel.TFIDPDISP_NAME, getDisplayName(idpmap));
                
            } else {
                
                // to show display name for an entity with single role
                while (rIt.hasNext()) {
                    String role = (String)rIt.next();
                    if (role.equals(EntityModel.SERVICE_PROVIDER)) {
                        spmap = model.getServiceProviderAttributes(
                            realm, entityName);
                        setDisplayFieldValue(
                            WSFedPropertiesModel.TF_DISPNAME,
                                getDisplayName(spmap));
                    } else if (role.equals(EntityModel.IDENTITY_PROVIDER)) {
                        idpmap = 
                            model.getIdentityProviderAttributes(
                                realm, entityName);
                        setDisplayFieldValue(
                            WSFedPropertiesModel.TF_DISPNAME,
                                getDisplayName(idpmap));
                    }
                }
            }
            
            
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "error in setting Display Name ");
        }
        
    }
    
    private String getDisplayName(Map map) {
        String disName = null;
        if (map.get(WSFedPropertiesModel.TF_DISPNAME) != null) {
            List list = (List)map.get(WSFedPropertiesModel.TF_DISPNAME);
            Iterator i = list.iterator();
            while ((i !=  null)&& (i.hasNext())) {
                disName = (String) i.next();
            }
        }
        return disName;
    }
    
    private List getWSFedRoles(String entityName, String realm) {
        EntityModelImpl entModel = (EntityModelImpl)getEntityModel();
        List role = entModel.getWSFedRoles(entityName, realm);
        return role;
    }
}
