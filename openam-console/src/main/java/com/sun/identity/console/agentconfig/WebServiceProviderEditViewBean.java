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
 * $Id: WebServiceProviderEditViewBean.java,v 1.10 2009/12/19 00:06:54 asyhuang Exp $
 *
 */

package com.sun.identity.console.agentconfig;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.ChildContentDisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.agentconfig.model.AgentsModel;
import com.sun.identity.console.agentconfig.model.WSSAttributeNames;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.components.view.html.SerializedField;
import com.sun.web.ui.model.CCActionTableModel;
import com.sun.web.ui.model.CCActionTableModelInterface;
import com.sun.web.ui.model.CCEditableListModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.editablelist.CCEditableList;
import com.sun.web.ui.view.html.CCButton;
import com.sun.web.ui.view.html.CCSelectableList;
import com.sun.web.ui.view.table.CCActionTable;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Customized view bean for WSP.
 */
public class WebServiceProviderEditViewBean 
    extends WebServiceEditViewBean {
    private static final String EDIT_LINK_TRACKER = "WebServiceEditTracker";
    private static final String PAGE_NAME = "WebServiceProviderEdit";
    private static final String TBL_USER_CRED = "tblUserCredential";
    
    // table
    private static final String TBL_BUTTON_ADD = "tblButtonAdd";
    private static final String TBL_BUTTON_DELETE = "tblButtonDelete";
    private static final String TBL_COL_NAME = "tblColName";
    private static final String TBL_COL_PWD = "tblColPassword";
    private static final String TBL_DATA_ACTION_HREF = "tblDataActionHref";
    private static final String TBL_DATA_NAME = "tblDataName";
    private static final String TBL_DATA_PWD = "tblDataPassword";

    private static final String CHILD_NAME_AUTH_CHAIN = "authenticationchain";
    private static final String CHILD_NAME_TOKEN_CONV_TYPE = 
        "tokenconversiontype";
    private static final String CHILD_NAME_SAML_ATTR_MAPPING =
        "SAMLAttributeMapping";
    
    static final String DEFAULT_DISPLAY_URL =
        "/console/agentconfig/WebServiceProviderEdit.jsp";
    private static Map attrToChildNames = new HashMap();

    private Set providerUIProperties = parseExternalizeUIProperties(
        "webServiceProviderUI");
    private CCActionTableModel tblUserCredential;

    static final String BTN_EXPORT_POLICY = "btnExportPolicy";

    static {
        attrToChildNames.put("userpassword", "userpassword");
        attrToChildNames.put("WSPProxyEndpoint", "wssproxyEndPoint");
        attrToChildNames.put("privateKeyAlias", "certalias");
        attrToChildNames.put("privateKeyType", "keyType");
        attrToChildNames.put("publicKeyAlias", "publicKeyAlias");
        attrToChildNames.put("WSPEndpoint", "wspendpoint");
        attrToChildNames.put("sunIdentityServerDeviceStatus",
            "sunIdentityServerDeviceStatus");
        attrToChildNames.put("serviceType", "libertyservicetype");
        attrToChildNames.put("DnsClaim", "DnsClaim");
        attrToChildNames.put("authenticationChain", "authenticationchain");
        attrToChildNames.put("TokenConversionType", "tokenconversiontype");
        attrToChildNames.put("isResponseSign", "isresponsesigned");
        attrToChildNames.put("isResponseEncrypt", "isresponsedecrypted");
        attrToChildNames.put("isRequestHeaderEncrypt",
            "isRequestHeaderEncrypt");
        attrToChildNames.put("keepSecurityHeaders", "preservesecurityheader");
        attrToChildNames.put("isRequestSign", "isrequestsigned");
        attrToChildNames.put("useDefaultStore", "keystoreusage");
        attrToChildNames.put("isRequestEncrypt", "isrequestencrypted");
        attrToChildNames.put("SecurityMech", "SecurityMech");
        attrToChildNames.put("UserCredential", "UserCredential");
        attrToChildNames.put("SAMLAttributeMapping", "SAMLAttributeMapping");
        attrToChildNames.put("DetectMessageReplay", "detectmessagereplay");
        attrToChildNames.put("DetectUserTokenReplay", "detectusertokenreplay");
    }
    
    /**
     * Creates an instance of this view bean.
     */
    public WebServiceProviderEditViewBean() {
        super(PAGE_NAME, DEFAULT_DISPLAY_URL, false,
            "com/sun/identity/console/propertyWebServiceProviderEdit.xml");
        createTableModel();
    }
    
    protected void registerChildren() {
        super.registerChildren();
        registerChild(TBL_USER_CRED, CCActionTable.class);
        registerChild(BTN_EXPORT_POLICY, CCButton.class);
        tblUserCredential.registerChildren(this);
    }

    protected View createChild(String name) {
        View view = null;
        if (tblUserCredential.isChildSupported(name)) {
            view = tblUserCredential.createChild(this, name);
        } else if (name.equals(TBL_USER_CRED)) {
            SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
            populateTableModel((List)szCache.getSerializedObj());
            view = new CCActionTable(this, tblUserCredential, name);
        } else {
            view = super.createChild(name);
        }
        return view;
    }

    protected void setExtendedDefaultValues(Map values)
        throws AMConsoleException {
        populateTableModel(getUserCredentials(values));
        String authChains = getValueFromMap(values, 
            WSSAttributeNames.AUTH_CHAIN);
        if (authChains == null) {
            authChains = "";
        }

        String tokenConversionType = getValueFromMap(values, 
            WSSAttributeNames.TOKEN_CONVERSION_TYPE);
        if (tokenConversionType == null) {
            tokenConversionType = "";
        }

        if (!inheritedPropertyNames.contains(WSSAttributeNames.AUTH_CHAIN)) {
            CCSelectableList cb = (CCSelectableList)getChild(
                CHILD_NAME_AUTH_CHAIN);
            cb.setOptions(getAuthChainOptionList());
        }
        propertySheetModel.setValue(CHILD_NAME_AUTH_CHAIN, authChains);
        
        if (!inheritedPropertyNames.contains(
            WSSAttributeNames.TOKEN_CONVERSION_TYPE)) {
            CCSelectableList cb = (CCSelectableList)getChild(
                CHILD_NAME_TOKEN_CONV_TYPE);
            cb.setOptions(getTokenConversionTypeOptionList());
        }
        propertySheetModel.setValue(CHILD_NAME_TOKEN_CONV_TYPE, 
            tokenConversionType);
        
        if (!inheritedPropertyNames.contains(
            WSSAttributeNames.SAML_ATTR_MAPPING)) {
            CCEditableList list = (CCEditableList)getChild(
                CHILD_NAME_SAML_ATTR_MAPPING);
            CCEditableListModel m = (CCEditableListModel)list.getModel();
            list.resetStateData();
            m.setOptionList((Set)values.get(
                WSSAttributeNames.SAML_ATTR_MAPPING));
        }

        setExternalizeUIValues(providerUIProperties, values);
        setPageSessionAttribute(EDIT_LINK_TRACKER, (Serializable)values);
    }

    private OptionList getAuthChainOptionList()
        throws AMConsoleException {
        Set config = ((AgentsModel)getModel()).getAuthenticationChains();
        OptionList optList = new OptionList();
        if ((config != null) && !config.isEmpty()) {
            for (Iterator iter = config.iterator(); iter.hasNext(); ) {
                String c = (String)iter.next();
                optList.add(c, c);
            }
        }
        return optList;
    }
    
    private OptionList getTokenConversionTypeOptionList()
        throws AMConsoleException {
        List config = ((AgentsModel)getModel()).getTokenConversionTypes();
        return createOptionList(config);
    }
    
    private void populateTableModel(List list) {
        Map map = new HashMap();
        
        if (list != null) {
            for (Iterator i = list.iterator(); i.hasNext(); ) {
                String uc = (String)i.next();
                String[] userpwd = splitUserCredToken(uc);
                if (userpwd != null) {
                    map.put(userpwd[0], userpwd[1]);
                }
            }
        }
        populateTableModel(map);
    }
    
    private void populateTableModel(Map nameToPassword) {
        tblUserCredential.clearAll();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        boolean inheriting = inheritedPropertyNames.contains(
            WSSAttributeNames.USERCREDENTIAL);
        
        if (inheriting) {
            tblUserCredential.setSelectionType(
                CCActionTableModelInterface.NONE);
        }
        if ((nameToPassword != null) && !nameToPassword.isEmpty()) {
            boolean firstEntry = true;
            int counter = 0;
            List cache = new ArrayList(nameToPassword.size()); 
            
            for (Iterator i = nameToPassword.keySet().iterator(); i.hasNext();
                counter++
            ) {
                if (firstEntry) {
                    firstEntry = false;
                } else {
                    tblUserCredential.appendRow();
                }
                
                String name = (String)i.next();
                String token = WebServiceEditViewBean.formUserCredToken(
                    name, (String)nameToPassword.get(name));
                tblUserCredential.setSelectionVisible(counter, !inheriting);
                
                if (!inheriting) {
                    tblUserCredential.setValue(TBL_DATA_ACTION_HREF, token);
                } else {
                    tblUserCredential.setValue(TBL_DATA_ACTION_HREF, "");
                }
                
                tblUserCredential.setValue(TBL_DATA_NAME, name);

                // mask password
                tblUserCredential.setValue(TBL_DATA_PWD, "********");
                cache.add(token);
            }
            szCache.setValue((ArrayList)cache);
        } else {
            szCache.setValue(null);
        }
    }

    /**
     * Removed the anchor tag if user credential is to be inherit from group.
     *
     * @param event Child Content Display Event.
     * @return the manipulated HTML.
     */
    public String endTblDataActionHrefDisplay(ChildContentDisplayEvent event) {
        String value = (String)tblUserCredential.getValue(TBL_DATA_ACTION_HREF);
        String content = event.getContent();
        if (value.length() > 0) {
            return content;
        } else {
            int idx = content.indexOf(">");
            int idx1 = content.indexOf("</a>");
            return content.substring(idx+1, idx1);
        }
    }

    protected void getExtendedFormsValues(Map values)
        throws AMConsoleException {

        String authChain = (String)propertySheetModel.getValue(
            CHILD_NAME_AUTH_CHAIN);
        Set setAuthChain = new HashSet(2);
        if ((authChain != null) && (authChain.length() > 0)) {
            setAuthChain.add(authChain);
        }
        values.put(WSSAttributeNames.AUTH_CHAIN, setAuthChain);

        String tokenConversionType = (String)propertySheetModel.getValue(
            CHILD_NAME_TOKEN_CONV_TYPE);
        Set setTokenConversionType = new HashSet(2);
        if ((tokenConversionType != null) && 
            (tokenConversionType.length() > 0)
        ) {
            setTokenConversionType.add(tokenConversionType);
        }
        values.put(WSSAttributeNames.TOKEN_CONVERSION_TYPE, 
            setTokenConversionType);
        
        CCEditableList elist  = (CCEditableList)getChild(
            CHILD_NAME_SAML_ATTR_MAPPING);
        elist.restoreStateData();
        Set samlAttrMapping = getValues(elist.getModel().getOptionList());
        values.put(WSSAttributeNames.SAML_ATTR_MAPPING, samlAttrMapping);

        getExternalizeUIValues(providerUIProperties, values);

        if (!inheritedPropertyNames.contains(WSSAttributeNames.USERCREDENTIAL)){
            try {
                CCActionTable table = (CCActionTable)getChild(TBL_USER_CRED);
                table.restoreStateData();
                
                SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
                List list = (List)szCache.getSerializedObj();
                Set set = new HashSet(2);
                
                if ((list != null) && !list.isEmpty()) {
                    StringBuffer buff = new StringBuffer();
                    boolean first = true;
                    for (int i = 0; i < list.size(); i++) {
                        if (!first) {
                            buff.append(",");
                        } else {
                            first = false;
                        }
                        buff.append((String)list.get(i));
                    }
                    set.add(buff.toString());
                }
                values.put(WSSAttributeNames.USERCREDENTIAL, set);
            } catch (ModelControlException ex) {
                throw new AMConsoleException(ex.getMessage());
            }
        }
    }
    
    private void createTableModel() {
        tblUserCredential = new CCActionTableModel(
            getClass().getClassLoader().getResourceAsStream(
            "com/sun/identity/console/tblWebServiceUserCred.xml"));
        tblUserCredential.setTitleLabel("label.items");
        tblUserCredential.setActionValue(TBL_BUTTON_ADD, 
            "web.services.profile.username-token-tbl-add-btn");
        tblUserCredential.setActionValue(TBL_BUTTON_DELETE, 
            "web.services.profile.username-token-tbl-remove-btn");
        tblUserCredential.setActionValue(TBL_COL_NAME, 
            "web.services.profile.username-token-tbl-col-name");
        tblUserCredential.setActionValue(TBL_COL_PWD,
            "web.services.profile.username-token-tbl-password-name");
    }

    /**
     * Forwards request to add user credential view bean.
     *
     * @param event Request Invocation Event.
     */
    public void handleTblButtonAddRequest(RequestInvocationEvent event) 
        throws ModelControlException
    {
        try {
            Map values = getFormValues();
            String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
            AgentsModel model = (AgentsModel)getModel();
            String realm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);
            values.putAll(model.getAgentGroupValues(realm,
                universalId, inheritedPropertyNames));
            setPageSessionAttribute(TRACKER_ATTR, (Serializable)values);
            WebServiceUserCredAddViewBean vb = (WebServiceUserCredAddViewBean)
                getViewBean(WebServiceUserCredAddViewBean.class);
            unlockPageTrail();
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
            forwardTo();
        }
    }

    public void handleBtnExportPolicyRequest(RequestInvocationEvent event) throws ModelControlException {
        AgentExportPolicyViewBean vb =(AgentExportPolicyViewBean)getViewBean(AgentExportPolicyViewBean.class);
        getViewBean(AgentExportPolicyViewBean.class);
        vb.setPageSessionAttribute(AgentExportPolicyViewBean.PG_ATTR_CONFIG_PAGE, getClass().getName());
        passPgSessionMap(vb);
        vb.forwardTo(getRequestContext());
    }
    
    /**
     * Handles edit user token credential request.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblDataActionHrefRequest(RequestInvocationEvent event)
        throws ModelControlException
    {
        Map attrValues = (Map)removePageSessionAttribute(EDIT_LINK_TRACKER);
        setPageSessionAttribute(TRACKER_ATTR, (Serializable)attrValues);
        WebServiceUserCredEditViewBean vb = (WebServiceUserCredEditViewBean)
            getViewBean(WebServiceUserCredEditViewBean.class);
        unlockPageTrail();
        passPgSessionMap(vb);
        String token = (String)getDisplayFieldValue(TBL_DATA_ACTION_HREF);
        vb.setDisplayFieldValue(WebServiceUserCredEditViewBean.HIDDEN_TOKEN,
            token);
        String[] userpwd = splitUserCredToken(token);
        vb.setDisplayFieldValue("username", userpwd[0]);
        vb.setDisplayFieldValue("password", userpwd[1]);
        vb.forwardTo(getRequestContext());
    }


    /**
     * Deletes user token credential.
     *
     * @param event Request Invocation Event.
     * @throws ModelControlException if table model cannot be restored.
     */
    public void handleTblButtonDeleteRequest(RequestInvocationEvent event)
        throws ModelControlException
    {
        submitCycle = true;
        CCActionTable table = (CCActionTable)getChild(TBL_USER_CRED);
        table.restoreStateData();
        tblUserCredential = (CCActionTableModel)table.getModel();
        Integer[] selected = tblUserCredential.getSelectedRows();
        SerializedField szCache = (SerializedField)getChild(SZ_CACHE);
        List list = (List)szCache.getSerializedObj();
        Set tokens = new HashSet(selected.length *2);
        
        for (int i = 0; i < selected.length; i++) {
            String sel = (String)list.get(selected[i].intValue());
            String[] userpwd = splitUserCredToken(sel);
            tokens.add(userpwd[0]);
        }

        try {
            Map map = getFormValues();
            WebServiceEditViewBean.removeUserCredTokenAttr(tokens, map);
            populateTableModel(getUserCredentials(map));
            setPageSessionAttribute(TRACKER_ATTR, (Serializable)map);
            setInlineAlertMessage(CCAlert.TYPE_INFO, "message.warning",
                "message.profile.modified");
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
        forwardTo();
    }
    
    protected Map getAttrToChildNamesMapping() {
        return attrToChildNames;
    }
    
    protected String handleReadonlyAttributes(String xml) {
        xml = super.handleReadonlyAttributes(xml);
        if (inheritedPropertyNames.contains(WSSAttributeNames.USERCREDENTIAL)) {
            disableButton(TBL_BUTTON_ADD, true);
            disableButton(TBL_BUTTON_DELETE, true);
        }
        return xml;
    }
    
    public String endPropertyAttributesDisplay(
        ChildContentDisplayEvent event
    ) {
        String html = event.getContent();
        int idx = html.indexOf("<a name=\"lblsaml\"");
        html = html.substring(0, idx) + 
            "<div id=\"samlconf\" style=\"display:none\">" + 
            html.substring(idx);
        idx = html.indexOf("<a name=\"lblsignencrypt\"");
        html = html.substring(0, idx) + "</div>" +  html.substring(idx);
        return html;
    }
} 
