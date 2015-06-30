/*
 * The contents of this file are subject to the terms of the Common Development and
 * Distribution License (the License). You may not use this file except in compliance with the
 * License.
 *
 * You can obtain a copy of the License at legal/CDDLv1.0.txt. See the License for the
 * specific language governing permission and limitations under the License.
 *
 * When distributing Covered Software, include this CDDL Header Notice in each file and include
 * the License file at legal/CDDLv1.0.txt. If applicable, add the following below the CDDL
 * Header, with the fields enclosed by brackets [] replaced by your own identifying
 * information: "Portions Copyrighted [year] [name of copyright owner]".
 *
 * Copyright 2014-2015 ForgeRock AS.
 */

package com.sun.identity.console.sts;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.identity.console.realm.RealmPropertiesBase;
import com.sun.identity.console.sts.model.STSHomeViewBeanModel;
import com.sun.identity.console.sts.model.STSHomeViewBeanModelImpl;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCPageTitleModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.pagetitle.CCPageTitle;
import com.sun.web.ui.view.table.CCActionTable;
import org.forgerock.openam.utils.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.sun.identity.console.sts.model.STSHomeViewBeanModel.STSType;

public class STSHomeViewBean extends RealmPropertiesBase {
    public static final String DEFAULT_DISPLAY_URL = "/console/sts/STSHome.jsp";
    /*
    Used to set a session attribute
     */
    public static final String INSTANCE_NAME = "instanceName";

    private static final String PAGETITLE = "pgtitle";

    /*
    These values below must correspond to tblRestSTSInstances.xml/tblSoapSTSInstances.xml, and also correspond to the handleTbl* method
    names below. The first block corresponds to the table displaying the rest-sts instances, and the second to the
    table displaying the soap-sts instances.
     */
    private static final String TBL_REST_STS_INSTANCES = "tblRestSTSInstances";
    private static final String TBL_REST_STS_INSTANCES_BUTTON_ADD = "tblRestSTSInstancesButtonAdd";
    private static final String TBL_REST_STS_INSTANCES_BUTTON_DELETE = "tblRestSTSInstancesButtonDelete";
    private static final String TBL_REST_STS_INSTANCES_COL_NAME = "tblRestSTSInstancesColName";
    private static final String TBL_REST_STS_INSTANCES_DATA_NAME = "tblRestSTSInstancesDataName";
    private static final String TBL_REST_STS_INSTANCES_COL_ACTION = "tblRestSTSInstancesColAction";
    private static final String TBL_REST_STS_INSTANCES_DATA_ACTION_HREF = "tblRestSTSInstancesDataActionHref";
    private static final String REST_STS_PUBLISHED_INSTANCES_CACHE_KEY = "restSTSPublishedInstancesCache";
    private CCActionTableModel tblModelRestSTSInstances = null;

    private static final String TBL_SOAP_STS_INSTANCES = "tblSoapSTSInstances";
    private static final String TBL_SOAP_STS_INSTANCES_BUTTON_ADD = "tblSoapSTSInstancesButtonAdd";
    private static final String TBL_SOAP_STS_INSTANCES_BUTTON_DELETE = "tblSoapSTSInstancesButtonDelete";
    private static final String TBL_SOAP_STS_INSTANCES_COL_NAME = "tblSoapSTSInstancesColName";
    private static final String TBL_SOAP_STS_INSTANCES_DATA_NAME = "tblSoapSTSInstancesDataName";
    private static final String TBL_SOAP_STS_INSTANCES_COL_ACTION = "tblSoapSTSInstancesColAction";
    private static final String TBL_SOAP_STS_INSTANCES_DATA_ACTION_HREF = "tblSoapSTSInstancesDataActionHref";
    private static final String SOAP_STS_PUBLISHED_INSTANCES_CACHE_KEY = "soapSTSPublishedInstancesCache";
    private CCActionTableModel tblModelSoapSTSInstances = null;

    public STSHomeViewBean() {
        /*
        must be class name minus 'ViewBean', and the .jsp in the REST_DEFAULT_DISPLAY_URL must match this pattern as well,
        as does the url in the entry in amConsoleConfig.xml
         */
        super("STSHome");
        setDefaultDisplayURL(DEFAULT_DISPLAY_URL);
        createTableModel();
        createPageTitleModel();
        registerChildren();
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(PAGETITLE, CCPageTitle.class);
        registerChild(TBL_REST_STS_INSTANCES, CCActionTable.class);
        registerChild(TBL_SOAP_STS_INSTANCES, CCActionTable.class);
        ptModel.registerChildren(this);
        tblModelRestSTSInstances.registerChildren(this);
        tblModelSoapSTSInstances.registerChildren(this);
    }

    protected View createChild(String name) {
        View view;
        if (name.equals(TBL_REST_STS_INSTANCES)) {
            Set<String> publishedInstances = getPublishedInstancesFromCache(REST_STS_PUBLISHED_INSTANCES_CACHE_KEY);
            if (!CollectionUtils.isEmpty(publishedInstances)) {
                populateRestSTSTableModel(publishedInstances);
            } else {
                setRestSTSInstanceNamesInTable();
            }
            view = new CCActionTable(this, tblModelRestSTSInstances, name);
        } else if (name.equals(TBL_SOAP_STS_INSTANCES)) {
            Set<String> publishedInstances = getPublishedInstancesFromCache(SOAP_STS_PUBLISHED_INSTANCES_CACHE_KEY);
            if (!CollectionUtils.isEmpty(publishedInstances)) {
                populateSoapSTSTableModel(publishedInstances);
            } else {
                setSoapSTSInstanceNamesInTable();
            }
            view = new CCActionTable(this, tblModelSoapSTSInstances, name);
        } else if (name.equals(PAGETITLE)) {
            view = new CCPageTitle(this, ptModel, name);
        } else if (tblModelRestSTSInstances.isChildSupported(name)) {
            view = tblModelRestSTSInstances.createChild(this, name);
        } else if (tblModelSoapSTSInstances.isChildSupported(name)) {
            view = tblModelSoapSTSInstances.createChild(this, name);
        } else if (ptModel.isChildSupported(name)) {
            view = ptModel.createChild(this,name);
        } else {
            view = super.createChild(name);
        }

        return view;
    }

    private Set<String> getPublishedInstancesFromCache(String cacheKey) {
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        Map<String, Set<String>> instancesCache = (Map<String, Set<String>>) szCache.getSerializedObj();
        if (instancesCache != null) {
            return instancesCache.get(cacheKey);
        } else {
            return Collections.emptySet();
        }
    }

    public void beginDisplay(DisplayEvent event)
            throws ModelControlException {
        super.beginDisplay(event);
        resetButtonState(TBL_REST_STS_INSTANCES_BUTTON_DELETE);
        resetButtonState(TBL_SOAP_STS_INSTANCES_BUTTON_DELETE);
        setRestSTSInstanceNamesInTable();
        setSoapSTSInstanceNamesInTable();
        setPageTitle(getModel(), "sts.home.page.title");
    }

    protected AMModel getModelInternal() {
        RequestContext rc = RequestManager.getRequestContext();
        HttpServletRequest req = rc.getRequest();
        try {
            return new STSHomeViewBeanModelImpl(req, getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error", e.getMessage());
            throw new IllegalStateException("Exception getting model in STSHomeViewBean: " + e.getMessage(), e);
        }
    }

    private void createPageTitleModel() {
        ptModel = new CCPageTitleModel(
                getClass().getClassLoader().getResourceAsStream(
                        "com/sun/identity/console/oneBtnPageTitle.xml"));
        ptModel.setValue("button1", getBackButtonLabel());
    }

    private void createTableModel() {
        tblModelRestSTSInstances = new CCActionTableModel(
                getClass().getClassLoader().getResourceAsStream(
                        "com/sun/identity/console/tblRestSTSInstances.xml"));
        tblModelRestSTSInstances.setTitleLabel("label.items");
        tblModelRestSTSInstances.setActionValue(TBL_REST_STS_INSTANCES_BUTTON_ADD, "rest.sts.home.instances.table.button.new");
        tblModelRestSTSInstances.setActionValue(TBL_REST_STS_INSTANCES_BUTTON_DELETE,
                "rest.sts.home.instances.table.button.delete");
        tblModelRestSTSInstances.setActionValue(TBL_REST_STS_INSTANCES_COL_NAME,
                "rest.sts.home.instances.table.column.name");
        tblModelRestSTSInstances.setActionValue(TBL_REST_STS_INSTANCES_COL_ACTION,
                "rest.sts.home.instances.table.action.column.name");

        tblModelSoapSTSInstances = new CCActionTableModel(
                getClass().getClassLoader().getResourceAsStream(
                        "com/sun/identity/console/tblSoapSTSInstances.xml"));
        tblModelSoapSTSInstances.setTitleLabel("label.items");
        tblModelSoapSTSInstances.setActionValue(TBL_SOAP_STS_INSTANCES_BUTTON_ADD, "soap.sts.home.instances.table.button.new");
        tblModelSoapSTSInstances.setActionValue(TBL_SOAP_STS_INSTANCES_BUTTON_DELETE,
                "soap.sts.home.instances.table.button.delete");
        tblModelSoapSTSInstances.setActionValue(TBL_SOAP_STS_INSTANCES_COL_NAME,
                "soap.sts.home.instances.table.column.name");
        tblModelSoapSTSInstances.setActionValue(TBL_SOAP_STS_INSTANCES_COL_ACTION,
                "soap.sts.home.instances.table.action.column.name");
    }

    private void setRestSTSInstanceNamesInTable() {
        STSHomeViewBeanModel model = (STSHomeViewBeanModel)getModel();
        try {
            String curRealm = (String)getPageSessionAttribute(
                    AMAdminConstants.CURRENT_REALM);
            populateRestSTSTableModel(model.getPublishedInstances(STSType.REST, curRealm));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
    }

    private void setSoapSTSInstanceNamesInTable() {
        STSHomeViewBeanModel model = (STSHomeViewBeanModel)getModel();
        try {
            String curRealm = (String)getPageSessionAttribute(
                    AMAdminConstants.CURRENT_REALM);
            populateSoapSTSTableModel(model.getPublishedInstances(STSType.SOAP, curRealm));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
    }

    private void populateRestSTSTableModel(Set<String> publishedInstances) {
        tblModelRestSTSInstances.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        Map<String, Set<String>> cacheMap = (Map<String, Set<String>>)szCache.getSerializedObj();
        if (cacheMap == null) {
            cacheMap = new HashMap<>();
        }

        if ((publishedInstances != null) && !publishedInstances.isEmpty()) {
            boolean firstEntry = true;
            for (String instanceName : publishedInstances) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblModelRestSTSInstances.appendRow();
                }

                tblModelRestSTSInstances.setValue(TBL_REST_STS_INSTANCES_DATA_NAME, instanceName);
                tblModelRestSTSInstances.setValue(TBL_REST_STS_INSTANCES_DATA_ACTION_HREF, instanceName);
            }
            cacheMap.put(REST_STS_PUBLISHED_INSTANCES_CACHE_KEY, publishedInstances);
            szCache.setValue(cacheMap);
        } else {
            szCache.setValue(null);
        }
    }

    private void populateSoapSTSTableModel(Set<String> publishedInstances) {
        tblModelSoapSTSInstances.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        Map<String, Set<String>> cacheMap = (Map<String, Set<String>>)szCache.getSerializedObj();
        if (cacheMap == null) {
            cacheMap = new HashMap<>();
        }

        if ((publishedInstances != null) && !publishedInstances.isEmpty()) {
            boolean firstEntry = true;
            for (String instanceName : publishedInstances) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblModelSoapSTSInstances.appendRow();
                }

                tblModelSoapSTSInstances.setValue(TBL_SOAP_STS_INSTANCES_DATA_NAME, instanceName);
                tblModelSoapSTSInstances.setValue(TBL_SOAP_STS_INSTANCES_DATA_ACTION_HREF, instanceName);
            }
            cacheMap.put(SOAP_STS_PUBLISHED_INSTANCES_CACHE_KEY, publishedInstances);
            szCache.setValue(cacheMap);
        } else {
            szCache.setValue(null);
        }
    }

    /**
     * Forwards request to creation view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblRestSTSInstancesButtonAddRequest(RequestInvocationEvent event) {
        RestSTSAddViewBean vb = (RestSTSAddViewBean)getViewBean(RestSTSAddViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Forwards request to creation view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblSoapSTSInstancesButtonAddRequest(RequestInvocationEvent event) {
        SoapSTSAddViewBean vb = (SoapSTSAddViewBean)getViewBean(SoapSTSAddViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }


    /**
     * Forwards request to ViewBean to edit Rest STS instance
     */
    public void handleTblRestSTSInstancesDataActionHrefRequest(RequestInvocationEvent event) {
        RestSTSEditViewBean vb = (RestSTSEditViewBean)getViewBean(
                RestSTSEditViewBean.class);
        String instanceName = hexToString((String)getDisplayFieldValue(
                TBL_REST_STS_INSTANCES_DATA_ACTION_HREF));
        setPageSessionAttribute(INSTANCE_NAME, instanceName);
        setPageSessionAttribute(AMAdminConstants.SAVE_VB_NAME, getClass().getName());
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    /**
     * Forwards request to ViewBean to edit Soap STS instance
     */
    public void handleTblSoapSTSInstancesDataActionHrefRequest(RequestInvocationEvent event) {
        SoapSTSEditViewBean vb = (SoapSTSEditViewBean)getViewBean(
                SoapSTSEditViewBean.class);
        String instanceName = hexToString((String)getDisplayFieldValue(
                TBL_SOAP_STS_INSTANCES_DATA_ACTION_HREF));
        setPageSessionAttribute(INSTANCE_NAME, instanceName);
        setPageSessionAttribute(AMAdminConstants.SAVE_VB_NAME, getClass().getName());
        unlockPageTrail();
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }

    public void handleTblRestSTSInstancesButtonDeleteRequest(RequestInvocationEvent event) throws ModelControlException {
        CCActionTable table = (CCActionTable)getChild(TBL_REST_STS_INSTANCES);
        table.restoreStateData();

        Integer[] selected = tblModelRestSTSInstances.getSelectedRows();
        Set<String> instanceNames = new HashSet<>(selected.length);

        for (int i = 0; i < selected.length; i++) {
            tblModelRestSTSInstances.setRowIndex(selected[i].intValue());
            instanceNames.add((String) tblModelRestSTSInstances.getValue(TBL_REST_STS_INSTANCES_DATA_NAME));
        }

        try {
            STSHomeViewBeanModel model = (STSHomeViewBeanModel)getModel();
            model.deleteInstances(STSType.REST, instanceNames);

            if (selected.length == 1) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                        "rest.sts.home.instance.deleted");
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                        "rest.sts.home.instances.deleted");
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }

        forwardTo();
    }

    public void handleTblSoapSTSInstancesButtonDeleteRequest(RequestInvocationEvent event) throws ModelControlException {
        CCActionTable table = (CCActionTable)getChild(TBL_SOAP_STS_INSTANCES);
        table.restoreStateData();

        Integer[] selected = tblModelSoapSTSInstances.getSelectedRows();
        Set<String> instanceNames = new HashSet<>(selected.length);

        for (int i = 0; i < selected.length; i++) {
            tblModelSoapSTSInstances.setRowIndex(selected[i].intValue());
            instanceNames.add((String) tblModelSoapSTSInstances.getValue(TBL_SOAP_STS_INSTANCES_DATA_NAME));
        }

        try {
            STSHomeViewBeanModel model = (STSHomeViewBeanModel)getModel();
            model.deleteInstances(STSType.SOAP, instanceNames);

            if (selected.length == 1) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                        "rest.sts.home.instance.deleted");
            } else {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                        "rest.sts.home.instances.deleted");
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }

        forwardTo();
    }

    /**
     * Handles "back to" page request.
     *
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event) {
        backTrail();
        forwardToRealmView(event);
    }
}
