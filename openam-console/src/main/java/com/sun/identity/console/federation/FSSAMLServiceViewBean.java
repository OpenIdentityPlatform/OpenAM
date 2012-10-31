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
 * $Id: FSSAMLServiceViewBean.java,v 1.4 2008/06/25 05:49:34 qcheng Exp $
 *
 */

package com.sun.identity.console.federation;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.DisplayField;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPipeDelimitAttrTokenizer;
import com.sun.identity.console.base.AMPrimaryMastHeadViewBean;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMModelBase;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.delegation.model.DelegationConfig;
import com.sun.identity.console.federation.model.FSSAMLServiceModel;
import com.sun.identity.console.federation.model.FSSAMLServiceModelImpl;
import com.sun.identity.console.federation.FederationViewBean;
import com.sun.identity.saml.common.SAMLConstants;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class FSSAMLServiceViewBean
    extends AMPrimaryMastHeadViewBean {
    public static final String DEFAULT_DISPLAY_URL =
        "/console/federation/FSSAMLService.jsp";
    private static final String PGTITLE = "pgtitle";
    private static final String TBL_SITE_ID_COL_NAME = "tblSiteIDColName";
    private static final String TBL_TARGET_URLS_COL_NAME =
        "tblTargetURLsColName";
    
    private static Set tabledAttributes = new HashSet(6);
    private static Map DUMMY = new HashMap();
    
    /**
     * Set this attribute in page session attribute whenever this profile
     * is altered in secondary page, and a page is modified alert message
     * box will be displayed.
     */
    static final String MODIFIED = "FSSAMLServiceViewBeanModified";
    
    protected static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    protected static final String TABLE_SITE_ID =
        "iplanet-am-saml-siteid-issuername-list";
    protected static final String TABLE_TARGET_URLS =
        "iplanet-am-saml-post-to-target-urls";
    
    
    static {
        tabledAttributes.add(TABLE_SITE_ID);
        tabledAttributes.add(TABLE_TARGET_URLS);
    }
    
    private CCPageTitleModel ptModel;
    private AMPropertySheetModel propertySheetModel;
    private CCActionTableModel tblSiteIdModel;
    
    private CCActionTableModel tblTargetURLsModel;
    private Map tabledAttributesModel = new HashMap(6);
    private boolean preInitialized = false;
    
    /**
     * Creates a personal profile service profile view bean.
     */
    public FSSAMLServiceViewBean() {
        super("FSSAMLService");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
    }
    
    public void forwardTo(RequestContext rc) {
        Object values = getPageSessionAttribute(PROPERTY_ATTRIBUTE);
        if (values == null) {
            setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)DUMMY);
        }
        super.forwardTo(rc);
    }
    
    protected void initialize() {
        if (!initialized) {
            if (!preInitialized) {
                createPageTitleModel();
                createTableModels();
                createPropertyModel();
                registerChildren();
                preInitialized = true;
            }
            
            Object values = getPageSessionAttribute(PROPERTY_ATTRIBUTE);
            if (values != null) {
                if (values == DUMMY) {
                    removePageSessionAttribute(PROPERTY_ATTRIBUTE);
                }
                super.initialize();
                initialized = true;
                setValues();
            }
        }
    }
    
    protected void registerChildren() {
        super.registerChildren();
        registerChild(PGTITLE, CCPageTitle.class);
        ptModel.registerChildren(this);
        registerChild(PROPERTY_ATTRIBUTE, AMPropertySheet.class);
        propertySheetModel.registerChildren(this);
    }
    
    protected View createChild(String name) {
        View view = null;
        if (name.equals(PGTITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this, name);
        } else if (name.equals(PROPERTY_ATTRIBUTE)) {
            view = new AMPropertySheet(this, propertySheetModel, name);
        } else if (propertySheetModel.isChildSupported(name)) {
            view = propertySheetModel.createChild(this, name, getModel());
        } else {
            view = super.createChild(name);
        }
        
        return view;
    }
    
    public void beginDisplay(DisplayEvent event)
    throws ModelControlException {
        super.beginDisplay(event);
        setLabels();
        setInlineHelps();
        resetButtonState("tblSiteIDButtonDelete");
        resetButtonState("tblTargetURLsButtonDelete");
        
        if ((getPageSessionAttribute(MODIFIED) != null) &&
            !isInlineAlertMessageSet()
            ) {
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "message.profile.modified");
        }
    }
    
    private void setLabels() {
        FSSAMLServiceModel model = (FSSAMLServiceModel)getModel();
        Map labels = model.getAttributeLabels();
        
        for (Iterator iter = labels.keySet().iterator(); iter.hasNext(); ) {
            String name = (String)iter.next();
            
            try {
                View view = getChild("lbl" + name);
                if (DisplayField.class.isInstance(view)) {
                    ((DisplayField)view).setValue((String)labels.get(name));
                }
            } catch (IllegalArgumentException e) {
                // do nothing.
                // child is not a label.
            }
        }
    }
    
    private void setInlineHelps() {
        FSSAMLServiceModel model = (FSSAMLServiceModel)getModel();
        Map helps = model.getAttributeInlineHelps();
        
        for (Iterator iter = helps.keySet().iterator(); iter.hasNext(); ) {
            String name = (String)iter.next();
            
            try {
                View view = getChild("help" + name);
                if (DisplayField.class.isInstance(view)) {
                    ((DisplayField)view).setValue((String)helps.get(name));
                }
            } catch (IllegalArgumentException e) {
                // do nothing.
                // child is not a help.
            }
        }
    }
    
    void setValues() {
        Map attrValues = (Map)getPageSessionAttribute(PROPERTY_ATTRIBUTE);
        FSSAMLServiceModel model = (FSSAMLServiceModel)getModel();
        if (attrValues == null) {
            attrValues = model.getAttributeValues();
        }
        
        Map displayValues = new HashMap(attrValues.size() *2);
        
        // set tables
        for (Iterator iter = attrValues.keySet().iterator(); iter.hasNext(); ){
            String name = (String)iter.next();
            Set values = (Set)attrValues.get(name);
            
            if (tabledAttributes.contains(name)) {
                populateTable(name, values);
                OrderedSet ordered = new OrderedSet();
                ordered.addAll(values);
                displayValues.put(name, ordered);
            } else {
                displayValues.put(name, values);
            }
        }
        
        // set other attributes
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        ps.setAttributeValues(attrValues, model);
        
        setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)displayValues);
    }
    
    private void populateTable(String name, Set values) {
        CCActionTableModel tmodel =
            (CCActionTableModel)tabledAttributesModel.get(name);
        tmodel.clearAll();
        
        if ((values != null) && !values.isEmpty()) {
            if (name.equals(TABLE_TARGET_URLS)) {
                populateListTable(name, tmodel, values, "target");
            } else if (name.equals(TABLE_SITE_ID)) {
                populatePipeSeparatedFormatTable(name, tmodel, values,
                    SAMLConstants.INSTANCEID, "siteIDName");
            }
        }
    }
    
    private void populateListTable(
        String attrName,
        CCActionTableModel tmodel,
        Set values,
        String columnName
        ) {
        boolean firstEntry = true;
        int counter = 0;
        for (Iterator iter = values.iterator(); iter.hasNext(); counter++) {
            if (!firstEntry) {
                tmodel.appendRow();
            } else {
                firstEntry = false;
            }
            
            String value = (String)iter.next();
            tmodel.setValue(columnName, value);
            populateEditDupLink(tmodel, counter, attrName);
        }
    }
    
    private void populatePipeSeparatedFormatTable(
        String attrName,
        CCActionTableModel tmodel,
        Set values,
        String identifier,
        String columnName
        ) {
        AMPipeDelimitAttrTokenizer tokenizer =
            AMPipeDelimitAttrTokenizer.getInstance();
        boolean firstEntry = true;
        int counter = 0;
        
        for (Iterator iter = values.iterator(); iter.hasNext(); counter++) {
            if (!firstEntry) {
                tmodel.appendRow();
            } else {
                firstEntry = false;
            }
            
            String tokenizedValue = (String)iter.next();
            Map map = AMAdminUtils.upCaseKeys(
                tokenizer.tokenizes(tokenizedValue));
            tmodel.setValue(columnName, (String)map.get(identifier));
            populateEditDupLink(tmodel, counter, attrName);
        }
    }
    
    private void populateEditDupLink(
        CCActionTableModel tmodel,
        int counter,
        String name
        ) {
        String qualifiedName = getQualifiedNameForTableCol(name);
        tmodel.setValue(qualifiedName + "editLink", Integer.toString(counter));
    }
    
    private String getQualifiedNameForTableCol(String name) {
        int idx = name.indexOf("-");
        while (idx != -1) {
            name = name.substring(0, idx) + name.substring(idx+1);
            idx = name.indexOf("-");
        }
        return name;
    }
    
    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/threeBtnsPageTitle.xml"));
        ptModel.setValue("button1", "button.save");
        ptModel.setValue("button2", "button.reset");
        ptModel.setValue("button3", getBackButtonLabel());
    }
    
    private void createPropertyModel() {
        DelegationConfig dConfig = DelegationConfig.getInstance();
        String startDN = AMModelBase.getStartDN(
            getRequestContext().getRequest());
        boolean canModify = dConfig.hasPermission(startDN, null,
            AMAdminConstants.PERMISSION_MODIFY,
            getRequestContext().getRequest(), getClass().getName());
        
        // TBD: add readonly xml back
        //"com/sun/identity/console/propertyFSSAMLProfile_Readonly.xml";
        String xmlFile = (canModify) ?
            "com/sun/identity/console/propertyFSSAMLProfile.xml" :
            "com/sun/identity/console/propertyFSSAMLProfile.xml";
        
        propertySheetModel = new AMPropertySheetModel(
            getClass().getClassLoader().getResourceAsStream(xmlFile));
        propertySheetModel.setModel(TABLE_SITE_ID, tblSiteIdModel);
        propertySheetModel.setModel(TABLE_TARGET_URLS, tblTargetURLsModel);
        propertySheetModel.clear();
    }
    
    private void createTableModels() {
        tblSiteIdModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblFSSAMLSiteID.xml"));
        tblSiteIdModel.setTitleLabel("label.items");
        tblSiteIdModel.setActionValue("tblSiteIDButtonAdd", "button.new");
        tblSiteIdModel.setActionValue("tblSiteIDButtonDelete",
            "button.delete");
        tblSiteIdModel.setActionValue(TBL_SITE_ID_COL_NAME,
            "saml.profile.siteid.table.column.instance");
        tabledAttributesModel.put(TABLE_SITE_ID, tblSiteIdModel);
        
        tblTargetURLsModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblFSSAMLTargetURLs.xml"));
        tblTargetURLsModel.setTitleLabel("label.items");
        tblTargetURLsModel.setActionValue("tblTargetURLsButtonAdd",
            "button.new");
        tblTargetURLsModel.setActionValue("tblTargetURLsButtonDelete",
            "button.delete");
        tblTargetURLsModel.setActionValue(TBL_TARGET_URLS_COL_NAME,
            "saml.profile.targetURLs.table.column.url");
        tabledAttributesModel.put(TABLE_TARGET_URLS, tblTargetURLsModel);
    }
    
    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        return new FSSAMLServiceModelImpl(req, getPageSessionAttributes());
    }
    
    public void handleButton1Request(RequestInvocationEvent event)
    throws ModelControlException {
        FSSAMLServiceModel model = (FSSAMLServiceModel)getModel();
        try {
            model.setAttributeValues(getValues(true));
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                "message.updated");
            handleButton2Request(event);
        } catch (AMConsoleException e) {
            try {
                setPageSessionAttribute(PROPERTY_ATTRIBUTE,
                    (HashMap)getValues(false, false));
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
                forwardTo();
            } catch (AMConsoleException ex) {
                /* exception will not be thrown because we are instructing
                 * AMPropertySheet to return values without doing password
                 * validation.
                 */
            }
        }
    }
    
    public void handleButton2Request(RequestInvocationEvent event) {
        removePageSessionAttribute(PROPERTY_ATTRIBUTE);
        removePageSessionAttribute(MODIFIED);
        for (Iterator iter = tabledAttributesModel.keySet().iterator();
        iter.hasNext();
        ) {
            String tblName = (String)iter.next();
            CCActionTableModel tmodel =
                (CCActionTableModel)tabledAttributesModel.get(tblName);
            tmodel.clearAll();
        }
        
        propertySheetModel.clear();
        setValues();
        forwardTo();
    }
    
    /**
     * Handles cancel request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event)
    throws ModelControlException, AMConsoleException {
        FederationViewBean vb =
            (FederationViewBean) getViewBean(FederationViewBean.class);
        backTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
    
    /**
     * Creates the label used to take the user back to the previous page
     * they were viewing. The label is defined in the property file as
     * "Back to {0}" which lets us control the format of the label in
     * different locales.  The name of the current object is substituted.
     */
    protected String getBackButtonLabel() {
        String[] arg = { getModel().getLocalizedString(
            "breadcrumbs.federation.authdomains")};
        return MessageFormat.format(
            getModel().getLocalizedString("back.button"), arg);
    }
    
    private Map getValues(boolean modified)
    throws ModelControlException, AMConsoleException {
        return getValues(modified, true);
    }
    
    private Map getValues(boolean modified, boolean matchPassword)
    throws ModelControlException, AMConsoleException {
        AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        FSSAMLServiceModel model = (FSSAMLServiceModel)getModel();
        Map origValues = model.getAttributeValues();
        Map origNonTableValues = new HashMap();
        origNonTableValues.putAll(origValues);
        AMAdminUtils.removeMapEntries(origNonTableValues, tabledAttributes);
        
        Map mapAttrValues = ps.getAttributeValues(
            origNonTableValues, modified, matchPassword, model);
        Map cachedValues = (Map)getPageSessionAttribute(PROPERTY_ATTRIBUTE);
        
        for (Iterator i = cachedValues.keySet().iterator(); i.hasNext();){
            String attrName = (String)i.next();
            Set values = (Set)cachedValues.get(attrName);
            if (tabledAttributes.contains(attrName)) {
                mapAttrValues.put(attrName, values);
            }
        }
        
        return mapAttrValues;
    }
    
    protected void passPgSessionMap(AMViewBeanBase vb)
    throws ModelControlException {
        try {
            Map modifiedValues = getValues(false, false);
            setPageSessionAttribute(
                PROPERTY_ATTRIBUTE, (HashMap)modifiedValues);
            super.passPgSessionMap(vb);
        } catch (AMConsoleException e) {
            /* exception will not be thrown because we are instructing
             * AMPropertySheet to return values without doing password
             * validation.
             */
        }
    }
    
    public void handleTblSiteIDButtonAddRequest(RequestInvocationEvent event)
    throws ModelControlException {
        FSSAMLSiteIDAddViewBean vb = (FSSAMLSiteIDAddViewBean)
        getViewBean(FSSAMLSiteIDAddViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
    
    public void handleTblSiteIDButtonDeleteRequest(
        RequestInvocationEvent event
        ) throws ModelControlException {
        Map modifiedValues = null;
        
        try {
            modifiedValues = getValues(false, false);
        } catch (AMConsoleException e) {
            /* exception will not be thrown because we are instructing
             * AMPropertySheet to return values without doing password
             * validation.
             */
        }
        
        CCActionTable tbl = (CCActionTable)getChild(TABLE_SITE_ID);
        tbl.restoreStateData();
        Integer[] selected = tblSiteIdModel.getSelectedRows();
        
        if ((selected != null) && (selected.length > 0)) {
            OrderedSet tblValues = (OrderedSet)modifiedValues.get(
                TABLE_SITE_ID);
            tblValues.removeAll(selected);
            modifiedValues.put(TABLE_SITE_ID, tblValues);
        }
        
        setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)modifiedValues);
        setPageSessionAttribute(MODIFIED, "1");
        setValues();
        forwardTo();
    }
    
    public void handleIplanetamsamlsiteidissuernamelisteditLinkRequest(
        RequestInvocationEvent event
        ) throws ModelControlException {
        FSSAMLSiteIDEditViewBean vb = (FSSAMLSiteIDEditViewBean)
        getViewBean(FSSAMLSiteIDEditViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.populateValues((String)getDisplayFieldValue(
            "iplanetamsamlsiteidissuernamelisteditLink"));
        vb.forwardTo(getRequestContext());
    }
    
    public void handleTblTargetURLsButtonAddRequest(
        RequestInvocationEvent event
        ) throws ModelControlException {
        FSSAMLTargetURLsAddViewBean vb = (FSSAMLTargetURLsAddViewBean)
        getViewBean(FSSAMLTargetURLsAddViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
    
    public void handleTblTargetURLsButtonDeleteRequest(
        RequestInvocationEvent event
        ) throws ModelControlException {
        Map modifiedValues = null;
        
        try {
            modifiedValues = getValues(false, false);
        } catch (AMConsoleException e) {
            /* exception will not be thrown because we are instructing
             * AMPropertySheet to return values without doing password
             * validation.
             */
        }
        
        CCActionTable tbl = (CCActionTable)getChild(TABLE_TARGET_URLS);
        tbl.restoreStateData();
        Integer[] selected = tblTargetURLsModel.getSelectedRows();
        
        if ((selected != null) && (selected.length > 0)) {
            OrderedSet tblValues = (OrderedSet)modifiedValues.get(
                TABLE_TARGET_URLS);
            tblValues.removeAll(selected);
            modifiedValues.put(TABLE_TARGET_URLS, tblValues);
        }
        
        setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)modifiedValues);
        setPageSessionAttribute(MODIFIED, "1");
        setValues();
        forwardTo();
    }
    
    public void handleIplanetamsamlposttotargeturlseditLinkRequest(
        RequestInvocationEvent event
        ) throws ModelControlException {
        FSSAMLTargetURLsEditViewBean vb = (FSSAMLTargetURLsEditViewBean)
        getViewBean(FSSAMLTargetURLsEditViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.populateValues((String)getDisplayFieldValue(
            "iplanetamsamlposttotargeturlseditLink"));
        vb.forwardTo(getRequestContext());
    }
    
    protected String getBreadCrumbDisplayName() {
        return "breadcrumbs.saml";
    }
}
