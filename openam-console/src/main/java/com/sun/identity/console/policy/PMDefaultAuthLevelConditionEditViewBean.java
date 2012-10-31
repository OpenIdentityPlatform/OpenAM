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
 * $Id: PMDefaultAuthLevelConditionEditViewBean.java,v 1.2 2008/06/25 05:43:02 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCSelect;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

public class PMDefaultAuthLevelConditionEditViewBean
    extends ConditionEditViewBean
{
    private static AuthLevelHelper helper = AuthLevelHelper.getInstance();

    private String realmValue;
    private boolean bSearchForRealm;

    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/PMDefaultAuthLevelConditionEdit.jsp";

    public PMDefaultAuthLevelConditionEditViewBean() {
        super("PMDefaultAuthLevelConditionEdit", DEFAULT_DISPLAY_URL);
    }

    /**
     * Searches for realms and set up realm selectable list.
     *
     * @param event Display Event.
     * @throws ModelControlException if <code>JATO</code> default model fails.
     */
    public void beginDisplay(DisplayEvent event)
        throws ModelControlException {
        super.beginDisplay(event);
        String filter = (String)getDisplayFieldValue(
                AuthLevelHelper.ATTR_FILTER);
        if ((filter == null) || (filter.trim().length() == 0)) {
           setDisplayFieldValue(AuthLevelHelper.ATTR_FILTER, "*");
        }
                                                                                
        PolicyModel model = (PolicyModel)getModel();

        try {
            Set realmNames = helper.getRealmNames(filter, model);
            if (realmNames.isEmpty()) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "policy.condition.authlevel.no.search.result.message");
            } else {
                realmNames.add("");
            }
            String realmValue = (String)propertySheetModel.getValue(
                AuthLevelHelper.ATTR_REALM);
            if (realmValue == null) {
                realmValue = "";
            }

            if (!bSearchForRealm) {
                if ((realmValue.length() > 0) &&
                    !realmNames.contains(realmValue)
                ) {
                    realmNames.add(realmValue);
                    if (!isInlineAlertMessageSet()) {
                        String msg = model.getLocalizedString(
                            "policy.condition.authlevel.no.longer.exist.realm");
                        String[] param = {realmValue};
                        setInlineAlertMessage(CCAlert.TYPE_INFO,
                                "message.information",
                                MessageFormat.format(msg, (Object[])param));
                    }
                }
            }
            CCSelect sl = (CCSelect)getChild(AuthLevelHelper.ATTR_REALM);
            sl.setOptions(createOptionList(getLabelValueMap(realmNames)));

            if (sl.getValue() == null) {
                sl.setValue(realmValue);
            }
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                    e.getMessage());
        }
    }

    protected String getConditionXML(
        String curRealm,
        String condType,
        boolean readonly
    ) {
        String xml = (readonly) ?
        "com/sun/identity/console/propertyPMConditionAuthLevel_Readonly.xml":
            "com/sun/identity/console/propertyPMConditionAuthLevel.xml";
        return AMAdminUtils.getStringFromInputStream(
            getClass().getClassLoader().getResourceAsStream(xml));
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
        bSearchForRealm = true;
        forwardTo();
    }

    protected void setPropertiesValues(Map values) {
        helper.setPropertiesValues(values, propertySheetModel);
    }
}
