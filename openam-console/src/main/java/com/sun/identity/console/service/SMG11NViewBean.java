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
 * $Id: SMG11NViewBean.java,v 1.5 2008/07/07 20:39:20 veiming Exp $
 *
 */

package com.sun.identity.console.service;

import com.iplanet.jato.NavigationException;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMServiceProfileModel;
import com.sun.identity.console.realm.ServicesAddViewBean;
import com.sun.identity.console.realm.ServicesEditViewBean;
import com.sun.identity.console.service.model.CharsetAliasEntry;
import com.sun.identity.console.service.model.LocaleSupportedCharsetsEntry;
import com.sun.identity.console.service.model.SMG11NModelImpl;
import com.sun.identity.shared.datastruct.OrderedSet;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.table.CCActionTable;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class SMG11NViewBean
    extends AMServiceProfileViewBeanBase
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/service/SMG11N.jsp";
    public static final String PAGE_MODIFIED = "pageModified";

    private static final String TEMPLATE = "template";
    private static final String TBL_SUPPORTED_CHARSETS_COL_CHARSETS =
        "tblSupportedCharsetsColCharsets";
    private static final String TBL_SUPPORTED_CHARSETS_COL_LOCALE =
        "tblSupportedCharsetsColLocale";
    private static final String TBL_SUPPORTED_CHARSETS_DATA_LOCALE =
        "tblSupportedCharsetsDataLocale";
    private static final String TBL_SUPPORTED_CHARSETS_DATA_CHARSETS =
        "tblSupportedCharsetsDataCharsets";
    private static final String TBL_SUPPORTED_CHARSETS_HREF_ACTION =
        "tblSupportedCharsetsHrefAction";
    private static final String TBL_SUPPORTED_CHARSETS_ADD_BTN =
        "tblSupportedCharsetsButtonAdd";
    private static final String TBL_SUPPORTED_CHARSETS_DELETE_BTN =
        "tblSupportedCharsetsButtonDelete";

    private static final String TBL_CHARSET_ALIAS_COL_MIMENAME =
        "tblCharsetAliasColMimeName";
    private static final String TBL_CHARSET_ALIAS_COL_JAVANAME =
        "tblCharsetAliasColJavaName";
    private static final String TBL_CHARSET_ALIAS_DATA_MIMENAME =
        "tblCharsetAliasDataMimeName";
    private static final String TBL_CHARSET_ALIAS_DATA_JAVANAME =
        "tblCharsetAliasDataJavaName";
    private static final String TBL_CHARSET_ALIAS_HREF_ACTION =
        "tblCharsetAliasHrefAction";
    private static final String TBL_CHARSET_ALIAS_ADD_BTN =
        "tblCharsetAliasButtonAdd";
    private static final String TBL_CHARSET_ALIAS_DELETE_BTN =
        "tblCharsetAliasButtonDelete";

    private boolean tablePopulated = false;

    /**
     * Creates a personal profile service profile view bean.
     */
    public SMG11NViewBean() {
        super("SMG11N", DEFAULT_DISPLAY_URL, "iPlanetG11NSettings");
    }

    /**
     * Forwards to template base creation and modification view bean.
     */
    public void forwardTo(RequestContext reqContext)
        throws NavigationException {
        HttpServletRequest req = reqContext.getRequest();
        String template = (String)req.getParameter("Template");

        if ((template != null) && template.equals("true")) {
            String operation = (String)req.getParameter("Op");

            AMServiceProfileViewBeanBase vb = null;
            
            if (operation.equals("add")) {
                vb = (AMServiceProfileViewBeanBase)getViewBean(
                    ServicesAddViewBean.class);
            } else {
                vb = (AMServiceProfileViewBeanBase)getViewBean(
                    ServicesEditViewBean.class);
                vb.setPageSessionAttribute(AMAdminConstants.SAVE_VB_NAME,
                    "com.sun.identity.console.realm.ServicesViewBean");
            }

            vb.setPageSessionAttribute(AMAdminConstants.CURRENT_REALM,
                hexToString((String)req.getParameter("Location")));
            vb.setPageSessionAttribute(
                AMServiceProfileViewBeanBase.SERVICE_NAME,
                (String)req.getParameter("ServiceName"));
            passPgSessionMap(vb);
            vb.forwardTo(reqContext);
        } else {
            super.forwardTo(reqContext);
        }
    }

    protected View createChild(String name) {
        if (!tablePopulated) {
            prePopulateTable();
        }
        return super.createChild(name);
    }

    protected void createPropertyModel() {
        super.createPropertyModel();
        createSupportedCharsetsTableModel();
        createCharsetAliasTableModel();
    }
    
    private void createSupportedCharsetsTableModel() {
        CCActionTableModel tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
       "com/sun/identity/console/tblG11NSupportedCharsets.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_SUPPORTED_CHARSETS_COL_LOCALE,
            "globalization.service.table.SupportedCharsets.locale");
        tblModel.setActionValue(TBL_SUPPORTED_CHARSETS_COL_CHARSETS,
            "globalization.service.table.SupportedCharsets.charsets");
        tblModel.setActionValue(TBL_SUPPORTED_CHARSETS_ADD_BTN,
        "globalization.service.table.SupportedCharsets.add.button");
        tblModel.setActionValue(TBL_SUPPORTED_CHARSETS_DELETE_BTN,
        "globalization.service.table.SupportedCharsets.delete.button");
        propertySheetModel.setModel(
        SMG11NModelImpl.ATTRIBUTE_NAME_SUPPORTED_CHARSETS,
            tblModel);
    }

    private void createCharsetAliasTableModel() {
        CCActionTableModel tblModel = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
                "com/sun/identity/console/tblG11NCharsetAlias.xml"));
        tblModel.setTitleLabel("label.items");
        tblModel.setActionValue(TBL_CHARSET_ALIAS_COL_MIMENAME,
            "globalization.service.table.CharsetAlias.mimeName");
        tblModel.setActionValue(TBL_CHARSET_ALIAS_COL_JAVANAME,
            "globalization.service.table.CharsetAlias.javaName");
        tblModel.setActionValue(TBL_CHARSET_ALIAS_ADD_BTN,
            "globalization.service.table.CharsetAlias.add.button");
        tblModel.setActionValue(TBL_CHARSET_ALIAS_DELETE_BTN,
            "globalization.service.table.CharsetAlias.delete.button");
        propertySheetModel.setModel(
            SMG11NModelImpl.ATTRIBUTE_NAME_CHARSET_ALIAS, tblModel);
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
            RequestManager.getRequestContext().getRequest();
        try {
            return new SMG11NModelImpl(
                req, serviceName, getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        return null;
    }

    private void prePopulateTable() {
        Map attributeValues = (Map)removePageSessionAttribute(
            PROPERTY_ATTRIBUTE);
        if (attributeValues != null) {
            AMPropertySheet ps = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
            ps.setAttributeValues(attributeValues, getModel());
        }
        prePopulateSupportedCharsetsTable(attributeValues);
        prePopulateCharsetAliasTable(attributeValues);
    }

    private void prePopulateSupportedCharsetsTable(Map attributeValues) {
        Set supportedCharsets = null;

        if (attributeValues != null) {
            supportedCharsets = (Set)attributeValues.get(
                SMG11NModelImpl.ATTRIBUTE_NAME_SUPPORTED_CHARSETS);
            if (supportedCharsets == null) {
                supportedCharsets = Collections.EMPTY_SET;
            }
        } else {
            supportedCharsets = (Set)removePageSessionAttribute(
                SMG11NModelImpl.ATTRIBUTE_NAME_SUPPORTED_CHARSETS);
        }

        if (supportedCharsets != null) {
            populateSupportedCharsetsTable(supportedCharsets);
        }
    }

    private void prePopulateCharsetAliasTable(Map attributeValues) {
        Set charsetAlias = null;

        if (attributeValues != null) {
            charsetAlias = (Set)attributeValues.get(
                SMG11NModelImpl.ATTRIBUTE_NAME_CHARSET_ALIAS);
            if (charsetAlias == null) {
                charsetAlias = Collections.EMPTY_SET;
            }
        } else {
            charsetAlias = (Set)removePageSessionAttribute(
                SMG11NModelImpl.ATTRIBUTE_NAME_CHARSET_ALIAS);
        }

        if (charsetAlias != null) {
            populateCharsetAliasTable(charsetAlias);
        }
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);

        resetButtonState(TBL_SUPPORTED_CHARSETS_DELETE_BTN);
        resetButtonState(TBL_CHARSET_ALIAS_DELETE_BTN);

        if (!tablePopulated) {
            if (!isSubmitCycle()) {
                AMServiceProfileModel model = (AMServiceProfileModel)getModel();

                if (model != null) {
                    Set supportedCharsets = new OrderedSet();
                    supportedCharsets.addAll(model.getAttributeValues(
                        SMG11NModelImpl.ATTRIBUTE_NAME_SUPPORTED_CHARSETS));
                    populateSupportedCharsetsTable(supportedCharsets);

                    Set charsetAlias = new OrderedSet();
                    charsetAlias.addAll(model.getAttributeValues(
                        SMG11NModelImpl.ATTRIBUTE_NAME_CHARSET_ALIAS));
                    populateCharsetAliasTable(charsetAlias);
                }
            }
        }

        if (!isInlineAlertMessageSet()) {
            String flag = (String)getPageSessionAttribute(PAGE_MODIFIED);
            if ((flag != null) && flag.equals("1")) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "message.profile.modified");
            }
        }
    }

    private void populateSupportedCharsetsTable(Set charsets) {
        tablePopulated = true;
        CCActionTableModel tblModel =
            (CCActionTableModel)propertySheetModel.getModel(
                SMG11NModelImpl.ATTRIBUTE_NAME_SUPPORTED_CHARSETS);
        tblModel.clearAll();
        boolean firstEntry = true;
        int counter = 0;

        for (Iterator iter = charsets.iterator(); iter.hasNext(); ) {
            String c = (String)iter.next();
            LocaleSupportedCharsetsEntry entry = new
                LocaleSupportedCharsetsEntry(c);

            if (entry.isValid()) {
                if (!firstEntry) {
                    tblModel.appendRow();
                } else {
                    firstEntry = false;
                }

                tblModel.setValue(TBL_SUPPORTED_CHARSETS_DATA_LOCALE,
                    entry.strLocale);
                tblModel.setValue(TBL_SUPPORTED_CHARSETS_DATA_CHARSETS,
                    entry.strCharsets);
                tblModel.setValue(TBL_SUPPORTED_CHARSETS_HREF_ACTION,
                    Integer.toString(counter));
            }
            counter++;
        }

        setPageSessionAttribute(
            SMG11NModelImpl.ATTRIBUTE_NAME_SUPPORTED_CHARSETS,
            (OrderedSet)charsets);
    }

    private void populateCharsetAliasTable(Set charsets) {
        tablePopulated = true;
        CCActionTableModel tblModel =
            (CCActionTableModel)propertySheetModel.getModel(
                SMG11NModelImpl.ATTRIBUTE_NAME_CHARSET_ALIAS);
        tblModel.clearAll();
        boolean firstEntry = true;
        int counter = 0;

        for (Iterator iter = charsets.iterator(); iter.hasNext(); ) {
            String c = (String)iter.next();
            CharsetAliasEntry entry = new CharsetAliasEntry(c);

            if (entry.isValid()) {
                if (!firstEntry) {
                    tblModel.appendRow();
                } else {
                    firstEntry = false;
                }

                tblModel.setValue(TBL_CHARSET_ALIAS_DATA_MIMENAME,
                    entry.strMimeName);
                tblModel.setValue(TBL_CHARSET_ALIAS_DATA_JAVANAME,
                    entry.strJavaName);
                tblModel.setValue(TBL_CHARSET_ALIAS_HREF_ACTION,
                    Integer.toString(counter));
            }
            counter++;
        }

        setPageSessionAttribute(
            SMG11NModelImpl.ATTRIBUTE_NAME_CHARSET_ALIAS, (OrderedSet)charsets);
    }

    protected boolean onBeforeSaveProfile(Map attrValues) {
        Set supportedCharsets = (Set)getPageSessionAttribute(
            SMG11NModelImpl.ATTRIBUTE_NAME_SUPPORTED_CHARSETS);

        if ((supportedCharsets != null) && !supportedCharsets.isEmpty()) {
            attrValues.put(SMG11NModelImpl.ATTRIBUTE_NAME_SUPPORTED_CHARSETS,
                supportedCharsets);
        }

        Set charsetAlias = (Set)getPageSessionAttribute(
            SMG11NModelImpl.ATTRIBUTE_NAME_CHARSET_ALIAS);

        if ((charsetAlias != null) && !charsetAlias.isEmpty()) {
            attrValues.put(SMG11NModelImpl.ATTRIBUTE_NAME_CHARSET_ALIAS,
                charsetAlias);
        }

        return true;
    }

    protected void onBeforeResetProfile() {
        removePageSessionAttribute(
            SMG11NModelImpl.ATTRIBUTE_NAME_SUPPORTED_CHARSETS);
        removePageSessionAttribute(
            SMG11NModelImpl.ATTRIBUTE_NAME_CHARSET_ALIAS);
        tablePopulated = false;
    }

    /**
     * Handles remove supported container request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblSupportedCharsetsButtonDeleteRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        setSubmitCycle(true);
        CCActionTable table = (CCActionTable)getChild(
            SMG11NModelImpl.ATTRIBUTE_NAME_SUPPORTED_CHARSETS);
        table.restoreStateData();
        CCActionTableModel tblModel =
            (CCActionTableModel)propertySheetModel.getModel(
                SMG11NModelImpl.ATTRIBUTE_NAME_SUPPORTED_CHARSETS);
        Integer[] selected = tblModel.getSelectedRows();

        if ((selected != null) && (selected.length > 0)) {
            OrderedSet tblValues = (OrderedSet)getPageSessionAttribute(
                SMG11NModelImpl.ATTRIBUTE_NAME_SUPPORTED_CHARSETS);
            tblValues.removeAll(selected);
            setPageSessionAttribute(
                SMG11NModelImpl.ATTRIBUTE_NAME_SUPPORTED_CHARSETS, tblValues);
            populateSupportedCharsetsTable(tblValues);
        }

        forwardTo();
    }

    /**
     * Handles add supported container request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblSupportedCharsetsButtonAddRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        try {
            Map values = getValues();
            onBeforeSaveProfile(values);
            setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
            G11NSupportedCharsetsAddViewBean vb =
                (G11NSupportedCharsetsAddViewBean)
                getViewBean(G11NSupportedCharsetsAddViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    /**
     * Handles edit supported container request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblSupportedCharsetsHrefActionRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        try {
            Map values = getValues();
            onBeforeSaveProfile(values);
            setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
            G11NSupportedCharsetsEditViewBean vb = 
                (G11NSupportedCharsetsEditViewBean)getViewBean(
                    G11NSupportedCharsetsEditViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.populateValues((String)getDisplayFieldValue(
                "tblSupportedCharsetsHrefAction"));
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    /**
     * Handles remove charset alias request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblCharsetAliasButtonDeleteRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        setSubmitCycle(true);
        CCActionTable table = (CCActionTable)getChild(
            SMG11NModelImpl.ATTRIBUTE_NAME_CHARSET_ALIAS);
        table.restoreStateData();
        CCActionTableModel tblModel =
            (CCActionTableModel)propertySheetModel.getModel(
                SMG11NModelImpl.ATTRIBUTE_NAME_CHARSET_ALIAS);
        Integer[] selected = tblModel.getSelectedRows();

        if ((selected != null) && (selected.length > 0)) {
            OrderedSet tblValues = (OrderedSet)getPageSessionAttribute(
                SMG11NModelImpl.ATTRIBUTE_NAME_CHARSET_ALIAS);
            tblValues.removeAll(selected);
            setPageSessionAttribute(
                SMG11NModelImpl.ATTRIBUTE_NAME_CHARSET_ALIAS, tblValues);
            populateCharsetAliasTable(tblValues);
        }

        forwardTo();
    }

    /**
     * Handles add charset alias request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblCharsetAliasButtonAddRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        try {
            Map values = getValues();
            onBeforeSaveProfile(values);
            setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
            G11NCharsetAliasAddViewBean vb = (G11NCharsetAliasAddViewBean)
                getViewBean(G11NCharsetAliasAddViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    /**
     * Handles edit charset alias request.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblCharsetAliasHrefActionRequest(
        RequestInvocationEvent event
    ) throws ModelControlException {
        try {
            Map values = getValues();
            onBeforeSaveProfile(values);
            setPageSessionAttribute(PROPERTY_ATTRIBUTE, (HashMap)values);
            G11NCharsetAliasEditViewBean vb =
                (G11NCharsetAliasEditViewBean)getViewBean(
                    G11NCharsetAliasEditViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.populateValues((String)getDisplayFieldValue(
                "tblCharsetAliasHrefAction"));
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    /**
     * Handles save request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        removePageSessionAttribute(PAGE_MODIFIED);
        super.handleButton1Request(event);
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        removePageSessionAttribute(PAGE_MODIFIED);
        super.handleButton2Request(event);
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event)
        throws ModelControlException
    {        
        backTrail();
        try {
            String name = (String) getPageSessionAttribute(
                    AMAdminConstants.SAVE_VB_NAME);
            SCConfigViewBean vb = (SCConfigViewBean) getViewBean(
                    Class.forName(name));
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext()); 
        } catch (ClassNotFoundException e) {
            debug.warning("SMG11NViewBean.handleButton3Request:", e);
        }
        
    }

    protected String getBreadCrumbDisplayName() {
        AMServiceProfileModel model = (AMServiceProfileModel)getModel();
        String[] arg = {model.getLocalizedServiceName(
            AMAdminConstants.G11N_SERVICE_NAME)};
        return MessageFormat.format(model.getLocalizedString(
            "breadcrumbs.services.edit"), (Object[])arg);
    }

    protected boolean startPageTrail() {
        return false;
    }
}
