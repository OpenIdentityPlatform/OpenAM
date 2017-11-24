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
 * $Id: WebServiceClientEditViewBean.java,v 1.4 2009/12/03 23:55:33 asyhuang Exp $
 *
 */

package com.sun.identity.console.agentconfig;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.sun.identity.console.agentconfig.model.AgentsModel;
import com.sun.identity.console.agentconfig.model.WSSAttributeNames;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.web.ui.model.CCEditableListModel;
import com.sun.web.ui.view.editablelist.CCEditableList;
import com.sun.web.ui.view.html.CCDropDownMenu;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Customized View Bean for WSS Client.
 */
public class WebServiceClientEditViewBean 
    extends WebServiceEditViewBean {
    private static final String PAGE_NAME = "WebServiceClientEdit";
    static final String CHILD_NAME_USERTOKEN_NAME = "usernametokenname";
    static final String CHILD_NAME_USERTOKEN_PASSWORD = "usernametokenpassword";
    private static final String CHILD_NAME_SAML_ATTR_MAPPING =
        "SAMLAttributeMapping";

    static final String DEFAULT_DISPLAY_URL =
        "/console/agentconfig/WebServiceClientEdit.jsp";
    
    private static Map attrToChildNames = new HashMap();
    
    private Set clientUIProperties = parseExternalizeUIProperties(
        "webServiceClientUI");
    
    static {
        attrToChildNames.put("isResponseSign", "isresponsesigned");
        attrToChildNames.put("userpassword", "userpassword");
        attrToChildNames.put("SecurityMech", "SecurityMech");
        attrToChildNames.put("WSPProxyEndpoint", "wssproxyEndPoint");
        attrToChildNames.put("privateKeyAlias", "certalias");
        attrToChildNames.put("forceUserAuthn", "foruserauthn");
        attrToChildNames.put("isRequestEncrypt", "isresponsedecrypted");
        attrToChildNames.put("isRequestHeaderEncrypt",
            "isRequestHeaderEncrypt");
        attrToChildNames.put("UserCredential", "usernametokenname");
        attrToChildNames.put("keepSecurityHeaders", "preservesecurityheader");
        attrToChildNames.put("Discovery", "discovery");
        attrToChildNames.put("publicKeyAlias", "publicKeyAlias");
        attrToChildNames.put("WSPEndpoint", "wspendpoint");
        attrToChildNames.put("sunIdentityServerDeviceStatus",
            "sunIdentityServerDeviceStatus");
        attrToChildNames.put("STS", "sts");
        attrToChildNames.put("isRequestSign", "isrequestsigned");
        attrToChildNames.put("useDefaultStore", "keystoreusage");
        attrToChildNames.put("serviceType", "libertyservicetype");
        attrToChildNames.put("DnsClaim", "DnsClaim");
        attrToChildNames.put("isResponseEncrypt", "isrequestencrypted");
        attrToChildNames.put("SAMLAttributeMapping", "SAMLAttributeMapping");
        attrToChildNames.put("NameIDMapper", "NameIDMapper");
        attrToChildNames.put("AttributeNamespace", "AttributeNamespace");
        attrToChildNames.put("includeMemberships", "includeMemberships");
    }

    /**
     * Creates an instance of this view bean
     */
    public WebServiceClientEditViewBean() {
        super(PAGE_NAME, DEFAULT_DISPLAY_URL, true,
            "com/sun/identity/console/propertyWebServiceClientEdit.xml");
    }
    
    /**
     * Populates the <code>STS</code> and Discovery dropdown list.
     *
     * @param event Display event.
     * @throws ModelControlException if cannot access to framework model.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        AgentsModel model = (AgentsModel)getModel();

        if (!inheritedPropertyNames.contains("STS")) {
            CCDropDownMenu ccSTS = (CCDropDownMenu)getChild("sts");
            ccSTS.setOptions(createOptionList(model.getSTSConfigurations()));
        }

        if (!inheritedPropertyNames.contains("Discovery")) {
            CCDropDownMenu ccDiscovery = (CCDropDownMenu)getChild("discovery");
            ccDiscovery.setOptions(createOptionList(
                model.getDiscoveryConfigurations()));
        }
    }

    protected void setExtendedDefaultValues(Map values)
        throws AMConsoleException {
          if (!inheritedPropertyNames.contains(
            WSSAttributeNames.SAML_ATTR_MAPPING)) {
            CCEditableList list = (CCEditableList)getChild(
                CHILD_NAME_SAML_ATTR_MAPPING);
            CCEditableListModel m = (CCEditableListModel)list.getModel();
            list.resetStateData();
            m.setOptionList((Set)values.get(
                WSSAttributeNames.SAML_ATTR_MAPPING));
        }
        setExternalizeUIValues(clientUIProperties, values);
        setUserCredential(values);
    }
    
    private void setUserCredential(Map values) {
        String userCredential = getValueFromMap(values, 
            WSSAttributeNames.USERCREDENTIAL);
        if ((userCredential != null) && (userCredential.trim().length() > 0)) {
            String[] result = splitUserCredToken(userCredential);
            if (result != null) {
                propertySheetModel.setValue(CHILD_NAME_USERTOKEN_NAME, 
                    result[0]);
                propertySheetModel.setValue(CHILD_NAME_USERTOKEN_PASSWORD, 
                    result[1]);
            }
        }
    }

    protected void getExtendedFormsValues(Map values)
        throws AMConsoleException {
        String userCredName = (String) propertySheetModel.getValue(
            CHILD_NAME_USERTOKEN_NAME);
        String userCredPwd = (String) propertySheetModel.getValue(
            CHILD_NAME_USERTOKEN_PASSWORD);

        if ((userCredName != null) && (userCredName.trim().length() > 0) &&
            (userCredPwd != null) && (userCredPwd.trim().length() > 0)
        ) {
            Map map = new HashMap(2);
            map.put(userCredName, userCredPwd);
            values.put(WSSAttributeNames.USERCREDENTIAL,
                formatUserCredential(map));
        }

        CCEditableList elist = (CCEditableList) getChild(
            CHILD_NAME_SAML_ATTR_MAPPING);
        elist.restoreStateData();
        Set samlAttrMapping = getValues(elist.getModel().getOptionList());
        values.put(WSSAttributeNames.SAML_ATTR_MAPPING, samlAttrMapping);

        String nameidMaper = (String) propertySheetModel.getValue(
            WSSAttributeNames.NAME_ID_MAPPER);
        Set nameIdset = new HashSet();
        nameIdset.add(nameidMaper);
        values.put(WSSAttributeNames.NAME_ID_MAPPER, nameIdset);

        String attrnamesp = (String) propertySheetModel.getValue(
            WSSAttributeNames.ATTR_NAME_SPACE);
        Set nameSPset = new HashSet();
        nameSPset.add(attrnamesp);
        values.put(WSSAttributeNames.ATTR_NAME_SPACE, nameSPset);

        String includeMember = (String) propertySheetModel.getValue(
            WSSAttributeNames.INCLUDE_MEMEBERSHIP);
        Set includeMem = new HashSet();
        includeMem.add(includeMember);
        values.put(WSSAttributeNames.INCLUDE_MEMEBERSHIP, includeMem);

        getExternalizeUIValues(clientUIProperties, values);
    }

    protected Map getAttrToChildNamesMapping() {
        return attrToChildNames;
    }
    
    protected String handleReadonlyAttributes(String xml) {
        xml = super.handleReadonlyAttributes(xml);
        if (inheritedPropertyNames.contains(WSSAttributeNames.USERCREDENTIAL)) {
            xml = makeReadOnly(xml, "usernametokenpassword");
        }
        return xml;
    }
} 
