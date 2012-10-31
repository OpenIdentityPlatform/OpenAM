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
 * $Id: AuthToServiceAddViewBean.java,v 1.2 2008/06/25 05:43:01 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.View;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.iplanet.jato.view.html.Button;
import com.iplanet.jato.view.html.OptionList;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCSelect;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * Authenticate to Service Condition View Bean (Add).
 */
public class AuthToServiceAddViewBean
    extends ConditionAddViewBean
{
    private static AuthToServiceHelper helper =
            AuthToServiceHelper.getInstance();

    /**
     * Default Display URL.
     */
    public static final String DEFAULT_DISPLAY_URL =
            "/console/policy/AuthToServiceAdd.jsp";

    private static final String BTN_REALM = "btnRealm";

    private boolean bRealmSelect;

    /**
     * Default Constructor.
     */
    public AuthToServiceAddViewBean() {
        super("AuthToServiceAdd", DEFAULT_DISPLAY_URL);
    }

    protected void registerChildren() {
        super.registerChildren();
        registerChild(BTN_REALM, Button.class);
    }

    protected View createChild(String name) {
        return (name.equals(BTN_REALM)) ?
                new Button(this, name, "") :
                super.createChild(name);
    }

    /**
     * Set the realm names and service names.
     *
     * @param event Display event.
     * @throws ModelControlException if default <code>JATO</code> cannot be
     *         instantiated.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        String filter = (String)getDisplayFieldValue(
                AuthToServiceHelper.ATTR_FILTER);
        if ((filter == null) || (filter.trim().length() == 0)) {
            setDisplayFieldValue(AuthToServiceHelper.ATTR_FILTER, "*");
        }

        Set realmNames = getRealmNames(filter);

        if ((realmNames != null) && !realmNames.isEmpty()) {
            realmNames.add("");
            CCSelect sl = (CCSelect)getChild(AuthToServiceHelper.ATTR_REALM);
            sl.setOptions(createOptionList(getLabelValueMap(realmNames)));
            String realm = null;
            
            if (!bRealmSelect) {
                realm = (String)realmNames.iterator().next();
                sl.setValue(realm);
            } else {
                realm = (String)sl.getValue();
            }
            setServiceNames(realm);
        } else {
            CCSelect slService = (CCSelect)getChild(
                    AuthToServiceHelper.ATTR_SERVICE);
            slService.setOptions(new OptionList());
        }
    }

    private Set getRealmNames(String filter) {
        Set realmNames = null;
        try {
            realmNames = helper.getRealmNames(filter,
                    (PolicyModel)getModel());
            if (realmNames.isEmpty() && !isInlineAlertMessageSet()) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                        "policy.condition.authlevel.no.search.result.message");
            }
        } catch (AMConsoleException e) {
            if (!isInlineAlertMessageSet()) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        e.getMessage());
            }
        }
        return realmNames;
    }

    private void setServiceNames(String realm) {
        try {
            Set serviceNames = helper.getAssignedServiceNamesInRealm(realm, 
                    (PolicyModel)getModel());
            CCSelect slService = (CCSelect)getChild(
                    AuthToServiceHelper.ATTR_SERVICE);
            slService.setOptions(createOptionList(serviceNames));
        } catch (AMConsoleException e) {
            if (!isInlineAlertMessageSet()) {
                setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                        e.getMessage());
            }
        }
    }

    protected String getConditionXML(
        String curRealm,
        String condType,
        boolean readonly
    ) {
        return AMAdminUtils.getStringFromInputStream(
                getClass().getClassLoader().getResourceAsStream(
              "com/sun/identity/console/propertyPMConditionAuthToService.xml"));
    }

    protected String getMissingValuesMessage() {
        return "policy.condition.missing.auth.level";
    }

    protected Map getConditionValues(
        PolicyModel model,
        String realmName,
        String conditionType
    ) {
        Map map = Collections.EMPTY_MAP;
        try {
            map = helper.getConditionValues(
                    (PolicyModel)getModel(), propertySheetModel);
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
        return map;
    }

    /**
     * Refreshes the view so that search for realms can be done.
     *
     * @param event Request Invocation Event.
     */
    public void handleBtnSearchRequest(RequestInvocationEvent event) {
        submitCycle = true;
        forwardTo();
    }

    /**
     * Refreshes the view so that search for services can be done.
     *
     * @param event Request Invocation Event.
     */
    public void handleBtnRealmRequest(RequestInvocationEvent event) {
        submitCycle = true;
        bRealmSelect = true;
        forwardTo();
    }

    protected void setPropertiesValues(Map values) {
        helper.setPropertiesValues(values, propertySheetModel);
    }

    /**
     * Overwrites this method to set the <code>bRealmSelect</code>.
     * 
     * @param event Request Invocation Event.
     * @throws ModelControlException if <code>JATO</code> default model fails.
     */
    public void handleButton2Request(RequestInvocationEvent event)
        throws ModelControlException
    {
        bRealmSelect = true;
        super.handleButton2Request(event);
    }
}
