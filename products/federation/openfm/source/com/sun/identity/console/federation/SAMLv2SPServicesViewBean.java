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
 * $Id: SAMLv2SPServicesViewBean.java,v 1.5 2008/12/11 18:51:51 babysunil Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.ContainerView;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.View;
import com.sun.identity.console.base.AMPropertySheet; 
import com.sun.identity.console.base.AMTableTiledView;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.federation.model.SAMLv2Model;
import com.sun.identity.console.federation.model.EntityModel;
import com.sun.identity.console.federation.SAMLv2AuthContexts;
import com.sun.identity.saml2.jaxb.metadata.AssertionConsumerServiceElement;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.table.CCActionTable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SAMLv2SPServicesViewBean extends SAMLv2Base {
    public static final String DEFAULT_DISPLAY_URL =
            "/console/federation/SAMLv2SPServices.jsp";
    
    public static final String TBL_ASSERTION_CONSUMER_SERVICE =
            "tblAssertionConsumerService";
    public static final String CHILD_ASSERT_CONSUMER_TILED_VIEW =
            "tableTiledView";
    public static final String TBL_COL_DEFAULT = "tblColDefault";
    public static final String TBL_DATA_DEFAULT = "tblDataDefault";
    public static final String TBL_COL_TYPE = "tblColType";
    public static final String TBL_DATA_LABEL = "tblDataLabel";
    public static final String TBL_DATA_TYPE = "tblDataType";
    public static final String TBL_COL_LOCATION = "tblColLocation";
    public static final String TBL_DATA_LOCATION = "tblDataLocation";
    public static final String TBL_COL_INDEX = "tblColIndex";
    public static final String TBL_DATA_INDEX = "tblDataIndex";
    
    protected CCActionTableModel tblAssertionConsumerModel;

    public SAMLv2SPServicesViewBean() {
        super("SAMLv2SPServices");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
   
  protected void registerChildren() {
	super.registerChildren();
        registerChild(CHILD_ASSERT_CONSUMER_TILED_VIEW, 
            AMTableTiledView.class);
    }
    
    protected View createChild(String name) {
        View view = null;
        if ( name.equals(CHILD_ASSERT_CONSUMER_TILED_VIEW)) {
            view = new AMTableTiledView(this, tblAssertionConsumerModel, name);
        } else if (name.equals(TBL_ASSERTION_CONSUMER_SERVICE)) {
            CCActionTable child = new CCActionTable(
                    this, tblAssertionConsumerModel, name);
            child.setTiledView((ContainerView)getChild(
                    CHILD_ASSERT_CONSUMER_TILED_VIEW));
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
        
        try {
            List assertionConServices = model.getAssertionConsumerServices(
                    realm, entityName);            
            populateAssertionConsumer(assertionConServices);

            //only metalias from ext
            setDisplayFieldValue(model.SP_META_ALIAS,
                    model.getMetaalias(realm, entityName, 
                    EntityModel.SERVICE_PROVIDER));
        } catch (AMConsoleException e){
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
    }
         
    protected void createPropertyModel() {
        retrieveCommonProperties();
        if (isHosted()) {
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertySAMLv2SPServicesHosted.xml"));
        } else {
            psModel = new AMPropertySheetModel(
                getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/propertySAMLv2SPServicesRemote.xml"));
        }
        createAssertionConsumerModel();
        psModel.setModel(TBL_ASSERTION_CONSUMER_SERVICE,
                tblAssertionConsumerModel);
        psModel.clear();
    }
    
    private void createAssertionConsumerModel() {
        tblAssertionConsumerModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblSAMLv2SPAssertionConsumerServices.xml"));
        tblAssertionConsumerModel.setActionValue(TBL_COL_DEFAULT,
                "samlv2.sp.assertionConsumerService.table.default.name");
        tblAssertionConsumerModel.setActionValue(TBL_COL_TYPE,
                "samlv2.sp.assertionConsumerService.table.default.type");
        tblAssertionConsumerModel.setActionValue(TBL_COL_LOCATION,
                "samlv2.sp.assertionConsumerService.table.default.location");
        tblAssertionConsumerModel.setActionValue(TBL_COL_INDEX,
                "samlv2.sp.assertionConsumerService.table.default.index");
    }
    
    private void populateAssertionConsumer(List assertionConServices) {
        tblAssertionConsumerModel.clear();
        SAMLv2Model model = (SAMLv2Model)getModel();
        int numberOfRows = assertionConServices.size();
        for (int i = 0; i < numberOfRows; i++) {
            tblAssertionConsumerModel.appendRow();
            AssertionConsumerServiceElement acsElem =
                (AssertionConsumerServiceElement) assertionConServices.get(i);
            tblAssertionConsumerModel.setValue(
                    TBL_DATA_DEFAULT, String.valueOf(acsElem.isIsDefault()));
            tblAssertionConsumerModel.setValue(TBL_DATA_TYPE, (
                    (acsElem.getBinding()).substring(37)));
            tblAssertionConsumerModel.setValue(TBL_DATA_LABEL, (
                    (acsElem.getBinding()).substring(37)));
            tblAssertionConsumerModel.setValue(TBL_DATA_LOCATION, 
                    acsElem.getLocation());
            tblAssertionConsumerModel.setValue(TBL_DATA_INDEX, 
                    Integer.toString(acsElem.getIndex()));
        }
        
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
            
            //save the standard metadata values for the Idp            
            List ascList = updateWithAssertionServiceVlues();
            model.setSPStdAttributeValues(realm,
                    entityName, spStdValues, ascList);
            
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
    
    private List updateWithAssertionServiceVlues()
    throws ModelControlException {
        List asconsServiceList = new ArrayList();
        int num = 0;
        SAMLv2Model model = (SAMLv2Model)getModel();
        CCActionTable tbl = (CCActionTable)getChild(
                TBL_ASSERTION_CONSUMER_SERVICE);
        tbl.restoreStateData();
        try {
            num = model.getAssertionConsumerServices(
                    realm, entityName).size();
        } catch (AMConsoleException e){
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
        
        for (int i = 0; i < num; i++) {
            tblAssertionConsumerModel.setLocation(i);
            String isDefault = (String)tblAssertionConsumerModel.getValue(
                    TBL_DATA_DEFAULT);
            boolean theValue = Boolean.parseBoolean(isDefault);
            String type = (String)tblAssertionConsumerModel.getValue(
                    TBL_DATA_TYPE);
            String binding = "urn:oasis:names:tc:SAML:2.0:bindings:"+type;
            String location = (String)tblAssertionConsumerModel.getValue(
                    TBL_DATA_LOCATION);
            String index = (String)tblAssertionConsumerModel.getValue(
                    TBL_DATA_INDEX);
            AssertionConsumerServiceElement acsElem = null;
            try {
                acsElem = model.getAscObject();
            } catch (AMConsoleException e){
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        e.getMessage());
            }
            acsElem.setBinding(binding);
            acsElem.setIsDefault(theValue);
            acsElem.setIndex(Integer.parseInt(index));
            acsElem.setLocation(location);
            asconsServiceList.add(acsElem);
            
        }
        
        return asconsServiceList;
    }
   
}
