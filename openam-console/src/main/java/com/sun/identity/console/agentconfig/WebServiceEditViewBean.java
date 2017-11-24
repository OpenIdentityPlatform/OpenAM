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
 * $Id: WebServiceEditViewBean.java,v 1.7 2009/12/10 17:14:02 ggennaro Exp $
 *
 */

package com.sun.identity.console.agentconfig;

import com.iplanet.jato.RequestContext;
import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.ChildDisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.AMPropertySheet;
import com.sun.identity.console.base.model.AMAdminConstants;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.base.model.AMModel;
import com.sun.identity.console.base.model.AMPropertySheetModel;
import com.sun.identity.console.agentconfig.model.AgentsModel;
import com.sun.identity.console.agentconfig.model.WSSAttributeNames;
import com.sun.identity.console.base.AMViewBeanBase;
import com.sun.identity.wss.security.ConfiguredSignedElements;
import com.sun.web.ui.model.CCNavNode;
import com.sun.web.ui.view.alert.CCAlert;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;

/**
 * Base class for all WSS customized View Bean.
 */
public abstract class WebServiceEditViewBean 
    extends AgentProfileViewBean {
    static final String TRACKER_ATTR = "pgWebServiceTracker";
    static final String CHILD_NAME_SECURITY_MECH = "SecurityMech";
    static final String SECURITY_MECH_PREFIX = "securitymech-";
    static final String CHILD_NAME_KEYSTORE_USAGE = "keystoreusage";
    static final String CHILD_NAME_KEY_STORE_LOCATION = "keystorelocation";
    static final String CHILD_NAME_KEY_STORE_PASSWORD = "keystorepassword";
    static final String CHILD_NAME_KEY_PASSWORD = "keypassword";
    static final String CHILD_NAME_CERT_ALIAS = "certalias";

    static final String CHILD_NAME_USERTOKEN_NAME = "usernametokenname";
    static final String CHILD_NAME_USERTOKEN_PASSWORD = "usernametokenpassword";
    
    protected static final String PROPERTY_ATTRIBUTE = "propertyAttributes";
    
    private Set externalizeUIProperties = parseExternalizeUIProperties(
        "webServiceUI");
    private boolean isWebClient;
    private String pageName;
    private String xmlFileName;
    
    WebServiceEditViewBean(
        String pageName, 
        String defaultURL,
        boolean isWebClient,
        String xml
   ) {
        super(pageName);
        setDefaultDisplayURL(defaultURL);
        this.pageName = pageName;
        this.isWebClient = isWebClient;
        xmlFileName = xml;
    }
    
    protected void setSelectedTabNode(String realmName) {
        String strID = (String)getPageSessionAttribute(getTrackingTabIDName());
        int id = GenericAgentProfileViewBean.TAB_GENERAL_ID;

        if ((strID == null) || (strID.trim().length() == 0)) {
            HttpServletRequest req = getRequestContext().getRequest();
            strID = req.getParameter(getTrackingTabIDName());
            setPageSessionAttribute(getTrackingTabIDName(), strID);
        }

        if ((strID != null) && (strID.trim().length() > 0)) {
            id = Integer.parseInt(strID);
            tabModel.clear();
            tabModel.setSelectedNode(id);
        }
    }
    
    protected void createTabModel() {
        String agentType = getAgentType();

        if (agentType != null) {
            super.createTabModel();
            tabModel.addNode(new CCNavNode(
                GenericAgentProfileViewBean.TAB_GENERAL_ID, 
                "tab.general", "", ""));

            if (isGroup) {
                tabModel.addNode(new CCNavNode(
                    GenericAgentProfileViewBean.TAB_GROUP_ID, 
                    "tab.group", "", ""));
            }

        }
    }

    /**
     * Returns <code>true</code> if there are more than one tab to display.
     *
     * @param event Child Display Event.
     * @return <code>true</code> if there are more than one tab to display.
     */
    public boolean beginTabCommonDisplay(ChildDisplayEvent event) {
        return (tabModel.getNodeCount() > 1);
    }

    /**
     * Handles tab selection.
     *
     * @param event Request Invocation Event.
     * @param nodeID Tab ID.
     */
    public void nodeClicked(RequestInvocationEvent event, int nodeID) {
        String agentType = getAgentType();
        AgentTabManager mgr = AgentTabManager.getInstance();

        if (nodeID == 4600) {
            removePageSessionAttribute(GenericAgentProfileViewBean.PS_TABNAME);
            setPageSessionAttribute(getTrackingTabIDName(),
                Integer.toString(nodeID));
            try {
                Class clazz = AgentsViewBean.getAgentCustomizedViewBean(
                    agentType);
                AMViewBeanBase vb = (AMViewBeanBase)getViewBean(clazz);
                passPgSessionMap(vb);
                vb.forwardTo(getRequestContext());
            } catch (ClassNotFoundException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    getModel().getErrorString(e));
                forwardTo();
            }
        } else if (nodeID == 4601) {
            AgentGroupMembersViewBean vb =
                (AgentGroupMembersViewBean)getViewBean(
                    AgentGroupMembersViewBean.class);
            passPgSessionMap(vb);
            vb.forwardTo(getRequestContext());
        }
    }

    protected AMPropertySheetModel createPropertySheetModel(String type) {
        AMPropertySheetModel psModel = null;
        String xml = AMAdminUtils.getStringFromInputStream(
            getClass().getClassLoader().getResourceAsStream(xmlFileName));
        AgentsModel model = (AgentsModel)getModel();
        String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
        if (!inheritedPropertyNames.contains(WSSAttributeNames.SECURITY_MECH)){
            xml = xml.replaceAll("@securitymechanism@",getSecurityMechMarkup());
        }

        if (model.isAgentGroup(universalId)) {
            // remove the first property which is group drop down list.
            int start = xml.indexOf("<property>");
            int end = xml.indexOf("</property>");
            xml = xml.substring(0, start) + xml.substring(end + 12);
        }

        xml = handleReadonlyAttributes(xml);
        return new AMPropertySheetModel(xml);
    }

    private String getSecurityMechMarkup() {
        StringBuffer buff = new StringBuffer();
        Map displayNameToValue = new HashMap();
        Set securityMechs = getMessageLevelSecurityMech(displayNameToValue);
        for (Iterator i = securityMechs.iterator(); i.hasNext(); ) {
            String displayName = (String)i.next();
            String uri = (String)displayNameToValue.get(displayName);
            Object[] params = new String[2];

            if (!isWebClient) {
                params[0] = SECURITY_MECH_PREFIX + uri;
                params[1] = displayName;
                buff.append(MessageFormat.format(PROPERTY_CHECKBOX_TEMPLATE,
                    params));
            } else {
                params[0] = displayName;
                params[1] = uri;
                buff.append(MessageFormat.format(PROPERTY_OPTION_TEMPLATE,
                    params));
            }
        }
        return buff.toString();
    }
    
    private Set getMessageLevelSecurityMech(Map displayNameToValue) {
        Set i18nKey = new TreeSet();
        String agentType = getAgentType();
        AgentsModel model = (AgentsModel)getModel();
        Map choiceValues = model.getSecurityMechanisms(agentType);
        i18nKey.addAll(choiceValues.keySet());
        displayNameToValue.putAll(choiceValues);
        return i18nKey;
    }
    
    protected void setDefaultValues(String type)
        throws AMConsoleException {
        if (propertySheetModel != null) {
            AgentsModel model = (AgentsModel)getModel();
            String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
            String realm = (String)getPageSessionAttribute(
                AMAdminConstants.CURRENT_REALM);

            try {
                Map attrValues = (Map)removePageSessionAttribute(
                    TRACKER_ATTR);
                Map inheritedValues = null;
                if (attrValues == null) {
                    attrValues = model.getAttributeValues(realm, 
                        universalId, true);
                }
                if (!isGroup) {
                    if (!inheritedPropertyNames.isEmpty()) {
                        inheritedValues = model.getAgentGroupValues(realm,
                            universalId, inheritedPropertyNames);
                        attrValues.putAll(inheritedValues);
                        if (inheritedPropertyNames.contains(
                            WSSAttributeNames.PASSWORD))
                        {
                            Set set = new HashSet(2);
                            set.add("*********");
                            attrValues.put(WSSAttributeNames.PASSWORD, set);
                        }
                    }
                }

                if (!submitCycle) {
                    propertySheetModel.clear();
                    AMPropertySheet prop = (AMPropertySheet)getChild(
                        PROPERTY_ATTRIBUTE);
                    prop.setAttributeValues(attrValues, model);
                    Set securityMechs = (Set)attrValues.get(
                        WSSAttributeNames.SECURITY_MECH);
                    
                    setSecurityMech(securityMechs);
                    setExternalizeUIValues(externalizeUIProperties, attrValues);
                    setKeyStoreInfo(attrValues);
                    setSignedElements(attrValues);
                    setEncryptionFlag(attrValues);
                } else if ((inheritedValues != null) && 
                    !inheritedValues.isEmpty()
                ){
                    AMPropertySheet prop = (AMPropertySheet)getChild(
                        PROPERTY_ATTRIBUTE);
                    prop.setAttributeValues(inheritedValues, model);
                    Set securityMechs = (Set)attrValues.get(
                        WSSAttributeNames.SECURITY_MECH);
                    
                    setSecurityMech(securityMechs);
                    setExternalizeUIValues(externalizeUIProperties, attrValues);
                    setKeyStoreInfo(attrValues);
                }
                
                setExtendedDefaultValues(attrValues);
            } catch (AMConsoleException e) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
            }

            String[] uuid = {universalId};
            propertySheetModel.setValues(PROPERTY_UUID, uuid, model);
        }
    }

    void setEncryptionFlag(Map values) {         
         Set set = (Set)values.get("isRequestEncrypt");
         String isrequestencrypted = ((set != null) && !set.isEmpty()) ?
             (String)set.iterator().next() : "";
        
         set = (Set)values.get("isRequestHeaderEncrypt");
         String isRequestHeaderEncrypt = ((set != null) && !set.isEmpty()) ?
             (String)set.iterator().next() : "";

         if(((isrequestencrypted != null) && (isrequestencrypted.equals("true")))
             || ((isRequestHeaderEncrypt != null) && (isRequestHeaderEncrypt.equals("true"))))
         {
             propertySheetModel.setValue("isRequestEncryptedEnabled", "true");
         }
    }
    
    void setSignedElements(Map values) {       
        Set set = (Set)values.get("isrequestsigned");
        String isresponsesigned = ((set != null) && !set.isEmpty()) ?
             (String)set.iterator().next() : "";

        set = (Set) values.get("SignedElements");
        
        if( set != null ) { 
            ConfiguredSignedElements configuredSignedElements = new ConfiguredSignedElements();
            Map map = configuredSignedElements.getChoiceValues();

            if(( set.isEmpty() || set.size()==0 ) 
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

    private void getSignedElements(Map values){
        String val=null;
        Set set = new HashSet();
        ConfiguredSignedElements configuredSignedElements = new ConfiguredSignedElements();
        Map map = configuredSignedElements.getChoiceValues();
        Iterator it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();           
            val = (String)propertySheetModel.getValue(pairs.getKey().toString());
            if(val.equals("true")){
                set.add(pairs.getValue());
            }
        }  

        val = (String)propertySheetModel.getValue("isrequestsigned");
        if(val.equals("true") && set.isEmpty()) {
             set.add("Body");
        }
        values.put("SignedElements", set);

    }

    void setExternalizeUIValues(Set extUIs, Map values) {
        for (Iterator i = extUIs.iterator(); i.hasNext(); ) {
            WebServiceUIElement elm = (WebServiceUIElement)i.next();
            Set set = (Set)values.get(elm.attrName);
            String value = ((set != null) && !set.isEmpty()) ? 
                (String)set.iterator().next() : "";
            propertySheetModel.setValue(elm.childName, value); 
        }
    }
    
    void getExternalizeUIValues(Set extUIs, Map values) {
        for (Iterator i = extUIs.iterator(); i.hasNext(); ) {
            WebServiceUIElement elm = (WebServiceUIElement)i.next();

            if (elm.attrType.equals(WebServiceUIElement.TYPE_BOOL)) {
                String val = (String)propertySheetModel.getValue(elm.childName);
                if (val != null) {
                    Set set = new HashSet(2);
                    if (val.equals("true")) {
                        set.add("true");
                    } else {
                        set.add("false");
                    }
                    values.put(elm.attrName, set);
                }
            } else if (elm.attrType.equals(WebServiceUIElement.TYPE_TEXT)) {
                String val = (String)propertySheetModel.getValue(elm.childName);
                if (val != null) {
                    Set set = new HashSet(2);
                    if ((val != null) && val.length() > 0) {
                        set.add(val);
                    }
                    values.put(elm.attrName, set);
                }
            }
        }   
    }
    
    private void setKeyStoreInfo(Map values) {
        String useDefaultKeyStore = getValueFromMap(values, 
            WSSAttributeNames.USE_DEFAULT_KEYSTORE);
        propertySheetModel.setValue(CHILD_NAME_CERT_ALIAS, 
            getValueFromMap(values, WSSAttributeNames.CERT_ALIAS));

        if ((useDefaultKeyStore != null) && 
            useDefaultKeyStore.equals("true")
        ) {
            propertySheetModel.setValue(CHILD_NAME_KEYSTORE_USAGE, "default"); 
        } else {
            propertySheetModel.setValue(CHILD_NAME_KEYSTORE_USAGE, "custom"); 
            propertySheetModel.setValue(CHILD_NAME_KEY_STORE_LOCATION, 
                getValueFromMap(values, WSSAttributeNames.KEY_STORE_LOCATION));
            propertySheetModel.setValue(CHILD_NAME_KEY_STORE_PASSWORD, 
                getValueFromMap(values, WSSAttributeNames.KEY_STORE_PASSWORD));
            propertySheetModel.setValue(CHILD_NAME_KEY_PASSWORD, 
                getValueFromMap(values, WSSAttributeNames.KEY_PASSWORD));
        }
    }
    
    private void setSecurityMech(Set values) {
        if ((values != null) && !values.isEmpty()) {
            if (isWebClient) {
                propertySheetModel.setValue(CHILD_NAME_SECURITY_MECH, 
                    (String)values.iterator().next());
            } else {
                if (!inheritedPropertyNames.contains(
                    WSSAttributeNames.SECURITY_MECH)){
                    for (Iterator i = values.iterator(); i.hasNext(); ) {
                        String uri = (String)i.next();
                        if (uri.length() > 0) {
                            propertySheetModel.setValue(
                                SECURITY_MECH_PREFIX + uri, "true");
                        }
                    }
                } else {
                    String strValues = values.toString();
                    strValues = strValues.substring(1, strValues.length() -1);
                    propertySheetModel.setValue(CHILD_NAME_SECURITY_MECH,
                        strValues);
                }
            }
        }
    }

    protected Map getFormValues()
        throws AMConsoleException, ModelControlException {
        AgentsModel model = (AgentsModel)getModel();
        String realm = (String)getPageSessionAttribute(
            AMAdminConstants.CURRENT_REALM);
        AMPropertySheet prop = (AMPropertySheet)getChild(PROPERTY_ATTRIBUTE);
        String universalId = (String)getPageSessionAttribute(UNIVERSAL_ID);
        Map oldValues = model.getAttributeValues(realm, universalId, false);
        
        Map values = prop.getAttributeValues(oldValues.keySet());

        if (!isWebClient) {
            getSecurityMech(values);
        } else {
            String secMech = (String)propertySheetModel.getValue(
                CHILD_NAME_SECURITY_MECH);
            Set set = new HashSet(2);
            if ((secMech != null) && (secMech.length() > 0)) {
                set.add(secMech);
            }
            values.put(WSSAttributeNames.SECURITY_MECH, set);   
        }
        
        getExternalizeUIValues(externalizeUIProperties, values);
        getSignedElements(values);
        
        String useDefaultKeyStore = (String)propertySheetModel.getValue(
            CHILD_NAME_KEYSTORE_USAGE);

        if (useDefaultKeyStore != null) {
            String certAlias = (String)propertySheetModel.getValue(
                CHILD_NAME_CERT_ALIAS);
            values.put(WSSAttributeNames.CERT_ALIAS, putStringInSet(certAlias));

            if (useDefaultKeyStore.equals("default")) {
                Set set = new HashSet(2);
                set.add("true");
                values.put(WSSAttributeNames.USE_DEFAULT_KEYSTORE, set);
            } else {
                Set set = new HashSet(2);
                set.add("false");
                values.put(WSSAttributeNames.USE_DEFAULT_KEYSTORE, set);
                getkeyStoreInfo(values);
            }
        }

        getExtendedFormsValues(values);
        
        if (!isGroup && !inheritedPropertyNames.isEmpty()) {
            values.putAll(model.getAgentGroupValues(realm,
                universalId, inheritedPropertyNames));
        }
        return values;
    }
    
    
    static String formUserCredToken(String username, String password) {
        return WSSAttributeNames.USERCREDENTIAL_NAME + username.trim() + "|" +
            WSSAttributeNames.USERCREDENTIAL_PWD + password.trim();
    }

    static String[] splitUserCredToken(String token) {
        String username = null;
        String password = null;
        int idx = token.indexOf('|');

        if (idx > 0) {
            String part1 = token.substring(0, idx);
            String part2 = token.substring(idx+1);
            if (part1.startsWith(WSSAttributeNames.USERCREDENTIAL_NAME)) {
                username = part1.substring(
                    WSSAttributeNames.USERCREDENTIAL_NAME.length());
            } else if (part1.startsWith(WSSAttributeNames.USERCREDENTIAL_PWD)) {
                password = part1.substring(
                    WSSAttributeNames.USERCREDENTIAL_PWD.length());
            }
            if (part2.startsWith(WSSAttributeNames.USERCREDENTIAL_NAME)) {
                username = part2.substring(
                    WSSAttributeNames.USERCREDENTIAL_NAME.length());
            } else if (part2.startsWith(WSSAttributeNames.USERCREDENTIAL_PWD)) {
                password = part2.substring(
                    WSSAttributeNames.USERCREDENTIAL_PWD.length());
            }
        }

        if ((username != null) && (password != null)) {
            String[] temp = {username, password};
            return temp;
        }
        return null;
    }
    
    static void addToUserCredTokenAttr(
        String username, 
        String password,
        Map attrValues,
        AMModel model
    ) throws AMConsoleException {
        Map map = getUserCredentials(attrValues);
        if (map.keySet().contains(username)) {
            throw new AMConsoleException(model.getLocalizedString(
                "web.services.profile.error-user-cred-exists"));
        }
        map.put(username, password);
        attrValues.put(WSSAttributeNames.USERCREDENTIAL, 
            formatUserCredential(map));
    }

    static void replaceUserCredTokenAttr(
        String username,
        String password,
        Map attrValues
    ) throws AMConsoleException {
        Map map = getUserCredentials(attrValues);
        map.put(username, password);
        attrValues.put(WSSAttributeNames.USERCREDENTIAL, 
            formatUserCredential(map));
    }

    static void removeUserCredTokenAttr(String username, Map attrValues) {
        Map map = getUserCredentials(attrValues);
        map.remove(username);
        attrValues.put(WSSAttributeNames.USERCREDENTIAL,
            formatUserCredential(map));
    }

    static void removeUserCredTokenAttr(Set todelete, Map attrValues) {
        Map map = getUserCredentials(attrValues);
        for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            if (todelete.contains(key)) {
                i.remove();
            }
        }
        attrValues.put(WSSAttributeNames.USERCREDENTIAL,
            formatUserCredential(map));
    }

    static Map getUserCredentials(Map values) {
        Map mapNameToPassword = new HashMap();
        Set userCredentials = (Set)values.get(WSSAttributeNames.USERCREDENTIAL);

        if ((userCredentials != null) && !userCredentials.isEmpty()){
	    String userCred = (String)userCredentials.iterator().next();
	    StringTokenizer st = new StringTokenizer(userCred, ",");
	    while (st.hasMoreTokens()) {
                String uc = st.nextToken();
                String[] userpwd = splitUserCredToken(uc);
                if (userpwd != null) {
                    mapNameToPassword.put(userpwd[0], userpwd[1]);
                }
            }
        }
        return mapNameToPassword;
    }

    static Set formatUserCredential(Map map) {
        Set values = new HashSet();
        StringBuffer buff = new StringBuffer();
        boolean first = true;
        if (!map.isEmpty()) {
            for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
                String key = (String)i.next();
                String val = (String)map.get(key);
                if (first) {
                    first = false;
                } else {
                    buff.append(",");
                }
                buff.append(formUserCredToken(key, val));
            }
            values.add(buff.toString());
        }
        return values;
    }
    
    private void getkeyStoreInfo(Map values)
        throws AMConsoleException {
        String keyStoreLoc = (String)propertySheetModel.getValue(
            CHILD_NAME_KEY_STORE_LOCATION);
        String keyStorePwd = (String)propertySheetModel.getValue(
            CHILD_NAME_KEY_STORE_PASSWORD);
        String keyPwd = (String)propertySheetModel.getValue(
            CHILD_NAME_KEY_PASSWORD);

        if ((keyStoreLoc == null) || (keyStoreLoc.trim().length() == 0) ||
            (keyStorePwd == null) || (keyStorePwd.trim().length() == 0) ||
            (keyPwd == null) || (keyPwd.trim().length() == 0)
        ){
            throw new AMConsoleException(getModel().getLocalizedString(
                "web.services.profile.missing-keystore-info"));
        }
        
        values.put(WSSAttributeNames.KEY_STORE_LOCATION, 
            putStringInSet(keyStoreLoc));
        values.put(WSSAttributeNames.KEY_STORE_PASSWORD, 
            putStringInSet(keyStorePwd));
        values.put(WSSAttributeNames.KEY_PASSWORD, putStringInSet(keyPwd));
    }
    
    private void getSecurityMech(Map values) {
        HttpServletRequest req = getRequestContext().getRequest();
        StringBuffer buff = new StringBuffer();
        String prefix = pageName + "." + SECURITY_MECH_PREFIX;
        Set set = new HashSet();
        
        Map map = req.getParameterMap();
        for (Iterator i = map.keySet().iterator(); i.hasNext(); ) {
            String key = (String)i.next();
            if (key.startsWith(prefix) && !key.endsWith(".jato_boolean")) {
                key = key.substring(prefix.length());
                set.add(key);
            }
        }
        
        values.put(WSSAttributeNames.SECURITY_MECH, set);
    }
    
    static Set parseExternalizeUIProperties(String propName) {
        Set set = new HashSet();
        ResourceBundle bundle = ResourceBundle.getBundle(propName);
        for (Enumeration e = bundle.getKeys(); e.hasMoreElements(); ) {
            String key = (String)e.nextElement();
            if (key.endsWith(".attributeName")) {
                String childName = key.substring(0, key.length() - 14);
                String attrName = bundle.getString(key);
                String attrType = bundle.getString(
                    childName + ".attributeType");
                set.add(new WebServiceUIElement(childName, attrName, attrType));
            }
        }
        return set;
    }

    protected String handleReadonlyAttributes(String xml) {
        Set childNames = getChildNames(inheritedPropertyNames);

        for (Iterator i = childNames.iterator(); i.hasNext(); ) {
            String childName = (String)i.next();

            if (childName.equals(WSSAttributeNames.PASSWORD)) {
                int idx = xml.indexOf("<cc name=\"userpassword_confirm\"");
                int start = xml.lastIndexOf("<property ", idx);
                int end = xml.indexOf("</property>", idx);
                xml = xml.substring(0, start) + xml.substring(end+12);
            }

            int idx = xml.indexOf("<cc name=\"" + childName + "\"");
            if (idx != -1) {
                xml = makeReadOnly(xml, childName);
            } else if (childName.equals(WSSAttributeNames.SECURITY_MECH)) {
                idx = xml.indexOf("<ccgroup>@securitymechanism@</ccgroup>");
                if (idx != -1) {
                    Object[] param = {childName};
                    xml = xml.substring(0, idx) + 
                    MessageFormat.format(PROPERTY_STATIC_TEXT_TEMPLATE, param)+
                        xml.substring(idx +38);
                }
            }
        }

        return xml;
    }
    
    protected String makeReadOnly(String xml, String childName) {
        int idx = xml.indexOf("<cc name=\"" + childName + "\"");
        if (idx != -1) {
            int endIdx = xml.indexOf("</cc>", idx);
            Object[] param = {childName};
            xml = xml.substring(0, idx) +
                MessageFormat.format(PROPERTY_STATIC_TEXT_TEMPLATE, param)+
                xml.substring(endIdx+5);
        }
        return xml;
    }

    protected Set getChildNames(Set set) {
        Set childNames = new HashSet();
        if ((set != null) && !set.isEmpty()) {
            Map map = getAttrToChildNamesMapping();
            for (Iterator i = set.iterator(); i.hasNext(); ) {
                String name = (String)i.next();
                Object o = map.get(name);
                if (o != null) {
                    childNames.add(o);
                }
            }
        }
        return childNames;
    }

    protected HashSet getPropertyNames() {
        HashSet set = new HashSet();
        set.addAll(getAttrToChildNamesMapping().keySet());
        return set;
    }

    protected boolean isFirstTab() {
        return true;
    }

    protected boolean handleRealmNameInTabSwitch(RequestContext rc) {
        return false;
    }

    protected abstract Map getAttrToChildNamesMapping();
    
    protected abstract void getExtendedFormsValues(Map values)
        throws AMConsoleException;
    protected abstract void setExtendedDefaultValues(Map attrValues)
        throws AMConsoleException;

    private static final String PROPERTY_STATIC_TEXT_TEMPLATE = 
        "<cc name=\"{0}\" tagclass=\"com.sun.web.ui.taglib.html.CCStaticTextFieldTag\"/>";
    
    private static final String PROPERTY_CHECKBOX_TEMPLATE =
"<property><cc name=\"{0}\" tagclass=\"com.sun.web.ui.taglib.html.CCCheckBoxTag\"><attribute name=\"label\" value=\"{1}\" /><attribute name=\"extraHtml\" value=\"onclick=&quot;return showSAMLConfig();&quot;\" /></cc></property>";
    private static final String PROPERTY_OPTION_TEMPLATE =
        "<option label=\"{0}\" value=\"{1}\" />";
}
