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
 * $Id: SAMLv2AttrQueryViewBean.java,v 1.3 2008/06/25 05:49:37 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.federation.model.SAMLv2Model;
import com.sun.web.ui.view.alert.CCAlert;
import javax.servlet.http.HttpServletRequest;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMConsoleException;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.Iterator;
import java.util.List;

public class SAMLv2AttrQueryViewBean extends SAMLv2Base {
    public static final String DEFAULT_DISPLAY_URL =
            "/console/federation/SAMLv2AttrQuery.jsp";
    
    /** Creates a new instance of SAMLv2QueryViewBean */
    public SAMLv2AttrQueryViewBean() {
        super("SAMLv2AttrQuery");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
    
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        AMPropertySheet ps = (AMPropertySheet) getChild(PROPERTIES);
        ps.init();
        SAMLv2Model model = (SAMLv2Model)getModel();
        ps.setAttributeValues(getStandardAttrQueryValues(), model);
        ps.setAttributeValues(getExtendedAttrQueryValues(), model);
    }
    
    protected void createPropertyModel() {
        retrieveCommonProperties();
        psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertySAMLv2AttrQuery.xml"));
        psModel.clear();
    }
    
    public void handleButton1Request(RequestInvocationEvent event)
    throws ModelControlException {
        try {
            SAMLv2Model model = (SAMLv2Model)getModel();
            AMPropertySheet ps =
                    (AMPropertySheet)getChild(PROPERTY_ATTRIBUTES);
            
            //retrieve the standard metadata values from the property sheet
            Map attrQueryValues = ps.getAttributeValues(
                    model.getStandardAttrQueryAttributes(
                    realm, entityName), false, model);
            
            //save the standard metadata values for attribute authority
            model.setStdAttributeQueryValues(realm, entityName, attrQueryValues);
            
            //retrieve the extended metadata values from the property sheet
            Map attrQueryExtValues = ps.getAttributeValues(
                    model.getattrQueryEXDataMap(), false, model);
            
            //save the extended metadata values for attribute authority
            model.setExtAttributeQueryValues(
                    realm, entityName, attrQueryExtValues,location);
            
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "samlv2.attrquery.property.updated");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
        forwardTo();
    }
    
    private Map getStandardAttrQueryValues() {
        Map map = new HashMap();
        try {
            SAMLv2Model model = (SAMLv2Model)getModel();
            map = model.getStandardAttrQueryAttributes(
                    realm, entityName);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage() );
        }
        return map;
    }
    
    private Map getExtendedAttrQueryValues() {
        Map extendedValues = new HashMap();
        try {
            SAMLv2Model model = (SAMLv2Model)getModel();
            Map attr = model.getExtendedAttrQueryAttributes(
                    realm, entityName);
            Set entries = attr.entrySet();
            Iterator iterator = entries.iterator();
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
