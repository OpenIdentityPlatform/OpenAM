/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009 Sun Microsystems Inc. All Rights Reserved
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
 * $Id: SecurityTokenServiceViewBean.java,v 1.6 2009/12/19 02:17:03 asyhuang Exp $
 *
 */
package com.sun.identity.console.service;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.RequestManager;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.AMServiceProfileViewBeanBase;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.base.model.AMServiceProfileModel;
import com.sun.identity.console.service.model.SecurityTokenServiceModel;
import com.sun.identity.console.service.model.SecurityTokenServiceModelImpl;
import com.sun.identity.wss.security.ConfiguredSignedElements;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCSelectableList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;

public class SecurityTokenServiceViewBean
        extends AMServiceProfileViewBeanBase {

    public static final String DEFAULT_DISPLAY_URL =
            "/console/service/SecurityTokenService.jsp";
    protected static final String PROPERTIES =
            "propertyAttributes";
    public static final String PAGE_MODIFIED = "pageModified";
    private static final String AUTHENTICATION_CHAIN =
            "AuthenticationChain";
    static final String BTN_STS_EXPORT_POLICY = "btnSTSExportPolicy";

    /**
     * Creates a authentication domains view bean.
     */
    public SecurityTokenServiceViewBean() {
        super("SecurityTokenService",
                DEFAULT_DISPLAY_URL,
                "sunFAMSTSService");
    }

    protected void initialize() {
        if (!initialized) {
            super.initialize();
            initialized = true;
            createPropertyModel();
            createPageTitleModel();
            registerChildren();
        }
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(BTN_STS_EXPORT_POLICY, CCButton.class);
    }

    protected View createChild(String name) {
        return super.createChild(name);
    }

    public void beginDisplay(DisplayEvent event)
            throws ModelControlException {
        try {
            super.beginDisplay(event);

            Map values = getAttributeValues();
            String authChains = getValueFromMap(values, AUTHENTICATION_CHAIN);
            if (authChains == null) {
                authChains = "";
            }
            CCSelectableList cb = (CCSelectableList) getChild(
                    AUTHENTICATION_CHAIN);
            cb.setOptions(getAuthChainOptionList());
            propertySheetModel.setValue(AUTHENTICATION_CHAIN, authChains);

            setSignedElements(values);
            setEncryptionFlag(values);
            if (!isInlineAlertMessageSet()) {
                String flag = (String) getPageSessionAttribute(PAGE_MODIFIED);
                if ((flag != null) && flag.equals("1")) {
                    setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                            "message.profile.modified");
                }
            }
        } catch (AMConsoleException ex) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    ex.getMessage());
        }
    }

    void setEncryptionFlag(Map values) {
        Set set = (Set) values.get("isRequestEncrypt");
        String isrequestencrypted = ((set != null) && !set.isEmpty()) ? (String) set.iterator().next() : "";

        set = (Set) values.get("isRequestHeaderEncrypt");
        String isRequestHeaderEncrypt = ((set != null) && !set.isEmpty()) ? (String) set.iterator().next() : "";

        if (((isrequestencrypted != null) && (isrequestencrypted.equals("true"))) || ((isRequestHeaderEncrypt != null) && (isRequestHeaderEncrypt.equals("true")))) {
            propertySheetModel.setValue("isRequestEncryptedEnabled", "true");
        }
    }

    void setSignedElements(Map values) {
        Set set = (Set) values.get("SignedElements");
        if( set != null ) {
            ConfiguredSignedElements configuredSignedElements = new ConfiguredSignedElements();
            Map map = configuredSignedElements.getChoiceValues();
            String isresponsesigned = (String) values.get("isrequestsigned");

            if((set.isEmpty() || set.size()==0 )
                    && (isresponsesigned != null) && (isresponsesigned.equals("true"))){
                propertySheetModel.setValue("Body", "true");
            } else {
                Iterator it = map.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pairs = (Map.Entry)it.next();               
                    if (set.contains(pairs.getKey())) {
                        propertySheetModel.setValue(pairs.getKey().toString(), "true");
                    }
                }            
            }
        }
    }

    private void getSignedElements(Map values) {
        String val = null;
        Set set = new HashSet();
        ConfiguredSignedElements configuredSignedElements = new ConfiguredSignedElements();
        Map map = configuredSignedElements.getChoiceValues();
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            val = (String) propertySheetModel.getValue(pairs.getKey().toString());
            if (val.equals("true")) {
                set.add(pairs.getValue());
            }
        }

        val = (String) propertySheetModel.getValue("isResponseSign");
        if (val.equals("true") && set.isEmpty()) {
            set.add("Body");
        }
        values.put("SignedElements", set);

    }

    private OptionList getAuthChainOptionList()
            throws AMConsoleException {
        Set config = ((SecurityTokenServiceModel) getModel()).getAuthenticationChains();
        OptionList optList = new OptionList();
        if ((config != null) && !config.isEmpty()) {
            for (Iterator iter = config.iterator(); iter.hasNext();) {
                String c = (String) iter.next();
                optList.add(c, c);
            }
        }
        return optList;
    }

    private String getValueFromMap(Map attrValues, String name) {
        Set set = (Set) attrValues.get(name);
        return ((set != null) && !set.isEmpty()) ? (String) set.iterator().next() : "";
    }

    protected void createPageTitleModel() {
        createThreeButtonPageTitleModel();
    }

    protected AMModel getModelInternal() {
        HttpServletRequest req =
                RequestManager.getRequestContext().getRequest();
        try {
            return new SecurityTokenServiceModelImpl(
                    req, getPageSessionAttributes());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
        return null;
    }

    protected void createPropertyModel() {
        String xmlFileName = "com/sun/identity/console/propertySecurityTokenService.xml";
        String xml = AMAdminUtils.getStringFromInputStream(
                getClass().getClassLoader().getResourceAsStream(xmlFileName));

        propertySheetModel = new AMPropertySheetModel(xml);
        propertySheetModel.clear();
    }

    /**
     * Handles save button request.
     * save
     * @param event Request invocation event
     */
    public void handleButton1Request(RequestInvocationEvent event)
            throws ModelControlException {
        removePageSessionAttribute(PAGE_MODIFIED);
        /*super.handleButton1Request(event);*/

        submitCycle = true;
        AMServiceProfileModel model = (AMServiceProfileModel) getModel();

        if (model != null) {
            try {
                Map values = getValues();
                onBeforeSaveProfile(values);
                getSignedElements(values);
                model.setAttributeValues(values);
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                        "message.updated");
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        e.getMessage());
            }
        }
        forwardTo();
    }

    /**
     * Handles page cancel request.
     * 
     * @param event Request invocation event
     */
    public void handleButton2Request(RequestInvocationEvent event)
            throws ModelControlException {
        removePageSessionAttribute(PAGE_MODIFIED);
        super.handleButton2Request(event);
    }

    /**
     * Handles reset request.
     *
     * @param event Request invocation event
     */
    public void handleButton3Request(RequestInvocationEvent event)
            throws ModelControlException, AMConsoleException {
        removePageSessionAttribute(PAGE_MODIFIED);
        backTrail();
        try {
            String name = (String) getPageSessionAttribute(
                    AMAdminConstants.SAVE_VB_NAME);
            SCConfigViewBean vb = (SCConfigViewBean) getViewBean(
                    Class.forName(name));
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (ClassNotFoundException e) {
            debug.warning(
                    "SecurityTokenServiceViewBean.handleButton3Request:", e);
        }
    }

    public void handleBtnSTSExportPolicyRequest(RequestInvocationEvent event) throws ModelControlException {
        STSExportPolicyViewBean vb = (STSExportPolicyViewBean) getViewBean(STSExportPolicyViewBean.class);
        getViewBean(STSExportPolicyViewBean.class);
        vb.setPageSessionAttribute(STSExportPolicyViewBean.PG_ATTR_CONFIG_PAGE, getClass().getName());
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
}
