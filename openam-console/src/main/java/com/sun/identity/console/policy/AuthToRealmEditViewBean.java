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
 * $Id: AuthToRealmEditViewBean.java,v 1.2 2008/06/25 05:43:01 qcheng Exp $
 *
 */

/*
 * Portions Copyrighted [2011] [ForgeRock AS]
 */
package com.sun.identity.console.policy;

import com.iplanet.jato.model.ModelControlException;
import com.iplanet.jato.view.event.DisplayEvent;
import com.iplanet.jato.view.event.RequestInvocationEvent;
import com.sun.identity.console.base.model.AMConsoleException;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.web.ui.view.alert.CCAlert;
import com.sun.web.ui.view.html.CCSelect;
import java.util.Map;
import java.util.Set;

public class AuthToRealmEditViewBean
    extends ConditionEditViewBean
{
    private static AuthToRealmHelper helper = AuthToRealmHelper.getInstance();

    private String realmValue;

    /**
     * Default Display URL.
     */
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/AuthToRealmEdit.jsp";

    /**
     * Default Constructor.
     */
    public AuthToRealmEditViewBean() {
        super("AuthToRealmEdit", DEFAULT_DISPLAY_URL);
    }

    protected String getConditionXML(
        String curRealm,
        String condType,
        boolean readonly
    ) {
        return helper.getConditionXML(false, readonly);
    }

    protected String getMissingValuesMessage() {
        return helper.getMissingValuesMessage();
    }

    protected void setPropertiesValues(Map values) {
        helper.setPropertiesValues(values, propertySheetModel, getModel());
        realmValue = (String)propertySheetModel.getValue(
                AuthToRealmHelper.ATTR_VALUE);
    }

    protected Map getConditionValues(
        PolicyModel model,
        String realmName,
        String conditionType
    ) {
        return helper.getConditionValues(model, propertySheetModel);
    }

    public void beginDisplay(DisplayEvent event)
        throws ModelControlException
    {
        super.beginDisplay(event);
        String filter = (String)getDisplayFieldValue(
            AuthToRealmHelper.ATTR_FILTER);
        if ((filter == null) || (filter.trim().length() == 0)) {
            setDisplayFieldValue(AuthToRealmHelper.ATTR_FILTER, "*");
        }

        try {
            Set realmNames = helper.getRealmNames(filter,
                (PolicyModel)getModel());
            if (realmNames.isEmpty()) {
                setInlineAlertMessage(CCAlert.TYPE_INFO, "message.information",
                    "policy.condition.authtorealm.no.search.result.message");
            }

            if (realmValue != null) {
                realmNames.add(realmValue);
            }

            CCSelect sl = (CCSelect)getChild(AuthToRealmHelper.ATTR_VALUE);
            sl.setOptions(createOptionList(realmNames));
        } catch (AMConsoleException e) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                e.getMessage());
        }
    }

    public void handleBtnSearchRequest(RequestInvocationEvent event) {
        forwardTo();
    }
}
