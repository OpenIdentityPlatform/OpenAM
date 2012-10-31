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
 * $Id: PMDefaultSessionConditionEditViewBean.java,v 1.2 2008/06/25 05:43:03 qcheng Exp $
 *
 */

package com.sun.identity.console.policy;

import com.sun.identity.console.base.model.AMAdminUtils;
import com.sun.identity.console.policy.model.PolicyModel;
import com.sun.identity.policy.plugins.SessionCondition;
import com.sun.web.ui.view.alert.CCAlert;
import java.util.Map;

public class PMDefaultSessionConditionEditViewBean
    extends ConditionEditViewBean
{
    public static final String DEFAULT_DISPLAY_URL =
        "/console/policy/PMDefaultSessionConditionEdit.jsp";

    public PMDefaultSessionConditionEditViewBean() {
        super("PMDefaultSessionConditionEdit", DEFAULT_DISPLAY_URL);
    }

    protected String getConditionXML(
        String curRealm,
        String condType,
        boolean readonly
    ) {
        String xml = (readonly) ?
            "com/sun/identity/console/propertyPMConditionSession_Readonly.xml":
            "com/sun/identity/console/propertyPMConditionSession.xml";
        return AMAdminUtils.getStringFromInputStream(
            getClass().getClassLoader().getResourceAsStream(xml));
    }

    protected String getMissingValuesMessage() {
        return "policy.condition.missing.session.max.time";
    }

    protected Map getConditionValues(
        PolicyModel model,
        String realmName,
        String conditionType
    ) {
        Map values = super.getConditionValues(model, realmName, conditionType);
        if (values.get(SessionCondition.MAX_SESSION_TIME) == null) {
            setInlineAlertMessage(CCAlert.TYPE_ERROR, "message.error",
                 getMissingValuesMessage());
            values = null;
        }
        return values;
    }
}
